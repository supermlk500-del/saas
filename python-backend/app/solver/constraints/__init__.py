from app.solver.constraints.base import (
    MachineConstraint,
    PASS,
    TaskConstraint,
    Verdict,
)
from app.solver.constraints.changeover import ChangeoverConstraint
from app.solver.constraints.continuous_limit import (
    ContinuousMaxTaskConstraint,
    ContinuousMinBatchConstraint,
)
from app.solver.constraints.due_priority import DueDateConstraint
from app.solver.constraints.dye_color_changeover import DyeColorChangeoverConstraint
from app.solver.constraints.machine_limit import (
    FabricScopeConstraint,
    MachineTypeConstraint,
    RouteMatchConstraint,
)
from app.solver.constraints.manual_lock import ManualLockConstraint

__all__ = [
    "MachineConstraint",
    "PASS",
    "TaskConstraint",
    "Verdict",
    "ChangeoverConstraint",
    "ContinuousMaxTaskConstraint",
    "ContinuousMinBatchConstraint",
    "DueDateConstraint",
    "DyeColorChangeoverConstraint",
    "FabricScopeConstraint",
    "MachineTypeConstraint",
    "RouteMatchConstraint",
    "ManualLockConstraint",
    "default_task_constraints",
    "default_machine_constraints",
]


def default_task_constraints() -> list[TaskConstraint]:
    return [
        ManualLockConstraint(),
        ContinuousMinBatchConstraint(),
        FabricScopeConstraint(),
    ]


def default_machine_constraints() -> list[MachineConstraint]:
    return [
        ContinuousMaxTaskConstraint(),
        MachineTypeConstraint(),
        RouteMatchConstraint(),
        ChangeoverConstraint(),
        DyeColorChangeoverConstraint(),
        DueDateConstraint(),
    ]
