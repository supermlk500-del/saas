from __future__ import annotations

import json


def _json_dump(data: object) -> str:
    return json.dumps(data, ensure_ascii=False, indent=2, default=str)


def render_unassigned_explanation_prompt(
    unassigned_items: list[dict],
    machine_summary: dict,
    constraint_summary: dict,
) -> str:
    return f"""请基于下面的排产系统结构化数据，解释未排入订单原因。

要求：
1. 只解释输入数据，不要编造新的订单、机台或规则。
2. 输出 JSON，字段固定为 summary、root_causes、suggestions。
3. summary 控制在 100 字以内。
4. suggestions 用业务人员能理解的话，且标注“AI 建议，仅供参考”。

未排入订单：
{_json_dump(unassigned_items)}

机台摘要：
{_json_dump(machine_summary)}

约束摘要：
{_json_dump(constraint_summary)}

JSON 输出格式：
{{
  "summary": "100 字以内的人话总结",
  "root_causes": ["原因 1", "原因 2"],
  "suggestions": ["建议 1", "建议 2"]
}}
"""


def render_benchmark_summary_prompt(kpi_table: list[dict]) -> str:
    return f"""请把下面 5 种排产策略 KPI 对比表总结成 200 字以内的 markdown 段落。

要求：
1. 指出最优策略、取舍关系、推荐使用场景。
2. 不要编造表格中没有的数字。
3. 结尾加一句“AI 建议，仅供参考”。
4. 只输出 markdown 正文，不要输出 JSON。

KPI 表：
{_json_dump(kpi_table)}
"""
