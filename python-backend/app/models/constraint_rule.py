from datetime import datetime

from sqlalchemy import Boolean, DateTime, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base


class ConstraintRule(Base):
    __tablename__ = "constraint_rule"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    rule_code: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    rule_name: Mapped[str] = mapped_column(String(128))
    rule_type: Mapped[str] = mapped_column(String(64), index=True)
    version_no: Mapped[str | None] = mapped_column(String(64), nullable=True, index=True)
    description: Mapped[str | None] = mapped_column(Text(), nullable=True)
    status: Mapped[str] = mapped_column(String(32), default="active")

    # 设计文档 md_constraint_rule 新增字段
    # 硬约束必须满足（违反 → 任务进 unassigned），软约束尽量满足（违反仅记录日志）
    is_hard_constraint: Mapped[bool] = mapped_column(Boolean, default=True)
    # 约束优先级：1=最高，数字越大优先级越低；多约束冲突时按此顺序取舍
    priority_level: Mapped[int] = mapped_column(Integer, default=3)
    # 规则表达式：人类可读的业务规则描述，便于追溯"此计划用的什么条件"
    rule_expression: Mapped[str | None] = mapped_column(Text(), nullable=True)

    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    created_by: Mapped[str] = mapped_column(String(64), default="system")
    updated_by: Mapped[str] = mapped_column(String(64), default="system")

    items = relationship("ConstraintRuleItem", back_populates="rule", cascade="all, delete-orphan")
