import csv
import json
from pathlib import Path


def discover_timeline_files(output_dir: Path) -> list[Path]:
    """Discover replay timeline CSV files."""
    return sorted(output_dir.glob("swipe_timeline_*.csv"), key=lambda p: p.name, reverse=True)


def load_timeline_events(csv_file: Path) -> list[dict[str, str]]:
    """Load timeline events from a CSV file."""
    if not csv_file.exists():
        return []

    with csv_file.open("r", encoding="utf-8-sig", newline="") as f:
        reader = csv.DictReader(f)
        rows = []
        for row in reader:
            rows.append(
                {
                    "event_time": (row.get("event_time") or "").strip(),
                    "event_type": (row.get("event_type") or "").strip(),
                    "event_title": (row.get("event_title") or "").strip(),
                    "detail": (row.get("detail") or "").strip(),
                    "ref_no": (row.get("ref_no") or "").strip(),
                }
            )
        return rows


def load_summary_for_timeline(output_dir: Path, timeline_filename: str) -> dict | None:
    """Load a summary JSON file matching a timeline CSV file."""
    prefix = "swipe_timeline_"
    suffix = ".csv"
    if not timeline_filename.startswith(prefix) or not timeline_filename.endswith(suffix):
        return None

    tag = timeline_filename[len(prefix) : -len(suffix)]
    summary_file = output_dir / f"swipe_summary_{tag}.json"
    if not summary_file.exists():
        return None
    try:
        return json.loads(summary_file.read_text(encoding="utf-8"))
    except Exception:
        return None
