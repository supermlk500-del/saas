from datetime import datetime

from app.models import ScheduleTask
from app.solver.sorters.base import TaskSorter


class CriticalRatioSorter(TaskSorter):
    name = "cr"

    def sort(self, tasks: list[ScheduleTask]) -> list[ScheduleTask]:
        now = datetime.utcnow().date()

        def key(task: ScheduleTask):
            due_date = task.due_date or now
            remaining_days = max((due_date - now).days, 0)
            work_proxy = max(float(task.order_quantity or 0), 1.0)
            critical_ratio = remaining_days / work_proxy
            return (critical_ratio, due_date, task.id)

        return sorted(tasks, key=key)
