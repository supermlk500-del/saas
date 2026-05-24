from __future__ import annotations

from datetime import datetime
from pathlib import Path

from PIL import Image, ImageDraw

from app.schemas.inspection import VisionInferenceResult
from app.utils.files import _build_file_name


def render_result_image(
    image_path: Path,
    base_dir: Path,
    relative_root: str,
    inference: VisionInferenceResult,
) -> tuple[Path, str]:
    image = Image.open(image_path).convert("RGB")
    draw = ImageDraw.Draw(image)

    color = {
        "PASS": "#1f9d55",
        "FAIL": "#d64545",
        "RECHECK": "#f0a202",
    }.get(inference.resultJudge, "#1677ff")

    for box in inference.boxes:
        draw.rectangle((box.x1, box.y1, box.x2, box.y2), outline=color, width=3)
        label = f"{box.label} {box.score:.2f}"
        draw.text((box.x1 + 4, max(4, box.y1 - 16)), label, fill=color)

    summary = f"{inference.inspectType} | {inference.resultJudge}"
    if inference.resultValue:
        summary = f"{summary} | {inference.resultValue}"
    draw.text((12, 12), summary, fill=color)

    date_dir = datetime.now().strftime("%Y%m%d")
    target_dir = base_dir / date_dir
    target_dir.mkdir(parents=True, exist_ok=True)

    output_name = _build_file_name(prefix="result", suffix=".jpg")
    output_path = target_dir / output_name
    image.save(output_path, quality=95)
    return output_path, f"{relative_root}/{date_dir}/{output_name}"
