# WFS-Datenverarbeitung im finalen Resolver

Diese Beschreibung gilt für das Paket `behoerden-resolver` im finalen Stand vom 14. November 2026. Die frühere Verarbeitung in `app/server.py` wurde durch den eigenständigen Resolver ersetzt.

## Zwei Zugriffswege

| Weg | Ablauf | Zweck |
| --- | --- | --- |
| Live-Abfrage | Fachquelle auswählen → kleine räumliche BBOX anfragen → Quellgeometrie lesen/transformieren → vollständiges Polygon gegen Punkt prüfen | Aktuelle Fachzuständigkeitsflächen im Modus `auto`/`wfs` |
| Lokaler Import | Quelle vollständig laden → normalisieren und prüfen → Snapshot abschließen → transaktional nach PostGIS laden | Wiederholbare lokale Suche, Quellen ohne geeignete Live-Abfrage und Standortindex |

Quelle, Layer, Ausgabeformat, CRS, Fachfilter, Namensregel, Lizenz und Raumumfang stehen in `src/behoerden_resolver/resources/sources.json`. Nicht jede geladene Quelle ist ein WFS: Es gibt auch XML-, GeoJSON-, Tabellen-, gesetzlich abgeleitete und optionale Dump-Quellen mit eigenen Adaptern.

## Abruf und Normalisierung

`wfs.py` erstellt versions-/formatabhängige GetFeature-Anfragen, verarbeitet GeoJSON bzw. die unterstützten GML-Strukturen und berücksichtigt den angeforderten Koordinatenbezug. Projektionen werden mit pyproj in EPSG:4326 überführt. BBOX-Achsen und CRS müssen zur jeweiligen Quelle passen; in der Python-API sind `lon` und `lat` explizit getrennt.

Bei vollständigen Downloads kontrolliert der ETL unter anderem Paging, wiederholte Feature-IDs, sich ändernde Gesamtzahlen, Vollständigkeit und stabile Quellschlüssel. Unvollständige Antworten dürfen keinen gültigen Bestand ersetzen. Fachfilter ordnen nur passende Objekte den konfigurierten Kategorien zu. Behördentitel stammen aus expliziten Quellfeldern; reine Grenznamen werden nicht zu Behördennamen umgedeutet.

Shapely prüft Flächen. `make_valid` repariert ungültige Geometrien soweit möglich, behält die Flächenteile und markiert Reparaturen. Kollabierte Reste ohne Fläche können entfallen. Standortquellen bleiben Punkte und werden nicht zu Zuständigkeitskreisen aufgebläht.

## Snapshot und Datenbank

`etl.py` erzeugt atomare Snapshotverzeichnisse mit `manifest.json` und `records.jsonl.gz`. Das Manifest beschreibt Quelle, Abruf-/Abschlusszeit, Zahlen und Prüfsumme. Lokale Rohantworten können zur Kontrolle zusätzlich vorhanden sein; die Rohantwortverzeichnisse werden nicht mit Git ausgeliefert. Deshalb bedeutet ein Manifest mit Rohantwortprüfsummen nicht, dass alle historischen Rohantworten Teil des Checkouts sind.

`load` verwendet je Quelle den jüngsten vollständigen Snapshot. Die Datenbank prüft Importdaten und ersetzt den bisherigen Quellenbestand transaktional. Fehler dürfen den vorherigen gültigen Bestand nicht zerstören. `behoerden.authorities` hat 36 Kategoriepartitionen; Referenzgrenzen, Standortpunkte, Kategorien, Quellen und Importläufe sind getrennte Tabellen.

## Räumliche Suche und Aussagekraft

GiST-Indizes und BBOX-Vorfilter grenzen die Suche ein; `ST_Covers` prüft anschließend den tatsächlichen Punkt einschließlich Grenzpunkten. Polygonlöcher bleiben relevant. Mehrere passende Quelltreffer werden gemeinsam ausgegeben. Die vollständigen Geometrien und Originalattribute bleiben im Resolver-Ergebnis erhalten; nur die Finder-CLI entfernt Flächen standardmäßig aus ihrer JSON-Ausgabe.

Reine Verwaltungsgrenzen sind räumliche Referenzen. Erst eine belegte Zuordnung, etwa ein eindeutiger GEMFA-Gemeindeschlüssel oder eine ausdrücklich dokumentierte Rechtszuordnung, erlaubt die Verbindung mit einer Behörde. GEMFA-Polygone können nur belegte Teile eines Finanzamtsbezirks darstellen. Die Kategorieabbildung nennt Regel und Belegstufe.

Nach erfolgloser Flächensuche kann der lokale Standortindex nahe Anlaufstellen liefern. Distanz und `jurisdiction_verified=false` kennzeichnen diesen Ersatz. Ein Fach-WFS-Ausfall ist keine leere Trefferliste und führt nicht still zu einer vermeintlich bestätigten Zuständigkeit.

## Reproduzieren

```bash
# Im Repository-Hauptordner:
docker compose -f behoerden-resolver/compose.yaml run --rm cli plan jobcenter
docker compose -f behoerden-resolver/compose.yaml run --rm cli verify --source bzb_jobcenter
docker compose -f behoerden-resolver/compose.yaml run --rm cli sync --source bzb_jobcenter
docker compose -f behoerden-resolver/compose.yaml run --rm cli resolve jobcenter --lon 8.466 --lat 49.488
```

Quellen hinzufügen: [Quellenrecherche](../behoerden-resolver/docs/RECHERCHE.md) und [Erweiterung](../behoerden-resolver/docs/ERWEITERUNG_QUELLEN.md). Abdeckung und Datenstand: [ABDECKUNG.md](../behoerden-resolver/docs/ABDECKUNG.md).
