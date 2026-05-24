from datetime import datetime

from sqlalchemy import Boolean, DateTime, ForeignKey, Integer, String, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base


class ConstraintRuleItem(Base):
    __tablename__ = "constraint_rule_item"
    __table_args__ = (UniqueConstraint("rule_id", "item_key", "item_order", name="uk_rule_item_order"),)

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    rule_id: Mapped[int] = mapped_column(ForeignKey("constraint_rule.id"), index=True)
    item_key: Mapped[str] = mapped_column(String(128))
    item_value: Mapped[str] = mapped_column(String(255))
    item_order: Mapped[int] = mapped_column(Integer, default=1)
    enabled_flag: Mapped[bool] = mapped_column(Boolean, default=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    created_by: Mapped[str] = mapped_column(String(64), default="system")
    updated_by: Mapped[str] = mapped_column(String(64), default="system")

    rule = relationship("ConstraintRule", back_populates="items")
