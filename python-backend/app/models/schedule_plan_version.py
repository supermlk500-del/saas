from datetime import datetime

from sqlalchemy import DateTime, String
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class SchedulePlanVersion(Base):
    __tablename__ = "schedule_plan_version"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    plan_version_no: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    plan_version_name: Mapped[str] = mapped_column(String(128))
    plan_status: Mapped[str] = mapped_column(String(32), default="draft")
    snapshot_version_no: Mapped[str] = mapped_column(String(64), index=True)
    constraint_version_no: Mapped[str] = mapped_column(String(64), index=True)
    generated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    generated_by: Mapped[str] = mapped_column(String(64), default="system")
    published_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    published_by: Mapped[str | None] = mapped_column(String(64), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
