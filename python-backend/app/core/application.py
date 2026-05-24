from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles

from app.api.router import api_router
from app.core.config import get_settings
from app.core.exceptions import register_exception_handlers
from app.core.logging import configure_logging
from app.services.inspection_service import InspectionService
from app.vision.engine import VisionEngine


def create_app() -> FastAPI:
    settings = get_settings()
    configure_logging(settings.log_level)
    settings.ensure_directories()

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        engine = VisionEngine(settings=settings)
        engine.initialize()
        app.state.inspection_service = InspectionService(settings=settings, engine=engine)
        yield

    app = FastAPI(
        title="Python Inspection Service",
        version="1.0.0",
        description="Independent FastAPI backend for fabric inspection inference.",
        lifespan=lifespan,
    )

    register_exception_handlers(app)
    app.include_router(api_router)
    app.mount("/photo/upload", StaticFiles(directory=settings.upload_dir), name="photo-upload")
    app.mount("/photo/results", StaticFiles(directory=settings.result_dir), name="photo-results")
    return app
