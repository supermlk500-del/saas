from fastapi import APIRouter

from app.api.api_v1.endpoints import ai_assistant, constraint, machine, order, schedule, timeline

api_router = APIRouter(prefix="/api/v1")
api_router.include_router(machine.router)
api_router.include_router(constraint.router)
api_router.include_router(schedule.router)
api_router.include_router(ai_assistant.router)
api_router.include_router(order.router)
api_router.include_router(timeline.router)
