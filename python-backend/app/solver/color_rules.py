from __future__ import annotations

from dataclasses import dataclass, field


def _norm(value: object | None) -> str:
    return str(value or "").strip().upper()


def _as_bool(value: object | None, default: bool = False) -> bool:
    if value is None:
        return default
    return str(value).strip().lower() in {"1", "true", "yes", "y", "on"}


def _as_float(value: object | None, default: float) -> float:
    try:
        return float(str(value).strip()) if value not in {None, ""} else default
    except Exception:
        return default


def _as_int(value: object | None, default: int) -> int:
    try:
        return int(str(value).strip()) if value not in {None, ""} else default
    except Exception:
        return default


def _split_items(raw: object | None) -> list[str]:
    if raw is None:
        return []
    return [item.strip() for item in str(raw).replace(",", ";").split(";") if item.strip()]


def _parse_map(raw: object | None) -> dict[str, str]:
    parsed: dict[str, str] = {}
    for item in _split_items(raw):
        if ":" not in item:
            continue
        key, value = item.split(":", 1)
        key = _norm(key)
        value = _norm(value)
        if key and value:
            parsed[key] = value
    return parsed


def _parse_int_map(raw: object | None) -> dict[str, int]:
    parsed: dict[str, int] = {}
    for key, value in _parse_map(raw).items():
        try:
            parsed[key] = int(value)
        except Exception:
            continue
    return parsed


def _parse_pair_set(raw: object | None) -> set[tuple[str, str]]:
    parsed: set[tuple[str, str]] = set()
    for item in _split_items(raw):
        if ">" not in item:
            continue
        left, right = item.split(">", 1)
        left = _norm(left)
        right = _norm(right)
        if left and right:
            parsed.add((left, right))
    return parsed


def _parse_matrix(raw: object | None) -> dict[tuple[str, str], float]:
    parsed: dict[tuple[str, str], float] = {}
    for item in _split_items(raw):
        if ">" not in item or ":" not in item:
            continue
        pair, value = item.split(":", 1)
        left, right = pair.split(">", 1)
        left = _norm(left)
        right = _norm(right)
        if not left or not right:
            continue
        try:
            parsed[(left, right)] = float(value.strip())
        except Exception:
            continue
    return parsed


@dataclass
class ColorRules:
    dedicated_machine_map: dict[str, str] = field(default_factory=dict)
    dedicated_strict: bool = True
    color_family_map: dict[str, str] = field(default_factory=dict)
    color_depth_map: dict[str, int] = field(default_factory=dict)
    color_undertone_map: dict[str, str] = field(default_factory=dict)
    color_change_matrix: dict[tuple[str, str], float] = field(default_factory=dict)
    default_change_loss: float = 0.5
    same_color_loss: float = 0.0
    cross_family_penalty: float = 1.0
    light_to_dark_loss: float = 0.3
    dark_to_light_loss: float = 2.0
    undertone_change_loss: float = 1.5
    undertone_requires_wash: bool = True
    mandatory_wash_pairs: set[tuple[str, str]] = field(default_factory=set)
    wash_duration_hours: float = 2.0
    max_continuous_same_color: int = 3
    max_continuous_same_family: int = 6
    continuous_violation_as_hard: bool = False

    @classmethod
    def from_cfg(cls, cfg: dict[str, str] | None) -> "ColorRules":
        cfg = cfg or {}
        return cls(
            dedicated_machine_map=_parse_map(cfg.get("dedicated_machine_map")),
            dedicated_strict=_as_bool(cfg.get("dedicated_strict"), True),
            color_family_map=_parse_map(cfg.get("color_family_map")),
            color_depth_map=_parse_int_map(cfg.get("color_depth_map")),
            color_undertone_map=_parse_map(cfg.get("color_undertone_map")),
            color_change_matrix=_parse_matrix(cfg.get("color_change_matrix")),
            default_change_loss=_as_float(cfg.get("default_change_loss"), 0.5),
            same_color_loss=_as_float(cfg.get("same_color_loss"), 0.0),
            cross_family_penalty=_as_float(cfg.get("cross_family_penalty"), 1.0),
            light_to_dark_loss=_as_float(cfg.get("light_to_dark_loss"), 0.3),
            dark_to_light_loss=_as_float(cfg.get("dark_to_light_loss"), 2.0),
            undertone_change_loss=_as_float(cfg.get("undertone_change_loss"), 1.5),
            undertone_requires_wash=_as_bool(cfg.get("undertone_requires_wash"), True),
            mandatory_wash_pairs=_parse_pair_set(cfg.get("mandatory_wash_pairs")),
            wash_duration_hours=_as_float(cfg.get("wash_duration_hours"), 2.0),
            max_continuous_same_color=_as_int(cfg.get("max_continuous_same_color"), 3),
            max_continuous_same_family=_as_int(cfg.get("max_continuous_same_family"), 6),
            continuous_violation_as_hard=_as_bool(
                cfg.get("continuous_violation_as_hard"),
                False,
            ),
        )

    def family_of(self, color: str | None) -> str:
        color_code = _norm(color)
        return self.color_family_map.get(color_code, color_code)

    def depth_of(self, color: str | None) -> int | None:
        return self.color_depth_map.get(_norm(color))

    def undertone_of(self, color: str | None) -> str:
        return self.color_undertone_map.get(_norm(color), "")

    def compute_changeover_hours(
        self,
        prev_color: str | None,
        curr_color: str,
        machine_code: str,
    ) -> tuple[float, bool]:
        prev = _norm(prev_color)
        curr = _norm(curr_color)
        if not prev or not curr:
            return (0.0, False)
        if prev == curr:
            return (self.same_color_loss, False)

        pair = (prev, curr)
        if pair in self.color_change_matrix:
            matrix_hours = self.color_change_matrix[pair]
            wash_required = pair in self.mandatory_wash_pairs
            return (matrix_hours, wash_required)

        extra = self.default_change_loss
        wash_required = False

        if self.family_of(prev) != self.family_of(curr):
            extra += self.cross_family_penalty

        prev_depth = self.depth_of(prev)
        curr_depth = self.depth_of(curr)
        if prev_depth is not None and curr_depth is not None:
            if prev_depth > curr_depth:
                extra += self.dark_to_light_loss
            elif prev_depth < curr_depth:
                extra += self.light_to_dark_loss

        prev_undertone = self.undertone_of(prev)
        curr_undertone = self.undertone_of(curr)
        if prev_undertone and curr_undertone and prev_undertone != curr_undertone:
            extra += self.undertone_change_loss
            wash_required = self.undertone_requires_wash

        if pair in self.mandatory_wash_pairs:
            extra += self.wash_duration_hours
            wash_required = True

        return (extra, wash_required)

    def is_color_allowed_on_machine(self, machine_code: str, color: str) -> bool:
        machine = _norm(machine_code)
        curr = _norm(color)
        dedicated_color = self.dedicated_machine_map.get(machine)
        return not dedicated_color or dedicated_color == curr

    def violates_continuous_same_color(
        self,
        recent_colors: list[str],
        curr_color: str,
    ) -> bool:
        curr = _norm(curr_color)
        if not curr or self.max_continuous_same_color <= 0:
            return False

        count = 1
        for color in recent_colors:
            if _norm(color) != curr:
                break
            count += 1
        return count > self.max_continuous_same_color

    def recent_color_limit(self) -> int:
        return max(self.max_continuous_same_color, self.max_continuous_same_family, 1)
