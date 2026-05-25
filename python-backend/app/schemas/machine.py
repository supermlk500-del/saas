from datetime import date, datetime

from pydantic import BaseModel, ConfigDict, Field


class MachineBase(BaseModel):
    machine_code: str = Field(..., max_length=64)
    machine_name: str = Field(..., max_length=128)
    machine_type: str = Field(..., max_length=64)
    status: str = "active"
    remark: str | None = None


class MachineCreate(MachineBase):
    created_by: str = "system"


class MachineUpdate(BaseModel):
    machine_name: str | None = None
    machine_type: str | None = None
    status: str | None = None
    remark: str | None = None
    updated_by: str = "system"


class MachineRead(MachineBase):
    model_config = ConfigDict(from_attributes=True)

    id: int
    created_at: datetime
    updated_at: datetime


class MachineCapabilityParamUpsert(BaseModel):
    speed_value: float
    speed_unit: str = "pcs/hour"
    changeover_loss: float = 0
    stability_score: float = 100
    param_version: str
    is_active: bool = True
    operator: str = "system"


class MachineShiftAvailabilityUpsert(BaseModel):
    shift_code: str
    available_flag: bool
    unavailable_reason: str | None = None
    effective_date: date
    operator: str = "system"


class SnapshotGenerateRequest(BaseModel):
    snapshot_no: str
    generated_by: str = "system"


class SnapshotRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    snapshot_no: str
    machine_id: int
    machine_code: str
    param_version: str
    snapshot_status: str
    available_flag: bool
    speed_value: float
    changeover_loss: float
    stability_score: float
    generated_at: datetime
    generated_by: str


class ApiMessage(BaseModel):
    message: str


class MachineResetResult(BaseModel):
    cleared_machines: int
    cleared_snapshots: int
    cleared_params: int
    cleared_shifts: int
    cleared_plan_versions: int
    cleared_plan_items: int
    cleared_unassigned: int
