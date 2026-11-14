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
package suchdienst.integrationstest.validator.jzufi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.restassured.builder.ResponseBuilder;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import suchdienst.integrationstest.validator.OpenApiDocument;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Test-Ziel: Die Antwortvalidierung nach dem Wechsel von openapi4j auf Jackson 3 und NetworkNT absichern,
 * damit durch den Austausch des Validators keine Prüfungen unbemerkt verloren gehen.
 * Gültige und gezielt fehlerhafte Antworten werden gegen künstliche OpenAPI-Spezifikationen geprüft,
 * unabhängig von einem laufenden Backend und bekannten Fehlern in dessen Schemas.
 * Geprüft werden Statuscodes, Medientypen, Header sowie Body-Schemas einschließlich Pflichtfeldern,
 * Datentypen, Formaten, rekursiven Referenzen und Schema-Kombinationen.
 * Diese Regressionstests ergänzen die bestehenden Jzufi-Integrationstests, ersetzen sie aber nicht.
 */
class OpenApiSchemaValidatorTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final String CONTENT_TYPE = "application/json;xzufi-version=2.2.0";
    private static final String VALID_BODY = "{\"id\":1,\"date\":\"2026-10-12\"}";

    private static ObjectNode specification() {
        return (ObjectNode) MAPPER.readTree(
            """
            {
              "openapi": "3.1.0",
              "paths": {
                "/test/{id}": {"get": {"responses": {
                  "200": {"$ref": "#/components/responses/Success"},
                  "204": {"description": "No content"}
                }}}
              },
              "components": {
                "responses": {"Success": {
                  "description": "Success",
                  "headers": {
                    "X-Count": {"$ref": "#/components/headers/Count"},
                    "X-Tags": {"schema": {"type": "array", "items": {"type": "integer"}}},
                    "X-Enabled": {"schema": {"type": "boolean"}},
                    "X-Language": {"schema": {"type": "string", "enum": ["de", "en"]}}
                  },
                  "content": {
                    "application/json;xzufi-version=2.2.0": {
                      "schema": {"$ref": "#/components/schemas/Node"},
                      "examples": {"referenced": {"$ref": "#/components/examples/Node"}}
                    },
                    "application/json;xzufi-version=2.3.1": {
                      "schema": {"type": "object", "required": ["name"],
                                 "properties": {"name": {"type": "string"}}}
                    }
                  }
                }},
                "headers": {"Count": {"required": true, "schema": {"type": "integer", "minimum": 1}}},
                "examples": {"Node": {"value": {"id": 1}}},
                "schemas": {"Node": {
                  "type": "object",
                  "required": ["id"],
                  "properties": {
                    "id": {"type": "integer", "minimum": 1},
                    "date": {"type": "string", "format": "date"},
                    "standardZustaendigkeitsgebiet": {"$ref": "#/components/schemas/Node"},
                    "children": {"type": "array", "items": {"$ref": "#/components/schemas/Node"}},
                    "choice": {"oneOf": [{"type": "number"}, {"type": "integer"}]},
                    "nullable": {"type": ["string", "null"]}
                  }
                }}
              }
            }
            """
        );
    }

    private static Response response(int status, String type, String body, String... extraHeaders) {
        var headers = new ArrayList<Header>();
        if (type != null) {
            headers.add(new Header("Content-Type", type));
        }
        for (int i = 0; i < extraHeaders.length; i += 2) {
            headers.add(new Header(extraHeaders[i], extraHeaders[i + 1]));
        }
        var builder = new ResponseBuilder().setStatusCode(status).setBody(body).setHeaders(new Headers(headers));
        if (type != null) {
            builder.setContentType(type);
        }
        return builder.build();
    }

    private static Response response(String body) {
        return response(200, CONTENT_TYPE, body, "x-count", "2");
    }

    @Test
    void validatesBothVersionsWithoutMutatingSpecification() {
        JsonNode spec = specification();
        JsonNode before = spec.deepCopy();
        var validator = new OpenApiSchemaValidator(spec);
        assertDoesNotThrow(() -> validator.validateResponse(response(VALID_BODY), "/test/{id}", "GET"));
        assertDoesNotThrow(() ->
            validator.validateResponse(
                response(
                    200,
                    "application/json; charset=UTF-8; xzufi-version=2.3.1",
                    "{\"name\":\"test\"}",
                    "X-Count",
                    "1"
                ),
                "/test/{id}",
                "get"
            )
        );
        assertThrows(AssertionError.class, () ->
            validator.validateResponse(
                response(200, "application/json;xzufi-version=2.3.1", VALID_BODY, "X-Count", "1"),
                "/test/{id}",
                "get"
            )
        );
        assertDoesNotThrow(() -> validator.validateResponse(response(VALID_BODY), "/test/{id}", "get"));
        assertEquals(before, spec);
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "{}",
            "{\"id\":\"1\"}",
            "{\"id\":null}",
            "{\"id\":0}",
            "{\"id\":1,\"date\":\"2026-99-99\"}",
            "{\"id\":1,\"children\":[{\"id\":\"wrong\"}]}",
            "{\"id\":1,\"standardZustaendigkeitsgebiet\":{}}",
            "{\"id\":1,\"choice\":1}",
        }
    )
    void rejectsSchemaViolations(String body) {
        var error = assertThrows(AssertionError.class, () ->
            new OpenApiSchemaValidator(specification()).validateResponse(response(body), "/test/{id}", "get")
        );
        assertTrue(error.getMessage().contains("Die Struktur entspricht nicht"), error.getMessage());
    }

    @Test
    void validatesRecursiveSchemasAndNullableProperties() {
        assertDoesNotThrow(() ->
            new OpenApiSchemaValidator(specification()).validateResponse(
                response("{\"id\":1,\"children\":[{\"id\":2}],\"nullable\":null,\"choice\":1.5}"),
                "/test/{id}",
                "get"
            )
        );
    }

    @Test
    void rejectsMalformedOrMissingJson() {
        var validator = new OpenApiSchemaValidator(specification());
        assertThrows(RuntimeException.class, () -> validator.validateResponse(response("{"), "/test/{id}", "get"));
        assertThrows(AssertionError.class, () -> validator.validateResponse(response(""), "/test/{id}", "get"));
    }

    @Test
    void validatesRequiredTypedAndCaseInsensitiveHeaders() {
        var validator = new OpenApiSchemaValidator(specification());
        assertDoesNotThrow(() ->
            validator.validateResponse(
                response(
                    200,
                    CONTENT_TYPE,
                    VALID_BODY,
                    "x-count",
                    "2",
                    "X-Tags",
                    "1,2",
                    "X-Enabled",
                    "true",
                    "X-Language",
                    "de"
                ),
                "/test/{id}",
                "get"
            )
        );
        assertThrows(AssertionError.class, () ->
            validator.validateResponse(response(200, CONTENT_TYPE, VALID_BODY), "/test/{id}", "get")
        );
        assertThrows(AssertionError.class, () ->
            validator.validateResponse(response(200, CONTENT_TYPE, VALID_BODY, "X-Count", "0"), "/test/{id}", "get")
        );
        assertThrows(AssertionError.class, () ->
            validator.validateResponse(
                response(200, CONTENT_TYPE, VALID_BODY, "X-Count", "1", "X-Language", "fr"),
                "/test/{id}",
                "get"
            )
        );
        assertThrows(AssertionError.class, () ->
            validator.validateResponse(
                response(200, CONTENT_TYPE, VALID_BODY, "X-Count", "1", "X-Tags", "1,true"),
                "/test/{id}",
                "get"
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = { "application/json", "application/json;xzufi-version=9", "text/plain", "" })
    void rejectsUndocumentedMediaTypes(String type) {
        assertThrows(AssertionError.class, () ->
            new OpenApiSchemaValidator(specification()).validateResponse(
                response(200, type, VALID_BODY, "X-Count", "1"),
                "/test/{id}",
                "get"
            )
        );
    }

    @Test
    void rejectsUndocumentedStatusPathAndMethod() {
        var validator = new OpenApiSchemaValidator(specification());
        assertThrows(AssertionError.class, () ->
            validator.validateResponse(response(500, CONTENT_TYPE, VALID_BODY), "/test/{id}", "get")
        );
        assertThrows(AssertionError.class, () -> validator.validateResponse(response(VALID_BODY), "/missing", "get"));
        assertThrows(AssertionError.class, () ->
            validator.validateResponse(response(VALID_BODY), "/test/{id}", "post")
        );
    }

    @Test
    void validatesStatusRangesDefaultAndBodylessResponses() {
        ObjectNode spec = specification();
        ObjectNode responses = (ObjectNode) spec.at("/paths/~1test~1{id}/get/responses");
        responses.set("2XX", MAPPER.readTree("{\"$ref\":\"#/components/responses/Success\"}"));
        responses.set("default", MAPPER.readTree("{\"description\":\"fallback\"}"));
        var validator = new OpenApiSchemaValidator(spec);
        assertDoesNotThrow(() ->
            validator.validateResponse(response(201, CONTENT_TYPE, VALID_BODY, "X-Count", "1"), "/test/{id}", "get")
        );
        assertDoesNotThrow(() -> validator.validateResponse(response(500, null, ""), "/test/{id}", "get"));
        assertDoesNotThrow(() -> validator.validateResponse(response(204, null, ""), "/test/{id}", "get"));
        assertThrows(AssertionError.class, () ->
            validator.validateResponse(response(204, null, "{}"), "/test/{id}", "get")
        );
    }

    @Test
    void resolvesReferencedAndSchemaExamplesWithJackson3() {
        ObjectNode spec = specification();
        String mediaPointer = OpenApiDocument.pointer("components", "responses", "Success", "content", CONTENT_TYPE);
        var document = new OpenApiDocument(spec);
        assertEquals(1, document.examples(mediaPointer).size());
        document.validateSchema(mediaPointer + "/schema", document.examples(mediaPointer).getFirst());
        ((ObjectNode) spec.at(mediaPointer)).remove("examples");
        ((ObjectNode) spec.at("/components/schemas/Node")).set("examples", MAPPER.readTree("[{\"id\":2},{\"id\":3}]"));
        assertEquals(2, new OpenApiDocument(spec).examples(mediaPointer).size());
    }

    @Test
    void validatesAllOfAnyOfAndAdditionalProperties() {
        ObjectNode spec = specification();
        ((ObjectNode) spec.at("/components/schemas")).set(
            "Combined",
            MAPPER.readTree(
                """
                {"allOf": [
                  {"type": "object", "required": ["code"]},
                  {"properties": {"code": {"anyOf": [{"type": "integer"}, {"type": "string", "pattern": "^[A-Z]+$"}]}},
                   "additionalProperties": false}
                ]}
                """
            )
        );
        var document = new OpenApiDocument(spec);
        assertDoesNotThrow(() ->
            document.validateSchema("/components/schemas/Combined", MAPPER.readTree("{\"code\":2}"))
        );
        assertDoesNotThrow(() ->
            document.validateSchema("/components/schemas/Combined", MAPPER.readTree("{\"code\":\"ABC\"}"))
        );
        for (String body : new String[] { "{}", "{\"code\":true}", "{\"code\":\"abc\"}", "{\"code\":1,\"extra\":1}" }) {
            assertThrows(AssertionError.class, () ->
                document.validateSchema("/components/schemas/Combined", MAPPER.readTree(body))
            );
        }
    }

    @Test
    void validatesObjectAndContentHeaders() {
        ObjectNode spec = specification();
        ObjectNode headers = (ObjectNode) spec.at("/components/responses/Success/headers");
        headers.set(
            "X-Object",
            MAPPER.readTree(
                """
                {"explode": true, "schema": {"type": "object", "required": ["count"],
                 "properties": {"count": {"type": "integer", "minimum": 1}}}}
                """
            )
        );
        headers.set(
            "X-Json",
            MAPPER.readTree(
                """
                {"content": {"application/json": {"schema": {"type": "object", "required": ["id"]}}}}
                """
            )
        );
        var validator = new OpenApiSchemaValidator(spec);
        assertDoesNotThrow(() ->
            validator.validateResponse(
                response(200, CONTENT_TYPE, VALID_BODY, "X-Count", "1", "X-Object", "count=2", "X-Json", "{\"id\":1}"),
                "/test/{id}",
                "get"
            )
        );
        assertThrows(AssertionError.class, () ->
            validator.validateResponse(
                response(200, CONTENT_TYPE, VALID_BODY, "X-Count", "1", "X-Object", "count=0"),
                "/test/{id}",
                "get"
            )
        );
        assertThrows(AssertionError.class, () ->
            validator.validateResponse(
                response(200, CONTENT_TYPE, VALID_BODY, "X-Count", "1", "X-Json", "{}"),
                "/test/{id}",
                "get"
            )
        );
    }

    @Test
    void validatesOpenApi30Nullable() {
        ObjectNode spec = specification();
        spec.put("openapi", "3.0.3");
        ((ObjectNode) spec.at("/components/schemas/Node/properties")).set(
            "nullable",
            MAPPER.readTree("{\"type\":\"string\",\"nullable\":true}")
        );
        assertDoesNotThrow(() ->
            new OpenApiSchemaValidator(spec).validateResponse(
                response("{\"id\":1,\"nullable\":null}"),
                "/test/{id}",
                "get"
            )
        );
    }
}
