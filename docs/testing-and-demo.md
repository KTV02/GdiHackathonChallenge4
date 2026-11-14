# Prüfstand und Demo

Stand der Bereinigung: 14. November 2026. Die Tests beziehen sich auf den bereinigten Quellbaum. Für diese Prüfung wurde ein vorhandener Python-3.12-Abhängigkeitssatz verwendet und über `PYTHONPATH` ausdrücklich der bereinigte Code geladen. Eine vollständige Neuinstallation mit Paket-/Modell-Downloads wurde in dieser Umgebung nicht durchgeführt.

## Automatisierte Prüfung

| Bereich | Ergebnis dieser Sitzung | Aussage |
| --- | --- | --- |
| Matcher | 57 bestanden | Normalisierung, FIM-Sync, PVOG/JZuFi-Verträge, Ranking und Cache mit Testantworten |
| Resolver | 55 bestanden, 3 übersprungen | Offline-Verarbeitung, Geometrien, Kategorien und Fallbacks; drei echte PostGIS-Tests benötigen `TEST_DATABASE_URL` |
| Finder | 130 bestanden, zusätzlich 7 unittest-Unterprüfungen | Pipeline, CLI, echte Snapshotgeometrien, Zusatzdaten und Leistungsbeschreibungen mit ersetzten externen Diensten |
| HTTP-Server | 9 durch Umgebung blockiert | Lokales Socket-Binding wurde mit `PermissionError: Operation not permitted` abgelehnt; kein erfolgreicher Lauf dieser Fälle behauptet |
| Snapshot-Bericht | Erfolgreich neu gemessen | 46 mitgelieferte offene Snapshots, alle Records-Prüfsummen im Lieferinventar kontrolliert |
| Shell/JavaScript | Syntaxprüfung bestanden | Setup-, Test- und macOS-Startskripte sowie finales `app.js` |
| CLI-Einstiege | Fünf `--help`-Aufrufe und Paketimporte erfolgreich | Bereinigter Quellbaum wird verwendet |
| Offline-Auswertung | 1.112/1.126 richtig, 4 falsch, 10 ungeklärt | Redaktionelle Trainingsmenge; keine unabhängige Qualitätsgarantie |
| Dokumentation | 83 lokale Links und Versions-/Pfadangaben geprüft | Aktuelle Einstiegspunkte und Detaildokumente |

Damit bestehen **242 Haupttests**; **3 Datenbanktests** und **9 HTTP-Tests** sind hier nicht erfolgreich ausgeführt worden. Die neun HTTP-Fälle wurden zunächst versucht und scheiterten am gesperrten Port-Binding. Der anschließende erfolgreiche Lauf hat ausschließlich diese `ServerTest`-Klasse explizit abgewählt. Das ist kein vollständiger Live-Systemnachweis.

Ein bestehender Resolver-Test prüfte den `non_spatial`-Status und rief dabei unbeabsichtigt den echten Standortindex ab. Er verwendet nun `point_fallback=False`, weil sein Prüfziel der ortsunabhängige Status ist; eigene Standorttests bleiben erhalten. Das Laufzeitverhalten des Resolvers wurde dafür nicht geändert.

## Reproduzieren

```bash
# Nach Einrichtung aus dem Repository-Hauptordner:
bash scripts/test.sh

# Bei absichtlich gesperrten lokalen Ports, ausdrücklich ohne HTTP-Serverklasse:
PYTEST_ADDOPTS='-k "not ServerTest"' bash scripts/test.sh
```

`scripts/test.sh` startet jede Komponente in einem eigenen Prozess und isoliert `AUTHORITY_MATCHER_HOME` in einem temporären Verzeichnis. So werden weder gleichnamige Testmodule vermischt noch die persönliche FIM-Datenbank als Testbestand verwendet.

Echte PostGIS-Prüfung nur mit separater Testdatenbank:

```bash
docker compose -f behoerden-resolver/compose.yaml exec -T db createdb -U resolver authorities_test
TEST_DATABASE_URL='postgresql://resolver:resolver-local@127.0.0.1:55432/authorities_test' bash scripts/test.sh
```

Die Datenbank muss auf `_test` enden. Die GitHub-Actions-Konfiguration im Repository-Hauptordner richtet diesen Dienst automatisch ein. Ein entfernter Actions-Lauf wurde bei dieser Bereinigung nicht gestartet. Vor der späteren Übergabe sollten die HTTP- und PostGIS-Fälle in einer uneingeschränkten lokalen Umgebung bzw. CI vollständig grün sein.

Weitere Prüfungen:

```bash
bash -n scripts/setup.sh scripts/test.sh
zsh -n web-starten.command scripts/tuning.command scripts/import-bzb.command
node --check behoerden-finder/src/behoerden_finder/web/static/app.js
.venv/bin/python behoerden-resolver/scripts/measure_snapshots.py
.venv/bin/python behoerden-finder/scripts/eval_offline.py
```

Die Offline-Auswertung misst ausschließlich die redaktionellen Regeln gegen ebenfalls redaktionelle Erwartungen. Sie ersetzt keine unabhängige bundesweite Qualitätsmessung. `eval_endtoend.py` benötigt zusätzlich den echten Matcher-/Suchbestand.

## Manueller Democheck

1. Docker-Dienste starten, Snapshots laden und Webapp auf Port 8765 öffnen.
2. Ein Anliegen mit einer öffentlichen Testadresse suchen, etwa „Hund anmelden“ und „Rathaus Mainz“. Nominatim sowie separat Photon-Vorschläge prüfen.
3. Karte, Trefferstatus und Quellen kontrollieren. Bei einem reinen Standortvorschlag die fehlende belegte Flächenzuständigkeit beachten.
4. Leistungsbeschreibung öffnen und den Herkunftshinweis sowie verfügbare Unterlagen/Formulare prüfen. Ein Text kann aus einer anderen Landesredaktion stammen oder fehlen.
5. Kontakt, Öffnungszeiten und Online-Dienste bei einem zweiten Beispiel prüfen; die amtliche Seite zum Vergleich öffnen.
6. Unbekannte Adresse, leeres Ergebnis, mehrdeutige Kategorie und einen bewusst nicht erreichbaren Zusatzdienst prüfen.

Die vollständige Browserdemo einschließlich externer Dienste wurde bei dieser Bereinigung nicht erneut ausgeführt. Historische Quellenprüfungen im Resolver-Manifest bestätigen nur den jeweils gespeicherten Abrufstand.
