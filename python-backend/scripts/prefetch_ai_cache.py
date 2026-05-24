"""Prefetch AI assistant cache for offline demos."""
from __future__ import annotations

import json
from pathlib import Path
import sys

from sqlalchemy import desc, func, select

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from app.core.config import settings
from app.db.session import SessionLocal
from app.models import SchedulePlanVersion, ScheduleUnassignedTask
from app.services.ai_assistant.explain_service import UnassignedExplainService
from app.services.ai_assistant.llm_client import LLMClient, LLMResponse
from app.services.ai_assistant.summary_service import BenchmarkSummaryService
from scripts.benchmark_strategies import run as run_benchmark


def _client() -> LLMClient:
    return LLMClient(
        api_key=settings.llm_api_key,
        base_url=settings.llm_base_url,
        model=settings.llm_model,
        timeout_seconds=settings.llm_timeout_seconds,
        max_retries=settings.llm_max_retries,
        offline_cache_path=settings.llm_offline_cache_path,
        enabled=settings.llm_enabled,
    )


def _cache_size(path: str) -> tuple[int, int]:
    cache_path = Path(path)
    if not cache_path.exists():
        return (0, 0)
    try:
        data = json.loads(cache_path.read_text(encoding="utf-8"))
    except Exception:
        return (cache_path.stat().st_size, 0)
    return (cache_path.stat().st_size, len(data) if isinstance(data, dict) else 0)


def _latest_plan_with_unassigned(db) -> SchedulePlanVersion | None:
    return db.scalar(
        select(SchedulePlanVersion)
        .join(ScheduleUnassignedTask, ScheduleUnassignedTask.plan_version_id == SchedulePlanVersion.id)
        .group_by(SchedulePlanVersion.id)
        .order_by(desc(func.count(ScheduleUnassignedTask.id)), desc(SchedulePlanVersion.id))
        .limit(1)
    )


def main() -> None:
    client = _client()

    rows = run_benchmark()
    summary_service = BenchmarkSummaryService(client)
    summary = summary_service.summarize(rows)
    client.write_cache_entry(
        summary_service._cache_key(rows),
        LLMResponse(
            success=summary["ai_powered"],
            content=summary["summary_markdown"],
            elapsed_ms=summary["elapsed_ms"],
            fallback_used=not summary["ai_powered"],
        ),
    )

    db = SessionLocal()
    try:
        plan = _latest_plan_with_unassigned(db)
        if plan is not None:
            explain_service = UnassignedExplainService(client)
            result = explain_service.explain(db, plan.id)
            unassigned_items = explain_service._load_unassigned_items(db, plan.id)
            cache_key = explain_service._cache_key(plan.id, unassigned_items)
            content = json.dumps(
                {
                    "summary": result["summary"],
                    "root_causes": result["root_causes"],
                    "suggestions": result["suggestions"],
                },
                ensure_ascii=False,
            )
            client.write_cache_entry(
                cache_key,
                LLMResponse(
                    success=result["ai_powered"],
                    content=content,
                    elapsed_ms=result["elapsed_ms"],
                    fallback_used=not result["ai_powered"],
                ),
            )
            print(f"prefetched unassigned explanation for plan_version_id={plan.id}")
        else:
            print("no plan with unassigned tasks found; skipped unassigned explanation cache")
    finally:
        db.close()

    size, entries = _cache_size(settings.llm_offline_cache_path)
    print(f"cache_path: {settings.llm_offline_cache_path}")
    print(f"cache_size_bytes: {size}")
    print(f"cache_entries: {entries}")


if __name__ == "__main__":
    main()
