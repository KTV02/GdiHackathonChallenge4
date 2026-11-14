# Behördenkategorie aus einer Textanfrage

Die aktuelle Aufgabe endet beim **Kategorie-Keyword**. Die CLI fragt weder PostGIS noch GeoServer ab, geocodiert keine Adresse und behauptet keine konkrete örtliche Zuständigkeit für einen Nutzerstandort. Der bestehende Flask-/Website-Code und der Download `suchdienst-main` bleiben unverändert.

## Start

Python 3.10 oder neuer genügt. Im Projekt ist bereits eine `.venv` vorhanden:

```bash
cd ~/Desktop/GdiHackathonChallenge4
source .venv/bin/activate
python -m pip install -r requirements-cli.txt
python -m authority_matcher "Ich möchte meinen Hund anmelden"
```

Das am 13.11.2026 live geprüfte Ergebnis enthält:

```text
Behördenkategorie: Kommunale Steuerverwaltung
Keyword: KOMMUNALE_STEUERVERWALTUNG
Leistung: Hundehaltung anmelden (L100039.LB.8966636)
Suchreferenz: Mainz (073150000000); keine Standortprüfung.
```

Ohne Argument fragt die CLI in einem interaktiven Terminal nach dem Anliegen:

```bash
python -m authority_matcher
```

Weitere Beispiele:

```bash
python -m authority_matcher "Ich möchte mein Auto anmelden"
python -m authority_matcher "Ich möchte einen Personalausweis beantragen"
python -m authority_matcher "Ich möchte meinen Reisepass beantragen"
python -m authority_matcher "Ich möchte mich arbeitslos melden"
python -m authority_matcher "Schenkungsteuerbescheid erhalten"
python -m authority_matcher "Schenkung anmelden" --json
python -m authority_matcher "Gewerbe anmelden" --json
python -m authority_matcher "Hund" --json
python -m authority_matcher categories
```

Auto anmelden ergibt `KFZ_ZULASSUNGSBEHOERDE`. Personalausweis und Reisepass ergeben in der Referenz Mainz `BUERGERSERVICE`; arbeitslos melden ergibt `BUNDESAGENTUR_FUER_ARBEIT`. Bei „Gewerbe anmelden“ sind offiziell mehrere Stellen unterschiedlicher Kategorien verknüpft: Das Ergebnis ist `ambiguous_category`, kein willkürlich ausgewähltes Keyword. „Hund“ allein benötigt eine Präzisierung der Leistung.

Bei mehreren Leistungen zeigt die CLI Kandidaten. In einem interaktiven Terminal kann eine Nummer gewählt werden. Für Skripte und spätere Oberflächen gibt es eine explizite Auswahl:

```bash
python -m authority_matcher "Hund" --service-id L100039.LB.8966636 --json
```

Die ID muss zu einem plausiblen Suchtreffer gehören. Bei unvollständiger Seitennavigation wird keine Leistung automatisch ausgewählt.

## Was die Anwendung tatsächlich macht

1. Deutsche Füllwörter entfernen, HTML/Steuerzeichen bereinigen und einige Wortformen vereinheitlichen. Aus „Ich möchte meinen Hund anmelden“ wird `hund anmelden`. Verneinungen bleiben erhalten.
2. Den offiziellen PVOG-Endpunkt `GET /v7/servicedescriptions/{ars}` mit dieser Suchanfrage aufrufen. Seiten werden bis zum konfigurierten Limit gelesen. Der Live-Suchindex und seine Verwaltungsleistungsdaten stammen vom PVOG.
3. Die Treffer gegen Titel **und die offiziell von PVOG gelieferten `synonyms`** prüfen, ergänzt durch Beschreibungstext. Jedes Synonym wird einzeln bewertet; Wörter aus unterschiedlichen Synonymen werden nicht zu einer erfundenen Formulierung zusammengelegt. Ein Treffer nur auf „beantragen“ reicht nicht. `anmelden`, `abmelden` und `ummelden` werden nicht als austauschbare Handlungen behandelt. Bei gleichem Score wird ein genau passender offizieller Titel gegenüber einem allgemeinen Synonym bevorzugt. Der zusätzliche lokale Score ist eine lexikalische Heuristik, keine Wahrscheinlichkeit und kein PVOG-Relevanzwert.
4. Bei ausreichend eindeutigem Treffer `GET /v6/servicedescriptions/{id}/detail?ars=…&useArsHierarchy=true` abrufen. Dort stehen tatsächliche `organisations`-Verknüpfungen mit OE-ID, Rolle und Kriterien.
5. Nur offizielle Rollen `01` (zuständige Stelle und Ansprechpunkt) und `02` (zuständige Stelle) aus der XZuFi-Rollen-Codeliste verwenden. Rolle `03` (Ansprechpunkt), ausdrücklich nicht öffentliche Einträge sowie zusätzliche ungeklärte Kriterien liefern kein bestätigtes Kategorie-Ergebnis.
6. Die verknüpften **Organisation-IDs** in das eigene Kategorieverzeichnis übersetzen. Eine hinterlegte Organisation kann dadurch beliebig viele offiziell mit ihr verknüpfte Leistungen abdecken. Es wird keine selbst erfundene Tabelle „alle Verwaltungsleistungen → Behörde“ benötigt.

PVOG liefert in den verwendeten Antworten **kein allgemeines Feld `authority_category`**. Die Kategoriecodes wie `BUERGERSERVICE` sind unsere eigene, überprüfbare Normalisierung. Sie sind weder offizielle PVOG-Codes noch bereits mit einem BKG-/GeoServer-Layernamen gleichgesetzt.

## Geprüfte Quellen im vorhandenen Download

Vor der Implementierung wurden die Projektdateien, die vorhandenen Datenquellen und der Suchdienst-Download untersucht. Im Projekt lagen keine lokale PostGIS-Verbindungsdatei und kein lokales GeoServer-Schema vor. Nach der Eingrenzung auf Kategorien wurde dafür kein Adapter angelegt.

Diese Dateien in `suchdienst-main` erklären die Herkunft der Beziehungen und die API:

- `src/main/java/de/dataport/suchdienst/importer/model/GenericZustaendigkeitTransferObjekt.java`: liest `leistungID` und `gebietID` aus XZuFi 2.2/2.3.
- `src/main/java/de/dataport/suchdienst/indexer/service/RelationImportService.java`: importiert die Zuständigkeitsrelationen in den Zuständigkeitsindex.
- `src/main/java/de/dataport/suchdienst/suchen/controller/ServiceDescriptionController.java`: v7-Suche und v6-Details; empfiehlt Gemeinde-ARS und verlangt eine Seitengröße von mindestens fünf.
- `src/main/java/de/dataport/suchdienst/suchen/model/out/organisation/OeInfoOutModel.java`: Organisation-ID, Titel, Rolle, Kriterien und Sichtbarkeitskennzeichen.
- `src/main/java/de/dataport/suchdienst/suchen/model/out/leistung/SdOutModelBase.java`: `synonyms` enthält unter anderem redaktionelle Kontextbegriffe und alternative Leistungsbezeichnungen.
- `src/main/resources/indexmappings/settings_verwaltungsleistungen_1.json`: deutsche Stoppwörter, Normalisierung, Stemmer und optionale Kompositazerlegung.
- `src/main/resources/codelisten/urn__xoev-de__fim__codeliste__xzufi.zustaendigkeitsrolle_1.0.xml`: Unterscheidung zuständige Stelle/Ansprechpunkt.

Der neue Python-Code verwendet die öffentliche API. Er ist keine Kopie des Java-Suchdienstes und bildet dessen Elasticsearch-Analyzer nicht vollständig nach. Ein leerer selbst gestarteter Elasticsearch-Server würde die Zuständigkeitsbeziehungen nicht enthalten.

Offizielle Einstiegspunkte: [PVOG API-Dokumentation](https://produktportal.pvog.fitko.de/api/suchdienst/) und [Swagger UI](https://pvog.fitko.net/suchdienst/api/swagger-ui/index.html).

Das Hund-Beispiel beruht auf dieser wirklichen Kette:

```text
L100039.LB.8966636 — Hundehaltung anmelden
LeiKa: 99102013104000
  → L100039.OE.249983665 — Rolle 02, Zuständige Stelle
  → eigene Kategorie: KOMMUNALE_STEUERVERWALTUNG
```

Die [Stadt Mainz beschreibt dieses Sachgebiet und seine Zugehörigkeit zur Steuerverwaltung](https://www.mainz.de/vv/oe/sachgebiet-gewerbesteuer-vergnuegungssteuer-jagdsteuer-hundesteuer-zweitwohnungsabgabe-beherbergungsabgabe). Die [PVOG-Leistungsdetails](https://pvog.fitko.net/suchdienst/api/v6/servicedescriptions/L100039.LB.8966636/detail?ars=073150000000&useArsHierarchy=true) liefern die konkrete Leistungs-Organisation-Verknüpfung. Es wird nicht pauschal behauptet, dass jede Hundeangelegenheit zum Ordnungsamt oder zur Steuerverwaltung gehört.

## Konfiguration und Abdeckung

Die Standardeinstellungen stehen in [`config/authority-matcher.json`](../config/authority-matcher.json). Pfade darin sind relativ zur jeweiligen Konfigurationsdatei. Eine andere Datei lässt sich mit `--config /absoluter/pfad/config.json` auswählen.

| Einstellung | Bedeutung |
|---|---|
| `pvog_url` | Öffentlicher oder eigener kompatibler Suchdienst, ohne Zugangsdaten in der URL |
| `reference_ars` | Zwölfstelliger Gemeinde-ARS als Suchreferenz; standardmäßig Mainz |
| `reference_region_name` | Anzeigename der Referenzregion |
| `categories_file` | Eigene Kategorien und belegte Organisation-Mitgliedschaften |
| `snapshot_file` | Begrenzter offizieller Offline-Auszug |
| `allow_snapshot_fallback` | Standard `false`; nur bei ausdrücklichem `true` automatischer Rückgriff bei PVOG-Ausfall |
| `suggest_related_on_no_match` | Standard `true`; bei leerer vollständiger Suche genau eine breitere Sachbegriffsuche für Vorschläge, ohne automatische Zuordnung |
| `snapshot_max_age_days` | Maximales Alter des Offline-Auszuges, standardmäßig 30 Tage |
| `timeout_seconds` | Zeitlimit pro HTTP-Aufruf, standardmäßig 15 Sekunden |
| `page_size`, `max_pages` | Begrenzung der Live-Suche, standardmäßig 50 × 3 Treffer |

Optional kann `PVOG_BEARER_TOKEN` aus der Umgebung gelesen werden. Bestehende `.env`-Dateien werden weder verändert noch automatisch geladen. Verbindungsfehler enthalten keine Zugangsdaten oder ungefilterten Antworttexte. Normale Suchen speichern keine Nutzereingaben; der normalisierte Text wird an den konfigurierten Suchdienst übertragen.

[`config/authority-categories.json`](../config/authority-categories.json) enthält derzeit zehn Kategorien mit jeweils einer belegten Organisation-Mitgliedschaft in der Referenz Mainz:

| Keyword | Kategorie |
|---|---|
| `FINANZAMT` | Finanzamt |
| `KOMMUNALE_STEUERVERWALTUNG` | Kommunale Steuerverwaltung |
| `BUERGERSERVICE` | Bürgerservice |
| `KFZ_ZULASSUNGSBEHOERDE` | Kfz-Zulassungsbehörde |
| `BAUAUFSICHT` | Bauaufsicht |
| `BUNDESAGENTUR_FUER_ARBEIT` | Bundesagentur für Arbeit |
| `GEWERBEBEHOERDE` | Gewerbebehörde |
| `EINHEITLICHER_ANSPRECHPARTNER` | Einheitlicher Ansprechpartner |
| `WOHNGELDSTELLE` | Wohngeldstelle |
| `AUSLANDSVERTRETUNG` | Auslandsvertretung des Bundes |

Weitere Kategorien oder Organisationen lassen sich als JSON-Einträge ergänzen. Eine Mitgliedschaft verlangt eine OE-ID, den genauen Referenz-ARS und eine Beleg-URL. Die Kategorien sind dadurch unabhängig vom Text-Matching und von den PVOG-Leistungsdaten austauschbar. Unbekannte OE-IDs liefern `category_unmapped`; die JSON-Ausgabe zeigt die IDs für die spätere Pflege an. Eine bloße Namensähnlichkeit erzeugt keine neue Zuordnung.

Mit `--ars` kann eine andere Suchreferenz gewählt werden. Die mitgelieferten Kategorienmitgliedschaften werden dabei **nicht** automatisch auf diese Region übertragen. Ohne passende Belege ergibt sich dort häufig `category_unmapped`. Für bundesweite Abdeckung müssen weitere Organisationen belegbar kategorisiert bzw. eine geeignete offizielle Organisationstypisierung angeschlossen werden. Die aktuelle Umsetzung ist bewusst kein bundesweit vollständiger Behördenklassifikator.

## Suche und Kategorien getrennt erweitern

Beim Auto-Beispiel war die fehlende Kategorieausgabe ein Fehler der ursprünglichen lokalen Trefferprüfung: Die Live-API lieferte bereits drei Leistungen, aber die Prüfung ignorierte `synonyms`. Bei der Erstzulassung `L100039.LB.277413462` steht dort ausdrücklich **„Auto anmelden“**, obwohl im Titel „Erstzulassung eines Fahrzeugs (Neuzulassung) beantragen“ steht. Diese offiziellen Synonyme werden jetzt generell ausgewertet und in neuen Offline-Exporten gespeichert; die bestehende Kategorienzuordnung der Kfz-Zulassungsbehörde musste nicht geändert werden. Die [PVOG-Dokumentation zum Suchranking](https://produktportal.pvog.fitko.de/blog/reihenfolge-suchdienst-ergebnisse/) erläutert die Nutzung von Titeln, Volltext, Kontextbegriffen und weiteren Feldern durch die Quellsuche.

Die JSON-Ausgabe zeigt unter `service.matched_field` und `service.matched_text`, über welches Feld die Leistung erkannt wurde. `search_summary.source_hits` und `search_summary.plausible_hits` unterscheiden Treffer der Datenquelle von lokal akzeptierten Treffern. Bei `no_match` erklärt die Meldung, ob die Quelle gar keine Treffer hatte oder die lokale Prüfung vorhandene Treffer verworfen hat. Bei `category_unmapped` wurde bereits eine Leistung mit offizieller Organisation gefunden; dann fehlt die eigene Kategorienzuordnung.

Die vorhandenen Lücken lassen sich ohne weitere Netzwerkanfragen auflisten:

```bash
python -m authority_matcher coverage
python -m authority_matcher coverage --json
```

Im aktualisierten Auszug vom **13.11.2026** sind für **54 von 90 Leistungen** alle zuständigen Organisationen bereits kategorisiert; **50** davon haben genau eine Kategorie ohne zusätzliche Kriterien. Es fehlen Zuordnungen für **23 weitere Organisation-IDs**, beispielsweise Führerscheinstelle, Ausländerangelegenheiten und Standesamtsaufgaben. Das ist eine Bestandsaufnahme dieses kleinen Auszuges, keine bundesweite Abdeckungsquote und keine gemessene Trefferquote für Freitextanfragen.

Für breitere Abdeckung sind drei Arbeiten sinnvoll:

1. **Sprachabdeckung:** Die offiziellen Synonyme weiterverwenden und ein Set echter Nutzeranfragen gegen erwartete Leistungen/Kategorien prüfen. Nicht zuverlässig erkannte Umschreibungen benötigen gezielte Sprachaufbereitung oder einen zusätzlichen semantischen Suchschritt. Ein solcher Schritt darf Vorschläge liefern, ersetzt aber nicht die offizielle Leistungs-Organisation-Verknüpfung. Die aktuelle Wortprüfung bleibt vorsichtig: Beispielsweise fordert „Auto abmelden“ trotz eines Live-Treffers weiterhin eine explizite Auswahl, wenn Titel und einzelne Synonyme die Handlung nicht ausreichend eindeutig bestätigen.
2. **Kategorienabdeckung:** Die von `coverage` aufgeführten **Organisationen**, nicht jede einzelne Leistung, mit Belegen in `config/authority-categories.json` einordnen. Der Bericht enthält offizielle Namen, betroffene Leistungs-IDs und Beleg-URLs. Eine Organisation-Mitgliedschaft deckt danach alle offiziell damit verknüpften Leistungen ab. Neue Kategorien nur ergänzen, wenn das fachlich definierte Kategorienvokabular sie benötigt.
3. **Daten-/Regionsabdeckung:** Für Offline-Betrieb einen breiteren offiziellen Leistungsbestand importieren und weitere Referenzregionen aufnehmen. Live greift die CLI schon auf den PVOG-Suchbestand zu, nicht nur auf die 90 Beispieldatensätze. Für weitere Regionen sind dennoch belegte Kategorienmitgliedschaften nötig. Mehr importierte Leistungsdaten erzeugen diese eigene Organisation-zu-Kategorie-Normalisierung nicht automatisch.

## Finanzamt und die Anfrage „Schenkung anmelden“

Finanzamtsleistungen sind im Live-PVOG vorhanden. Bei der Prüfung am **13.11.2026** ergaben sich zwei getrennte Lücken:

- `Schenkung anmelden` und `Schenkung anzeigen` lieferten für die Referenz Mainz keine direkten Treffer. Die breitere Suche `Schenkung` lieferte unter anderem `Schenkungsteuerbescheid erhalten` (`L100039.LB.229942405`). Auch Finanzamtsleistungen werden nicht unter jeder alltagssprachlichen Formulierung gefunden.
- Die lokale Kategorienkonfiguration enthielt bisher kein `FINANZAMT`. Die offiziell verknüpfte OE `L100039.OE.11309476`, Finanzamt Kusel-Landstuhl, ist nun mit diesem eigenen Kategoriecode aufgenommen. Das betrifft nur die belegte Suchreferenz und ist keine pauschale Aussage zum nächstgelegenen Finanzamt.

Die Quelle für die Beziehung ist der [PVOG-Detailaufruf für die Schenkungsteuerleistung](https://pvog.fitko.net/suchdienst/api/v6/servicedescriptions/L100039.LB.229942405/detail?ars=073150000000&useArsHierarchy=true). Die [Leistungsbeschreibung im Bundesportal](https://verwaltung.bund.de/leistungsverzeichnis/DE/leistung/99102016002000/herausgeber/RP-229942405/region/070000000000) behandelt im Verfahrensablauf auch die Anzeige einer Schenkung. Der Leistungstitel bezeichnet jedoch das Erhalten des Steuerbescheids. [ELSTER unterscheidet die erstmalige Anzeige ausdrücklich von der Schenkungsteuererklärung](https://www.elster.de/eportal/formulare-leistungen/alleformulare/schenkungsteuer). Diese Schritte werden in der CLI nicht als dieselbe exakt erkannte Leistung ausgegeben.

Bei leeren ursprünglichen Suchergebnissen entfernt die zusätzliche Vorschlagssuche bekannte Handlungswörter, etwa `anmelden`, und sucht einmal nach dem verbleibenden Sachbegriff. Die vollständige Anfrage bleibt unverändert. Die JSON-Ausgabe enthält `related_query` und `related_candidates`; diese tragen `match_scope: subject_only` und `request_match_verified: false`. Die Originalanfrage behält `no_match` und `category: null`. Im Terminal werden die Treffer ausdrücklich als verwandt angezeigt, und `--keyword-only` gibt für solche Vorschläge kein Keyword aus. Auch sachfremde Kontexttreffer des PVOG können darunter sein.

Um eine verwandte Leistung ausdrücklich zu prüfen, deren genaue Bezeichnung als neue Anfrage eingeben. Zum Nachweis der katalogisierten Finanzamtsleistung funktioniert beispielsweise:

```bash
python -m authority_matcher "Schenkungsteuerbescheid erhalten"
# Kategorie: Finanzamt; Keyword: FINANZAMT
```

Das ist kein Ersatz für die ursprüngliche Schenkungsanzeige. Deren genaue Erkennung bleibt offen, solange keine passende offizielle Leistung oder eine gesondert belegte Intent-Zuordnung verfügbar ist.

## Offline-Betrieb und Ausfälle

```bash
python -m authority_matcher "Ich möchte meinen Hund anmelden" --offline
python -m authority_matcher "Ich möchte einen Reisepass beantragen" --offline --json
```

[`data/pvog/mainz-extract.json`](../data/pvog/mainz-extract.json) enthält 90 tatsächlich abgerufene Leistungen, einschließlich ihrer offiziellen Suchsynonyme und öffentlich verwendbaren Organisation-Verknüpfungen. Abrufdatum: **13.11.2026**. Der Grundauszug stammt aus sieben Beispielabfragen zu Hund, Personalausweis, Reisepass, Kfz-Zulassung, Arbeitslosmeldung, Gewerbe und Baugenehmigung. Zwei ergänzende Abfragen zu `Schenkung` und `Schenkungsteuer` lieferten fünf weitere Leistungen, darunter vier mit Finanzamtsverknüpfung. Der Auszug enthält auch Suchtreffer, die für eine konkrete Anfrage ungeeignet sind oder noch keine lokale Kategorienzuordnung haben. Bei Ergänzungen bleibt `retrieved_at` auf dem Zeitpunkt des älteren Grundauszuges; neue Einzelbelege enthalten ihre tatsächliche Abrufzeit, und `updated_at` dokumentiert die Ergänzung.

Jeder Detaildatensatz enthält die Quell-URL, Abrufzeit und SHA-256-Prüfsumme des gespeicherten Detailobjekts. Die Prüfsumme erkennt versehentliche Änderungen; sie ist keine digitale Signatur einer Behörde. Kontaktpersonen und Organisations-Kontaktdetails werden für die Klassifikation nicht heruntergeladen oder in den Auszug aufgenommen.

Der Auszug ist **kein vollständiger PVOG-Katalog**. Sein Status lautet `matched_snapshot`, selbst wenn die Kategorie eindeutig erscheint. Eine unbekannte Anfrage wird als fehlende Abdeckung oder mehrdeutig ausgegeben. Die Anwendung führt im Offline-Modus keine Netzwerkanfrage aus. Abgelaufene Auszüge und andere Referenzregionen werden abgelehnt.

Einen neuen Auszug erzeugen:

```bash
python -m authority_matcher snapshot "Hund anmelden" --ars 073150000000 --output /tmp/pvog-hund-neu.json
```

Der Befehl exportiert echte aktuelle API-Treffer und überschreibt keine vorhandene Datei. Um ihn zu verwenden, `snapshot_file` in einer eigenen Konfiguration auf diese Datei setzen. Für einen breiteren Katalog sind mehrere fachlich geeignete Exporte bzw. ein gesonderter offizieller Datenimport nötig. Die CLI importiert kein beliebiges XZuFi-XML und verwendet keine synthetischen Suchdienst-Integrationstestdaten als Behördenwissen.

Ein Live-Ausfall wird standardmäßig als `pvog_unavailable` ausgegeben. Mit konfiguriertem Fallback wird nur ein passender und gültiger Auszug benutzt, auch wenn erst der Detailaufruf ausfällt; die Ausgabe bleibt ausdrücklich `matched_snapshot`.

## Wiederverwendung für Website und MCP

```python
from authority_matcher import AuthorityMatcher

matcher = AuthorityMatcher()
result = matcher.match("Ich möchte meinen Hund anmelden")
if result["status"] == "matched":
    keyword = result["category"]["code"]
```

`AuthorityMatcher.match()` gibt ein JSON-kompatibles Dictionary zurück und ist unabhängig von CLI, Flask, Datenbank und GeoServer. Für eine spätere Oberfläche können `candidates` angezeigt und die Auswahl mit `service_id=…` erneut übergeben werden. `offline=True` verwendet ausschließlich den Auszug. Eigene Datenquellen können über den kleinen `search(query, ars)`-/`detail(service_id, ars)`-Katalogvertrag eingebunden werden; Konfiguration und Quellen müssen vertrauenswürdig bereitgestellt werden.

Für eine Pipeline gibt es reine Keyword-Ausgabe:

```bash
python -m authority_matcher "Ich möchte meinen Hund anmelden" --keyword-only
```

Nur das Keyword steht auf stdout; Einschränkungen stehen auf stderr. Für robuste Integrationen ist `--json` vorzuziehen: Die Ausgabe enthält Kategorie, Leistungs-ID, LeiKa-Schlüssel, offizielle Organisationen/Rollen/Kriterien, Referenz-ARS, Datenmodus und Belege. `jurisdiction_resolved` bleibt immer `false`.

| Exitcode | Bedeutung |
|---|---|
| `0` | Live ermittelte, eindeutige Kategorie (`matched`) |
| `2` | Offline-Ergebnis (`matched_snapshot`) oder fachlich ungeklärtes Ergebnis wie `ambiguous_service`, `ambiguous_category`, `category_unmapped`, `criteria_required`, `no_match` |
| `1` | Konfigurations-, Quellen-, Eingabe- oder Verbindungsfehler |
| `130` | Interaktiver Abbruch |

## Prüfung

```bash
python -m unittest discover -s tests -v
```

Am 13.11.2026 bestanden **49 Python-Tests**, davon 31 für die CLI und 18 bestehende Backend-Tests. Sie prüfen unter anderem offizielle Synonyme, deren Abgrenzung gegen gegensätzliche Handlungen und andere Anliegen, den Auto-Fall und Finanzamtsleistungen mit echten Offline-Daten, getrennte verwandte Vorschläge ohne automatische Zuordnung, die Kategorien-Lückenübersicht, falsche/mehrdeutige Leistungen, unvollständige Seiten, Rollen, Sichtbarkeit, zusätzliche Kriterien, fehlende Kategorien, Regionsgrenzen, Ausfälle, expliziten Fallback, Prüfsummen, Ablaufdatum und Ausgabeformate. Tests gegen den offiziellen Auszug verwenden eine feste Testzeit, damit spätere Ausführungen nicht am beabsichtigten Ablaufdatum der Beispieldaten scheitern. Die Tests führen keine Live-Aufrufe aus.

Zusätzlich wurden Auto anmelden, Hund anmelden, Personalausweis beantragen, Reisepass beantragen und arbeitslos melden erfolgreich live geprüft. „Gewerbe anmelden“ bleibt wegen mehrerer Kategorien ungeklärt; „Auto abmelden“ wegen der vorsichtigen lokalen Sprachprüfung. Eine sachfremde „Rakete anmelden“-Anfrage liefert keine Live-Treffer. Die bestehenden Frontend-Tests konnten in dieser Umgebung nicht ausgeführt werden, weil `node` nicht installiert ist; Frontend-Dateien wurden nicht geändert.

Die Sprachaufbereitung ist ein transparenter Prototyp. Sie ersetzt keine umfassende semantische Sprachverarbeitung. Ungebräuchliche Umschreibungen, mehrere Anliegen oder fehlende PVOG-Daten können eine Präzisierung erfordern. Die offenen Fälle bleiben sichtbar, statt als sichere Kategorie ausgegeben zu werden.
