from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class ScheduleUnassignedTask(Base):
    __tablename__ = "schedule_unassigned_task"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    plan_version_id: Mapped[int] = mapped_column(ForeignKey("schedule_plan_version.id"), index=True)
    task_id: Mapped[int] = mapped_column(ForeignKey("schedule_task.id"), index=True)
    reason_code: Mapped[str] = mapped_column(String(64))
    reason_desc: Mapped[str] = mapped_column(Text())
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    created_by: Mapped[str] = mapped_column(String(64), default="system")
