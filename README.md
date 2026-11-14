# LeBeZi – Behörden-Zuständigkeitsfinder

LeBeZi verbindet ein frei formuliertes Bürgeranliegen mit einem Standort und sucht die passende Behörde. Die finale Hackathon-Anwendung besteht aus einer **Webapp mit Python-Backend**, einer **CLI**, einem **Leistungs-Matcher** und einem **räumlichen Resolver**. Stand: 14. November 2026.

Die Oberfläche bietet Adresssuche mit Nominatim oder Photon, eine interaktive Karte mit Zuständigkeitsflächen, Kontaktdaten und Öffnungszeiten sowie Leistungsinformationen wie erforderliche Unterlagen, Kosten, Fristen und Online-Dienste, soweit die Quellen diese enthalten. Das Erscheinungsbild orientiert sich an der GDI-DE-Übungsaufgabe.

> Das Projekt ist ein lokal betriebener Prototyp. Die 36 Behördenkategorien haben unterschiedliche Datenabdeckung. Ein naher Standort, eine redaktionelle Kategoriezuordnung und ein belegter Flächentreffer sind unterschiedliche Ergebnisse; eine verbindliche Zuständigkeitsauskunft für jeden Einzelfall ist damit nicht garantiert.

## Projektstruktur

| Pfad | Aufgabe |
| --- | --- |
| [`behoerden-finder/`](behoerden-finder/README.md) | Gemeinsame Pipeline, CLI, Webserver, Oberfläche und Zusatzinformationen |
| [`behoerden-matcher/`](behoerden-matcher/README.md) | FIM-Katalog, Elasticsearch-Suche und PVOG/JZuFi-Zuordnung zu 36 Kategorien |
| [`behoerden-resolver/`](behoerden-resolver/README.md) | WFS-Abfragen, Datenimport, PostGIS, Quellen- und Abdeckungsnachweise |
| [`scripts/`](scripts/) | Einrichtung, FIM-Synchronisierung, Tests und optionale Wartungsaufgaben |
| [`tools/fim-cli/`](tools/fim-cli/) | npm-Manifest und Lockdatei des FIM-Importwerkzeugs |
| [`docs/`](docs/README.md) | Architektur, Betrieb, Datenquellen, Validierung und Bereinigungsprotokoll |
| [`docs/reference/`](docs/reference/) | Originalaufgaben und historische Anforderungsnotizen |
| [`notebooks/`](notebooks/) | Optionale, vom Anwendungsstart unabhängige BZB-Erkundung |

## Voraussetzungen

- **Python 3.12 oder neuer** für die gemeinsame Umgebung.
- **Docker mit Compose**, z. B. Docker Desktop; für den Betrieb muss die Engine laufen.
- **Node.js ab Version 22.12 und npm** für die festgeschriebene FIM-CLI 0.9.0. Die Weboberfläche braucht keinen npm-Build.
- Internetzugang für Erstinstallation, FIM-Import und Live-Dienste. Embeddings laden beim ersten Indexaufbau zusätzlich ein Modell herunter.

Die folgenden Befehle gelten für macOS/Linux und werden, sofern nicht anders angegeben, im Repository-Hauptordner ausgeführt. Die `.command`-Starter benötigen macOS/zsh. Es gibt keine fest eingebauten persönlichen Rechnerpfade.

## Einrichtung auf einem neuen Rechner

### 1. Pakete installieren

```bash
git clone https://github.com/KTV02/GdiHackathonChallenge4.git
cd GdiHackathonChallenge4
bash scripts/setup.sh
```

Das Skript legt **eine gemeinsame `.venv/` im Hauptordner** an, installiert alle drei Python-Pakete editierbar einschließlich Testwerkzeugen und installiert das FIM-Werkzeug mit `npm ci`. Es verändert keine Shellprofile.

Bei einem abweichenden Python-Befehl: `PYTHON_BIN=python3 bash scripts/setup.sh`. Für BM25 ohne Embeddings: `bash scripts/setup.sh --no-semantic`; dann auch Matcher und Webapp immer mit `--no-semantic` starten. Ein Wechsel des Indexmodus löst einen erneuten Indexaufbau aus.

### 2. Resolver-Datenbank befüllen

```bash
docker compose -f behoerden-resolver/compose.yaml up -d db
docker compose -f behoerden-resolver/compose.yaml build cli
docker compose -f behoerden-resolver/compose.yaml run --rm cli load
docker compose -f behoerden-resolver/compose.yaml run --rm cli coverage
```

`load` importiert die mitgelieferten vollständigen Snapshots, ohne sie erneut von WFS-Diensten herunterzuladen. Die Daten liegen anschließend in einem Docker-Volume. Lizenzpflichten und tatsächlicher Umfang stehen im [Quellenmanifest](behoerden-resolver/src/behoerden_resolver/resources/sources.json) und in der [Abdeckungsdokumentation](behoerden-resolver/docs/ABDECKUNG.md).

### 3. FIM-Katalog und Suchindex vorbereiten

Der vollständige FIM-Katalog ist **kein Bestandteil des Git-Repositories**. Auf dem bisherigen Entwicklungsrechner liegt er standardmäßig unter `~/.local/share/authority-matcher/fim_catalog.sqlite3`; dieser Bestand kann weiterverwendet werden.

```bash
# Vorhandenen Bestand prüfen; auf einem neuen Rechner fehlt er zunächst.
.venv/bin/authority-matcher --status

# Nur wenn der Katalog fehlt oder ergänzt werden soll; fortsetzbar, kann lange dauern.
.venv/bin/python scripts/sync-fim.py

# Elasticsearch/Index vorbereiten und eine Anfrage ausprobieren.
.venv/bin/authority-matcher 'Ich möchte meinen Hund anmelden'
```

Elasticsearch startet bei Bedarf über die Compose-Datei des Matchers. Der erste Indexaufbau kann deutlich länger dauern als spätere Suchanfragen. Der FIM-Import erstellt noch keine flächendeckende amtliche Kategoriezuordnung: PVOG-Belege werden bei Suchanfragen gezielt nachgeladen und zwischengespeichert. Details: [Matcher](behoerden-matcher/README.md).

### 4. Webapp starten

```bash
.venv/bin/behoerden-finder-web
```

Danach **[http://127.0.0.1:8765](http://127.0.0.1:8765)** öffnen. Auf macOS startet `zsh web-starten.command` zusätzlich den PostGIS-Container. Beenden mit `Ctrl+C`.

```bash
# BM25-Modus, falls ohne semantic-Extra installiert:
.venv/bin/behoerden-finder-web --no-semantic

# Lokaler Resolver und PVOG-Cache; andere Web-Zusatzdienste bleiben separat aktiv.
.venv/bin/behoerden-finder-web --backend local --no-live
```

## CLI und einzelne Komponenten

```bash
# Ganze Kette: Anliegen + Adresse
.venv/bin/behoerden-finder --anliegen 'Ich möchte meinen Hund anmelden' \
  --adresse 'Rathaus Mainz' --json

# Nur Geocodierung + räumliche Suche
.venv/bin/behoerden-finder --kategorie finanzamt \
  --adresse 'Esplanade 38, Ingolstadt' --backend local

# Ohne Matcher und Geocoder direkt mit Koordinate testen
.venv/bin/behoerden-finder --kategorie jobcenter --lat 49.488 --lon 8.466 --backend local

# Nur Behördenkategorie, kein örtlich zuständiges Amt
.venv/bin/authority-matcher --json 'Ich brauche einen Personalausweis'

# Resolver allein, Antwort mit Geometrie und Quellenfeldern
docker compose -f behoerden-resolver/compose.yaml run --rm cli resolve jobcenter --lon 8.466 --lat 49.488
```

Die Beispielanfragen illustrieren die Bedienung; die Ergebnisse hängen vom Datenstand ab. `--help` zeigt die jeweiligen Optionen. Die Finder-CLI liefert Exitcode `0` bei Ergebnis/Anlaufstelle, `2` ohne auflösbares Ergebnis und `1` bei Eingabe- oder technischen Fehlern. Das JSON der Finder-CLI enthält standardmäßig keine Flächengeometrien; `--mit-geometrie` behält sie, `--details` ergänzt Matcher-Diagnosen.

## Wie die Suche arbeitet

1. Der Matcher sucht Leistungen im lokal importierten FIM-Katalog über Elasticsearch BM25 und optional E5-Embeddings. PVOG/JZuFi ergänzt belegte Beziehungen zwischen Leistungen und Organisationen. Der Finder ergänzt einen redaktionellen Fallkatalog und Themenregeln; diese sind fachliche Heuristiken.
2. Nominatim oder Photon übersetzt die Adresse in WGS84-Koordinaten. Bereits gewählte Koordinaten können direkt übergeben werden.
3. Der Resolver sucht Fachzuständigkeitsflächen per WFS und/oder PostGIS. Explizite gesetzliche Zuordnungen auf vorhandene Verwaltungsflächen können ergänzen. Erst danach kommen als solche gekennzeichnete nahe Behördenstandorte in Betracht.
4. Das Web-Backend ergänzt Kontaktinformationen, Öffnungszeiten, Online-Dienste und vorhandene Leistungsbeschreibungen. Das Herkunftsland eines Leistungstextes kann vom gesuchten Standort abweichen.

Die vorhandene Python-Pipeline kann aus weiteren Integrationen aufgerufen werden. **Ein MCP-Server ist in diesem Repository nicht enthalten.** Eine gesonderte MCP-Umsetzung gehört nicht zum hier dokumentierten Quellstand.

## Konfiguration und Datenhaltung

| Dienst/Bestand | Standard |
| --- | --- |
| Webserver | `127.0.0.1:8765` |
| Elasticsearch | `http://127.0.0.1:9200`, Docker-Volume des Matchers |
| PostgreSQL/PostGIS | `127.0.0.1:55432`, Datenbank `authorities`, Docker-Volume des Resolvers |
| FIM/PVOG/Indexmanifest | `~/.local/share/authority-matcher/` bzw. `AUTHORITY_MATCHER_HOME` |
| Resolver-Snapshots | `behoerden-resolver/data/snapshots/` |
| Neue Tuning-Protokolle | `logs/tuning/`, nicht versioniert |

Die [Beispielkonfiguration](.env.example) zeigt die wichtigsten Umgebungsvariablen. Python lädt `.env` **nicht automatisch**. PostGIS-Compose hat eine eigene [`.env.example`](behoerden-resolver/.env.example); Passwort und Port müssen mit `DATABASE_URL` übereinstimmen. Google Places ist optional, der Schlüssel wird nicht benötigt, wenn OSM verwendet wird. Alle Details, Sicherungshinweise und Fehlerbehebung: [Betrieb](docs/operations.md).

## Entwicklung und Prüfungen

```bash
bash scripts/test.sh

# Kategorienzuordnung des Fallkatalogs und der Themenregeln ohne Live-Dienste prüfen
.venv/bin/python behoerden-finder/scripts/eval_offline.py --fehler
```

Die Komponenten werden wegen gleichnamiger Testmodule in getrennten Prozessen geprüft. PostGIS-Integrationstests benötigen eine separate Datenbank mit Namen auf `_test`; ohne `TEST_DATABASE_URL` werden sie übersprungen. Die [GitHub-Actions-Konfiguration](.github/workflows/tests.yml) führt alle drei Testsuiten inklusive PostGIS aus. Lokale Ergebnisse und Testgrenzen stehen in [Validierung](docs/testing-and-demo.md).

## Datenqualität, Nutzung und Weiterarbeit

Die Abdeckung wird aus dem tatsächlich verfügbaren Bestand gemessen. Öffentliche Snapshots und separat berechtigt importierte BZB-Daten sind getrennt; eingeschränkte Daten werden nicht mitgeliefert. Standortnähe beweist keine Zuständigkeit, und Ranking-Scores sind keine Wahrscheinlichkeiten. Bei Öffnungszeiten, Unterlagen und regional abweichenden Leistungen bleibt die amtliche Detailseite maßgeblich.

Datenlizenzen und Namensnennungen gelten je Quelle; die MIT-Lizenz im Resolver gilt für dessen Code und ist keine pauschale Lizenz des gesamten Repositories. Siehe [Datenquellen und Lizenzen](docs/data-sources.md), [Architektur](docs/architecture.md), [Beitragen](CONTRIBUTING.md) und [Bereinigungsprotokoll](docs/cleanup-2026-11-14.md).
