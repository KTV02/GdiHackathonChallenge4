/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.clienttest.binding;

import static de.dataport.suchdienst.clienttest.support.JzufiContractAssertions.assertContains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dataport.suchdienst.client.model.Jzufi23Formular;
import de.dataport.suchdienst.client.model.Jzufi23Kontaktperson;
import de.dataport.suchdienst.client.model.Jzufi23Leistung;
import de.dataport.suchdienst.client.model.Jzufi23OrganisationseinheitErweitert;
import de.dataport.suchdienst.client.model.Jzufi23Zustaendigkeit;
import de.dataport.suchdienst.clienttest.support.StrictApiClientFactory;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Testziel: Jede in der neuen v2-OpenAPI registrierte JZuFi-2.3-Ausprägung wird vom
 * unabhängig generierten Client ohne Feldverlust gebunden, auch über mehrstufige
 * Vererbung und verschachtelte Basisfelder. Fehlende/unbekannte Typen und Felder
 * werden abgelehnt. Formular-Dateien unterstützen Links, XML und Binärdaten.
 *
 * <p>Die JSON-Beispiele enthalten gezielt unterscheidbare Basis-/Untertypfelder,
 * nicht zwingend alle XSD-Pflichtfelder. Geprüft wird das DTO-Binding;
 * Schema-Vollständigkeit und fachliche Gültigkeit sind separate Backend-Prüfungen.
 */
class JzufiPolymorphismBindingTest {

    private static final String REF = "#/components/schemas/";
    private static final String MODEL = "de.dataport.suchdienst.client.model.";
    private static final String RESPONSIBILITY_FIELDS =
        "\"id\":{\"value\":\"responsibility\"},\"leistungID\":[{\"value\":\"service-reference\"}]," +
        "\"gebietID\":[{\"value\":\"region-reference\"}]";
    private static final Map<String, String> FIELDS = Map.ofEntries(
        Map.entry("Anschrift", "\"strasse\":\"base-street\""),
        Map.entry("AnschriftOrganisationseinheit", "\"infoParkplatz\":[{\"value\":\"parking-details\"}]"),
        Map.entry("CodeType", "\"value\":\"base-code\",\"codeSpace\":\"urn:codes\""),
        Map.entry("CodeWithAuthorityType", "\"value\":\"authority-code\",\"codeSpace\":\"urn:authority\""),
        Map.entry("FormulardateiLink", "\"link\":{\"uri\":\"https://example.invalid/form\"}"),
        Map.entry("FormulardateiXML", "\"xmldaten\":{\"any\":[\"<form xmlns='urn:test'>payload</form>\"]}"),
        Map.entry(
            "FormulardateiBinaer",
            "\"binaerdaten\":{\"value\":\"AAECf/8=\",\"filename\":\"form.bin\",\"mimeCode\":\"application/octet-stream\"}"
        ),
        Map.entry("Kategorieklasse", "\"version\":\"base-class-version\""),
        Map.entry("Gebietsklasse", "\"kurzbezeichnung\":[{\"value\":\"area-short-name\"}]"),
        Map.entry("Kontaktperson", "\"vorname\":\"base-contact\""),
        Map.entry("KontaktpersonErweitert", "\"organisationseinheitID\":[{\"value\":\"organisation-reference\"}]"),
        Map.entry("LeistungsstrukturObjekt", "\"leistungsgruppierungLeiKa\":{\"code\":\"100\"}"),
        Map.entry(
            "LeistungsstrukturObjektMitVerrichtung",
            "\"leistungsgruppierungLeiKa\":{\"code\":\"100\"},\"leistungsobjektID\":{\"value\":\"service-object\"}," +
                "\"verrichtungLeiKa\":{\"code\":\"001\"}"
        ),
        Map.entry(
            "LeistungsstrukturObjektMitVerrichtungUndDetail",
            "\"leistungsgruppierungLeiKa\":{\"code\":\"100\"},\"leistungsobjektID\":{\"value\":\"service-object\"}," +
                "\"verrichtungLeiKa\":{\"code\":\"001\"},\"verrichtungsdetail\":[{\"value\":\"activity-detail\"}]"
        ),
        Map.entry("Onlinedienst", "\"kennzeichenOeffentlichAnzeigen\":true"),
        Map.entry(
            "OnlinedienstErweitert",
            "\"zustaendigkeit\":[{\"@type\":\"ZustaendigkeitOnlinedienst\",\"leistungID\":[{\"value\":\"service-reference\"}]}]"
        ),
        Map.entry("OnlinedienstVorlage", "\"infoEinfuehrung\":\"template-introduction\""),
        Map.entry("Sprache", "\"languageCode\":\"de\""),
        Map.entry("Sprachversion", "\"version\":\"language-version\",\"kennzeichenMaschinelleUebersetzung\":true"),
        Map.entry("Zustaendigkeit", RESPONSIBILITY_FIELDS),
        Map.entry(
            "ZustaendigkeitOrganisationseinheit",
            RESPONSIBILITY_FIELDS +
                ",\"rolle\":[{\"code\":\"02\"}],\"kennzeichenTerminvereinbarungVerpflichtend\":true," +
                "\"kriterium\":[{\"codeKriterium\":{\"code\":{\"code\":\"01\"}},\"auspraegung\":[\"A-Z\"]}]"
        ),
        Map.entry(
            "ZustaendigkeitOnlinedienst",
            RESPONSIBILITY_FIELDS +
                ",\"organisationseinheit\":[{\"organisationseinheitID\":{\"value\":\"oe\"},\"rolle\":[{\"code\":\"02\"}]}]"
        ),
        Map.entry(
            "ZustaendigkeitPerson",
            RESPONSIBILITY_FIELDS +
                ",\"kriterium\":[{\"codeKriterium\":{\"code\":{\"code\":\"01\"}},\"auspraegung\":[\"A-Z\"]}]"
        )
    );

    /** Die Fallliste stammt aus der tatsächlichen Spezifikation, nicht aus einer zweiten Untertypenliste. */
    static Stream<Map.Entry<String, JsonNode>> bases() {
        var document = YAMLMapper.builder()
            .build()
            .readTree(Path.of(System.getProperty("openapi.spec")).toFile());
        return document
            .path("components")
            .path("schemas")
            .properties()
            .stream()
            .filter(entry -> entry.getKey().startsWith("Jzufi23"))
            .filter(entry -> "@type".equals(entry.getValue().path("discriminator").path("propertyName").asString()));
    }

    static Stream<Arguments> variants() {
        return bases().flatMap(base ->
            base
                .getValue()
                .path("discriminator")
                .path("mapping")
                .properties()
                .stream()
                .map(variant -> Arguments.of(base.getKey(), variant.getKey(), variant.getValue().asString()))
        );
    }

    static Stream<String> baseNames() {
        return bases().map(Map.Entry::getKey);
    }

    @Test
    void generatedModelsKeepOriginalInheritanceWithoutCompanionModels() throws Exception {
        for (String name : List.of("Anschrift", "LeistungsstrukturObjekt", "LeistungsstrukturObjektMitVerrichtung")) {
            var type = Class.forName(MODEL + "Jzufi23" + name);
            assertEquals(type, type.getConstructor().newInstance().getClass());
        }
        for (var pair : List.of(
            List.of("AnschriftOrganisationseinheit", "Anschrift"),
            List.of("ZustaendigkeitOrganisationseinheit", "Zustaendigkeit"),
            List.of("ZustaendigkeitOnlinedienst", "Zustaendigkeit"),
            List.of("ZustaendigkeitPerson", "Zustaendigkeit"),
            List.of("FormulardateiLink", "Formulardatei"),
            List.of("FormulardateiXML", "Formulardatei"),
            List.of("FormulardateiBinaer", "Formulardatei"),
            List.of("KontaktpersonErweitert", "Kontaktperson"),
            List.of("OnlinedienstErweitert", "Onlinedienst"),
            List.of("OnlinedienstVorlage", "Onlinedienst"),
            List.of("Sprachversion", "Sprache"),
            List.of("Gebietsklasse", "Kategorieklasse"),
            List.of("CodeWithAuthorityType", "CodeType"),
            List.of("LeistungsstrukturObjekt", "Leistungsstruktur"),
            List.of("LeistungsstrukturObjektMitVerrichtung", "LeistungsstrukturObjekt"),
            List.of("LeistungsstrukturObjektMitVerrichtungUndDetail", "LeistungsstrukturObjektMitVerrichtung")
        )) {
            assertEquals(
                Class.forName(MODEL + "Jzufi23" + pair.get(1)),
                Class.forName(MODEL + "Jzufi23" + pair.get(0)).getSuperclass()
            );
        }
        for (String name : FIELDS.keySet()) {
            assertThrows(ClassNotFoundException.class, () -> Class.forName(MODEL + "Jzufi23" + name + "Exact"));
        }
        for (var base : bases().toList()) {
            assertThrows(ClassNotFoundException.class, () -> Class.forName(MODEL + base.getKey() + "Selection"));
        }
        assertThrows(ClassNotFoundException.class, () -> Class.forName(MODEL + "Jzufi23LeistungStruktur"));
    }

    @Test
    void mixedAddressListPreservesConcreteTypesOrderAndFields() {
        var mapper = StrictApiClientFactory.mapper();
        String child = "{\"strasse\":\"child-street\"," + payload("AnschriftOrganisationseinheit").substring(1);
        String json = rootPayload("Jzufi23Anschrift", payload("Anschrift") + "," + child);
        var root = mapper.readValue(json, Jzufi23OrganisationseinheitErweitert.class);
        var contact = assertInstanceOf(Jzufi23Kontaktperson.class, root.getKontaktperson().getFirst());
        assertEquals("Jzufi23Anschrift", contact.getAnschrift().getFirst().getClass().getSimpleName());
        assertEquals("Jzufi23AnschriftOrganisationseinheit", contact.getAnschrift().get(1).getClass().getSimpleName());
        assertContains(mapper.readTree(json), mapper.valueToTree(root));
    }

    @Test
    void coversAllReachableFamiliesAndAllFormFiles() {
        var families = bases().toList();
        assertEquals(9, families.size());
        for (String family : List.of(
            "Zustaendigkeit",
            "Leistungsstruktur",
            "Anschrift",
            "Kontaktperson",
            "Onlinedienst",
            "Sprache",
            "Kategorieklasse",
            "CodeType",
            "Formulardatei"
        )) {
            String name = "Jzufi23" + family;
            var base = families
                .stream()
                .filter(entry -> entry.getKey().equals(name))
                .findFirst()
                .orElseThrow()
                .getValue();
            assertTrue(base.path("discriminator").path("mapping").size() > 0);
            assertTrue(base.path("oneOf").isMissingNode());
            assertTrue(base.path("if").isMissingNode());
        }
        var files = families
            .stream()
            .filter(entry -> entry.getKey().equals("Jzufi23Formulardatei"))
            .findFirst()
            .orElseThrow();
        assertEquals(
            java.util.Set.of("FormulardateiLink", "FormulardateiXML", "FormulardateiBinaer"),
            files
                .getValue()
                .path("discriminator")
                .path("mapping")
                .properties()
                .stream()
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet())
        );
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("variants")
    void bindsEveryDocumentedVariantAndItsFields(String base, String type, String reference) throws Exception {
        assertTrue(reference.startsWith(REF), reference);
        assertTrue(FIELDS.containsKey(type), "Neue Ausprägung benötigt Feldwerte: " + type);
        var mapper = StrictApiClientFactory.mapper();
        String json = payload(type);
        Object bound = mapper.readValue(json, Class.forName(MODEL + base));
        assertEquals(Class.forName(MODEL + reference.substring(REF.length())), bound.getClass());
        assertContains(mapper.readTree(json), mapper.readTree(mapper.writeValueAsString(bound)));
        var rebound = mapper.readValue(mapper.writeValueAsString(bound), Class.forName(MODEL + base));
        assertEquals(bound.getClass(), rebound.getClass());
        assertContains(mapper.readTree(json), mapper.valueToTree(rebound));
        var root = rootPayload(base, json);
        var response = mapper.readValue(root, rootType(base));
        assertContains(mapper.readTree(root), mapper.readTree(mapper.writeValueAsString(response)));
        var rootRoundTrip = mapper.readValue(mapper.writeValueAsString(response), rootType(base));
        assertContains(mapper.readTree(root), mapper.valueToTree(rootRoundTrip));
    }

    @ParameterizedTest(name = "{0}: fehlende/unbekannte Typkennung")
    @MethodSource("baseNames")
    void rejectsMissingAndUnknownTypesDirectlyAndInRootProperties(String base) throws Exception {
        var mapper = StrictApiClientFactory.mapper();
        var target = Class.forName(MODEL + base);
        assertThrows(InvalidTypeIdException.class, () -> mapper.readValue("{}", target));
        assertThrows(InvalidTypeIdException.class, () -> mapper.readValue("{\"@type\":\"unknown\"}", target));
        var root = rootType(base);
        assertThrows(InvalidTypeIdException.class, () -> mapper.readValue(rootPayload(base, "{}"), root));
        assertThrows(InvalidTypeIdException.class, () ->
            mapper.readValue(rootPayload(base, "{\"@type\":\"unknown\"}"), root)
        );
    }

    @ParameterizedTest(name = "{0}: {1}, unbekanntes Feld")
    @MethodSource("variants")
    void rejectsUnknownFieldsDirectlyAndInRootProperties(String base, String type, String reference) {
        var mapper = StrictApiClientFactory.mapper();
        String json = "{\"notInTheContract\":true," + payload(type).substring(1);
        assertThrows(UnrecognizedPropertyException.class, () -> mapper.readValue(json, Class.forName(MODEL + base)));
        assertThrows(UnrecognizedPropertyException.class, () ->
            mapper.readValue(rootPayload(base, json), rootType(base))
        );
    }

    @Test
    void rejectsFieldsOfOtherResponsibilityVariants() {
        assertThrows(UnrecognizedPropertyException.class, () ->
            StrictApiClientFactory.mapper().readValue(
                "{\"@type\":\"ZustaendigkeitPerson\",\"rolle\":[{\"code\":\"02\"}]}",
                Jzufi23Zustaendigkeit.class
            )
        );
    }

    @Test
    void rejectsUnknownNestedRoleFields() {
        assertThrows(UnrecognizedPropertyException.class, () ->
            StrictApiClientFactory.mapper().readValue(
                "{\"@type\":\"ZustaendigkeitOrganisationseinheit\",\"rolle\":[{\"code\":\"02\",\"unexpected\":true}]}",
                Jzufi23Zustaendigkeit.class
            )
        );
    }

    @Test
    void bindsDifferentPolymorphicFamiliesInTheSameOrganisation() {
        var mapper = StrictApiClientFactory.mapper();
        String organisation =
            "{\"kontaktperson\":[" +
            payload("KontaktpersonErweitert") +
            "]," +
            "\"kategorie\":[{\"klasse\":" +
            payload("Gebietsklasse") +
            "}]," +
            "\"terminvereinbarungsdienst\":[" +
            payload("OnlinedienstVorlage") +
            "]}";
        assertContains(
            mapper.readTree(organisation),
            mapper.valueToTree(mapper.readValue(organisation, Jzufi23OrganisationseinheitErweitert.class))
        );
    }

    private static String payload(String type) {
        return "{\"@type\":\"" + type + "\"," + FIELDS.get(type) + "}";
    }

    private static Class<?> rootType(String base) {
        return switch (base) {
            case "Jzufi23Formulardatei", "Jzufi23Sprache", "Jzufi23Zustaendigkeit" -> Jzufi23Formular.class;
            case "Jzufi23Leistungsstruktur" -> Jzufi23Leistung.class;
            default -> Jzufi23OrganisationseinheitErweitert.class;
        };
    }

    private static String rootPayload(String base, String payload) {
        return switch (base) {
            case "Jzufi23Anschrift" -> "{\"kontaktperson\":[{\"@type\":\"Kontaktperson\",\"anschrift\":[" +
                payload +
                "]}]}";
            case "Jzufi23Kontaktperson" -> "{\"kontaktperson\":[" + payload + "]}";
            case "Jzufi23Onlinedienst" -> "{\"terminvereinbarungsdienst\":[" + payload + "]}";
            case "Jzufi23Kategorieklasse" -> "{\"kategorie\":[{\"klasse\":" + payload + "}]}";
            case "Jzufi23CodeType" -> "{\"kontaktperson\":[{\"@type\":\"Kontaktperson\",\"anschrift\":[{\"@type\":\"Anschrift\",\"geokodierung\":[{\"Point\":{\"name\":[" +
                payload +
                "]}}]}]}]}";
            case "Jzufi23Formulardatei" -> "{\"datei\":[" + payload + "]}";
            case "Jzufi23Sprache" -> "{\"datei\":[{\"@type\":\"FormulardateiLink\",\"link\":{\"uri\":\"https://example.invalid/form\"},\"sprache\":[" +
                payload +
                "]}]}";
            case "Jzufi23Zustaendigkeit" -> "{\"zustaendigkeit\":[" + payload + "]}";
            case "Jzufi23Leistungsstruktur" -> "{\"struktur\":" + payload + "}";
            default -> throw new IllegalArgumentException("Ungeprüfte Basis: " + base);
        };
    }
}
