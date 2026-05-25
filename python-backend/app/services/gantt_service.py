from app.models import ScheduleTask


def build_gantt_lanes(
    items, day_window_start, day_window_end, db,
    order_color_map, order_palette,
    machine_map, machine_type_map,
    changed_task_ids=None,
) -> list[dict]:
    """把 plan items 转换成甘特图 lanes 数据，供 JSON API 直接返回。"""
    if changed_task_ids is None:
        changed_task_ids = set()
    CANVAS_PX = 2400
    MIN_BAR_PX = 90
    BAR_H = 44
    BAR_GAP = 6
    TRACK_PAD = 6

    grouped: dict[int, list] = {}
    for i in items:
        grouped.setdefault(i.machine_id, []).append(i)

    result = []
    for machine_id in sorted(grouped.keys()):
        bars = []
        row_ends: list[float] = []
        for i in sorted(grouped[machine_id], key=lambda x: x.start_time):
            if i.end_time <= day_window_start or i.start_time >= day_window_end:
                continue
            o_start = max(i.start_time, day_window_start)
            o_end = min(i.end_time, day_window_end)
            left_pct = ((o_start - day_window_start).total_seconds() / 3600) / 24 * 100
            width_pct = max(((o_end - o_start).total_seconds() / 3600) / 24 * 100,
                            MIN_BAR_PX / CANVAS_PX * 100)
            row = next((r for r, end in enumerate(row_ends) if end <= left_pct + 0.05), len(row_ends))
            if row == len(row_ends):
                row_ends.append(0.0)
            row_ends[row] = left_pct + width_pct
            task = db.get(ScheduleTask, i.task_id)
            label = task.order_no if task else f"任务-{i.task_id}"
            if label not in order_color_map:
                order_color_map[label] = order_palette[abs(hash(label)) % len(order_palette)]
            is_overdue = bool(task and task.due_date and i.end_time.date() > task.due_date)
            bars.append({
                "left": round(left_pct, 2),
                "width": round(width_pct, 2),
                "top_px": TRACK_PAD + row * (BAR_H + BAR_GAP),
                "label": label,
                "start": i.start_time,
                "end": i.end_time,
                "qty": i.planned_quantity,
                "color": order_color_map.get(label, "#334155"),
                "machine_type": machine_type_map.get(machine_id, "未知"),
                "is_overdue": is_overdue,
                "is_changed": i.task_id in changed_task_ids,
                "is_compensated": (i.item_status or "").lower() == "compensated",
            })
        if not bars:
            continue
        num_rows = len(row_ends)
        result.append({
            "machine_id": machine_id,
            "machine_name": machine_map.get(machine_id, f"机台#{machine_id}"),
            "bars": bars,
            "track_h": TRACK_PAD * 2 + num_rows * BAR_H + max(0, num_rows - 1) * BAR_GAP,
        })
    return result
