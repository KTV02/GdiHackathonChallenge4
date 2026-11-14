# Behörden-Resolver: Kategorie und Koordinate

Der Resolver nimmt eine der **36 Behördenkategorien** sowie **lon/lat in WGS84** entgegen. Er liefert Quelltreffer mit Behördenname, vollständiger Geometrie, Originalattributen, Lizenz und Abdeckungsstatus. Er klassifiziert keinen Freitext; dafür sind [Matcher](../behoerden-matcher/README.md) und [Finder](../behoerden-finder/README.md) zuständig.

## Schnellstart

Für die komplette Anwendung bitte die [Haupt-README](../README.md) verwenden. Dieses Modul allein kann über Docker betrieben werden:

```bash
# Im Repository-Hauptordner:
docker compose -f behoerden-resolver/compose.yaml up -d db
docker compose -f behoerden-resolver/compose.yaml build cli
docker compose -f behoerden-resolver/compose.yaml run --rm cli load
docker compose -f behoerden-resolver/compose.yaml run --rm cli coverage
docker compose -f behoerden-resolver/compose.yaml run --rm cli resolve jobcenter --lon 8.466 --lat 49.488
```

`load` verwendet pro Quelle den jüngsten vollständigen Snapshot aus `data/snapshots`, ohne WFS-Abruf. Datenbank: `127.0.0.1:55432`, PostgreSQL 16/PostGIS 3.5. Auf Apple Silicon verwendet das offizielle PostGIS-Image AMD64-Emulation. Änderungen an Port/Passwort über `.env.example` abstimmen; Details unter [Betrieb](../docs/operations.md).

## Quellen und mitgelieferter Datenstand

Der bereinigte Checkout enthält **46 öffentliche Snapshots**: **6.838 Flächeneinträge in 16 Kategorien**, **1.105 Standortpunkte in 35 Kategorien** und **11.396 Verwaltungsgebietsreferenzen**. Zusätzlich können zwölf Kategorien dokumentierte gesetzliche Zuordnungen auf vorhandene Stadt-/Kreisverwaltungsflächen nutzen. Das sind Quellzeilen, keine garantierte Anzahl verschiedener Institutionen und keine vollständige bundesweite Abdeckung.

Maßgeblich sind [ABDECKUNG.md](docs/ABDECKUNG.md), [coverage-summary.json](data/coverage-summary.json) und [delivery-manifest.json](data/delivery-manifest.json). Diese Fassung wurde aus den tatsächlich mitgelieferten offenen Snapshots gemessen. Separat berechtigt importierte BZB-Daten sind nicht enthalten; lokale Datenbankberichte können deshalb andere Zahlen liefern.

Das [Quellenmanifest](src/behoerden_resolver/resources/sources.json) enthält Endpunkte, Layer, Filter, Namensregeln, CRS, Lizenzen und Raumumfang. [verification.json](src/behoerden_resolver/resources/verification.json) dokumentiert historische Live-Prüfungen mit Prüfsummen. Jeder Snapshot enthält ein eigenes Manifest und normalisierte Datensätze. Historische Rohantworten sind nicht vollständig im Repository enthalten.

## Abfrageverhalten

1. `--backend auto` fragt passende Fach-WFS für den Ort ab und nutzt lokale Ergänzungen. `--backend local` verwendet importierte PostGIS-Flächen. `--backend wfs` fordert Live-Flächen an; vor einem Standortersatz werden dennoch lokale Ergänzungen geprüft.
2. Der Punkt wird gegen das vollständige Polygon geprüft, einschließlich Löchern und Grenzen. Mehrere passende Quelltreffer bleiben erhalten. Ein allgemeiner Gebietstreffer entscheidet nicht automatisch persönliche, fachliche oder verfahrensrechtliche Sonderzuständigkeiten.
3. Dokumentierte gesetzliche Aufgaben-Zuordnungen aus `category_mappings.json` können vorhandene benannte Verwaltungsflächen nutzen. Die Antwort nennt `match_type=statutory_category_mapping`, Basisbehörde, Regel und Belegstufe. `--no-category-mapping` schaltet dies ab.
4. Erst ohne Flächentreffer kann der lokale Standortindex nahe Kontakte anbieten. `jurisdiction_verified=false`, Distanz und Warntext kennzeichnen diese Näheergänzung. `--no-point-fallback` deaktiviert sie.

Ein WFS-Ausfall wird als Fehler gemeldet und nicht als vermeintlich belegte Abdeckungslücke behandelt. `non_spatial` und `context_required` behalten ihre Bedeutung auch bei nahen Kontaktpunkten. Standardradien stehen in [fallback.json](src/behoerden_resolver/resources/fallback.json); `--radius-km` erlaubt größer 0 bis 300 km, `--point-limit` 1 bis 10 Treffer (Standard 3).

```bash
docker compose -f behoerden-resolver/compose.yaml run --rm cli resolve finanzamt --lon 11.4258 --lat 48.7665 --backend local
docker compose -f behoerden-resolver/compose.yaml run --rm cli resolve gesundheitsamt --lon 11.589 --lat 50.927 --radius-km 40
docker compose -f behoerden-resolver/compose.yaml run --rm cli resolve krankenkasse --lon 8.27 --lat 50.0
```

Antworten sind JSON; Fortschritt/Fehler gehen nach stderr. Geometrien werden immer vollständig geliefert. `candidate`/`ambiguous` sind territoriale Kandidaten, `nearby_candidate(s)` Standortvorschläge. `no_coverage_at_point` und `no_local_data` bedeuten fehlende integrierte Daten, keine tatsächliche Nichtzuständigkeit.

## Datenmodell und Verarbeitung

| Tabelle im Schema `behoerden` | Inhalt |
| --- | --- |
| `authorities` und 36 Kategoriepartitionen | Benannte Behördenflächen, Quelle und Originalattribute |
| `boundaries` | Reine Verwaltungsgebietsreferenzen |
| `locations` | Echte Standortpunkte mit Entfernungsindex |
| `categories`, `sources`, `import_runs` | Katalog, Quellen/Lizenzen und nachvollziehbare Importläufe |

GiST-Indizes und `ST_Covers` unterstützen die räumliche Suche. ETL prüft Paging, IDs, Geometrien und Vollständigkeit. Fehlerhafte Ersatzimporte werden zurückgerollt. GEMFA wird nur bei eindeutigen ganzen Gemeindezuordnungen mit VG25 verbunden; die Finanzamtsgeometrien können dadurch belegte Teilmengen sein. Zuständigkeitsflächen werden nicht aus bloßen Ortsnamen oder Büroadressen erfunden.

Detaillierter Ablauf: [WFS-Verarbeitung](../docs/WFS_Datenverarbeitung.md). Quellenstrategie/Ausschlüsse: [Recherche](docs/RECHERCHE.md) und [Erweiterungen](docs/ERWEITERUNG_QUELLEN.md).

## Quellen aktualisieren und erweitern

```bash
docker compose -f behoerden-resolver/compose.yaml run --rm cli plan jobcenter
docker compose -f behoerden-resolver/compose.yaml run --rm cli verify --source bzb_jobcenter
docker compose -f behoerden-resolver/compose.yaml run --rm cli sync --source bzb_jobcenter
.venv/bin/python behoerden-resolver/scripts/measure_snapshots.py
```

Neue Quellen brauchen eine fachlich belegte Behördenbezeichnung/-zuordnung, nachvollziehbare Geometrie und geeignete Nutzungsrechte. Der Snapshot-Bericht aktualisiert Dokumentation und Lieferinventar; `scripts/report.py` misst alternativ die laufende Datenbank. Nicht direkt in die Markdown-Datei umleiten. Originalaufgaben/Referenzmaterial bleiben außerhalb des Laufzeitpfads unter `../docs/reference/`.

Der optionale eingeschränkte BZB-Dump für Staatsanwaltschaften/Hauptzollämter kann mit `zsh scripts/import-bzb.command /absoluter/pfad/zustandigkeit.backup` aus dem Repository-Hauptordner importiert werden. Dump und daraus erzeugte Snapshots sind von Git ausgeschlossen. Die offene Auslieferung funktioniert ohne diese Daten, hat dort jedoch geringere Abdeckung.

## Python und Tests

```python
from behoerden_resolver.resolver import resolve
result = resolve('jobcenter', lon=8.466, lat=49.488, backend='local')
```

Für eine isolierte Python-Installation benötigt dieses Modul Python 3.12+, `requirements.lock` und das lokale Paket. `pip install -e '.[test]'` ergänzt pytest; `pip install -e '.[reports]'` ergänzt NumPy für den Snapshot-Bericht. Für die komplette Anwendung genügt der gemeinsame Setup im Repository-Hauptordner.

`bash scripts/test.sh` prüft alle Module. Resolver-Integrationstests verwenden ausschließlich eine separate Datenbank mit Namen auf `_test`; ohne `TEST_DATABASE_URL` werden sie übersprungen. Der gemeinsame [CI-Workflow](../.github/workflows/tests.yml) konfiguriert dafür PostGIS. Aktuelle Ergebnisse: [Validierung](../docs/testing-and-demo.md).

## Lizenzen

Die [MIT-Lizenz](LICENSE) gilt für den Resolver-Code. Quellendaten haben eigene Nutzungsrechte; Namensnennungen und Quellenhinweise aus dem Manifest müssen mitgeführt werden. OSM-Daten stehen unter ODbL 1.0 und sind Gemeinschaftsdaten. Öffentlich erreichbar bedeutet nicht automatisch frei weitergebbar. Eingeschränkte BZB-Daten dürfen nicht mit dem offenen Repository ausgeliefert werden.
