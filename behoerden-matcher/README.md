# Behörden-Matcher: FIM, Elasticsearch und PVOG

Dieses Paket ordnet einem Freitextanliegen eine der **36 projektspezifischen Behördenkategorien** zu. Es erfragt keinen Nutzerstandort und bestimmt keine konkrete örtliche Behörde. Webapp und vollständige Suche befinden sich im Schwesterpaket [Finder](../behoerden-finder/README.md).

## Einrichtung und Daten

Die [Haupt-README](../README.md) installiert dieses Paket gemeinsam mit Finder und Resolver in `.venv/`. Optional sind `elastic` für Elasticsearch und `semantic` für Embeddings; der Standard-Setup installiert beide. Das FIM-Importwerkzeug liegt nach `npm ci` unter `tools/fim-cli/node_modules/`, nicht im Git-Bestand.

Der Katalog liegt standardmäßig unter `~/.local/share/authority-matcher/fim_catalog.sqlite3`, umstellbar mit `AUTHORITY_MATCHER_HOME`. Eine vorhandene Datenbank wird weiterverwendet. Auf einem neuen Rechner muss sie aus einer Sicherung bereitgestellt oder importiert werden:

```bash
# Im Repository-Hauptordner:
.venv/bin/python scripts/sync-fim.py
.venv/bin/authority-matcher --status
.venv/bin/authority-matcher 'Ich möchte meinen Hund anmelden'
```

Der FIM-Import ist fortsetzbar; `--retry` versucht fehlgeschlagene Details sofort erneut. `--workers` und `--max-rps` steuern die Abrufe. Der Import baut noch keinen Elasticsearch-Index und lädt keine vollständige bundesweite Kategoriezuordnung.

Elasticsearch wird bei Bedarf mit der paketlokalen `compose.yaml` gestartet. Der erste Suchaufruf baut den Index auf; optional werden E5-Embeddings berechnet und das Modell `intfloat/multilingual-e5-small` heruntergeladen. Spätere Aufrufe verwenden den vorhandenen Index. Ein Wechsel zwischen semantischem Modus und `--no-semantic` löst einen erneuten Aufbau aus.

## Bedienung

```bash
.venv/bin/authority-matcher
.venv/bin/authority-matcher --json 'Ich brauche einen Personalausweis'
.venv/bin/authority-matcher --json --details 'Ich brauche einen Erbschein'
.venv/bin/authority-matcher --no-semantic 'Hund anmelden'
.venv/bin/authority-matcher --no-live 'Wohnsitz abmelden'
.venv/bin/authority-matcher --reindex 'Personalausweis beantragen'
```

Die kompakte JSON-Ausgabe enthält genau diese beiden Felder:

```json
{"behoerdenkategorie": "buergeramt", "score": 0.03814}
```

Der Score ist illustrativ und bezeichnet einen aggregierten Ranking-Wert, **keine Prozentwahrscheinlichkeit**. Ohne eindeutige Kategorie sind beide Werte `null`. `--details`/`--debug` zeigt zusätzlich Leistungen, Organisationen, Belege, Kandidaten und Diagnosen.

| Option | Zweck |
| --- | --- |
| `--no-live` | Nur gespeicherte PVOG-Belege verwenden |
| `--reference-ars` | Kommaseparierte Referenzregionen für den PVOG-Abgleich |
| `--live-top` | Maximale Zahl unterschiedlicher LeiKa-Schlüssel je Anfrage, Standard 3 |
| `--no-start-docker` | Elasticsearch bereits extern bereitstellen |
| `--no-semantic` | BM25 ohne Embeddings |
| `--status`, `--reindex` | Bestand prüfen bzw. Index neu aufbauen |

Standardreferenzen sind Berlin `110000000000`, Hamburg `020000000000` und Mainz `073150000000`. Das sind Abrufregionen, keine aus der Bürgeradresse ermittelten Zuständigkeiten. Standardadresse von Elasticsearch ist `http://127.0.0.1:9200`; `ELASTICSEARCH_URL` und `AUTHORITY_MATCHER_INDEX` überschreiben Dienst bzw. Indexnamen.

## Belegkette

1. FIM-Leistungssteckbriefe und Stammtexte werden normalisiert in SQLite gespeichert und lokal indexiert.
2. BM25 und optional E5 ranken die Leistungen. Für gültige 14-stellige LeiKa-Schlüssel sucht PVOG regionale Leistungsbeschreibungs-IDs.
3. JZuFi-Leistungsdaten/-Relationen und Organisationstitel verbinden die Leistung mit amtlich benannten Organisationen.
4. Explizite Namens-Aliasse aus `data/categories.json` normieren Organisationen auf die 36 Kategorien. Rollen `01`/`02` gelten als Zuständigkeitsbeleg, `03` allein nicht.
5. Eindeutige Belege werden aggregiert; fehlende oder widersprüchliche Zuordnungen bleiben sichtbar.

PVOG erhält die LeiKa-/Leistungs-IDs und Referenz-ARS, nicht den ursprünglichen Freitext. Tabellen `jzufi_services`, `jzufi_orgs`, `jzufi_links` und `pvog_live_attempts` speichern Beziehungen und Abrufstatus. Fehlversuche werden nach ungefähr fünf Minuten, leere Resultate nach 24 Stunden und erfolgreiche Abfragen nach sieben Tagen erneut versucht.

Die maßgebliche Zuordnung ist konservativ: Herausgeber oder Freigabestelle eines FIM-Textes allein sind keine zuständige Behörde. Der Finder ergänzt separat einen redaktionellen Fallkatalog und Themenhinweise; diese gehören nicht zum kompakten Matcher allein.

## Diagnose und weitere Werkzeuge

```bash
.venv/bin/authority-matcher jzufi-fetch --leika 99008001000000 --ars 110000000000
.venv/bin/authority-matcher jzufi-import /pfad/zu/jzufi.jsonl
.venv/bin/behoerden-match --help
```

`behoerden-match` bietet die kleineren Import-/Normalisierungs-/Index-/Suchwerkzeuge. Der Standard-Datensatz seiner lokalen `query`-Funktion ist ausdrücklich eine Demo; er ersetzt nicht den vollständigen FIM/PVOG-Betrieb.

`autorun_legacy.py` bleibt erhalten, weil der aktuelle Starter dessen Elasticsearch-Startfunktion verwendet. Sein älterer Gesamtworkflow ist kein zusätzlicher empfohlener Startweg. Die früheren Hotfix-ZIPs und Installations-/Update-Skripte sind durch die gemeinsame Einrichtung ersetzt.

## Tests und Tuning

```bash
bash scripts/test.sh
zsh scripts/tuning.command
```

Die Matcher-Tests prüfen unter anderem Normalisierung, FIM-Synchronisierung, Ranking-Ausgabe, JZuFi-Beziehungen, Live-API-Verträge und Cacheverhalten mit synthetischen Antworten. Sie belegen keine aktuelle Erreichbarkeit externer Dienste. Tuning verwendet redaktionelle Erwartungen aus `data/eval_1000.jsonl` und getrennte Kontrollmengen `eval_holdout*.jsonl`. Der Bericht unter `../docs/evaluation/` ist ein historischer Messstand; Laufzeitwerte werden daraus nicht automatisch geladen.

Grenzen: Nicht alle FIM-Einträge haben geeignete LeiKa-Schlüssel, PVOG-Beziehungen können fehlen, regionale Organisationen unterscheiden sich, und Referenzregionen bilden keinen vollständigen bundesweiten Datenexport ab. Eine leere Kategorie ist keine Aussage, dass es tatsächlich keine zuständige Behörde gibt. Weitere Hinweise: [Datenquellen](../docs/data-sources.md) und [Validierung](../docs/testing-and-demo.md).
