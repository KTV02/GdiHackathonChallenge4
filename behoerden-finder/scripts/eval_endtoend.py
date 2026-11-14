"""Gesamtauswertung mit echtem Matcher (Elasticsearch + PVOG): welche Stufe liefert wie viele richtige Behörden?

    PYTHONPATH=src:../behoerden-matcher python3 scripts/eval_endtoend.py [dateien.jsonl ...] [--fehler] [--no-live]

Je Anfrage läuft der Matcher einmal; die Stufen werden danach offline daraus abgeleitet:
  1 Matcher allein (amtliche Daten)            3 + Fall-Katalog, amtliche Matcher-Ergebnisse haben Vorrang
  2 Matcher + Stichwort-Rückfall               4 Fall-Katalog hat Vorrang (Standard der Anwendung)
Bewertung: richtig = erwartete Kategorie; falsch = andere Kategorie; keine = nichts geliefert; bei mehrdeutigem
Matcher-Ergebnis zählt 'vorn' (erwartete Kategorie liegt vorn) nicht als richtig, wird aber ausgewiesen.
"""
import argparse
import json
import sys
from pathlib import Path

from behoerden_finder import faelle
from behoerden_finder.matching import AuthorityMatcher, apply_topic_hints

DATA = Path(__file__).resolve().parents[2] / "behoerden-matcher" / "data"
DEFAULT = [DATA / "eval_1000.jsonl", DATA / "eval_holdout.jsonl", DATA / "eval_holdout2.jsonl"]
STAGES = ["1 Matcher allein", "2 + Stichwörter", "3 + Katalog (Matcher zuerst)", "4 + Katalog zuerst"]


def judge(expected, outcome):
    if outcome.category is not None:
        return "richtig" if outcome.category in expected else "falsch"
    top = outcome.candidates[0]["id"] if outcome.candidates else None
    return "vorn" if top in expected else "keine"


def main():
    p = argparse.ArgumentParser()
    p.add_argument("dateien", nargs="*", type=Path, default=DEFAULT)
    p.add_argument("--fehler", action="store_true", help="falsche Zuordnungen der Stufe 4 auflisten")
    p.add_argument("--no-live", action="store_true")
    p.add_argument("--no-semantic", action="store_true")
    a = p.parse_args()
    matcher = AuthorityMatcher(semantic=not a.no_semantic, live=not a.no_live, topic_hints=False)
    cat = faelle.ensure_installed()
    for path in a.dateien:
        if not path.exists():
            continue
        cases = [json.loads(l) for l in path.read_text(encoding="utf-8").splitlines() if l.strip() and not l.startswith("#")]
        tally = {s: {"richtig": 0, "falsch": 0, "vorn": 0, "keine": 0} for s in STAGES}
        wrong = []
        for c in cases:
            base = matcher.match(c["anliegen"])
            variants = [base, apply_topic_hints(base, c["anliegen"]),
                        apply_topic_hints(base, c["anliegen"], cat, faelle_vorrang=False),
                        apply_topic_hints(base, c["anliegen"], cat)]
            for stage, outcome in zip(STAGES, variants):
                r = judge(c["erwartet"], outcome)
                tally[stage][r] += 1
                if stage == STAGES[3] and r == "falsch":
                    wrong.append((c["anliegen"], c["erwartet"], outcome.category, outcome.source))
        n = len(cases)
        print(f"\n{path.name}  ({n} Anfragen)")
        for stage in STAGES:
            t = tally[stage]
            print(f"  {stage:30} richtig {t['richtig']/n:6.1%}  vorn {t['vorn']/n:6.1%}  falsch {t['falsch']/n:6.1%}  keine {t['keine']/n:6.1%}")
        if a.fehler:
            for q, exp, got, src in wrong:
                print(f"    FALSCH {q} | erwartet {','.join(exp)} | bekam {got} ({src})")


if __name__ == "__main__":
    sys.exit(main())
