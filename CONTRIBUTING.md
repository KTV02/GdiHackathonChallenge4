# Weiterentwicklung

Bitte zuerst die [Haupt-README](README.md) und die [Architektur](docs/architecture.md) lesen. Alle drei Pakete werden editierbar in der gemeinsamen `.venv/` installiert; `bash scripts/test.sh` prüft sie getrennt. PostGIS-Integrationstests dürfen ausschließlich eine separate Datenbank mit `_test` am Ende verwenden.

## Änderungen zuordnen

| Änderung | Ort und Prüfung |
| --- | --- |
| Anliegen, Fallkatalog, Themenregeln | `behoerden-finder/src/behoerden_finder/`; betroffene Tests und Offline-Auswertung |
| Ranking, FIM-/PVOG-Verknüpfung | `behoerden-matcher/authority_matcher/`; Matcher-Tests, anschließend getrennte Kontrollmengen |
| Datenquelle oder fachliche Gebietszuordnung | Resolver-`resources/` und Adapter; Quellen-/Lizenzbeleg, `verify`, vollständiger `fetch`, Import und räumliche Stichprobe |
| Weboberfläche oder API | Finder-`web/` und Web-Tests; manuelle Prüfung von Karte, Details und Fehlern |

Für neue Quellen Herkunft, Raumumfang, Quelldatum, CRS, Layer/Filter, Lizenz, Namensnennung und fachliche Eignung im Quellenmanifest dokumentieren. Ein Standortpunkt oder eine Verwaltungsgrenze allein belegt keinen Fachbezirk. Anschließend die gemessene Abdeckung und die betroffenen Anleitungen aktualisieren.

Keine virtuellen Umgebungen, `node_modules`, Zugangsdaten, eingeschränkten Daten, privaten Testadressen oder einzelnen Laufprotokolle committen. Beispielkonfigurationen dürfen Platzhalter enthalten. Öffentliche Snapshots nur vollständig und mit Manifest/Prüfsumme übernehmen. Bei neuen Fremdpaketen Lizenzdateien und Lockdatei prüfen.

Änderungen sollten Problem, resultierendes Verhalten und relevante Prüfungen beschreiben. Vor einem Push den Commit mit dem Repository-Eigentümer abstimmen. Fachliche Restaufgaben sind insbesondere regionale Abdeckung, Qualität der Leistungszuordnung, redaktionelle Rechtszuordnungen und ein belastbarer Betrieb externer Dienste. Die früheren Demo-Issues beschreiben teils einen älteren Prototyp und sind kein aktueller Statusnachweis.
