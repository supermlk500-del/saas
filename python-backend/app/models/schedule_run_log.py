from datetime import datetime

from sqlalchemy import DateTime, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class ScheduleRunLog(Base):
    __tablename__ = "schedule_run_log"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    log_type: Mapped[str] = mapped_column(String(64), index=True)
    business_no: Mapped[str] = mapped_column(String(64), index=True)
    operation_name: Mapped[str] = mapped_column(String(128))
    operation_result: Mapped[str] = mapped_column(String(32))
    error_message: Mapped[str | None] = mapped_column(Text(), nullable=True)
    operator: Mapped[str] = mapped_column(String(64), default="system")
    operation_time: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
