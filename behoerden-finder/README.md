# Behörden-Finder (CLI)

Anliegen + Adresse → zuständige Behörde. Verbindet die drei Bausteine aus dem Hackathon-Zielbild, ohne `behoerden-matcher` oder `behoerden-resolver` zu verändern:

```
Anliegen ──Matcher (Elasticsearch/FIM/PVOG)──▶ Leistung + Kategorie (1 von 36)
Adresse  ──Geocoder (Nominatim)──────────────▶ lat / lon
Kategorie + lat/lon ──Resolver (PostGIS / WFS)──▶ örtlich zuständige Behörde
```

## Einrichtung (einmalig)

Der Resolver braucht Python ≥ 3.12, deshalb eine eigene venv, in die beide Schwesterprojekte editierbar installiert werden:

```bash
cd ~/Desktop/GdiHackathonChallenge4/behoerden-finder
python3.12 -m venv .venv          # oder /Users/ktvmb/anaconda3/bin/python3.12 -m venv .venv
.venv/bin/python -m pip install -U pip
.venv/bin/python -m pip install -e ../behoerden-resolver -e '../behoerden-matcher[elastic,semantic]' -e '.[test]'
```

Voraussetzungen zur Laufzeit (Docker Desktop muss laufen):

```bash
# Resolver-Datenbank (PostGIS, 127.0.0.1:55432), einmalig befüllen:
cd ../behoerden-resolver && docker compose up -d db && docker compose run --rm cli load && cd -
# Matcher: FIM-Datenbank unter ~/.local/share/authority-matcher muss vorhanden sein (authority-matcher --status).
# Elasticsearch startet der Matcher beim ersten Aufruf selbst.
```

Die CLI liest `DATABASE_URL` wie der Resolver (Standard: `postgresql://resolver:resolver-local@127.0.0.1:55432/authorities`).

## Benutzung

```bash
# Beides in einem Aufruf
.venv/bin/behoerden-finder --anliegen "Meine Oma hat mir 10.000 € geschenkt. Wo muss ich das melden?" \
                           --adresse "Schillerstraße 13, 55116 Mainz"

# Interaktiv: fragt Anliegen und Adresse ab (Enter bei der Adresse = letzte Adresse wiederverwenden)
.venv/bin/behoerden-finder

# Teile einzeln testen
.venv/bin/behoerden-finder --kategorie finanzamt --adresse "Marienplatz 1, München"       # ohne Matcher
.venv/bin/behoerden-finder --kategorie jobcenter --lat 49.488 --lon 8.466                  # ohne Matcher und Geocoder
.venv/bin/behoerden-finder --anliegen "Erbschein" --adresse "Am Markt 1, Mainz" --backend local   # nur PostGIS, offline

# Maschinenlesbar (ohne Flächengeometrien; mit --mit-geometrie inklusive; --details fügt Matcher-Rohdaten hinzu)
.venv/bin/behoerden-finder --anliegen "..." --adresse "..." --json
```

Weitere Optionen: `--no-semantic`, `--no-live`, `--reference-ars`, `--live-top`, `--no-start-docker` (Matcher), `--backend`, `--no-point-fallback`, `--radius-km`, `--point-limit`, `--dsn` (Resolver), `--bei-mehrdeutig beste` (bei mehrdeutiger Kategorie mit mehreren verschiedenen Kandidaten die bestplatzierte nehmen statt anzuhalten; gibt es nur eine Kandidatenkategorie, wird sie ohnehin übernommen und mit einem Hinweis versehen). `--help` zeigt alles.

**Exitcode:** `0` Behörde bzw. Anlaufstelle gefunden · `2` kein Ergebnis (keine Kategorie, Adresse unbekannt, keine Abdeckung) · `1` Eingabe- oder technischer Fehler.

## Ergebnis (`--json`)

| Feld | Inhalt |
| --- | --- |
| `leistung` | erkannte Verwaltungsleistung (Titel, LeiKa-Schlüssel, Score) |
| `kategorie` | `id`, `label`, `quelle` (`matcher`/`vorgegeben`), `score`, `kandidaten` |
| `standort` | `lat`, `lon`, `anzeige_name`, `genauigkeit` (`hausnummer`/`strasse`/`ort`), `mehrdeutig`, `alternativen` |
| `zustaendigkeit` | unveränderte Resolver-Antwort (Treffer, Quelle, Beleg, Originalattribute) |
| `status` | `zustaendige_behoerde`, `mehrere_kandidaten`, `naechster_standort`, `ortsunabhaengig`, `kontext_erforderlich`, `keine_abdeckung`, `keine_kategorie`, `kategorie_mehrdeutig`, `adresse_nicht_gefunden`, `resolver_fehler` |
| `hinweise`, `fehler` | Warnungen (ungenaue/mehrdeutige Adresse, Einzelfallabhängigkeit, Standortersatz) bzw. Fehlertext |

## Verhalten, das man kennen sollte

- **Ohne Kategorie keine Adresse an den Geocoder:** Ist die Kategorie nicht eindeutig oder fehlt sie, bricht die Pipeline vorher ab. Es geht keine Adresse an Nominatim.
- **Mehrdeutige Kategorie** wird nicht still entschieden, sondern mit Kandidaten gemeldet. Das entspricht dem Matcher, der in diesem Fall absichtlich keine Kategorie liefert.
- **Fall-Katalog (hat Vorrang):** Rund 5.400 gängige Behördengänge („Hund anmelden“, „Kfz zulassen“, „Scheidung einreichen“ …) sind jeweils einer Behördenart zugeordnet. Der Katalog liegt als Tabelle `faelle` (Spalten `kategorie`, `phrase`, `quelle`) in der FIM-Datenbank des Matchers (`fim_catalog.sqlite3`); beim ersten Lauf wird sie angelegt bzw. aktualisiert, andere Tabellen bleiben unberührt, eigene Zeilen mit anderer `quelle` bleiben erhalten. Eine Wendung trifft, wenn alle ihre Wörter in der Eingabe vorkommen (Groß-/Kleinschreibung, Umlaute, Endungen und Zusammensetzungen werden berücksichtigt). Der Katalog steht vor dem Matcher: Eine amtlich belegte Kategorie wird nur bei einem starken Treffer (Wendung aus mindestens zwei Wörtern oder mit denselben Wörtern wie die Eingabe) ersetzt; der Hinweis nennt dann auch den Vorschlag des Matchers. Das Ergebnis steht mit `quelle: fallkatalog` im JSON und als „(aus Fall-Katalog abgeleitet, nicht amtlich belegt)“ in der Ausgabe. Mit `--matcher-vorrang` gelten amtlich belegte Matcher-Ergebnisse vor dem Katalog. Pflege: `python -m behoerden_finder.faelle installieren|status|pruefen "<Anliegen>"`; die Daten stehen in `src/behoerden_finder/faelle_daten.py`.
- **Behördenart aus Stichwörtern (Rückfall):** Der Matcher leitet die Kategorie aus den amtlichen JZuFi-Daten ab. Fehlen die (z. B. bei Steuern, Kindergeld, Arbeitslosengeld, BAföG), ordnet der Finder anhand von Stichwörtern in deiner Eingabe und in den Titeln der besten Leistungen zu (`src/behoerden_finder/themen.py`, anpassbar). Das Ergebnis steht dann mit `quelle: themenhinweis` im JSON, als „(aus Stichwörtern abgeleitet)“ in der Ausgabe und mit einem Hinweis „nicht amtlich belegt“. Eine amtlich belegte Kategorie wird nie ersetzt; bei mehreren gleichrangigen Kandidaten wählen die Stichwörter nur unter diesen. Abschalten mit `--no-themenhinweise`.
- **Adresse:** Nominatim, nur Deutschland, höchstens eine Anfrage pro Sekunde, eigener User-Agent (Kontakt über `GEOCODER_CONTACT`). Liegen weitere Treffer weit entfernt (> 2 km), wird die Adresse als mehrdeutig markiert; ist die Koordinate nicht hausgenau, gibt es einen Hinweis. Eigene Instanz: `NOMINATIM_URL`.
- **Matcher-Indexmodus:** `--no-semantic` ändert gegenüber dem letzten Indexaufbau die Einstellung und löst eine Neuindizierung aus. Am besten immer denselben Modus verwenden wie bei `authority-matcher`.
- **Zuständigkeit ≠ Rechtsberatung:** Die Hinweise des Resolvers (Einzelfall, Nähe ist keine Zuständigkeit) werden unverändert durchgereicht.

## Neue Datensätze / später die Website

- Neue Quellen kommen in den Resolver (`sources.json`, `sync`/`load`). Die CLI ändert sich dafür nicht, sie ruft nur `resolve(kategorie, lon, lat)` auf.
- Für die Website: `behoerden_finder.pipeline.Finder` direkt aus z. B. FastAPI/Flask nutzen (`Finder(matcher=..., geocoder=...).find(anliegen, adresse)` liefert das JSON-Dict oben). Matcher und Geocoder sind austauschbar, ein Matcher-Objekt sollte einmal pro Prozess erzeugt werden.

## Tests

```bash
.venv/bin/python -m pytest -q
```

Die Tests laufen ohne Datenbank, Elasticsearch und Netz (Test-Doubles). Sie prüfen die Matcher-Aufrufe zusätzlich gegen die echten Signaturen aus `authority_matcher.autorun`.

`tests/test_real_data.py` testet die ganze Kette (CLI → Finder → Geocoder-Parser → Resolver-Antwort → Ausgabe) mit den **echten Snapshots** aus `../behoerden-resolver/data/snapshots` (Finanzamt Ingolstadt, Polizeiinspektion Ostprignitz-Ruppin, Standortersatz bei Mainz). PostGIS wird dort durch einen kleinen Punkt-in-Polygon-Ersatz (`tests/snapshot_resolver.py`) ersetzt; Nominatim und Elasticsearch durch Antworten im jeweiligen Format. Ohne die Snapshots wird der Test übersprungen.

### Echter Systemtest auf deinem Mac (Docker läuft, Datenbank geladen)

```bash
# 1. Resolver + PostGIS allein (Erwartung: "Finanzamt Ingolstadt", Esplanade 38, Telefon +49 (0)841 311-0)
.venv/bin/behoerden-finder --kategorie finanzamt --lat 48.7665 --lon 11.4258 --backend local
# 2. Geocoder + Resolver
.venv/bin/behoerden-finder --kategorie finanzamt --adresse "Esplanade 38, Ingolstadt" --backend local
# 3. Ganze Kette inkl. Matcher (erster Aufruf baut ggf. den Elasticsearch-Index auf, das kann dauern)
.venv/bin/behoerden-finder --anliegen "Meine Oma hat mir 10.000 € geschenkt" --adresse "Esplanade 38, Ingolstadt" --backend local
```


## Auswertung mit Testmengen

Testanfragen mit erwarteter Behörde liegen in `../behoerden-matcher/data/` (`eval_1000.jsonl` zum Einstellen, `eval_holdout.jsonl` und `eval_holdout2.jsonl` als Kontrolle; Quelle jeweils die `.txt`, Umbau mit `python data/build_eval.py <datei>.txt`). Die Erwartungen sind Annahmen, nicht amtlich belegt.

- Offline (ohne Elasticsearch), nur Fall-Katalog + Stichwörter: `PYTHONPATH=src python3 scripts/eval_offline.py [datei] [--fehler]`
- Gesamtkette mit echtem Matcher, Stufen nebeneinander (Matcher allein / + Stichwörter / + Katalog): `PYTHONPATH=src:../behoerden-matcher python3 scripts/eval_endtoend.py --fehler`
- Parameter von Suche und Auswertung einstellen: siehe `../behoerden-matcher/README` bzw. `python -m authority_matcher.tune --help`


## Weboberfläche

Start (Docker Desktop muss laufen): `zsh web-starten.command` im Projektordner, oder
`behoerden-finder/.venv/bin/python -m behoerden_finder.web [--port 8765] [--backend auto|local|wfs] [--no-live]`.
Danach http://127.0.0.1:8765 öffnen.

- Eingabe: Anliegen (Freitext) und Adresse; Adresssuche wahlweise über **Nominatim** oder **Photon**. Bei Photon erscheinen beim Tippen Adressvorschläge; ein gewählter Vorschlag wird direkt mit seiner Koordinate verwendet. (Nominatim erlaubt keine Autovervollständigung, daher dort keine Vorschläge.)
- Gestaltung nach den Vorgaben der Übungsaufgabe (GDI-DE): rote Navigation, schwarze Überschriften, türkise Bedienelemente, transparente türkise Zuständigkeitsflächen; Kartenhintergrund TopPlusOpen Grau (BKG, WMTS, dl-de/by-2-0).
- Bei mehrdeutiger Behördenart werden die Kandidaten als Auswahl angeboten. Herkunft der Behördenart (amtliche Daten / Fall-Katalog / Stichwörter) steht als Kennzeichnung daneben.
- Nur Standardbibliothek auf Serverseite (`src/behoerden_finder/web/`); Leaflet und die Kartenkacheln werden aus dem Netz geladen. Der Server bindet standardmäßig nur an 127.0.0.1.
- Zusatzinfos (Öffnungszeiten, Anschrift, Telefon, Web): Standard ist **OpenStreetMap** über die Nominatim-Suche mit `extratags` (Open Data, ODbL, kein Schlüssel; Treffer per Namensähnlichkeit/Anschrift im Umkreis der Behörde; `opening_hours` wird in Wochentage übersetzt). Umschalten mit `--zusatzinfos osm|google|aus` (Standard `auto`: Google, wenn ein Schlüssel vorhanden ist, sonst OSM).
- Leistungsbeschreibung (aufklappbar): Kurzbeschreibung, erforderliche Unterlagen, Voraussetzungen, Kosten, Fristen, Ablauf, Formulare und ein Hinweis, ob die Leistung laut Beschreibung online erledigt werden kann. Quelle sind die beim PVOG-Abruf gespeicherten Leistungsbeschreibungen (Tabelle `jzufi_services`, `src/behoerden_finder/leistung_details.py`); angezeigt wird das Land, aus dem der Text stammt.
- Zusatzinfos über Google (optional): Für die gefundene Behörde fragt der Server die Google Places API (Text Search) nach genauer Anschrift, Öffnungszeiten, Telefon und Webseite. Gesendet werden nur Name und Ort der Behörde, nicht Anliegen oder Adresse der Person. Schlüssel als Umgebungsvariable `GOOGLE_MAPS_API_KEY` oder in `~/.config/behoerdenfinder/google_maps_api_key`. Ohne Schlüssel entfallen die Zusatzinfos; der Knopf „Route in Google Maps“ (Google-Maps-URL, kein Schlüssel nötig) bleibt.
- Öffnungszeiten aus mehreren öffentlichen Quellen (`src/behoerden_finder/zusatz_quellen.py`), in dieser Reihenfolge: (1) amtliche Datensätze des Resolvers, z. B. Finanzamtsverzeichnis GemFA (`OeffnungszeitListe`) oder städtische Behördenverzeichnisse (`sprechzeiten`); (2) Portalverbund/PVOG – zuständige Stelle für Leistung und Gemeinde (ARS aus der OSM-Gemeindegrenze) mit Anschrift, Kontakt, Zeiten und Online-Diensten (z. B. ELSTER); (3) OpenStreetMap bzw. Google; (4) die Webseite der Behörde (Abschnitt „Öffnungszeiten/Sprechzeiten“, auch auf einer verlinkten Unterseite; nur öffentliche Hosts). Freitext wird mit `oeffnungszeiten.py` in Wochentage übersetzt; die Quelle steht unter den Zeiten.
