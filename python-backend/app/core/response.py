from typing import TypeVar

from app.schemas.common import ApiResponse

T = TypeVar("T")


def success(data: T, msg: str = "success") -> ApiResponse[T]:
    return ApiResponse(code=200, msg=msg, data=data)
