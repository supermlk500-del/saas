from datetime import date, datetime

from pydantic import BaseModel, ConfigDict, Field


class ScheduleTaskCreate(BaseModel):
    order_no: str
    fabric_type: str
    width_cm: float
    gram_weight: float
    color_code: str
    process_route: str
    order_quantity: float = Field(..., gt=0)
    quantity_unit: str = "m"
    due_urgency_level: str | None = None
    customer_priority_level: str = "P2"
    due_date: date
    priority_level: int = Field(default=1, ge=1, le=9)
    task_status: str = "pending"
    created_by: str = "system"


class ScheduleTaskRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    order_no: str
    fabric_type: str
    width_cm: float | None
    gram_weight: float | None
    color_code: str | None
    process_route: str | None
    order_quantity: float
    quantity_unit: str
    due_urgency_level: str
    customer_priority_level: str
    due_date: date
    priority_level: int
    task_status: str


class BaselineScheduleRunRequest(BaseModel):
    plan_version_no: str
    plan_version_name: str
    snapshot_version_no: str
    constraint_version_no: str
    generated_by: str = "system"
    strategy_name: str | None = None


class RealtimeRescheduleRequest(BaseModel):
    source_plan_version_id: int
    new_plan_version_no: str
    new_plan_version_name: str
    freeze_minutes: int = Field(default=0, ge=0, le=720)
    generated_by: str = "system"
    strategy_name: str | None = None


class LineCompensationRequest(BaseModel):
    source_plan_version_id: int
    compensation_plan_version_no: str
    compensation_plan_version_name: str
    line_machine_type: str
    task_ids: list[int] = Field(default_factory=list)
    generated_by: str = "system"
    strategy_name: str | None = None


class SchedulePlanVersionRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    plan_version_no: str
    plan_version_name: str
    plan_status: str
    snapshot_version_no: str
    constraint_version_no: str
    generated_at: datetime
    generated_by: str
    published_at: datetime | None
    published_by: str | None


class SchedulePlanItemRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    plan_version_id: int
    task_id: int
    machine_id: int
    start_time: datetime
    end_time: datetime
    planned_quantity: int
    item_status: str


class ScheduleUnassignedRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    plan_version_id: int
    task_id: int
    reason_code: str
    reason_desc: str
    created_at: datetime


class PublishPlanRequest(BaseModel):
    published_by: str = "system"


class ScheduleTaskUpdate(BaseModel):
    fabric_type: str | None = None
    width_cm: float | None = None
    gram_weight: float | None = None
    color_code: str | None = None
    process_route: str | None = None
    order_quantity: float | None = None
    quantity_unit: str | None = None
    customer_priority_level: str | None = None
    due_date: date | None = None
    priority_level: int | None = None
    task_status: str | None = None
    operator: str = "system"


class OrderDashboardRow(BaseModel):
    order_no: str
    fabric_type: str
    color_code: str
    process_route: str
    quantity: float
    quantity_unit: str
    due_date: date
    priority_level: int
    due_urgency_level: str
    customer_priority_level: str
    task_status: str
    dispatch: str


class OrderDashboardResponse(BaseModel):
    latest_plan_no: str
    latest_snapshot_no: str
    latest_constraint_no: str
    total_count: int
    assigned_count: int
    unassigned_count: int
    pending_count: int
    status_counter: dict[str, int]
    priority_counter: dict[int, int]
    due_urgency_counter: dict[str, int]
    customer_priority_counter: dict[str, int]
    due_risk: dict[str, int]
    rows: list[OrderDashboardRow]


class OrderImportResult(BaseModel):
    created: int
    skipped: int
    errors: int


class OrderClearResult(BaseModel):
    cleared_tasks: int
    cleared_plans: int
    cleared_items: int
    cleared_unassigned: int


class GanttBar(BaseModel):
    left: float
    width: float
    top_px: int
    label: str
    start: datetime
    end: datetime
    qty: int
    color: str
    machine_type: str
    is_overdue: bool
    is_changed: bool
    is_compensated: bool


class GanttLane(BaseModel):
    machine_id: int
    machine_name: str
    bars: list[GanttBar]
    track_h: int


class GanttTimeline(BaseModel):
    start: datetime | None
    end: datetime | None
    hours: float


class GanttDayOption(BaseModel):
    value: int
    label: str


class GanttResponse(BaseModel):
    plan: SchedulePlanVersionRead
    timeline: GanttTimeline
    day_count: int
    day_index: int
    day_options: list[GanttDayOption]
    day_window_start: datetime | None
    day_window_end: datetime | None
    lanes: list[GanttLane]


class PlanCompareResponse(BaseModel):
    total_count: int
    impacted_count: int
    unchanged_count: int
    changed_count: int
    added_count: int
    removed_count: int
    change_rate: float
    changed_task_ids: list[int]


class PlanDemoNavItem(BaseModel):
    plan_id: int
    label: str
    cmp_plan_id: int | None


class SoftWarningRead(BaseModel):
    id: int
    operation_time: datetime
    message: str | None
    business_no: str
