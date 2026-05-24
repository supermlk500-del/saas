from __future__ import annotations

from datetime import datetime

from fastapi import Form
from pydantic import BaseModel, Field, field_validator


class HealthStatus(BaseModel):
    status: str
    service: str
    modelReady: bool
    modelBackend: str


class DetectionBox(BaseModel):
    label: str
    score: float
    x1: int
    y1: int
    x2: int
    y2: int


class InspectionResult(BaseModel):
    inspectType: str
    resultJudge: str
    confidenceScore: float | None = None
    defectType: str | None = None
    resultValue: str | None = None
    imageUrl: str | None = None
    sourceImageUrl: str | None = None
    boxes: list[DetectionBox] = Field(default_factory=list)


class ImageInspectionForm(BaseModel):
    planStepId: int = Field(..., ge=1)
    qcItemCode: str | None = None
    cameraId: int | None = Field(default=None, ge=1)
    sourceType: str | None = "manual"

    @classmethod
    def as_form(
        cls,
        planStepId: int = Form(...),
        qcItemCode: str | None = Form(default=None),
        cameraId: int | None = Form(default=None),
        sourceType: str | None = Form(default="manual"),
    ) -> "ImageInspectionForm":
        return cls(
            planStepId=planStepId,
            qcItemCode=qcItemCode,
            cameraId=cameraId,
            sourceType=sourceType,
        )


class FrameInspectionForm(BaseModel):
    planStepId: int = Field(..., ge=1)
    cameraId: int | None = Field(default=None, ge=1)
    frameTime: datetime | None = None

    @classmethod
    def as_form(
        cls,
        planStepId: int = Form(...),
        cameraId: int | None = Form(default=None),
        frameTime: datetime | None = Form(default=None),
    ) -> "FrameInspectionForm":
        return cls(planStepId=planStepId, cameraId=cameraId, frameTime=frameTime)


class InspectionContext(BaseModel):
    planStepId: int
    qcItemCode: str | None = None
    cameraId: int | None = None
    sourceType: str | None = None
    frameTime: datetime | None = None


class VisionBox(BaseModel):
    label: str
    score: float
    x1: int
    y1: int
    x2: int
    y2: int


class VisionInferenceResult(BaseModel):
    inspectType: str
    resultJudge: str
    confidenceScore: float | None = None
    defectType: str | None = None
    resultValue: str | None = None
    boxes: list[VisionBox] = Field(default_factory=list)

    @field_validator("resultJudge")
    @classmethod
    def validate_result_judge(cls, value: str) -> str:
        if value not in {"PASS", "FAIL", "RECHECK"}:
            raise ValueError("resultJudge must be PASS, FAIL, or RECHECK")
        return value
