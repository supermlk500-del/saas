from datetime import datetime

from sqlalchemy import DateTime, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base


class Machine(Base):
    __tablename__ = "machine"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    machine_code: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    machine_name: Mapped[str] = mapped_column(String(128))
    machine_type: Mapped[str] = mapped_column(String(64))
    status: Mapped[str] = mapped_column(String(32), default="active")
    remark: Mapped[str | None] = mapped_column(Text(), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    created_by: Mapped[str] = mapped_column(String(64), default="system")
    updated_by: Mapped[str] = mapped_column(String(64), default="system")

    capability_params = relationship("MachineCapabilityParam", back_populates="machine", cascade="all, delete-orphan")
    shift_availabilities = relationship("MachineShiftAvailability", back_populates="machine", cascade="all, delete-orphan")
    snapshots = relationship("MachineCapabilitySnapshot", back_populates="machine", cascade="all, delete-orphan")
