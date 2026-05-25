from abc import ABC, abstractmethod

from app.solver.problem import ScheduleProblem
from app.solver.result import SolveResult


class BaseSolver(ABC):
    @abstractmethod
    def solve(self, problem: ScheduleProblem) -> SolveResult:
        ...

