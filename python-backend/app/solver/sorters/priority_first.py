from datetime import datetime

from app.models import ScheduleTask
from app.solver.sorters.base import TaskSorter

_DUE_URGENCY_RANK = {"P0": 3, "P1": 2, "P2": 1, "加急": 3, "紧急": 2, "常规": 1}
_CUSTOMER_PRIORITY_RANK = {"P0": 3, "P1": 2, "P2": 1, "战略": 3, "重点": 2, "普通": 1}


class PriorityFirstSorter(TaskSorter):
    name = "priority_first"

    def sort(self, tasks: list[ScheduleTask]) -> list[ScheduleTask]:
        def key(task: ScheduleTask):
            customer_priority = _CUSTOMER_PRIORITY_RANK.get(
                (task.customer_priority_level or "").strip(),
                1,
            )
            due_urgency = _DUE_URGENCY_RANK.get(
                (task.due_urgency_level or "").strip(),
                1,
            )
            due_date = task.due_date or datetime.utcnow().date()
            return (
                -customer_priority,
                -due_urgency,
                -(task.priority_level or 1),
                due_date,
                task.id,
            )

        return sorted(tasks, key=key)
