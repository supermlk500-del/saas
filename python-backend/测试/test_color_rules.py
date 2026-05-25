from types import SimpleNamespace
import unittest

from app.solver.color_rules import ColorRules
from app.solver.constraints.dye_color_changeover import DyeColorChangeoverConstraint


DEMO_CFG = {
    "dedicated_machine_map": "DY-01:BLACK;DY-02:WHITE;DY-03:NAVY;DY-04:RED",
    "dedicated_strict": "true",
    "color_family_map": (
        "BLACK:BLACK;WHITE:WHITE;NAVY:BLUE;LIGHT_BLUE:BLUE;BLUE:BLUE;"
        "KHAKI:EARTH;LIGHT_KHAKI:EARTH;BEIGE:EARTH;"
        "RED:RED;LIGHT_RED:RED;DARK_GRAY:BLACK;LIGHT_GRAY:WHITE"
    ),
    "color_depth_map": (
        "WHITE:1;BEIGE:2;LIGHT_KHAKI:2;LIGHT_GRAY:2;LIGHT_RED:2;"
        "LIGHT_BLUE:2;BLUE:2;KHAKI:3;RED:3;DARK_GRAY:4;NAVY:4;BLACK:5"
    ),
    "color_undertone_map": "BLACK_GREEN:GREEN;BLACK_RED:RED",
    "default_change_loss": "0.5",
    "same_color_loss": "0",
    "cross_family_penalty": "1.0",
    "light_to_dark_loss": "0.3",
    "dark_to_light_loss": "2.0",
    "undertone_change_loss": "1.5",
    "undertone_requires_wash": "true",
    "mandatory_wash_pairs": "BLACK>WHITE;RED>WHITE;NAVY>BEIGE",
    "wash_duration_hours": "2.0",
    "max_continuous_same_color": "3",
    "max_continuous_same_family": "6",
    "continuous_violation_as_hard": "false",
}


class ColorRulesTest(unittest.TestCase):
    def test_color_rules_parsing_uses_defaults_and_demo_maps(self):
        rules = ColorRules.from_cfg(DEMO_CFG)

        self.assertEqual(4, len(rules.dedicated_machine_map))
        self.assertEqual(3, len(rules.mandatory_wash_pairs))
        self.assertTrue(rules.dedicated_strict)
        self.assertTrue(rules.undertone_requires_wash)
        self.assertEqual(3, rules.max_continuous_same_color)
        self.assertEqual("EARTH", rules.family_of("light_khaki"))

        default_rules = ColorRules.from_cfg({})
        self.assertEqual(0.5, default_rules.default_change_loss)
        self.assertFalse(default_rules.violates_continuous_same_color([], "BLACK"))

    def test_dye_changeover_three_tier_cost_and_hard_dedicated_check(self):
        rules = ColorRules.from_cfg(DEMO_CFG)

        self.assertEqual((2.5, False), rules.compute_changeover_hours("KHAKI", "LIGHT_KHAKI", "DY-05"))
        self.assertEqual((3.5, False), rules.compute_changeover_hours("KHAKI", "BLUE", "DY-05"))
        self.assertEqual((5.5, True), rules.compute_changeover_hours("BLACK", "WHITE", "DY-05"))

        constraint = DyeColorChangeoverConstraint()
        task = SimpleNamespace(color_code="KHAKI")
        state = SimpleNamespace(
            last_color="BLACK",
            recent_colors=[],
            machine=SimpleNamespace(machine_code="DY-01"),
        )

        verdict = constraint.check(task, state, None, DEMO_CFG)
        self.assertFalse(verdict.passed)
        self.assertTrue(verdict.is_hard)
        self.assertEqual("color_not_allowed_on_dedicated_vat", verdict.reason_code)


if __name__ == "__main__":
    unittest.main()
