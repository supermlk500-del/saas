"""临时性能测试：测量插单（实时重排）的真实耗时，
读、写各阶段分别打点，方便定位瓶颈。
"""
from pathlib import Path
import sys
ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

import time
from sqlalchemy import select, func

from app.db.session import SessionLocal
from app.models import (
    Machine,
    MachineCapabilitySnapshot,
    SchedulePlanItem,
    SchedulePlanVersion,
    ScheduleTask,
    ScheduleUnassignedTask,
)
from app.schemas.schedule import RealtimeRescheduleRequest
from app.services.schedule_service import ScheduleService


def main(repeat: int = 3) -> None:
    print("=" * 70)
    print("插单（实时重排）性能基线 — 直接调用 service 层")
    print("=" * 70)

    db = SessionLocal()

    base = db.scalar(select(SchedulePlanVersion).where(SchedulePlanVersion.plan_version_no == "PLAN-LIVE-BASE-20260506"))
    if base is None:
        print("没找到基线计划 PLAN-LIVE-BASE-20260506，先跑 reset_and_seed_live_demo.py")
        return

    base_items_cnt = db.scalar(select(func.count(SchedulePlanItem.id)).where(SchedulePlanItem.plan_version_id == base.id))
    pending_cnt = db.scalar(select(func.count(ScheduleTask.id)).where(ScheduleTask.task_status == "pending"))
    active_machine_cnt = db.scalar(select(func.count(Machine.id)).where(Machine.status == "active"))
    snapshot_cnt = db.scalar(
        select(func.count(MachineCapabilitySnapshot.id)).where(
            MachineCapabilitySnapshot.snapshot_no == base.snapshot_version_no
        )
    )

    print(f"基线计划      : {base.plan_version_no}")
    print(f"  - 已排明细  : {base_items_cnt} 条")
    print(f"  - 待排订单  : {pending_cnt} 条 (插单候选)")
    print(f"  - 活跃机台  : {active_machine_cnt} 台")
    print(f"  - 快照机台  : {snapshot_cnt} 台")
    print()

    durations = []
    for i in range(repeat):
        suffix = int(time.time() * 1000) % 1000000
        new_no = f"PLAN-BENCH-{suffix}"

        t0 = time.perf_counter()
        result = ScheduleService.run_realtime_reschedule(
            db,
            RealtimeRescheduleRequest(
                source_plan_version_id=base.id,
                new_plan_version_no=new_no,
                new_plan_version_name=f"性能测试-{suffix}",
                freeze_minutes=30,
                generated_by="bench",
            ),
        )
        t1 = time.perf_counter()
        elapsed_ms = (t1 - t0) * 1000
        durations.append(elapsed_ms)

        new_plan = db.scalar(select(SchedulePlanVersion).where(SchedulePlanVersion.plan_version_no == new_no))
        new_items_cnt = db.scalar(select(func.count(SchedulePlanItem.id)).where(SchedulePlanItem.plan_version_id == new_plan.id))
        unassigned_cnt = db.scalar(
            select(func.count(ScheduleUnassignedTask.id)).where(ScheduleUnassignedTask.plan_version_id == new_plan.id)
        )

        print(
            f"第{i+1}次  耗时={elapsed_ms:7.1f} ms  "
            f"已排={result['assigned_count']}  锁定={result.get('locked_count', 0)}  "
            f"未排={result['unassigned_count']}  新计划明细={new_items_cnt}  未排清单={unassigned_cnt}"
        )

        # 清理本次测试产生的计划，否则后续测试影响互相
        db.query(SchedulePlanItem).filter(SchedulePlanItem.plan_version_id == new_plan.id).delete()
        db.query(ScheduleUnassignedTask).filter(ScheduleUnassignedTask.plan_version_id == new_plan.id).delete()
        db.query(SchedulePlanVersion).filter(SchedulePlanVersion.id == new_plan.id).delete()
        db.commit()

        # 把任务状态恢复成 pending（实时重排会把它们改成 scheduled）
        db.query(ScheduleTask).filter(
            ScheduleTask.id.in_(
                select(SchedulePlanItem.task_id).where(SchedulePlanItem.plan_version_id == base.id)
            )
        ).update({"task_status": "scheduled"}, synchronize_session=False)
        # 待排订单复位
        db.query(ScheduleTask).filter(
            ~ScheduleTask.id.in_(
                select(SchedulePlanItem.task_id).where(SchedulePlanItem.plan_version_id == base.id)
            )
        ).update({"task_status": "pending"}, synchronize_session=False)
        db.commit()

    print()
    if durations:
        avg = sum(durations) / len(durations)
        mn = min(durations)
        mx = max(durations)
        print(f"汇总：min={mn:.1f}ms  avg={avg:.1f}ms  max={mx:.1f}ms")

    db.close()


if __name__ == "__main__":
    main(repeat=3)
