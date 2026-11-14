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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dataport.suchdienst.suchen.model.out.leistung.detail.AbstractServiceModule;
import de.dataport.suchdienst.suchen.model.out.leistung.detail.ServiceDescriptionDetail;
import de.dataport.suchdienst.suchen.model.out.leistung.detail.ServiceModule;
import de.dataport.suchdienst.suchen.model.out.leistung.detail.ServiceModuleDate;
import de.dataport.suchdienst.suchen.model.out.leistung.detail.ServiceModuleDetail;
import io.swagger.v3.oas.models.media.Schema;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testziel: Alle Leistungsdetail-Module bleiben mit Springdocs Polymorphiekonverter
 * mit Discriminator, unmittelbarer allOf-Basis und sämtlichen Modellfeldern erhalten.
 * Exakte Typ-Patterns machen die vollständige oneOf-Auswahl gegenseitig ausschließend.
 */
class ServiceModuleOpenApiSchemaTest {

    private static final String REF = "#/components/schemas/";

    /** Je OAS-Version einmal Root-zuerst sowie je ein Basis-/Untertyp-zuerst-Fall, ohne doppelte Root-Permutationen. */
    static Stream<Arguments> resolutionCases() {
        return Stream.of(false, true).flatMap(openapi31 ->
            Stream.of(
                AbstractServiceModule.class,
                ServiceModule.class,
                ServiceModuleDetail.class,
                ServiceModuleDate.class,
                ServiceDescriptionDetail.class
            ).map(entry -> Arguments.of(openapi31, entry))
        );
    }

    /**
     * Sichert die Modellannotationen mit Springdocs tatsächlichem Polymorphiekonverter ab:
     * unmittelbare Vererbung ohne Feldverlust und vollständige Modulauswahl am details-Slot.
     */
    @ParameterizedTest(name = "OpenAPI 3.1: {0}, erster Einstieg: {1}")
    @MethodSource("resolutionCases")
    void preservesEveryModuleAcrossResolutionOrders(boolean openapi31, Class<?> entry) {
        var converters = OpenApiSchemaTestSupport.springdocConverters(openapi31);
        var schemas = OpenApiSchemaTestSupport.schemas(
            converters,
            entry == ServiceDescriptionDetail.class
                ? List.of(entry)
                : List.of(entry, ServiceDescriptionDetail.class)
        );
        Schema<?> base = schemas.get("AbstractServiceModule");
        assertNotNull(base);
        assertEquals("@type", base.getDiscriminator().getPropertyName());
        assertTrue(base.getRequired().contains("@type"));
        assertEquals(
            Map.of(
                "_module",
                REF + "ServiceModule",
                "_moduleDetail",
                REF + "ServiceModuleDetail",
                "_moduleDate",
                REF + "ServiceModuleDate"
            ),
            base.getDiscriminator().getMapping()
        );
        assertNull(base.getOneOf());
        for (var type : List.of(
            ServiceModule.class,
            ServiceModuleDetail.class,
            ServiceModuleDate.class
        )) {
            Schema<?> child = schemas.get(type.getSimpleName());
            assertNotNull(child, type.getSimpleName());
            assertEquals(1, child.getAllOf().size());
            assertEquals(REF + "AbstractServiceModule", child.getAllOf().getFirst().get$ref());
            String discriminator = switch (type.getSimpleName()) {
                case "ServiceModule" -> "_module";
                case "ServiceModuleDetail" -> "_moduleDetail";
                default -> "_moduleDate";
            };
            assertEquals(
                "^" + discriminator + "$(?![\\s\\S])",
                child.getProperties().get("@type").getPattern()
            );
            assertNull(child.getProperties().get("@type").getEnum());
            assertTrue(child.getRequired().contains("@type"));
            var mapper = SwaggerCoreConfiguration.openApiObjectMapper();
            Set<String> expected = new LinkedHashSet<>();
            mapper
                .getSerializationConfig()
                .introspect(mapper.constructType(type))
                .findProperties()
                .forEach(property -> expected.add(property.getName()));
            Set<String> actual = new LinkedHashSet<>(base.getProperties().keySet());
            actual.addAll(child.getProperties().keySet());
            assertEquals(expected, actual, type.getSimpleName());
        }
        Schema<?> detail = schemas.get("ServiceDescriptionDetail");
        Schema<?> items = detail.getProperties().get("details").getItems();
        assertNull(items.get$ref());
        assertFalse(schemas.containsKey("ServiceDescriptionDetailModule"));
        assertEquals(
            Set.of(REF + "ServiceModule", REF + "ServiceModuleDetail", REF + "ServiceModuleDate"),
            new LinkedHashSet<>(items.getOneOf().stream().map(Schema::get$ref).toList())
        );
        assertNull(items.getAnyOf());
        Schema<?> date = schemas.get("ServiceModuleDate");
        assertEquals("date", date.getProperties().get("date").getFormat());
    }

    /** Prüft ohne Discriminator-Sonderlogik, dass jede Kennung exakt einen Zweig erfüllt. */
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void validatesExclusiveModulePayloads(boolean openapi31) {
        var schemas = OpenApiSchemaTestSupport.springdocConverters(openapi31).readAll(
            ServiceDescriptionDetail.class
        );
        var mapper = OpenApiSchemaTestSupport.schemaMapper(openapi31);
        var response = mapper
            .createObjectNode()
            .put("id", "test-service")
            .put("name", "Testleistung")
            .put("lastUpdate", "2026-10-23T08:00:00Z");
        response
            .putArray("details")
            .addObject()
            .put("@type", "_moduleDate")
            .put("title", "Fachliche Freigabe")
            .put("code", "35")
            .put("accessChannel", "ONLINE_OFFLINE")
            .put("date", "2026-10-23");
        var validator = OpenApiSchemaTestSupport.validator(
            schemas,
            "ServiceDescriptionDetail",
            openapi31
        );
        var module = (com.fasterxml.jackson.databind.node.ObjectNode) response
            .path("details")
            .get(0);
        var typeNames = Map.of(
            "_module",
            "ServiceModule",
            "_moduleDetail",
            "ServiceModuleDetail",
            "_moduleDate",
            "ServiceModuleDate"
        );
        // Alle übrigen Felder bleiben gleich; nur die Kennung darf den passenden oneOf-Zweig bestimmen.
        for (var variant : typeNames.entrySet()) {
            module.put("@type", variant.getKey());
            var errors = validator.validate(response);
            assertTrue(errors.isEmpty(), errors.toString());
            int matches = 0;
            for (String schemaName : typeNames.values()) {
                var subtypeValidator = OpenApiSchemaTestSupport.validator(
                    schemas,
                    schemaName,
                    openapi31
                );
                if (subtypeValidator.validate(module).isEmpty()) {
                    matches++;
                    assertEquals(variant.getValue(), schemaName);
                }
            }
            assertEquals(1, matches);
        }
        module.put("@type", "unknown-module");
        assertFalse(validator.validate(response).isEmpty());
        // Ein abschließender Zeilenumbruch darf das exakte Typ-Pattern nicht umgehen.
        for (String typeName : typeNames.keySet()) {
            module.put("@type", typeName + "\n");
            assertFalse(validator.validate(response).isEmpty());
        }
        module.remove("@type");
        assertFalse(validator.validate(response).isEmpty());
        module.put("@type", "_moduleDate").put("date", "not-a-date");
        assertFalse(validator.validate(response).isEmpty());
        module.put("date", 17);
        assertFalse(validator.validate(response).isEmpty());
    }
}
