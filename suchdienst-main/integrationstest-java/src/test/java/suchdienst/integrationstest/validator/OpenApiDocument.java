/*-
 * ---license-start
 * Suchdienst Integrationstests
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package suchdienst.integrationstest.validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.dialect.Dialect;
import com.networknt.schema.dialect.Dialects;
import com.networknt.schema.keyword.NonValidationKeyword;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/** Validiert Schemas im ursprünglichen Dokument, einschließlich rekursiver $refs. */
public final class OpenApiDocument {

    private static final String DOCUMENT_URI = "https://suchdienst.test/openapi.json";
    private final JsonNode document;
    private final SchemaRegistry registry;

    public OpenApiDocument(JsonNode document) {
        assertNotNull(document, "OpenAPI-Dokument fehlt.");
        this.document = document.deepCopy();
        String version = document.path("openapi").asString("");
        assertTrue(version.startsWith("3.0.") || version.startsWith("3.1."), "Unbekannte OpenAPI-Version: " + version);
        var dialect = Dialect.builder(version.startsWith("3.0.") ? Dialects.getOpenApi30() : Dialects.getOpenApi31())
            // Discriminator ist Binding-Metadatum; oneOf/allOf und Typbedingungen validieren die Auswahl.
            .keyword(new NonValidationKeyword("discriminator"))
            .build();
        registry = SchemaRegistry.withDialect(dialect, builder ->
            builder
                .schemas(Map.of(DOCUMENT_URI, this.document.toString()))
                .schemaRegistryConfig(SchemaRegistryConfig.builder().formatAssertionsEnabled(true).build())
        );
    }

    public static String pointer(String... segments) {
        StringBuilder pointer = new StringBuilder();
        for (String segment : segments) {
            pointer.append('/').append(segment.replace("~", "~0").replace("/", "~1"));
        }
        return pointer.toString();
    }

    /** Löst Reference Objects (Response, Header, Example) auf, ohne das Dokument zu verändern. */
    public String resolvePointer(String pointer) {
        var visited = new HashSet<String>();
        while (document.at(pointer).has("$ref")) {
            assertTrue(visited.add(pointer), "Zyklisches Reference Object: " + pointer);
            String ref = document.at(pointer).path("$ref").asString("");
            assertTrue(ref.startsWith("#/"), "Externes Reference Object wird nicht unterstützt: " + ref);
            pointer = ref.substring(1);
        }
        assertFalse(document.at(pointer).isMissingNode(), "OpenAPI-Eintrag fehlt: " + pointer);
        return pointer;
    }

    public JsonNode node(String pointer) {
        return document.at(resolvePointer(pointer));
    }

    public void validateSchema(String pointer, JsonNode value) {
        JsonNode schema = document.at(pointer);
        assertTrue(schema.isObject() || schema.isBoolean(), "Schema fehlt: " + pointer);
        var errors = registry.getSchema(SchemaLocation.of(DOCUMENT_URI + "#" + pointer)).validate(value);
        assertTrue(
            errors.isEmpty(),
            () -> "Die Struktur entspricht nicht dem OpenAPI-Schema " + pointer + ": " + errors
        );
    }

    public List<JsonNode> examples(String mediaPointer) {
        List<JsonNode> examples = new ArrayList<>();
        JsonNode media = node(mediaPointer);
        if (media.has("example")) {
            examples.add(media.get("example"));
        }
        if (media.path("examples").isObject()) {
            for (String name : media.get("examples").propertyNames()) {
                JsonNode example = node(mediaPointer + pointer("examples", name));
                assertTrue(example.has("value"), "Beispiel ohne prüfbaren value: " + name);
                examples.add(example.get("value"));
            }
        }
        if (examples.isEmpty()) {
            JsonNode schema = node(mediaPointer + "/schema");
            if (schema.has("example")) {
                examples.add(schema.get("example"));
            }
            if (schema.path("examples").isArray()) {
                schema.get("examples").forEach(examples::add);
            }
        }
        return examples;
    }
}
