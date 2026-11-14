"""Treffergenauigkeit des Matchers an einer Liste von Beispiel-Anliegen messen.

    python -m authority_matcher.evaluate [datei.jsonl] [--no-semantic] [--no-live] [--details]

Jede Zeile der Datei: {"anliegen": "...", "erwartet": ["finanzamt"], "leistung_enthaelt": "Schenkung"}
(``leistung_enthaelt`` ist optional). Die Kategorien kommen erst aus der PVOG-Anreicherung (JZuFi); ohne sie (--no-live) bleibt die Kategorie
immer leer und es wird nur die Leistungsauswahl beurteilt. Die Erwartungen sind Annahmen und gehören geprüft; die Auswertung
zeigt vor allem, *welche* Leistung und Kategorie ein Anliegen trifft, damit sich Änderungen am
Matcher vorher/nachher vergleichen lassen.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

DEFAULT_FILE = Path(__file__).resolve().parents[1] / "data" / "eval_queries.jsonl"


def load_cases(path: Path) -> list[dict]:
    cases = []
    for n, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        try:
            case = json.loads(line)
        except json.JSONDecodeError as exc:
            raise ValueError(f"{path}:{n}: ungültiges JSON ({exc})") from exc
        if not case.get("anliegen"):
            raise ValueError(f"{path}:{n}: 'anliegen' fehlt")
        cases.append(case)
    return cases


def judge(case: dict, result: dict) -> dict:
    """Einen Fall bewerten: Kategorie (nur wenn der Matcher eine liefert) und Leistungstitel."""
    expected = case.get("erwartet") or []
    category = result.get("behoerdenkategorie")
    top = result.get("top_kategorie")
    services = result.get("leistungen") or []
    title = services[0].get("titel") if services else None
    needle = case.get("leistung_enthaelt")
    if not expected or (category is None and top is None):
        category_ok = None                   # nichts zu prüfen bzw. keine Kategorie geliefert (siehe 'ohne Kategorie')
    elif category is not None:
        category_ok = category in expected
    else:                                    # mehrdeutig: nicht falsch, wenn die erwartete Kategorie vorn liegt
        category_ok = None if top in expected else False
    service_ok = None if not needle else bool(title and needle.lower() in title.lower())
    return {"anliegen": case["anliegen"], "status": result.get("status"), "kategorie": category, "top": top,
            "leistung": title, "kategorie_ok": category_ok, "leistung_ok": service_ok,
            "erwartet": expected}


def evaluate(cases: list[dict], match) -> list[dict]:
    return [judge(case, match(case["anliegen"])) for case in cases]


def summarize(rows: list[dict]) -> str:
    def mark(value):
        return "–" if value is None else "ja" if value else "NEIN"
    lines = []
    for r in rows:
        lines.append(f"[Kat {mark(r['kategorie_ok']):>4}] [Leistung {mark(r['leistung_ok']):>4}] {r['anliegen']}")
        lines.append(f"      Matcher: {r['status']}, Kategorie={r['kategorie'] or r['top'] or '-'} "
                     f"(erwartet: {', '.join(r['erwartet']) or '-'}), Leistung={r['leistung'] or '-'}")
    wrong_cat = sum(r["kategorie_ok"] is False for r in rows)
    wrong_svc = sum(r["leistung_ok"] is False for r in rows)
    no_cat = sum(r["kategorie"] is None for r in rows)
    lines.append(f"\n{len(rows)} Fälle: {wrong_cat} falsche Kategorie, {wrong_svc} falsche Leistung, "
                 f"{no_cat} ohne eindeutige Kategorie ('–' = nicht bewertbar)")
    return "\n".join(lines)


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(prog="authority_matcher.evaluate", description=__doc__.splitlines()[0])
    p.add_argument("datei", nargs="?", type=Path, default=DEFAULT_FILE)
    p.add_argument("--no-semantic", action="store_true")
    p.add_argument("--no-live", action="store_true",
                   help="ohne PVOG-Anreicherung (schneller, aber dann haben die Leistungen keine Behördenkategorie)")
    p.add_argument("--no-start-docker", action="store_true")
    p.add_argument("--details", action="store_true", help="Top-3-Leistungen je Fall mit ausgeben")
    args = p.parse_args(argv)
    cases = load_cases(args.datei)

    from authority_matcher import autorun as am
    from authority_matcher.catalog import AuthorityTaxonomy
    from authority_matcher.matcher import match_authority
    semantic = not args.no_semantic
    am.get_index_docs(semantic=semantic, force=False, no_start_docker=args.no_start_docker)
    tax = AuthorityTaxonomy(am.DEFAULT_TAXONOMY)
    regions = am.reference_regions(None)

    def match(text: str) -> dict:
        enricher = am.make_enricher(am.DB, tax, regions=regions, max_keys=3, trace=[]) if not args.no_live else None
        return match_authority(text, backend="elasticsearch", es_url=am.ES_URL, es_index=am.INDEX,
                               semantic=semantic, embedding_model=am.MODEL, enrich=enricher)

    results = [match(case["anliegen"]) for case in cases]
    rows = [judge(case, result) for case, result in zip(cases, results)]
    print(summarize(rows))
    if args.details:
        for case, result in zip(cases, results):
            print(f"\n{case['anliegen']}")
            for s in (result.get("leistungen") or [])[:3]:
                print(f"   {s.get('score'):.5f}  {s.get('titel')}  {s.get('behoerdenkategorien')}")
    return 1 if any(r["kategorie_ok"] is False or r["leistung_ok"] is False for r in rows) else 0


if __name__ == "__main__":
    sys.exit(main())
