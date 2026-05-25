from __future__ import annotations

from app.services.ai_assistant.llm_client import LLMClient
from app.services.ai_assistant.prompt_templates import render_benchmark_summary_prompt


class BenchmarkSummaryService:
    """Summarize strategy KPI tables without affecting scheduling decisions."""

    def __init__(self, llm_client: LLMClient) -> None:
        self.llm = llm_client

    def summarize(self, kpi_table: list[dict]) -> dict:
        cache_key = self._cache_key(kpi_table)
        prompt = render_benchmark_summary_prompt(kpi_table)
        response = self.llm.chat(prompt, cache_key=cache_key)
        if response.success and response.content:
            summary = response.content.strip()
            ai_powered = not response.fallback_used
        else:
            summary = self._rule_based_summary(kpi_table)
            ai_powered = False
        return {
            "summary_markdown": summary,
            "ai_powered": ai_powered,
            "elapsed_ms": response.elapsed_ms,
        }

    def _rule_based_summary(self, kpi_table: list[dict]) -> str:
        if not kpi_table:
            return (
                "**策略对比分析**：当前没有可用 KPI 数据，无法判断最优策略。\n\n"
                "AI 建议，仅供参考：请先运行 benchmark 生成 5 策略对比表。"
            )

        best_changeover = min(kpi_table, key=lambda x: float(x.get("total_changeover_hours", 0)))
        best_wash = min(kpi_table, key=lambda x: int(x.get("wash_count", 0)))
        best_on_time = max(
            kpi_table,
            key=lambda x: (
                float(x.get("on_time_rate", 0)),
                -float(x.get("total_changeover_hours", 0)),
            ),
        )
        return (
            "**策略对比分析**：\n\n"
            f"- 总换型损耗最低：**{best_changeover['strategy']}**"
            f"（{best_changeover.get('total_changeover_hours', 0)}h）\n"
            f"- 洗缸次数最少：**{best_wash['strategy']}**"
            f"（{best_wash.get('wash_count', 0)} 次）\n"
            f"- 准交率与换型平衡最佳：**{best_on_time['strategy']}**\n\n"
            "在染色场景下，推荐优先采用换型优先策略控制助剂和洗缸成本；"
            "在交期紧的场景下，推荐综合评分策略兼顾交期与换型。"
            "\n\nAI 建议，仅供参考。"
        )

    @staticmethod
    def _cache_key(kpi_table: list[dict]) -> str:
        parts = [
            f"{row.get('strategy')}:{row.get('total_changeover_hours')}:{row.get('wash_count')}"
            for row in kpi_table
        ]
        return "benchmark:" + "|".join(parts)
