"""Offline-Auswertung der Testmenge (ohne Elasticsearch): Fall-Katalog und Stichwort-Tabelle.

    PYTHONPATH=src python3 scripts/eval_offline.py [../behoerden-matcher/data/eval_1000.jsonl] [--fehler] [--kat <id>]
"""
import argparse
import json
import sys
from collections import Counter, defaultdict
from pathlib import Path

from behoerden_finder import faelle, themen

DEFAULT = Path(__file__).resolve().parents[2] / "behoerden-matcher" / "data" / "eval_1000.jsonl"


def load(path):
    return [json.loads(l) for l in path.read_text(encoding="utf-8").splitlines() if l.strip() and not l.startswith("#")]


def main():
    p = argparse.ArgumentParser()
    p.add_argument("datei", nargs="?", type=Path, default=DEFAULT)
    p.add_argument("--fehler", action="store_true", help="falsche und fehlende Zuordnungen auflisten")
    p.add_argument("--kat", help="nur diese erwartete Kategorie")
    a = p.parse_args()
    cases = load(a.datei)
    cat = faelle.builtin()
    stats = defaultdict(Counter)
    rows = []
    for c in cases:
        exp = c["erwartet"]
        h = cat.infer(c["anliegen"])
        t = themen.infer(c["anliegen"])
        k = h.category if h else (t.category if t else None)
        src = "katalog" if h else ("stichwort" if t else "-")
        res = "ok" if k in exp else ("keine" if k is None else "falsch")
        for e in exp:
            stats[e][res] += 1
        rows.append((res, c["anliegen"], exp, k, src))
    tot = Counter(r[0] for r in rows)
    n = len(rows)
    print(f"{n} Anfragen: richtig {tot['ok']} ({tot['ok']/n:.1%}), falsch {tot['falsch']} ({tot['falsch']/n:.1%}), "
          f"keine Zuordnung {tot['keine']} ({tot['keine']/n:.1%})")
    print("\nje erwarteter Kategorie (richtig/falsch/keine):")
    for e in sorted(stats, key=lambda x: -sum(stats[x].values())):
        s = stats[e]
        print(f"  {e:26} {s['ok']:3}/{s['falsch']:3}/{s['keine']:3}")
    if a.fehler:
        print()
        for res, q, exp, k, src in rows:
            if res != "ok" and (not a.kat or a.kat in exp):
                print(f"[{res:6}] {q}  | erwartet {','.join(exp)} | bekam {k} ({src})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
