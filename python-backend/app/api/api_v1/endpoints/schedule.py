from datetime import timedelta

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.domain import machine_name_display, machine_type_display
from app.db.session import get_db
from app.models import Machine
from app.schemas.schedule import (
    BaselineScheduleRunRequest,
    GanttResponse,
    LineCompensationRequest,
    PlanCompareResponse,
    PlanDemoNavItem,
    PublishPlanRequest,
    RealtimeRescheduleRequest,
    SchedulePlanItemRead,
    SchedulePlanVersionRead,
    ScheduleTaskCreate,
    ScheduleTaskRead,
    ScheduleUnassignedRead,
    SoftWarningRead,
)
from app.services.gantt_service import build_gantt_lanes
from app.services.schedule_service import ScheduleService

router = APIRouter(prefix="/schedules", tags=["baseline-schedule"])


@router.get("/tasks", response_model=list[ScheduleTaskRead])
def list_tasks(db: Session = Depends(get_db)):
    return ScheduleService.list_tasks(db)


@router.post("/tasks", response_model=ScheduleTaskRead)
def create_task(payload: ScheduleTaskCreate, db: Session = Depends(get_db)):
    return ScheduleService.create_task(db, payload)


@router.post("/run")
def run_baseline_schedule(payload: BaselineScheduleRunRequest, db: Session = Depends(get_db)):
    return ScheduleService.run_baseline_schedule(db, payload)


@router.post("/run-realtime")
def run_realtime_schedule(payload: RealtimeRescheduleRequest, db: Session = Depends(get_db)):
    return ScheduleService.run_realtime_reschedule(db, payload)


@router.post("/run-compensation")
def run_line_compensation(payload: LineCompensationRequest, db: Session = Depends(get_db)):
    return ScheduleService.run_line_compensation(db, payload)


@router.get("/plans", response_model=list[SchedulePlanVersionRead])
def list_plans(db: Session = Depends(get_db)):
    return ScheduleService.list_plan_versions(db)


@router.get("/plans/demo-nav", response_model=list[PlanDemoNavItem])
def demo_nav(db: Session = Depends(get_db)):
    plans = ScheduleService.list_plan_versions(db)
    result: list[dict] = []
    for tag, label in [
        ("BASE", "① 基线计划（100单）"),
        ("RT", "② 插单重排（120单）"),
        ("COMP", "③ 补单计划（140单）"),
    ]:
        matched = [p for p in plans if f"-LIVE-{tag}-" in p.plan_version_no]
        if matched:
            plan = sorted(matched, key=lambda x: x.id, reverse=True)[0]
            prev = sorted([p for p in plans if p.id < plan.id], key=lambda x: x.id, reverse=True)
            result.append({"plan_id": plan.id, "label": label, "cmp_plan_id": prev[0].id if prev else None})
    return result


@router.get("/plans/{plan_version_id}", response_model=SchedulePlanVersionRead)
def get_plan(plan_version_id: int, db: Session = Depends(get_db)):
    return ScheduleService.get_plan_version(db, plan_version_id)


@router.get("/plans/{plan_version_id}/items", response_model=list[SchedulePlanItemRead])
def list_plan_items(plan_version_id: int, db: Session = Depends(get_db)):
    return ScheduleService.list_plan_items(db, plan_version_id)


@router.get("/plans/{plan_version_id}/unassigned", response_model=list[ScheduleUnassignedRead])
def list_unassigned(plan_version_id: int, db: Session = Depends(get_db)):
    return ScheduleService.list_unassigned(db, plan_version_id)


@router.get("/logs")
def list_schedule_logs(business_no: str | None = None, db: Session = Depends(get_db)):
    return ScheduleService.list_run_logs(db, business_no)


@router.post("/plans/{plan_version_id}/publish", response_model=SchedulePlanVersionRead)
def publish_plan(plan_version_id: int, payload: PublishPlanRequest, db: Session = Depends(get_db)):
    return ScheduleService.publish_plan(db, plan_version_id, payload)


@router.get("/plans/{plan_version_id}/gantt", response_model=GanttResponse)
def get_plan_gantt(plan_version_id: int, day: int | None = None, db: Session = Depends(get_db)):
    plan = ScheduleService.get_plan_version(db, plan_version_id)
    items = ScheduleService.list_plan_items(db, plan.id)
    machine_ids = sorted({i.machine_id for i in items})
    machine_map = {}
    machine_type_map = {}
    order_color_map: dict[str, str] = {}
    order_palette = [
        "#0f766e", "#1d4ed8", "#b45309", "#7c3aed", "#be123c", "#0e7490",
        "#15803d", "#a16207", "#c2410c", "#4338ca", "#0f766e", "#9f1239",
    ]
    if machine_ids:
        machines = list(
            db.scalars(
                select(Machine).where(Machine.id.in_(machine_ids)).order_by(Machine.machine_type, Machine.id)
            ).all()
        )
        type_seq: dict[str, int] = {}
        for m in machines:
            type_seq[m.machine_type] = type_seq.get(m.machine_type, 0) + 1
            machine_type_cn = machine_type_display(m.machine_type)
            fallback_name = f"{machine_type_cn}-{type_seq[m.machine_type]:02d}"
            machine_map[m.id] = machine_name_display(m.machine_name or fallback_name, m.machine_type)
            machine_type_map[m.id] = machine_type_cn

    timeline = {"start": None, "end": None, "hours": 0}
    day_count = 1
    day_index = 1
    day_options: list[dict] = []
    day_window_start = None
    day_window_end = None
    lanes: list = []
    if items:
        start_dt = min(i.start_time for i in items)
        end_dt = max(i.end_time for i in items)
        total_hours = max((end_dt - start_dt).total_seconds() / 3600, 1)
        timeline = {"start": start_dt, "end": end_dt, "hours": round(total_hours, 2)}
        day_count = max(int((total_hours - 1e-9) // 24) + 1, 1)
        day_index = day or 1
        if day_index < 1:
            day_index = 1
        if day_index > day_count:
            day_index = day_count
        day_window_start = start_dt + timedelta(days=day_index - 1)
        day_window_end = day_window_start + timedelta(hours=24)
        for d in range(1, day_count + 1):
            d_start = start_dt + timedelta(days=d - 1)
            day_options.append({"value": d, "label": f"第{d}天 ({d_start.strftime('%m-%d')})"})
        lanes = build_gantt_lanes(
            items, day_window_start, day_window_end, db,
            order_color_map, order_palette, machine_map, machine_type_map,
            changed_task_ids=set(),
        )
    return {
        "plan": plan,
        "timeline": timeline,
        "day_count": day_count,
        "day_index": day_index,
        "day_options": day_options,
        "day_window_start": day_window_start,
        "day_window_end": day_window_end,
        "lanes": lanes,
    }


@router.get("/plans/{plan_version_id}/compare/{base_plan_id}", response_model=PlanCompareResponse)
def compare_plans(plan_version_id: int, base_plan_id: int, db: Session = Depends(get_db)):
    selected = ScheduleService.get_plan_version(db, plan_version_id)
    base = ScheduleService.get_plan_version(db, base_plan_id)
    selected_map = {x.task_id: x for x in ScheduleService.list_plan_items(db, selected.id)}
    compare_map = {x.task_id: x for x in ScheduleService.list_plan_items(db, base.id)}
    all_task_ids = set(selected_map.keys()) | set(compare_map.keys())
    unchanged_count = 0
    changed_count = 0
    added_count = 0
    removed_count = 0
    changed_task_ids: set[int] = set()
    for task_id in all_task_ids:
        new_item = selected_map.get(task_id)
        old_item = compare_map.get(task_id)
        if new_item and not old_item:
            added_count += 1
            changed_task_ids.add(task_id)
            continue
        if old_item and not new_item:
            removed_count += 1
            changed_task_ids.add(task_id)
            continue
        if not new_item or not old_item:
            continue
        is_changed = (
            new_item.machine_id != old_item.machine_id
            or new_item.start_time != old_item.start_time
            or new_item.end_time != old_item.end_time
            or new_item.planned_quantity != old_item.planned_quantity
        )
        if is_changed:
            changed_count += 1
            changed_task_ids.add(task_id)
        else:
            unchanged_count += 1
    total_count = len(all_task_ids)
    impacted_count = changed_count + added_count + removed_count
    return {
        "total_count": total_count,
        "impacted_count": impacted_count,
        "unchanged_count": unchanged_count,
        "changed_count": changed_count,
        "added_count": added_count,
        "removed_count": removed_count,
        "change_rate": round((impacted_count / total_count) * 100, 2) if total_count else 0.0,
        "changed_task_ids": sorted(changed_task_ids),
    }


@router.get("/plans/{plan_version_id}/soft-warnings", response_model=list[SoftWarningRead])
def list_plan_soft_warnings(plan_version_id: int, db: Session = Depends(get_db)):
    plan = ScheduleService.get_plan_version(db, plan_version_id)
    return [
        {
            "id": log.id,
            "operation_time": log.operation_time,
            "message": log.error_message,
            "business_no": log.business_no,
        }
        for log in ScheduleService.list_soft_warnings(db, plan.plan_version_no)
    ]
