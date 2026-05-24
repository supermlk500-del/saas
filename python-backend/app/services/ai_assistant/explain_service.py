from __future__ import annotations

import json
from collections import Counter

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models import (
    ConstraintRule,
    Machine,
    MachineCapabilitySnapshot,
    SchedulePlanVersion,
    ScheduleTask,
    ScheduleUnassignedTask,
)
from app.services.ai_assistant.llm_client import LLMClient, LLMResponse
from app.services.ai_assistant.prompt_templates import render_unassigned_explanation_prompt


RULE_BASED_EXPLANATIONS = {
    "machine_route_mismatch": {
        "summary": "未排入订单的核心原因是工艺路线无匹配机台。",
        "suggestion": "建议核对订单 process_route 字段，或补充对应机台类型。",
    },
    "machine_capacity_full": {
        "summary": "候选机台均已达到连续生产上限。",
        "suggestion": "建议增加可用机台，或调整 continuous_limit 中的连续生产上限。",
    },
    "color_not_allowed_on_dedicated_vat": {
        "summary": "固定专用染缸只接受其专属颜色，部分订单颜色被拦截。",
        "suggestion": "建议检查订单颜色是否应走通用染缸，或调整 dedicated_machine_map 规则。",
    },
    "machine_speed_invalid": {
        "summary": "候选机台速度参数无效，系统无法计算生产时长。",
        "suggestion": "建议补齐机台能力参数 speed_value，并重新生成能力快照。",
    },
    "machine_no_candidate": {
        "summary": "当前没有满足所有硬约束的候选机台。",
        "suggestion": "建议检查机台可用状态、工艺路线、颜色专用缸和约束版本配置。",
    },
    "constraint_due_limit": {
        "summary": "受交期硬约束影响，当前候选机台无法按期完成。",
        "suggestion": "建议评估交期是否可协商，或启用加班/增加产能。",
    },
    "task_min_batch": {
        "summary": "订单数量低于最小起排批量。",
        "suggestion": "建议合并同类订单，或调整最小起排批量规则。",
    },
    "constraint_fabric_scope": {
        "summary": "订单布种不在当前约束允许范围内。",
        "suggestion": "建议核对 fabric_type，或扩充 allowed_fabric_types 配置。",
    },
    "machine_type_mismatch": {
        "summary": "候选机台类型不在允许范围内。",
        "suggestion": "建议核对 allow_machine_types 与机台类型配置是否一致。",
    },
    "constraint_manual_lock": {
        "summary": "订单命中人工锁定条件，当前不允许自动重排。",
        "suggestion": "建议确认锁定原因，必要时解除 lock_order_nos 配置。",
    },
}


class UnassignedExplainService:
    """Read-only explanation service for unassigned schedule tasks."""

    def __init__(self, llm_client: LLMClient) -> None:
        self.llm = llm_client

    def explain(self, db: Session, plan_version_id: int) -> dict:
        plan = db.get(SchedulePlanVersion, plan_version_id)
        if plan is None:
            raise ValueError(f"plan_version_id not found: {plan_version_id}")

        unassigned_items = self._load_unassigned_items(db, plan_version_id)
        machine_summary = self._machine_summary(db, plan)
        constraint_summary = self._constraint_summary(db, plan)
        cache_key = self._cache_key(plan_version_id, unassigned_items)

        prompt = render_unassigned_explanation_prompt(
            unassigned_items,
            machine_summary,
            constraint_summary,
        )
        response = self.llm.chat(prompt, json_mode=True, cache_key=cache_key)
        parsed = self._parse_llm_json(response)
        if parsed is None:
            parsed = self._rule_based_explanation(unassigned_items)
            ai_powered = False
        else:
            ai_powered = response.success and not response.fallback_used

        return {
            "plan_version_id": plan_version_id,
            "unassigned_count": len(unassigned_items),
            "summary": parsed["summary"],
            "root_causes": parsed["root_causes"],
            "suggestions": parsed["suggestions"],
            "ai_powered": ai_powered,
            "elapsed_ms": response.elapsed_ms,
        }

    def _load_unassigned_items(self, db: Session, plan_version_id: int) -> list[dict]:
        rows = list(
            db.execute(
                select(ScheduleUnassignedTask, ScheduleTask)
                .join(ScheduleTask, ScheduleTask.id == ScheduleUnassignedTask.task_id)
                .where(ScheduleUnassignedTask.plan_version_id == plan_version_id)
                .order_by(ScheduleUnassignedTask.id)
            ).all()
        )
        return [
            {
                "task_id": unassigned.task_id,
                "order_no": task.order_no,
                "fabric_type": task.fabric_type,
                "color_code": task.color_code,
                "process_route": task.process_route,
                "order_quantity": task.order_quantity,
                "due_date": task.due_date,
                "reason_code": unassigned.reason_code,
                "reason_desc": unassigned.reason_desc,
            }
            for unassigned, task in rows
        ]

    def _machine_summary(self, db: Session, plan: SchedulePlanVersion) -> dict:
        active_count = db.scalar(select(func.count(Machine.id)).where(Machine.status == "active")) or 0
        snapshot_count = (
            db.scalar(
                select(func.count(MachineCapabilitySnapshot.id)).where(
                    MachineCapabilitySnapshot.snapshot_no == plan.snapshot_version_no,
                    MachineCapabilitySnapshot.available_flag.is_(True),
                )
            )
            or 0
        )
        type_rows = db.execute(
            select(Machine.machine_type, func.count(Machine.id))
            .where(Machine.status == "active")
            .group_by(Machine.machine_type)
        ).all()
        return {
            "active_machine_count": active_count,
            "available_snapshot_count": snapshot_count,
            "machine_type_counts": {str(k): int(v) for k, v in type_rows},
        }

    def _constraint_summary(self, db: Session, plan: SchedulePlanVersion) -> dict:
        rows = db.execute(
            select(ConstraintRule.rule_type, ConstraintRule.is_hard_constraint, func.count(ConstraintRule.id))
            .where(ConstraintRule.version_no == plan.constraint_version_no)
            .group_by(ConstraintRule.rule_type, ConstraintRule.is_hard_constraint)
        ).all()
        return {
            "constraint_version_no": plan.constraint_version_no,
            "rules": [
                {"rule_type": rule_type, "is_hard": bool(is_hard), "count": int(count)}
                for rule_type, is_hard, count in rows
            ],
        }

    def _parse_llm_json(self, response: LLMResponse) -> dict | None:
        if not response.success or not response.content:
            return None
        try:
            data = json.loads(response.content)
        except Exception:
            return None
        if not isinstance(data, dict):
            return None
        summary = str(data.get("summary", "")).strip()
        root_causes = data.get("root_causes")
        suggestions = data.get("suggestions")
        if not summary or not isinstance(root_causes, list) or not isinstance(suggestions, list):
            return None
        return {
            "summary": summary,
            "root_causes": [str(x) for x in root_causes],
            "suggestions": [str(x) for x in suggestions],
        }

    def _rule_based_explanation(self, unassigned_items: list[dict]) -> dict:
        if not unassigned_items:
            return {
                "summary": "本计划所有订单均已排入，暂无未排入订单需要处理。",
                "root_causes": ["未发现未排入订单。"],
                "suggestions": ["AI 建议，仅供参考：可继续关注准交率、换型小时和洗缸次数。"],
            }

        counter = Counter(str(item.get("reason_code") or "machine_no_candidate") for item in unassigned_items)
        root_causes: list[str] = []
        suggestions: list[str] = []
        for reason_code, count in counter.most_common():
            explanation = RULE_BASED_EXPLANATIONS.get(
                reason_code,
                {
                    "summary": f"存在未识别原因 {reason_code}。",
                    "suggestion": "建议查看未排入明细中的 reason_desc，并补充规则解释字典。",
                },
            )
            root_causes.append(f"{explanation['summary']}（{count} 单，reason_code={reason_code}）")
            suggestions.append(f"AI 建议，仅供参考：{explanation['suggestion']}")

        top = counter.most_common(1)[0][0]
        top_summary = RULE_BASED_EXPLANATIONS.get(top, RULE_BASED_EXPLANATIONS["machine_no_candidate"])["summary"]
        return {
            "summary": f"本次有 {len(unassigned_items)} 单未排入，主要原因是：{top_summary}",
            "root_causes": root_causes,
            "suggestions": suggestions,
        }

    @staticmethod
    def _cache_key(plan_version_id: int, unassigned_items: list[dict]) -> str:
        reason_part = ",".join(sorted(str(item.get("reason_code", "")) for item in unassigned_items))
        return f"unassigned:{plan_version_id}:{len(unassigned_items)}:{reason_part}"
