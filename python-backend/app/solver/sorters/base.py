from abc import ABC, abstractmethod

from app.models import ScheduleTask


class TaskSorter(ABC):
    name: str

    @abstractmethod
    def sort(self, tasks: list[ScheduleTask]) -> list[ScheduleTask]:
        ...
