from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field


class ConstraintRuleCreate(BaseModel):
    rule_code: str = Field(..., max_length=64)
    rule_name: str = Field(..., max_length=128)
    rule_type: str = Field(..., max_length=64)
    description: str | None = None
    status: str = "active"
    created_by: str = "system"
    is_hard_constraint: bool = True
    priority_level: int = 3
    rule_expression: str | None = None


class ConstraintRuleUpdate(BaseModel):
    rule_name: str | None = None
    rule_type: str | None = None
    description: str | None = None
    status: str | None = None
    updated_by: str = "system"
    is_hard_constraint: bool | None = None
    priority_level: int | None = None
    rule_expression: str | None = None


class ConstraintRuleRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    rule_code: str
    rule_name: str
    rule_type: str
    version_no: str | None
    description: str | None
    status: str
    is_hard_constraint: bool
    priority_level: int
    rule_expression: str | None
    created_at: datetime
    updated_at: datetime


class ConstraintRuleItemUpsert(BaseModel):
    item_key: str
    item_value: str
    item_order: int = 1
    enabled_flag: bool = True
    operator: str = "system"


class ConstraintRuleItemRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    rule_id: int
    item_key: str
    item_value: str
    item_order: int
    enabled_flag: bool
    updated_at: datetime


class ConstraintRuntimeRuleItemRead(BaseModel):
    item_key: str
    item_value: str
    item_order: int
    enabled_flag: bool


class ConstraintRuntimeRuleRead(BaseModel):
    id: int
    rule_code: str
    rule_name: str
    rule_type: str
    status: str
    is_hard_constraint: bool
    priority_level: int
    rule_expression: str | None
    runtime_enabled: bool
    items: list[ConstraintRuntimeRuleItemRead]


class ConstraintRuntimeRulesResponse(BaseModel):
    version_no: str
    version_name: str
    rules: list[ConstraintRuntimeRuleRead]


class ConstraintVersionSave(BaseModel):
    version_no: str
    version_name: str
    remark: str | None = None
    created_by: str = "system"


class ConstraintVersionPublish(BaseModel):
    published_by: str = "system"


class ConstraintVersionRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    version_no: str
    version_name: str
    version_status: str
    published_flag: bool
    published_at: datetime | None
    published_by: str | None
    remark: str | None
    created_at: datetime


class ConstraintHealthResponse(BaseModel):
    active_rule_count: int
    present_types: list[str]
    missing_types: list[str]
    item_count: int
    type_ready: bool
    item_ready: bool
    all_ready: bool
    hard_count: int
    soft_count: int
