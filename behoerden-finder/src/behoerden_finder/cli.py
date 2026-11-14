"""Kommandozeile: Anliegen + Adresse -> zuständige Behörde.

Beispiele:
    behoerden-finder --anliegen "Meine Oma hat mir 10.000 EUR geschenkt" --adresse "Am Markt 1, Mainz"
    behoerden-finder                       # interaktiv
    behoerden-finder --kategorie finanzamt --adresse "Marienplatz 1, München"   # ohne Matcher
    behoerden-finder --kategorie jobcenter --lat 49.488 --lon 8.466 --json      # ohne Matcher und Geocoder

Ausgabe auf stdout, Fortschritt und Fehler auf stderr.
Exitcode: 0 Behörde bzw. Anlaufstelle gefunden, 2 kein Ergebnis, 1 Eingabe- oder technischer Fehler.
"""
from __future__ import annotations

import argparse
import json
import sys

from .geocode import Geocoder
from .matching import AuthorityMatcher
from .pipeline import FOUND, Finder, ResolveOptions
from .render import render_text


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(prog="behoerden-finder", description=__doc__.split("\n\n")[0])
    p.add_argument("--anliegen", help="Freitext, z. B. 'Ich habe einen Hund gekauft'")
    p.add_argument("--adresse", help="Adresse, z. B. 'Musterstraße 12, 55116 Mainz'")
    p.add_argument("--kategorie", help="Behördenkategorie direkt vorgeben und den Matcher überspringen")
    p.add_argument("--lat", type=float, help="Breite (WGS84) statt Adresse; nur zusammen mit --lon")
    p.add_argument("--lon", type=float, help="Länge (WGS84) statt Adresse; nur zusammen mit --lat")
    p.add_argument("--json", action="store_true", help="Ergebnis als JSON (ohne Flächengeometrie)")
    p.add_argument("--mit-geometrie", action="store_true", help="Flächengeometrien im Ergebnis behalten")
    p.add_argument("--details", "--debug", dest="details", action="store_true",
                   help="Matcher-Rohdaten (Leistungen, Belege) in --json aufnehmen")
    r = p.add_argument_group("Resolver")
    r.add_argument("--backend", choices=["auto", "local", "wfs"], default="auto",
                   help="auto: Fach-WFS + lokale Ergänzungen; local: nur PostGIS (offline); wfs: nur live")
    r.add_argument("--no-point-fallback", action="store_true", help="Nur Zuständigkeitsflächen, keine Standortvorschläge")
    r.add_argument("--radius-km", type=float, help="Maximale Entfernung für Standortvorschläge")
    r.add_argument("--point-limit", type=int, default=3, help="Höchstens so viele nahe Standorte (Standard 3)")
    r.add_argument("--dsn", help="PostgreSQL-Verbindung, sonst DATABASE_URL bzw. lokaler Standard (Port 55432)")
    m = p.add_argument_group("Matcher")
    m.add_argument("--no-semantic", action="store_true",
                   help="BM25 statt BM25+Embeddings. Achtung: Wechsel gegenüber dem letzten Indexaufbau erzwingt Reindexierung")
    m.add_argument("--no-live", action="store_true", help="Keine PVOG-Abfrage, nur Cache")
    m.add_argument("--reference-ars", help="Kommaseparierte Referenzgebiete für die PVOG-Datenbeschaffung")
    m.add_argument("--live-top", type=int, default=3, help="Anzahl LeiKa-Schlüssel pro Anfrage (1-10)")
    m.add_argument("--no-start-docker", action="store_true", help="Elasticsearch nicht selbst starten")
    m.add_argument("--no-themenhinweise", action="store_true",
                   help="Keine Behördenart aus Stichwörtern ableiten, wenn die amtlichen Daten keine liefern")
    m.add_argument("--matcher-vorrang", action="store_true",
                   help="Amtliche Matcher-Ergebnisse haben Vorrang vor dem Fall-Katalog (Standard: Fall-Katalog zuerst)")
    m.add_argument("--bei-mehrdeutig", choices=["stop", "beste"], default="stop",
                   help="Bei mehrdeutiger Kategorie anhalten (Standard) oder die bestplatzierte nehmen")
    return p


def _prompt(label: str, default: str | None = None) -> str | None:
    suffix = f" [Enter = {default}]" if default else ""
    try:
        value = input(f"{label}{suffix}> ").strip()
    except (EOFError, KeyboardInterrupt):
        print(file=sys.stderr)
        return None
    return value or default


def _make_finder(args) -> Finder:
    matcher = None
    if not args.kategorie:
        matcher = AuthorityMatcher(semantic=not args.no_semantic, live=not args.no_live,
                                   reference_ars=args.reference_ars, live_top=args.live_top,
                                   start_docker=not args.no_start_docker,
                                   accept_ambiguous=args.bei_mehrdeutig == "beste",
                                   topic_hints=not args.no_themenhinweise,
                                   faelle_vorrang=not args.matcher_vorrang)
    options = ResolveOptions(backend=args.backend, point_fallback=not args.no_point_fallback,
                             radius_km=args.radius_km, point_limit=args.point_limit, dsn=args.dsn)
    return Finder(matcher=matcher, geocoder=None if (args.lat is not None) else Geocoder(), options=options,
                  include_geometry=args.mit_geometrie, include_details=args.details)


def _run_one(finder: Finder, args, anliegen, adresse) -> int:
    coords = (args.lat, args.lon) if args.lat is not None else None
    try:
        result = finder.find(anliegen, adresse, kategorie=args.kategorie, coords=coords)
    except (ValueError, RuntimeError, FileNotFoundError) as exc:
        print(f"Fehler: {exc}", file=sys.stderr)
        return 1
    except Exception as exc:  # z. B. Elasticsearch-Verbindungsfehler: knapp melden statt Traceback
        print(f"Fehler: {type(exc).__name__}: {exc}", file=sys.stderr)
        return 1
    print(json.dumps(result, ensure_ascii=False, indent=2, default=str) if args.json else render_text(result))
    if result["status"] == "resolver_fehler":
        return 1
    return 0 if result["status"] in FOUND else 2


def main(argv=None, *, finder_factory=None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    if (args.lat is None) != (args.lon is None):
        parser.error("--lat und --lon nur zusammen angeben")
    try:
        finder = (finder_factory or _make_finder)(args)
    except (ValueError, RuntimeError) as exc:
        print(f"Fehler: {exc}", file=sys.stderr)
        return 1

    have_location = args.adresse or args.lat is not None
    if args.anliegen or args.kategorie or have_location:
        anliegen, adresse = args.anliegen, args.adresse
        if not args.kategorie and not anliegen:
            anliegen = _prompt("Anliegen") if sys.stdin.isatty() else None
        if not have_location:
            adresse = _prompt("Adresse") if sys.stdin.isatty() else None
        return _run_one(finder, args, anliegen, adresse)

    # Interaktiv: Anliegen und Adresse abfragen, Adresse darf wiederverwendet werden.
    print("Behörden-Finder · Anliegen und Adresse eingeben, 'exit' beendet", file=sys.stderr)
    last_address, code = None, 0
    while True:
        anliegen = _prompt("Anliegen")
        if anliegen is None or anliegen.casefold() in ("exit", "quit", "ende"):
            return code
        adresse = _prompt("Adresse", last_address)
        if adresse is None or adresse.casefold() in ("exit", "quit", "ende"):
            return code
        last_address = adresse
        code = _run_one(finder, args, anliegen, adresse)
        print()
