from app.solver.scorers.base import MachineScorer, ScoreContext


class EFTScorer(MachineScorer):
    name = "eft"

    def score(self, ctx: ScoreContext) -> tuple:
        return (ctx.proposed_end, 1 if ctx.color_switch else 0)
