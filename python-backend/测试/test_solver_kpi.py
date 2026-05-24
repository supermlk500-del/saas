import unittest
from datetime import date, datetime

from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session, sessionmaker

from app.core.status import SnapshotStatus
from app.db.base import Base
from app.models import (
    ConstraintRule,
    ConstraintRuleItem,
    ConstraintVersion,
    Machine,
    MachineCapabilitySnapshot,
)
from app.schemas.schedule import BaselineScheduleRunRequest, ScheduleTaskCreate
from app.services.schedule_service import ScheduleService


class SolverKPITest(unittest.TestCase):
    def setUp(self) -> None:
        self.engine = create_engine("sqlite+pysqlite:///:memory:", future=True)
        Base.metadata.create_all(self.engine)
        self.SessionLocal = sessionmaker(
            bind=self.engine,
            autoflush=False,
            autocommit=False,
            future=True,
        )
        self.db: Session = self.SessionLocal()
        self._seed()

    def tearDown(self) -> None:
        self.db.close()
        self.engine.dispose()

    def _seed(self) -> None:
        for code in ("M-01", "M-02"):
            self.db.add(
                Machine(
                    machine_code=code,
                    machine_name=f"Weave-{code}",
                    machine_type="WEAVE",
                    status="active",
                    created_by="t",
                    updated_by="t",
                )
            )
        self.db.commit()

        machines = list(self.db.scalars(select(Machine).order_by(Machine.id)).all())
        for machine in machines:
            self.db.add(
                MachineCapabilitySnapshot(
                    snapshot_no="SNAP-KPI-TEST",
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
            ConstraintVersion(
                version_no="CV-KPI-TEST",
                version_name="kpi test",
                version_status="published",
                published_flag=True,
                published_at=datetime.utcnow(),
                published_by="t",
                created_by="t",
                updated_by="t",
            )
        )
        self.db.commit()

        rules = [
            ("due_priority", {"strict_due": "false"}),
            ("machine_limit", {"allow_machine_types": "WEAVE"}),
            ("changeover", {"color_change_loss": "0.2", "fabric_change_loss": "0.1"}),
            ("continuous_limit", {"max_task_per_machine": "99", "min_start_batch": "0"}),
            ("manual_lock", {"lock_order_nos": ""}),
        ]
        for idx, (rule_type, items) in enumerate(rules, start=1):
            rule = ConstraintRule(
                rule_code=f"R{idx}",
                rule_name=rule_type,
                rule_type=rule_type,
                version_no="CV-KPI-TEST",
                status="active",
                created_by="t",
                updated_by="t",
            )
            self.db.add(rule)
            self.db.flush()
            for order_num, (key, value) in enumerate(items.items(), start=1):
                self.db.add(
                    ConstraintRuleItem(
                        rule_id=rule.id,
                        item_key=key,
                        item_value=value,
                        item_order=order_num,
                        enabled_flag=True,
                        created_by="t",
                        updated_by="t",
                    )
                )
        self.db.commit()

        orders = [
            ("ORD-A", "RED", date(2026, 5, 25)),
            ("ORD-B", "BLUE", date(2026, 5, 25)),
            ("ORD-C", "RED", date(2026, 5, 25)),
            ("ORD-D", "BLUE", date(2026, 5, 25)),
        ]
        for order_no, color, due in orders:
            ScheduleService.create_task(
                self.db,
                ScheduleTaskCreate(
                    order_no=order_no,
                    fabric_type="FAB",
                    width_cm=160,
                    gram_weight=200,
                    color_code=color,
                    process_route="WEAVE",
                    order_quantity=500,
                    quantity_unit="m",
                    due_urgency_level="P2",
                    customer_priority_level="P2",
                    due_date=due,
                    priority_level=5,
                    task_status="pending",
                    created_by="t",
                ),
            )

    def test_baseline_kpi_snapshot(self) -> None:
        result = ScheduleService.run_baseline_schedule(
            self.db,
            BaselineScheduleRunRequest(
                plan_version_no="PLAN-KPI-001",
                plan_version_name="kpi snapshot",
                snapshot_version_no="SNAP-KPI-TEST",
                constraint_version_no="CV-KPI-TEST",
                generated_by="t",
            ),
        )

        kpi = result["kpi"]
        self.assertEqual(kpi["assigned_count"], 4)
        self.assertEqual(kpi["unassigned_count"], 0)
        self.assertEqual(kpi["on_time_rate"], 100.0)
        self.assertEqual(kpi["late_count"], 0)
        self.assertEqual(kpi["max_late_days"], 0)
        self.assertEqual(kpi["total_changeover_hours"], 0.0)

        expected_keys = {
            "assigned_count",
            "unassigned_count",
            "soft_warning_count",
            "on_time_count",
            "late_count",
            "on_time_rate",
            "total_changeover_hours",
            "max_late_days",
            "wash_count",
        }
        self.assertEqual(set(kpi.keys()), expected_keys)


class StrategySwitchTest(unittest.TestCase):
    def test_all_strategies_can_be_instantiated(self) -> None:
        from app.solver.strategies import SOLVER_STRATEGIES, get_solver

        expected = {"greedy_eft", "edd", "cr", "changeover_first", "composite"}
        self.assertEqual(set(SOLVER_STRATEGIES.keys()), expected)

        for name in expected:
            solver = get_solver(name)
            self.assertIsNotNone(solver)
            self.assertTrue(hasattr(solver, "solve"))

    def test_get_solver_defaults_to_greedy_eft(self) -> None:
        from app.solver.strategies import DEFAULT_STRATEGY, get_solver

        self.assertEqual(DEFAULT_STRATEGY, "greedy_eft")
        default_solver = get_solver(None)
        named_solver = get_solver("greedy_eft")
        self.assertEqual(type(default_solver).__name__, type(named_solver).__name__)

    def test_unknown_strategy_raises_value_error(self) -> None:
        from app.solver.strategies import get_solver

        with self.assertRaises(ValueError):
            get_solver("nonexistent_strategy")


if __name__ == "__main__":
    unittest.main()
