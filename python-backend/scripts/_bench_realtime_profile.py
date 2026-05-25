"""带 cProfile 的性能剖析：定位插单耗时分布"""
from pathlib import Path
import sys
ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

import cProfile
import pstats
import time
from io import StringIO
from sqlalchemy import select

from app.db.session import SessionLocal
from app.models import (
    SchedulePlanItem,
    SchedulePlanVersion,
    ScheduleTask,
    ScheduleUnassignedTask,
)
from app.schemas.schedule import RealtimeRescheduleRequest
from app.services.schedule_service import ScheduleService


def cleanup(db, plan_no_prefix: str, base_id: int):
    plans = list(db.scalars(select(SchedulePlanVersion).where(SchedulePlanVersion.plan_version_no.like(f"{plan_no_prefix}%"))).all())
    for p in plans:
        db.query(SchedulePlanItem).filter(SchedulePlanItem.plan_version_id == p.id).delete()
        db.query(ScheduleUnassignedTask).filter(ScheduleUnassignedTask.plan_version_id == p.id).delete()
        db.query(SchedulePlanVersion).filter(SchedulePlanVersion.id == p.id).delete()
    db.commit()
    db.query(ScheduleTask).filter(
        ScheduleTask.id.in_(
            select(SchedulePlanItem.task_id).where(SchedulePlanItem.plan_version_id == base_id)
        )
    ).update({"task_status": "scheduled"}, synchronize_session=False)
    db.query(ScheduleTask).filter(
        ~ScheduleTask.id.in_(
            select(SchedulePlanItem.task_id).where(SchedulePlanItem.plan_version_id == base_id)
        )
    ).update({"task_status": "pending"}, synchronize_session=False)
    db.commit()


def main():
    db = SessionLocal()
    base = db.scalar(select(SchedulePlanVersion).where(SchedulePlanVersion.plan_version_no == "PLAN-LIVE-BASE-20260506"))

    suffix = int(time.time() * 1000) % 1000000

    pr = cProfile.Profile()
    pr.enable()
    ScheduleService.run_realtime_reschedule(
        db,
        RealtimeRescheduleRequest(
            source_plan_version_id=base.id,
            new_plan_version_no=f"PLAN-PROFILE-{suffix}",
            new_plan_version_name=f"profile-{suffix}",
            freeze_minutes=30,
            generated_by="bench",
        ),
    )
    pr.disable()

    buf = StringIO()
    stats = pstats.Stats(pr, stream=buf).sort_stats("cumulative")
    stats.print_stats(40)
    print(buf.getvalue())

    cleanup(db, "PLAN-PROFILE-", base.id)
    db.close()


if __name__ == "__main__":
    main()
