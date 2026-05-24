import unittest

from app.core.domain import machine_name_display, machine_type_display


class MachineAliasDisplayTest(unittest.TestCase):
    def test_machine_type_display_for_english(self) -> None:
        self.assertEqual(machine_type_display("WEAVE"), "织机")
        self.assertEqual(machine_type_display("warp"), "整经")

    def test_machine_type_display_for_chinese_and_unknown(self) -> None:
        self.assertEqual(machine_type_display("染色"), "染色")
        self.assertEqual(machine_type_display("LINEA"), "LINEA")

    def test_machine_name_display_for_english_prefix(self) -> None:
        self.assertEqual(machine_name_display("WEAVE-01", "WEAVE"), "织机-01")
        self.assertEqual(machine_name_display("WARP-12", "WARP"), "整经-12")

    def test_machine_name_display_for_existing_chinese_or_empty(self) -> None:
        self.assertEqual(machine_name_display("织机-01", "WEAVE"), "织机-01")
        self.assertEqual(machine_name_display("", "FINISH"), "后整理")


if __name__ == "__main__":
    unittest.main()
