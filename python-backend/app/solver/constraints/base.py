from abc import ABC, abstractmethod
from dataclasses import dataclass
from datetime import datetime

from app.models import ScheduleTask
from app.solver.problem import MachineState


@dataclass
class Verdict:
    passed: bool
    is_hard: bool
    reason_code: str = ""
    reason_desc: str = ""
    score_delta: float = 0.0
    extra_changeover_hours: float = 0.0
    wash_required: bool = False


PASS = Verdict(True, True)


class TaskConstraint(ABC):
    """任务级前置约束，不依赖具体机台。"""

    rule_type: str
    is_hard: bool = True

    @abstractmethod
    def check(self, task: ScheduleTask, cfg: dict) -> Verdict:
        ...


class MachineConstraint(ABC):
    """候选机台约束，依赖任务、机台与当前排产状态。"""

    rule_type: str
    is_hard: bool = True
    requires_proposed_end: bool = False

    @abstractmethod
    def check(
        self,
        task: ScheduleTask,
        state: MachineState,
        proposed_end: datetime | None,
        cfg: dict,
    ) -> Verdict:
        ...
