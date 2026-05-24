from app.solver.constraints.base import PASS, TaskConstraint, Verdict


class ManualLockConstraint(TaskConstraint):
    rule_type = "manual_lock"
    is_hard = True

    @staticmethod
    def _parse_csv(raw: object) -> set[str]:
        if raw is None:
            return set()
        return {item.strip() for item in str(raw).split(",") if item.strip()}

    def check(self, task, cfg: dict) -> Verdict:
        lock_order_nos = self._parse_csv(cfg.get("lock_order_nos"))
        if task.order_no and task.order_no in lock_order_nos:
            return Verdict(
                False,
                True,
                "constraint_manual_lock",
                f"订单命中人工锁定条件: {task.order_no}",
            )
        return PASS
