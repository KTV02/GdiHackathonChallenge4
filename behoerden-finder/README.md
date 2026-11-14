# Behörden-Finder: Webapp, CLI und Pipeline

Dieses Paket verbindet Leistungs-Matcher, Geocodierung und räumlichen Resolver. Die vollständige Einrichtung aller drei Schwesterpakete steht in der [Haupt-README](../README.md). Alle folgenden Befehle beginnen im Repository-Hauptordner und verwenden die gemeinsame `.venv/`.

## Start und Beispiele

```bash
.venv/bin/behoerden-finder-web
# Browser: http://127.0.0.1:8765

.venv/bin/behoerden-finder --anliegen 'Ich brauche einen Erbschein' --adresse 'Rathaus Mainz'
.venv/bin/behoerden-finder --kategorie finanzamt --adresse 'Esplanade 38, Ingolstadt' --backend local
.venv/bin/behoerden-finder --kategorie jobcenter --lat 49.488 --lon 8.466 --json
.venv/bin/behoerden-finder
```

Ohne Argumente fragt die CLI Anliegen und Adresse interaktiv ab. Enter bei der Adresse verwendet die letzte Adresse derselben Sitzung. `--kategorie` überspringt den Matcher; gemeinsame `--lat`/`--lon` überspringen den Geocoder. Die feste Kategorie plus Koordinate ist der einfachste unabhängige Resolver-Test.

## Ergebnis und Optionen

| Feld | Inhalt |
| --- | --- |
| `leistung` | Titel, LeiKa-Schlüssel und Ranking-Score der erkannten Leistung |
| `kategorie` | ID, Label, Herkunft, Score und Kandidaten |
| `standort` | WGS84-Koordinate, Anzeigename, Genauigkeit und mögliche Alternativen |
| `zustaendigkeit` | Resolver-Antwort einschließlich Treffern, Quellen, Lizenz und Originalattributen |
| `status` | Fachlicher Ergebniszustand, siehe unten |
| `hinweise`, `fehler` | Einschränkungen oder Fehlermeldung |

Finder-Status: `zustaendige_behoerde`, `mehrere_kandidaten`, `naechster_standort`, `ortsunabhaengig`, `kontext_erforderlich`, `keine_abdeckung`, `keine_kategorie`, `kategorie_mehrdeutig`, `adresse_nicht_gefunden`, `resolver_fehler`.

Exitcodes: **0** Ergebnis/Anlaufstelle, **2** kein auflösbares Ergebnis, **1** Eingabe- oder technischer Fehler. `--json` erzeugt maschinenlesbare Ausgabe; Flächen sind standardmäßig entfernt. `--mit-geometrie` behält sie, `--details`/`--debug` ergänzt Matcher-Rohdaten.

| Optionsgruppe | Häufige Optionen |
| --- | --- |
| Matcher | `--no-semantic`, `--no-live`, `--reference-ars`, `--live-top`, `--no-start-docker` |
| Redaktionelle Ergänzungen | `--matcher-vorrang`, `--no-themenhinweise`, `--bei-mehrdeutig stop\|beste` |
| Resolver | `--backend auto\|local\|wfs`, `--no-point-fallback`, `--radius-km`, `--point-limit`, `--dsn` |
| Webserver | `--host`, `--port`, `--backend`, `--no-live`, `--no-semantic`, `--matcher-vorrang`, `--zusatzinfos auto\|osm\|google\|aus` |

Nicht alle CLI-Optionen existieren auch im Webstarter; `--help` ist jeweils maßgeblich.

## Fachliches Verhalten

Der Matcher liefert amtlich belegte Kategorien, Kandidaten oder ein ungeklärtes Ergebnis. Der Finder ergänzt den redaktionellen **Fallkatalog** aus `faelle_daten.py` sowie **Themenhinweise** aus `themen.py`. Starke Katalogtreffer können standardmäßig eine abweichende amtliche Matcher-Kategorie ersetzen; `--matcher-vorrang` gibt amtlichen Vorschlägen Vorrang. Themenhinweise sind Rückfälle und überschreiben keinen amtlichen Vorschlag. Beides ist keine amtliche Klassifikation.

Die Tabelle `faelle` liegt in der FIM-SQLite-Datenbank. Pflegebefehle:

```bash
.venv/bin/python -m behoerden_finder.faelle status
.venv/bin/python -m behoerden_finder.faelle installieren
.venv/bin/python -m behoerden_finder.faelle pruefen 'Hund anmelden'
```

Ohne auflösbare Kategorie wird die Adresse nicht geocodiert. Mehrdeutige Kategorien bleiben auswählbar; die CLI kann mit `--bei-mehrdeutig beste` bewusst die bestplatzierte Kategorie nehmen. Die Webapp bietet eine Auswahl an. Nominatim-Suche erfolgt für Deutschland und ohne Autocomplete; Photon unterstützt Vorschläge. Ungenaue und mehrdeutige Standorte werden im Pipeline-Ergebnis gekennzeichnet.

Der räumliche Resolver unterscheidet Fachflächen, gesetzliche Aufgaben-Zuordnungen und nahe Standortpunkte. Eine Anlaufstelle ist keine bestätigte persönliche oder verfahrensbezogene Zuständigkeit. Bei Grenz-/Mehrfachtreffern werden mehrere Kandidaten zurückgegeben.

## Webfunktionen und Zusatzdaten

Die deutschsprachige Oberfläche verwendet Leaflet und TopPlusOpen Grau. Der Webserver aus der Python-Standardbibliothek liefert Oberfläche und [HTTP-API](../docs/web-api.md) gemeinsam auf Port 8765. Es ist kein Flask-Server und kein Frontend-Build notwendig.

Leistungsinformationen werden aus bereits gespeicherten `jzufi_services` gelesen: Kurztext, Voraussetzungen, Unterlagen, Kosten, Fristen, Ablauf und Formulare/Links, soweit enthalten. Das Herkunftsland wird genannt, weil die Beschreibung aus einer anderen Region stammen kann. Ein redaktioneller Kategorie-Treffer garantiert keinen dazugehörigen vollständigen amtlichen Leistungstext.

Kontaktdaten/Öffnungszeiten kommen aus Resolver-Originalattributen, PVOG, OSM bzw. optional Google Places und gegebenenfalls einer öffentlichen Behördenwebseite. Die Anzeige bevorzugt vorhandene amtliche Informationen. Die Quellen bleiben prüfbar; Zusatzdienstfehler verhindern möglichst nicht das Hauptresultat. `--zusatzinfos aus` deaktiviert nur den OSM-/Google-Client. PVOG und Webseiten können weiterhin abgefragt werden.

Die aktuelle Web-Dekoration entfernt interne Hinweise zur Kategorieherkunft. Vollständige Herkunftsdiagnosen sind deshalb über CLI/Matcher-Details bzw. die Pipeline zu prüfen; die Oberfläche zeigt sie nicht durchgehend. Konfiguration und Datenweitergabe: [Betrieb](../docs/operations.md) und [Datenquellen](../docs/data-sources.md).

## Direkte Python-Einbindung

```python
from behoerden_finder.pipeline import Finder

result = Finder().find(None, None, kategorie='jobcenter', coords=(49.488, 8.466))
```

Die Koordinatentuple ist hier **(lat, lon)**; der Finder übergibt dem Resolver separat **lon, lat**. Für Freitext wird ein `AuthorityMatcher` aus `behoerden_finder.matching` eingesetzt. Matcher, Geocoder und Resolver-Funktion lassen sich für Tests/Integrationen ersetzen. `Finder.find(...)` liefert das oben beschriebene Dictionary; das Web-Backend ergänzt Anzeigefelder.

## Tests und Auswertung

```bash
bash scripts/test.sh
.venv/bin/python behoerden-finder/scripts/eval_offline.py --fehler
.venv/bin/python behoerden-finder/scripts/eval_endtoend.py --help
```

Die automatischen Finder-Tests verwenden Test-Doubles für externe Dienste. `tests/test_real_data.py` nutzt zusätzlich echte mitgelieferte Resolver-Snapshots mit einem lokalen Punkt-in-Polygon-Ersatz; das ist kein Live-Systemtest gegen Elasticsearch, Nominatim oder PostGIS. Die redaktionellen Auswertungsmengen liegen unter `../behoerden-matcher/data/`; Training und Kontrollmengen getrennt betrachten.
