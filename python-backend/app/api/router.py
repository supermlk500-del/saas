from fastapi import APIRouter

from app.api.health_api import router as health_router
from app.api.inspection_api import router as inspection_router

api_router = APIRouter()
api_router.include_router(health_router, prefix="/api", tags=["health"])
api_router.include_router(inspection_router, prefix="/api/inspection", tags=["inspection"])
