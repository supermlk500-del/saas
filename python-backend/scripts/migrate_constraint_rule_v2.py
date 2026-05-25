"""
迁移脚本：给 constraint_rule 表新增三个字段
  - is_hard_constraint  TINYINT(1) NOT NULL DEFAULT 1
  - priority_level      INT        NOT NULL DEFAULT 3
  - rule_expression     TEXT       NULL

对应设计文档 md_constraint_rule 表的字段补齐。
已存在的记录会自动获得默认值，无需手动处理。

用法：
    python scripts/migrate_constraint_rule_v2.py
"""
from __future__ import annotations

from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from sqlalchemy import text
from app.db.session import SessionLocal


def run_migration() -> None:
    db = SessionLocal()
    try:
        # 检查字段是否已存在，避免重复执行报错
        result = db.execute(text("SHOW COLUMNS FROM constraint_rule")).fetchall()
        existing_cols = {row[0] for row in result}

        added = []

        if "is_hard_constraint" not in existing_cols:
            db.execute(text(
                "ALTER TABLE constraint_rule "
                "ADD COLUMN is_hard_constraint TINYINT(1) NOT NULL DEFAULT 1 "
                "COMMENT '1=硬约束(违反则任务unassigned), 0=软约束(违反仅记日志)'"
            ))
            added.append("is_hard_constraint")

        if "priority_level" not in existing_cols:
            db.execute(text(
                "ALTER TABLE constraint_rule "
                "ADD COLUMN priority_level INT NOT NULL DEFAULT 3 "
                "COMMENT '约束优先级：1最高，数字越大优先级越低'"
            ))
            added.append("priority_level")

        if "rule_expression" not in existing_cols:
            db.execute(text(
                "ALTER TABLE constraint_rule "
                "ADD COLUMN rule_expression TEXT NULL "
                "COMMENT '规则表达式：人类可读的业务规则描述，便于追溯'"
            ))
            added.append("rule_expression")

        db.commit()

        if added:
            print(f"[OK] 迁移完成，新增字段：{', '.join(added)}")
        else:
            print("[OK] 字段已存在，无需迁移。")

    except Exception as e:
        db.rollback()
        print(f"[ERROR] 迁移失败：{e}")
        raise
    finally:
        db.close()


if __name__ == "__main__":
    run_migration()
