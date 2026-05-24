from datetime import datetime

from fastapi import HTTPException
from sqlalchemy import and_, select
from sqlalchemy.orm import Session

from app.models import ConstraintRule, ConstraintRuleItem, ConstraintVersion, ScheduleRunLog
from app.schemas.constraint import (
    ConstraintRuleCreate,
    ConstraintRuleItemUpsert,
    ConstraintRuleUpdate,
    ConstraintVersionPublish,
    ConstraintVersionSave,
)


class ConstraintService:
    REQUIRED_RULE_TYPES = {
        "due_priority",
        "machine_limit",
        "changeover",
        "continuous_limit",
        "manual_lock",
    }

    @staticmethod
    def _log(db: Session, business_no: str, result: str, operator: str, error: str | None = None) -> None:
        db.add(
            ScheduleRunLog(
                log_type="constraint_publish",
                business_no=business_no,
                operation_name="publish_constraint_version",
                operation_result=result,
                error_message=error,
                operator=operator,
                operation_time=datetime.utcnow(),
            )
        )
        db.commit()

    @staticmethod
    def list_rules(db: Session) -> list[ConstraintRule]:
        return list(db.scalars(select(ConstraintRule).order_by(ConstraintRule.id.desc())).all())

    @staticmethod
    def get_rule(db: Session, rule_id: int) -> ConstraintRule:
        row = db.get(ConstraintRule, rule_id)
        if not row:
            raise HTTPException(status_code=404, detail="约束不存在")
        return row

    @staticmethod
    def create_rule(db: Session, payload: ConstraintRuleCreate) -> ConstraintRule:
        exists = db.scalar(select(ConstraintRule).where(ConstraintRule.rule_code == payload.rule_code))
        if exists:
            raise HTTPException(status_code=400, detail="规则编码已存在")
        row = ConstraintRule(
            rule_code=payload.rule_code,
            rule_name=payload.rule_name,
            rule_type=payload.rule_type,
            description=payload.description,
            status=payload.status,
            is_hard_constraint=payload.is_hard_constraint,
            priority_level=payload.priority_level,
            rule_expression=payload.rule_expression,
            created_by=payload.created_by,
            updated_by=payload.created_by,
        )
        db.add(row)
        db.commit()
        db.refresh(row)
        return row

    @staticmethod
    def update_rule(db: Session, rule_id: int, payload: ConstraintRuleUpdate) -> ConstraintRule:
        row = ConstraintService.get_rule(db, rule_id)
        for k, v in payload.model_dump(exclude_unset=True).items():
            setattr(row, k, v)
        db.commit()
        db.refresh(row)
        return row

    @staticmethod
    def list_rule_items(db: Session, rule_id: int) -> list[ConstraintRuleItem]:
        ConstraintService.get_rule(db, rule_id)
        stmt = (
            select(ConstraintRuleItem)
            .where(ConstraintRuleItem.rule_id == rule_id)
            .order_by(ConstraintRuleItem.item_order, ConstraintRuleItem.id)
        )
        return list(db.scalars(stmt).all())

    @staticmethod
    def upsert_rule_item(db: Session, rule_id: int, payload: ConstraintRuleItemUpsert) -> ConstraintRuleItem:
        ConstraintService.get_rule(db, rule_id)
        stmt = select(ConstraintRuleItem).where(
            and_(
                ConstraintRuleItem.rule_id == rule_id,
                ConstraintRuleItem.item_key == payload.item_key,
                ConstraintRuleItem.item_order == payload.item_order,
            )
        )
        row = db.scalar(stmt)
        if not row:
            row = ConstraintRuleItem(
                rule_id=rule_id,
                item_key=payload.item_key,
                item_order=payload.item_order,
                created_by=payload.operator,
            )
            db.add(row)

        row.item_value = payload.item_value
        row.enabled_flag = payload.enabled_flag
        row.updated_by = payload.operator
        db.commit()
        db.refresh(row)
        return row

    @staticmethod
    def save_version(db: Session, payload: ConstraintVersionSave) -> ConstraintVersion:
        exists = db.scalar(select(ConstraintVersion).where(ConstraintVersion.version_no == payload.version_no))
        if exists:
            raise HTTPException(status_code=400, detail="约束版本号已存在")

        active_rules = list(db.scalars(select(ConstraintRule).where(ConstraintRule.status == "active")).all())
        if not active_rules:
            raise HTTPException(status_code=400, detail="无可用规则，不能保存约束版本")

        version = ConstraintVersion(
            version_no=payload.version_no,
            version_name=payload.version_name,
            version_status="draft",
            published_flag=False,
            remark=payload.remark,
            created_by=payload.created_by,
            updated_by=payload.created_by,
        )
        db.add(version)
        for rule in active_rules:
            rule.version_no = payload.version_no
            rule.updated_by = payload.created_by
        db.commit()
        db.refresh(version)
        return version

    @staticmethod
    def _validate_publish(db: Session, version_no: str) -> tuple[bool, str | None]:
        rules = list(
            db.scalars(
                select(ConstraintRule).where(
                    and_(ConstraintRule.version_no == version_no, ConstraintRule.status == "active")
                )
            ).all()
        )
        if not rules:
            return False, "规则缺失，禁止发布"

        type_set = {r.rule_type for r in rules}
        missing = sorted(ConstraintService.REQUIRED_RULE_TYPES - type_set)
        if missing:
            return False, f"缺少必要规则类型: {', '.join(missing)}"

        rule_ids = [r.id for r in rules]
        items = list(
            db.scalars(
                select(ConstraintRuleItem).where(
                    and_(
                        ConstraintRuleItem.rule_id.in_(rule_ids),
                        ConstraintRuleItem.enabled_flag.is_(True),
                    )
                )
            ).all()
        )
        if not items:
            return False, "规则明细缺失，禁止发布"

        rule_type_map = {r.id: r.rule_type for r in rules}
        value_map: dict[tuple[str, str], set[str]] = {}
        for item in items:
            key = (rule_type_map[item.rule_id], item.item_key)
            value_map.setdefault(key, set()).add(item.item_value)

        conflicts = [k for k, v in value_map.items() if len(v) > 1]
        if conflicts:
            conflict_desc = "; ".join([f"{x[0]}:{x[1]}" for x in conflicts])
            return False, f"存在冲突规则: {conflict_desc}"

        return True, None

    @staticmethod
    def publish_version(db: Session, version_no: str, payload: ConstraintVersionPublish) -> ConstraintVersion:
        version = db.scalar(select(ConstraintVersion).where(ConstraintVersion.version_no == version_no))
        if not version:
            raise HTTPException(status_code=404, detail="约束版本不存在")

        ok, reason = ConstraintService._validate_publish(db, version_no)
        if not ok:
            version.version_status = "failed"
            version.updated_by = payload.published_by
            db.commit()
            ConstraintService._log(db, version_no, "failed", payload.published_by, reason)
            raise HTTPException(status_code=400, detail=reason)

        active_versions = list(
            db.scalars(select(ConstraintVersion).where(ConstraintVersion.published_flag.is_(True))).all()
        )
        for v in active_versions:
            v.published_flag = False
            v.version_status = "inactive"
            v.updated_by = payload.published_by

        version.published_flag = True
        version.version_status = "published"
        version.published_at = datetime.utcnow()
        version.published_by = payload.published_by
        version.updated_by = payload.published_by

        db.commit()
        db.refresh(version)
        ConstraintService._log(db, version_no, "success", payload.published_by, None)
        return version

    @staticmethod
    def list_versions(db: Session) -> list[ConstraintVersion]:
        return list(db.scalars(select(ConstraintVersion).order_by(ConstraintVersion.id.desc())).all())

    @staticmethod
    def current_version(db: Session) -> ConstraintVersion:
        row = db.scalar(
            select(ConstraintVersion).where(ConstraintVersion.published_flag.is_(True)).order_by(ConstraintVersion.id.desc())
        )
        if not row:
            raise HTTPException(status_code=404, detail="当前无有效约束版本")
        return row
