from app.solver.constraints.base import MachineConstraint, PASS, TaskConstraint, Verdict


class FabricScopeConstraint(TaskConstraint):
    rule_type = "machine_limit"

    @staticmethod
    def _parse_csv(raw: object) -> set[str]:
        if raw is None:
            return set()
        return {item.strip() for item in str(raw).split(",") if item.strip()}

    def check(self, task, cfg: dict) -> Verdict:
        allowed_fabric_types = self._parse_csv(cfg.get("allowed_fabric_types"))
        if not allowed_fabric_types:
            return PASS
        if (task.fabric_type or "").strip() not in allowed_fabric_types:
            return Verdict(
                False,
                self.is_hard,
                "constraint_fabric_scope",
                f"布种不在允许范围内: {task.fabric_type}",
            )
        return PASS


class MachineTypeConstraint(MachineConstraint):
    rule_type = "machine_limit"

    @staticmethod
    def _parse_csv(raw: object) -> set[str]:
        if raw is None:
            return set()
        return {item.strip() for item in str(raw).split(",") if item.strip()}

    def check(self, task, state, proposed_end, cfg: dict) -> Verdict:
        allow_machine_types = self._parse_csv(cfg.get("allow_machine_types"))
        if not allow_machine_types:
            return PASS
        if (state.machine.machine_type or "").strip() not in allow_machine_types:
            return Verdict(False, self.is_hard, "machine_type_mismatch", "机台类型不在允许范围内")
        return PASS


class RouteMatchConstraint(MachineConstraint):
    rule_type = "machine_limit"

    def check(self, task, state, proposed_end, cfg: dict) -> Verdict:
        route = (task.process_route or "").strip()
        machine_type = (state.machine.machine_type or "").strip()
        if route and machine_type and machine_type not in route:
            return Verdict(
                False,
                self.is_hard,
                "machine_route_mismatch",
                f"订单工艺路线不匹配机台类型 {machine_type}",
            )
        return PASS

