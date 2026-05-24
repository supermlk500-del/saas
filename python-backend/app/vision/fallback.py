from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image

from app.schemas.inspection import InspectionContext, VisionBox, VisionInferenceResult
from app.vision.base import VisionAdapter


class FallbackHeuristicAdapter(VisionAdapter):
    name = "fallback-heuristic"

    @property
    def ready(self) -> bool:
        return True

    @property
    def reason(self) -> str | None:
        return None

    def infer(
        self,
        image_path: Path,
        inspect_type: str,
        context: InspectionContext,
    ) -> VisionInferenceResult:
        image = Image.open(image_path).convert("L")
        gray = np.asarray(image)
        dark_mask = gray < 40
        bright_mask = gray > 235
        suspicious_mask = dark_mask | bright_mask

        suspicious_ratio = float(suspicious_mask.mean())
        contrast = float(gray.std())

        if suspicious_ratio >= 0.01:
            coords = np.argwhere(suspicious_mask)
            y1, x1 = coords.min(axis=0)
            y2, x2 = coords.max(axis=0)
            score = round(min(0.55 + suspicious_ratio * 8, 0.95), 4)
            label = "surface_defect" if suspicious_ratio < 0.04 else "hole"
            result_value = f"{label}:{score:.2f}"
            return VisionInferenceResult(
                inspectType=inspect_type,
                resultJudge="FAIL",
                confidenceScore=score,
                defectType=label,
                resultValue=result_value,
                boxes=[
                    VisionBox(
                        label=label,
                        score=score,
                        x1=int(x1),
                        y1=int(y1),
                        x2=int(x2),
                        y2=int(y2),
                    )
                ],
            )

        if contrast < 12:
            score = round(max(0.45, 0.7 - contrast / 40), 4)
            return VisionInferenceResult(
                inspectType=inspect_type,
                resultJudge="RECHECK",
                confidenceScore=score,
                defectType="low_contrast",
                resultValue=f"low_contrast:{score:.2f}",
                boxes=[],
            )

        return VisionInferenceResult(
            inspectType=inspect_type,
            resultJudge="PASS",
            confidenceScore=0.98,
            defectType=None,
            resultValue="OK",
            boxes=[],
        )
