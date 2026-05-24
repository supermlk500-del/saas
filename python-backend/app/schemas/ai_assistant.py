from pydantic import BaseModel


class ExplainUnassignedRequest(BaseModel):
    plan_version_id: int


class ExplainUnassignedResponse(BaseModel):
    plan_version_id: int
    unassigned_count: int
    summary: str
    root_causes: list[str]
    suggestions: list[str]
    ai_powered: bool
    elapsed_ms: int


class SummarizeBenchmarkRequest(BaseModel):
    kpi_table: list[dict]


class SummarizeBenchmarkResponse(BaseModel):
    summary_markdown: str
    ai_powered: bool
    elapsed_ms: int
