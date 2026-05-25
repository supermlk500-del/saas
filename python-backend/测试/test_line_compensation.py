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
    SchedulePlanItem,
    SchedulePlanVersion,
    ScheduleTask,
)
from app.schemas.schedule import BaselineScheduleRunRequest, LineCompensationRequest, ScheduleTaskCreate
from app.services.schedule_service import ScheduleService


class LineCompensationTest(unittest.TestCase):
    def setUp(self) -> None:
        self.engine = create_engine("sqlite+pysqlite:///:memory:", future=True)
        Base.metadata.create_all(self.engine)
        self.SessionLocal = sessionmaker(bind=self.engine, autoflush=False, autocommit=False, future=True)
        self.db: Session = self.SessionLocal()
        self._seed_base()

    def tearDown(self) -> None:
        self.db.close()
        self.engine.dispose()

    def _seed_base(self) -> None:
        m1 = Machine(machine_code="A-01", machine_name="A-01", machine_type="LINEA", status="active", created_by="t", updated_by="t")
        m2 = Machine(machine_code="B-01", machine_name="B-01", machine_type="LINEB", status="active", created_by="t", updated_by="t")
        self.db.add(m1)
        self.db.add(m2)
        self.db.commit()
        self.db.refresh(m1)
        self.db.refresh(m2)

        self.db.add(
            MachineCapabilitySnapshot(
                snapshot_no="SNAP-COMP-001",
                machine_id=m1.id,
                machine_code=m1.machine_code,
                param_version="PV1",
                snapshot_status=SnapshotStatus.GENERATED,
                available_flag=True,
                speed_value=120.0,
                changeover_loss=0.1,
                stability_score=95.0,
                generated_by="t",
            )
        )
        self.db.add(
            MachineCapabilitySnapshot(
                snapshot_no="SNAP-COMP-001",
                machine_id=m2.id,
                machine_code=m2.machine_code,
                param_version="PV1",
                snapshot_status=SnapshotStatus.GENERATED,
                available_flag=True,
                speed_value=120.0,
                changeover_loss=0.1,
                stability_score=95.0,
                generated_by="t",
            )
        )

        self.db.add(
            ConstraintVersion(
                version_no="CV-COMP-001",
                version_name="test",
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
            ("machine_limit", {"allow_machine_types": "LINEA,LINEB"}),
            ("changeover", {"same_color_continuous": "false", "color_change_loss": "0.1", "fabric_change_loss": "0.1"}),
            ("continuous_limit", {"max_task_per_machine": "99", "min_start_batch": "0"}),
            ("manual_lock", {"lock_order_nos": ""}),
        ]
        for idx, (rule_type, items) in enumerate(rules, start=1):
            r = ConstraintRule(
                rule_code=f"R{idx}",
                rule_name=rule_type,
                rule_type=rule_type,
                version_no="CV-COMP-001",
                status="active",
                created_by="t",
                updated_by="t",
            )
            self.db.add(r)
            self.db.flush()
            order_num = 1
            for k, v in items.items():
                self.db.add(
                    ConstraintRuleItem(
                        rule_id=r.id,
                        item_key=k,
                        item_value=v,
                        item_order=order_num,
                        enabled_flag=True,
                        created_by="t",
                        updated_by="t",
                    )
                )
                order_num += 1
        self.db.commit()

    def _create_order(self, order_no: str, route: str) -> int:
        row = ScheduleService.create_task(
            self.db,
            ScheduleTaskCreate(
                order_no=order_no,
                fabric_type="FAB",
                width_cm=160,
                gram_weight=200,
                color_code="BLUE",
                process_route=route,
                order_quantity=600,
                quantity_unit="m",
                due_urgency_level="P1",
                customer_priority_level="P1",
                due_date=date(2026, 5, 20),
                priority_level=7,
                task_status="pending",
                created_by="t",
            ),
        )
        return row.id

    def test_line_compensation_assigns_only_target_line(self) -> None:
        self._create_order("ORD-001", "LINEA>FINISH")
        affected_task_id = self._create_order("ORD-002", "LINEA>FINISH")

        ScheduleService.run_baseline_schedule(
            self.db,
            BaselineScheduleRunRequest(
                plan_version_no="PLAN-BASE-001",
                plan_version_name="base",
                snapshot_version_no="SNAP-COMP-001",
                constraint_version_no="CV-COMP-001",
                generated_by="t",
            ),
        )
        base_plan = self.db.scalar(select(SchedulePlanVersion).where(SchedulePlanVersion.plan_version_no == "PLAN-BASE-001"))
        assert base_plan is not None

        affected_task = self.db.get(ScheduleTask, affected_task_id)
        affected_task.task_status = "pending"
        self.db.commit()

        result = ScheduleService.run_line_compensation(
            self.db,
            LineCompensationRequest(
                source_plan_version_id=base_plan.id,
                compensation_plan_version_no="PLAN-COMP-001",
                compensation_plan_version_name="comp",
                line_machine_type="LINEA",
                task_ids=[affected_task_id],
                generated_by="t",
            ),
        )
        self.assertEqual(result["assigned_count"], 1)

        comp_plan = self.db.scalar(select(SchedulePlanVersion).where(SchedulePlanVersion.plan_version_no == "PLAN-COMP-001"))
        assert comp_plan is not None
        items = list(self.db.scalars(select(SchedulePlanItem).where(SchedulePlanItem.plan_version_id == comp_plan.id)).all())
        self.assertEqual(len(items), 1)
        self.assertEqual(items[0].item_status, "compensated")

        machine = self.db.get(Machine, items[0].machine_id)
        self.assertIsNotNone(machine)
        self.assertEqual(machine.machine_type, "LINEA")


if __name__ == "__main__":
    unittest.main()
