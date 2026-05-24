from __future__ import annotations

import imghdr
import uuid
from datetime import datetime
from pathlib import Path

from fastapi import UploadFile, status

from app.core.exceptions import AppException

ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}


def _validate_image_suffix(filename: str | None) -> str:
    if not filename:
        raise AppException("file name is required")

    suffix = Path(filename).suffix.lower()
    if suffix not in ALLOWED_EXTENSIONS:
        raise AppException("unsupported image file type")
    return suffix


def _build_file_name(prefix: str, suffix: str) -> str:
    timestamp = datetime.now().strftime("%H%M%S")
    unique_tail = uuid.uuid4().hex[:12]
    return f"{prefix}_{timestamp}_{unique_tail}{suffix}"


async def save_upload_file(
    upload: UploadFile,
    base_dir: Path,
    relative_root: str,
    max_size_mb: int,
) -> tuple[Path, str]:
    suffix = _validate_image_suffix(upload.filename)
    content = await upload.read()
    if not content:
        raise AppException("uploaded file is empty")

    if len(content) > max_size_mb * 1024 * 1024:
        raise AppException(f"uploaded file exceeds {max_size_mb}MB limit", status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE)

    image_type = imghdr.what(None, h=content)
    if image_type not in {"jpeg", "png", "bmp", "webp"}:
        raise AppException("uploaded file is not a valid image")

    date_dir = datetime.now().strftime("%Y%m%d")
    target_dir = base_dir / date_dir
    target_dir.mkdir(parents=True, exist_ok=True)

    file_name = _build_file_name(prefix="source", suffix=suffix)
    target_path = target_dir / file_name
    target_path.write_bytes(content)
    return target_path, f"{relative_root}/{date_dir}/{file_name}"
