"""列出所有机台和订单编号前缀的真实含义"""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from sqlalchemy import select
from app.db.session import SessionLocal
from app.models import Machine, ScheduleTask

db = SessionLocal()

print("=== Machine 编号前缀和 machine_type ===")
seen = set()
for m in db.scalars(select(Machine).order_by(Machine.machine_code)).all():
    prefix = m.machine_code.split("-")[0]
    key = (prefix, m.machine_type)
    if key not in seen:
        seen.add(key)
        print(f"  {prefix}-XX  ->  type={m.machine_type}, ex:{m.machine_code}, name:{m.machine_name}")

print()
print("=== 订单号前缀 ===")
seen2 = set()
for t in db.scalars(select(ScheduleTask)).all():
    no = t.order_no
    if not no:
        continue
    parts = no.split("-")
    prefix = "-".join(parts[:2]) if len(parts) >= 2 else parts[0]
    if prefix not in seen2:
        seen2.add(prefix)
        cust = getattr(t, "customer_priority", "") or "-"
        fab = getattr(t, "fabric_type", "") or "-"
        print(f"  {prefix}-XXX  ex:{no}  fabric:{fab}  cust_prio:{cust}")

db.close()
