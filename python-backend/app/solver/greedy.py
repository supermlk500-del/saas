from datetime import timedelta

from app.solver.constraints import (
    Verdict,
    default_machine_constraints,
    default_task_constraints,
)
from app.solver.problem import ScheduleProblem
from app.solver.result import AssignedItem, SoftWarning, SolveResult, UnassignedItem
from app.solver.scorers.base import MachineScorer, ScoreContext
from app.solver.scorers.eft import EFTScorer
from app.solver.solver_base import BaseSolver
from app.solver.sorters.base import TaskSorter
from app.solver.sorters.priority_first import PriorityFirstSorter


class GreedySolver(BaseSolver):
    def __init__(
        self,
        sorter: TaskSorter | None = None,
        scorer: MachineScorer | None = None,
    ) -> None:
        self.sorter = sorter or PriorityFirstSorter()
        self.scorer = scorer or EFTScorer()
        self._task_constraints = default_task_constraints()
        self._machine_constraints = default_machine_constraints()

    def solve(self, problem: ScheduleProblem) -> SolveResult:
        self.scorer.configure(problem.constraint_cfg)
        result = SolveResult()

        for task in self.sorter.sort(list(problem.tasks)):
            blocked = self._check_task_constraints(task, problem)
            if blocked is not None:
                result.unassigned.append(blocked)
                continue

            chosen = self._pick_best_machine(task, problem, result)
            if chosen is None:
                continue

            state, start_time, end_time, extra_changeover, wash_required = chosen
            self._record_assignment(
                task,
                state,
                start_time,
                end_time,
                extra_changeover,
                wash_required,
                problem,
                result,
            )

        result.refresh_kpi()
        return result

    def _check_task_constraints(self, task, problem: ScheduleProblem) -> UnassignedItem | None:
        for constraint in self._task_constraints:
            cfg = problem.constraint_cfg.get(constraint.rule_type, {}) or {}
            verdict: Verdict = constraint.check(task, cfg)
            if verdict.passed:
                continue
            if constraint.rule_type in problem.hard_types:
                return UnassignedItem(task.id, verdict.reason_code, verdict.reason_desc)
        return None

    def _pick_best_machine(self, task, problem: ScheduleProblem, result: SolveResult):
        qty = float(task.order_quantity)
        task_color = (task.color_code or "").strip()
        task_fabric = (task.fabric_type or "").strip()
        chosen = None
        best_score = None
        blocked_counter: dict[str, int] = {}

        for state in problem.machine_states:
            speed = state.snapshot.speed_value
            if speed <= 0:
                blocked_counter["machine_speed_invalid"] = (
                    blocked_counter.get("machine_speed_invalid", 0) + 1
                )
                continue

            hours_no_changeover = (qty / speed) + state.snapshot.changeover_loss
            start_time = state.next_available
            end_time = start_time + timedelta(hours=hours_no_changeover)

            extra_changeover = 0.0
            wash_required = False
            soft_verdicts: list[Verdict] = []
            rejected = False

            for constraint in self._machine_constraints:
                cfg = problem.constraint_cfg.get(constraint.rule_type, {}) or {}
                proposed_end = end_time if constraint.requires_proposed_end else None
                verdict: Verdict = constraint.check(task, state, proposed_end, cfg)
                if verdict.passed:
                    if verdict.reason_code and constraint.rule_type in problem.soft_types:
                        soft_verdicts.append(verdict)
                    if verdict.extra_changeover_hours:
                        extra_changeover += verdict.extra_changeover_hours
                        end_time = start_time + timedelta(
                            hours=hours_no_changeover + extra_changeover
                        )
                    wash_required = wash_required or verdict.wash_required
                    continue

                if constraint.rule_type in problem.hard_types or verdict.is_hard:
                    blocked_counter[verdict.reason_code] = (
                        blocked_counter.get(verdict.reason_code, 0) + 1
                    )
                    rejected = True
                    break

            if rejected:
                continue

            color_switch = bool(state.last_color and task_color and state.last_color != task_color)
            fabric_switch = bool(
                state.last_fabric and task_fabric and state.last_fabric != task_fabric
            )
            score = self.scorer.score(
                ScoreContext(
                    task=task,
                    state=state,
                    proposed_start=start_time,
                    proposed_end=end_time,
                    extra_changeover_hours=extra_changeover,
                    color_switch=color_switch,
                    fabric_switch=fabric_switch,
                )
            )
            if best_score is None or score < best_score:
                best_score = score
                chosen = (
                    state,
                    start_time,
                    end_time,
                    extra_changeover,
                    wash_required,
                    soft_verdicts,
                )

        if chosen is None:
            reason_code, reason_desc = self._summarize_block_reason(
                blocked_counter,
                len(problem.machine_states),
            )
            result.unassigned.append(UnassignedItem(task.id, reason_code, reason_desc))
            return None

        state, start_time, end_time, extra_changeover, wash_required, soft_verdicts = chosen
        for verdict in soft_verdicts:
            result.soft_warnings.append(
                SoftWarning(task.order_no or "", f"{task.order_no}:{verdict.reason_desc}")
            )
        return (state, start_time, end_time, extra_changeover, wash_required)

    @staticmethod
    def _summarize_block_reason(counter: dict[str, int], total: int) -> tuple[str, str]:
        if counter.get("machine_route_mismatch", 0) == total:
            return "machine_route_mismatch", "无机台匹配订单工艺路线"
        if counter.get("machine_capacity_full", 0) == total:
            return "machine_capacity_full", "候选机台均达到连续生产上限"
        if counter.get("constraint_due_limit", 0) > 0:
            return "constraint_due_limit", "受交期约束限制，当前无机台可按期完成"
        if counter.get("machine_speed_invalid", 0) == total:
            return "machine_speed_invalid", "候选机台速度参数无效"
        return "machine_no_candidate", "当前无满足约束条件的候选机台"

    def _record_assignment(
        self,
        task,
        state,
        start_time,
        end_time,
        extra_changeover,
        wash_required,
        problem: ScheduleProblem,
        result: SolveResult,
    ) -> None:
        order_no = task.order_no or ""
        task_color = (task.color_code or "").strip()
        task_fabric = (task.fabric_type or "").strip()

        if extra_changeover > 0 and "changeover" in problem.soft_types:
            result.soft_warnings.append(
                SoftWarning(
                    order_no,
                    f"{order_no}:换型+{extra_changeover:.2f}h@{state.machine.machine_code}",
                )
            )
        if (
            task.due_date
            and end_time.date() > task.due_date
            and "due_priority" in problem.soft_types
        ):
            result.soft_warnings.append(
                SoftWarning(order_no, f"{order_no}:完成{end_time.date()}>交期{task.due_date}")
            )

        result.assigned.append(
            AssignedItem(
                task_id=task.id,
                machine_id=state.snapshot.machine_id,
                start_time=start_time,
                end_time=end_time,
                planned_quantity=int(round(float(task.order_quantity))),
                item_status=problem.item_status,
                order_no=order_no,
                due_date=task.due_date,
                extra_changeover_hours=extra_changeover,
                wash_required=wash_required,
            )
        )
        state.task_count += 1
        state.next_available = end_time
        state.last_color = task_color or state.last_color
        state.last_fabric = task_fabric or state.last_fabric
        if task_color:
            state.recent_colors.insert(0, task_color)
            del state.recent_colors[10:]
