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

Weitere Optionen: `--no-semantic`, `--no-live`, `--reference-ars`, `--live-top`, `--no-start-docker` (Matcher), `--backend`, `--no-point-fallback`, `--radius-km`, `--point-limit`, `--dsn` (Resolver), `--bei-mehrdeutig beste` (bei mehrdeutiger Kategorie die bestplatzierte nehmen statt anzuhalten). `--help` zeigt alles.

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
