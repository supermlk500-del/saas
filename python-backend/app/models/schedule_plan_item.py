from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class SchedulePlanItem(Base):
    __tablename__ = "schedule_plan_item"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    plan_version_id: Mapped[int] = mapped_column(ForeignKey("schedule_plan_version.id"), index=True)
    task_id: Mapped[int] = mapped_column(ForeignKey("schedule_task.id"), index=True)
    machine_id: Mapped[int] = mapped_column(ForeignKey("machine.id"), index=True)
    start_time: Mapped[datetime] = mapped_column(DateTime)
    end_time: Mapped[datetime] = mapped_column(DateTime)
    planned_quantity: Mapped[int] = mapped_column(Integer)
    item_status: Mapped[str] = mapped_column(String(32), default="planned")
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
