#!/usr/bin/env python3
import csv
from collections import Counter
from pathlib import Path
root = Path(__file__).resolve().parents[1] / "app/src/main/assets/hsk2025"
def load(name):
    with (root / name).open(encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f, delimiter="\t", quoting=csv.QUOTE_NONE))
def counts(rows, key):
    c = Counter(int(r[key]) for r in rows if r[key])
    return [c[i] for i in range(1, 8)]
v, h, g = (load(x) for x in ("vocabulary.tsv", "hanzi.tsv", "grammar.tsv"))
assert counts(v, "level") == [300,200,500,1000,1600,1800,5600]
assert [int(r["serial"]) for r in v] == list(range(1,11001))
assert all(r["word"] and r["pinyin"] for r in v)
assert counts(h, "recognition_level") == [246,125,284,441,431,413,1148]
assert len({r["hanzi"] for r in h}) == 3088
assert all(r["readings"] for r in h)
assert counts(h, "writing_level") == [0,100,150,150,150,150,500]
assert all(not r["writing_level"] or int(r["writing_level"]) >= int(r["recognition_level"]) for r in h)
assert counts(g, "level") == [70,78,96,95,70,50,134]
assert [int(r["serial"]) for r in g] == list(range(1,594))
assert all(any(r[k] for k in ("content", "detail", "category", "type")) for r in g)
print("PASS: 11000 vocabulary records, 3088 recognition characters, 1200 writing characters, 593 grammar headings")

english = [row for band in range(1,8) for row in load(f"cedict-{band}.tsv")]
assert len(english) == 10734
assert len({int(r["serial"]) for r in english}) == 10734
assert all(1 <= int(r["serial"]) <= 11000 and r["english"] for r in english)
print("PASS: 10734 independently licensed English gloss records")
