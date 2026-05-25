from collections.abc import Callable

from app.solver.greedy import GreedySolver
from app.solver.scorers import ChangeoverFirstScorer, CompositeScorer, EFTScorer
from app.solver.solver_base import BaseSolver
from app.solver.sorters import (
    CriticalRatioSorter,
    EarliestDueDateSorter,
    PriorityFirstSorter,
)

SolverFactory = Callable[[], BaseSolver]

SOLVER_STRATEGIES: dict[str, SolverFactory] = {
    "greedy_eft": lambda: GreedySolver(PriorityFirstSorter(), EFTScorer()),
    "edd": lambda: GreedySolver(EarliestDueDateSorter(), EFTScorer()),
    "cr": lambda: GreedySolver(CriticalRatioSorter(), EFTScorer()),
    "changeover_first": lambda: GreedySolver(
        PriorityFirstSorter(),
        ChangeoverFirstScorer(),
    ),
    "composite": lambda: GreedySolver(PriorityFirstSorter(), CompositeScorer()),
}

DEFAULT_STRATEGY = "greedy_eft"


def get_solver(strategy_name: str | None) -> BaseSolver:
    name = (strategy_name or DEFAULT_STRATEGY).strip()
    if name not in SOLVER_STRATEGIES:
        raise ValueError(f"未知策略 '{name}'，可选: {sorted(SOLVER_STRATEGIES.keys())}")
    return SOLVER_STRATEGIES[name]()
