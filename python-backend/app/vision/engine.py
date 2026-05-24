from __future__ import annotations

import logging
from pathlib import Path

from app.core.config import Settings
from app.schemas.inspection import InspectionContext, VisionInferenceResult
from app.vision.base import VisionAdapter
from app.vision.fallback import FallbackHeuristicAdapter
from app.vision.ultralytics_adapter import UltralyticsAdapter

logger = logging.getLogger(__name__)


class VisionEngine:
    def __init__(self, settings: Settings):
        self.settings = settings
        self.adapter: VisionAdapter | None = None
        self.model_ready: bool = False
        self.active_backend: str = "uninitialized"
        self.model_path: Path | None = settings.resolved_model_path

    def initialize(self) -> None:
        if self.model_path:
            ultralytics_adapter = UltralyticsAdapter(
                model_path=self.model_path,
                confidence_threshold=self.settings.confidence_threshold,
            )
            if ultralytics_adapter.ready:
                self.adapter = ultralytics_adapter
                self.model_ready = True
                self.active_backend = ultralytics_adapter.name
                logger.info("Loaded inspection model: %s", self.model_path)
                return

            logger.warning(
                "Failed to activate ultralytics adapter, fallback enabled. reason=%s",
                ultralytics_adapter.reason,
            )

        fallback_adapter = FallbackHeuristicAdapter()
        self.adapter = fallback_adapter
        self.model_ready = False
        self.active_backend = fallback_adapter.name
        logger.info("Using fallback inspection adapter")

    def infer(
        self,
        image_path: Path,
        inspect_type: str,
        context: InspectionContext,
    ) -> VisionInferenceResult:
        if not self.adapter:
            raise RuntimeError("Vision engine is not initialized")
        return self.adapter.infer(image_path=image_path, inspect_type=inspect_type, context=context)
