from fastapi import APIRouter, Depends, File, Request, UploadFile

from app.core.response import success
from app.schemas.common import ApiResponse
from app.schemas.inspection import (
    FrameInspectionForm,
    InspectionResult,
    ImageInspectionForm,
)
from app.services.inspection_service import InspectionService

router = APIRouter()


def get_inspection_service(request: Request) -> InspectionService:
    return request.app.state.inspection_service


@router.post("/image", response_model=ApiResponse[InspectionResult])
async def inspect_image(
    payload: ImageInspectionForm = Depends(ImageInspectionForm.as_form),
    file: UploadFile = File(...),
    service: InspectionService = Depends(get_inspection_service),
) -> ApiResponse[InspectionResult]:
    result = await service.inspect_image(file=file, payload=payload)
    return success(result)


@router.post("/frame", response_model=ApiResponse[InspectionResult])
async def inspect_frame(
    payload: FrameInspectionForm = Depends(FrameInspectionForm.as_form),
    file: UploadFile = File(...),
    service: InspectionService = Depends(get_inspection_service),
) -> ApiResponse[InspectionResult]:
    result = await service.inspect_frame(file=file, payload=payload)
    return success(result)
