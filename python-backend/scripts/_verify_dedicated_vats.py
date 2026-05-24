"""验证固定专用缸（黑缸/白缸/藏蓝缸/大红缸）的实际排产纯度。

固定缸应该永远只染其专属颜色，不发生换色。
"""
from __future__ import annotations

from pathlib import Path
import sys
from collections import defaultdict

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from sqlalchemy import desc, select
from app.db.session import SessionLocal
from app.models import (
    Machine,
    SchedulePlanItem,
    SchedulePlanVersion,
    ScheduleTask,
)


DEDICATED_VAT_MAP = {
    "DY-01": "BLACK",
    "DY-02": "WHITE",
    "DY-03": "NAVY",
    "DY-04": "RED",
}


def main() -> None:
    db = SessionLocal()
    try:
        plan = db.scalar(
            select(SchedulePlanVersion)
            .where(SchedulePlanVersion.plan_version_no.like("PLAN-LIVE-BASE-%"))
            .order_by(desc(SchedulePlanVersion.id))
            .limit(1)
        )
        if plan is None:
            print("[ERROR] 未找到 PLAN-LIVE-BASE-* 计划版本")
            return

        print(f"分析计划版本：{plan.plan_version_no} (id={plan.id})")
        print(f"snapshot: {plan.snapshot_version_no}")
        print(f"constraint: {plan.constraint_version_no}")
        print()

        rows = list(
            db.execute(
                select(
                    SchedulePlanItem,
                    ScheduleTask.color_code,
                    ScheduleTask.order_no,
                    Machine.machine_code,
                )
                .join(ScheduleTask, ScheduleTask.id == SchedulePlanItem.task_id)
                .join(Machine, Machine.id == SchedulePlanItem.machine_id)
                .where(SchedulePlanItem.plan_version_id == plan.id)
                .order_by(Machine.machine_code, SchedulePlanItem.start_time)
            ).all()
        )

        per_machine: dict[str, list[tuple[str, str]]] = defaultdict(list)
        for _item, color_code, order_no, machine_code in rows:
            per_machine[machine_code].append((order_no, color_code or ""))

        print("=" * 70)
        print("固定专用缸纯度检查")
        print("=" * 70)

        all_pure = True
        for machine_code, expected_color in DEDICATED_VAT_MAP.items():
            tasks = per_machine.get(machine_code, [])
            colors = [c for _, c in tasks]
            unique_colors = set(colors)
            is_pure = (not tasks) or unique_colors == {expected_color}

            status = "✅ 纯净" if is_pure else "❌ 混色"
            print(f"\n{machine_code} (应染 {expected_color}):  {status}")
            print(f"  本机台共排 {len(tasks)} 单")
            if tasks:
                print(f"  颜色分布: {dict((c, colors.count(c)) for c in unique_colors)}")
                print(f"  前 5 单订单号: {[order for order, _ in tasks[:5]]}")
            if not is_pure:
                all_pure = False
                wrong_orders = [
                    (order, color) for order, color in tasks if color != expected_color
                ]
                print(f"  ⚠️  混入的非专属颜色订单:")
                for order, color in wrong_orders[:5]:
                    print(f"     - {order}: {color}")

        print("\n" + "=" * 70)
        if all_pure:
            print("✅ 全部 4 个固定缸纯净，没有发生换色")
        else:
            print("❌ 存在固定缸被混入非专属颜色的情况")
        print("=" * 70)

        print("\n[补充] 通用循环缸样本（前 3 台 DYE 类型非固定缸）:")
        print("-" * 70)
        dye_circulating = [
            (code, tasks)
            for code, tasks in per_machine.items()
            if code.startswith("DY-") and code not in DEDICATED_VAT_MAP
        ][:3]
        for machine_code, tasks in dye_circulating:
            colors = [c for _, c in tasks]
            unique = set(colors)
            print(f"\n{machine_code} (循环缸):  共 {len(tasks)} 单，{len(unique)} 种颜色")
            print(f"  颜色序列: {' -> '.join(colors[:8])}{'...' if len(colors) > 8 else ''}")

    finally:
        db.close()


if __name__ == "__main__":
    main()
