from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.core.config import settings
from app.db.session import get_db
from app.schemas.ai_assistant import (
    ExplainUnassignedRequest,
    ExplainUnassignedResponse,
    SummarizeBenchmarkRequest,
    SummarizeBenchmarkResponse,
)
from app.services.ai_assistant.explain_service import UnassignedExplainService
from app.services.ai_assistant.llm_client import LLMClient
from app.services.ai_assistant.summary_service import BenchmarkSummaryService

router = APIRouter(prefix="/ai", tags=["ai-assistant"])


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


@router.post("/explain-unassigned", response_model=ExplainUnassignedResponse)
def explain_unassigned(
    payload: ExplainUnassignedRequest,
    db: Session = Depends(get_db),
) -> dict:
    try:
        return UnassignedExplainService(_client()).explain(db, payload.plan_version_id)
    except ValueError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc


@router.post("/summarize-benchmark", response_model=SummarizeBenchmarkResponse)
def summarize_benchmark(payload: SummarizeBenchmarkRequest) -> dict:
    return BenchmarkSummaryService(_client()).summarize(payload.kpi_table)
