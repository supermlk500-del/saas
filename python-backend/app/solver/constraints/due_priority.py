from app.solver.constraints.base import MachineConstraint, PASS, Verdict


class DueDateConstraint(MachineConstraint):
    rule_type = "due_priority"
    requires_proposed_end = True

    def check(self, task, state, proposed_end, cfg: dict) -> Verdict:
        strict_due = str(cfg.get("strict_due", "")).strip().lower() in {"1", "true", "yes", "y", "on"}
        if not strict_due or proposed_end is None or task.due_date is None:
            return PASS
        if proposed_end.date() > task.due_date:
            return Verdict(
                False,
                self.is_hard,
                "constraint_due_limit",
                f"预计完工 {proposed_end.date()} 超过交期 {task.due_date}",
            )
        return PASS

