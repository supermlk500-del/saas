from datetime import datetime

from sqlalchemy import Boolean, DateTime, Float, ForeignKey, String, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base


class MachineCapabilityParam(Base):
    __tablename__ = "machine_capability_param"
    __table_args__ = (UniqueConstraint("machine_id", "param_version", name="uk_machine_param_version"),)

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    machine_id: Mapped[int] = mapped_column(ForeignKey("machine.id"), index=True)
    speed_value: Mapped[float] = mapped_column(Float)
    speed_unit: Mapped[str] = mapped_column(String(32), default="pcs/hour")
    changeover_loss: Mapped[float] = mapped_column(Float, default=0)
    stability_score: Mapped[float] = mapped_column(Float, default=100)
    param_version: Mapped[str] = mapped_column(String(64))
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    created_by: Mapped[str] = mapped_column(String(64), default="system")
    updated_by: Mapped[str] = mapped_column(String(64), default="system")

    machine = relationship("Machine", back_populates="capability_params")
