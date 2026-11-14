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
package suchdienst.integrationstest.tests.sonstiges;

import static io.restassured.RestAssured.given;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.RestAssuredUtil;
import suchdienst.integrationstest.validator.OpenApiDocument;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Testziel: Laden und Validieren der vom Backend bereitgestellten OpenAPI-Dokumentation (PV-8997)
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class OpenApiDocTest {

    // Endpunkte zentral definiert
    private static final String[] TESTED_ENDPOINTS = {
        "/v1/personalmatters/{ars}",
        "/v1/servicedescriptions/suggestions/{ars}",
        "/v2/servicedescriptions/leikahierarchy",
        "/v3/servicedescriptions/leikaid",
        "/v5/servicedescriptions/{ars}/detail",
        "/v7/servicedescriptions/{ars}",
        "/v3/locations",
        "/v3/locations/ars",
        "/v3/locations/details",
        "/v3/locations/suggestions",
        "/v2/locations/reverse",
        "/v2/client-stats",
        "/v2/locations",
        "/v2/locations/suggestions",
        "/v2/organisationunits/titles",
        "/v5/organisationunits/detail",
        "/v2/onlineservices/detail",
        "/v2/servicedescriptions/{id}/jzufi-2-3",
        "/v2/organisationunits/{id}/jzufi-2-3",
        "/v2/onlineservices/{id}/jzufi-2-3",
        "/v2/relations/jzufi-2-3",
    };

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    /**
     * Lädt die generierte OpenAPI-Dokumentation als yaml-Datei runter und prüft einige Sachen, insbesondere
     * de.dataport.suchdienst.suchen.model.LifeSituationAggOutModel#children.
     */
    @Test
    @SuppressWarnings("unchecked")
    void downloadYaml() {
        final Response response = given(RestAssuredUtil.getRequestSpec()).accept(ContentType.JSON).get("/v3/api-docs");
        response.then().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON);

        final ObjectMapper yamlReader = YAMLMapper.builder().build();
        final String yamlString = response.getBody().asString();
        // Für die Strukturprüfungen lesen wir die Dokumentation in eine generische Map.
        final var o = yamlReader.readValue(yamlString, Map.class);

        assertThat(o.get("openapi"), is("3.1.0"));
        final var info = (Map<String, Object>) o.get("info");
        assertThat(info.get("title"), is("Portalverbund - Suchdienst REST-API"));
        final var contact = (Map<String, Object>) info.get("contact");
        assertThat(contact.get("name"), is("FITKO"));
        assertThat(contact.get("url"), is("https://www.fitko.de/"));
        final var license = (Map<String, Object>) info.get("license");
        assertThat(license.get("name"), is("Creative Commons Attribution Share Alike 4.0 (CC BY-SA 4.0)"));
        assertThat(license.get("url"), is("https://creativecommons.org/licenses/by-sa/4.0/"));
        assertThat(info.get("version"), notNullValue()); // property expansion für @project.version@
        final var servers = (List<Map<String, String>>) o.get("servers");
        assertThat(servers, hasSize(1));
        final var server = servers.getFirst();
        assertThat(server.get("url"), notNullValue());
        assertThat(server.get("description"), is("Generated server url"));
        //                                             ^^^^^^^^^^^^^^^^^^^^
        // Hier wäre IMHO richtig: "Produktivsystem" oder "Testsystem" oder sowas, siehe Diskussion in PV-8419
        final var paths = (Map<String, Map<String, Object>>) o.get("paths");
        final var v7ServiceDescripionArs = paths.get("/v7/servicedescriptions/{ars}");
        final var get = (Map<String, Object>) v7ServiceDescripionArs.get("get");
        assertThat(get.get("tags"), is(singletonList("Leistungen")));
        assertThat(get.get("summary"), notNullValue());
        assertThat(get.get("operationId"), is("getSearchOutModelV7"));

        final var responses = (Map<String, Map<String, Map<String, Object>>>) get.get("responses");
        final var responseOK = responses.get("200");
        final var responseOKContent = responseOK.get("content");
        assertThat(responseOKContent.keySet(), is(singleton("application/json")));
        final var responseOKContentJSON = (Map<String, Map<String, Object>>) responseOKContent.get("application/json");
        assertThat(responseOKContentJSON.keySet(), is(singleton("schema")));
        final var responseOKContentJSONSchema = responseOKContentJSON.get("schema");
        assertThat(
            responseOKContentJSONSchema.get("$ref"),
            is("#/components/schemas/SearchOutModelSdOutModelV7_Search")
        );

        final var components = (Map<String, Map<String, Map<String, Object>>>) o.get("components");
        assertThat(components.keySet(), is(Set.of("schemas", "parameters", "headers")));
        // Validate schemas
        final var componentsSchemas = components.get("schemas");
        final var lifeSituationAggOutModelSearch = componentsSchemas.get("LifeSituationAggOutModel_Search");
        assertThat(lifeSituationAggOutModelSearch.keySet(), is(Set.of("type", "description", "properties")));
        final var properties = (Map<String, Map<String, Object>>) lifeSituationAggOutModelSearch.get("properties");
        // PV-8997: hier fehlte bisher "children"
        assertThat(properties.keySet(), containsInAnyOrder("code", "name", "count", "children"));
        final var children = properties.get("children");
        assertThat(children.keySet(), containsInAnyOrder("type", "description", "items"));
        final var items = (Map<String, Object>) children.get("items");
        assertThat(items.keySet(), is(singleton("$ref")));
        assertThat(items.get("$ref"), is("#/components/schemas/LifeSituationAggOutModel_Search"));

        final var v5ServicedescriptionsArsDetail = paths.get("/v5/servicedescriptions/{ars}/detail");
        final var getMapSvD5 = (Map<String, Object>) v5ServicedescriptionsArsDetail.get("get");
        assertThat(getMapSvD5.get("tags"), is(singletonList("Leistungen")));
        assertThat(getMapSvD5.get("summary"), notNullValue());
        assertThat(getMapSvD5.get("operationId"), is("getSdOutModelV5"));
        final var responseSvD5 = (Map<String, Map<String, Map<String, Object>>>) getMapSvD5.get("responses");
        final var responseSvD5OK = responseSvD5.get("200");
        final var responsesSvD5Content = responseSvD5OK.get("content");
        assertThat(responsesSvD5Content.keySet(), is(singleton("application/json")));
        final var responseSvD5OKContentJSON = (Map<String, Map<String, Object>>) responsesSvD5Content.get(
            "application/json"
        );

        // Prüft das Vorhandensein der @type-Property in den SeDeBlock-Modellen
        // Es wird der Parent-Typ SeDeBlock_Detail geprüft, da dieser via allOf in die Kind-Typen vererbt wird.
        assertComponentSchemaHasProperty(componentsSchemas, "SeDeBlock_Detail", "@type");

        assertThat(responseSvD5OKContentJSON.keySet(), is(singleton("schema")));
        final var responseSvD5OKContentJSONSchema = responseSvD5OKContentJSON.get("schema");
        assertThat(responseSvD5OKContentJSONSchema.get("$ref"), is("#/components/schemas/SdOutModelV5_Detail"));

        final var v5OrganisationUnitsDetail = paths.get("/v5/organisationunits/detail");
        final var getMapOu5 = (Map<String, Object>) v5OrganisationUnitsDetail.get("get");
        assertThat(getMapOu5.get("tags"), is(singletonList("Organisationseinheiten")));
        assertThat(getMapOu5.get("summary"), notNullValue());
        assertThat(getMapOu5.get("operationId"), is("getDetailOeOutModelV5"));
        final var responseOu5 = (Map<String, Map<String, Map<String, Object>>>) getMapOu5.get("responses");
        final var responseOu5OK = responseOu5.get("200");
        final var responsesOu5Content = responseOu5OK.get("content");
        assertThat(responsesOu5Content.keySet(), is(singleton("application/json")));
        final var responseOu5OKContentJSON = (Map<String, Map<String, Object>>) responsesOu5Content.get(
            "application/json"
        );
        assertThat(responseOu5OKContentJSON.keySet(), is(singleton("schema")));
        final var responseOu5OKContentJSONSchema = responseOu5OKContentJSON.get("schema");
        assertThat(responseOu5OKContentJSONSchema.get("$ref"), is("#/components/schemas/DetailOeOutModelV5_Detail"));

        final var v3ServicedescriptionsLeikaid = paths.get("/v3/servicedescriptions/leikaid");
        final var getMapSvDLeikaid = (Map<String, Object>) v3ServicedescriptionsLeikaid.get("get");
        assertThat(getMapSvDLeikaid.get("tags"), is(singletonList("Leistungen")));
        assertThat(getMapSvDLeikaid.get("summary"), notNullValue());
        assertThat(getMapSvDLeikaid.get("operationId"), is("findByLeikaIdV3"));
        final var responseSvDLeikaid = (Map<String, Map<String, Map<String, Object>>>) getMapSvDLeikaid.get(
            "responses"
        );
        final var responseSvDLeikaidOK = responseSvDLeikaid.get("200");
        final var responsesSvDLeikaidContent = responseSvDLeikaidOK.get("content");
        assertThat(responsesSvDLeikaidContent.keySet(), is(singleton("application/json")));
        final var responseSvDLeikaidOKContentJSON = (Map<String, Map<String, Object>>) responsesSvDLeikaidContent.get(
            "application/json"
        );
        assertThat(responseSvDLeikaidOKContentJSON.keySet(), is(singleton("schema")));
        final var responseSvDLeikaidOKContentJSONSchema = responseSvDLeikaidOKContentJSON.get("schema");
        assertThat(
            responseSvDLeikaidOKContentJSONSchema.get("$ref"),
            is("#/components/schemas/ResultPageSdOutModelV7")
        );

        final var v3Locations = paths.get("/v3/locations");
        final var getMapLocations = (Map<String, Object>) v3Locations.get("get");
        assertThat(getMapLocations.get("tags"), is(singletonList("Orte - BKG")));
        assertThat(getMapLocations.get("summary"), notNullValue());
        assertThat(getMapLocations.get("operationId"), is("getLocationsByQuery"));
        final var responseLocations = (Map<String, Map<String, Map<String, Object>>>) getMapLocations.get("responses");
        final var responseLocationsOK = responseLocations.get("200");
        final var responsesLocationsContent = responseLocationsOK.get("content");
        assertThat(responsesLocationsContent.keySet(), is(singleton("application/json")));
        final var responseLocationsOKContentJSON = (Map<String, Map<String, Object>>) responsesLocationsContent.get(
            "application/json"
        );
        assertThat(responseLocationsOKContentJSON.keySet(), is(Set.of("schema", "example")));
        final var responseLocationsOKContentJSONSchema = responseLocationsOKContentJSON.get("schema");
        final var responseLocationsOKContentJSONSchemaItems = (Map<
            String,
            Object
        >) responseLocationsOKContentJSONSchema.get("items");
        assertThat(responseLocationsOKContentJSONSchemaItems.get("$ref"), is("#/components/schemas/GeoArsOutModelV3"));

        final var v3LocationsDetails = paths.get("/v3/locations/details");
        final var getMapLocationsSuggestions = (Map<String, Object>) v3LocationsDetails.get("get");
        assertThat(getMapLocationsSuggestions.get("tags"), is(singletonList("Orte - BKG")));
        assertThat(getMapLocationsSuggestions.get("summary"), notNullValue());
        assertThat(getMapLocationsSuggestions.get("operationId"), is("getLocationsByAddress"));
        final var responseLocationsSuggestions = (Map<
            String,
            Map<String, Map<String, Object>>
        >) getMapLocationsSuggestions.get("responses");
        final var responseLocationsSuggestionsOK = responseLocationsSuggestions.get("200");
        final var responsesLocationsSuggestionsContent = responseLocationsSuggestionsOK.get("content");
        assertThat(responsesLocationsSuggestionsContent.keySet(), is(singleton("application/json")));
        final var responseLocationsSuggestionsOKContentJSON = (Map<
            String,
            Map<String, Object>
        >) responsesLocationsSuggestionsContent.get("application/json");
        assertThat(responseLocationsSuggestionsOKContentJSON.keySet(), is(Set.of("schema", "example")));
        final var responseLocationsSuggestionsOKContentJSONSchema = responseLocationsSuggestionsOKContentJSON.get(
            "schema"
        );
        final var responseLocationsSuggestionsOKContentJSONSchemaItems = (Map<
            String,
            Object
        >) responseLocationsSuggestionsOKContentJSONSchema.get("items");
        assertThat(
            responseLocationsSuggestionsOKContentJSONSchemaItems.get("$ref"),
            is("#/components/schemas/GeoArsOutModelV3")
        );
    }

    /**
     * Testet, dass der von elasticsearch als JSON-String zurückgegebene Health-Status schematisch demjenigen entspricht,
     * das in der openapi-Doku erstens als Schema (in #/components/schemas/ElasticHealthResponse)
     * und zweitens als Beispiel (in #/paths/'/admin/backoffice/elastic-status/health'/get/200/'application/json'/example[0]/) angegeben ist.
     */
    @Test
    @SuppressWarnings("unchecked")
    void verifyHealthStatusSchema() {
        final Response response = given(RestAssuredUtil.getRequestSpec()).accept(ContentType.JSON).get("/v3/api-docs");
        response.then().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON);

        final ObjectMapper yamlReader = YAMLMapper.builder().build();
        final String yamlString = response.getBody().asString();
        final var o = yamlReader.readValue(yamlString, Map.class);

        final var components = (Map<String, Map<String, Map<String, Map<String, Object>>>>) o.get("components");
        assertThat(components.keySet(), is(Set.of("schemas", "parameters", "headers")));
        final var componentsSchemas = components.get("schemas");
        final var elasticHealthResponse = componentsSchemas.get("ElasticHealthResponse");
        final var elasticHealthResponseProperties = elasticHealthResponse.get("properties");
        final String[] expectedProps = {
            "epoch",
            "timestamp",
            "cluster",
            "status",
            "node.total",
            "node.data",
            "shards",
            "pri",
            "relo",
            "init",
            "unassign",
            "pending_tasks",
            "max_task_wait_time",
            "active_shards_percent",
        };
        assertThat(elasticHealthResponseProperties.keySet(), containsInAnyOrder(expectedProps));
        final var paths = (Map<String, Map<String, Map<String, Object>>>) o.get("paths");
        final var pathHealth = paths.get("/admin/backoffice/elastic-status/health");
        final var pathHealthGet = pathHealth.get("get");
        final var pathHealthGetResponses = (Map<String, Map<String, Object>>) pathHealthGet.get("responses");
        final var pathHealthGetResponses200 = pathHealthGetResponses.get("200");
        final var pathHealthGetResponses200Content = (Map<
            String,
            Map<String, Map<String, Object>>
        >) pathHealthGetResponses200.get("content");
        final var pathHealthGetResponses200ContentJson = pathHealthGetResponses200Content.get("application/json");
        final var pathHealthGetResponses200ContentJsonSchema = pathHealthGetResponses200ContentJson.get("schema");
        assertThat(pathHealthGetResponses200ContentJsonSchema.get("type"), is("array"));
        final var pathHealthGetResponses200ContentJsonSchemaItems = (Map<
            String,
            Object
        >) pathHealthGetResponses200ContentJsonSchema.get("items");
        assertThat(
            pathHealthGetResponses200ContentJsonSchemaItems.get("$ref"),
            is("#/components/schemas/ElasticHealthResponse")
        );
        final var pathHealthGetResponses200ContentJsonExample = (List<?>) pathHealthGetResponses200ContentJson.get(
            "example"
        );
        final var pathHealthGetResponses200ContentJsonExample0 = (Map<
            String,
            Object
        >) pathHealthGetResponses200ContentJsonExample.getFirst();
        assertThat(pathHealthGetResponses200ContentJsonExample0.keySet(), containsInAnyOrder(expectedProps));
    }

    /**
     * Testet für die in TESTED_ENDPOINTS enthaltenen Endpunkte die Beispiele für 200-Responses gegen das hinterlegte Schema.
     * Wird zusätzlich via Spectral geprüft.
     */
    @Test
    void openApiTest() {
        Response response = given(RestAssuredUtil.getRequestSpec()).accept(ContentType.JSON).get("/v3/api-docs");
        response.then().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON);
        var apiDoc = new OpenApiDocument(OBJECT_MAPPER.readTree(response.asString()));
        List<Executable> validations = new ArrayList<>();
        for (String endpoint : TESTED_ENDPOINTS) {
            validations.add(() -> {
                String responsePointer = apiDoc.resolvePointer(
                    OpenApiDocument.pointer("paths", endpoint, "get", "responses", "200")
                );
                String mediaPointer = responsePointer + OpenApiDocument.pointer("content", "application/json");
                apiDoc.node(mediaPointer + "/schema");
                List<Executable> examples = new ArrayList<>();
                for (var example : apiDoc.examples(mediaPointer)) {
                    examples.add(() -> apiDoc.validateSchema(mediaPointer + "/schema", example));
                }
                assertAll("OpenAPI-Beispiele für " + endpoint, examples);
            });
        }
        assertAll("OpenAPI-Beispiele aller getesteten Endpunkte", validations);
    }

    /**
     * Testziel: Die generierten SeDeBlock-Varianten müssen anhand von @type eindeutig sein.
     * Prüft die automatische oneOf-Auswahl am Listenfeld, das Discriminator-Mapping der Basis
     * sowie die exklusiven Untertypschemas. Die vererbbare Basis enthält selbst keine Auswahl.
     */
    @ParameterizedTest
    @ValueSource(strings = { "/v3/api-docs", "/v3/api-docs/suchen" })
    void seDeBlockSchemasAreUnambiguous(String documentationPath) {
        Response response = given(RestAssuredUtil.getRequestSpec()).accept(ContentType.JSON).get(documentationPath);
        response.then().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON);
        assertSeDeBlockSchemasAreUnambiguous(new OpenApiDocument(OBJECT_MAPPER.readTree(response.asString())));
    }

    static void assertSeDeBlockSchemasAreUnambiguous(OpenApiDocument apiDoc) {
        String itemsPointer = OpenApiDocument.pointer(
            "components",
            "schemas",
            "SdOutModelV5_Detail",
            "properties",
            "details",
            "items"
        );
        String mappingPointer = OpenApiDocument.pointer(
            "components",
            "schemas",
            "SeDeBlock_Detail",
            "discriminator",
            "mapping"
        );
        String modulePointer = apiDoc.node(mappingPointer).path("_module").asString().substring(1);
        String detailPointer = apiDoc.node(mappingPointer).path("_moduleDetail").asString().substring(1);
        assertThat(
            apiDoc
                .node(itemsPointer)
                .path("oneOf")
                .valueStream()
                .map(branch -> branch.path("$ref").asString())
                .toList(),
            containsInAnyOrder("#" + modulePointer, "#" + detailPointer)
        );
        assertThat(
            apiDoc.node(OpenApiDocument.pointer("components", "schemas", "SeDeBlock_Detail")).has("oneOf"),
            is(false)
        );
        assertThat(apiDoc.node(mappingPointer).propertyNames(), containsInAnyOrder("_module", "_moduleDetail"));
        var block = OBJECT_MAPPER.createObjectNode()
            .put("@type", "_module")
            .put("code", "02")
            .put("filled", true)
            .put("placeholder", false)
            .put("replace", false)
            .put("title", "Testmodul");

        apiDoc.validateSchema(itemsPointer, block);
        apiDoc.validateSchema(modulePointer, block);
        assertThrows(AssertionError.class, () -> apiDoc.validateSchema(detailPointer, block));

        block.put("@type", "_moduleDetail");
        apiDoc.validateSchema(itemsPointer, block);
        apiDoc.validateSchema(detailPointer, block);
        assertThrows(AssertionError.class, () -> apiDoc.validateSchema(modulePointer, block));

        for (String invalidType : List.of("_unknown", "_moduleExtra", "_module\n", "_moduleDetail\n")) {
            block.put("@type", invalidType);
            assertThrows(AssertionError.class, () -> apiDoc.validateSchema(itemsPointer, block));
            assertThrows(AssertionError.class, () -> apiDoc.validateSchema(modulePointer, block));
            assertThrows(AssertionError.class, () -> apiDoc.validateSchema(detailPointer, block));
        }
        block.remove("@type");
        assertThrows(AssertionError.class, () -> apiDoc.validateSchema(itemsPointer, block));
        assertThrows(AssertionError.class, () -> apiDoc.validateSchema(modulePointer, block));
        assertThrows(AssertionError.class, () -> apiDoc.validateSchema(detailPointer, block));
    }

    /**
     * Testziel: Die neuen v2-Antworten referenzieren ausschließlich die lokalen JZuFi-2.3-Roots;
     * alle neun Familien beschreiben ihre Ausprägungen über @type und vererbbare Strukturen.
     */
    @ParameterizedTest
    @ValueSource(strings = { "/v3/api-docs", "/v3/api-docs/suchen" })
    void jzufi23SchemasExposeAllFamilyContracts(String documentationPath) {
        Response response = given(RestAssuredUtil.getRequestSpec()).accept(ContentType.JSON).get(documentationPath);
        response.then().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON);
        var apiDoc = new OpenApiDocument(OBJECT_MAPPER.readTree(response.asString()));
        var endpoints = Map.of(
            "/v2/servicedescriptions/{id}/jzufi-2-3",
            "Jzufi23Leistung",
            "/v2/organisationunits/{id}/jzufi-2-3",
            "Jzufi23OrganisationseinheitErweitert",
            "/v2/onlineservices/{id}/jzufi-2-3",
            "Jzufi23OnlinedienstErweitert",
            "/v2/relations/jzufi-2-3",
            "JzufiRelationPage23"
        );
        endpoints.forEach((endpoint, schema) -> {
            String responsePointer = apiDoc.resolvePointer(
                OpenApiDocument.pointer("paths", endpoint, "get", "responses", "200")
            );
            assertThat(
                apiDoc.resolvePointer(
                    responsePointer + OpenApiDocument.pointer("content", "application/json", "schema")
                ),
                is(OpenApiDocument.pointer("components", "schemas", schema))
            );
        });
        var families = Map.of(
            "Zustaendigkeit",
            4,
            "Leistungsstruktur",
            3,
            "Formulardatei",
            3,
            "Anschrift",
            2,
            "Kontaktperson",
            2,
            "Onlinedienst",
            3,
            "Sprache",
            2,
            "Kategorieklasse",
            2,
            "CodeType",
            2
        );
        families.forEach((family, count) -> {
            var base = apiDoc.node(OpenApiDocument.pointer("components", "schemas", "Jzufi23" + family));
            assertThat(base.path("discriminator").path("propertyName").asString(), is("@type"));
            assertThat(
                base
                    .path("required")
                    .valueStream()
                    .anyMatch(value -> "@type".equals(value.asString())),
                is(true)
            );
            var mapping = base.path("discriminator").path("mapping");
            assertThat(mapping.size(), is(count));
            for (String type : mapping.propertyNames()) {
                String reference = mapping.path(type).asString();
                assertThat(reference, is("#/components/schemas/Jzufi23" + type));
                apiDoc.node(reference.substring(1));
            }
        });
        apiDoc.node(OpenApiDocument.pointer("components", "schemas")).forEachEntry((name, schema) -> {
            if (!name.startsWith("Jzufi23")) return;
            assertThat(name.endsWith("Exact") || name.endsWith("Selection"), is(false));
            for (String keyword : List.of("oneOf", "anyOf", "if", "then", "else")) {
                assertThat(name + ": " + keyword, schema.findValues(keyword).isEmpty(), is(true));
            }
        });
    }

    /**
     * Prüft, ob im übergebenen componentsSchemas das Schema mit dem Schlüssel schemaKey eine Property mit dem
     * Schlüssel propertyKey besitzt.
     */
    @SuppressWarnings("unchecked")
    private void assertComponentSchemaHasProperty(
        Map<String, Map<String, Object>> componentsSchemas,
        String schemaKey,
        String propertyKey
    ) {
        var schema = componentsSchemas.get(schemaKey);
        assertThat("Schema " + schemaKey + " nicht gefunden", schema, notNullValue());
        var props = (Map<String, Object>) schema.get("properties");
        assertThat("Properties von " + schemaKey + " fehlen", props, notNullValue());
        assertThat(
            "Property '" + propertyKey + "' fehlt im Schema " + schemaKey,
            props.containsKey(propertyKey),
            is(true)
        );
    }
}
