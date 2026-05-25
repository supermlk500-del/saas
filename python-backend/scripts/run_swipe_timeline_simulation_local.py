from __future__ import annotations

from pathlib import Path
import sys

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from app.db.base import Base
from app import models  # noqa: F401

import run_swipe_timeline_simulation as swipe_module


def run_local() -> None:
    output_dir = ROOT / "数据"
    output_dir.mkdir(parents=True, exist_ok=True)
    db_file = output_dir / "swipe_local_test.db"

    engine = create_engine(f"sqlite+pysqlite:///{db_file}", future=True)
    Base.metadata.drop_all(bind=engine)
    Base.metadata.create_all(bind=engine)

    local_session = sessionmaker(bind=engine, autoflush=False, autocommit=False, future=True)
    swipe_module.SessionLocal = local_session
    swipe_module.run_swipe_timeline_simulation()


if __name__ == "__main__":
    run_local()
