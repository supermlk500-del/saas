from fastapi import APIRouter, Depends
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models import ConstraintRuleItem
from app.schemas.constraint import (
    ConstraintHealthResponse,
    ConstraintRuleCreate,
    ConstraintRuleItemRead,
    ConstraintRuleItemUpsert,
    ConstraintRuleRead,
    ConstraintRuntimeRuleItemRead,
    ConstraintRuntimeRuleRead,
    ConstraintRuntimeRulesResponse,
    ConstraintRuleUpdate,
    ConstraintVersionPublish,
    ConstraintVersionRead,
    ConstraintVersionSave,
)
from app.services.constraint_service import ConstraintService
from app.services.schedule_service import ScheduleService

router = APIRouter(prefix="/constraints", tags=["constraint-config"])


@router.get("/rules", response_model=list[ConstraintRuleRead])
def list_rules(db: Session = Depends(get_db)):
    return ConstraintService.list_rules(db)


@router.post("/rules", response_model=ConstraintRuleRead)
def create_rule(payload: ConstraintRuleCreate, db: Session = Depends(get_db)):
    return ConstraintService.create_rule(db, payload)


@router.put("/rules/{rule_id}", response_model=ConstraintRuleRead)
def update_rule(rule_id: int, payload: ConstraintRuleUpdate, db: Session = Depends(get_db)):
    return ConstraintService.update_rule(db, rule_id, payload)


@router.get("/rules/{rule_id}", response_model=ConstraintRuleRead)
def get_rule(rule_id: int, db: Session = Depends(get_db)):
    return ConstraintService.get_rule(db, rule_id)


@router.get("/rules/{rule_id}/items", response_model=list[ConstraintRuleItemRead])
def list_rule_items(rule_id: int, db: Session = Depends(get_db)):
    return ConstraintService.list_rule_items(db, rule_id)


@router.post("/rules/{rule_id}/items", response_model=ConstraintRuleItemRead)
def upsert_rule_item(rule_id: int, payload: ConstraintRuleItemUpsert, db: Session = Depends(get_db)):
    return ConstraintService.upsert_rule_item(db, rule_id, payload)


@router.post("/versions/save", response_model=ConstraintVersionRead)
def save_version(payload: ConstraintVersionSave, db: Session = Depends(get_db)):
    return ConstraintService.save_version(db, payload)


@router.post("/versions/{version_no}/publish", response_model=ConstraintVersionRead)
def publish_version(version_no: str, payload: ConstraintVersionPublish, db: Session = Depends(get_db)):
    return ConstraintService.publish_version(db, version_no, payload)


@router.get("/versions", response_model=list[ConstraintVersionRead])
def list_versions(db: Session = Depends(get_db)):
    return ConstraintService.list_versions(db)


@router.get("/versions/current", response_model=ConstraintVersionRead)
def current_version(db: Session = Depends(get_db)):
    return ConstraintService.current_version(db)


@router.get("/runtime-rules", response_model=ConstraintRuntimeRulesResponse)
def runtime_rules(db: Session = Depends(get_db)):
    version = ConstraintService.current_version(db)
    rules = [
        r for r in ConstraintService.list_rules(db)
        if r.version_no == version.version_no and r.status == "active"
    ]

    result_rules: list[ConstraintRuntimeRuleRead] = []
    for rule in rules:
        items = ConstraintService.list_rule_items(db, rule.id)
        result_rules.append(
            ConstraintRuntimeRuleRead(
                id=rule.id,
                rule_code=rule.rule_code,
                rule_name=rule.rule_name,
                rule_type=rule.rule_type,
                status=rule.status,
                is_hard_constraint=rule.is_hard_constraint,
                priority_level=rule.priority_level,
                rule_expression=rule.rule_expression,
                runtime_enabled=ScheduleService._rule_runtime_enabled(rule.rule_type),
                items=[
                    ConstraintRuntimeRuleItemRead(
                        item_key=item.item_key,
                        item_value=item.item_value,
                        item_order=item.item_order,
                        enabled_flag=item.enabled_flag,
                    )
                    for item in items
                ],
            )
        )

    result_rules.sort(key=lambda x: (x.priority_level, x.rule_type))
    return ConstraintRuntimeRulesResponse(
        version_no=version.version_no,
        version_name=version.version_name,
        rules=result_rules,
    )


@router.get("/health", response_model=ConstraintHealthResponse)
def constraint_health(db: Session = Depends(get_db)):
    rules = ConstraintService.list_rules(db)
    required_types = {"due_priority", "machine_limit", "changeover", "continuous_limit", "manual_lock"}
    active_rules = [r for r in rules if r.status == "active"]
    present_types = {r.rule_type for r in active_rules}
    missing_types = sorted(required_types - present_types)
    active_rule_ids = [r.id for r in active_rules]
    item_count = 0
    if active_rule_ids:
        item_count = db.scalar(
            select(func.count(ConstraintRuleItem.id)).where(
                ConstraintRuleItem.rule_id.in_(active_rule_ids),
                ConstraintRuleItem.enabled_flag.is_(True),
            )
        ) or 0
    type_ready = len(missing_types) == 0
    item_ready = item_count >= 15
    hard_count = sum(1 for r in active_rules if getattr(r, "is_hard_constraint", True))
    soft_count = len(active_rules) - hard_count
    return {
        "active_rule_count": len(active_rules),
        "present_types": sorted(present_types),
        "missing_types": missing_types,
        "item_count": item_count,
        "type_ready": type_ready,
        "item_ready": item_ready,
        "all_ready": type_ready and item_ready,
        "hard_count": hard_count,
        "soft_count": soft_count,
    }
