import sys
sys.path.insert(0, r'c:\Users\kll\Desktop\织慧通\排产（2）')
from app.db.session import SessionLocal
from app.models import SchedulePlanItem, SchedulePlanVersion, Machine
from sqlalchemy import select, func

db = SessionLocal()

base = db.scalar(
    select(SchedulePlanVersion)
    .where(SchedulePlanVersion.plan_version_no.like('%BASE%'))
    .order_by(SchedulePlanVersion.id.desc())
)
print(f"BASE 计划: {base.plan_version_no}, id={base.id}")

total_cnt = db.scalar(
    select(func.count(SchedulePlanItem.id))
    .where(SchedulePlanItem.plan_version_id == base.id)
)
print(f"共 {total_cnt} 条 item\n")

items10 = list(db.scalars(
    select(SchedulePlanItem)
    .where(SchedulePlanItem.plan_version_id == base.id)
    .order_by(SchedulePlanItem.start_time)
    .limit(10)
).all())

for it in items10:
    duration_h = (it.end_time - it.start_time).total_seconds() / 3600
    duration_min = (it.end_time - it.start_time).total_seconds() / 60
    m = db.get(Machine, it.machine_id)
    mname = m.machine_name if m else str(it.machine_id)
    print(f"  [{mname}] qty={it.planned_quantity}  {it.start_time} -> {it.end_time}  时长={duration_h:.3f}h ({duration_min:.1f}min)")

all_items = list(db.scalars(
    select(SchedulePlanItem)
    .where(SchedulePlanItem.plan_version_id == base.id)
).all())

min_start = min(i.start_time for i in all_items)
max_end   = max(i.end_time   for i in all_items)
span_h    = (max_end - min_start).total_seconds() / 3600
machine_set = {i.machine_id for i in all_items}
avg_h = sum((i.end_time - i.start_time).total_seconds() / 3600 for i in all_items) / len(all_items)

print()
print(f"全局起点: {min_start}")
print(f"全局终点: {max_end}")
print(f"总跨度:   {span_h:.2f} 小时 = {span_h/24:.2f} 天")
print(f"涉及机台: {len(machine_set)} 台")
print(f"平均每条: {avg_h:.4f} 小时 = {avg_h*60:.2f} 分钟")
db.close()
