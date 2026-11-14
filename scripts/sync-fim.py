#!/usr/bin/env python3
"""Lädt/ergänzt den FIM-Katalog, ohne anschließend den Suchindex aufzubauen."""
import argparse
import json
from pathlib import Path


def main():
    from authority_matcher.autorun import DB
    from authority_matcher.catalog import AuthorityTaxonomy
    from authority_matcher.cli import DEFAULT_TAXONOMY
    from authority_matcher.full_sync import SOURCES, sync_catalog

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--workers', type=int, default=4)
    parser.add_argument('--max-rps', type=float, default=2.0)
    parser.add_argument('--max-records', type=int, help='Begrenzter Entwicklungsimport, kein vollständiger Bestand')
    parser.add_argument('--retry', action='store_true', help='Fehlgeschlagene Detailabrufe sofort erneut versuchen')
    args = parser.parse_args()
    executable = Path(__file__).resolve().parents[1] / 'tools/fim-cli/node_modules/.bin/fim-portal'
    if not executable.is_file():
        parser.error('FIM-CLI fehlt. Zuerst bash scripts/setup.sh ausführen.')
    result = sync_catalog(DB, AuthorityTaxonomy(DEFAULT_TAXONOMY),
                          executable=str(executable), workers=args.workers, max_rps=args.max_rps,
                          max_records=args.max_records, force_retry=args.retry)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    complete = all(result['sources'].get(s, {}).get('finished') for s in SOURCES)
    return 0 if complete and not result['missing_details'] else 2


if __name__ == '__main__':
    raise SystemExit(main())
