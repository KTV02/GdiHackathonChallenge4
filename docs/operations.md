# Einrichtung, Betrieb und Wartung

Die [Haupt-README](../README.md) enthält die vollständige Ersteinrichtung. Dieses Dokument beschreibt den Betrieb nach der Installation. Befehle beginnen im Repository-Hauptordner.

## Konfiguration

| Variable/Option | Wirkung |
| --- | --- |
| `DATABASE_URL` | PostGIS-Verbindung für Python; Standard lokal auf Port 55432 |
| `ELASTICSEARCH_URL` | Matcher-Suchdienst; Standard `http://127.0.0.1:9200` |
| `AUTHORITY_MATCHER_HOME` | FIM-SQLite, PVOG-Cache und Indexmanifeste; Standard `~/.local/share/authority-matcher` |
| `AUTHORITY_MATCHER_INDEX` | Elasticsearch-Indexname; Standard `fim-jzufi-v3` |
| `RESOLVER_DATA_DIR` | Snapshotverzeichnis der Resolver-CLI; Standard relativ zum Arbeitsordner `data/snapshots`, im Container `/data/snapshots` |
| `NOMINATIM_URL`, `PHOTON_URL` | Alternative kompatible Adresssuch-Endpunkte |
| `GEOCODER_CONTACT` | Kontakt im User-Agent der Adresssuche |
| `GOOGLE_MAPS_API_KEY` | Optionaler Google-Places-Schlüssel; alternativ `~/.config/behoerdenfinder/google_maps_api_key` |
| `--zusatzinfos auto\|osm\|google\|aus` | Webapp: OSM/Places-Zusatzclient; `auto` wählt Google mit Schlüssel, sonst OSM |

`.env` wird von Python nicht automatisch geladen. Eine lokale Konfiguration kann so exportiert werden:

```bash
cp .env.example .env
# .env bei Bedarf bearbeiten
set -a
source .env
set +a
.venv/bin/behoerden-finder-web
```

PostGIS-Compose liest seine Einstellungen aus `behoerden-resolver/.env` bzw. der aufrufenden Shell. Die dortige `.env.example` zeigt `POSTGRES_PASSWORD`, `POSTGRES_PORT` und `DATABASE_URL`. Ändert sich der Port oder das Passwort, muss die Python-Verbindung dazu passen. Eine geänderte Compose-Variable ändert nicht das Passwort eines bereits initialisierten PostgreSQL-Volumes.

## Starten und Stoppen

```bash
docker compose -f behoerden-resolver/compose.yaml up -d db
docker compose -f behoerden-matcher/compose.yaml up -d
.venv/bin/behoerden-finder-web

# In der Webserver-Konsole Ctrl+C, dann bei Bedarf:
docker compose -f behoerden-resolver/compose.yaml stop
docker compose -f behoerden-matcher/compose.yaml stop
```

`stop` und `down` ohne `-v` bewahren die Daten-Volumes. `down -v` löscht sie. Beide Compose-Dateien betreiben lokale Entwicklungsdienste; Elasticsearch läuft ohne Authentifizierung auf Loopback. Der Standard-Webserver bindet an Loopback und besitzt keine Benutzerverwaltung. Eine öffentliche Bereitstellung erfordert eine gesonderte Betriebsarchitektur.

## Daten sichern und wiederherstellen

| Bestand | Sicherung/Wiederaufbau |
| --- | --- |
| FIM/PVOG und eigene Fallkatalogzeilen | Gesamtes `AUTHORITY_MATCHER_HOME` sichern; SQLite dafür bei gestopptem Matcher/Webserver kopieren oder SQLite-Backup verwenden |
| Elasticsearch | Aus dem FIM-Katalog neu indexierbar; bei Wiederherstellung entweder passendes Volume behalten oder mit `authority-matcher --reindex` neu aufbauen |
| Offene Resolver-Daten | Versionierte Snapshots; `cli load` lädt sie erneut in PostGIS |
| Zusätzliche lokale Resolver-Daten | PostgreSQL-Dump und/oder zugehörige vollständige Snapshots separat sichern |
| Zugangsbeschränkte BZB-Daten | Separat sichern, Berechtigungen erhalten; gehören nicht in Git |
| Konfiguration/Schlüssel | Lokale `.env` und Schlüsseldateien getrennt vom Repository sichern |

Ein Datenbankdump kann ohne Anwendungszugriffe erstellt werden:

```bash
mkdir -p logs/backup
docker compose -f behoerden-resolver/compose.yaml exec -T db \
  pg_dump -U resolver -d authorities -Fc > logs/backup/authorities.backup
```

Die Snapshots ersetzen keine Sicherung zusätzlicher lokaler Importquellen. Die Standard-FIM-Datenbank liegt außerhalb des Projektordners und wird beim Entfernen von Python-Umgebungen nicht gelöscht.

## Quellen und Berichte aktualisieren

```bash
# FIM-Synchronisierung fortsetzen; fehlerhafte Details sofort erneut versuchen
.venv/bin/python scripts/sync-fim.py --retry

# Nach geändertem FIM-Bestand Index gezielt neu aufbauen
.venv/bin/authority-matcher --reindex 'Personalausweis beantragen'

# Eine bestimmte Resolver-Quelle prüfen und aktualisieren
docker compose -f behoerden-resolver/compose.yaml run --rm cli verify --source bzb_jobcenter
docker compose -f behoerden-resolver/compose.yaml run --rm cli sync --source bzb_jobcenter

# Abdeckung nur aus vorhandenen Snapshots neu messen, ohne PostGIS
.venv/bin/python behoerden-resolver/scripts/measure_snapshots.py

# Alternativ tatsächlichen lokalen DB-Bestand messen
.venv/bin/python behoerden-resolver/scripts/report.py
```

Beide Berichtsskripte schreiben `docs/ABDECKUNG.md` im Resolver-Paket sowie `coverage-summary.json` und `delivery-manifest.json` selbst. **Die Ausgabe nicht auf `ABDECKUNG.md` umleiten.** Der Snapshot-Bericht misst nur die vorhandenen Dateien; ein lokaler Datenbankbericht kann zusätzlich berechtigt importierte Bestände enthalten. Vor einem Commit den gemessenen Umfang prüfen. Für den Snapshot-Bericht wird NumPy benötigt; es wird mit den Matcher-Abhängigkeiten installiert.

Neue Quellen zunächst fachlich und lizenzrechtlich prüfen. Ein vollständiger Download mit Manifest und Prüfsumme ist Voraussetzung für den Import. Rechtliche Kategoriezuordnungen müssen redaktionell neu geprüft werden; ein neuer Geometriedownload aktualisiert keine Rechtsgrundlage.

## Optionaler eingeschränkter BZB-Import und Tuning

```bash
zsh scripts/import-bzb.command /absoluter/pfad/zustandigkeit.backup
zsh scripts/tuning.command
```

Der BZB-Import setzt einen bereits berechtigt bezogenen Dump voraus und lädt ausschließlich die konfigurierten Staatsanwaltschafts- und Hauptzollamtsquellen. Er sucht nicht selbst nach Dateien auf dem Rechner. Dump, daraus erzeugte Snapshots und Importprotokoll bleiben ignoriert. Anschließend kann `scripts/report.py` den erweiterten lokalen Bestand dokumentieren.

Tuning arbeitet mit redaktionellen Erwartungsdaten aus dem Matcher-Paket und kann lange dauern. Ergebnisse liegen unter `logs/tuning/`. `--no-fetch` unterbindet PVOG-Nachladen beim Tuning. Das historische Ergebnis in `docs/evaluation/` wird nicht automatisch eingelesen.

## Häufige Fehler

| Symptom | Prüfung/Lösung |
| --- | --- |
| `ModuleNotFoundError` oder falscher Matcher | Gemeinsame `.venv` verwenden; `bash scripts/setup.sh` erneut ausführen; keine alte Root-Implementierung hinzufügen |
| FIM-SQLite fehlt | `AUTHORITY_MATCHER_HOME` prüfen, Sicherung bereitstellen oder `scripts/sync-fim.py` ausführen |
| Erste Suche dauert sehr lange | FIM-Import, erster Modell-Download oder Indexaufbau; Fortschritt im Terminal prüfen |
| Elasticsearch nicht erreichbar | Docker und Matcher-Compose prüfen; bei eigener Instanz `ELASTICSEARCH_URL` setzen |
| PostGIS-Verbindung fehlgeschlagen | Containerstatus, Port und `DATABASE_URL` prüfen; `load` nach neuer Installation ausführen |
| Keine passende Behörde | Kategorie/Standort und Resolver-Abdeckung prüfen; fehlende Daten sind keine Aussage über tatsächliche Nichtzuständigkeit |
| WFS-Dienst ausgefallen | Fehler bleibt sichtbar; bewusst auf `--backend local` wechseln, wenn der vorhandene Datenstand genügt |
| Keine Leistungsbeschreibung/Öffnungszeiten | Zusatzdaten sind optional; regionaler PVOG-Cache oder geeignete öffentliche Quelle kann fehlen |
| Keine Photon-Vorschläge | Mindestens drei Zeichen; Geocoder-Ausfall kann als leere Vorschlagsliste mit `fehler` erscheinen |
| TLS-Zertifikatsfehler auf macOS | Vertrauenswürdiges CA-Bundle korrekt konfigurieren, ggf. `SSL_CERT_FILE=/etc/ssl/cert.pem`; Zertifikatsprüfung beibehalten |

`--backend local` betrifft nur den Resolver. `--no-live` betrifft das PVOG-Nachladen des Matchers. `--zusatzinfos aus` deaktiviert nur den OSM-/Google-Zusatzclient; PVOG-Ergänzungen und Webseitenabrufe sind im aktuellen Web-Backend weiterhin möglich. Auch Adresssuche, Leaflet und Kartenkacheln können Netz benötigen. Diese Schalter ergeben zusammen keinen vollständig offline betriebenen Browsermodus.
