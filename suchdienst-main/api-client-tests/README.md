# OpenAPI-Java-Clienttests

Dieses eigenständige Maven-Projekt prüft die API aus Sicht eines externen
Java-Konsumenten: Clientgenerierung, Kompilierung, striktes JSON-Binding und
Live-Aufrufe. Es hat keine Abhängigkeit auf den Suchdienst-Backend-Code.
Java 25 und die eingerichteten Maven-Repositories werden vorausgesetzt.
Backendstart, Infrastruktur und HTTP-Export stehen in der
[Root-README: Lokaler Ablauf](../README.md#lokaler-ablauf).

## Aufbau und Spezifikation

| Bestandteil | Aufgabe |
| --- | --- |
| `generated-client` | OpenAPI Generator CLI 7.25.0 erzeugt den vollständigen Client unter `target/openapi`; `package` baut das Client-JAR. |
| `client-tests` | Binding-, Mock-HTTP- und Live-Vertragstests verwenden ausschließlich den generierten Client. PV-Schema-Modelle dienen nur dem XML-Fixture-Import. |
| [generator-config.json](generator-config.json) | Spring RestClient, Spring 7, Jackson 3, Jakarta und Unterstützung für `oneOf`-Interfaces. |

Standardeingabe ist `target/docs/openapi.yml` im Repositorywurzelverzeichnis.
`-Dopenapi.spec=/absoluter/pfad` wählt einen anderen Export.
Die Spezifikation und generierte Quellen werden nicht nachbearbeitet.
Vor jeder Generierung werden alte Generatorquellen, kompilierte Klassen und
Compilerstatus des generierten Moduls entfernt. Damit bleiben bei Modelländerungen
keine veralteten Typen im Client-JAR.

Der HTTP-Export `/v3/api-docs.yaml` enthält öffentliche, Beta-, interne und
deprecated APIs. Generierbarkeit und Kompilierung gelten für dieses gesamte
Dokument; das Ergebnis ist **kein ausschließlich öffentliches SDK**.
Die Live-Tests konzentrieren sich auf die unten aufgeführten öffentlichen und
Beta-Leseoperationen. Admin-/Push-Endpunkte werden nur für das Setup verwendet.

Surefire führt sowohl `*Test` als auch `*IT` in der normalen `test`-Phase aus:

| Profil | Umfang |
| --- | --- |
| Ohne Profil | Offline-Tests und Live-API-Tests; benötigt eine dedizierte Backendinstanz. |
| `unit-tests` | Alle Offline-Tests; vorhandene aktuelle Spezifikation genügt. |
| `jzufi-model-contract` | JZuFi-Polymorphie-, Relations- und Zeitformat-Binding; geeignet für den begrenzten Modellexport. |

## Striktes Binding und Grenzen

- Der generierte Mapper und die
  [StrictApiClientFactory](client-tests/src/test/java/de/dataport/suchdienst/clienttest/support/StrictApiClientFactory.java)
  lehnen unbekannte Modellfelder, ungültige Subtypen und unbekannte oder numerische
  Enum-Werte ab. Es gibt keine toleranten Ersatzwerte.
- Implizite zusätzliche Properties werden nicht als freie Maps erzeugt
  (`disallowAdditionalPropertiesIfNotPresent=true`, Generatorstandard).
  Dies ist bewusst strenger als OpenAPI bei fehlendem `additionalProperties`.
  Explizit dokumentierte Maps behalten ihre dynamischen Schlüssel.
- `openApiNullable=false`: Geprüft werden Leseverträge, nicht die Unterscheidung
  zwischen fehlenden und explizit leeren Werten in PATCH-Operationen.
- DTO-Binding ist keine vollständige JSON-Schema-Validierung. Pflichtfelder,
  Wertebereiche und fachliche Constraints brauchen zusätzliche Prüfungen.
- Fehlerstatus und Fehlerbody werden erhalten und anschließend streng an das
  dokumentierte Modell gebunden. Der Relations-v2-Fehlervertrag verwendet auch
  für HTTP 500 `ApiErrorModel` einschließlich `requestId`.

### JZuFi-Ausgabevertrag

Nur die vier neuen `/v2/.../jzufi-2-3`-Operationen verwenden lokale v23-Modelle.
Bestandsrouten für 2.2 und 2.3 bleiben deprecated mit unveränderten Nutzdaten;
sie erhalten keine eigenen neuen Consumer-Live-Szenarien.
Die Originalmodelle, Versionskonvertierung und Persistenz bleiben bei `pv_schema`.

Die lokalen Modelle werden aus dessen versionierten XSD-Ressourcen erzeugt.
Die [XJC-Bindings](../src/main/resources/jzufi/2_3_1/jzufi-model.xjb)
beschreiben `@type` und unmittelbare `allOf`-Eltern für Zuständigkeit,
Leistungsstruktur, Formulardatei, Anschrift, Kontaktperson, Onlinedienst, Sprache,
Kategorieklasse und GML-`CodeType`. Der Präfix `Jzufi23` trennt ihre Schemanamen
von Bestandsmodellen.

Konkrete JZuFi-Basen und Untertypen können strukturell überlappen. Deshalb
überspringt [SwaggerCoreConfiguration](../src/main/java/de/dataport/suchdienst/config/SwaggerCoreConfiguration.java)
Springdocs automatische `oneOf`-Auswahl nur im lokalen v23-Paket.
Für andere Modelle bleibt sie aktiv. Die notwendigen Swagger-Core-Workarounds
sind dort dokumentiert; die Modul-Typ-Patterns bei
[AbstractServiceModule](../src/main/java/de/dataport/suchdienst/suchen/model/out/leistung/detail/AbstractServiceModule.java).

**Validierungsgrenze:** Discriminator-Mappings steuern die Typbindung, ersetzen
aber keine exklusive JSON-Schema-Auswahl. Eine Basisreferenz prüft nur die
Basisbedingungen, nicht automatisch alle Pflichtfelder des ausgewählten
Untertyps. Konkrete Schemas und Client-Dispatch werden deshalb separat geprüft.

Wichtige JSON-Formen:

- Formulardateien enthalten Link-, XML- und Binärvarianten in ihrer Reihenfolge.
  `xmldaten.any` ist eine Liste von XML-Strings; `binaerdaten.value` ist Base64,
  einschließlich der Dateimetadaten im umgebenden Objekt.
- XML-Datum und reine Uhrzeit bleiben Strings mit optionalem Offset und
  Sekundenbruchteilen; sie werden nicht als RFC3339-`date-time` umgedeutet.
- Zeitstempel erhalten ihren Zeitpunkt und XML-Offset bei der Backendausgabe.
  Das interne `Calendar`-Modell begrenzt auf Millisekunden; ohne XML-Offset gilt
  die JVM-Standardzeitzone. Der Client bindet `OffsetDateTime` und normalisiert
  standardmäßig auf UTC, ohne den Zeitpunkt zu ändern.

Bei einem Schema-Update müssen `pv.schema.version` und die dazu passende
`pv.schema.annotation.version`, XJC-Bindings, MapStruct-Untertypmapping und
Bindingfälle zusammenpassen.

## Testebenen und Zuständigkeiten

| Ebene | Primärer Nachweis |
| --- | --- |
| Allgemeines Binding | `StrictJsonBindingTest`, `ServiceModuleBindingTest`, `SearchViewModuleBindingTest`: unbekannte Felder/Enums, Datum und View-spezifische Module. |
| JZuFi-Varianten | `JzufiPolymorphismBindingTest`: Familieninventar, Vererbung, positive/negative Typ- und Feldbindung sowie Root-Roundtrips. |
| JZuFi-Wrapper | `JzufiRelationBindingTest`: gemischte Listen, Reihenfolge, Formular-/Seitenwrapper, Seitenmetadaten und dokumentiertes Beispiel. |
| XML-Zeitformate | `JzufiTemporalBindingTest`: Datum, reine Uhrzeit und unveränderter Zeitpunkt. |
| HTTP-Transport | `StrictHttpConverterTest`, `HttpErrorTransportTest`, `ParameterTransportTest`: tatsächlicher Converter, Fehlerbody, Listen, Encoding, Sprachheader, ISO-Datum und opaque Cursor. |
| Live-Verträge | Eine Operationsklasse pro aktivem Pfad/Version aus der folgenden Matrix. |
| Backend-Mapping/Producer | `JzufiModelMapperTest`, `JzufiJsonContractTest`: Mapping, Typemission, XML-/Binärinhalte, Zeitwerte und historische Persistenz. |
| Backend-Schemas | `JzufiOpenApiSchemaTest`: Modellfelder, Vererbung und Auflösungsreihenfolge. `ConcurrentOpenApiModelResolutionTest`: parallele Auflösung mit geteiltem Resolver. Weitere Schematests sichern Module und Views mit OpenAPI 3.0/3.1 ab. |
| MVC-Verträge | `SpringdocOpenApiContractTest`: produktive Exporte einschließlich Parameter-/Fehlerschemas. `JzufiVersionContractTest`: Controlleradapter, Legacy-Isolation und Fehlerantworten. |
| Fachsuite | Import/Konvertierung, Ausgabewerte, fachliche Filter, Gültigkeitsunabhängigkeit, Paging und Importänderungen über HTTP. |

Backend und Consumer teilen keine produktiven Mapping-Oracles.

Der Consumer dupliziert nicht die gesamte Fachsuite. Insbesondere beweisen
Mock-Cursorseiten keine serverseitige Mehrseitenerzeugung, Sprachheader-Mocks
keinen Übersetzungsfallback und DTOs keine vollständige Constraint-Abdeckung.
Lokale Pflichtparameterprüfungen des Generators und rohe HTTP-Probes für die
Servervalidierung sind getrennte Nachweise. Infrastrukturfehler müssen nicht
an jedem Live-Pfad künstlich ausgelöst werden.

### Pfadabdeckung

Alle Operationen sind GET. Klassennamen sind relativ zu
`client-tests/src/test/java/de/dataport/suchdienst/clienttest/contract/`;
Unterpaket und Pfadversion bestimmen den vollständigen Namen.
Jede Klasse enthält einen Live-Erfolgsfall mit generiertem DTO-Binding.

| Pfad | Operationsklasse |
| --- | --- |
| `/v7/servicedescriptions/{ars}` | `servicedescriptions.ServiceDescriptionSearchV7IT` |
| `/v8beta1/servicedescriptions` | `servicedescriptions.ServiceDescriptionSearchV8Beta1IT` |
| `/v6/servicedescriptions/{lbid}/detail` | `servicedescriptions.ServiceDescriptionDetailV6IT` |
| `/v3/servicedescriptions/leikaid` | `servicedescriptions.ServiceDescriptionLeikaSearchV3IT` |
| `/v4beta1/servicedescriptions/leikaid` | `servicedescriptions.ServiceDescriptionLeikaSearchV4Beta1IT` |
| `/v2/servicedescriptions/leikahierarchy` | `servicedescriptions.ServiceDescriptionLeikaHierarchyV2IT` |
| `/v3beta1/servicedescriptions/leikahierarchy` | `servicedescriptions.ServiceDescriptionLeikaHierarchyV3Beta1IT` |
| `/v1/servicedescriptions/suggestions/{ars}` | `servicedescriptions.ServiceDescriptionSuggestionsV1IT` |
| `/v5/organisationunits/detail` | `organisationunits.OrganisationUnitDetailV5IT` |
| `/v2/organisationunits/titles` | `organisationunits.OrganisationUnitTitlesV2IT` |
| `/v1/organisationunits/{oeId}/ars` | `organisationunits.OrganisationUnitArsV1IT` |
| `/v2/onlineservices/detail` | `onlineservices.OnlineServiceDetailV2IT` |
| `/v3beta1/onlineservices/{odId}/detail` | `onlineservices.OnlineServiceDetailV3Beta1IT` |
| `/v1beta2/onlineservices` | `onlineservices.OnlineServiceIteratorV1Beta2IT` |
| `/v1beta2/onlineservices/{odId}/organisations` | `onlineservices.OnlineServiceOrganisationsV1Beta2IT` |
| `/v1/personalmatters/{ars}` | `personalmatters.PersonalMattersV1IT` |
| `/v3/locations` | `locations.LocationSearchV3IT` |
| `/v3/locations/suggestions` | `locations.LocationSuggestionsV3IT` |
| `/v3/locations/details` | `locations.LocationDetailsV3IT` |
| `/v3/locations/ars` | `locations.LocationArsV3IT` |
| `/v2/locations/reverse` | `locations.LocationReverseV2IT` |
| `/v2/client-stats` | `statistics.ClientStatisticsV2IT` |
| `/v2/servicedescriptions/{id}/jzufi-2-3` | `xzufi.ServiceDescriptionXzufiV2IT` |
| `/v2/organisationunits/{id}/jzufi-2-3` | `xzufi.OrganisationUnitXzufiV2IT` |
| `/v2/onlineservices/{id}/jzufi-2-3` | `xzufi.OnlineServiceXzufiV2IT` |
| `/v2/relations/jzufi-2-3` | `xzufi.RelationXzufiV2IT` |

## Fixtures und Isolation

**Nur gegen eine dedizierte Testinstanz ausführen.**
Die datenbasierten Klassen teilen über `SearchFixtureExtension` eine Fixture
pro JUnit-Lauf: sechs Leistungen für Pagination, eine Organisation und einen
Onlinedienst mit definierten IDs, Texten, Gebühren, LeiKa-Schlüsseln und
Gültigkeiten. Ein laufbezogener Namespace isoliert IDs und Suchbegriffe.
Die Codelist-Fixture `sdk-contract-1` bleibt für Folgeläufe bestehen.

Die Extension importiert einmal, berechnet ausschließlich die Relationen ihres
eigenen Redaktionssystems und wartet begrenzt auf abgeschlossene Nachrichten,
stabile Neuberechnung und Suchsichtbarkeit. JUnit schließt die Ressource am
Laufende, auch bei Vorbereitungs- oder Testfehlern. Bereinigt werden nur die
eigenen Geschäftsobjekte und Relationsdaten, niemals ganze Indizes.
`SearchFixtureExtensionTest` prüft diesen Lebenszyklus ohne Backend.
Verändernde Tests brauchen eine eigene Fixture.

Die Ortsklassen starten je einen WireMock auf Port `18082`; sie dürfen nicht
parallel laufen. Backend-Geocoder-URL und gegebenenfalls
`-Dsuchdienst.geo-mock-port=<Port>` müssen übereinstimmen.
Zufällige Queries vermeiden veraltete Such-/Suggestionstreffer; feste
ARS-/Adressantworten sind identisch und dürfen gecacht werden.
Es gibt weder externe Geocoderaufrufe noch globale Cache-Löschungen.
Das Profil `integration-test` stellt die ARS-Seed-Codeliste bereit.

## Offline-Vertragsprüfungen

Alle Befehle werden im Repositorywurzelverzeichnis ausgeführt.
Der MVC-Test exportiert produktive Controller-/Advice-Signaturen anhand ihrer
Code-Herkunft; Testcontroller sind ausgeschlossen. Nur diese Instanzen sind
gemockt, Fachdienste und Elasticsearch/Redis werden nicht gestartet.
Der Export ist kein Ersatz für einen frischen HTTP-Export und Live-Tests:

```shell
mvn -Dtest=SpringdocOpenApiContractTest test &&
mvn -f api-client-tests/pom.xml -Punit-tests \
    "-Dopenapi.spec=$PWD/target/docs/springdoc-contract/all.json" clean test
```

Den MVC-Export separat ausführen: Swagger-Core initialisiert Schemaoptionen
statisch; vorherige Modelltests können die Darstellung leerer Basisschemas beeinflussen.

Der begrenzte JZuFi-Modellexport eignet sich zur gezielten Diagnose ohne Backend:

```shell
mvn -Dtest=JzufiModelMapperTest,JzufiJsonContractTest,JzufiOpenApiSchemaTest,ConcurrentOpenApiModelResolutionTest test &&
mvn -f api-client-tests/pom.xml -Pjzufi-model-contract \
    "-Dopenapi.spec=$PWD/target/docs/zustaendigkeit-openapi-3.1.json" clean test
```

Der neue JZuFi-Vertrag wird mit OpenAPI 3.1 geprüft. Backendtests sichern Mapping,
JSON und Persistenz unabhängig vom Consumer ab; `JzufiVersionContractTest` prüft
zusätzlich Routen, Deprecation-Header, Pagination und tatsächliche Fehlerantworten.
`OpenApiDocTest` in der Fachsuite validiert die über HTTP ausgelieferte Dokumentation.

Formatierung der handgeschriebenen Clienttests:

```shell
mvn -f api-client-tests/pom.xml spotless:apply
```

## CI und Fehlerdiagnose

In Merge Requests sind `OpenApi`, `Generate:OpenAPI Java Client` und
`integrationstest` verpflichtend, auch bei Renovate. Außerhalb von MRs bleiben
die OpenAPI-Jobs manuell; der Clientjob läuft nicht auf Tags.
Der Exportjob liefert die HTTP-YAML. Der Clientjob verwendet genau dieses
Artefakt und das JAR aus `Package:Jar`, startet eine eigene Backend-/ES-/Redis-
Instanz und führt `clean package` mit allen Tests aus.
Die Fachsuite verwendet separate Ressourcen und darf ihre globale
Datenbereinigung nicht mit dem Consumer teilen.

CI bewahrt Export, generierten Code, Client-JAR, JUnit-Berichte der Clienttests,
Backend-/Redis-Logs und Import-/Lösch-XMLs auf. Surefire-Berichte enthalten
Fallzahlen und Fehler; Fixture-XMLs liegen unter
`client-tests/target/fixture-artifacts`.
Logs und XMLs vor einer Weitergabe auf sensible Inhalte prüfen.
Für die Abnahme werden beide erfolgreichen Testgates benötigt, nicht allein
ein grüner Generator- oder Consumer-Build.
