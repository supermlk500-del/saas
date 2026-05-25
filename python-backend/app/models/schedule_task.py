from datetime import date, datetime

from sqlalchemy import Date, DateTime, Float, Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class ScheduleTask(Base):
    __tablename__ = "schedule_task"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)

    order_no: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    fabric_type: Mapped[str] = mapped_column(String(64), index=True)
    width_cm: Mapped[float | None] = mapped_column(Float, nullable=True)
    gram_weight: Mapped[float | None] = mapped_column(Float, nullable=True)
    color_code: Mapped[str | None] = mapped_column(String(64), nullable=True)
    process_route: Mapped[str | None] = mapped_column(String(128), nullable=True)
    order_quantity: Mapped[float] = mapped_column(Float)
    quantity_unit: Mapped[str] = mapped_column(String(16), default="m")
    due_urgency_level: Mapped[str] = mapped_column(String(16), default="P2")
    customer_priority_level: Mapped[str] = mapped_column(String(16), default="P2")

    due_date: Mapped[date] = mapped_column(Date, index=True)
    priority_level: Mapped[int] = mapped_column(Integer, default=1)
    task_status: Mapped[str] = mapped_column(String(32), default="pending")
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    created_by: Mapped[str] = mapped_column(String(64), default="system")
    updated_by: Mapped[str] = mapped_column(String(64), default="system")
