import importlib.util
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location("homophones", Path(__file__).parents[1] / "scripts/build_homophones.py")
homophones = importlib.util.module_from_spec(spec)
spec.loader.exec_module(homophones)

TERRA = "---\nname: terra_pinyin\n...\n\n好\thao3\t95%\n好\thao4\t5%\n郝\thao3\n的\tde5\t98%\n的\tdi2\t1%\n敵\tdi2\n好人\thao3 ren2\n龘\tda2\n"
ESSAY = "郝\t5\n好\t100\n好人\t50\n的\t900\n"
TSV = "gz\t好\nx\t郝\nxx\t郝\nd\t的\ne\t敵\n"


class HomophoneBuildTest(unittest.TestCase):
    def test_frequency_order_shortest_code_and_typable_only(self):
        text, count = homophones.build(TERRA, ESSAY, TSV)
        lines = text.splitlines()
        self.assertEqual(lines[lines.index("hao3\t好\tgz") + 1], "hao3\t郝\tx")
        self.assertIn("hao4\t好\tgz", lines)
        self.assertFalse(any("龘" in l for l in lines), "characters missing from the Boshiamy table are dropped")
        self.assertFalse(any(l.startswith("hao3 ren2") for l in lines), "phrases are ignored")
        self.assertEqual(count, len(lines))

    def test_rare_polyphone_readings_are_dropped(self):
        lines = homophones.build(TERRA, ESSAY, TSV)[0].splitlines()
        self.assertNotIn("di2\t的\td", lines)
        self.assertIn("di2\t敵\te", lines)


if __name__ == "__main__":
    unittest.main()
