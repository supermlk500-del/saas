import unittest

from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.pool import StaticPool
from sqlalchemy.orm import sessionmaker

from app.db.base import Base
from app.db.session import get_db
from app.main import app
from app.models import ConstraintRule, ConstraintRuleItem, Machine


class MiscApiTest(unittest.TestCase):
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

    def test_constraint_health_fields(self):
        db = self.SessionLocal()
        try:
            rule = ConstraintRule(
                rule_code="R-HEALTH",
                rule_name="machine",
                rule_type="machine_limit",
                status="active",
                is_hard_constraint=True,
            )
            db.add(rule)
            db.flush()
            db.add(ConstraintRuleItem(rule_id=rule.id, item_key="allow_machine_types", item_value="WEAVE"))
            db.commit()
        finally:
            db.close()

        response = self.client.get("/api/v1/constraints/health")

        self.assertEqual(200, response.status_code)
        data = response.json()
        self.assertIn("active_rule_count", data)
        self.assertIn("present_types", data)
        self.assertIn("all_ready", data)

    def test_machine_reset_result(self):
        db = self.SessionLocal()
        try:
            db.add(Machine(machine_code="M-RESET", machine_name="M-RESET", machine_type="WEAVE", status="active"))
            db.commit()
        finally:
            db.close()

        response = self.client.post("/api/v1/machines/reset")

        self.assertEqual(200, response.status_code)
        self.assertIn("cleared_machines", response.json())
        self.assertEqual(1, response.json()["cleared_machines"])

    def test_timeline_files_shape(self):
        response = self.client.get("/api/v1/timeline/files")

        self.assertEqual(200, response.status_code)
        self.assertIn("files", response.json())
        self.assertIsInstance(response.json()["files"], list)
