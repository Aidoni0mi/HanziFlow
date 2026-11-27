#!/usr/bin/env python3
import csv
import re
import unicodedata
from collections import defaultdict
from pathlib import Path

path = Path(__file__).resolve().parents[1] / "app/src/main/assets/life_course.tsv"
expected_header = [
    "situation_id", "category_ru", "situation_title_ru", "level", "text_id", "kind",
    "text_title_ru", "line_order", "speaker", "hanzi", "pinyin", "russian",
]
with path.open(encoding="utf-8", newline="") as handle:
    reader = csv.DictReader(handle, delimiter="\t", quoting=csv.QUOTE_NONE)
    assert reader.fieldnames == expected_header
    rows = list(reader)

assert rows and all(None not in row for row in rows)
assert all(all(row[name].strip() for name in expected_header) for row in rows)
assert all(re.fullmatch(r"[a-z0-9][a-z0-9_-]*", row["situation_id"]) for row in rows)
assert all(re.fullmatch(r"[a-z0-9][a-z0-9_-]*", row["text_id"]) for row in rows)
assert all(1 <= int(row["level"]) <= 4 for row in rows)
assert {int(row["level"]) for row in rows} == {1, 2, 3, 4}
assert all(row["kind"] in {"DIALOGUE", "STORY"} for row in rows)
assert all("\ufffd" not in row["hanzi"] + row["pinyin"] + row["russian"] for row in rows)

def has_script(value, script_prefix):
    return any(unicodedata.name(char, "").startswith(script_prefix) for char in value)

tone_combining_marks = {"\u0304", "\u0301", "\u030c", "\u0300"}
assert all(has_script(row["hanzi"], "CJK UNIFIED IDEOGRAPH") for row in rows)
assert all(has_script(row["russian"], "CYRILLIC") for row in rows)
assert all(not has_script(row["pinyin"], "CJK UNIFIED IDEOGRAPH") for row in rows)
assert all(
    any(mark in unicodedata.normalize("NFD", row["pinyin"]) for mark in tone_combining_marks)
    or any(char in "12345" for char in row["pinyin"])
    for row in rows
)

by_situation = defaultdict(list)
by_text = defaultdict(list)
for row in rows:
    by_situation[row["situation_id"]].append(row)
    by_text[row["text_id"]].append(row)

assert len(by_situation) >= 16
assert len(by_text) == len(by_situation) * 2
for situation_rows in by_situation.values():
    assert len({row["category_ru"] for row in situation_rows}) == 1
    assert len({row["situation_title_ru"] for row in situation_rows}) == 1
    assert len({row["level"] for row in situation_rows}) == 1
    assert {row["kind"] for row in situation_rows} == {"DIALOGUE", "STORY"}
    assert len({row["text_id"] for row in situation_rows}) == 2
for text_rows in by_text.values():
    assert len({row["situation_id"] for row in text_rows}) == 1
    assert len(text_rows) >= 2
    assert [int(row["line_order"]) for row in text_rows] == list(range(1, len(text_rows) + 1))
    assert len({row["kind"] for row in text_rows}) == 1
    assert len({row["text_title_ru"] for row in text_rows}) == 1
    if text_rows[0]["kind"] == "DIALOGUE":
        assert len({row["speaker"] for row in text_rows}) >= 2

print(f"PASS: {len(by_situation)} original life situations, {len(by_text)} dialogues/stories, {len(rows)} voiced lines")
