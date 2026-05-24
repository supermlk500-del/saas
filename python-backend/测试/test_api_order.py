import csv
from datetime import date
from io import StringIO
import unittest

from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.pool import StaticPool
from sqlalchemy.orm import sessionmaker

from app.db.base import Base
from app.db.session import get_db
from app.main import app


class OrderApiTest(unittest.TestCase):
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

    def _payload(self, order_no: str = "ORD-API-001") -> dict:
        return {
            "order_no": order_no,
            "fabric_type": "FAB_A",
            "width_cm": 160,
            "gram_weight": 210,
            "color_code": "BLACK",
            "process_route": "WEAVE",
            "order_quantity": 100,
            "quantity_unit": "m",
            "customer_priority_level": "P1",
            "due_date": date.today().isoformat(),
            "priority_level": 3,
            "task_status": "pending",
            "created_by": "test",
        }

    def test_get_orders_empty(self):
        response = self.client.get("/api/v1/orders")
        self.assertEqual(200, response.status_code)
        self.assertEqual([], response.json())

    def test_create_order_then_list(self):
        created = self.client.post("/api/v1/orders", json=self._payload())
        self.assertEqual(200, created.status_code)

        listed = self.client.get("/api/v1/orders")
        self.assertEqual(200, listed.status_code)
        self.assertEqual(1, len(listed.json()))
        self.assertEqual("ORD-API-001", listed.json()[0]["order_no"])

    def test_import_csv(self):
        output = StringIO()
        writer = csv.writer(output)
        writer.writerow(["订单号", "布种", "幅宽(cm)", "克重(gsm)", "颜色", "工艺路线", "数量", "单位", "交期", "优先级", "客户优先级"])
        writer.writerow(["ORD-CSV-001", "FAB_A", 160, 210, "BLACK", "WEAVE", 100, "m", date.today().isoformat(), 3, "P1"])

        response = self.client.post(
            "/api/v1/orders/import-csv",
            files={"file": ("orders.csv", output.getvalue().encode("utf-8-sig"), "text/csv")},
        )

        self.assertEqual(200, response.status_code)
        self.assertEqual({"created": 1, "skipped": 0, "errors": 0}, response.json())

    def test_clear_orders(self):
        self.client.post("/api/v1/orders", json=self._payload())

        response = self.client.post("/api/v1/orders/clear")

        self.assertEqual(200, response.status_code)
        self.assertEqual(1, response.json()["cleared_tasks"])
        self.assertEqual([], self.client.get("/api/v1/orders").json())

    def test_dashboard_fields(self):
        response = self.client.get("/api/v1/orders/dashboard")

        self.assertEqual(200, response.status_code)
        data = response.json()
        self.assertEqual("-", data["latest_plan_no"])
        self.assertIn("due_risk", data)
        self.assertIn("rows", data)

    def test_csv_template(self):
        response = self.client.get("/api/v1/orders/csv-template")

        self.assertEqual(200, response.status_code)
        self.assertIn("text/csv", response.headers["content-type"])
        self.assertIn("订单号".encode("utf-8"), response.content)
