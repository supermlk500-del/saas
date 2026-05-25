from __future__ import annotations

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


def reset_fabric_demo_data() -> None:
    db = SessionLocal()
    try:
        stat = {}

        # 1) plan and dependent rows (fabric demo plans)
        plan_ids = list(
            db.scalars(select(SchedulePlanVersion.id).where(SchedulePlanVersion.plan_version_no.like("PLAN-FABRIC-%"))).all()
        )
        if plan_ids:
            stat["schedule_plan_item"] = db.execute(
                delete(SchedulePlanItem).where(SchedulePlanItem.plan_version_id.in_(plan_ids))
            ).rowcount or 0
            stat["schedule_unassigned_task"] = db.execute(
                delete(ScheduleUnassignedTask).where(ScheduleUnassignedTask.plan_version_id.in_(plan_ids))
            ).rowcount or 0
        else:
            stat["schedule_plan_item"] = 0
            stat["schedule_unassigned_task"] = 0

        stat["schedule_plan_version"] = db.execute(
            delete(SchedulePlanVersion).where(SchedulePlanVersion.plan_version_no.like("PLAN-FABRIC-%"))
        ).rowcount or 0

        # 2) delete demo orders and any plan refs by task
        task_ids = list(
            db.scalars(
                select(ScheduleTask.id).where(
                    (ScheduleTask.created_by == "demo")
                    | (ScheduleTask.order_no.like("ORD-%"))
                )
            ).all()
        )
        if task_ids:
            stat["schedule_plan_item_by_task"] = db.execute(
                delete(SchedulePlanItem).where(SchedulePlanItem.task_id.in_(task_ids))
            ).rowcount or 0
            stat["schedule_unassigned_task_by_task"] = db.execute(
                delete(ScheduleUnassignedTask).where(ScheduleUnassignedTask.task_id.in_(task_ids))
            ).rowcount or 0
        else:
            stat["schedule_plan_item_by_task"] = 0
            stat["schedule_unassigned_task_by_task"] = 0

        stat["schedule_task"] = db.execute(
            delete(ScheduleTask).where(
                (ScheduleTask.created_by == "demo")
                | (ScheduleTask.order_no.like("ORD-%"))
            )
        ).rowcount or 0

        # 3) snapshots and machines
        machine_ids = list(
            db.scalars(select(Machine.id).where(Machine.created_by == "demo")).all()
        )
        if machine_ids:
            stat["machine_capability_snapshot_by_machine"] = db.execute(
                delete(MachineCapabilitySnapshot).where(MachineCapabilitySnapshot.machine_id.in_(machine_ids))
            ).rowcount or 0
            stat["machine_capability_param"] = db.execute(
                delete(MachineCapabilityParam).where(MachineCapabilityParam.machine_id.in_(machine_ids))
            ).rowcount or 0
            stat["machine_shift_availability"] = db.execute(
                delete(MachineShiftAvailability).where(MachineShiftAvailability.machine_id.in_(machine_ids))
            ).rowcount or 0
        else:
            stat["machine_capability_snapshot_by_machine"] = 0
            stat["machine_capability_param"] = 0
            stat["machine_shift_availability"] = 0

        stat["machine_capability_snapshot_by_no"] = db.execute(
            delete(MachineCapabilitySnapshot).where(MachineCapabilitySnapshot.snapshot_no.like("SNAP-FABRIC-%"))
        ).rowcount or 0

        stat["machine"] = db.execute(delete(Machine).where(Machine.created_by == "demo")).rowcount or 0

        # 4) constraints
        rule_ids = list(db.scalars(select(ConstraintRule.id).where(ConstraintRule.rule_code.like("FABRIC-R%"))).all())
        if rule_ids:
            stat["constraint_rule_item"] = db.execute(
                delete(ConstraintRuleItem).where(ConstraintRuleItem.rule_id.in_(rule_ids))
            ).rowcount or 0
        else:
            stat["constraint_rule_item"] = 0

        stat["constraint_rule"] = db.execute(
            delete(ConstraintRule).where(ConstraintRule.rule_code.like("FABRIC-R%"))
        ).rowcount or 0

        stat["constraint_version"] = db.execute(
            delete(ConstraintVersion).where(ConstraintVersion.version_no.like("CV-FABRIC-%"))
        ).rowcount or 0

        # 5) logs
        stat["schedule_run_log"] = db.execute(
            delete(ScheduleRunLog).where(
                ScheduleRunLog.business_no.like("SNAP-FABRIC-%")
                | ScheduleRunLog.business_no.like("CV-FABRIC-%")
                | ScheduleRunLog.business_no.like("PLAN-FABRIC-%")
            )
        ).rowcount or 0

        db.commit()

        total = sum(int(v) for v in stat.values())
        print("=== RESET FABRIC DEMO DATA DONE ===")
        for k, v in stat.items():
            print(f"{k}: {v}")
        print(f"total_deleted: {total}")

    except Exception:
        db.rollback()
        raise
    finally:
        db.close()


if __name__ == "__main__":
    reset_fabric_demo_data()
