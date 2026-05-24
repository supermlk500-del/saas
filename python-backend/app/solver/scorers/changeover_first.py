from app.solver.scorers.base import MachineScorer, ScoreContext


class ChangeoverFirstScorer(MachineScorer):
    name = "changeover_first"

    def score(self, ctx: ScoreContext) -> tuple:
        return (
            ctx.extra_changeover_hours,
            1 if ctx.color_switch else 0,
            ctx.proposed_end,
        )
