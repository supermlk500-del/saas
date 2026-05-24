from datetime import date as dt_date, datetime, timedelta

from fastapi import HTTPException
from sqlalchemy import and_, desc, select
from sqlalchemy.orm import Session

from app.core.status import PlanStatus, SnapshotStatus
from app.models import (
    ConstraintRule,
    ConstraintRuleItem,
    ConstraintVersion,
    Machine,
    MachineCapabilitySnapshot,
    SchedulePlanItem,
    SchedulePlanVersion,
    ScheduleRunLog,
    ScheduleTask,
    ScheduleUnassignedTask,
)
from app.schemas.schedule import (
    BaselineScheduleRunRequest,
    LineCompensationRequest,
    PublishPlanRequest,
    RealtimeRescheduleRequest,
    ScheduleTaskCreate,
)
from app.solver.problem import MachineState, ScheduleProblem
from app.solver.result import SolveResult
from app.solver.strategies import DEFAULT_STRATEGY, get_solver


class ScheduleService:
    DISABLED_RUNTIME_RULE_TYPES = {"continuous_limit", "changeover"}

    @staticmethod
    def _log(
        db: Session,
        business_no: str,
        result: str,
        operator: str,
        error: str | None = None,
        operation_name: str = "baseline_schedule_run",
        log_type: str = "schedule_run",
    ) -> None:
        db.add(
            ScheduleRunLog(
                log_type=log_type,
                business_no=business_no,
                operation_name=operation_name,
                operation_result=result,
                error_message=error,
                operator=operator,
                operation_time=datetime.utcnow(),
            )
        )
        db.commit()

    @staticmethod
    def _as_bool(value: str | None, default: bool = False) -> bool:
        if value is None:
            return default
        return str(value).strip().lower() in {"1", "true", "yes", "y", "on"}

    @staticmethod
    def _as_int(value: str | None, default: int) -> int:
        try:
            return int(str(value).strip()) if value is not None else default
        except Exception:
            return default

    @staticmethod
    def _as_float(value: str | None, default: float) -> float:
        try:
            return float(str(value).strip()) if value is not None else default
        except Exception:
            return default

    @staticmethod
    def _rule_runtime_enabled(rule_type: str) -> bool:
        return rule_type not in ScheduleService.DISABLED_RUNTIME_RULE_TYPES

    @staticmethod
    def _derive_due_urgency(due_date: dt_date, ref_date: dt_date | None = None) -> str:
        base_date = ref_date or datetime.utcnow().date()
        days_left = (due_date - base_date).days
        if days_left <= 2:
            return "P0"
        if days_left <= 7:
            return "P1"
        return "P2"

    @staticmethod
    def _refresh_due_urgency_for_tasks(tasks: list[ScheduleTask], operator: str) -> bool:
        changed = False
        today = datetime.utcnow().date()
        for task in tasks:
            target = ScheduleService._derive_due_urgency(task.due_date, today)
            if task.due_urgency_level != target:
                task.due_urgency_level = target
                task.updated_by = operator
                changed = True
        return changed

    @staticmethod
    def _normalize_due_urgency(value: str | None) -> str:
        raw = (value or "").strip()
        if raw in {"P0", "加急"}:
            return "P0"
        if raw in {"P1", "紧急"}:
            return "P1"
        return "P2"

    @staticmethod
    def _normalize_customer_priority(value: str | None) -> str:
        raw = (value or "").strip()
        if raw in {"P0", "战略"}:
            return "P0"
        if raw in {"P1", "重点"}:
            return "P1"
        return "P2"

    @staticmethod
    def list_tasks(db: Session) -> list[ScheduleTask]:
        return list(db.scalars(select(ScheduleTask).order_by(ScheduleTask.id.desc())).all())

    @staticmethod
    def get_task(db: Session, task_id: int) -> ScheduleTask:
        row = db.get(ScheduleTask, task_id)
        if not row:
            raise HTTPException(status_code=404, detail="订单不存在")
        return row

    @staticmethod
    def create_task(db: Session, payload: ScheduleTaskCreate) -> ScheduleTask:
        exists = db.scalar(select(ScheduleTask).where(ScheduleTask.order_no == payload.order_no))
        if exists:
            raise HTTPException(status_code=400, detail="订单号已存在")
        data = payload.model_dump()
        row = ScheduleTask(
            order_no=data["order_no"],
            fabric_type=data["fabric_type"],
            width_cm=data["width_cm"],
            gram_weight=data["gram_weight"],
            color_code=data["color_code"],
            process_route=data["process_route"],
            order_quantity=data["order_quantity"],
            quantity_unit=data["quantity_unit"],
            due_urgency_level=ScheduleService._derive_due_urgency(data["due_date"]),
            customer_priority_level=ScheduleService._normalize_customer_priority(data["customer_priority_level"]),
            due_date=data["due_date"],
            priority_level=data["priority_level"],
            task_status=data["task_status"],
            created_by=data["created_by"],
            updated_by=data["created_by"],
        )
        db.add(row)
        db.commit()
        db.refresh(row)
        return row

    @staticmethod
    def update_task(
        db: Session,
        task_id: int,
        *,
        fabric_type: str,
        width_cm: float,
        gram_weight: float,
        color_code: str,
        process_route: str,
        order_quantity: float,
        quantity_unit: str,
        customer_priority_level: str,
        due_date,
        priority_level: int,
        task_status: str,
        operator: str,
    ) -> ScheduleTask:
        row = ScheduleService.get_task(db, task_id)
        row.fabric_type = fabric_type
        row.width_cm = width_cm
        row.gram_weight = gram_weight
        row.color_code = color_code
        row.process_route = process_route
        row.order_quantity = order_quantity
        row.quantity_unit = quantity_unit
        row.due_urgency_level = ScheduleService._derive_due_urgency(due_date)
        row.customer_priority_level = ScheduleService._normalize_customer_priority(customer_priority_level)
        row.due_date = due_date
        row.priority_level = priority_level
        row.task_status = task_status
        row.updated_by = operator
        db.commit()
        db.refresh(row)
        return row

    @staticmethod
    def load_constraints(
        db: Session, constraint_version_no: str
    ) -> tuple[dict[str, dict[str, str]], set[str], set[str]]:
        """返回 (by_type, hard_types, soft_types)。

        - by_type     : {rule_type -> {param_key -> param_value}}
        - hard_types  : is_hard_constraint=True 的 rule_type 集合（违反 → 任务进 unassigned）
        - soft_types  : is_hard_constraint=False 的 rule_type 集合（违反 → 记录警告日志，计划仍生成）
        """
        version = db.scalar(select(ConstraintVersion).where(ConstraintVersion.version_no == constraint_version_no))
        if not version or not version.published_flag:
            raise HTTPException(status_code=400, detail="无有效约束版本")

        rules = list(
            db.scalars(
                select(ConstraintRule).where(
                    and_(ConstraintRule.version_no == constraint_version_no, ConstraintRule.status == "active")
                )
            ).all()
        )
        if not rules:
            raise HTTPException(status_code=400, detail="约束版本无可用规则")

        rule_ids = [r.id for r in rules]
        items = list(
            db.scalars(
                select(ConstraintRuleItem).where(
                    and_(ConstraintRuleItem.rule_id.in_(rule_ids), ConstraintRuleItem.enabled_flag.is_(True))
                )
            ).all()
        )

        by_type: dict[str, dict[str, str]] = {}
        hard_types: set[str] = set()
        soft_types: set[str] = set()

        id_to_rule = {r.id: r for r in rules}
        for it in items:
            r = id_to_rule[it.rule_id]
            rt = r.rule_type
            by_type.setdefault(rt, {})[it.item_key] = it.item_value
            if getattr(r, "is_hard_constraint", True):
                hard_types.add(rt)
            else:
                soft_types.add(rt)

        return by_type, hard_types, soft_types

    @staticmethod
    def _create_unassigned(
        db: Session,
        plan_version_id: int,
        task_id: int,
        reason_code: str,
        reason_desc: str,
        operator: str,
    ) -> None:
        db.add(
            ScheduleUnassignedTask(
                plan_version_id=plan_version_id,
                task_id=task_id,
                reason_code=reason_code,
                reason_desc=reason_desc,
                created_by=operator,
            )
        )

    @staticmethod
    def _solver_constraint_cfg(constraint_values: dict[str, dict[str, str]]) -> dict[str, dict[str, str]]:
        continuous_cfg = constraint_values.get("continuous_limit", {})
        changeover_cfg = constraint_values.get("changeover", {})
        return {
            "machine_limit": constraint_values.get("machine_limit", {}),
            "continuous_limit": continuous_cfg if ScheduleService._rule_runtime_enabled("continuous_limit") else {},
            "due_priority": constraint_values.get("due_priority", {}),
            "manual_lock": constraint_values.get("manual_lock", {}),
            "changeover": changeover_cfg if ScheduleService._rule_runtime_enabled("changeover") else {},
            "dye_color_changeover": constraint_values.get("dye_color_changeover", {}),
        }

    @staticmethod
    def _persist_solver_result(
        db: Session,
        *,
        plan_id: int,
        result: SolveResult,
        operator: str,
    ) -> list[str]:
        for item in result.assigned:
            db.add(
                SchedulePlanItem(
                    plan_version_id=plan_id,
                    task_id=item.task_id,
                    machine_id=item.machine_id,
                    start_time=item.start_time,
                    end_time=item.end_time,
                    planned_quantity=int(round(item.planned_quantity)),
                    item_status=item.item_status,
                )
            )
            task = db.get(ScheduleTask, item.task_id)
            if not task:
                continue
            task.task_status = "scheduled"
            task.updated_by = operator

        for item in result.unassigned:
            ScheduleService._create_unassigned(
                db,
                plan_id,
                item.task_id,
                item.reason_code,
                item.reason_desc,
                operator,
            )

        return [warning.message for warning in result.soft_warnings]

    @staticmethod
    def _log_soft_warnings(
        db: Session,
        *,
        business_no: str,
        operator: str,
        soft_warnings: list[str],
    ) -> None:
        if not soft_warnings:
            return
        ScheduleService._log(
            db,
            business_no,
            "warning",
            operator,
            f"[软约束受损 {len(soft_warnings)} 处] " + " | ".join(soft_warnings[:30]),
            operation_name="soft_constraint_warning",
            log_type="soft_constraint_warning",
        )

    @staticmethod
    def run_baseline_schedule(db: Session, payload: BaselineScheduleRunRequest) -> dict:
        if db.scalar(select(SchedulePlanVersion).where(SchedulePlanVersion.plan_version_no == payload.plan_version_no)):
            raise HTTPException(status_code=400, detail="计划版本号已存在")

        tasks = list(db.scalars(select(ScheduleTask).where(ScheduleTask.task_status == "pending")).all())
        if ScheduleService._refresh_due_urgency_for_tasks(tasks, payload.generated_by):
            db.commit()
        if not tasks:
            ScheduleService._log(db, payload.plan_version_no, "failed", payload.generated_by, "无任务数据")
            raise HTTPException(status_code=400, detail="无任务数据时禁止排产")

        snapshots = list(
            db.scalars(
                select(MachineCapabilitySnapshot).where(
                    and_(
                        MachineCapabilitySnapshot.snapshot_no == payload.snapshot_version_no,
                        MachineCapabilitySnapshot.snapshot_status == SnapshotStatus.GENERATED,
                        MachineCapabilitySnapshot.available_flag.is_(True),
                    )
                )
            ).all()
        )
        if not snapshots:
            ScheduleService._log(db, payload.plan_version_no, "failed", payload.generated_by, "无机台能力快照")
            raise HTTPException(status_code=400, detail="无机台能力快照时禁止排产")

        constraint_values, hard_types, soft_types = ScheduleService.load_constraints(db, payload.constraint_version_no)

        machine_limit_cfg = constraint_values.get("machine_limit", {})
        allow_types = {x.strip() for x in machine_limit_cfg.get("allow_machine_types", "").split(",") if x.strip()}
        solver_constraint_cfg = ScheduleService._solver_constraint_cfg(constraint_values)

        machine_ids = [s.machine_id for s in snapshots]
        machine_map = {m.id: m for m in db.scalars(select(Machine).where(Machine.id.in_(machine_ids))).all()}
        candidate_snapshots = []
        for s in snapshots:
            m = machine_map.get(s.machine_id)
            if not m:
                continue
            if allow_types and m.machine_type not in allow_types:
                continue
            candidate_snapshots.append((s, m))

        if not candidate_snapshots:
            ScheduleService._log(db, payload.plan_version_no, "failed", payload.generated_by, "约束过滤后无候选机台")
            raise HTTPException(status_code=400, detail="约束过滤后无候选机台")

        plan = SchedulePlanVersion(
            plan_version_no=payload.plan_version_no,
            plan_version_name=payload.plan_version_name,
            plan_status=PlanStatus.GENERATED,
            snapshot_version_no=payload.snapshot_version_no,
            constraint_version_no=payload.constraint_version_no,
            generated_at=datetime.utcnow(),
            generated_by=payload.generated_by,
        )
        db.add(plan)
        db.commit()
        db.refresh(plan)

        machine_states = [
            MachineState(
                snapshot=s,
                machine=m,
                next_available=datetime.utcnow(),
                task_count=0,
                last_color=None,
                last_fabric=None,
            )
            for s, m in candidate_snapshots
        ]
        solver_result = get_solver(payload.strategy_name).solve(
            ScheduleProblem(
                tasks=tasks,
                machine_states=machine_states,
                constraint_cfg=solver_constraint_cfg,
                hard_types=hard_types,
                soft_types=soft_types,
                plan_version_id=plan.id,
                operator=payload.generated_by,
                context="baseline",
            )
        )

        soft_warnings = ScheduleService._persist_solver_result(
            db,
            plan_id=plan.id,
            result=solver_result,
            operator=payload.generated_by,
        )
        db.commit()
        ScheduleService._log(db, payload.plan_version_no, "success", payload.generated_by, None, "baseline_schedule_run")
        ScheduleService._log_soft_warnings(
            db,
            business_no=payload.plan_version_no,
            operator=payload.generated_by,
            soft_warnings=soft_warnings,
        )
        return {
            "plan_version_no": payload.plan_version_no,
            "assigned_count": len(solver_result.assigned),
            "unassigned_count": len(solver_result.unassigned),
            "soft_warning_count": len(soft_warnings),
            "strategy_name": payload.strategy_name or DEFAULT_STRATEGY,
            "kpi": solver_result.kpi.__dict__,
            "plan_status": PlanStatus.GENERATED,
        }

    @staticmethod
    def run_realtime_reschedule(db: Session, payload: RealtimeRescheduleRequest) -> dict:
        if db.scalar(select(SchedulePlanVersion).where(SchedulePlanVersion.plan_version_no == payload.new_plan_version_no)):
            raise HTTPException(status_code=400, detail="新计划版本号已存在")

        source_plan = ScheduleService.get_plan_version(db, payload.source_plan_version_id)
        source_items = list(
            db.scalars(
                select(SchedulePlanItem)
                .where(SchedulePlanItem.plan_version_id == source_plan.id)
                .order_by(SchedulePlanItem.start_time, SchedulePlanItem.id)
            ).all()
        )
        if not source_items:
            raise HTTPException(status_code=400, detail="来源计划无明细，无法实时重排")

        freeze_until = datetime.utcnow() + timedelta(minutes=payload.freeze_minutes)
        locked_items = [x for x in source_items if x.start_time <= freeze_until]
        unlocked_items = [x for x in source_items if x.start_time > freeze_until]
        locked_task_ids = {x.task_id for x in locked_items}

        # Restore unlocked tasks back to pending so they can be rescheduled.
        for item in unlocked_items:
            t = db.get(ScheduleTask, item.task_id)
            if t and t.task_status != "cancelled":
                t.task_status = "pending"
                t.updated_by = payload.generated_by
        db.commit()

        snapshots = list(
            db.scalars(
                select(MachineCapabilitySnapshot).where(
                    and_(
                        MachineCapabilitySnapshot.snapshot_no == source_plan.snapshot_version_no,
                        MachineCapabilitySnapshot.snapshot_status == SnapshotStatus.GENERATED,
                        MachineCapabilitySnapshot.available_flag.is_(True),
                    )
                )
            ).all()
        )
        if not snapshots:
            ScheduleService._log(
                db,
                payload.new_plan_version_no,
                "failed",
                payload.generated_by,
                "无机台能力快照",
                "realtime_reschedule",
            )
            raise HTTPException(status_code=400, detail="无机台能力快照时禁止排产")

        constraint_values, hard_types, soft_types = ScheduleService.load_constraints(db, source_plan.constraint_version_no)

        machine_limit_cfg = constraint_values.get("machine_limit", {})
        allow_types = {x.strip() for x in machine_limit_cfg.get("allow_machine_types", "").split(",") if x.strip()}
        solver_constraint_cfg = ScheduleService._solver_constraint_cfg(constraint_values)

        machine_ids = [s.machine_id for s in snapshots]
        machine_map = {m.id: m for m in db.scalars(select(Machine).where(Machine.id.in_(machine_ids))).all()}
        candidate_snapshots = []
        for s in snapshots:
            m = machine_map.get(s.machine_id)
            if not m:
                continue
            if allow_types and m.machine_type not in allow_types:
                continue
            candidate_snapshots.append((s, m))

        if not candidate_snapshots:
            ScheduleService._log(
                db,
                payload.new_plan_version_no,
                "failed",
                payload.generated_by,
                "约束过滤后无候选机台",
                "realtime_reschedule",
            )
            raise HTTPException(status_code=400, detail="约束过滤后无候选机台")

        new_plan = SchedulePlanVersion(
            plan_version_no=payload.new_plan_version_no,
            plan_version_name=payload.new_plan_version_name,
            plan_status=PlanStatus.GENERATED,
            snapshot_version_no=source_plan.snapshot_version_no,
            constraint_version_no=source_plan.constraint_version_no,
            generated_at=datetime.utcnow(),
            generated_by=payload.generated_by,
        )
        db.add(new_plan)
        db.commit()
        db.refresh(new_plan)

        # Copy locked items directly into new plan.
        for li in locked_items:
            db.add(
                SchedulePlanItem(
                    plan_version_id=new_plan.id,
                    task_id=li.task_id,
                    machine_id=li.machine_id,
                    start_time=li.start_time,
                    end_time=li.end_time,
                    planned_quantity=li.planned_quantity,
                    item_status="locked",
                )
            )
        db.commit()

        machine_states = [
            MachineState(
                snapshot=s,
                machine=m,
                next_available=datetime.utcnow(),
                task_count=0,
                last_color=None,
                last_fabric=None,
            )
            for s, m in candidate_snapshots
        ]
        state_by_machine = {state.snapshot.machine_id: state for state in machine_states}

        # Initialize machine state from locked items.
        for machine_id, state in state_by_machine.items():
            machine_locked = [x for x in locked_items if x.machine_id == machine_id]
            if not machine_locked:
                continue
            state.task_count = len(machine_locked)
            last_item = max(machine_locked, key=lambda x: x.end_time)
            state.next_available = max(state.next_available, last_item.end_time)
            t = db.get(ScheduleTask, last_item.task_id)
            if t:
                state.last_color = (t.color_code or "").strip() or None
                state.last_fabric = t.fabric_type.strip() or None

        tasks = list(db.scalars(select(ScheduleTask).where(ScheduleTask.task_status == "pending")).all())
        if ScheduleService._refresh_due_urgency_for_tasks(tasks, payload.generated_by):
            db.commit()
        tasks = [t for t in tasks if t.id not in locked_task_ids]

        solver_result = get_solver(payload.strategy_name).solve(
            ScheduleProblem(
                tasks=tasks,
                machine_states=machine_states,
                constraint_cfg=solver_constraint_cfg,
                hard_types=hard_types,
                soft_types=soft_types,
                plan_version_id=new_plan.id,
                operator=payload.generated_by,
                context="realtime",
            )
        )
        soft_warnings = ScheduleService._persist_solver_result(
            db,
            plan_id=new_plan.id,
            result=solver_result,
            operator=payload.generated_by,
        )

        db.commit()
        ScheduleService._log(
            db,
            payload.new_plan_version_no,
            "success",
            payload.generated_by,
            None,
            "realtime_reschedule",
        )
        ScheduleService._log_soft_warnings(
            db,
            business_no=payload.new_plan_version_no,
            operator=payload.generated_by,
            soft_warnings=soft_warnings,
        )
        return {
            "plan_version_no": payload.new_plan_version_no,
            "assigned_count": len(solver_result.assigned) + len(locked_items),
            "unassigned_count": len(solver_result.unassigned),
            "locked_count": len(locked_items),
            "soft_warning_count": len(soft_warnings),
            "strategy_name": payload.strategy_name or DEFAULT_STRATEGY,
            "kpi": solver_result.kpi.__dict__,
            "plan_status": PlanStatus.GENERATED,
        }

    @staticmethod
    def run_line_compensation(db: Session, payload: LineCompensationRequest) -> dict:
        if db.scalar(
            select(SchedulePlanVersion).where(
                SchedulePlanVersion.plan_version_no == payload.compensation_plan_version_no
            )
        ):
            raise HTTPException(status_code=400, detail="补偿计划版本号已存在")

        source_plan = ScheduleService.get_plan_version(db, payload.source_plan_version_id)
        snapshots = list(
            db.scalars(
                select(MachineCapabilitySnapshot).where(
                    and_(
                        MachineCapabilitySnapshot.snapshot_no == source_plan.snapshot_version_no,
                        MachineCapabilitySnapshot.snapshot_status == SnapshotStatus.GENERATED,
                        MachineCapabilitySnapshot.available_flag.is_(True),
                    )
                )
            ).all()
        )
        if not snapshots:
            ScheduleService._log(
                db,
                payload.compensation_plan_version_no,
                "failed",
                payload.generated_by,
                "无机台能力快照",
                "line_compensation",
            )
            raise HTTPException(status_code=400, detail="无机台能力快照，无法补偿排产")

        machine_ids = [s.machine_id for s in snapshots]
        machine_map = {m.id: m for m in db.scalars(select(Machine).where(Machine.id.in_(machine_ids))).all()}
        line_candidates = [(s, machine_map.get(s.machine_id)) for s in snapshots if machine_map.get(s.machine_id)]
        line_candidates = [(s, m) for s, m in line_candidates if m.machine_type == payload.line_machine_type]
        if not line_candidates:
            raise HTTPException(status_code=400, detail="目标产线无可用机台快照，无法补偿排产")

        constraint_values, hard_types, soft_types = ScheduleService.load_constraints(db, source_plan.constraint_version_no)
        machine_limit_cfg = constraint_values.get("machine_limit", {})
        allow_types = {x.strip() for x in machine_limit_cfg.get("allow_machine_types", "").split(",") if x.strip()}
        if allow_types and payload.line_machine_type not in allow_types:
            raise HTTPException(status_code=400, detail="约束版本不允许该产线类型执行补偿排产")
        solver_constraint_cfg = ScheduleService._solver_constraint_cfg(constraint_values)

        if payload.task_ids:
            tasks = list(db.scalars(select(ScheduleTask).where(ScheduleTask.id.in_(payload.task_ids))).all())
        else:
            from_unassigned = list(
                db.scalars(
                    select(ScheduleUnassignedTask.task_id).where(
                        ScheduleUnassignedTask.plan_version_id == source_plan.id
                    )
                ).all()
            )
            tasks = list(db.scalars(select(ScheduleTask).where(ScheduleTask.id.in_(from_unassigned))).all()) if from_unassigned else []
        if not tasks:
            raise HTTPException(status_code=400, detail="无可补偿任务，请检查 task_ids 或来源计划未排入清单")

        ScheduleService._refresh_due_urgency_for_tasks(tasks, payload.generated_by)

        for t in tasks:
            if t.task_status != "cancelled":
                t.task_status = "pending"
                t.updated_by = payload.generated_by
        db.commit()

        tasks = [t for t in tasks if t.task_status != "cancelled"]

        plan = SchedulePlanVersion(
            plan_version_no=payload.compensation_plan_version_no,
            plan_version_name=payload.compensation_plan_version_name,
            plan_status=PlanStatus.GENERATED,
            snapshot_version_no=source_plan.snapshot_version_no,
            constraint_version_no=source_plan.constraint_version_no,
            generated_at=datetime.utcnow(),
            generated_by=payload.generated_by,
        )
        db.add(plan)
        db.commit()
        db.refresh(plan)

        line_machine_ids = [s.machine_id for s, _ in line_candidates]
        source_line_items = list(
            db.scalars(
                select(SchedulePlanItem)
                .where(
                    and_(
                        SchedulePlanItem.plan_version_id == source_plan.id,
                        SchedulePlanItem.machine_id.in_(line_machine_ids),
                    )
                )
                .order_by(SchedulePlanItem.end_time)
            ).all()
        )

        machine_states = [
            MachineState(
                snapshot=s,
                machine=m,
                next_available=datetime.utcnow(),
                task_count=0,
                last_color=None,
                last_fabric=None,
            )
            for s, m in line_candidates
        ]
        state_by_machine = {state.snapshot.machine_id: state for state in machine_states}

        for machine_id, state in state_by_machine.items():
            history = [x for x in source_line_items if x.machine_id == machine_id]
            if not history:
                continue
            state.task_count = len(history)
            last_item = max(history, key=lambda x: x.end_time)
            state.next_available = max(state.next_available, last_item.end_time)
            last_task = db.get(ScheduleTask, last_item.task_id)
            if last_task:
                state.last_color = (last_task.color_code or "").strip() or None
                state.last_fabric = last_task.fabric_type.strip() or None

        solver_result = get_solver(payload.strategy_name).solve(
            ScheduleProblem(
                tasks=tasks,
                machine_states=machine_states,
                constraint_cfg=solver_constraint_cfg,
                hard_types=hard_types,
                soft_types=soft_types,
                plan_version_id=plan.id,
                operator=payload.generated_by,
                context="line_compensation",
                item_status="compensated",
            )
        )
        soft_warnings = ScheduleService._persist_solver_result(
            db,
            plan_id=plan.id,
            result=solver_result,
            operator=payload.generated_by,
        )

        db.commit()
        ScheduleService._log(
            db,
            payload.compensation_plan_version_no,
            "success",
            payload.generated_by,
            None,
            "line_compensation",
        )
        ScheduleService._log_soft_warnings(
            db,
            business_no=payload.compensation_plan_version_no,
            operator=payload.generated_by,
            soft_warnings=soft_warnings,
        )
        return {
            "plan_version_no": payload.compensation_plan_version_no,
            "line_machine_type": payload.line_machine_type,
            "assigned_count": len(solver_result.assigned),
            "unassigned_count": len(solver_result.unassigned),
            "soft_warning_count": len(soft_warnings),
            "strategy_name": payload.strategy_name or DEFAULT_STRATEGY,
            "kpi": solver_result.kpi.__dict__,
            "plan_status": PlanStatus.GENERATED,
        }

    @staticmethod
    def list_plan_versions(db: Session) -> list[SchedulePlanVersion]:
        return list(db.scalars(select(SchedulePlanVersion).order_by(SchedulePlanVersion.id.desc())).all())

    @staticmethod
    def get_plan_version(db: Session, plan_version_id: int) -> SchedulePlanVersion:
        row = db.get(SchedulePlanVersion, plan_version_id)
        if not row:
            raise HTTPException(status_code=404, detail="计划版本不存在")
        return row

    @staticmethod
    def list_plan_items(db: Session, plan_version_id: int) -> list[SchedulePlanItem]:
        ScheduleService.get_plan_version(db, plan_version_id)
        return list(
            db.scalars(
                select(SchedulePlanItem)
                .where(SchedulePlanItem.plan_version_id == plan_version_id)
                .order_by(SchedulePlanItem.start_time)
            ).all()
        )

    @staticmethod
    def list_unassigned(db: Session, plan_version_id: int) -> list[ScheduleUnassignedTask]:
        ScheduleService.get_plan_version(db, plan_version_id)
        return list(
            db.scalars(
                select(ScheduleUnassignedTask)
                .where(ScheduleUnassignedTask.plan_version_id == plan_version_id)
                .order_by(ScheduleUnassignedTask.id)
            ).all()
        )

    @staticmethod
    def list_run_logs(db: Session, business_no: str | None = None) -> list[ScheduleRunLog]:
        stmt = (
            select(ScheduleRunLog)
            .where(ScheduleRunLog.log_type == "schedule_run")
            .order_by(desc(ScheduleRunLog.operation_time), desc(ScheduleRunLog.id))
        )
        if business_no:
            stmt = stmt.where(ScheduleRunLog.business_no == business_no)
        return list(db.scalars(stmt).all())

    @staticmethod
    def list_soft_warnings(db: Session, business_no: str) -> list[ScheduleRunLog]:
        """返回指定版本号下所有软约束受损警告日志。"""
        return list(
            db.scalars(
                select(ScheduleRunLog)
                .where(
                    and_(
                        ScheduleRunLog.log_type == "soft_constraint_warning",
                        ScheduleRunLog.business_no == business_no,
                    )
                )
                .order_by(desc(ScheduleRunLog.operation_time))
            ).all()
        )

    @staticmethod
    def publish_plan(db: Session, plan_version_id: int, payload: PublishPlanRequest) -> SchedulePlanVersion:
        row = ScheduleService.get_plan_version(db, plan_version_id)
        if row.plan_status not in {PlanStatus.GENERATED, PlanStatus.PUBLISHED}:
            raise HTTPException(status_code=400, detail="仅 generated/published 计划可发布")

        active = list(db.scalars(select(SchedulePlanVersion).where(SchedulePlanVersion.plan_status == PlanStatus.PUBLISHED)).all())
        for v in active:
            v.plan_status = PlanStatus.INACTIVE

        row.plan_status = PlanStatus.PUBLISHED
        row.published_at = datetime.utcnow()
        row.published_by = payload.published_by
        db.commit()
        db.refresh(row)
        return row
