from dataclasses import dataclass
from datetime import datetime

from app.solver.scorers.base import MachineScorer, ScoreContext

_DUE_URGENCY_RANK = {"P0": 3, "P1": 2, "P2": 1, "加急": 3, "紧急": 2, "常规": 1}
_CUSTOMER_PRIORITY_RANK = {"P0": 3, "P1": 2, "P2": 1, "战略": 3, "重点": 2, "普通": 1}


@dataclass(frozen=True)
class CompositeWeights:
    w_end_time: float = 1.0
    w_changeover: float = 0.2
    w_due_urgency: float = 0.3
    w_customer_priority: float = 0.2

    @classmethod
    def from_cfg(cls, cfg: dict | None) -> "CompositeWeights":
        if not cfg:
            return cls()

        defaults = cls()

        def read(key: str, default: float) -> float:
            raw = cfg.get(key)
            if raw is None:
                return default
            try:
                return float(str(raw).strip())
            except Exception:
                return default

        return cls(
            w_end_time=read("w_end_time", defaults.w_end_time),
            w_changeover=read("w_changeover", defaults.w_changeover),
            w_due_urgency=read("w_due_urgency", defaults.w_due_urgency),
            w_customer_priority=read(
                "w_customer_priority",
                defaults.w_customer_priority,
            ),
        )


class CompositeScorer(MachineScorer):
    name = "composite"

    def __init__(self, weights: CompositeWeights | None = None) -> None:
        self._explicit_weights = weights
        self.weights = weights or CompositeWeights()

    def configure(self, constraint_cfg: dict) -> None:
        if self._explicit_weights is not None:
            return
        self.weights = CompositeWeights.from_cfg(constraint_cfg.get("composite"))

    def score(self, ctx: ScoreContext) -> tuple:
        baseline = datetime.utcnow()
        end_hours = max((ctx.proposed_end - baseline).total_seconds() / 3600.0, 0.0)
        end_score = min(end_hours / 720.0, 5.0)
        change_score = min(ctx.extra_changeover_hours / 24.0, 1.0)

        due_rank = _DUE_URGENCY_RANK.get((ctx.task.due_urgency_level or "").strip(), 1)
        customer_rank = _CUSTOMER_PRIORITY_RANK.get(
            (ctx.task.customer_priority_level or "").strip(),
            1,
        )
        due_score = 1.0 - due_rank / 3.0
        customer_score = 1.0 - customer_rank / 3.0

        w = self.weights
        total = (
            w.w_end_time * end_score
            + w.w_changeover * change_score
            + w.w_due_urgency * due_score
            + w.w_customer_priority * customer_score
        )
        return (total, ctx.proposed_end, 1 if ctx.color_switch else 0)
