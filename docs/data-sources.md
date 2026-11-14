# Datenquellen, externe Dienste und Lizenzen

Maßgeblich für einzelne Resolver-Quellen ist das maschinenlesbare [Quellenmanifest](../behoerden-resolver/src/behoerden_resolver/resources/sources.json): Endpunkt, Layer, Filter, CRS, Herkunft, Lizenz, Namensnennung und Verarbeitungsregeln. Der [Abdeckungsbericht](../behoerden-resolver/docs/ABDECKUNG.md) misst den tatsächlich mitgelieferten Bestand; konfigurierte, aber nicht geladene Quellen zählen nicht als Abdeckung.

## Fachliche Daten

| Quelle | Verwendung | Einordnung |
| --- | --- | --- |
| FIM-Portal | Lokal importierte Leistungssteckbriefe/Stammtexte und Suchindex | Vollständiger Import wird außerhalb von Git gespeichert; FIM allein enthält nicht durchgehend eine belastbare Behördenzuordnung |
| Öffentlicher PVOG-Suchdienst | Regionale Leistungsbeschreibungen, JZuFi-Beziehungen, Organisationstitel und Online-Dienste | Gezielte Referenzregionen; keine bundesweite Vollständigkeitsgarantie |
| BKG BZB Open | Fachbezirke von Amts-/Landgerichten, Jobcentern und Arbeitsagenturen | Datenlizenz Deutschland – Namensnennung 2.0; genaue Angaben im Manifest |
| BKG VG25 | Verwaltungsgrenzen als räumliche Referenz und Grundlage belegter Ableitungen | CC BY 4.0 laut Projektmanifest; eine Grenze allein benennt keine zuständige Fachbehörde |
| BZSt GEMFA | Finanzamt-Gemeindezuordnungen; Verbindung mit VG25 bei eindeutigen ganzen Gemeinden | Teilflächen, keine erfundene Vollständigkeit geteilter Bezirke |
| Länder/Kommunen | Weitere Fachflächen, Behördenverzeichnisse und Standorte | Unterschiedliche Regionen, Aktualität und Lizenzen je Quelle |
| OpenStreetMap | Lokale Standortimporte sowie Kontakt-/Öffnungszeitenergänzung | ODbL 1.0, © OpenStreetMap-Mitwirkende; keine amtliche Zuständigkeitsbestätigung |
| BKG BZB ohne Open | Optional berechtigt importierter Dump für Staatsanwaltschaften/Hauptzollämter | Zugangsbeschränkt; Dump und abgeleitete Snapshots nicht im Repository |

Der redaktionelle Fallkatalog und die Themenregeln des Finders sind projektspezifische Heuristiken. Testanfragen und erwartete Kategorien sind redaktionelle Annahmen, kein amtlicher Goldstandard. Rechtszuordnungen stehen separat in `category_mappings.json` mit Belegstufe und Prüfdaten.

## Netzwerkzugriffe und Datenweitergabe

| Zugriff | Übertragene Informationen |
| --- | --- |
| Elasticsearch lokal | Freitextanliegen für Suche und Ranking |
| Matcher → PVOG | LeiKa-/Leistungs-IDs und feste Referenz-ARS, kein ursprünglicher Bürgerfreitext |
| Nominatim/Photon | Eingegebene Adresse; bei Photon auch Eingaben für Vorschläge |
| WFS | Geografische Such-BBOX/Koordinate und Fachkategorie/Layerfilter |
| PVOG-Webergänzung | Leistungsschlüssel und Gemeinde-ARS; Gemeindeermittlung kann die Standortkoordinate an Nominatim Reverse senden |
| OSM-/Google-Details | Behördenname, Behördenadresse/-koordinate bzw. Suchregion |
| Öffentliche Behördenwebseite | HTTP-Abruf einer ermittelten Behörden-URL für Öffnungszeiten |
| Leaflet-CDN/Kartenanbieter | Browserabrufe für Bibliothek und Kacheln des betrachteten Kartenausschnitts |

Die frühere pauschale Aussage „keine Nutzerkoordinate an Zusatzdienste“ gilt nicht für alle Web-Details: Die Gemeindeermittlung nutzt Reverse-Geocodierung. Der interaktive CLI-Modus merkt die letzte Adresse nur innerhalb der Sitzung. PVOG-/FIM-Daten werden dagegen im lokalen Katalog dauerhaft zwischengespeichert. Allgemeine Dienstlogs und Providerbedingungen sind gesondert zu beachten.

## Adresssuche und Karte

Öffentliche Nominatim-Suche wird gedrosselt und nicht als Autocomplete verwendet. Photon ermöglicht die Vorschlagsliste. Mehrere Clients/Prozesse besitzen teils eigene Drosseln; der Prototyp hat keine dienstübergreifende globale Anfragebegrenzung. Vor größerem gemeinsamem Betrieb sind eigene/geeignete Dienste und eine zentrale Begrenzung nötig.

Die Karte verwendet Leaflet 1.9.4 aus unpkg und TopPlusOpen Grau des BKG. Die Quellenkennzeichnung steht in der Karte. Online-Routenlinks führen zu externen Kartendiensten. Die aktuelle Oberfläche ist deutschsprachig; die frühere zweisprachige Flask-Oberfläche gehört nicht zum finalen Stand.

## Weitergabe und Fremdcode

Quellendaten behalten ihre jeweilige Lizenz und Namensnennung, unabhängig vom Anwendungscode. Der Resolver enthält eine eigene [MIT-Lizenz](../behoerden-resolver/LICENSE); daraus folgt keine pauschale Lizenzierung aller anderen Projektteile. Die festgeschriebene FIM-CLI 0.9.0 ist laut Lockdatei AGPL-3.0-or-later und benötigt Node.js ab 22.12. Sie wird als separates Importwerkzeug über npm installiert; ihre Manifest-/Lockdateien bleiben unter `tools/fim-cli/`, ihre Lizenzdateien werden mit dem Paket installiert. `node_modules` wird nicht versioniert.

Der frühere Ordner `suchdienst-main/` war eine Referenzkopie des externen **PVOG Suchdiensts 11.17.0** laut dessen `publiccode.yml` (EUPL-1.2), keine Laufzeitabhängigkeit dieser Anwendung. Originalprojekt: [FITKO/PVOG Suchdienst auf Open CoDE](https://gitlab.opencode.de/fitko/pvog/suchdienst). Der unveränderte Fremdquellbaum wurde entfernt; die Anwendung nutzt dessen öffentliche HTTP-API.

Technische Einstiege: [FIM-Portal](https://fimportal.de/), [PVOG Swagger](https://pvog.fitko.net/suchdienst/api/swagger-ui/index.html), [BKG GeoDatenZentrum](https://gdz.bkg.bund.de/) und [OSM-Lizenz](https://www.openstreetmap.org/copyright). Live-Verfügbarkeit und Nutzungsbedingungen müssen beim späteren Betrieb erneut geprüft werden.
