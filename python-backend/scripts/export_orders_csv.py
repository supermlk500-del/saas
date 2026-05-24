from __future__ import annotations

import csv
from datetime import date
from pathlib import Path
import sys

from sqlalchemy import select

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from app.db.session import SessionLocal
from app.models import ScheduleTask


def export_orders() -> None:
    db = SessionLocal()
    try:
        tasks = list(db.scalars(select(ScheduleTask).order_by(ScheduleTask.id)).all())
        out_dir = ROOT / "数据"
        out_dir.mkdir(exist_ok=True)
        file_path = out_dir / f"布匹订单导出_{date.today().isoformat()}.csv"

        with file_path.open("w", newline="", encoding="utf-8-sig") as f:
            writer = csv.writer(f)
            writer.writerow(["订单号", "布种", "幅宽(cm)", "克重(gsm)", "颜色", "工艺路线", "数量", "单位", "交期", "优先级"])
            for t in tasks:
                writer.writerow(
                    [
                        t.order_no,
                        t.fabric_type,
                        t.width_cm or "",
                        t.gram_weight or "",
                        t.color_code or "",
                        t.process_route or "",
                        t.order_quantity,
                        t.quantity_unit or "m",
                        t.due_date,
                        t.priority_level,
                    ]
                )

        print(f"export_file: {file_path}")
        print(f"order_count: {len(tasks)}")
    finally:
        db.close()


if __name__ == "__main__":
    export_orders()
