from app.solver.color_rules import ColorRules
from app.solver.constraints.base import MachineConstraint, PASS, Verdict


class DyeColorChangeoverConstraint(MachineConstraint):
    rule_type = "dye_color_changeover"
    is_hard = False

    def check(self, task, state, proposed_end, cfg: dict) -> Verdict:
        if not cfg:
            return PASS

        rules = ColorRules.from_cfg(cfg)
        task_color = (task.color_code or "").strip()
        if not task_color:
            return PASS

        machine_code = (state.machine.machine_code or "").strip()
        if not rules.is_color_allowed_on_machine(machine_code, task_color):
            return Verdict(
                False,
                rules.dedicated_strict,
                "color_not_allowed_on_dedicated_vat",
                f"{machine_code} is dedicated to another color",
            )

        if rules.violates_continuous_same_color(state.recent_colors, task_color):
            if rules.continuous_violation_as_hard:
                return Verdict(
                    False,
                    True,
                    "continuous_same_color_exceeded",
                    f"{task_color} exceeds continuous same-color limit",
                )
            return Verdict(
                True,
                False,
                "continuous_same_color_warning",
                f"{task_color} exceeds recommended continuous same-color limit",
            )

        extra_hours, wash_required = rules.compute_changeover_hours(
            prev_color=state.last_color,
            curr_color=task_color,
            machine_code=machine_code,
        )
        return Verdict(
            True,
            False,
            extra_changeover_hours=extra_hours,
            wash_required=wash_required,
        )
