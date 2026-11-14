"""Stellschrauben von Suche und Auswertung an einer Testmenge einstellen (Koordinatenabstieg).

    python -m authority_matcher.tune [--train data/eval_1000.jsonl] [--test data/eval_holdout.jsonl data/eval_holdout2.jsonl]
                                     [--no-semantic] [--no-fetch] [--runden 2] [--finder] [--ausgabe tune_ergebnis.json]

Ablauf:
1. Vorlauf: alle Anfragen einmal mit den aktuellen Werten gegen Elasticsearch; für die besten ``PREFETCH_KEYS``
   Leistungen werden fehlende PVOG-Belege (JZuFi) nachgeladen und in der Datenbank zwischengespeichert
   (abschaltbar mit ``--no-fetch``). Danach läuft alles ohne Netz.
2. Koordinatenabstieg: je Parameter werden die Kandidatenwerte probiert, der beste bleibt. Gewählt wird nur auf der
   Trainingsmenge; die Kontrollmengen (--test) werden mitgemessen, um Überanpassung zu erkennen.
   Treffer der Vektorsuche und der Wortsuche werden je Anfrage zwischengespeichert, daher ist ein Durchlauf schnell.
3. Mit ``--finder``: zusätzlich die Gesamtkette des behoerden-finder (Stichwörter, Fall-Katalog) vorher/nachher.

Bewertung je Anfrage: ``richtig`` = eindeutige erwartete Kategorie; ``falsch`` = andere Kategorie; ``vorn`` = mehrdeutig,
erwartete liegt vorn; ``keine`` = nichts. Zielwert: richtig + 0.5*vorn - GEWICHT_FALSCH*falsch (Standard 3).
Kein Neuaufbau des Index nötig (Analyzer, Synonyme und Embedding-Modell bleiben unverändert).
"""
from __future__ import annotations

import argparse
import json
import sys
import time
from contextlib import contextmanager
from pathlib import Path

from . import index as idx
from . import matcher as mt

DATA = Path(__file__).resolve().parents[1] / "data"
_self = sys.modules[__name__]

LIVE_TOP = 3          # so viele Leistungsschlüssel je Anfrage werden mit PVOG-Belegen versehen (wie --live-top)
PREFETCH_KEYS = 6     # Vorlauf lädt Belege für so viele Schlüssel, damit LIVE_TOP bis 6 offline probierbar ist

GRID: dict[str, tuple[object, str, list]] = {
    "QUERY_CLEAN": (idx, "QUERY_CLEAN", [False, True]),
    "FUZZY_BOOST": (idx, "FUZZY_BOOST", [0.0, 0.15, 0.3, 0.5]),
    "FUZZINESS": (idx, "FUZZINESS", ["AUTO:5,100", "AUTO:4,8", "AUTO:6,100"]),
    "LEXICAL_FIELDS": (idx, "LEXICAL_FIELDS", [["titel^4", "synonyme^3", "beschreibung"],
                                              ["titel^4", "synonyme^3", "beschreibung^0.5"],
                                              ["titel^3", "synonyme^4", "beschreibung^0.5"],
                                              ["titel^5", "synonyme^2", "beschreibung^0.2"]]),
    "LEXICAL_WEIGHT": (idx, "LEXICAL_WEIGHT", [0.5, 0.8, 1.0, 1.3]),
    "RRF_K": (idx, "RRF_K", [10, 30, 60, 100]),
    "SEARCH_SIZE": (mt, "SEARCH_SIZE", [40, 60, 80]),
    "MIN_SCORE_ES_SEMANTIC": (mt, "MIN_SCORE_ES_SEMANTIC", [0.009, 0.011, 0.013, 0.015, 0.017]),
    "MIN_SCORE_ES": (mt, "MIN_SCORE_ES", [0.008, 0.010, 0.012, 0.014]),
    "RANK_DECAY": (mt, "RANK_DECAY", [0.15, 0.25, 0.35, 0.6]),
    "MULTI_CATEGORY_WEIGHT": (mt, "MULTI_CATEGORY_WEIGHT", [0.4, 0.6, 0.8, 1.0]),
    "AMBIGUITY_RATIO": (mt, "AMBIGUITY_RATIO", [0.75, 0.8, 0.86, 0.92]),
    "AMBIGUITY_RATIO_MULTI": (mt, "AMBIGUITY_RATIO_MULTI", [0.6, 0.7, 0.8]),
    "LIVE_TOP": (_self, "LIVE_TOP", [3, 4, 6]),
}
GEWICHT_FALSCH = 3.0


def load_cases(paths: list[Path]) -> list[dict]:
    from .evaluate import load_cases as load
    cases = []
    for p in paths:
        cases += load(p)
    return [c for c in cases if c.get("erwartet")]


def current(grid=None) -> dict:
    return {name: getattr(mod, attr) for name, (mod, attr, _) in (grid or GRID).items()}


@contextmanager
def applied(params: dict):
    old = {}
    try:
        for name, value in params.items():
            mod, attr, _ = GRID[name]
            old[name] = getattr(mod, attr)
            setattr(mod, attr, value)
        yield
    finally:
        for name, value in old.items():
            mod, attr, _ = GRID[name]
            setattr(mod, attr, value)


def judge(expected: list[str], result: dict) -> str:
    category, top = result.get("behoerdenkategorie"), result.get("top_kategorie")
    if category is not None:
        return "richtig" if category in expected else "falsch"
    if top is not None:
        return "vorn" if top in expected else "keine"
    return "keine"


def score(counts: dict, weight_wrong: float = GEWICHT_FALSCH) -> float:
    n = sum(counts.values()) or 1
    return (counts["richtig"] + 0.5 * counts["vorn"] - weight_wrong * counts["falsch"]) / n


def run(cases: list[dict], match_fn) -> dict:
    counts = {"richtig": 0, "falsch": 0, "vorn": 0, "keine": 0}
    for c in cases:
        counts[judge(c["erwartet"], match_fn(c["anliegen"]))] += 1
    return counts


def fmt(counts: dict) -> str:
    n = sum(counts.values()) or 1
    return (f"richtig {counts['richtig']/n:6.1%}  vorn {counts['vorn']/n:6.1%}  falsch {counts['falsch']/n:6.1%}  "
            f"keine {counts['keine']/n:6.1%}  Ziel {score(counts):+.3f}")


def coordinate_descent(train: list[dict], test: list[dict], match_fn, *, rounds: int = 2, grid=None, log=print) -> dict:
    grid = grid or GRID
    start = current(grid)
    best = dict(start)
    with applied(best):
        base_train, base_test = run(train, match_fn), (run(test, match_fn) if test else None)
    log(f"Ausgang      Training: {fmt(base_train)}" + (f"\n             Kontrolle: {fmt(base_test)}" if base_test else ""))
    best_score = score(base_train)
    for rnd in range(1, rounds + 1):
        changed = False
        for name, (_, _, values) in grid.items():
            results = []
            for value in values:
                with applied({**best, name: value}):
                    counts = run(train, match_fn)
                results.append((score(counts), value, counts))
                log(f"  [{rnd}] {name} = {value!s:<44} {fmt(counts)}")
            top = max(results, key=lambda r: (r[0], r[1] == best[name]))
            if top[0] > best_score + 1e-9 and top[1] != best[name]:
                best[name], best_score, changed = top[1], top[0], True
                log(f"  -> {name} auf {top[1]!r} gesetzt")
        if not changed:
            break
    with applied(best):
        final_train, final_test = run(train, match_fn), (run(test, match_fn) if test else None)
    log("\nErgebnis")
    log(f"  Training   vorher: {fmt(base_train)}\n             nachher: {fmt(final_train)}")
    if base_test:
        log(f"  Kontrolle  vorher: {fmt(base_test)}\n             nachher: {fmt(final_test)}")
    return {"parameter": best, "ausgang": start, "training": {"vorher": base_train, "nachher": final_train},
            "kontrolle": {"vorher": base_test, "nachher": final_test} if base_test else None}


class CachedSearch:
    """Merkt sich Wort- und Vektortreffer je Anfrage (Vektortreffer hängen nur von Anfrage und Größe ab)."""

    def __init__(self):
        self.lexical, self.knn = {}, {}
        self._orig_lex, self._orig_knn = idx._lexical_hits, idx._knn_hits

    def install(self):
        def lex(client, index_name, query, size):
            key = (query, size, idx.QUERY_CLEAN, idx.FUZZY_BOOST, idx.FUZZINESS, tuple(idx.LEXICAL_FIELDS))
            if key not in self.lexical:
                self.lexical[key] = self._orig_lex(client, index_name, query, size)
            return self.lexical[key]

        def knn(client, index_name, query, size, model_name):
            key = (query, size, model_name)
            if key not in self.knn:
                self.knn[key] = self._orig_knn(client, index_name, query, size, model_name)
            return self.knn[key]
        idx._lexical_hits, idx._knn_hits = lex, knn
        client = idx.es_client
        memo = {}
        idx.es_client = lambda url: memo.setdefault(url, client(url))


def build(args, all_queries: list[str], log=print):
    """Liefert match(text) -> Rohergebnis des Matchers, mit Vorlauf (PVOG-Belege) und Zwischenspeichern."""
    from . import autorun as am
    from .catalog import AuthorityTaxonomy
    from .pvog_live import connect, evidence_by_key, make_enricher
    semantic = not args.no_semantic
    am.get_index_docs(semantic=semantic, force=False, no_start_docker=args.no_start_docker)
    CachedSearch().install()

    def raw_match(text, enrich):
        return mt.match_authority(text, backend="elasticsearch", es_url=am.ES_URL, es_index=am.INDEX, semantic=semantic,
                                  embedding_model=am.MODEL, enrich=enrich)

    if not args.no_fetch:
        taxonomy = AuthorityTaxonomy(am.DEFAULT_TAXONOMY)
        live = make_enricher(am.DB, taxonomy, regions=am.reference_regions(None), max_keys=PREFETCH_KEYS, trace=[])
        t0 = time.time()
        for i, text in enumerate(all_queries, 1):
            raw_match(text, live)
            if i % 100 == 0:
                log(f"  Vorlauf {i}/{len(all_queries)} ({time.time() - t0:.0f} s)")
        log(f"Vorlauf fertig: {len(all_queries)} Anfragen in {time.time() - t0:.0f} s")

    con = connect(am.DB)
    try:
        evidence = evidence_by_key(con)
    finally:
        con.close()

    def cached_enrich(rows):
        keys = []
        for doc, _ in rows:
            key = doc.get("leistungsschluessel")
            if isinstance(key, str) and key not in keys:
                keys.append(key)
            if len(keys) >= LIVE_TOP:
                break
        out = []
        for doc, s in rows:
            doc = dict(doc)
            items = evidence.get(doc.get("leistungsschluessel"), []) if doc.get("leistungsschluessel") in keys else []
            if items:
                doc["behoerdenkategorien"] = sorted({e["category"] for e in items})
                doc["mapping_status"] = "jzufi_live_belegt" if len(doc["behoerdenkategorien"]) == 1 else "jzufi_live_mehrdeutig"
                doc["zuordnung_belege"] = items
            out.append((doc, s))
        return out

    return lambda text: raw_match(text, cached_enrich)


FINDER_STAGES = ["Matcher allein", "+ Stichwörter", "+ Katalog (Matcher zuerst)", "+ Katalog zuerst"]


def finder_report(cases: list[dict], match_fn) -> dict | None:
    """Gesamtkette des behoerden-finder auf den Rohergebnissen; None, wenn behoerden_finder fehlt."""
    try:
        from behoerden_finder import faelle
        from behoerden_finder.matching import apply_topic_hints, interpret_match
    except ImportError:
        return None
    cat = faelle.ensure_installed()
    tally = {s: {"richtig": 0, "falsch": 0, "vorn": 0, "keine": 0} for s in FINDER_STAGES}
    wrong = []
    for c in cases:
        base = interpret_match(match_fn(c["anliegen"]))
        variants = [base, apply_topic_hints(base, c["anliegen"]),
                    apply_topic_hints(base, c["anliegen"], cat, faelle_vorrang=False),
                    apply_topic_hints(base, c["anliegen"], cat)]
        for stage, o in zip(FINDER_STAGES, variants):
            if o.category is not None:
                r = "richtig" if o.category in c["erwartet"] else "falsch"
            else:
                top = o.candidates[0]["id"] if o.candidates else None
                r = "vorn" if top in c["erwartet"] else "keine"
            tally[stage][r] += 1
            if r == "falsch" and stage == FINDER_STAGES[3]:
                wrong.append({"anliegen": c["anliegen"], "erwartet": c["erwartet"], "bekam": o.category, "quelle": o.source})
    return {"stufen": tally, "falsch_katalog_zuerst": wrong}


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(prog="authority_matcher.tune", description=__doc__.splitlines()[0])
    p.add_argument("--train", nargs="+", type=Path, default=[DATA / "eval_1000.jsonl"])
    p.add_argument("--test", nargs="*", type=Path, default=[DATA / "eval_holdout.jsonl", DATA / "eval_holdout2.jsonl"])
    p.add_argument("--no-semantic", action="store_true")
    p.add_argument("--no-start-docker", action="store_true")
    p.add_argument("--no-fetch", action="store_true", help="keine PVOG-Belege nachladen, nur den Zwischenspeicher nutzen")
    p.add_argument("--runden", type=int, default=2)
    p.add_argument("--nur", nargs="*", choices=sorted(GRID), help="nur diese Parameter variieren")
    p.add_argument("--finder", action="store_true", help="Gesamtkette des behoerden-finder vorher/nachher auswerten")
    p.add_argument("--ausgabe", type=Path, default=Path("tune_ergebnis.json"))
    args = p.parse_args(argv)
    train = load_cases(args.train)
    tests = {t.name: load_cases([t]) for t in args.test if t.exists()}
    test = [c for cs in tests.values() for c in cs]
    print(f"{len(train)} Trainings- und {len(test)} Kontrollanfragen", flush=True)
    grid = {k: v for k, v in GRID.items() if not args.nur or k in args.nur}
    if args.no_semantic:
        for k in ("MIN_SCORE_ES_SEMANTIC", "LEXICAL_WEIGHT"):
            grid.pop(k, None)
    else:
        grid.pop("MIN_SCORE_ES", None)
    log = lambda *a: print(*a, flush=True)
    match = build(args, [c["anliegen"] for c in train + test], log=log)
    result = coordinate_descent(train, test, match, rounds=args.runden, grid=grid, log=log)
    result["kontrolle_je_datei"] = {}
    for name, cases in tests.items():
        with applied(result["ausgang"]):
            before = run(cases, match)
        with applied(result["parameter"]):
            after = run(cases, match)
        result["kontrolle_je_datei"][name] = {"vorher": before, "nachher": after}
        log(f"  {name:22} vorher: {fmt(before)}\n  {'':22} nachher: {fmt(after)}")
    if args.finder:
        result["finder"] = {}
        for name, cases in {"training": train, **tests}.items():
            for label, params in (("vorher", result["ausgang"]), ("nachher", result["parameter"])):
                with applied(params):
                    rep = finder_report(cases, match)
                if rep is None:
                    log("behoerden_finder nicht installiert – Gesamtkette übersprungen")
                    break
                result["finder"].setdefault(name, {})[label] = rep
                log(f"\nGesamtkette {name} ({label})")
                for stage, counts in rep["stufen"].items():
                    log(f"  {stage:28} {fmt(counts)}")
    args.ausgabe.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    log(f"\nGespeichert: {args.ausgabe}\nEmpfohlene Änderungen (nur übernehmen, wenn die Kontrolle mitzieht):")
    for k, v in result["parameter"].items():
        if v != result["ausgang"][k]:
            log(f"  {k} = {v!r}   (bisher {result['ausgang'][k]!r})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
