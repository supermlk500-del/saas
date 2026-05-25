from __future__ import annotations

from datetime import date, datetime, timedelta
from pathlib import Path
import random
import sys

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from sqlalchemy import func, select

from app.core.domain import FABRIC_MACHINE_TYPES
from app.db.session import SessionLocal
from app.models import (
    ConstraintVersion,
    ConstraintRule,
    ConstraintRuleItem,
    Machine,
    MachineCapabilityParam,
    MachineShiftAvailability,
    SchedulePlanItem,
    SchedulePlanVersion,
    ScheduleTask,
    ScheduleUnassignedTask,
)
from app.schemas.constraint import ConstraintVersionPublish
from app.schemas.machine import SnapshotGenerateRequest
from app.schemas.schedule import BaselineScheduleRunRequest
from app.services.constraint_service import ConstraintService
from app.services.machine_service import MachineService
from app.services.schedule_service import ScheduleService


def seed_demo() -> None:
    db = SessionLocal()
    tag = datetime.now().strftime("%Y%m%d%H%M%S")
    today = date.today()

    try:
        # 1) machines: 8 (fabric line types)
        machines = []
        type_code = {"织机": "ZJ", "整经": "ZJG", "染色": "RS", "定型": "DX", "后整理": "HZL"}
        type_counter: dict[str, int] = {}
        for i in range(1, 9):
            status = "active"
            if i == 7:
                status = "inactive"
            if i == 8:
                status = "stopped"
            m_type = FABRIC_MACHINE_TYPES[(i - 1) % len(FABRIC_MACHINE_TYPES)]
            type_counter[m_type] = type_counter.get(m_type, 0) + 1
            serial = type_counter[m_type]
            code = f"{type_code.get(m_type, 'MT')}-{serial:02d}"
            m = Machine(
                machine_code=code,
                machine_name=f"{m_type}-{serial:02d}",
                machine_type=m_type,
                status=status,
                remark="fabric demo data",
                created_by="demo",
                updated_by="demo",
            )
            db.add(m)
            machines.append(m)
        db.commit()
        for m in machines:
            db.refresh(m)

        # 2) capability params: 8 current + 4 history
        for idx, m in enumerate(machines, start=1):
            speed = 260 + idx * 25  # m/hour
            db.add(
                MachineCapabilityParam(
                    machine_id=m.id,
                    speed_value=speed,
                    speed_unit="m/hour",
                    changeover_loss=0.5 + (idx % 3) * 0.3,
                    stability_score=95 - (idx % 5),
                    param_version="PV-FABRIC-001",
                    is_active=True,
                    created_by="demo",
                    updated_by="demo",
                )
            )
            if idx <= 4:
                db.add(
                    MachineCapabilityParam(
                        machine_id=m.id,
                        speed_value=speed - 40,
                        speed_unit="m/hour",
                        changeover_loss=1.2,
                        stability_score=88,
                        param_version="PV-FABRIC-HIS-001",
                        is_active=False,
                        created_by="demo",
                        updated_by="demo",
                    )
                )
        db.commit()

        # 3) shifts: 24 rows
        for m in machines:
            for s in ["A", "B", "C"]:
                db.add(
                    MachineShiftAvailability(
                        machine_id=m.id,
                        shift_code=s,
                        available_flag=True,
                        unavailable_reason=None,
                        effective_date=today,
                        created_by="demo",
                        updated_by="demo",
                    )
                )
        db.commit()

        # 4) snapshot
        snapshot_no = f"SNAP-FABRIC-{tag}"
        MachineService.generate_snapshot(db, SnapshotGenerateRequest(snapshot_no=snapshot_no, generated_by="demo"))

        # 5) constraint rules/items for fabric semantics (setup first, no deep implementation)
        rule_defs = [
            ("due_priority", "交期优先规则"),
            ("machine_limit", "机台限制"),
            ("changeover", "换型规则"),
            ("continuous_limit", "连续生产限制"),
            ("manual_lock", "人工锁定条件"),
        ]
        rule_rows = []
        for i, (rtype, name) in enumerate(rule_defs, start=1):
            row = ConstraintRule(
                rule_code=f"FABRIC-R{i:02d}-{tag}",
                rule_name=name,
                rule_type=rtype,
                status="active",
                description="fabric demo rule",
                created_by="demo",
                updated_by="demo",
            )
            db.add(row)
            rule_rows.append(row)
        db.commit()
        for r in rule_rows:
            db.refresh(r)

        rule_map = {r.rule_type: r for r in rule_rows}
        items = [
            (rule_map["due_priority"].id, "strict_due", "true"),
            (rule_map["due_priority"].id, "sort_by", "priority_then_due"),
            (rule_map["due_priority"].id, "timezone", "Asia/Shanghai"),
            (rule_map["machine_limit"].id, "allow_machine_types", "织机,整经,染色"),
            (rule_map["machine_limit"].id, "allowed_fabric_types", "平纹布,斜纹布,针织布"),
            (rule_map["machine_limit"].id, "require_active", "true"),
            (rule_map["changeover"].id, "same_color_continuous", "true"),
            (rule_map["changeover"].id, "color_change_loss", "0.8"),
            (rule_map["changeover"].id, "fabric_change_loss", "1.2"),
            (rule_map["continuous_limit"].id, "max_task_per_machine", "6"),
            (rule_map["continuous_limit"].id, "min_start_batch", "180"),
            (rule_map["continuous_limit"].id, "allow_overtime", "false"),
            (rule_map["manual_lock"].id, "lock_order_nos", f"ORD-{tag}-003,ORD-{tag}-011"),
            (rule_map["manual_lock"].id, "lock_reason", "quality_review"),
            (rule_map["manual_lock"].id, "enabled", "true"),
        ]
        for idx, (rule_id, key, value) in enumerate(items, start=1):
            db.add(
                ConstraintRuleItem(
                    rule_id=rule_id,
                    item_key=key,
                    item_value=value,
                    item_order=idx,
                    enabled_flag=True,
                    created_by="demo",
                    updated_by="demo",
                )
            )
        db.commit()

        draft_ver = ConstraintVersion(
            version_no=f"CV-FABRIC-DRAFT-{tag}",
            version_name="布匹约束草稿",
            version_status="draft",
            published_flag=False,
            remark="fabric demo draft",
            created_by="demo",
            updated_by="demo",
        )
        pub_ver = ConstraintVersion(
            version_no=f"CV-FABRIC-PUB-{tag}",
            version_name="布匹约束发布版",
            version_status="draft",
            published_flag=False,
            remark="fabric demo publish",
            created_by="demo",
            updated_by="demo",
        )
        db.add(draft_ver)
        db.add(pub_ver)
        for r in rule_rows:
            r.version_no = pub_ver.version_no
            r.updated_by = "demo"
        db.commit()
        db.refresh(draft_ver)
        db.refresh(pub_ver)

        ConstraintService.publish_version(db, pub_ver.version_no, ConstraintVersionPublish(published_by="demo"))

        # 6) orders/tasks: 30 with fabric semantics
        fabrics = ["平纹布", "斜纹布", "针织布", "牛津布", "弹力布"]
        colors = ["藏青", "黑色", "米白", "浅灰", "酒红", "军绿"]
        routes = ["整经>织机>染色>定型", "整经>织机>后整理", "织机>染色>定型>后整理"]

        for i in range(1, 31):
            due = today + timedelta(days=(i % 7) + 1)
            fabric = fabrics[i % len(fabrics)]
            color = colors[i % len(colors)]
            route = routes[i % len(routes)]
            qty_m = 300 + i * 45
            pri = 5 if i % 7 == 0 else 4 if i % 5 == 0 else 3 if i % 3 == 0 else 2
            order_no = f"ORD-{tag}-{i:03d}"

            db.add(
                ScheduleTask(
                    order_no=order_no,
                    fabric_type=fabric,
                    width_cm=150 + (i % 4) * 10,
                    gram_weight=180 + (i % 5) * 20,
                    color_code=color,
                    process_route=route,
                    order_quantity=float(qty_m),
                    quantity_unit="m",
                    due_date=due,
                    priority_level=pri,
                    task_status="pending",
                    created_by="demo",
                    updated_by="demo",
                )
            )
        db.commit()

        # 7) run baseline schedule
        plan_no = f"PLAN-FABRIC-{tag}"
        result = ScheduleService.run_baseline_schedule(
            db,
            BaselineScheduleRunRequest(
                plan_version_no=plan_no,
                plan_version_name="布匹基准计划演示",
                snapshot_version_no=snapshot_no,
                constraint_version_no=pub_ver.version_no,
                generated_by="demo",
            ),
        )

        plan = db.scalar(select(SchedulePlanVersion).where(SchedulePlanVersion.plan_version_no == plan_no))
        assigned_count = db.scalar(select(func.count(SchedulePlanItem.id)).where(SchedulePlanItem.plan_version_id == plan.id))
        unassigned_count = db.scalar(
            select(func.count(ScheduleUnassignedTask.id)).where(ScheduleUnassignedTask.plan_version_id == plan.id)
        )

        print("=== FABRIC DEMO SCHEDULING READY ===")
        print(f"snapshot_no: {snapshot_no}")
        print(f"constraint_draft: {draft_ver.version_no}")
        print(f"constraint_published: {pub_ver.version_no}")
        print(f"plan_version_no: {plan_no}")
        print(f"assigned_count: {assigned_count}")
        print(f"unassigned_count: {unassigned_count}")
        print(f"result: {result}")

    finally:
        db.close()


if __name__ == "__main__":
    seed_demo()
