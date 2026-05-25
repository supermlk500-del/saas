from datetime import datetime

from sqlalchemy import Boolean, DateTime, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class ConstraintVersion(Base):
    __tablename__ = "constraint_version"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    version_no: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    version_name: Mapped[str] = mapped_column(String(128))
    version_status: Mapped[str] = mapped_column(String(32), default="draft")
    published_flag: Mapped[bool] = mapped_column(Boolean, default=False)
    published_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    published_by: Mapped[str | None] = mapped_column(String(64), nullable=True)
    remark: Mapped[str | None] = mapped_column(Text(), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    created_by: Mapped[str] = mapped_column(String(64), default="system")
    updated_by: Mapped[str] = mapped_column(String(64), default="system")
