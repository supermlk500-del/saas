from __future__ import annotations

import json
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any


@dataclass
class LLMResponse:
    success: bool
    content: str
    elapsed_ms: int
    error: str | None = None
    fallback_used: bool = False


class LLMClient:
    """OpenAI-compatible LLM client with offline-cache fallback."""

    def __init__(
        self,
        api_key: str,
        base_url: str,
        model: str,
        timeout_seconds: float = 3.0,
        max_retries: int = 1,
        offline_cache_path: str | None = None,
        enabled: bool = False,
    ) -> None:
        self.api_key = api_key
        self.base_url = base_url.rstrip("/")
        self.model = model
        self.timeout_seconds = timeout_seconds
        self.max_retries = max(0, max_retries)
        self.offline_cache_path = Path(offline_cache_path) if offline_cache_path else None
        self.enabled = enabled

    def chat(
        self,
        prompt: str,
        *,
        json_mode: bool = False,
        cache_key: str | None = None,
    ) -> LLMResponse:
        """Call an LLM. Cache hits return before any network request."""
        started = time.perf_counter()
        if cache_key:
            cached = self._try_offline_fallback(cache_key)
            if cached is not None:
                return cached

        if not self.enabled:
            return LLMResponse(
                success=False,
                content="",
                elapsed_ms=self._elapsed_ms(started),
                error="llm_disabled",
                fallback_used=True,
            )
        if not self.api_key:
            return LLMResponse(
                success=False,
                content="",
                elapsed_ms=self._elapsed_ms(started),
                error="missing_api_key",
                fallback_used=True,
            )

        last_error = ""
        for _attempt in range(self.max_retries + 1):
            try:
                import httpx

                response = httpx.post(
                    f"{self.base_url}/chat/completions",
                    headers={
                        "Authorization": f"Bearer {self.api_key}",
                        "Content-Type": "application/json",
                    },
                    json=self._payload(prompt, json_mode),
                    timeout=self.timeout_seconds,
                )
                response.raise_for_status()
                data = response.json()
                content = str(data["choices"][0]["message"]["content"]).strip()
                llm_response = LLMResponse(
                    success=True,
                    content=content,
                    elapsed_ms=self._elapsed_ms(started),
                    fallback_used=False,
                )
                if cache_key:
                    self.write_cache_entry(cache_key, llm_response)
                return llm_response
            except Exception as exc:  # noqa: BLE001 - user-facing fallback path
                last_error = str(exc)

        return LLMResponse(
            success=False,
            content="",
            elapsed_ms=self._elapsed_ms(started),
            error=last_error or "llm_call_failed",
            fallback_used=True,
        )

    def _try_offline_fallback(self, cache_key: str) -> LLMResponse | None:
        cache = self._read_cache()
        raw = cache.get(cache_key)
        if raw is None:
            return None
        if isinstance(raw, dict):
            return LLMResponse(
                success=bool(raw.get("success", True)),
                content=str(raw.get("content", "")),
                elapsed_ms=0,
                error=raw.get("error"),
                fallback_used=bool(raw.get("fallback_used", True)),
            )
        return LLMResponse(success=True, content=str(raw), elapsed_ms=0, fallback_used=True)

    def write_cache_entry(self, cache_key: str, response: LLMResponse) -> None:
        if not self.offline_cache_path:
            return
        cache = self._read_cache()
        cache[cache_key] = {
            "success": response.success,
            "content": response.content,
            "error": response.error,
            "fallback_used": response.fallback_used,
        }
        self.offline_cache_path.parent.mkdir(parents=True, exist_ok=True)
        self.offline_cache_path.write_text(
            json.dumps(cache, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )

    def _read_cache(self) -> dict[str, Any]:
        if not self.offline_cache_path or not self.offline_cache_path.exists():
            return {}
        try:
            data = json.loads(self.offline_cache_path.read_text(encoding="utf-8"))
        except Exception:
            return {}
        return data if isinstance(data, dict) else {}

    def _payload(self, prompt: str, json_mode: bool) -> dict[str, Any]:
        payload: dict[str, Any] = {
            "model": self.model,
            "messages": [
                {
                    "role": "system",
                    "content": "你是排产系统解释助手，只解释已有数据，不编造排产决策。",
                },
                {"role": "user", "content": prompt},
            ],
            "temperature": 0.2,
        }
        if json_mode:
            payload["response_format"] = {"type": "json_object"}
        return payload

    @staticmethod
    def _elapsed_ms(started: float) -> int:
        return int((time.perf_counter() - started) * 1000)
