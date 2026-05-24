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


DEMO_PREFIXES = {
    "machine_code": "DEMO-%",
    "snapshot_no": "SNAP-DEMO-%",
    "constraint_version_no": "CV-DEMO-%",
    "rule_code": "DEMO-R%",
    "plan_version_no": "PLAN-DEMO-%",
    "order_no": "DEMO-%",
}


def cleanup_old_demo() -> None:
    db = SessionLocal()
    try:
        deleted: dict[str, int] = {}

        # 1) plans + related
        plan_ids = list(
            db.scalars(
                select(SchedulePlanVersion.id).where(SchedulePlanVersion.plan_version_no.like(DEMO_PREFIXES["plan_version_no"]))
            ).all()
        )
        if plan_ids:
            deleted["schedule_plan_item"] = db.execute(
                delete(SchedulePlanItem).where(SchedulePlanItem.plan_version_id.in_(plan_ids))
            ).rowcount or 0
            deleted["schedule_unassigned_task"] = db.execute(
                delete(ScheduleUnassignedTask).where(ScheduleUnassignedTask.plan_version_id.in_(plan_ids))
            ).rowcount or 0
        else:
            deleted["schedule_plan_item"] = 0
            deleted["schedule_unassigned_task"] = 0

        deleted["schedule_plan_version"] = db.execute(
            delete(SchedulePlanVersion).where(SchedulePlanVersion.plan_version_no.like(DEMO_PREFIXES["plan_version_no"]))
        ).rowcount or 0

        # 2) tasks + cross-plan references
        demo_task_ids = list(
            db.scalars(
                select(ScheduleTask.id).where(
                    ScheduleTask.order_no.like(DEMO_PREFIXES["order_no"])
                )
            ).all()
        )
        if demo_task_ids:
            deleted["schedule_plan_item_by_task"] = db.execute(
                delete(SchedulePlanItem).where(SchedulePlanItem.task_id.in_(demo_task_ids))
            ).rowcount or 0
            deleted["schedule_unassigned_task_by_task"] = db.execute(
                delete(ScheduleUnassignedTask).where(ScheduleUnassignedTask.task_id.in_(demo_task_ids))
            ).rowcount or 0
        else:
            deleted["schedule_plan_item_by_task"] = 0
            deleted["schedule_unassigned_task_by_task"] = 0

        deleted["schedule_task"] = db.execute(
            delete(ScheduleTask).where(
                ScheduleTask.order_no.like(DEMO_PREFIXES["order_no"])
            )
        ).rowcount or 0

        # 3) machines + related
        machine_ids = list(
            db.scalars(select(Machine.id).where(Machine.machine_code.like(DEMO_PREFIXES["machine_code"]))).all()
        )
        if machine_ids:
            deleted["machine_capability_snapshot_by_machine"] = db.execute(
                delete(MachineCapabilitySnapshot).where(MachineCapabilitySnapshot.machine_id.in_(machine_ids))
            ).rowcount or 0
            deleted["machine_capability_param"] = db.execute(
                delete(MachineCapabilityParam).where(MachineCapabilityParam.machine_id.in_(machine_ids))
            ).rowcount or 0
            deleted["machine_shift_availability"] = db.execute(
                delete(MachineShiftAvailability).where(MachineShiftAvailability.machine_id.in_(machine_ids))
            ).rowcount or 0
        else:
            deleted["machine_capability_snapshot_by_machine"] = 0
            deleted["machine_capability_param"] = 0
            deleted["machine_shift_availability"] = 0

        # safe extra: any snapshot rows by DEMO snapshot no
        deleted["machine_capability_snapshot_by_no"] = db.execute(
            delete(MachineCapabilitySnapshot).where(MachineCapabilitySnapshot.snapshot_no.like(DEMO_PREFIXES["snapshot_no"]))
        ).rowcount or 0

        deleted["machine"] = db.execute(
            delete(Machine).where(Machine.machine_code.like(DEMO_PREFIXES["machine_code"]))
        ).rowcount or 0

        # 4) constraints + related
        rule_ids = list(db.scalars(select(ConstraintRule.id).where(ConstraintRule.rule_code.like(DEMO_PREFIXES["rule_code"]))).all())
        if rule_ids:
            deleted["constraint_rule_item"] = db.execute(
                delete(ConstraintRuleItem).where(ConstraintRuleItem.rule_id.in_(rule_ids))
            ).rowcount or 0
        else:
            deleted["constraint_rule_item"] = 0

        deleted["constraint_rule"] = db.execute(
            delete(ConstraintRule).where(ConstraintRule.rule_code.like(DEMO_PREFIXES["rule_code"]))
        ).rowcount or 0

        deleted["constraint_version"] = db.execute(
            delete(ConstraintVersion).where(ConstraintVersion.version_no.like(DEMO_PREFIXES["constraint_version_no"]))
        ).rowcount or 0

        # 5) logs
        deleted["schedule_run_log"] = db.execute(
            delete(ScheduleRunLog).where(
                ScheduleRunLog.business_no.like("DEMO-%")
                | ScheduleRunLog.business_no.like("SNAP-DEMO-%")
                | ScheduleRunLog.business_no.like("CV-DEMO-%")
                | ScheduleRunLog.business_no.like("PLAN-DEMO-%")
            )
        ).rowcount or 0

        db.commit()

        print("=== CLEANUP OLD DEMO DONE ===")
        total = 0
        for k, v in deleted.items():
            total += int(v)
            print(f"{k}: {v}")
        print(f"total_deleted: {total}")

    except Exception:
        db.rollback()
        raise
    finally:
        db.close()


if __name__ == "__main__":
    cleanup_old_demo()
