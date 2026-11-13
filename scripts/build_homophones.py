#!/usr/bin/env python3
"""Build the bundled homophone table (同音字) for the Boshi keyboard.

Inputs:
  terra_pinyin.dict.yaml  - Rime Terra Pinyin (CC BY-SA 3.0, derived from CC-CEDICT)
  essay.txt               - Rime essay word frequencies, used only to order characters
  boshiamy.tsv            - the bundled Boshiamy table (code<TAB>character)

Output lines: ``reading<TAB>character<TAB>shortest Boshiamy code``,
grouped by reading and ordered from most to least frequent character.
Only characters that can actually be typed with the bundled table are kept.
"""
import re
import sys
from collections import defaultdict
from pathlib import Path

READING = re.compile(r"^[a-z]+[1-5]$")
MIN_PERCENT = 5.0  # drop rare readings such as 的 di2 (1%)


def is_han(ch):
    cp = ord(ch)
    return (0x3400 <= cp <= 0x4DBF or 0x4E00 <= cp <= 0x9FFF or 0xF900 <= cp <= 0xFAFF
            or 0x20000 <= cp <= 0x3134F or cp == 0x3007)


def parse_readings(terra_text):
    """Single-character readings, skipping YAML header and rare polyphone readings."""
    readings = defaultdict(list)
    body = terra_text.split("\n...", 1)[1] if "\n..." in terra_text else terra_text
    for line in body.splitlines():
        if not line or line.startswith("#"):
            continue
        fields = line.split("\t")
        if len(fields) < 2 or len(fields[0]) != 1 or not is_han(fields[0]):
            continue
        reading = fields[1].strip()
        if not READING.match(reading):
            continue
        if len(fields) > 2 and fields[2].strip().endswith("%"):
            try:
                if float(fields[2].strip()[:-1]) < MIN_PERCENT:
                    continue
            except ValueError:
                continue
        if reading not in readings[fields[0]]:
            readings[fields[0]].append(reading)
    return readings


def parse_frequency(essay_text):
    freq = defaultdict(int)
    for line in essay_text.splitlines():
        fields = line.split("\t")
        if len(fields) != 2:
            continue
        try:
            weight = int(fields[1])
        except ValueError:
            continue
        for ch in fields[0]:
            if is_han(ch):
                freq[ch] += weight
    return freq


def parse_codes(tsv_text):
    codes = {}
    for line in tsv_text.splitlines():
        fields = line.split("\t")
        if len(fields) != 2 or len(fields[1]) != 1 or not fields[0].isalpha():
            continue
        code, ch = fields[0].lower(), fields[1]
        best = codes.get(ch)
        if best is None or (len(code), code) < (len(best), best):
            codes[ch] = code
    return codes


def build(terra_text, essay_text, tsv_text):
    readings = parse_readings(terra_text)
    freq = parse_frequency(essay_text)
    codes = parse_codes(tsv_text)
    groups = defaultdict(list)
    for ch, rs in readings.items():
        if ch not in codes:
            continue
        for r in rs:
            groups[r].append(ch)
    lines = []
    for r in sorted(groups):
        for ch in sorted(groups[r], key=lambda c: (-freq.get(c, 0), c)):
            lines.append(f"{r}\t{ch}\t{codes[ch]}")
    return "\n".join(lines) + "\n", len(lines)


def main(argv):
    if len(argv) != 5:
        raise SystemExit("usage: build_homophones.py terra_pinyin.dict.yaml essay.txt boshiamy.tsv output.tsv")
    text, count = build(*(Path(p).read_text(encoding="utf-8-sig") for p in argv[1:4]))
    assert count > 10000, f"Unexpected homophone table size: {count}"
    Path(argv[4]).write_text(text, encoding="utf-8")
    print(f"Bundled {count} homophone entries")


if __name__ == "__main__":
    main(sys.argv)
