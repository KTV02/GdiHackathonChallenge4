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

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.net.MediaType;
import io.restassured.http.Header;
import io.restassured.response.Response;
import java.util.Locale;
import java.util.TreeMap;
import suchdienst.integrationstest.common.Constants;
import suchdienst.integrationstest.common.RestAssuredUtil;
import suchdienst.integrationstest.validator.OpenApiDocument;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class OpenApiSchemaValidator {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static OpenApiSchemaValidator api;
    private final OpenApiDocument document;

    public OpenApiSchemaValidator(JsonNode document) {
        this.document = new OpenApiDocument(document);
    }

    private static synchronized OpenApiSchemaValidator getApi() {
        if (api == null) {
            Response response = given(RestAssuredUtil.getRequestSpec()).get(Constants.BASE_PATH + "/v3/api-docs");
            response.then().statusCode(200);
            api = new OpenApiSchemaValidator(MAPPER.readTree(response.asString()));
        }
        return api;
    }

    /**
     * Validiert die Antwort basierend auf dem OpenAPI-Schema für die angegebene Version und HTTP-Methode.
     *
     * @param response      Die HTTP-Antwort, die validiert werden soll.
     * @param endpointPath  Der Endpunktpfad in der OpenAPI-Spezifikation.
     * @param httpMethod    Die HTTP-Methode (z. B. "get", "post").
     */
    public static void validate(Response response, String endpointPath, String httpMethod) {
        getApi().validateResponse(response, endpointPath, httpMethod);
    }

    public void validateResponse(Response response, String endpointPath, String httpMethod) {
        assertNotNull(response, "Die HTTP-Antwort ist null.");
        assertNotNull(endpointPath, "Der Endpunktpfad ist null.");
        assertNotNull(httpMethod, "Die HTTP-Methode ist null.");
        assertFalse(endpointPath.isBlank(), "Der Endpunktpfad ist leer.");
        assertFalse(httpMethod.isBlank(), "Die HTTP-Methode ist leer.");
        String operationPointer =
            document.resolvePointer(OpenApiDocument.pointer("paths", endpointPath)) +
            OpenApiDocument.pointer(httpMethod.toLowerCase(Locale.ROOT));
        JsonNode responses = document.node(operationPointer).path("responses");
        String status = String.valueOf(response.statusCode());
        String responseKey = responses.has(status) ? status : status.charAt(0) + "XX";
        if (!responses.has(responseKey)) {
            responseKey = "default";
        }
        assertTrue(responses.has(responseKey), "Statuscode " + status + " ist nicht dokumentiert: " + endpointPath);
        String responsePointer = document.resolvePointer(
            operationPointer + OpenApiDocument.pointer("responses", responseKey)
        );
        JsonNode definition = document.node(responsePointer);
        validateHeaders(response, responsePointer, definition.path("headers"));
        String body = response.asString();
        JsonNode content = definition.path("content");
        if (content.isMissingNode() || content.isEmpty()) {
            assertTrue(body.isEmpty(), "Nicht dokumentierter Response-Body: " + endpointPath);
            return;
        }
        String media = selectMediaType(content, response.getContentType());
        String schemaPointer = responsePointer + OpenApiDocument.pointer("content", media, "schema");
        if (!document.node(responsePointer + OpenApiDocument.pointer("content", media)).has("schema")) {
            return; // OpenAPI erlaubt Content ohne Schema.
        }
        assertFalse(body.isEmpty(), "Dokumentierter Response-Body fehlt: " + endpointPath);
        MediaType actual = MediaType.parse(response.getContentType());
        JsonNode value =
            actual.subtype().equals("json") || actual.subtype().endsWith("+json")
                ? MAPPER.readTree(body)
                : MAPPER.valueToTree(body);
        document.validateSchema(schemaPointer, value);
    }

    private static String selectMediaType(JsonNode content, String contentType) {
        assertNotNull(contentType, "Content-Type fehlt.");
        assertFalse(contentType.isBlank(), "Content-Type fehlt.");
        MediaType actual = MediaType.parse(contentType);
        String selected = null;
        int bestScore = -1;
        for (String candidate : content.propertyNames()) {
            MediaType expected = MediaType.parse(candidate);
            if (actual.is(expected)) {
                int score =
                    (expected.type().equals("*") ? 0 : 100) +
                    (expected.subtype().equals("*") ? 0 : 10) +
                    expected.parameters().size();
                if (score > bestScore) {
                    selected = candidate;
                    bestScore = score;
                }
            }
        }
        assertNotNull(selected, "Content-Type ist nicht dokumentiert: " + contentType);
        return selected;
    }

    private void validateHeaders(Response response, String responsePointer, JsonNode headers) {
        var actual = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        for (Header header : response.getHeaders()) {
            actual.merge(header.getName(), header.getValue(), (a, b) -> a + "," + b);
        }
        for (String name : headers.propertyNames()) {
            if (name.equalsIgnoreCase("Content-Type")) {
                continue; // Laut OpenAPI wird dieser Header ausschließlich über content geprüft.
            }
            String headerPointer = document.resolvePointer(responsePointer + OpenApiDocument.pointer("headers", name));
            JsonNode header = document.node(headerPointer);
            String value = actual.get(name);
            if (value == null) {
                assertFalse(header.path("required").asBoolean(), "Pflichtheader fehlt: " + name);
                continue;
            }
            if (header.has("schema")) {
                assertTrue(
                    header.path("style").asString("simple").equals("simple"),
                    "Unbekannter Header-Stil: " + name
                );
                String schemaPointer = headerPointer + "/schema";
                document.validateSchema(
                    schemaPointer,
                    headerValue(value, document.node(schemaPointer), header.path("explode").asBoolean())
                );
            } else if (header.has("content")) {
                assertTrue(header.get("content").size() == 1, "Header muss genau einen Medientyp haben: " + name);
                String media = header.get("content").propertyNames().iterator().next();
                assertTrue(media.equals("application/json"), "Nicht unterstützter Header-Medientyp: " + media);
                document.validateSchema(
                    headerPointer + OpenApiDocument.pointer("content", media, "schema"),
                    MAPPER.readTree(value)
                );
            } else {
                fail("Header ohne Schema oder Content: " + name);
            }
        }
    }

    private JsonNode headerValue(String value, JsonNode schema, boolean explode) {
        if (schema.has("$ref")) {
            schema = document.node(schema.path("$ref").asString().substring(1));
        }
        String type = schema.path("type").asString("string");
        if (type.equals("array")) {
            var array = MAPPER.createArrayNode();
            for (String item : value.split(",", -1)) {
                array.add(headerValue(item.trim(), schema.path("items"), false));
            }
            return array;
        }
        if (type.equals("object")) {
            var object = MAPPER.createObjectNode();
            String[] parts = value.split(",", -1);
            assertTrue(explode || parts.length % 2 == 0, "Ungültiger Objekt-Header: " + value);
            for (int i = 0; i < parts.length; i += explode ? 1 : 2) {
                String[] pair = explode ? parts[i].split("=", 2) : new String[] { parts[i], parts[i + 1] };
                assertTrue(pair.length == 2, "Ungültiger Objekt-Header: " + value);
                object.set(
                    pair[0].trim(),
                    headerValue(pair[1].trim(), schema.path("properties").path(pair[0].trim()), false)
                );
            }
            return object;
        }
        if (type.equals("integer") || type.equals("number") || type.equals("boolean")) {
            return MAPPER.readTree(value);
        }
        return MAPPER.valueToTree(value);
    }
}
