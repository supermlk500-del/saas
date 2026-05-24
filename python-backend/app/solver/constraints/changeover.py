from app.solver.constraints.base import MachineConstraint, Verdict


class ChangeoverConstraint(MachineConstraint):
    rule_type = "changeover"
    is_hard = False

    @staticmethod
    def _as_bool(value: object, default: bool = False) -> bool:
        if value is None:
            return default
        return str(value).strip().lower() in {"1", "true", "yes", "y", "on"}

    @staticmethod
    def _as_float(value: object, default: float = 0.0) -> float:
        try:
            return float(str(value).strip()) if value is not None else default
        except Exception:
            return default

    def check(self, task, state, proposed_end, cfg: dict) -> Verdict:
        color_change_loss = self._as_float(cfg.get("color_change_loss"), 0.0)
        fabric_change_loss = self._as_float(cfg.get("fabric_change_loss"), 0.0)
        same_color_continuous = self._as_bool(cfg.get("same_color_continuous"), False)

        task_color = (task.color_code or "").strip()
        task_fabric = (task.fabric_type or "").strip()

        extra = 0.0
        if state.last_color and task_color and state.last_color != task_color:
            extra += color_change_loss
            if same_color_continuous:
                extra += color_change_loss
        if state.last_fabric and task_fabric and state.last_fabric != task_fabric:
            extra += fabric_change_loss

        return Verdict(True, False, extra_changeover_hours=extra)
