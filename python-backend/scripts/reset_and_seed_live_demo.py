from __future__ import annotations

from datetime import date, datetime, timedelta
from pathlib import Path
import sys

from sqlalchemy import delete, select

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from app.core.status import PlanStatus, SnapshotStatus
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
from app.schemas.schedule import BaselineScheduleRunRequest, RealtimeRescheduleRequest
from app.services.constraint_service import ConstraintService
from app.services.machine_service import MachineService
from app.services.schedule_service import ScheduleService


DEMO_OPERATOR = "live_demo"


def _clear_all_demo_data(db) -> None:
    db.execute(delete(SchedulePlanItem))
    db.execute(delete(ScheduleUnassignedTask))
    db.execute(delete(SchedulePlanVersion))
    db.execute(delete(ScheduleTask))
    db.execute(delete(MachineCapabilitySnapshot))
    db.execute(delete(MachineCapabilityParam))
    db.execute(delete(MachineShiftAvailability))
    db.execute(delete(Machine))
    db.execute(delete(ConstraintRuleItem))
    db.execute(delete(ConstraintRule))
    db.execute(delete(ConstraintVersion))
    db.execute(delete(ScheduleRunLog))
    db.commit()


def _seed_machines_and_snapshot(db, snapshot_no: str, today: date) -> None:
    machine_types = ["WEAVE", "WARP", "DYE", "SET", "FINISH"]
    machine_prefix = {"WEAVE": "WV", "WARP": "WP", "DYE": "DY", "SET": "ST", "FINISH": "FN"}
    type_counter: dict[str, int] = {t: 0 for t in machine_types}
    machines: list[Machine] = []

    for i in range(1, 41):
        machine_type = machine_types[(i - 1) % len(machine_types)]
        type_counter[machine_type] += 1
        seq = type_counter[machine_type]
        code = f"{machine_prefix[machine_type]}-{seq:02d}"
        machine = Machine(
            machine_code=code,
            machine_name=f"{machine_type}-{seq:02d}",
            machine_type=machine_type,
            status="active",
            remark="演示专用机台",
            created_by=DEMO_OPERATOR,
            updated_by=DEMO_OPERATOR,
        )
        db.add(machine)
        machines.append(machine)
    db.commit()
    for m in machines:
        db.refresh(m)

    for idx, m in enumerate(machines, start=1):
        db.add(
            MachineCapabilityParam(
                machine_id=m.id,
                speed_value=30 + (idx % 5) * 4,   # 30~46 m/hour，每任务约6~20小时，100单跨3天
                speed_unit="m/hour",
                changeover_loss=0.5,
                stability_score=95.0,
                param_version=f"PV-{snapshot_no}",
                is_active=True,
                created_by=DEMO_OPERATOR,
                updated_by=DEMO_OPERATOR,
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
                    created_by=DEMO_OPERATOR,
                    updated_by=DEMO_OPERATOR,
                )
            )
    db.commit()

    MachineService.generate_snapshot(db, SnapshotGenerateRequest(snapshot_no=snapshot_no, generated_by=DEMO_OPERATOR))


def _seed_constraints(db, constraint_no: str) -> None:
    # 5 rule_types × 3 items = 15 条，与 README 及发布校验要求对齐
    # rule_meta: (rule_type, is_hard, priority_level, rule_expression, items_dict)
    #   is_hard      : True  = 硬约束（违反则任务进 unassigned，计划不能包含该任务）
    #                  False = 软约束（违反仅记日志，计划仍生成但有警告标记）
    #   priority_level: 1=最高优先级，多约束冲突时先满足优先级高的
    rule_defs = [
        (
            "machine_limit",
            True,   # 硬约束：任务只能排到允许的机型，否则物理上无法生产
            1,      # 优先级最高：机型不匹配直接排不进去
            "task.machine_type IN allow_machine_types AND task.fabric_type IN allowed_fabric_types",
            {
                "allow_machine_types": "WEAVE,WARP,DYE,SET,FINISH",
                "allowed_fabric_types": "FAB_A,FAB_B,FAB_C,FAB_D,FAB_E",
                "require_active": "true",
            },
        ),
        (
            "manual_lock",
            True,   # 硬约束：被锁定的订单不允许被重排，位置固定
            1,      # 与机台限制并列最高优先级
            "task.order_no NOT IN lock_order_nos",
            {
                "lock_order_nos": "ORD-BASE-003,ORD-BASE-011",
                "lock_reason": "quality_review",
                "enabled": "true",
            },
        ),
        (
            "due_priority",
            False,  # 软约束：交期优先是排序策略，违反（如被插单挤后）不会取消计划
            2,      # 第二优先：影响任务排序，但不阻断生成
            "sort_key = (-customer_priority, -due_urgency, due_date)",
            {
                "strict_due": "false",
                "sort_by": "customer_due_priority",
                "timezone": "Asia/Shanghai",
            },
        ),
        (
            "continuous_limit",
            True,   # 硬约束：最小批量不足则任务进 unassigned，避免无效小批次
            2,      # 与交期优先同级，但性质是硬约束
            "task.order_quantity >= min_start_batch AND machine.task_count <= max_task_per_machine",
            {
                "max_task_per_machine": "500",
                "min_start_batch": "50",
                "allow_overtime": "false",
            },
        ),
        (
            "changeover",
            False,  # 软约束：换型损耗会计入工时，但不阻止排入，只影响完工时间
            3,      # 最低优先级：在满足前几条约束后再考虑换型优化
            "IF task.color != prev.color THEN duration += color_change_loss * speed; "
            "IF task.fabric != prev.fabric THEN duration += fabric_change_loss * speed",
            {
                "same_color_continuous": "true",
                "color_change_loss": "0",
                "fabric_change_loss": "0",
            },
        ),
        (
            "dye_color_changeover",
            False,
            3,
            "dye vat color changeover is derived from dedicated vats, color family, depth, undertone and wash rules",
            {
                "dedicated_machine_map": "DY-01:BLACK;DY-02:WHITE;DY-03:NAVY;DY-04:RED",
                "dedicated_strict": "true",
                "color_family_map": (
                    "BLACK:BLACK;WHITE:WHITE;NAVY:BLUE;LIGHT_BLUE:BLUE;"
                    "KHAKI:EARTH;LIGHT_KHAKI:EARTH;BEIGE:EARTH;"
                    "RED:RED;LIGHT_RED:RED;DARK_GRAY:BLACK;LIGHT_GRAY:WHITE;"
                    "BLACK_GREEN:BLACK;BLACK_RED:BLACK"
                ),
                "color_depth_map": (
                    "WHITE:1;BEIGE:2;LIGHT_KHAKI:2;LIGHT_GRAY:2;LIGHT_RED:2;"
                    "LIGHT_BLUE:2;KHAKI:3;RED:3;DARK_GRAY:4;NAVY:4;"
                    "BLACK:5;BLACK_GREEN:5;BLACK_RED:5"
                ),
                "color_undertone_map": "BLACK_GREEN:GREEN;BLACK_RED:RED",
                "default_change_loss": "0.5",
                "same_color_loss": "0",
                "cross_family_penalty": "1.0",
                "light_to_dark_loss": "0.3",
                "dark_to_light_loss": "2.0",
                "undertone_change_loss": "1.5",
                "undertone_requires_wash": "true",
                "mandatory_wash_pairs": (
                    "BLACK>WHITE;RED>WHITE;NAVY>BEIGE;"
                    "KHAKI>LIGHT_RED;LIGHT_RED>LIGHT_GRAY;LIGHT_BLUE>DARK_GRAY;"
                    "RED>DARK_GRAY;WHITE>BLACK;BLACK_GREEN>KHAKI"
                ),
                "wash_duration_hours": "2.0",
                "max_continuous_same_color": "3",
                "max_continuous_same_family": "6",
                "continuous_violation_as_hard": "false",
            },
        ),
    ]

    rules: list[ConstraintRule] = []
    for idx, (rule_type, is_hard, priority, expression, _items) in enumerate(rule_defs, start=1):
        r = ConstraintRule(
            rule_code=f"LIVE-RULE-{idx:02d}",
            rule_name=f"LIVE-{rule_type}",
            rule_type=rule_type,
            version_no=constraint_no,
            status="active",
            is_hard_constraint=is_hard,
            priority_level=priority,
            rule_expression=expression,
            created_by=DEMO_OPERATOR,
            updated_by=DEMO_OPERATOR,
        )
        db.add(r)
        rules.append(r)
    db.commit()
    for r in rules:
        db.refresh(r)
    rule_map = {r.rule_type: r for r in rules}

    for rule_type, _is_hard, _priority, _expression, items in rule_defs:
        item_order = 1
        for key, value in items.items():
            db.add(
                ConstraintRuleItem(
                    rule_id=rule_map[rule_type].id,
                    item_key=key,
                    item_value=value,
                    item_order=item_order,
                    enabled_flag=True,
                    created_by=DEMO_OPERATOR,
                    updated_by=DEMO_OPERATOR,
                )
            )
            item_order += 1

    db.add(
        ConstraintVersion(
            version_no=constraint_no,
            version_name="实时重排演示约束",
            version_status="draft",
            published_flag=False,
            created_by=DEMO_OPERATOR,
            updated_by=DEMO_OPERATOR,
        )
    )
    db.commit()
    ConstraintService.publish_version(db, constraint_no, ConstraintVersionPublish(published_by=DEMO_OPERATOR))


def _create_task(
    *,
    order_no: str,
    due_date: date,
    qty: float,
    color_code: str,
    customer_priority: str,
    priority_level: int,
    source_flag: str,
) -> ScheduleTask:
    fabric = "FAB_A"
    route = "WARP>WEAVE>DYE>SET>FINISH"
    return ScheduleTask(
        order_no=order_no,
        fabric_type=fabric,
        width_cm=160.0,
        gram_weight=210.0,
        color_code=color_code,
        process_route=route,
        order_quantity=qty,
        quantity_unit="m",
        due_urgency_level="P1",
        customer_priority_level=customer_priority,
        due_date=due_date,
        priority_level=priority_level,
        task_status="pending",
        created_by=DEMO_OPERATOR,
        updated_by=DEMO_OPERATOR,
    )


def _seed_base_orders(db, base_day: date) -> list[int]:
    base_ids: list[int] = []
    colors = [
        "BLACK",
        "WHITE",
        "NAVY",
        "RED",
        "KHAKI",
        "LIGHT_KHAKI",
        "BEIGE",
        "DARK_GRAY",
        "LIGHT_GRAY",
        "LIGHT_BLUE",
        "LIGHT_RED",
        "BLACK_GREEN",
        "BLACK_RED",
    ]

    base_rows: list[ScheduleTask] = []
    for i in range(1, 101):
        row = _create_task(
            order_no=f"ORD-BASE-{i:03d}",
            due_date=base_day + timedelta(days=5),
            qty=400 + (i % 5) * 60,
            color_code=colors[i % len(colors)],
            customer_priority="P2",
            priority_level=3,
            source_flag="BASE",
        )
        base_rows.append(row)
    db.add_all(base_rows)
    db.commit()
    base_ids = list(db.scalars(select(ScheduleTask.id).where(ScheduleTask.order_no.like("ORD-BASE-%")).order_by(ScheduleTask.id)).all())
    return base_ids


def _seed_wave_orders(
    db,
    *,
    prefix: str,
    count: int,
    due_date: date,
    qty_base: int,
    qty_step_mod: int,
) -> list[int]:
    colors = [
        "BLACK",
        "WHITE",
        "NAVY",
        "RED",
        "KHAKI",
        "LIGHT_KHAKI",
        "BEIGE",
        "DARK_GRAY",
        "LIGHT_GRAY",
        "LIGHT_BLUE",
        "LIGHT_RED",
        "BLACK_GREEN",
        "BLACK_RED",
    ]
    rows: list[ScheduleTask] = []
    for i in range(1, count + 1):
        row = _create_task(
            order_no=f"ORD-{prefix}-{i:03d}",
            due_date=due_date,
            qty=qty_base + (i % qty_step_mod) * 10,
            color_code=colors[(i + 1) % len(colors)],
            customer_priority="P0",
            priority_level=9,
            source_flag=prefix,
        )
        rows.append(row)
    db.add_all(rows)
    db.commit()
    return list(db.scalars(select(ScheduleTask.id).where(ScheduleTask.order_no.like(f"ORD-{prefix}-%")).order_by(ScheduleTask.id)).all())


def _clone_realtime_as_comp_plan(
    db,
    *,
    source_plan: SchedulePlanVersion,
    comp_plan_no: str,
    comp_task_ids: list[int],
    base_day_start: datetime,
) -> SchedulePlanVersion:
    comp_plan = SchedulePlanVersion(
        plan_version_no=comp_plan_no,
        plan_version_name="补单后计划",
        plan_status=PlanStatus.GENERATED,
        snapshot_version_no=source_plan.snapshot_version_no,
        constraint_version_no=source_plan.constraint_version_no,
        generated_at=datetime.utcnow(),
        generated_by=DEMO_OPERATOR,
    )
    db.add(comp_plan)
    db.commit()
    db.refresh(comp_plan)

    src_items = list(
        db.scalars(
            select(SchedulePlanItem)
            .where(SchedulePlanItem.plan_version_id == source_plan.id)
            .order_by(SchedulePlanItem.start_time, SchedulePlanItem.id)
        ).all()
    )
    for it in src_items:
        db.add(
            SchedulePlanItem(
                plan_version_id=comp_plan.id,
                task_id=it.task_id,
                machine_id=it.machine_id,
                start_time=it.start_time,
                end_time=it.end_time,
                planned_quantity=it.planned_quantity,
                item_status=it.item_status or "planned",
            )
        )
    db.commit()

    weave_machines = list(db.scalars(select(Machine).where(Machine.machine_type == "WEAVE").order_by(Machine.machine_name)).all())
    if len(weave_machines) < 3:
        raise RuntimeError("WEAVE 机台数量不足，无法构造2号线故障补单场景")
    broken_line = weave_machines[1]
    broken_line.status = "stopped"
    broken_line.updated_by = DEMO_OPERATOR
    db.commit()

    candidate_machines = [m for m in weave_machines if m.id != broken_line.id]
    if not candidate_machines:
        raise RuntimeError("无可用替代机台")
    candidate_ids = [m.id for m in candidate_machines]

    comp_snapshot_map = {
        s.machine_id: s
        for s in db.scalars(
            select(MachineCapabilitySnapshot).where(
                MachineCapabilitySnapshot.snapshot_no == source_plan.snapshot_version_no,
                MachineCapabilitySnapshot.snapshot_status == SnapshotStatus.GENERATED,
                MachineCapabilitySnapshot.available_flag.is_(True),
                MachineCapabilitySnapshot.machine_id.in_(candidate_ids),
            )
        ).all()
    }

    # Mark line-2 as unavailable in current snapshot for visual consistency.
    broken_snapshot = db.scalar(
        select(MachineCapabilitySnapshot).where(
            MachineCapabilitySnapshot.snapshot_no == source_plan.snapshot_version_no,
            MachineCapabilitySnapshot.machine_id == broken_line.id,
        )
    )
    if broken_snapshot:
        broken_snapshot.available_flag = False
        broken_snapshot.speed_value = 0.0
    db.commit()

    next_available: dict[int, datetime] = {m.id: base_day_start + timedelta(days=2, hours=8) for m in candidate_machines}
    existing_items = list(
        db.scalars(
            select(SchedulePlanItem).where(
                SchedulePlanItem.plan_version_id == comp_plan.id, SchedulePlanItem.machine_id.in_(candidate_ids)
            )
        ).all()
    )
    for m in candidate_machines:
        m_items = [x for x in existing_items if x.machine_id == m.id]
        if m_items:
            next_available[m.id] = max(next_available[m.id], max(x.end_time for x in m_items))

    for task_id in comp_task_ids:
        task = db.get(ScheduleTask, task_id)
        if not task:
            continue
        qty = float(task.order_quantity)
        chosen_machine_id = None
        chosen_start = None
        chosen_end = None
        best_end = None
        for machine in candidate_machines:
            snap = comp_snapshot_map.get(machine.id)
            if not snap or snap.speed_value <= 0:
                continue
            hours = max((qty / snap.speed_value) + snap.changeover_loss, 0.1)
            start_time = next_available[machine.id]
            end_time = start_time + timedelta(hours=hours)
            if best_end is None or end_time < best_end:
                best_end = end_time
                chosen_machine_id = machine.id
                chosen_start = start_time
                chosen_end = end_time
        if chosen_machine_id is None:
            continue

        db.add(
            SchedulePlanItem(
                plan_version_id=comp_plan.id,
                task_id=task.id,
                machine_id=chosen_machine_id,
                start_time=chosen_start,
                end_time=chosen_end,
                planned_quantity=int(round(qty)),
                item_status="compensated",
            )
        )
        task.task_status = "scheduled"
        task.updated_by = DEMO_OPERATOR
        next_available[chosen_machine_id] = chosen_end

    db.commit()
    return comp_plan


def reset_and_seed_live_demo() -> None:
    db = SessionLocal()
    today = date.today()
    base_day = today
    base_day_start = datetime.combine(base_day, datetime.min.time())

    snapshot_no = f"SNAP-LIVE-{today.strftime('%Y%m%d')}"
    constraint_no = f"CV-LIVE-{today.strftime('%Y%m%d')}"
    base_plan_no = f"PLAN-LIVE-BASE-{today.strftime('%Y%m%d')}"
    rt_plan_no = f"PLAN-LIVE-RT-{today.strftime('%Y%m%d')}"
    comp_plan_no = f"PLAN-LIVE-COMP-{today.strftime('%Y%m%d')}"

    try:
        _clear_all_demo_data(db)
        _seed_machines_and_snapshot(db, snapshot_no, today)
        _seed_constraints(db, constraint_no)
        _seed_base_orders(db, base_day)

        base_result = ScheduleService.run_baseline_schedule(
            db,
            BaselineScheduleRunRequest(
                plan_version_no=base_plan_no,
                plan_version_name="基线计划(100订单)",
                snapshot_version_no=snapshot_no,
                constraint_version_no=constraint_no,
                generated_by=DEMO_OPERATOR,
            ),
        )
        base_plan = db.scalar(select(SchedulePlanVersion).where(SchedulePlanVersion.plan_version_no == base_plan_no))
        if not base_plan:
            raise RuntimeError("基线计划生成失败")

        _seed_wave_orders(
            db,
            prefix="INSERT",
            count=20,
            due_date=base_day + timedelta(days=3),
            qty_base=350,
            qty_step_mod=4,
        )

        rt_result = ScheduleService.run_realtime_reschedule(
            db,
            RealtimeRescheduleRequest(
                source_plan_version_id=base_plan.id,
                new_plan_version_no=rt_plan_no,
                new_plan_version_name="插单重排计划(100+20)",
                freeze_minutes=720,
                generated_by=DEMO_OPERATOR,
            ),
        )
        rt_plan = db.scalar(select(SchedulePlanVersion).where(SchedulePlanVersion.plan_version_no == rt_plan_no))
        if not rt_plan:
            raise RuntimeError("实时重排计划生成失败")

        comp_task_ids = _seed_wave_orders(
            db,
            prefix="COMP",
            count=20,
            due_date=base_day + timedelta(days=2),
            qty_base=100,
            qty_step_mod=4,
        )
        comp_task_ids = list(
            db.scalars(select(ScheduleTask.id).where(ScheduleTask.id.in_(comp_task_ids)).order_by(ScheduleTask.id)).all()
        )[:20]
        comp_plan = _clone_realtime_as_comp_plan(
            db,
            source_plan=rt_plan,
            comp_plan_no=comp_plan_no,
            comp_task_ids=comp_task_ids,
            base_day_start=base_day_start,
        )

        print("=== LIVE DEMO DATA READY ===")
        print(f"snapshot_no: {snapshot_no}")
        print(f"constraint_no: {constraint_no}")
        print(f"base_plan_no: {base_plan_no} -> {base_result}")
        print(f"realtime_plan_no: {rt_plan_no} -> {rt_result}")
        print(f"comp_plan_no: {comp_plan.plan_version_no}")
        print("orders: BASE=100, INSERT=20, COMP=20")
        print("line_issue: WEAVE-02 stopped, comp orders scheduled on other WEAVE lines")
    finally:
        db.close()


if __name__ == "__main__":
    reset_and_seed_live_demo()
