import sys
sys.path.insert(0, r'c:\Users\kll\Desktop\织慧通\排产（2）')
from app.db.session import SessionLocal
from app.models import Machine, ScheduleTask, MachineCapabilityParam
from sqlalchemy import select, func

db = SessionLocal()

# 总机台数
total_m = db.scalar(select(func.count(Machine.id)))
print(f"机台总数: {total_m}")

# 按 machine_type 分组
print()
print("机台按类型分布:")
types = db.execute(select(Machine.machine_type, func.count(Machine.id)).group_by(Machine.machine_type)).all()
for t, c in types:
    print(f"  {t}: {c} 台")

print()
print("前 10 台机台名:")
machines = list(db.scalars(select(Machine).order_by(Machine.id).limit(10)).all())
for m in machines:
    print(f"  id={m.id}  code={m.machine_code}  name={m.machine_name}  type={m.machine_type}  status={m.status}  remark={m.remark}")

print()
print("机台 created_by 分布:")
creators = db.execute(select(Machine.created_by, func.count(Machine.id)).group_by(Machine.created_by)).all()
for cb, c in creators:
    print(f"  {cb}: {c}")

print()
print("订单总数: ", db.scalar(select(func.count(ScheduleTask.id))))
print("订单 created_by 分布:")
creators = db.execute(select(ScheduleTask.created_by, func.count(ScheduleTask.id)).group_by(ScheduleTask.created_by)).all()
for cb, c in creators:
    print(f"  {cb}: {c}")

# 检查 BASE 订单实际创建时间
print()
print("BASE 订单 ID 范围:")
ids = db.execute(select(func.min(ScheduleTask.id), func.max(ScheduleTask.id)).where(ScheduleTask.order_no.like("ORD-BASE-%"))).first()
print(f"  min={ids[0]}  max={ids[1]}")
print("INSERT 订单 ID 范围:")
ids = db.execute(select(func.min(ScheduleTask.id), func.max(ScheduleTask.id)).where(ScheduleTask.order_no.like("ORD-INSERT-%"))).first()
print(f"  min={ids[0]}  max={ids[1]}")
print("COMP 订单 ID 范围:")
ids = db.execute(select(func.min(ScheduleTask.id), func.max(ScheduleTask.id)).where(ScheduleTask.order_no.like("ORD-COMP-%"))).first()
print(f"  min={ids[0]}  max={ids[1]}")

db.close()
