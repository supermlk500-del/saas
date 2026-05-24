from dataclasses import dataclass, field
from datetime import datetime

from app.models.machine import Machine
from app.models.machine_capability_snapshot import MachineCapabilitySnapshot
from app.models.schedule_task import ScheduleTask


@dataclass
class MachineState:
    snapshot: MachineCapabilitySnapshot
    machine: Machine
    next_available: datetime
    task_count: int
    last_color: str | None
    last_fabric: str | None
    recent_colors: list[str] = field(default_factory=list)


@dataclass
class ScheduleProblem:
    tasks: list[ScheduleTask]
    machine_states: list[MachineState]
    constraint_cfg: dict[str, object]
    hard_types: set[str]
    soft_types: set[str]
    plan_version_id: int
    operator: str
    context: str
    item_status: str = "planned"
