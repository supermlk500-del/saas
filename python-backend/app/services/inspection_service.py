from __future__ import annotations

import logging
from pathlib import Path

from fastapi import UploadFile

from app.core.config import Settings
from app.core.exceptions import AppException
from app.schemas.inspection import (
    DetectionBox,
    FrameInspectionForm,
    InspectionContext,
    InspectionResult,
    ImageInspectionForm,
)
from app.utils.files import save_upload_file
from app.utils.image_tools import render_result_image
from app.vision.engine import VisionEngine

logger = logging.getLogger(__name__)


class InspectionService:
    def __init__(self, settings: Settings, engine: VisionEngine):
        self.settings = settings
        self.engine = engine

    async def inspect_image(self, file: UploadFile, payload: ImageInspectionForm) -> InspectionResult:
        context = InspectionContext(
            planStepId=payload.planStepId,
            qcItemCode=payload.qcItemCode,
            cameraId=payload.cameraId,
            sourceType=payload.sourceType,
        )
        return await self._inspect(file=file, context=context, inspect_type="offline")

    async def inspect_frame(self, file: UploadFile, payload: FrameInspectionForm) -> InspectionResult:
        context = InspectionContext(
            planStepId=payload.planStepId,
            cameraId=payload.cameraId,
            frameTime=payload.frameTime,
            sourceType="camera",
        )
        return await self._inspect(file=file, context=context, inspect_type="video")

    async def _inspect(
        self,
        file: UploadFile,
        context: InspectionContext,
        inspect_type: str,
    ) -> InspectionResult:
        source_path, source_url = await save_upload_file(
            upload=file,
            base_dir=self.settings.upload_dir,
            relative_root=self.settings.upload_relative_root,
            max_size_mb=self.settings.image_max_size_mb,
        )
        logger.info("Saved inspection source file: %s", source_path)

        try:
            inference = self.engine.infer(image_path=source_path, inspect_type=inspect_type, context=context)
            result_path, result_url = render_result_image(
                image_path=source_path,
                base_dir=self.settings.result_dir,
                relative_root=self.settings.result_relative_root,
                inference=inference,
            )
        except AppException:
            raise
        except Exception as exc:
            logger.exception("Inspection inference failed: %s", exc)
            raise AppException("model inference failed", code=500, status_code=500) from exc

        logger.info("Rendered inspection result file: %s", result_path)

        return InspectionResult(
            inspectType=inference.inspectType,
            resultJudge=inference.resultJudge,
            confidenceScore=inference.confidenceScore,
            defectType=inference.defectType,
            resultValue=inference.resultValue,
            imageUrl=result_url,
            sourceImageUrl=source_url,
            boxes=[DetectionBox(**box.model_dump()) for box in inference.boxes],
        )
