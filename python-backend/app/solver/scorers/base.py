from abc import ABC, abstractmethod
from dataclasses import dataclass
from datetime import datetime

from app.models import ScheduleTask
from app.solver.problem import MachineState


@dataclass(frozen=True)
class ScoreContext:
    task: ScheduleTask
    state: MachineState
    proposed_start: datetime
    proposed_end: datetime
    extra_changeover_hours: float
    color_switch: bool
    fabric_switch: bool


class MachineScorer(ABC):
    name: str

    def configure(self, constraint_cfg: dict) -> None:
        return None

    @abstractmethod
    def score(self, ctx: ScoreContext) -> tuple:
        ...
