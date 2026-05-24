from pathlib import Path
import sys

import pymysql

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from app.core.config import settings
from app.db.init_db import create_tables


def create_database() -> None:
    conn = pymysql.connect(
        host=settings.db_host,
        port=settings.db_port,
        user=settings.db_user,
        password=settings.db_password,
        charset="utf8mb4",
        autocommit=True,
    )
    try:
        with conn.cursor() as cursor:
            cursor.execute(
                f"CREATE DATABASE IF NOT EXISTS `{settings.db_name}` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
            )
    finally:
        conn.close()


def _table_exists(cursor, table_name: str) -> bool:
    cursor.execute(
        """
        SELECT COUNT(*)
        FROM information_schema.tables
        WHERE table_schema = %s AND table_name = %s
        """,
        (settings.db_name, table_name),
    )
    return cursor.fetchone()[0] > 0


def _column_exists(cursor, table_name: str, column_name: str) -> bool:
    cursor.execute(
        """
        SELECT COUNT(*)
        FROM information_schema.columns
        WHERE table_schema = %s AND table_name = %s AND column_name = %s
        """,
        (settings.db_name, table_name, column_name),
    )
    return cursor.fetchone()[0] > 0


def _index_exists(cursor, table_name: str, index_name: str) -> bool:
    cursor.execute(
        """
        SELECT COUNT(*)
        FROM information_schema.statistics
        WHERE table_schema = %s AND table_name = %s AND index_name = %s
        """,
        (settings.db_name, table_name, index_name),
    )
    return cursor.fetchone()[0] > 0


def migrate_schedule_task_columns() -> None:
    conn = pymysql.connect(
        host=settings.db_host,
        port=settings.db_port,
        user=settings.db_user,
        password=settings.db_password,
        database=settings.db_name,
        charset="utf8mb4",
        autocommit=True,
    )
    columns_to_add = {
        "order_no": "VARCHAR(64) NULL",
        "fabric_type": "VARCHAR(64) NULL",
        "width_cm": "DOUBLE NULL",
        "gram_weight": "DOUBLE NULL",
        "color_code": "VARCHAR(64) NULL",
        "process_route": "VARCHAR(128) NULL",
        "order_quantity": "DOUBLE NULL",
        "quantity_unit": "VARCHAR(16) NULL",
        "due_urgency_level": "VARCHAR(16) NOT NULL DEFAULT 'P2'",
        "customer_priority_level": "VARCHAR(16) NOT NULL DEFAULT 'P2'",
    }
    legacy_columns = ("task_no", "task_name", "product_code", "quantity")

    try:
        with conn.cursor() as cursor:
            if not _table_exists(cursor, "schedule_task"):
                return

            for col, ddl in columns_to_add.items():
                if not _column_exists(cursor, "schedule_task", col):
                    cursor.execute(f"ALTER TABLE schedule_task ADD COLUMN {col} {ddl}")

            has_task_no = _column_exists(cursor, "schedule_task", "task_no")
            has_product_code = _column_exists(cursor, "schedule_task", "product_code")
            has_quantity = _column_exists(cursor, "schedule_task", "quantity")

            update_parts: list[str] = []
            if has_task_no:
                update_parts.append("order_no = COALESCE(NULLIF(order_no, ''), task_no)")
            if has_product_code:
                update_parts.append("fabric_type = COALESCE(NULLIF(fabric_type, ''), product_code)")
            if has_quantity:
                update_parts.append("order_quantity = COALESCE(order_quantity, quantity)")
            update_parts.append("quantity_unit = COALESCE(NULLIF(quantity_unit, ''), 'm')")

            if update_parts:
                cursor.execute("UPDATE schedule_task SET " + ", ".join(update_parts))

            # 给缺失必填值的旧数据兜底，保证重复执行和结构升级都不会失败。
            cursor.execute(
                """
                UPDATE schedule_task
                SET order_no = CONCAT('ORD-AUTO-', id)
                WHERE order_no IS NULL OR TRIM(order_no) = ''
                """
            )
            cursor.execute(
                """
                UPDATE schedule_task
                SET fabric_type = 'UNKNOWN'
                WHERE fabric_type IS NULL OR TRIM(fabric_type) = ''
                """
            )
            cursor.execute(
                """
                UPDATE schedule_task
                SET order_quantity = 0
                WHERE order_quantity IS NULL
                """
            )
            cursor.execute(
                """
                UPDATE schedule_task
                SET quantity_unit = 'm'
                WHERE quantity_unit IS NULL OR TRIM(quantity_unit) = ''
                """
            )

            # 只有在当前字段存在时才调整约束，重复执行不会报错。
            if _column_exists(cursor, "schedule_task", "order_no"):
                cursor.execute("ALTER TABLE schedule_task MODIFY order_no VARCHAR(64) NOT NULL")
            if _column_exists(cursor, "schedule_task", "fabric_type"):
                cursor.execute("ALTER TABLE schedule_task MODIFY fabric_type VARCHAR(64) NOT NULL")
            if _column_exists(cursor, "schedule_task", "order_quantity"):
                cursor.execute("ALTER TABLE schedule_task MODIFY order_quantity DOUBLE NOT NULL")
            if _column_exists(cursor, "schedule_task", "quantity_unit"):
                cursor.execute("ALTER TABLE schedule_task MODIFY quantity_unit VARCHAR(16) NOT NULL DEFAULT 'm'")

            if not _index_exists(cursor, "schedule_task", "uk_schedule_task_order_no"):
                cursor.execute("ALTER TABLE schedule_task ADD UNIQUE KEY uk_schedule_task_order_no (order_no)")

            for col in legacy_columns:
                if _column_exists(cursor, "schedule_task", col):
                    cursor.execute(f"ALTER TABLE schedule_task DROP COLUMN {col}")
    finally:
        conn.close()


if __name__ == "__main__":
    create_database()
    create_tables()
    migrate_schedule_task_columns()
    print(f"Database ready: {settings.db_name}")
