import sys
sys.path.insert(0, r'c:\Users\kll\Desktop\织慧通\排产（2）')
from app.db.session import SessionLocal
from app.models import ScheduleTask, SchedulePlanItem, SchedulePlanVersion, MachineCapabilitySnapshot, MachineCapabilityParam
from sqlalchemy import select

db = SessionLocal()

print("=== ScheduleTask 表（10条 BASE 订单）===")
tasks = list(db.scalars(select(ScheduleTask).where(ScheduleTask.order_no.like("ORD-BASE-%")).order_by(ScheduleTask.id).limit(10)).all())
for t in tasks:
    print(f"  {t.order_no}  order_quantity={t.order_quantity}  unit={t.quantity_unit}")

print()
print("=== SchedulePlanItem 表（同样这10条对应的 plan item）===")
task_ids = [t.id for t in tasks]
items = list(db.scalars(select(SchedulePlanItem).where(SchedulePlanItem.task_id.in_(task_ids))).all())
for it in items:
    t = db.get(ScheduleTask, it.task_id)
    duration_h = (it.end_time - it.start_time).total_seconds() / 3600
    print(f"  task_id={it.task_id} ({t.order_no})  planned_qty={it.planned_quantity}  时长={duration_h:.3f}h  ({duration_h*60:.1f}min)")

print()
print("=== MachineCapabilitySnapshot 表（前10台机台快照）===")
snaps = list(db.scalars(select(MachineCapabilitySnapshot).order_by(MachineCapabilitySnapshot.id).limit(10)).all())
for s in snaps:
    print(f"  machine_id={s.machine_id}  speed={s.speed_value}  unit={getattr(s, 'speed_unit', '?')}  changeover_loss={s.changeover_loss}  available={s.available_flag}")

print()
print("=== MachineCapabilityParam 表（前10条）===")
params = list(db.scalars(select(MachineCapabilityParam).order_by(MachineCapabilityParam.id).limit(10)).all())
for p in params:
    print(f"  machine_id={p.machine_id}  speed={p.speed_value}  unit={p.speed_unit}  changeover_loss={p.changeover_loss}  active={p.is_active}")

db.close()
