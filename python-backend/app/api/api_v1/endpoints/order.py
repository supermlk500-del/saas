import csv
from collections import Counter
from datetime import date
from io import StringIO
from urllib.parse import quote

from fastapi import APIRouter, Depends, File, UploadFile
from fastapi.responses import StreamingResponse
from sqlalchemy import delete, select
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models import (
    SchedulePlanItem,
    SchedulePlanVersion,
    ScheduleTask,
    ScheduleUnassignedTask,
)
from app.schemas.schedule import (
    OrderClearResult,
    OrderDashboardResponse,
    OrderImportResult,
    ScheduleTaskCreate,
    ScheduleTaskRead,
    ScheduleTaskUpdate,
)
from app.services.schedule_service import ScheduleService

router = APIRouter(prefix="/orders", tags=["order-management"])


@router.get("", response_model=list[ScheduleTaskRead])
def list_orders(prefix: str | None = None, db: Session = Depends(get_db)):
    tasks = ScheduleService.list_tasks(db)
    if prefix:
        tasks = [t for t in tasks if (t.order_no or "").startswith(prefix)]
    return tasks


@router.post("", response_model=ScheduleTaskRead)
def create_order(payload: ScheduleTaskCreate, db: Session = Depends(get_db)):
    return ScheduleService.create_task(db, payload)


@router.put("/{task_id}", response_model=ScheduleTaskRead)
def update_order(task_id: int, payload: ScheduleTaskUpdate, db: Session = Depends(get_db)):
    current = ScheduleService.get_task(db, task_id)
    data = payload.model_dump(exclude_unset=True)
    return ScheduleService.update_task(
        db,
        task_id,
        fabric_type=data.get("fabric_type", current.fabric_type),
        width_cm=data.get("width_cm", current.width_cm),
        gram_weight=data.get("gram_weight", current.gram_weight),
        color_code=data.get("color_code", current.color_code),
        process_route=data.get("process_route", current.process_route),
        order_quantity=data.get("order_quantity", current.order_quantity),
        quantity_unit=data.get("quantity_unit", current.quantity_unit),
        customer_priority_level=data.get("customer_priority_level", current.customer_priority_level),
        due_date=data.get("due_date", current.due_date),
        priority_level=data.get("priority_level", current.priority_level),
        task_status=data.get("task_status", current.task_status),
        operator=payload.operator,
    )


@router.get("/dashboard", response_model=OrderDashboardResponse)
def orders_dashboard(db: Session = Depends(get_db)):
    tasks = list(db.scalars(select(ScheduleTask).order_by(ScheduleTask.due_date, ScheduleTask.id)).all())
    plans = ScheduleService.list_plan_versions(db)
    latest_plan = plans[0] if plans else None

    plan_item_map: dict[int, dict] = {}
    unassigned_map: dict[int, str] = {}
    if latest_plan:
        plan_items = list(
            db.scalars(
                select(SchedulePlanItem).where(SchedulePlanItem.plan_version_id == latest_plan.id)
            ).all()
        )
        for item in plan_items:
            plan_item_map[item.task_id] = {
                "machine_id": item.machine_id,
                "start_time": item.start_time,
                "end_time": item.end_time,
            }

        unassigned_rows = list(
            db.scalars(
                select(ScheduleUnassignedTask).where(
                    ScheduleUnassignedTask.plan_version_id == latest_plan.id
                )
            ).all()
        )
        for row in unassigned_rows:
            unassigned_map[row.task_id] = row.reason_desc

    today = date.today()
    status_counter = Counter([t.task_status for t in tasks]) if tasks else Counter()
    priority_counter = Counter([t.priority_level for t in tasks]) if tasks else Counter()
    due_urgency_counter = Counter([(t.due_urgency_level or "P2") for t in tasks]) if tasks else Counter()
    customer_priority_counter = Counter([(t.customer_priority_level or "P2") for t in tasks]) if tasks else Counter()

    due_risk = {"overdue": 0, "today": 0, "d1_3": 0, "d4_plus": 0}
    table_rows = []
    for t in tasks:
        if t.due_date < today:
            due_risk["overdue"] += 1
        elif t.due_date == today:
            due_risk["today"] += 1
        elif (t.due_date - today).days <= 3:
            due_risk["d1_3"] += 1
        else:
            due_risk["d4_plus"] += 1

        dispatch = plan_item_map.get(t.id)
        if dispatch:
            dispatch_text = f"机台#{dispatch['machine_id']} {dispatch['start_time']}~{dispatch['end_time']}"
        elif t.id in unassigned_map:
            dispatch_text = f"未排入 {unassigned_map[t.id]}"
        else:
            dispatch_text = "-"

        table_rows.append(
            {
                "order_no": t.order_no,
                "fabric_type": t.fabric_type,
                "color_code": t.color_code or "-",
                "process_route": t.process_route or "-",
                "quantity": t.order_quantity,
                "quantity_unit": t.quantity_unit or "m",
                "due_date": t.due_date,
                "priority_level": t.priority_level,
                "due_urgency_level": t.due_urgency_level or "P2",
                "customer_priority_level": t.customer_priority_level or "P2",
                "task_status": t.task_status,
                "dispatch": dispatch_text,
            }
        )

    total_count = len(tasks)
    assigned_count = len(plan_item_map)
    unassigned_count = len(unassigned_map)
    pending_count = max(total_count - assigned_count - unassigned_count, 0)
    return {
        "latest_plan_no": latest_plan.plan_version_no if latest_plan else "-",
        "latest_snapshot_no": latest_plan.snapshot_version_no if latest_plan else "-",
        "latest_constraint_no": latest_plan.constraint_version_no if latest_plan else "-",
        "total_count": total_count,
        "assigned_count": assigned_count,
        "unassigned_count": unassigned_count,
        "pending_count": pending_count,
        "status_counter": dict(status_counter),
        "priority_counter": dict(priority_counter),
        "due_urgency_counter": dict(due_urgency_counter),
        "customer_priority_counter": dict(customer_priority_counter),
        "due_risk": due_risk,
        "rows": table_rows,
    }


@router.get("/export-csv")
def export_orders_csv(db: Session = Depends(get_db)):
    tasks = list(db.scalars(select(ScheduleTask).order_by(ScheduleTask.id)).all())
    output = StringIO()
    writer = csv.writer(output)
    writer.writerow(
        [
            "订单号",
            "布种",
            "幅宽(cm)",
            "克重(gsm)",
            "颜色",
            "工艺路线",
            "数量",
            "单位",
            "交期",
            "优先级",
            "交期紧急度",
            "客户优先级",
        ]
    )
    for t in tasks:
        writer.writerow(
            [
                t.order_no,
                t.fabric_type,
                t.width_cm or "",
                t.gram_weight or "",
                t.color_code or "",
                t.process_route or "",
                t.order_quantity,
                t.quantity_unit or "m",
                t.due_date,
                t.priority_level,
                t.due_urgency_level or "P2",
                t.customer_priority_level or "P2",
            ]
        )
    data = output.getvalue().encode("utf-8-sig")
    filename = f"布匹订单导出_{date.today().isoformat()}.csv"
    return StreamingResponse(
        iter([data]),
        media_type="text/csv; charset=utf-8",
        headers={"Content-Disposition": f"attachment; filename*=UTF-8''{quote(filename)}"},
    )


@router.get("/csv-template")
def download_orders_csv_template():
    output = StringIO()
    writer = csv.writer(output)
    writer.writerow(
        [
            "订单号",
            "布种",
            "幅宽(cm)",
            "克重(gsm)",
            "颜色",
            "工艺路线",
            "数量",
            "单位",
            "交期",
            "优先级",
            "客户优先级",
        ]
    )
    writer.writerow(["ORD-TPL-001", "平纹布", 160, 200, "藏青", "整经>织机>染色>定型", 1200, "m", "2026-05-03", 4, "P1"])
    writer.writerow(["ORD-TPL-002", "斜纹布", 150, 180, "黑色", "整经>织机>后整理", 900, "m", "2026-05-04", 3, "P2"])
    data = output.getvalue().encode("utf-8-sig")
    return StreamingResponse(
        iter([data]),
        media_type="text/csv; charset=utf-8",
        headers={"Content-Disposition": "attachment; filename*=UTF-8''%E5%B8%83%E5%8C%B9%E8%AE%A2%E5%8D%95%E5%AF%BC%E5%85%A5%E6%A8%A1%E6%9D%BF.csv"},
    )


@router.post("/import-csv", response_model=OrderImportResult)
async def import_orders_csv(file: UploadFile = File(...), db: Session = Depends(get_db)):
    raw = await file.read()
    text = raw.decode("utf-8-sig")
    reader = csv.DictReader(StringIO(text))

    def _pick(row_data: dict, *keys: str, default: str = "") -> str:
        for key in keys:
            value = row_data.get(key)
            if value is not None and str(value).strip() != "":
                return str(value).strip()
        return default

    created = 0
    skipped = 0
    errors = 0
    for row in reader:
        try:
            order_no = _pick(row, "订单号", "order_no")
            if not order_no:
                errors += 1
                continue
            exists = db.scalar(select(ScheduleTask.id).where(ScheduleTask.order_no == order_no))
            if exists:
                skipped += 1
                continue
            db.add(
                ScheduleTask(
                    order_no=order_no,
                    fabric_type=_pick(row, "布种", "fabric_type", default="未分类布种"),
                    width_cm=float(_pick(row, "幅宽(cm)", "width_cm", default="0")),
                    gram_weight=float(_pick(row, "克重(gsm)", "gram_weight", default="0")),
                    color_code=_pick(row, "颜色", "color_code", default="未标注"),
                    process_route=_pick(row, "工艺路线", "process_route", default="-"),
                    order_quantity=float(_pick(row, "数量", "order_quantity", default="0")),
                    quantity_unit=_pick(row, "单位", "quantity_unit", default="m"),
                    due_urgency_level=None,
                    customer_priority_level=_pick(row, "客户优先级", "customer_priority_level", default="P2"),
                    due_date=date.fromisoformat(_pick(row, "交期", "due_date")),
                    priority_level=int(_pick(row, "优先级", "priority_level", default="1")),
                    task_status="pending",
                    created_by="csv_import",
                    updated_by="csv_import",
                )
            )
            created += 1
        except Exception:
            errors += 1

    db.commit()
    return {"created": created, "skipped": skipped, "errors": errors}


@router.post("/clear", response_model=OrderClearResult)
def clear_orders(db: Session = Depends(get_db)):
    plan_ids = list(db.scalars(select(SchedulePlanVersion.id)).all())
    plan_item_deleted = 0
    unassigned_deleted = 0
    if plan_ids:
        plan_item_deleted = db.execute(
            delete(SchedulePlanItem).where(SchedulePlanItem.plan_version_id.in_(plan_ids))
        ).rowcount or 0
        unassigned_deleted = db.execute(
            delete(ScheduleUnassignedTask).where(ScheduleUnassignedTask.plan_version_id.in_(plan_ids))
        ).rowcount or 0
    plan_deleted = db.execute(delete(SchedulePlanVersion)).rowcount or 0
    task_deleted = db.execute(delete(ScheduleTask)).rowcount or 0
    db.commit()
    return {
        "cleared_tasks": task_deleted,
        "cleared_plans": plan_deleted,
        "cleared_items": plan_item_deleted,
        "cleared_unassigned": unassigned_deleted,
    }
