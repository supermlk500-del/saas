import sys
sys.path.insert(0, r'c:\Users\kll\Desktop\织慧通\排产（2）')
from app.db.session import SessionLocal
from app.models import SchedulePlanItem, SchedulePlanVersion
from sqlalchemy import select

db = SessionLocal()
plans = list(db.scalars(select(SchedulePlanVersion).order_by(SchedulePlanVersion.id.desc()).limit(3)).all())
for p in plans:
    items = list(db.scalars(select(SchedulePlanItem).where(SchedulePlanItem.plan_version_id == p.id)).all())
    if not items:
        print(f"{p.plan_version_no}: 无 item")
        continue
    s = min(i.start_time for i in items)
    e = max(i.end_time for i in items)
    h = (e - s).total_seconds() / 3600
    print(f"{p.plan_version_no}: {len(items)} 条, {s} -> {e}  跨度 {h:.2f}h ({h/24:.2f}天)")

db.close()
