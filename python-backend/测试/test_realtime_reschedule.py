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
    ScheduleTask,
)
from app.schemas.schedule import BaselineScheduleRunRequest, RealtimeRescheduleRequest, ScheduleTaskCreate
from app.services.schedule_service import ScheduleService


class RealtimeRescheduleTest(unittest.TestCase):
    def setUp(self) -> None:
        self.engine = create_engine("sqlite+pysqlite:///:memory:", future=True)
        Base.metadata.create_all(self.engine)
        self.SessionLocal = sessionmaker(bind=self.engine, autoflush=False, autocommit=False, future=True)
        self.db: Session = self.SessionLocal()
        self._seed_minimum_data()

    def tearDown(self) -> None:
        self.db.close()
        self.engine.dispose()

    def _seed_minimum_data(self) -> None:
        self.db.add(
            Machine(
                machine_code="M-01",
                machine_name="织机-01",
                machine_type="织机",
                status="active",
                created_by="test",
                updated_by="test",
            )
        )
        self.db.commit()
        machine = self.db.scalar(select(Machine).where(Machine.machine_code == "M-01"))
        assert machine is not None

        self.db.add(
            MachineCapabilitySnapshot(
                snapshot_no="SNAP-TEST-001",
                machine_id=machine.id,
                machine_code=machine.machine_code,
                param_version="PV-1",
                snapshot_status=SnapshotStatus.GENERATED,
                available_flag=True,
                speed_value=100.0,
                changeover_loss=0.2,
                stability_score=95.0,
                generated_by="test",
            )
        )
        self.db.add(
            ConstraintVersion(
                version_no="CV-TEST-001",
                version_name="测试约束",
                version_status="published",
                published_flag=True,
                published_at=datetime.utcnow(),
                published_by="test",
                created_by="test",
                updated_by="test",
            )
        )
        self.db.commit()

        rule_defs = [
            ("R-DUE", "交期优先", "due_priority", {"strict_due": "false"}),
            ("R-MACHINE", "机台限制", "machine_limit", {"allow_machine_types": "织机"}),
            ("R-CHANGE", "换型规则", "changeover", {"same_color_continuous": "true", "color_change_loss": "0.2"}),
            ("R-CONT", "连续生产", "continuous_limit", {"max_task_per_machine": "50", "min_start_batch": "100"}),
            ("R-LOCK", "人工锁定", "manual_lock", {"lock_order_nos": ""}),
        ]

        for rule_code, rule_name, rule_type, items in rule_defs:
            rule = ConstraintRule(
                rule_code=rule_code,
                rule_name=rule_name,
                rule_type=rule_type,
                version_no="CV-TEST-001",
                status="active",
                created_by="test",
                updated_by="test",
            )
            self.db.add(rule)
            self.db.flush()
            order_num = 1
            for k, v in items.items():
                self.db.add(
                    ConstraintRuleItem(
                        rule_id=rule.id,
                        item_key=k,
                        item_value=v,
                        item_order=order_num,
                        enabled_flag=True,
                        created_by="test",
                        updated_by="test",
                    )
                )
                order_num += 1
        self.db.commit()

    def _create_order(
        self,
        order_no: str,
        qty: float,
        color: str,
        priority: int,
        due_urgency_level: str = "P2",
        customer_priority_level: str = "P2",
        due_date: date = date(2026, 5, 10),
    ) -> None:
        ScheduleService.create_task(
            self.db,
            ScheduleTaskCreate(
                order_no=order_no,
                fabric_type="平纹布",
                width_cm=160,
                gram_weight=200,
                color_code=color,
                process_route="整经>织机>后整理",
                order_quantity=qty,
                quantity_unit="m",
                due_urgency_level=due_urgency_level,
                customer_priority_level=customer_priority_level,
                due_date=due_date,
                priority_level=priority,
                task_status="pending",
                created_by="test",
            ),
        )

    def test_baseline_schedule_respects_customer_and_due_urgency(self) -> None:
        self._create_order("ORD-LOW-001", 300, "灰色", 9, "P2", "P2", date(2026, 5, 25))
        self._create_order("ORD-MID-001", 300, "灰色", 9, "P0", "P2", date(2026, 5, 14))
        self._create_order("ORD-HIGH-001", 300, "灰色", 1, "P2", "P0", date(2026, 5, 11))

        ScheduleService.run_baseline_schedule(
            self.db,
            BaselineScheduleRunRequest(
                plan_version_no="PLAN-BASE-PRIORITY",
                plan_version_name="优先级排序测试",
                snapshot_version_no="SNAP-TEST-001",
                constraint_version_no="CV-TEST-001",
                generated_by="test",
            ),
        )
        plan = ScheduleService.list_plan_versions(self.db)[0]
        items = list(
            self.db.scalars(
                select(SchedulePlanItem)
                .where(SchedulePlanItem.plan_version_id == plan.id)
                .order_by(SchedulePlanItem.start_time)
            ).all()
        )
        order_nos = []
        for it in items:
            task = self.db.get(ScheduleTask, it.task_id)
            order_nos.append(task.order_no if task else "")

        self.assertEqual(order_nos[0], "ORD-HIGH-001")
        self.assertEqual(order_nos[1], "ORD-MID-001")
        self.assertEqual(order_nos[2], "ORD-LOW-001")

    def test_realtime_reschedule_handles_insert_order(self) -> None:
        self._create_order("ORD-001", 1200, "藏青", 9)
        self._create_order("ORD-002", 1000, "黑色", 8)

        baseline = ScheduleService.run_baseline_schedule(
            self.db,
            BaselineScheduleRunRequest(
                plan_version_no="PLAN-BASE-001",
                plan_version_name="基准计划",
                snapshot_version_no="SNAP-TEST-001",
                constraint_version_no="CV-TEST-001",
                generated_by="test",
            ),
        )
        self.assertEqual(baseline["assigned_count"], 2)

        self._create_order("ORD-NEW-001", 900, "米白", 9)

        source_plan = ScheduleService.list_plan_versions(self.db)[0]
        rt = ScheduleService.run_realtime_reschedule(
            self.db,
            RealtimeRescheduleRequest(
                source_plan_version_id=source_plan.id,
                new_plan_version_no="PLAN-REALTIME-001",
                new_plan_version_name="实时重排-插单",
                freeze_minutes=30,
                generated_by="dispatcher",
            ),
        )

        self.assertEqual(rt["plan_version_no"], "PLAN-REALTIME-001")
        self.assertGreaterEqual(rt["assigned_count"], 2)
        self.assertIn("locked_count", rt)

        new_plan = self.db.scalar(select(SchedulePlanItem).join_from(SchedulePlanItem, ScheduleTask).where(ScheduleTask.order_no == "ORD-NEW-001"))
        self.assertIsNotNone(new_plan)

    def test_realtime_reschedule_marks_locked_items(self) -> None:
        self._create_order("ORD-101", 1800, "藏青", 9)
        self._create_order("ORD-102", 600, "藏青", 8)

        ScheduleService.run_baseline_schedule(
            self.db,
            BaselineScheduleRunRequest(
                plan_version_no="PLAN-BASE-LOCK",
                plan_version_name="基准计划-锁定测试",
                snapshot_version_no="SNAP-TEST-001",
                constraint_version_no="CV-TEST-001",
                generated_by="test",
            ),
        )
        source_plan = ScheduleService.list_plan_versions(self.db)[0]

        ScheduleService.run_realtime_reschedule(
            self.db,
            RealtimeRescheduleRequest(
                source_plan_version_id=source_plan.id,
                new_plan_version_no="PLAN-REALTIME-LOCK",
                new_plan_version_name="实时重排-锁定测试",
                freeze_minutes=30,
                generated_by="dispatcher",
            ),
        )

        realtime_plan = self.db.scalar(
            select(SchedulePlanItem)
            .join_from(SchedulePlanItem, ScheduleTask)
            .where(SchedulePlanItem.plan_version_id == 2)
            .order_by(SchedulePlanItem.start_time)
        )
        self.assertIsNotNone(realtime_plan)

        locked_items = list(
            self.db.scalars(
                select(SchedulePlanItem)
                .where(SchedulePlanItem.plan_version_id == 2, SchedulePlanItem.item_status == "locked")
                .order_by(SchedulePlanItem.id)
            ).all()
        )
        self.assertGreaterEqual(len(locked_items), 1)


if __name__ == "__main__":
    unittest.main()
