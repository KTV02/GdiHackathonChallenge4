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
package de.dataport.suchdienst.config;

import static de.dataport.suchdienst.config.OpenApiSchemaTestSupport.REF;
import static de.dataport.suchdienst.config.OpenApiSchemaTestSupport.converters;
import static de.dataport.suchdienst.config.OpenApiSchemaTestSupport.properties;
import static de.dataport.suchdienst.config.OpenApiSchemaTestSupport.schemaMapper;
import static de.dataport.suchdienst.config.OpenApiSchemaTestSupport.schemas;
import static de.dataport.suchdienst.config.OpenApiSchemaTestSupport.validator;
import static de.dataport.suchdienst.testutil.JzufiModelTestSupport.families;
import static de.dataport.suchdienst.testutil.JzufiModelTestSupport.local;
import static de.dataport.suchdienst.testutil.JzufiModelTestSupport.outputRoots;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.dataport.suchdienst.jzufi.controller.ZustaendigkeitJzufiController;
import de.dataport.suchdienst.jzufi.model.out.JzufiRelationPage23;
import de.dataport.suchdienst.jzufi.model.out.RelationPage22;
import de.dataport.suchdienst.jzufi.model.out.RelationPage23;
import de.dataport.suchdienst.jzufi.model.v23.Leistung;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Testziel: OpenAPI 3.1 beschreibt alle erreichbaren JZuFi-2.3-Modellfelder,
 * vererbbaren Strukturen und @type-Discriminator-Zuordnungen ohne zusätzliche Auswahlverträge.
 * Feldverdrahtung, Nutzdatenvalidierung und Auflösungsreihenfolge werden getrennt geprüft.
 * Die unveränderten Modell-Exporte ermöglichen weiterhin unabhängige Clienttests.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JzufiOpenApiSchemaTest {

    private final Map<Class<?>, Set<Class<?>>> modelFamilies = families();
    private Map<String, Schema> modelSchemas;

    /** Berechnet einen klassenlokalen Referenzgraphen, den die Tests nur lesen; Reihenfolgefälle nutzen eigene Resolver. */
    @BeforeAll
    void resolveModelGraph() {
        modelSchemas = schemas(converters(true), roots());
    }

    private static List<Class<?>> roots() {
        var roots = new ArrayList<>(outputRoots());
        roots.add(JzufiRelationPage23.class);
        return roots;
    }

    /**
     * Vergleicht den erreichbaren Schemaumfang mit den Java-Modellfeldern und exportiert
     * einen begrenzten Vertrag für Clienttests; ersetzt keinen vollständigen Springdoc-HTTP-Export.
     */
    @Test
    void exportsCompleteModelContract() throws Exception {
        var schemas = modelSchemas;
        assertStructuralFamilies(schemas);
        var models = modelTypes();
        for (var component : schemas.entrySet()) {
            Class<?> model = models.get(component.getKey());
            if (model == null) continue;
            var actual = properties(component.getValue(), schemas);
            actual.remove("@type");
            var expected = modelProperties(model);
            expected.remove("@type");
            assertEquals(expected, actual, model.getName());
        }
        var mapper = schemaMapper(true);
        for (var component : schemas.entrySet()) {
            if (!component.getKey().startsWith("Jzufi23")) continue;
            var schema = mapper.valueToTree(component.getValue());
            for (String keyword : List.of("oneOf", "anyOf", "if", "then", "else")) {
                assertTrue(
                    schema.findValues(keyword).isEmpty(),
                    component.getKey() + ": " + keyword
                );
            }
            assertFalse(component.getKey().endsWith("Exact"), component.getKey());
            assertFalse(component.getKey().endsWith("Selection"), component.getKey());
        }
        var xml = mapper.valueToTree(schemas.get("Jzufi23AnyXMLContent"));
        assertEquals("string", xml.at("/properties/any/items/type").asText());
        var binary = mapper.valueToTree(schemas.get("Jzufi23BinaryObject"));
        assertEquals("string", binary.at("/properties/value/type").asText());
        assertEquals("byte", binary.at("/properties/value/format").asText());

        ObjectNode contract = mapper.valueToTree(
            new OpenAPI()
                .openapi("3.1.0")
                .info(new Info().title("JZuFi model regression").version("1"))
                .paths(new Paths())
                .components(new Components().schemas(schemas))
        );
        // Das Beispiel nur im Exportbaum ergänzen, nicht in den gemeinsam gelesenen Referenzschemas.
        ((ObjectNode) contract.at("/components/schemas/JzufiRelationPage23")).set(
            "example",
            mapper.readTree(relationExample())
        );
        var output = Path.of("target", "docs", "zustaendigkeit-openapi-3.1.json");
        Files.createDirectories(output.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), contract);
    }

    /**
     * Folgt den Java-Feldtypen und JAXB-Untertypen unabhängig vom Schemaexport.
     * Jeder erreichbare skalare oder Listen-Slot muss auf seine strukturelle Basis zeigen.
     */
    @Test
    void everyReachableBaseFieldReferencesItsStructuralModel() {
        var mapper = SwaggerCoreConfiguration.openApiObjectMapper();
        var bases = modelFamilies
            .keySet()
            .stream()
            .map(Class::getSimpleName)
            .collect(Collectors.toSet());
        var pending = new ArrayDeque<>(outputRoots());
        Set<Class<?>> visited = new LinkedHashSet<>();
        Set<String> selectedFamilies = new LinkedHashSet<>();
        while (!pending.isEmpty()) {
            var type = pending.removeFirst();
            if (
                !type.getPackageName().equals(Leistung.class.getPackageName()) || !visited.add(type)
            ) continue;
            if (type.getSuperclass() != null) pending.add(type.getSuperclass());
            var children = type.getDeclaredAnnotation(XmlSeeAlso.class);
            if (children != null) pending.addAll(List.of(children.value()));
            Map<String, Schema> holderSchemas = modelSchemas;
            for (var field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                var valueType = mapper.constructType(field.getGenericType());
                boolean list = valueType.isContainerType();
                if (list) valueType = valueType.getContentType();
                pending.add(valueType.getRawClass());
                String base = valueType.getRawClass().getSimpleName();
                if (!bases.contains(base)) continue;
                selectedFamilies.add(base);
                String holderName = "Jzufi23" + type.getSimpleName();
                // Manche abstrakten Eltern werden im Root-Export nur abgeflacht, nicht als Komponente aufgelöst.
                if (!holderSchemas.containsKey(holderName)) holderSchemas = converters(
                    true
                ).readAll(type);
                Schema<?> holder = holderSchemas.get(holderName);
                assertNotNull(holder, type.getName());
                var property = mapper
                    .getSerializationConfig()
                    .introspect(mapper.constructType(type))
                    .findProperties()
                    .stream()
                    .filter(candidate -> candidate.getInternalName().equals(field.getName()))
                    .findFirst()
                    .orElseThrow();
                Schema<?> slot = holder.getProperties().get(property.getName());
                assertNotNull(slot, field.toString());
                if (list) slot = slot.getItems();
                assertStructuralReference(slot, base, field.toString());
                if (type.getSimpleName().equals("Kategorie") && field.getName().equals("klasse")) {
                    assertEquals("Kategorieklasse", slot.getTitle());
                }
            }
        }
        assertEquals(bases, selectedFamilies);
    }

    /**
     * Prüft Pflichtfelder und Feldtypen an konkreten Schemas sowie ihre Einbettung in Basis-Slots.
     * Ein Discriminator-Mapping allein erzwingt im JSON-Schema keine Auswahl des konkreten Untertyps.
     */
    @Test
    void concreteSchemasValidateTheirOwnFieldsWithoutExclusiveDispatch() throws Exception {
        var schemas = modelSchemas;
        var families = modelFamilies;
        assertEquals(9, families.size());
        assertEquals(23, families.values().stream().mapToInt(Set::size).sum());
        var mixedTransfer = Json.mapper().createObjectNode();
        var responsibilities = mixedTransfer.putArray("zustaendigkeit");
        for (var family : families.entrySet()) {
            var baseValidator = validator(
                schemas,
                "Jzufi23" + family.getKey().getSimpleName(),
                true
            );
            for (var variant : family.getValue()) {
                String type = variant.getSimpleName();
                var validator = validator(schemas, "Jzufi23" + type, true);
                var payload = payload(type);
                assertValid(validator, payload);
                assertValid(baseValidator, payload);
                // Absichtlich gültig: Das Mapping beschränkt keine String-Werte; unbekannte Typen lehnen Bindingtests ab.
                assertValid(baseValidator, payload.deepCopy().put("@type", "unknown"));
                var missing = payload.deepCopy();
                missing.remove("@type");
                assertFalse(validator.validate(missing).isEmpty(), type);
                assertFalse(
                    validator.validate(payload.deepCopy().put("@type", 17)).isEmpty(),
                    type
                );
                assertFalse(
                    validator.validate(payload.deepCopy().putNull("@type")).isEmpty(),
                    type
                );
                String required = requiredField(type);
                if (required != null) {
                    missing = payload.deepCopy();
                    missing.remove(required);
                    assertFalse(validator.validate(missing).isEmpty(), type + "." + required);
                    assertFalse(
                        validator.validate(payload.deepCopy().put(required, 17)).isEmpty(),
                        type + "." + required + " type"
                    );
                }
                if (family.getKey() == de.xoev.xzufi.v2_3_1.Zustaendigkeit.class) {
                    responsibilities.add(payload);
                } else if (family.getKey() == de.xoev.xzufi.v2_3_1.Leistungsstruktur.class) {
                    assertValid(
                        validator(schemas, "Jzufi23Leistung/properties/struktur", true),
                        payload
                    );
                }
            }
        }
        var transferValidator = validator(schemas, "Jzufi23ZustaendigkeitTransferObjekt", true);
        assertValid(transferValidator, mixedTransfer);
        var missingType = mixedTransfer.deepCopy();
        ((ObjectNode) missingType.path("zustaendigkeit").get(0)).remove("@type");
        assertFalse(transferValidator.validate(missingType).isEmpty());
        var missingService = mixedTransfer.deepCopy();
        ((ObjectNode) missingService.path("zustaendigkeit").get(0)).remove("leistungID");
        assertFalse(transferValidator.validate(missingService).isEmpty());
        var incompleteDetail = payload("LeistungsstrukturObjektMitVerrichtungUndDetail");
        incompleteDetail.remove("verrichtungsdetail");
        // Die Basis kennt Untertyp-Pflichtfelder nicht; erst das konkrete Schema muss das fehlende Feld ablehnen.
        assertValid(validator(schemas, "Jzufi23Leistungsstruktur", true), incompleteDetail);
        assertFalse(
            validator(schemas, "Jzufi23LeistungsstrukturObjektMitVerrichtungUndDetail", true)
                .validate(incompleteDetail)
                .isEmpty()
        );
    }

    static Stream<Arguments> resolutionCases() {
        // Basis-, Untertyp-, mehrstufige und verschachtelte Einstiege decken unterschiedliche Resolverrisiken ab.
        return Stream.of(
            de.xoev.xzufi.v2_3_1.Zustaendigkeit.class,
            de.xoev.xzufi.v2_3_1.ZustaendigkeitOrganisationseinheit.class,
            de.xoev.xzufi.v2_3_1.LeistungsstrukturObjektMitVerrichtungUndDetail.class,
            de.xoev.xzufi.v2_3_1.FormulardateiXML.class,
            de.xoev.xzufi.v2_3_1.Formular.class,
            de.xoev.xzufi.v2_3_1.Kontaktperson.class
        ).flatMap(entry -> Stream.of(false, true).map(first -> Arguments.of(entry, first)));
    }

    /**
     * Vergleicht alternative Einstiege mit dem Root-Referenzgraphen.
     * Dabei zählt der effektive Feldvertrag, nicht die redundante Platzierung geerbter Felder durch swagger-core.
     */
    @ParameterizedTest(name = "OpenAPI 3.1, Einstieg: {0}, Einstieg zuerst: {1}")
    @MethodSource("resolutionCases")
    void resolutionOrdersPreserveInheritanceFieldsAndDiscriminators(Class<?> variant, boolean first)
        throws Exception {
        var converters = converters(true);
        var entry = local(variant);
        var order = new ArrayList<Class<?>>();
        if (first) order.add(entry);
        order.addAll(roots());
        if (!first) order.add(entry);
        var actual = schemas(converters, order);
        assertEquals(modelSchemas.keySet(), actual.keySet());
        // swagger-core variiert redundantes type: object und wiederholte Elternfelder je nach Einstieg.
        // Effektive Felder samt Constraints, Pflichtfelder, Vererbung und Typauswahl bleiben gleich.
        var mapper = schemaMapper(true);
        for (var component : modelSchemas.entrySet()) {
            var expected = mapper.valueToTree(component.getValue());
            var resolved = mapper.valueToTree(actual.get(component.getKey()));
            String name = component.getKey();
            assertEquals(
                mapper.valueToTree(
                    OpenApiSchemaTestSupport.propertySchemas(component.getValue(), modelSchemas)
                ),
                mapper.valueToTree(
                    OpenApiSchemaTestSupport.propertySchemas(actual.get(name), actual)
                ),
                name + ".properties"
            );
            for (String keyword : List.of("required", "allOf", "discriminator", "oneOf", "anyOf")) {
                assertEquals(expected.path(keyword), resolved.path(keyword), name + "." + keyword);
            }
        }
    }

    @Test
    void legacyAndLocalSchemasKeepSeparateContracts() {
        var schemas = schemas(
            converters(true),
            List.of(RelationPage22.class, RelationPage23.class, JzufiRelationPage23.class)
        );
        for (String name : List.of("Zustaendigkeit", "de.xoev.xzufi.v2_3_1.Zustaendigkeit")) {
            Schema<?> legacy = schemas.get(name);
            assertNotNull(legacy, name);
            assertNull(legacy.getDiscriminator(), name);
            assertFalse(properties(legacy, schemas).contains("@type"), name);
        }
        Schema<?> local = schemas.get("Jzufi23Zustaendigkeit");
        assertEquals("@type", local.getDiscriminator().getPropertyName());
        Schema<?> page = schemas.get("JzufiRelationPage23");
        assertEquals(
            REF + "Jzufi23ZustaendigkeitTransferObjekt",
            page.getProperties().get("content").getItems().get$ref()
        );
    }

    /**
     * XML-Datum und reine Uhrzeit bleiben wegen optionaler Offsets beziehungsweise fehlender Datumsteile Strings.
     * Nur echte Zeitstempel erhalten das für Clientgeneratoren relevante date-time-Format.
     */
    @Test
    void temporalSchemasMatchTheJsonRepresentation() {
        var schemas = modelSchemas;
        var mapper = schemaMapper(true);
        for (String type : List.of("Zeitraum", "Zeitserie")) {
            Schema<?> temporal = schemas.get("Jzufi23" + type);
            for (String property : List.of("beginn", "ende")) {
                Schema<?> field = temporal.getProperties().get(property);
                assertEquals("string", mapper.valueToTree(field).path("type").asText());
                assertNull(field.getFormat(), type + "." + property);
            }
        }
        Schema<?> version = schemas.get("Jzufi23Versionsinformation");
        Schema<?> timestamp = version.getProperties().get("erstelltDatumZeit");
        assertEquals("date-time", timestamp.getFormat());
    }

    /** Prüft Typregistrierung und unmittelbare Vererbung, ohne die Basen durch exklusive Typ-Patterns einzuschränken. */
    private void assertStructuralFamilies(Map<String, Schema> schemas) throws Exception {
        for (var family : modelFamilies.entrySet()) {
            String baseName = "Jzufi23" + family.getKey().getSimpleName();
            Schema<?> base = schemas.get(baseName);
            assertNotNull(base, baseName);
            assertEquals("@type", base.getDiscriminator().getPropertyName());
            assertTrue(base.getRequired().contains("@type"), baseName);
            Map<String, String> expected = new LinkedHashMap<>();
            for (var variant : family.getValue()) {
                String name = "Jzufi23" + variant.getSimpleName();
                expected.put(variant.getSimpleName(), REF + name);
                Schema<?> schema = schemas.get(name);
                assertNotNull(schema, name);
                if (variant != family.getKey()) {
                    String parent = REF + "Jzufi23" + variant.getSuperclass().getSimpleName();
                    assertNotNull(schema.getAllOf(), name);
                    assertTrue(
                        schema
                            .getAllOf()
                            .stream()
                            .anyMatch(part -> parent.equals(part.get$ref())),
                        name
                    );
                }
                for (
                    Class<?> model = local(variant);
                    model != null && model.getPackageName().equals(Leistung.class.getPackageName());
                    model = model.getSuperclass()
                ) {
                    Schema<?> structural = schemas.get("Jzufi23" + model.getSimpleName());
                    assertNotNull(structural, model.getName());
                    assertNull(structural.getOneOf(), model.getName());
                    if (
                        structural.getProperties() != null &&
                        structural.getProperties().containsKey("@type")
                    ) {
                        assertNull(
                            structural.getProperties().get("@type").getPattern(),
                            model.getName()
                        );
                        assertNull(
                            structural.getProperties().get("@type").getEnum(),
                            model.getName()
                        );
                    }
                }
            }
            assertEquals(expected, base.getDiscriminator().getMapping(), baseName);
        }
    }

    /** Akzeptiert auch einen allOf-Referenzwrapper, mit dem Swagger zusätzliche Feldmetadaten wie Titel erhält. */
    private static void assertStructuralReference(Schema<?> slot, String base, String field) {
        assertNotNull(slot, field);
        String name = "Jzufi23" + base;
        if (slot.get$ref() != null) {
            assertEquals(REF + name, slot.get$ref(), field);
        } else {
            assertNotNull(slot.getAllOf(), field);
            assertEquals(1, slot.getAllOf().size(), field);
            assertEquals(REF + name, slot.getAllOf().getFirst().get$ref(), field);
        }
    }

    private static Set<String> modelProperties(Class<?> type) {
        var mapper = SwaggerCoreConfiguration.openApiObjectMapper();
        return mapper
            .getSerializationConfig()
            .introspect(mapper.constructType(type))
            .findProperties()
            .stream()
            .map(BeanPropertyDefinition::getName)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Ordnet erreichbare lokale Java-Typen ihren Swagger-Komponentennamen zu, statt Erwartungen aus Schemas abzuleiten. */
    private static Map<String, Class<?>> modelTypes() {
        var mapper = SwaggerCoreConfiguration.openApiObjectMapper();
        var names = new CustomTypeNameResolver(true);
        Map<String, Class<?>> models = new LinkedHashMap<>();
        Set<Class<?>> visited = new LinkedHashSet<>();
        var queue = new ArrayDeque<>(roots());
        while (!queue.isEmpty()) {
            var model = queue.removeFirst();
            if (
                (!model.getPackageName().startsWith("de.dataport.suchdienst.jzufi.model.v23") &&
                    model != JzufiRelationPage23.class) ||
                !visited.add(model)
            ) continue;
            models.put(names.nameForType(mapper.constructType(model)), model);
            queue.addAll(Arrays.asList(model.getDeclaredClasses()));
            if (model.getSuperclass() != null) queue.add(model.getSuperclass());
            var children = model.getDeclaredAnnotation(XmlSeeAlso.class);
            if (children != null) queue.addAll(List.of(children.value()));
            for (var property : mapper
                .getSerializationConfig()
                .introspect(mapper.constructType(model))
                .findProperties()) {
                var type = property.getPrimaryType();
                // Container und JAXB-Elementwrapper sind keine Modellknoten; relevant ist ihr eigentlicher Werttyp.
                while (
                    type != null &&
                    (type.isContainerType() || type.getRawClass() == JAXBElement.class)
                ) {
                    type = type.isContainerType() ? type.getContentType() : type.containedType(0);
                }
                if (type != null) queue.add(type.getRawClass());
            }
        }
        return models;
    }

    /** Handgeschriebene Schema-Payloads mit Basis-/Untertypfeldern, nicht zwingend vollständige fachliche XSD-Fixtures. */
    private static ObjectNode payload(String type) throws Exception {
        String fields = switch (type) {
            case
                "Zustaendigkeit",
                "ZustaendigkeitOrganisationseinheit",
                "ZustaendigkeitOnlinedienst",
                "ZustaendigkeitPerson" -> "\"leistungID\":[{\"value\":\"lb\",\"schemeAgencyID\":\"test\"}],\"gebietID\":[{\"value\":\"gebiet\",\"schemeAgencyID\":\"test\"}]" +
                switch (type) {
                    case "ZustaendigkeitOrganisationseinheit" -> ",\"rolle\":[{\"code\":\"02\"}],\"kennzeichenTerminvereinbarungVerpflichtend\":true";
                    case "ZustaendigkeitOnlinedienst" -> ",\"organisationseinheit\":[{\"organisationseinheitID\":{\"value\":\"oe\"},\"rolle\":[{\"code\":\"02\"}]}]";
                    case "ZustaendigkeitPerson" -> ",\"kriterium\":[{\"codeKriterium\":{\"code\":{\"code\":\"01\"}},\"auspraegung\":[\"A-Z\"]}]";
                    default -> "";
                };
            case
                "LeistungsstrukturObjekt",
                "LeistungsstrukturObjektMitVerrichtung",
                "LeistungsstrukturObjektMitVerrichtungUndDetail" -> "\"leistungsgruppierungLeiKa\":{\"code\":\"100\"}" +
                (type.equals("LeistungsstrukturObjekt")
                    ? ""
                    : ",\"leistungsobjektID\":{\"value\":\"object\"},\"verrichtungLeiKa\":{\"code\":\"001\"}") +
                (type.endsWith("UndDetail")
                    ? ",\"verrichtungsdetail\":[{\"value\":\"detail\"}]"
                    : "");
            case
                "Anschrift",
                "AnschriftOrganisationseinheit" -> "\"typ\":{\"code\":\"01\"},\"postleitzahl\":\"12345\",\"ort\":\"Teststadt\",\"strasse\":\"Testweg\"" +
                (type.equals("AnschriftOrganisationseinheit")
                    ? ",\"infoParkplatz\":[{\"value\":\"parking\"}]"
                    : "");
            case
                "Kontaktperson",
                "KontaktpersonErweitert" -> "\"id\":{\"value\":\"contact\"},\"vorname\":\"Test\"" +
                (type.equals("KontaktpersonErweitert")
                    ? ",\"organisationseinheitID\":[{\"value\":\"oe\"}]"
                    : "");
            case
                "Kategorieklasse",
                "Gebietsklasse" -> "\"id\":{\"value\":\"category\"},\"typ\":{\"code\":\"01\"},\"bezeichnung\":[{\"value\":\"Category\"}]" +
                (type.equals("Gebietsklasse") ? ",\"kurzbezeichnung\":[{\"value\":\"area\"}]" : "");
            case "Sprache", "Sprachversion" -> "\"languageCode\":\"de\"" +
                (type.equals("Sprachversion")
                    ? ",\"version\":\"2\",\"kennzeichenMaschinelleUebersetzung\":true"
                    : "");
            case
                "CodeType",
                "CodeWithAuthorityType" -> "\"value\":\"code\",\"codeSpace\":\"urn:test:codes\"";
            case "FormulardateiLink" -> "\"link\":{\"uri\":\"https://example.invalid/form\"}";
            case "FormulardateiXML" -> "\"xmldaten\":{\"any\":[\"<form xmlns=\\\"urn:test\\\">payload</form>\"]}";
            case "FormulardateiBinaer" -> "\"binaerdaten\":{\"value\":\"AAECf/8=\",\"filename\":\"form.bin\",\"mimeCode\":\"application/octet-stream\"}";
            case
                "Onlinedienst",
                "OnlinedienstErweitert",
                "OnlinedienstVorlage" -> "\"id\":{\"value\":\"service\"},\"bezeichnung\":[{\"value\":\"Service\"}],\"status\":{\"code\":\"01\"}," +
                "\"vertrauensniveau\":{\"code\":\"10\"},\"zahlungsweisen\":{\"kostenfrei\":true}," +
                "\"sprachversion\":[{\"@type\":\"Sprachversion\",\"languageCode\":\"de\"}]" +
                (type.equals("OnlinedienstVorlage")
                    ? ",\"infoEinfuehrung\":\"introduction\""
                    : ",\"kennzeichenOeffentlichAnzeigen\":true");
            default -> throw new IllegalArgumentException("Fehlende Vertrags-Fixture: " + type);
        };
        return (ObjectNode) Json.mapper().readTree("{\"@type\":\"" + type + "\"," + fields + "}");
    }

    /** Wählt unabhängig vom geprüften Schema ein repräsentatives Pflichtfeld für Negativfälle, keine vollständige Liste. */
    private static String requiredField(String type) {
        return switch (type) {
            case "LeistungsstrukturObjekt" -> "leistungsgruppierungLeiKa";
            case "LeistungsstrukturObjektMitVerrichtung" -> "verrichtungLeiKa";
            case "LeistungsstrukturObjektMitVerrichtungUndDetail" -> "verrichtungsdetail";
            case "ZustaendigkeitOrganisationseinheit" -> "rolle";
            case
                "Zustaendigkeit",
                "ZustaendigkeitOnlinedienst",
                "ZustaendigkeitPerson" -> "leistungID";
            case "FormulardateiLink" -> "link";
            case "FormulardateiXML" -> "xmldaten";
            case "FormulardateiBinaer" -> "binaerdaten";
            case "Anschrift", "AnschriftOrganisationseinheit" -> "ort";
            case
                "Kontaktperson",
                "KontaktpersonErweitert",
                "Kategorieklasse",
                "Gebietsklasse",
                "Onlinedienst",
                "OnlinedienstErweitert",
                "OnlinedienstVorlage" -> "id";
            case "Sprache", "Sprachversion" -> "languageCode";
            case "CodeType", "CodeWithAuthorityType" -> null;
            default -> throw new IllegalArgumentException("Fehlende Pflichtfeldprüfung: " + type);
        };
    }

    private static void assertValid(com.networknt.schema.Schema validator, ObjectNode payload) {
        var errors = validator.validate(payload);
        assertTrue(errors.isEmpty(), () -> payload.path("@type") + ": " + errors);
    }

    /** Übernimmt das dokumentierte v2-Antwortbeispiel für Clienttests des begrenzten Modellexports. */
    private static String relationExample() {
        var method = Arrays.stream(ZustaendigkeitJzufiController.class.getDeclaredMethods())
            .filter(candidate -> candidate.getName().equals("getZsTrByLbIdAndArs231V2"))
            .findFirst()
            .orElseThrow();
        return Arrays.stream(method.getAnnotationsByType(ApiResponse.class))
            .filter(response -> response.responseCode().equals("200"))
            .findFirst()
            .orElseThrow()
            .content()[0]
            .examples()[0]
            .value();
    }
}
