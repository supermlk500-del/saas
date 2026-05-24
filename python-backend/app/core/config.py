from __future__ import annotations

from functools import lru_cache
from pathlib import Path

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    service_name: str = "python-inspection-service"
    host: str = "0.0.0.0"
    port: int = 8001
    log_level: str = "INFO"
    image_max_size_mb: int = 20
    confidence_threshold: float = 0.5
    workspace_root: Path = Field(default_factory=lambda: Path(__file__).resolve().parents[3])
    upload_dir: Path = Field(default_factory=lambda: Path(__file__).resolve().parents[3] / "photo" / "upload")
    result_dir: Path = Field(default_factory=lambda: Path(__file__).resolve().parents[3] / "photo" / "results")
    model_dir: Path = Field(default_factory=lambda: Path(__file__).resolve().parents[2] / "models")
    model_path: Path | None = None
    frontend_model_path: Path = Field(
        default_factory=lambda: Path(__file__).resolve().parents[3] / "frontend" / "best.pt"
    )

    model_config = SettingsConfigDict(
        env_prefix="INSPECTION_",
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    def ensure_directories(self) -> None:
        self.upload_dir.mkdir(parents=True, exist_ok=True)
        self.result_dir.mkdir(parents=True, exist_ok=True)
        self.model_dir.mkdir(parents=True, exist_ok=True)

    @property
    def upload_relative_root(self) -> str:
        return "photo/upload"

    @property
    def result_relative_root(self) -> str:
        return "photo/results"

    @property
    def resolved_model_path(self) -> Path | None:
        if self.model_path:
            candidate = Path(self.model_path)
            if candidate.exists():
                return candidate

        local_model = self.model_dir / "best.pt"
        if local_model.exists():
            return local_model

        if self.frontend_model_path.exists():
            return self.frontend_model_path

        return None


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()
