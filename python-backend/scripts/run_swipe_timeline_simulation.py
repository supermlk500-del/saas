from __future__ import annotations

import csv
import json
from dataclasses import asdict, dataclass
from datetime import date, datetime, timedelta
from pathlib import Path
import sys

from sqlalchemy import delete, select

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from app.db.session import SessionLocal
from app.models import (
    ConstraintRule,
    ConstraintRuleItem,
    ConstraintVersion,
    Machine,
    MachineCapabilityParam,
    MachineCapabilitySnapshot,
    MachineShiftAvailability,
    SchedulePlanItem,
    SchedulePlanVersion,
    ScheduleRunLog,
    ScheduleTask,
    ScheduleUnassignedTask,
)
from app.schemas.constraint import ConstraintVersionPublish
from app.schemas.machine import SnapshotGenerateRequest
from app.schemas.schedule import BaselineScheduleRunRequest, LineCompensationRequest, RealtimeRescheduleRequest
from app.services.constraint_service import ConstraintService
from app.services.machine_service import MachineService
from app.services.schedule_service import ScheduleService


@dataclass
class SwipeEvent:
    event_time: str
    event_type: str
    event_title: str
    detail: str
    ref_no: str


def _add_event(
    events: list[SwipeEvent],
    current_time: datetime,
    event_type: str,
    event_title: str,
    detail: str,
    ref_no: str,
) -> None:
    events.append(
        SwipeEvent(
            event_time=current_time.strftime("%Y-%m-%d %H:%M:%S"),
            event_type=event_type,
            event_title=event_title,
            detail=detail,
            ref_no=ref_no,
        )
    )


def _assume_customer_priority(index: int) -> str:
    if index % 20 == 0:
        return "P0"
    if index % 5 == 0:
        return "P1"
    return "P2"


def _seed_machine_snapshot_and_constraints(db, tag: str, today: date) -> tuple[str, str]:
    machine_types = ["WEAVE", "WARP", "DYE", "SET", "FINISH"]
    machine_prefix = {"WEAVE": "WV", "WARP": "WP", "DYE": "DY", "SET": "ST", "FINISH": "FN"}

    machines: list[Machine] = []
    type_counter: dict[str, int] = {k: 0 for k in machine_types}
    for i in range(1, 41):
        machine_type = machine_types[(i - 1) % len(machine_types)]
        type_counter[machine_type] += 1
        seq = type_counter[machine_type]
        code = f"{machine_prefix[machine_type]}-{seq:02d}"
        m = Machine(
            machine_code=code,
            machine_name=f"{machine_type}-{seq:02d}",
            machine_type=machine_type,
            status="active",
            remark="swipe-month simulation",
            created_by="swipe",
            updated_by="swipe",
        )
        db.add(m)
        machines.append(m)
    db.commit()
    for m in machines:
        db.refresh(m)

    for idx, m in enumerate(machines, start=1):
        db.add(
            MachineCapabilityParam(
                machine_id=m.id,
                speed_value=420 + (idx % 9) * 30,
                speed_unit="m/hour",
                changeover_loss=0.25 + (idx % 3) * 0.15,
                stability_score=93.0,
                param_version=f"PV-SWIPE-{tag}",
                is_active=True,
                created_by="swipe",
                updated_by="swipe",
            )
        )
        for shift in ["A", "B", "C"]:
            db.add(
                MachineShiftAvailability(
                    machine_id=m.id,
                    shift_code=shift,
                    available_flag=True,
                    unavailable_reason=None,
                    effective_date=today,
                    created_by="swipe",
                    updated_by="swipe",
                )
            )
    db.commit()

    snapshot_no = f"SNAP-SWIPE-{tag}"
    MachineService.generate_snapshot(db, SnapshotGenerateRequest(snapshot_no=snapshot_no, generated_by="swipe"))

    rule_defs = [
        ("due_priority", {"strict_due": "false", "sort_by": "customer_due_priority"}),
        (
            "machine_limit",
            {
                "allow_machine_types": "WEAVE,WARP,DYE,SET,FINISH",
                "allowed_fabric_types": "FAB_A,FAB_B,FAB_C,FAB_D,FAB_E",
            },
        ),
        ("changeover", {"same_color_continuous": "true", "color_change_loss": "0.6", "fabric_change_loss": "0.8"}),
        ("continuous_limit", {"max_task_per_machine": "500", "min_start_batch": "100"}),
        ("manual_lock", {"lock_order_nos": ""}),
    ]

    created_rules: list[ConstraintRule] = []
    for idx, (rule_type, _items) in enumerate(rule_defs, start=1):
        rule = ConstraintRule(
            rule_code=f"SWIPE-RULE-{tag}-{idx:02d}",
            rule_name=f"SWIPE-{rule_type}",
            rule_type=rule_type,
            status="active",
            created_by="swipe",
            updated_by="swipe",
        )
        db.add(rule)
        created_rules.append(rule)
    db.commit()
    for r in created_rules:
        db.refresh(r)
    rule_map = {r.rule_type: r for r in created_rules}

    for rule_type, items in rule_defs:
        order_num = 1
        for key, value in items.items():
            db.add(
                ConstraintRuleItem(
                    rule_id=rule_map[rule_type].id,
                    item_key=key,
                    item_value=value,
                    item_order=order_num,
                    enabled_flag=True,
                    created_by="swipe",
                    updated_by="swipe",
                )
            )
            order_num += 1

    constraint_version_no = f"CV-SWIPE-{tag}"
    db.add(
        ConstraintVersion(
            version_no=constraint_version_no,
            version_name="SWIPE_MONTH_CONSTRAINTS",
            version_status="draft",
            published_flag=False,
            created_by="swipe",
            updated_by="swipe",
        )
    )
    for r in created_rules:
        r.version_no = constraint_version_no
    db.commit()
    ConstraintService.publish_version(db, constraint_version_no, ConstraintVersionPublish(published_by="swipe"))
    return snapshot_no, constraint_version_no


def _cleanup_previous_swipe_demo_data(db) -> None:
    demo_machine_prefixes = ("WV-", "WP-", "DY-", "ST-", "FN-")
    machine_ids = list(
        db.scalars(
            select(Machine.id).where(
                (Machine.created_by.in_(["swipe", "live_demo"]))
                | (Machine.remark.in_(["swipe-month simulation", "演示专用机台"]))
                | (Machine.machine_code.like(f"{demo_machine_prefixes[0]}%"))
                | (Machine.machine_code.like(f"{demo_machine_prefixes[1]}%"))
                | (Machine.machine_code.like(f"{demo_machine_prefixes[2]}%"))
                | (Machine.machine_code.like(f"{demo_machine_prefixes[3]}%"))
                | (Machine.machine_code.like(f"{demo_machine_prefixes[4]}%"))
            )
        ).all()
    )
    rule_ids = list(
        db.scalars(select(ConstraintRule.id).where(ConstraintRule.created_by.in_(["swipe", "live_demo"]))).all()
    )
    plan_ids = list(
        db.scalars(select(SchedulePlanVersion.id).where(SchedulePlanVersion.generated_by.in_(["swipe", "live_demo"]))).all()
    )
    task_ids = list(
        db.scalars(select(ScheduleTask.id).where(ScheduleTask.created_by.in_(["swipe", "live_demo"]))).all()
    )
    constraint_version_nos = list(
        db.scalars(
            select(ConstraintVersion.version_no).where(ConstraintVersion.created_by.in_(["swipe", "live_demo"]))
        ).all()
    )
    plan_version_nos = list(
        db.scalars(
            select(SchedulePlanVersion.plan_version_no).where(
                SchedulePlanVersion.generated_by.in_(["swipe", "live_demo"])
            )
        ).all()
    )

    if plan_ids:
        db.execute(delete(SchedulePlanItem).where(SchedulePlanItem.plan_version_id.in_(plan_ids)))
        db.execute(delete(ScheduleUnassignedTask).where(ScheduleUnassignedTask.plan_version_id.in_(plan_ids)))
        db.execute(delete(SchedulePlanVersion).where(SchedulePlanVersion.id.in_(plan_ids)))

    if task_ids:
        db.execute(delete(ScheduleTask).where(ScheduleTask.id.in_(task_ids)))

    if rule_ids:
        db.execute(delete(ConstraintRuleItem).where(ConstraintRuleItem.rule_id.in_(rule_ids)))
        db.execute(delete(ConstraintRule).where(ConstraintRule.id.in_(rule_ids)))

    if constraint_version_nos:
        db.execute(delete(ConstraintVersion).where(ConstraintVersion.version_no.in_(constraint_version_nos)))

    if plan_version_nos:
        db.execute(delete(ScheduleRunLog).where(ScheduleRunLog.business_no.in_(plan_version_nos)))

    if machine_ids:
        db.execute(delete(MachineCapabilitySnapshot).where(MachineCapabilitySnapshot.machine_id.in_(machine_ids)))
        db.execute(delete(MachineCapabilityParam).where(MachineCapabilityParam.machine_id.in_(machine_ids)))
        db.execute(delete(MachineShiftAvailability).where(MachineShiftAvailability.machine_id.in_(machine_ids)))
        db.execute(delete(Machine).where(Machine.id.in_(machine_ids)))

    db.commit()


def _seed_orders_bulk(db, tag: str, start_date: date, batch_name: str, order_count: int) -> list[int]:
    fabrics = ["FAB_A", "FAB_B", "FAB_C", "FAB_D", "FAB_E"]
    colors = ["BLUE", "BLACK", "WHITE", "GRAY", "RED", "GREEN"]
    routes = [
        "WARP>WEAVE>DYE>SET>FINISH",
        "WARP>WEAVE>FINISH",
        "WEAVE>DYE>FINISH",
        "WARP>WEAVE>SET>FINISH",
    ]

    rows: list[ScheduleTask] = []
    for i in range(1, order_count + 1):
        due_date = start_date + timedelta(days=((i - 1) % 30) + 1)
        order_no = f"ORD-{batch_name}-{tag}-{i:05d}"
        qty = 260 + (i % 40) * 55
        customer_priority = _assume_customer_priority(i)
        row = ScheduleTask(
            order_no=order_no,
            fabric_type=fabrics[i % len(fabrics)],
            width_cm=150 + (i % 4) * 10,
            gram_weight=180 + (i % 5) * 20,
            color_code=colors[i % len(colors)],
            process_route=routes[i % len(routes)],
            order_quantity=float(qty),
            quantity_unit="m",
            due_urgency_level=ScheduleService._derive_due_urgency(due_date, start_date),
            customer_priority_level=customer_priority,
            due_date=due_date,
            priority_level=9 - (i % 4),
            task_status="pending",
            created_by="swipe",
            updated_by="swipe",
        )
        rows.append(row)

    chunk = 1000
    for i in range(0, len(rows), chunk):
        db.add_all(rows[i : i + chunk])
        db.commit()

    ids = list(
        db.scalars(
            select(ScheduleTask.id).where(ScheduleTask.order_no.like(f"ORD-{batch_name}-{tag}-%")).order_by(ScheduleTask.id)
        ).all()
    )
    return ids


def run_swipe_timeline_simulation() -> None:
    db = SessionLocal()
    tag = datetime.now().strftime("%Y%m%d%H%M%S")
    start_time = datetime.now().replace(microsecond=0)
    start_date = start_time.date()
    out_dir = ROOT / "数据"
    out_dir.mkdir(parents=True, exist_ok=True)
    events: list[SwipeEvent] = []

    try:
        _cleanup_previous_swipe_demo_data(db)
        snapshot_no, constraint_version_no = _seed_machine_snapshot_and_constraints(db, tag, start_date)
        _add_event(events, start_time, "SETUP", "Initialize resources", "40 machines + snapshot + constraints ready", snapshot_no)

        _seed_orders_bulk(db, tag, start_date, "BASE", 10000)
        _add_event(events, start_time + timedelta(minutes=20), "ORDER", "Load baseline orders", "Loaded 10000 orders", f"BASE-{tag}")

        baseline_plan_no = f"PLAN-SWIPE-BASE-{tag}"
        baseline = ScheduleService.run_baseline_schedule(
            db,
            BaselineScheduleRunRequest(
                plan_version_no=baseline_plan_no,
                plan_version_name="SWIPE_BASELINE_MONTH_PLAN",
                snapshot_version_no=snapshot_no,
                constraint_version_no=constraint_version_no,
                generated_by="swipe",
            ),
        )
        _add_event(
            events,
            start_time + timedelta(minutes=45),
            "SCHEDULE",
            "Baseline schedule",
            f"Assigned {baseline['assigned_count']} / Unassigned {baseline['unassigned_count']}",
            baseline_plan_no,
        )

        baseline_plan = db.scalar(select(SchedulePlanVersion).where(SchedulePlanVersion.plan_version_no == baseline_plan_no))
        base_items = list(
            db.scalars(
                select(SchedulePlanItem)
                .where(SchedulePlanItem.plan_version_id == baseline_plan.id)
                .order_by(SchedulePlanItem.start_time)
            ).all()
        )
        completed_count = min(1800, len(base_items))
        completed_task_ids = {x.task_id for x in base_items[:completed_count]}
        for item in base_items[:completed_count]:
            task = db.get(ScheduleTask, item.task_id)
            if task:
                task.task_status = "completed"
                task.updated_by = "swipe"
        db.commit()
        _add_event(
            events,
            start_time + timedelta(days=3, hours=2),
            "PROGRESS",
            "Production progress",
            f"Completed {completed_count} orders",
            baseline_plan_no,
        )

        broken_machine = db.scalar(select(Machine).where(Machine.machine_name == "WEAVE-01"))
        impacted_task_ids: list[int] = []
        if broken_machine:
            broken_machine.status = "stopped"
            broken_machine.updated_by = "swipe"
            db.commit()
            for item in base_items:
                if item.machine_id == broken_machine.id and item.task_id not in completed_task_ids:
                    task = db.get(ScheduleTask, item.task_id)
                    if task and task.task_status != "completed":
                        task.task_status = "pending"
                        task.updated_by = "swipe"
                        impacted_task_ids.append(task.id)
                if len(impacted_task_ids) >= 220:
                    break
            db.commit()
        _add_event(
            events,
            start_time + timedelta(days=12, hours=4),
            "ISSUE",
            "Line issue",
            f"WEAVE-01 stopped, impacted {len(impacted_task_ids)} orders",
            broken_machine.machine_code if broken_machine else "-",
        )

        insert_ids = _seed_orders_bulk(db, tag, start_date + timedelta(days=12), "INSERT", 200)
        for task_id in insert_ids:
            task = db.get(ScheduleTask, task_id)
            if task:
                task.customer_priority_level = "P0"
                task.priority_level = 9
                task.updated_by = "swipe"
        db.commit()
        _add_event(
            events,
            start_time + timedelta(days=12, hours=6),
            "INSERT",
            "Insert orders",
            "Added 200 high-priority insert orders",
            f"INSERT-{tag}",
        )

        rt_plan_no = f"PLAN-SWIPE-RT-{tag}"
        realtime = ScheduleService.run_realtime_reschedule(
            db,
            RealtimeRescheduleRequest(
                source_plan_version_id=baseline_plan.id,
                new_plan_version_no=rt_plan_no,
                new_plan_version_name="SWIPE_REALTIME_MONTH_PLAN",
                freeze_minutes=240,
                generated_by="swipe",
            ),
        )
        _add_event(
            events,
            start_time + timedelta(days=13, hours=2),
            "RESCHEDULE",
            "Realtime reschedule",
            (
                f"Assigned {realtime['assigned_count']} / Unassigned {realtime['unassigned_count']} / "
                f"Locked {realtime['locked_count']}"
            ),
            rt_plan_no,
        )

        rt_plan = db.scalar(select(SchedulePlanVersion).where(SchedulePlanVersion.plan_version_no == rt_plan_no))
        rt_unassigned = list(
            db.scalars(select(ScheduleUnassignedTask).where(ScheduleUnassignedTask.plan_version_id == rt_plan.id)).all()
        )
        compensation_task_ids = [x.task_id for x in rt_unassigned][:260]
        if not compensation_task_ids:
            rt_items = list(
                db.scalars(
                    select(SchedulePlanItem)
                    .where(SchedulePlanItem.plan_version_id == rt_plan.id)
                    .order_by(SchedulePlanItem.start_time)
                ).all()
            )
            machine_ids = [x.machine_id for x in rt_items]
            machine_map = (
                {m.id: m for m in db.scalars(select(Machine).where(Machine.id.in_(machine_ids))).all()}
                if machine_ids
                else {}
            )
            for item in rt_items:
                m = machine_map.get(item.machine_id)
                if not m or m.machine_type != "WEAVE":
                    continue
                task = db.get(ScheduleTask, item.task_id)
                if not task or task.task_status == "completed":
                    continue
                task.task_status = "pending"
                task.updated_by = "swipe"
                compensation_task_ids.append(task.id)
                if len(compensation_task_ids) >= 260:
                    break
            db.commit()

        comp_plan_no = f"PLAN-SWIPE-COMP-{tag}"
        compensation = ScheduleService.run_line_compensation(
            db,
            LineCompensationRequest(
                source_plan_version_id=rt_plan.id,
                compensation_plan_version_no=comp_plan_no,
                compensation_plan_version_name="SWIPE_LINE_COMPENSATION",
                line_machine_type="WEAVE",
                task_ids=compensation_task_ids,
                generated_by="swipe",
            ),
        )
        _add_event(
            events,
            start_time + timedelta(days=14, hours=1),
            "COMPENSATION",
            "Line compensation",
            f"WEAVE compensation assigned {compensation['assigned_count']} / unassigned {compensation['unassigned_count']}",
            comp_plan_no,
        )

        for day in range(1, 31):
            done = min(completed_count + day * 220, 10200)
            _add_event(
                events,
                start_time + timedelta(days=day, hours=9),
                "DAY_PROGRESS",
                f"Day-{day:02d} progress",
                f"Completed about {done} orders, monitoring insert + issue impact",
                f"D{day:02d}",
            )

        comp_plan = db.scalar(select(SchedulePlanVersion).where(SchedulePlanVersion.plan_version_no == comp_plan_no))
        comp_items = list(
            db.scalars(select(SchedulePlanItem).where(SchedulePlanItem.plan_version_id == comp_plan.id)).all()
        )
        machine_ids = [x.machine_id for x in comp_items]
        machine_map = (
            {m.id: m for m in db.scalars(select(Machine).where(Machine.id.in_(machine_ids))).all()} if machine_ids else {}
        )
        in_line_count = 0
        for item in comp_items:
            machine = machine_map.get(item.machine_id)
            if machine and machine.machine_type == "WEAVE":
                in_line_count += 1
        hit_rate = round((in_line_count / len(comp_items) * 100), 2) if comp_items else 0.0

        ordered_events = sorted(events, key=lambda x: x.event_time)
        timeline_csv = out_dir / f"swipe_timeline_{tag}.csv"
        with timeline_csv.open("w", encoding="utf-8-sig", newline="") as f:
            writer = csv.DictWriter(f, fieldnames=["event_time", "event_type", "event_title", "detail", "ref_no"])
            writer.writeheader()
            for ev in ordered_events:
                writer.writerow(asdict(ev))

        summary = {
            "tag": tag,
            "machine_count": 40,
            "base_order_count": 10000,
            "insert_order_count": 200,
            "timeline_days": 30,
            "snapshot_no": snapshot_no,
            "constraint_version_no": constraint_version_no,
            "baseline_plan_no": baseline_plan_no,
            "realtime_plan_no": rt_plan_no,
            "compensation_plan_no": comp_plan_no,
            "baseline_result": baseline,
            "realtime_result": realtime,
            "compensation_result": compensation,
            "line_compensation_hit_rate": hit_rate,
            "timeline_csv": timeline_csv.name,
            "timeline_event_count": len(ordered_events),
        }
        summary_json = out_dir / f"swipe_summary_{tag}.json"
        summary_json.write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")

        print("=== SWIPE month simulation completed ===")
        print(f"Machine count: {summary['machine_count']}")
        print(f"Base orders: {summary['base_order_count']}")
        print(f"Insert orders: {summary['insert_order_count']}")
        print(f"Timeline days: {summary['timeline_days']}")
        print(f"Baseline plan: {baseline_plan_no}")
        print(f"Realtime plan: {rt_plan_no}")
        print(f"Compensation plan: {comp_plan_no}")
        print(f"Same-line compensation hit rate: {hit_rate}%")
        print(f"Timeline CSV: {timeline_csv}")
        print(f"Summary JSON: {summary_json}")
    finally:
        db.close()


if __name__ == "__main__":
    run_swipe_timeline_simulation()
