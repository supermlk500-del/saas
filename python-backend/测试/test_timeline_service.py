import csv
import tempfile
import unittest
from pathlib import Path

from app.services.timeline_service import (
    discover_timeline_files,
    load_summary_for_timeline,
    load_timeline_events,
)


class TimelineServiceTest(unittest.TestCase):
    def test_discover_timeline_files_sorted_by_name_desc(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            out = Path(tmpdir)
            (out / "swipe_timeline_20260504135111.csv").write_text("", encoding="utf-8")
            (out / "swipe_timeline_20260505112011.csv").write_text("", encoding="utf-8")
            (out / "other.csv").write_text("", encoding="utf-8")

            files = discover_timeline_files(out)
            self.assertEqual(
                [x.name for x in files],
                ["swipe_timeline_20260505112011.csv", "swipe_timeline_20260504135111.csv"],
            )

    def test_load_timeline_events(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            csv_file = Path(tmpdir) / "swipe_timeline_20260505112011.csv"
            with csv_file.open("w", encoding="utf-8-sig", newline="") as f:
                writer = csv.DictWriter(
                    f,
                    fieldnames=["event_time", "event_type", "event_title", "detail", "ref_no"],
                )
                writer.writeheader()
                writer.writerow(
                    {
                        "event_time": "2026-05-05 11:20:11",
                        "event_type": "INSERT",
                        "event_title": "Insert orders",
                        "detail": "Added 200 high-priority insert orders",
                        "ref_no": "INSERT-20260505112011",
                    }
                )

            events = load_timeline_events(csv_file)
            self.assertEqual(len(events), 1)
            self.assertEqual(events[0]["event_type"], "INSERT")
            self.assertEqual(events[0]["event_title"], "Insert orders")

    def test_load_summary_for_timeline(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            out = Path(tmpdir)
            (out / "swipe_timeline_20260505112011.csv").write_text("", encoding="utf-8")
            (out / "swipe_summary_20260505112011.json").write_text(
                '{"insert_order_count":200,"timeline_days":30}',
                encoding="utf-8",
            )

            summary = load_summary_for_timeline(out, "swipe_timeline_20260505112011.csv")
            self.assertIsNotNone(summary)
            assert summary is not None
            self.assertEqual(summary["insert_order_count"], 200)
            self.assertEqual(summary["timeline_days"], 30)


if __name__ == "__main__":
    unittest.main()
