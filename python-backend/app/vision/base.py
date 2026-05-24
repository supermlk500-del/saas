from __future__ import annotations

from abc import ABC, abstractmethod
from pathlib import Path

from app.schemas.inspection import InspectionContext, VisionInferenceResult


class VisionAdapter(ABC):
    name: str = "base"

    @property
    @abstractmethod
    def ready(self) -> bool:
        raise NotImplementedError

    @property
    @abstractmethod
    def reason(self) -> str | None:
        raise NotImplementedError

    @abstractmethod
    def infer(
        self,
        image_path: Path,
        inspect_type: str,
        context: InspectionContext,
    ) -> VisionInferenceResult:
        raise NotImplementedError
