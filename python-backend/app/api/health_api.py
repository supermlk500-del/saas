from fastapi import APIRouter, Request

from app.core.response import success
from app.schemas.common import ApiResponse
from app.schemas.inspection import HealthStatus

router = APIRouter()


@router.get("/health", response_model=ApiResponse[HealthStatus])
async def health_check(request: Request) -> ApiResponse[HealthStatus]:
    service = request.app.state.inspection_service
    return success(
        HealthStatus(
            status="UP",
            service="python-inspection-service",
            modelReady=service.engine.model_ready,
            modelBackend=service.engine.active_backend,
        )
    )
