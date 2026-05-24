from sqlalchemy.exc import OperationalError

from app.db.base import Base
from app.db.session import engine
from app import models  # noqa: F401


def create_tables() -> None:
    Base.metadata.create_all(bind=engine)


if __name__ == "__main__":
    try:
        create_tables()
        print("Tables created successfully")
    except OperationalError as exc:
        print(f"Create tables failed: {exc}")
        raise
