from datetime import date, datetime, timedelta
import unittest

from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.pool import StaticPool
from sqlalchemy.orm import sessionmaker

from app.db.base import Base
from app.db.session import get_db
from app.main import app
from app.models import Machine, SchedulePlanItem, SchedulePlanVersion, ScheduleRunLog, ScheduleTask


class ScheduleExtraApiTest(unittest.TestCase):
    def setUp(self) -> None:
        self.engine = create_engine(
            "sqlite+pysqlite:///:memory:",
            connect_args={"check_same_thread": False},
            poolclass=StaticPool,
            future=True,
        )
        Base.metadata.create_all(self.engine)
        self.SessionLocal = sessionmaker(bind=self.engine, autoflush=False, autocommit=False, future=True)

        def override_db():
            db = self.SessionLocal()
            try:
                yield db
            finally:
                db.close()

        app.dependency_overrides[get_db] = override_db
        self.client = TestClient(app)

    def tearDown(self) -> None:
        app.dependency_overrides.clear()
        Base.metadata.drop_all(self.engine)
        self.engine.dispose()

    def _seed_plan(self) -> tuple[int, int]:
        db = self.SessionLocal()
        try:
            machine = Machine(machine_code="M-01", machine_name="WEAVE-01", machine_type="WEAVE", status="active")
            task = ScheduleTask(
                order_no="ORD-GANTT-001",
                fabric_type="FAB_A",
                width_cm=160,
                gram_weight=210,
                color_code="BLACK",
                process_route="WEAVE",
                order_quantity=100,
                quantity_unit="m",
                due_date=date.today() + timedelta(days=1),
                due_urgency_level="P1",
                customer_priority_level="P1",
                priority_level=3,
                task_status="scheduled",
            )
            base = SchedulePlanVersion(
                plan_version_no="PLAN-LIVE-BASE-TEST",
                plan_version_name="base",
                plan_status="generated",
                snapshot_version_no="SNAP-1",
                constraint_version_no="CV-1",
                generated_at=datetime.utcnow(),
            )
            rt = SchedulePlanVersion(
                plan_version_no="PLAN-LIVE-RT-TEST",
                plan_version_name="rt",
                plan_status="generated",
                snapshot_version_no="SNAP-1",
                constraint_version_no="CV-1",
                generated_at=datetime.utcnow(),
            )
            db.add_all([machine, task, base, rt])
            db.flush()
            start = datetime.utcnow()
            db.add(
                SchedulePlanItem(
                    plan_version_id=rt.id,
                    task_id=task.id,
                    machine_id=machine.id,
                    start_time=start,
                    end_time=start + timedelta(hours=2),
                    planned_quantity=100,
                    item_status="planned",
                )
            )
            db.add(
                ScheduleRunLog(
                    log_type="soft_constraint_warning",
                    business_no=rt.plan_version_no,
                    operation_name="soft_constraint_warning",
                    operation_result="warning",
                    error_message="soft warning",
                    operator="test",
                )
            )
            db.commit()
            return base.id, rt.id
        finally:
            db.close()

    def test_demo_nav_empty(self):
        response = self.client.get("/api/v1/schedules/plans/demo-nav")
        self.assertEqual(200, response.status_code)
        self.assertEqual([], response.json())

    def test_gantt_and_compare(self):
        base_id, rt_id = self._seed_plan()

        gantt = self.client.get(f"/api/v1/schedules/plans/{rt_id}/gantt?day=1")
        compare = self.client.get(f"/api/v1/schedules/plans/{rt_id}/compare/{base_id}")

        self.assertEqual(200, gantt.status_code)
        self.assertIn("lanes", gantt.json())
        self.assertEqual(200, compare.status_code)
        self.assertEqual(1, compare.json()["added_count"])

    def test_soft_warnings(self):
        _base_id, rt_id = self._seed_plan()

        response = self.client.get(f"/api/v1/schedules/plans/{rt_id}/soft-warnings")

        self.assertEqual(200, response.status_code)
        self.assertEqual(1, len(response.json()))
        self.assertEqual("soft warning", response.json()[0]["message"])
