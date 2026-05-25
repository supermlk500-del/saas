from dataclasses import dataclass, field
from datetime import date, datetime
from datetime import datetime


@dataclass
class AssignedItem:
    task_id: int
    machine_id: int
    start_time: datetime
    end_time: datetime
    planned_quantity: float
    item_status: str = "planned"
    order_no: str = ""
    due_date: date | None = None
    extra_changeover_hours: float = 0.0
    wash_required: bool = False


@dataclass
class UnassignedItem:
    task_id: int
    reason_code: str
    reason_desc: str


@dataclass
class SoftWarning:
    order_no: str
    message: str


@dataclass
class SolveKPI:
    assigned_count: int = 0
    unassigned_count: int = 0
    soft_warning_count: int = 0
    on_time_count: int = 0
    late_count: int = 0
    on_time_rate: float = 0.0
    total_changeover_hours: float = 0.0
    max_late_days: int = 0
    wash_count: int = 0


@dataclass
class SolveResult:
    assigned: list[AssignedItem] = field(default_factory=list)
    unassigned: list[UnassignedItem] = field(default_factory=list)
    soft_warnings: list[SoftWarning] = field(default_factory=list)
    kpi: SolveKPI = field(default_factory=SolveKPI)

    def refresh_kpi(self) -> None:
        on_time_count = 0
        late_count = 0
        max_late_days = 0

        for item in self.assigned:
            if item.due_date is None:
                continue
            late_days = (item.end_time.date() - item.due_date).days
            if late_days <= 0:
                on_time_count += 1
            else:
                late_count += 1
                max_late_days = max(max_late_days, late_days)

        assigned_count = len(self.assigned)
        self.kpi = SolveKPI(
            assigned_count=assigned_count,
            unassigned_count=len(self.unassigned),
            soft_warning_count=len(self.soft_warnings),
            on_time_count=on_time_count,
            late_count=late_count,
            on_time_rate=round(on_time_count / assigned_count * 100, 2) if assigned_count else 0.0,
            total_changeover_hours=round(sum(item.extra_changeover_hours for item in self.assigned), 2),
            max_late_days=max_late_days,
            wash_count=sum(1 for item in self.assigned if item.wash_required),
        )
