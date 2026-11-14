import argparse
import json
import sys

from .catalog import export_snapshot
from .config import load_config
from .coverage import coverage_report
from .errors import MatcherError
from .resolver import AuthorityMatcher
from .text import normalize_query


def _parser():
    parser = argparse.ArgumentParser(description="Anliegen → Verwaltungsleistung → Behördenkategorie (ohne GeoServer-/Datenbankabfrage).")
    sub = parser.add_subparsers(dest="command", required=True)
    match = sub.add_parser("match", help="Ein Anliegen einer Behördenkategorie zuordnen")
    match.add_argument("query", nargs="?", help="Natürlich formuliertes Anliegen")
    match.add_argument("--config", help="Pfad zur JSON-Konfiguration")
    match.add_argument("--ars", help="Gemeinde-ARS als Suchreferenz, keine Standortauflösung")
    match.add_argument("--service-id", help="Eine angezeigte Leistungs-ID ausdrücklich auswählen")
    match.add_argument("--offline", action="store_true", help="Begrenzten offiziellen Offline-Auszug verwenden")
    output = match.add_mutually_exclusive_group()
    output.add_argument("--json", action="store_true", help="Strukturiertes Ergebnis ausgeben")
    output.add_argument("--keyword-only", action="store_true", help="Nur Kategoriecode auf stdout; Hinweise auf stderr")
    cats = sub.add_parser("categories", help="Konfigurierte Kategorien mit Belegen anzeigen")
    cats.add_argument("--config")
    coverage = sub.add_parser("coverage", help="Fehlende Kategorienzuordnungen im offiziellen Offline-Auszug anzeigen")
    coverage.add_argument("--config")
    coverage.add_argument("--json", action="store_true")
    snapshot = sub.add_parser("snapshot", help="Öffentliche PVOG-Treffer mit Belegen für Offline-Nutzung exportieren")
    snapshot.add_argument("query")
    snapshot.add_argument("--config")
    snapshot.add_argument("--ars")
    snapshot.add_argument("--service-id")
    snapshot.add_argument("--output", required=True, help="Neue Ausgabedatei; vorhandene Dateien werden nicht überschrieben")
    return parser


def render(result, stream=None):
    stream = stream or sys.stdout
    if result["category"]:
        print(f"Behördenkategorie: {result['category']['label']}", file=stream)
        print(f"Keyword: {result['category']['code']}", file=stream)
    else:
        print(f"Keine eindeutige Kategorie: {result['message']} [{result['status']}]", file=stream)
    if result.get("service"):
        print(f"Leistung: {result['service']['name']} ({result['service']['id']})", file=stream)
        if result["service"].get("matched_field") == "synonyms":
            print(f"Offizieller Suchbegriff: {result['service']['matched_text']}", file=stream)
    elif result.get("candidates"):
        for index, item in enumerate(result["candidates"], 1):
            print(f"  {index}. {item['name']} — {item['id']}", file=stream)
    if result.get("related_candidates"):
        print(f"Verwandte Treffer zu „{result['related_query']}“ (Anliegen nicht bestätigt):", file=stream)
        for item in result["related_candidates"]:
            print(f"  {item['name']} — {item['id']}", file=stream)
        print("Zur weiteren Prüfung eine passende offizielle Leistungsbezeichnung als neue Anfrage eingeben.", file=stream)
    if len(result["categories"]) > 1:
        for category in result["categories"]:
            print(f"  Kategorie: {category['label']} ({category['code']})", file=stream)
    if result.get("reference_ars"):
        region = result.get("reference_region_name") or result["reference_ars"]
        print(f"Suchreferenz: {region} ({result['reference_ars']}); keine Standortprüfung.", file=stream)
    for warning in result["warnings"]:
        print(f"Hinweis: {warning}", file=stream)
    if result.get("data_mode") == "snapshot":
        print(f"Datenstand des Offline-Auszuges: {result['snapshot_retrieved_at']}", file=stream)


def main(argv=None):
    argv = list(sys.argv[1:] if argv is None else argv)
    if not argv or argv[0] not in {"match", "snapshot", "categories", "coverage", "--help", "-h"}:
        argv.insert(0, "match")
    args = _parser().parse_args(argv)
    try:
        config = load_config(args.config)
        if args.command == "coverage":
            report = coverage_report(config)
            if args.json:
                print(json.dumps(report, ensure_ascii=False, indent=2))
            else:
                print(f"Begrenzter Offline-Auszug: {report['service_count']} Leistungen; Referenz-ARS {report['reference_ars']}.")
                print(f"Konfigurierte Kategorien: {report['configured_categories']}")
                print(f"Alle zuständigen Organisationen kategorisiert: {report['services_with_all_organisations_categorised']} Leistungen.")
                print(f"Eine Kategorie ohne zusätzliche Kriterien: {report['services_with_unique_category_and_no_criteria']} Leistungen.")
                print(f"Noch nicht kategorisierte Organisationen: {report['unmapped_organisation_count']}")
                for item in report["unmapped_organisations"]:
                    print(f"  {item['id']} — {item['name']} ({len(item['service_ids'])} Leistungen)")
                print("Die Übersicht misst die Zuordnung bekannter Leistungen, nicht die bundesweite Suchabdeckung.")
            return 0
        if args.command == "snapshot":
            query = normalize_query(args.query)
            ars = args.ars or config["reference_ars"]
            import re
            if not query or not re.fullmatch(r"[0-9]{12}", ars):
                raise MatcherError("invalid_query", "Suchbegriff und zwölfstelligen ARS angeben.")
            count = export_snapshot(config, query, ars, args.output, args.service_id)
            print(f"{count} offizielle Leistung(en) mit Belegen exportiert: {args.output}")
            return 0
        matcher = AuthorityMatcher(config=config)
        if args.command == "categories":
            print(json.dumps(matcher.registry.categories, ensure_ascii=False, indent=2))
            return 0
        query = args.query
        if query is None:
            if not sys.stdin.isatty():
                raise MatcherError("invalid_query", "Anliegen als Argument übergeben oder interaktives Terminal verwenden.")
            query = input("Dein Anliegen: ")
        result = matcher.match(query, ars=args.ars, service_id=args.service_id, offline=args.offline)
        if result["status"] == "ambiguous_service" and sys.stdin.isatty() and not args.json and not args.keyword_only:
            render(result)
            choice = input("Leistung auswählen (Nummer, Enter zum Abbrechen): ").strip()
            if choice.isascii() and choice.isdigit() and 1 <= int(choice) <= len(result["candidates"]):
                result = matcher.match(query, ars=args.ars, service_id=result["candidates"][int(choice)-1]["id"], offline=args.offline)
        if args.json:
            print(json.dumps(result, ensure_ascii=False, indent=2))
        elif args.keyword_only:
            if result["category"]:
                print(result["category"]["code"])
            render(result, stream=sys.stderr)
        else:
            render(result)
        return 0 if result["status"] == "matched" else (2 if result["status"] in {
            "matched_snapshot", "ambiguous_service", "ambiguous_category", "category_unmapped", "no_match",
            "no_association", "no_competent_role", "criteria_required"} else 1)
    except MatcherError as error:
        if getattr(args, "json", False):
            print(json.dumps({"status": error.code, "message": error.message, "category": None}, ensure_ascii=False))
        else:
            print(f"Fehler: {error.message} [{error.code}]", file=sys.stderr)
        return 1
    except (KeyboardInterrupt, EOFError):
        return 130


if __name__ == "__main__":
    sys.exit(main())
