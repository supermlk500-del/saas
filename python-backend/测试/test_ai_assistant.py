import json
import tempfile
import unittest
from datetime import date, datetime
from pathlib import Path

from sqlalchemy import create_engine
from sqlalchemy.orm import Session, sessionmaker

from app.core.status import SnapshotStatus
from app.db.base import Base
from app.models import (
    ConstraintRule,
    Machine,
    MachineCapabilitySnapshot,
    SchedulePlanVersion,
    ScheduleTask,
    ScheduleUnassignedTask,
)
from app.services.ai_assistant.explain_service import (
    RULE_BASED_EXPLANATIONS,
    UnassignedExplainService,
)
from app.services.ai_assistant.llm_client import LLMClient
from app.services.ai_assistant.summary_service import BenchmarkSummaryService


class LLMClientTest(unittest.TestCase):
    def test_llm_disabled_returns_fallback_without_http(self):
        client = LLMClient(
            api_key="",
            base_url="https://invalid.local/v1",
            model="demo",
            enabled=False,
        )

        response = client.chat("hello")

        self.assertFalse(response.success)
        self.assertTrue(response.fallback_used)
        self.assertEqual("llm_disabled", response.error)

    def test_offline_cache_hit_returns_cached(self):
        with tempfile.TemporaryDirectory() as tmp:
            cache_path = Path(tmp) / "ai_cache.json"
            cache_path.write_text(
                json.dumps(
                    {
                        "demo-key": {
                            "success": True,
                            "content": "cached answer",
                            "fallback_used": False,
                        }
                    }
                ),
                encoding="utf-8",
            )
            client = LLMClient(
                api_key="",
                base_url="https://invalid.local/v1",
                model="demo",
                offline_cache_path=str(cache_path),
                enabled=True,
            )

            response = client.chat("hello", cache_key="demo-key")

            self.assertTrue(response.success)
            self.assertEqual("cached answer", response.content)
            self.assertFalse(response.fallback_used)


class UnassignedExplainServiceTest(unittest.TestCase):
    def setUp(self) -> None:
        self.engine = create_engine("sqlite+pysqlite:///:memory:", future=True)
        Base.metadata.create_all(self.engine)
        self.SessionLocal = sessionmaker(bind=self.engine, autoflush=False, autocommit=False, future=True)
        self.db: Session = self.SessionLocal()
        self._seed()

    def tearDown(self) -> None:
        self.db.close()
        self.engine.dispose()

    def _seed(self) -> None:
        machine = Machine(
            machine_code="DY-01",
            machine_name="染缸-01",
            machine_type="DYE",
            status="active",
            created_by="t",
            updated_by="t",
        )
        self.db.add(machine)
        self.db.flush()
        self.db.add(
            MachineCapabilitySnapshot(
                snapshot_no="SNAP-AI",
                machine_id=machine.id,
                machine_code=machine.machine_code,
                param_version="PV1",
                snapshot_status=SnapshotStatus.GENERATED,
                available_flag=True,
                speed_value=100.0,
                changeover_loss=0.1,
                stability_score=95.0,
                generated_by="t",
            )
        )
        self.db.add(
            ConstraintRule(
                rule_code="R-AI",
                rule_name="dye",
                rule_type="dye_color_changeover",
                version_no="CV-AI",
                status="active",
                is_hard_constraint=False,
                created_by="t",
                updated_by="t",
            )
        )
        task = ScheduleTask(
            order_no="ORD-AI-001",
            fabric_type="FAB_A",
            width_cm=160,
            gram_weight=210,
            color_code="KHAKI",
            process_route="DYE",
            order_quantity=100,
            quantity_unit="m",
            due_urgency_level="P1",
            customer_priority_level="P1",
            due_date=date.today(),
            priority_level=1,
            task_status="pending",
            created_by="t",
            updated_by="t",
        )
        self.db.add(task)
        self.db.flush()
        plan = SchedulePlanVersion(
            plan_version_no="PLAN-AI",
            plan_version_name="ai test",
            plan_status="generated",
            snapshot_version_no="SNAP-AI",
            constraint_version_no="CV-AI",
            generated_at=datetime.utcnow(),
            generated_by="t",
        )
        self.db.add(plan)
        self.db.flush()
        self.plan_id = plan.id
        self.db.add(
            ScheduleUnassignedTask(
                plan_version_id=plan.id,
                task_id=task.id,
                reason_code="color_not_allowed_on_dedicated_vat",
                reason_desc="专用染缸颜色不匹配",
                created_by="t",
            )
        )
        self.db.commit()

    def test_fallback_explanation_for_each_reason_code(self):
        service = UnassignedExplainService(LLMClient("", "", "demo", enabled=False))
        items = [
            {"reason_code": reason_code}
            for reason_code in list(RULE_BASED_EXPLANATIONS.keys())[:5]
        ]

        result = service._rule_based_explanation(items)

        self.assertGreaterEqual(len(result["root_causes"]), 5)
        self.assertTrue(all("AI 建议，仅供参考" in x for x in result["suggestions"]))

    def test_returns_structured_response(self):
        service = UnassignedExplainService(LLMClient("", "", "demo", enabled=False))

        result = service.explain(self.db, self.plan_id)

        self.assertEqual(self.plan_id, result["plan_version_id"])
        self.assertEqual(1, result["unassigned_count"])
        self.assertIn("summary", result)
        self.assertIn("root_causes", result)
        self.assertIn("suggestions", result)
        self.assertFalse(result["ai_powered"])


class BenchmarkSummaryServiceTest(unittest.TestCase):
    def test_rule_based_summary_picks_best_strategies(self):
        service = BenchmarkSummaryService(LLMClient("", "", "demo", enabled=False))
        kpi_table = [
            {"strategy": "greedy_eft", "on_time_rate": 100.0, "total_changeover_hours": 81.2, "wash_count": 1},
            {"strategy": "changeover_first", "on_time_rate": 100.0, "total_changeover_hours": 0.0, "wash_count": 0},
        ]

        result = service.summarize(kpi_table)

        self.assertFalse(result["ai_powered"])
        self.assertIn("changeover_first", result["summary_markdown"])
        self.assertIn("洗缸次数最少", result["summary_markdown"])

    def test_handles_empty_kpi_table(self):
        service = BenchmarkSummaryService(LLMClient("", "", "demo", enabled=False))

        result = service.summarize([])

        self.assertFalse(result["ai_powered"])
        self.assertIn("没有可用 KPI 数据", result["summary_markdown"])


if __name__ == "__main__":
    unittest.main()
