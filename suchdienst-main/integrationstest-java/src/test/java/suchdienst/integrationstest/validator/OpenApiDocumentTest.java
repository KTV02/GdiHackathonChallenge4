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
package suchdienst.integrationstest.validator;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Testziel: OpenAPI-3.1-Discriminatoren dienen als Dispatch-Metadaten, nicht als
 * zusätzliche Schema-Constraints. Basisreferenzen validieren Basisfelder; konkrete
 * Untertypschemas validieren über allOf zusätzlich ihre eigenen Pflichtfelder.
 */
class OpenApiDocumentTest {

    @Test
    void validatesStructuralInheritanceWithoutImplicitSubtypeDispatch() {
        var mapper = JsonMapper.builder().build();
        var apiDoc = new OpenApiDocument(
            mapper.readTree(
                """
                {
                  "openapi": "3.1.0",
                  "components": {"schemas": {
                    "Base": {
                      "type": "object",
                      "required": ["@type", "id"],
                      "properties": {"@type": {"type": "string"}, "id": {"type": "string"}},
                      "discriminator": {
                        "propertyName": "@type",
                        "mapping": {"Base": "#/components/schemas/Base", "Foo": "#/components/schemas/Foo"}
                      }
                    },
                    "Foo": {
                      "allOf": [{"$ref": "#/components/schemas/Base"}],
                      "required": ["text"],
                      "properties": {"text": {"type": "string"}}
                    }
                  }}
                }
                """
            )
        );
        String base = OpenApiDocument.pointer("components", "schemas", "Base");
        String child = OpenApiDocument.pointer("components", "schemas", "Foo");
        var value = mapper.createObjectNode().put("@type", "Foo").put("id", "id").put("text", "content");
        apiDoc.validateSchema(base, value);
        apiDoc.validateSchema(child, value);
        apiDoc.validateSchema(base, value.deepCopy().put("@type", "unknown"));
        assertThrows(AssertionError.class, () -> apiDoc.validateSchema(child, value.deepCopy().put("text", 17)));
        value.remove("text");
        apiDoc.validateSchema(base, value);
        assertThrows(AssertionError.class, () -> apiDoc.validateSchema(child, value));
        value.remove("id");
        assertThrows(AssertionError.class, () -> apiDoc.validateSchema(base, value));
        value.put("id", "id").remove("@type");
        assertThrows(AssertionError.class, () -> apiDoc.validateSchema(base, value));
    }
}
