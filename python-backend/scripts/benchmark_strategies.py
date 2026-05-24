"""Run in-memory KPI comparison for all solver strategies.

Usage:
    python scripts/benchmark_strategies.py
"""
from __future__ import annotations

from datetime import datetime
from pathlib import Path
import sys

from sqlalchemy import and_, desc, func, select
from sqlalchemy.orm import Session

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from app.core.status import SnapshotStatus
from app.db.session import SessionLocal
from app.models import (
    ConstraintVersion,
    Machine,
    MachineCapabilitySnapshot,
    ScheduleTask,
)
from app.services.schedule_service import ScheduleService
from app.solver.problem import MachineState, ScheduleProblem
from app.solver.strategies import SOLVER_STRATEGIES, get_solver

REPORT_PATH = ROOT.parent / "开发过程" / "演示数据" / "strategy_benchmark_报告.md"


def _latest_snapshot_no(db: Session) -> str:
    snapshot_no = db.scalar(
        select(MachineCapabilitySnapshot.snapshot_no)
        .where(MachineCapabilitySnapshot.snapshot_no.like("SNAP-LIVE-%"))
        .order_by(desc(MachineCapabilitySnapshot.id))
        .limit(1)
    )
    if not snapshot_no:
        raise RuntimeError("未找到 SNAP-LIVE-* 快照，请先运行 scripts/reset_and_seed_live_demo.py")
    return snapshot_no


def _latest_constraint_no(db: Session) -> str:
    constraint_no = db.scalar(
        select(ConstraintVersion.version_no)
        .where(
            and_(
                ConstraintVersion.version_no.like("CV-LIVE-%"),
                ConstraintVersion.published_flag.is_(True),
            )
        )
        .order_by(desc(ConstraintVersion.id))
        .limit(1)
    )
    if not constraint_no:
        raise RuntimeError("未找到已发布的 CV-LIVE-* 约束版本，请先运行 scripts/reset_and_seed_live_demo.py")
    return constraint_no


def _full_solver_cfg(constraint_values: dict[str, dict[str, str]]) -> dict[str, dict[str, str]]:
    """构造完整 solver 配置，不应用 ScheduleService.DISABLED_RUNTIME_RULE_TYPES 过滤。

    service 默认会把 changeover、continuous_limit 在运行时清空，导致 benchmark 中
    所有策略评分维度趋同（换型小时永远为 0），看不出策略差异。
    benchmark 是诊断脚本，必须使用完整约束集来还原策略真实表现。
    """
    return {
        "machine_limit": constraint_values.get("machine_limit", {}),
        "continuous_limit": constraint_values.get("continuous_limit", {}),
        "due_priority": constraint_values.get("due_priority", {}),
        "manual_lock": constraint_values.get("manual_lock", {}),
        "changeover": constraint_values.get("changeover", {}),
        "dye_color_changeover": constraint_values.get("dye_color_changeover", {}),
    }


def build_problem(db: Session, snapshot_no: str, constraint_no: str) -> ScheduleProblem:
    tasks = list(
        db.scalars(
            select(ScheduleTask)
            .where(ScheduleTask.task_status != "cancelled")
            .order_by(ScheduleTask.id)
        ).all()
    )
    snapshots = list(
        db.scalars(
            select(MachineCapabilitySnapshot)
            .where(
                and_(
                    MachineCapabilitySnapshot.snapshot_no == snapshot_no,
                    MachineCapabilitySnapshot.snapshot_status == SnapshotStatus.GENERATED,
                    MachineCapabilitySnapshot.available_flag.is_(True),
                )
            )
            .order_by(MachineCapabilitySnapshot.id)
        ).all()
    )
    machine_ids = [snapshot.machine_id for snapshot in snapshots]
    machine_map = {
        machine.id: machine
        for machine in db.scalars(select(Machine).where(Machine.id.in_(machine_ids))).all()
    }
    states = [
        MachineState(
            snapshot=snapshot,
            machine=machine_map[snapshot.machine_id],
            next_available=datetime.utcnow(),
            task_count=0,
            last_color=None,
            last_fabric=None,
        )
        for snapshot in snapshots
        if snapshot.machine_id in machine_map
    ]
    constraint_values, hard_types, soft_types = ScheduleService.load_constraints(db, constraint_no)
    return ScheduleProblem(
        tasks=tasks,
        machine_states=states,
        constraint_cfg=_full_solver_cfg(constraint_values),
        hard_types=hard_types,
        soft_types=soft_types,
        plan_version_id=0,
        operator="benchmark",
        context="benchmark",
    )


def _format_table(rows: list[dict[str, object]]) -> str:
    header = "| 策略 | 已排 | 未排 | 准交率 | 总换型h | wash_count | 最大延期天 |"
    sep = "|---|---:|---:|---:|---:|---:|---:|"
    body = [
        "| {strategy} | {assigned_count} | {unassigned_count} | {on_time_rate:.2f}% | {total_changeover_hours:.2f} | {wash_count} | {max_late_days} |".format(
            **row
        )
        for row in rows
    ]
    return "\n".join([header, sep, *body])


def append_ai_summary(report_path: Path, kpi_table: list[dict[str, object]]) -> None:
    """Append AI/fallback summary without affecting benchmark success."""
    try:
        from app.core.config import settings
        from app.services.ai_assistant.llm_client import LLMClient
        from app.services.ai_assistant.summary_service import BenchmarkSummaryService

        client = LLMClient(
            api_key=settings.llm_api_key,
            base_url=settings.llm_base_url,
            model=settings.llm_model,
            timeout_seconds=settings.llm_timeout_seconds,
            max_retries=settings.llm_max_retries,
            offline_cache_path=settings.llm_offline_cache_path,
            enabled=settings.llm_enabled,
        )
        summary = BenchmarkSummaryService(client).summarize(kpi_table)
        with report_path.open("a", encoding="utf-8") as f:
            f.write("\n\n## AI 总结\n\n")
            f.write(summary["summary_markdown"])
            f.write(
                f"\n\n*ai_powered: {summary['ai_powered']}, "
                f"elapsed: {summary['elapsed_ms']}ms*\n"
            )
    except Exception as exc:  # noqa: BLE001 - benchmark must remain stable
        with report_path.open("a", encoding="utf-8") as f:
            f.write("\n\n## AI 总结\n\n")
            f.write(f"AI 总结暂不可用，benchmark 主流程已正常完成。错误：{exc}\n")


def run() -> list[dict[str, object]]:
    db = SessionLocal()
    try:
        snapshot_no = _latest_snapshot_no(db)
        constraint_no = _latest_constraint_no(db)
        rows: list[dict[str, object]] = []
        for strategy_name in SOLVER_STRATEGIES:
            problem = build_problem(db, snapshot_no, constraint_no)
            result = get_solver(strategy_name).solve(problem)
            kpi = result.kpi
            rows.append(
                {
                    "strategy": strategy_name,
                    "assigned_count": kpi.assigned_count,
                    "unassigned_count": kpi.unassigned_count,
                    "on_time_rate": kpi.on_time_rate,
                    "total_changeover_hours": kpi.total_changeover_hours,
                    "wash_count": kpi.wash_count,
                    "max_late_days": kpi.max_late_days,
                }
            )

        task_count = db.scalar(
            select(func.count(ScheduleTask.id)).where(ScheduleTask.task_status != "cancelled")
        )
        machine_count = len(
            db.scalars(
                select(MachineCapabilitySnapshot.id).where(
                    and_(
                        MachineCapabilitySnapshot.snapshot_no == snapshot_no,
                        MachineCapabilitySnapshot.available_flag.is_(True),
                    )
                )
            ).all()
        )
        generated_at = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        report = "\n".join(
            [
                "# 多策略 KPI 对比报告",
                "",
                f"数据日期：{generated_at}",
                f"数据来源：scripts/reset_and_seed_live_demo.py",
                f"快照版本：{snapshot_no}",
                f"约束版本：{constraint_no}",
                f"数据规模：任务约 {task_count or 0} 条，可用机台 {machine_count} 台",
                "",
                "## 结果",
                "",
                _format_table(rows),
                "",
                "## 观察",
                "- benchmark 走内存版 solver，不写入计划表，适合作为策略横向比较基线。",
                "- 本脚本**绕过 ScheduleService.DISABLED_RUNTIME_RULE_TYPES**，强制启用 changeover 与 continuous_limit，",
                "  确保策略差异可见；线上 service 的运行时行为不受影响。",
                "- 默认策略 greedy_eft 保持历史排序行为，但换型小时数会因为本脚本启用了 changeover 而非零，",
                "  与默认 service 输出可能不同，这是预期的。",
            ]
        )
        REPORT_PATH.parent.mkdir(parents=True, exist_ok=True)
        REPORT_PATH.write_text(report, encoding="utf-8")
        append_ai_summary(REPORT_PATH, rows)

        print(_format_table(rows))
        print(f"\nreport: {REPORT_PATH}")
        return rows
    finally:
        db.close()


if __name__ == "__main__":
    run()
