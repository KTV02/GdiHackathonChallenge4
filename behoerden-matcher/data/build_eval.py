"""eval_1000.txt (Abschnitte 'kategorie:' + eine Anfrage je Zeile) -> eval_1000.jsonl (gleiches Format wie eval_queries.jsonl).

Gleiche Anfragen in mehreren Abschnitten werden zusammengeführt (alle genannten Kategorien gelten als richtig).
    python data/build_eval.py
"""
import json
import sys
from pathlib import Path

here = Path(__file__).parent
cases: dict[str, list[str]] = {}
category = None
for raw in (here / (sys.argv[1] if len(sys.argv) > 1 else "eval_1000.txt")).read_text(encoding="utf-8").splitlines():
    line = raw.strip()
    if not line or line.startswith("#"):
        continue
    if line.endswith(":") and " " not in line and " => " not in line:
        category = line[:-1]
        continue
    if " => " in line:                      # "Anfrage => kat1,kat2": weitere zulässige Kategorien
        query, extra = line.split(" => ", 1)
        expected = cases.setdefault(query.strip(), [])
        expected += [c.strip() for c in extra.split(",") if c.strip() and c.strip() not in expected]
        continue
    expected = cases.setdefault(line, [])
    if category not in expected:
        expected.append(category)
out = (here / (sys.argv[1] if len(sys.argv) > 1 else "eval_1000.txt")).with_suffix(".jsonl")
with out.open("w", encoding="utf-8") as fh:
    fh.write("# Erwartungen sind Annahmen (nicht amtlich belegt); erzeugt aus eval_1000.txt\n")
    for query, expected in cases.items():
        fh.write(json.dumps({"anliegen": query, "erwartet": expected}, ensure_ascii=False) + "\n")
print(f"{len(cases)} Anfragen, {sum(len(v) > 1 for v in cases.values())} mit mehreren zulässigen Kategorien -> {out}")
