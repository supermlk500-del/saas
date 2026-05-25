from datetime import datetime

from sqlalchemy import Boolean, DateTime, Float, ForeignKey, String, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base


class MachineCapabilitySnapshot(Base):
    __tablename__ = "machine_capability_snapshot"
    __table_args__ = (
        UniqueConstraint("snapshot_no", "machine_id", name="uk_snapshot_machine"),
    )

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    snapshot_no: Mapped[str] = mapped_column(String(64), index=True)
    machine_id: Mapped[int] = mapped_column(ForeignKey("machine.id"), index=True)
    machine_code: Mapped[str] = mapped_column(String(64))
    param_version: Mapped[str] = mapped_column(String(64))
    snapshot_status: Mapped[str] = mapped_column(String(32), default="draft")
    available_flag: Mapped[bool] = mapped_column(Boolean, default=True)
    speed_value: Mapped[float] = mapped_column(Float)
    changeover_loss: Mapped[float] = mapped_column(Float, default=0)
    stability_score: Mapped[float] = mapped_column(Float, default=100)
    generated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    generated_by: Mapped[str] = mapped_column(String(64), default="system")
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    machine = relationship("Machine", back_populates="snapshots")
