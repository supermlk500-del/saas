import re

FABRIC_MACHINE_TYPES = [
    "织机",
    "整经",
    "染色",
    "定型",
    "后整理",
]

MACHINE_TYPE_CN_ALIAS = {
    "WEAVE": "织机",
    "WARP": "整经",
    "DYE": "染色",
    "SET": "定型",
    "FINISH": "后整理",
    "织机": "织机",
    "整经": "整经",
    "染色": "染色",
    "定型": "定型",
    "后整理": "后整理",
}

CODE_PREFIX_CN_ALIAS = [
    ("PLAN-SWIPE-BASE-", "基准计划-"),
    ("PLAN-SWIPE-RT-", "实时重排计划-"),
    ("PLAN-SWIPE-COMP-", "补偿计划-"),
    ("PLAN-", "计划-"),
    ("SNAP-SWIPE-", "机台快照-"),
    ("SNAPSHOT-", "机台快照-"),
    ("SNAP-", "机台快照-"),
    ("CV-SWIPE-", "约束版本-"),
    ("CV-", "约束版本-"),
    ("ORD-INSERT-", "插单-"),
    ("ORD-BASE-", "基线订单-"),
    ("ORD-NEW-", "插单-"),
    ("ORD-", "订单-"),
    ("SWIPE-RULE-", "规则-"),
    ("R-", "规则-"),
    ("WV-", "织机机台-"),
    ("WP-", "整经机台-"),
    ("DY-", "染色机台-"),
    ("ST-", "定型机台-"),
    ("FN-", "后整理机台-"),
]


def machine_type_display(machine_type: str | None) -> str:
    if not machine_type:
        return "未知"
    raw = machine_type.strip()
    mapped = MACHINE_TYPE_CN_ALIAS.get(raw, MACHINE_TYPE_CN_ALIAS.get(raw.upper()))
    return mapped if mapped else raw


def machine_name_display(machine_name: str | None, machine_type: str | None) -> str:
    raw_name = (machine_name or "").strip()
    if not raw_name:
        return machine_type_display(machine_type)

    if "-" not in raw_name:
        return display_identifier(raw_name)

    prefix, suffix = raw_name.split("-", 1)
    prefix_cn = MACHINE_TYPE_CN_ALIAS.get(prefix, MACHINE_TYPE_CN_ALIAS.get(prefix.upper()))
    if prefix_cn:
        return f"{prefix_cn}-{suffix}"
    return display_identifier(raw_name)


def display_identifier(value: str | None) -> str:
    if value is None:
        return "-"

    text = str(value).strip()
    if not text:
        return "-"

    if any("\u4e00" <= ch <= "\u9fff" for ch in text):
        return text

    upper_text = text.upper()
    for prefix, cn_prefix in CODE_PREFIX_CN_ALIAS:
        if upper_text.startswith(prefix):
            return f"{cn_prefix}{text[len(prefix):]}"

    if re.fullmatch(r"P\d+", text, flags=re.IGNORECASE):
        return text.upper()

    if re.fullmatch(r"[A-Za-z0-9_-]+", text):
        return f"编号-{text}"

    return text


FABRIC_CONSTRAINT_KEYS = {
    "due_priority": ["strict_due", "sort_by"],
    "machine_limit": ["allow_machine_types", "allowed_fabric_types"],
    "changeover": ["same_color_continuous", "color_change_loss", "fabric_change_loss"],
    "continuous_limit": ["max_task_per_machine", "min_start_batch"],
    "manual_lock": ["lock_order_nos", "lock_reason"],
}
