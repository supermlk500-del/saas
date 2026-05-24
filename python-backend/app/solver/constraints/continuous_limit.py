from app.solver.constraints.base import MachineConstraint, PASS, TaskConstraint, Verdict


class ContinuousMinBatchConstraint(TaskConstraint):
    rule_type = "continuous_limit"

    def check(self, task, cfg: dict) -> Verdict:
        raw = cfg.get("min_start_batch")
        if raw in {None, ""}:
            return PASS

        try:
            min_start_batch = float(str(raw).strip())
        except Exception:
            return PASS

        qty = float(task.order_quantity or 0)
        if qty < min_start_batch:
            return Verdict(False, self.is_hard, "task_min_batch", f"订单量低于最小起排批量 {min_start_batch:g}")
        return PASS


class ContinuousMaxTaskConstraint(MachineConstraint):
    rule_type = "continuous_limit"

    def check(self, task, state, proposed_end, cfg: dict) -> Verdict:
        raw = cfg.get("max_task_per_machine")
        if raw in {None, ""}:
            return PASS

        try:
            max_task_per_machine = int(str(raw).strip())
        except Exception:
            return PASS

        if state.task_count >= max_task_per_machine:
            return Verdict(
                False,
                self.is_hard,
                "machine_capacity_full",
                f"机台任务数达到上限 {max_task_per_machine}",
            )
        return PASS
