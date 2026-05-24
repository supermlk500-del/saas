from __future__ import annotations

from pathlib import Path

from app.schemas.inspection import InspectionContext, VisionBox, VisionInferenceResult
from app.vision.base import VisionAdapter


class UltralyticsAdapter(VisionAdapter):
    name = "ultralytics-yolo"

    def __init__(self, model_path: Path, confidence_threshold: float):
        self.model_path = model_path
        self.confidence_threshold = confidence_threshold
        self._ready = False
        self._reason: str | None = None
        self.model = None
        self._load()

    @property
    def ready(self) -> bool:
        return self._ready

    @property
    def reason(self) -> str | None:
        return self._reason

    def _load(self) -> None:
        if not self.model_path.exists():
            self._reason = f"model not found: {self.model_path}"
            return

        try:
            from ultralytics import YOLO
        except ImportError:
            self._reason = "ultralytics is not installed"
            return

        try:
            self.model = YOLO(str(self.model_path))
            self._ready = True
        except Exception as exc:  # pragma: no cover - runtime dependent
            self._reason = f"model load failed: {exc}"

    def infer(
        self,
        image_path: Path,
        inspect_type: str,
        context: InspectionContext,
    ) -> VisionInferenceResult:
        if not self._ready or self.model is None:
            raise RuntimeError(self._reason or "ultralytics model is not ready")

        results = self.model.predict(
            source=str(image_path),
            conf=self.confidence_threshold,
            verbose=False,
        )
        boxes: list[VisionBox] = []

        for result in results:
            names = result.names
            for item in result.boxes:
                score = float(item.conf[0])
                cls_index = int(item.cls[0])
                x1, y1, x2, y2 = item.xyxy[0].tolist()
                boxes.append(
                    VisionBox(
                        label=str(names.get(cls_index, cls_index)),
                        score=round(score, 4),
                        x1=int(x1),
                        y1=int(y1),
                        x2=int(x2),
                        y2=int(y2),
                    )
                )

        if not boxes:
            return VisionInferenceResult(
                inspectType=inspect_type,
                resultJudge="PASS",
                confidenceScore=0.99,
                defectType=None,
                resultValue="OK",
                boxes=[],
            )

        top_box = max(boxes, key=lambda item: item.score)
        judge = "FAIL" if top_box.score >= max(self.confidence_threshold, 0.65) else "RECHECK"
        return VisionInferenceResult(
            inspectType=inspect_type,
            resultJudge=judge,
            confidenceScore=top_box.score,
            defectType=top_box.label,
            resultValue=f"{top_box.label}:{top_box.score:.2f}",
            boxes=boxes,
        )
