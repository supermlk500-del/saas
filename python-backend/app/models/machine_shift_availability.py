from datetime import date, datetime

from sqlalchemy import Boolean, Date, DateTime, ForeignKey, String, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base


class MachineShiftAvailability(Base):
    __tablename__ = "machine_shift_availability"
    __table_args__ = (
        UniqueConstraint("machine_id", "shift_code", "effective_date", name="uk_machine_shift_date"),
    )

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    machine_id: Mapped[int] = mapped_column(ForeignKey("machine.id"), index=True)
    shift_code: Mapped[str] = mapped_column(String(32))
    available_flag: Mapped[bool] = mapped_column(Boolean, default=True)
    unavailable_reason: Mapped[str | None] = mapped_column(String(255), nullable=True)
    effective_date: Mapped[date] = mapped_column(Date)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    created_by: Mapped[str] = mapped_column(String(64), default="system")
    updated_by: Mapped[str] = mapped_column(String(64), default="system")

    machine = relationship("Machine", back_populates="shift_availabilities")
