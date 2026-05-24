"""阶段二验证脚本：检查硬/软约束分类与软警告日志写入"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from sqlalchemy import select, desc
from app.db.session import SessionLocal
from app.models import ConstraintRule, ScheduleRunLog, SchedulePlanVersion, ScheduleUnassignedTask


def main():
    db = SessionLocal()
    try:
        print("=" * 70)
        print("[1] 约束规则硬/软分类（数据库实际值）")
        print("=" * 70)
        rules = list(
            db.scalars(
                select(ConstraintRule)
                .where(ConstraintRule.status == "active")
                .order_by(ConstraintRule.priority_level)
            ).all()
        )
        for r in rules:
            tag = "硬约束" if r.is_hard_constraint else "软约束"
            print(f"  - {r.rule_code:20s} {r.rule_type:18s} | {tag} | P{r.priority_level} | {r.rule_name}")

        print()
        print("=" * 70)
        print("[2] 各计划版本汇总（unassigned 是硬约束阻断 / soft_warning 是软约束受损）")
        print("=" * 70)
        plans = list(db.scalars(select(SchedulePlanVersion).order_by(desc(SchedulePlanVersion.id))).all())
        for p in plans:
            unassigned_cnt = len(
                list(db.scalars(select(ScheduleUnassignedTask).where(ScheduleUnassignedTask.plan_version_id == p.id)).all())
            )
            soft_logs = list(
                db.scalars(
                    select(ScheduleRunLog)
                    .where(ScheduleRunLog.business_no == p.plan_version_no)
                    .where(ScheduleRunLog.log_type == "soft_constraint_warning")
                ).all()
            )
            print(f"  - {p.plan_version_no:30s} unassigned={unassigned_cnt:2d}  soft_warning_log={len(soft_logs):2d}")

        print()
        print("=" * 70)
        print("[3] 最近一条软约束警告日志详情（前 600 字）")
        print("=" * 70)
        latest = db.scalar(
            select(ScheduleRunLog)
            .where(ScheduleRunLog.log_type == "soft_constraint_warning")
            .order_by(desc(ScheduleRunLog.id))
        )
        if latest:
            print(f"  business_no : {latest.business_no}")
            print(f"  operation   : {latest.operation_name}")
            print(f"  result      : {latest.operation_result}")
            print(f"  operator    : {latest.operator}")
            print(f"  time        : {latest.operation_time}")
            print(f"  detail      : {(latest.error_message or '')[:600]}")
        else:
            print("  [WARN] 暂无软约束警告日志")

        print()
        print("=" * 70)
        print("[OK] 阶段二验证完成")
        print("=" * 70)
    finally:
        db.close()


if __name__ == "__main__":
    main()
