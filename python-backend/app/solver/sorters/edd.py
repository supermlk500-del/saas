from datetime import datetime

from app.models import ScheduleTask
from app.solver.sorters.base import TaskSorter


class EarliestDueDateSorter(TaskSorter):
    name = "edd"

    def sort(self, tasks: list[ScheduleTask]) -> list[ScheduleTask]:
        fallback = datetime.max.date()
        return sorted(tasks, key=lambda task: (task.due_date or fallback, task.id))
