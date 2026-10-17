import importlib.util
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location("lexicon", Path(__file__).parents[1] / "scripts/build_offline_lexicon.py")
lexicon = importlib.util.module_from_spec(spec)
spec.loader.exec_module(lexicon)


class OfflineLexiconBuildTest(unittest.TestCase):
    def test_seed_precedence_frequency_and_deduplication(self):
        result, count = lexicon.build("# comment\n有時間\n", "有問題\t20\n有沒有\t100\n有時間\t2\n有問題\t30\n")
        self.assertEqual(result.splitlines(), ["有時間", "有沒有", "有問題"])
        self.assertEqual(count, 3)

    def test_invalid_entries_do_not_enter_asset(self):
        result, count = lexicon.build("有時間\n", "字\t100\nABC\t200\n有!\t300\n有問題\tnan\n有沒有\t20\n")
        self.assertEqual(result.splitlines(), ["有時間", "有沒有"])
        self.assertEqual(count, 1)


if __name__ == "__main__":
    unittest.main()
