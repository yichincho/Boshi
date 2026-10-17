"""Append frequent traditional-Chinese Rime phrases to the conversation seeds.

The original LGPL-3.0 Rime essay and attribution are bundled separately by CI.
"""
import argparse
from pathlib import Path


def build(seed, essay):
    words = {}
    for line in essay.splitlines():
        fields = line.split("\t")
        if len(fields) != 2:
            continue
        phrase, frequency = fields
        if not 2 <= len(phrase) <= 12 or not all(
            "\u3400" <= c <= "\u9fff" or "\U00020000" <= c <= "\U0002fa1f"
            for c in phrase
        ):
            continue
        try:
            words[phrase] = max(words.get(phrase, 0), int(frequency))
        except ValueError:
            continue
    ordered = dict.fromkeys(
        line.strip() for line in seed.splitlines()
        if line.strip() and not line.startswith("#")
    )
    for phrase in sorted(words, key=lambda word: (-words[word], word))[:60000]:
        ordered[phrase] = None
    return "\n".join(ordered) + "\n", len(words)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("essay", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    combined, count = build(args.output.read_text(), args.essay.read_text(encoding="utf-8-sig"))
    assert count > 10000, f"Unexpected Rime essay vocabulary: {count}"
    args.output.write_text(combined, encoding="utf-8")
    print(f"Bundled {len(combined.splitlines())} offline phrases from {count} eligible Rime entries")
