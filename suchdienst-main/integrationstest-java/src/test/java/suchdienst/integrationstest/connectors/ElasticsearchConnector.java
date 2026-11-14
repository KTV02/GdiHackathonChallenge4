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
package suchdienst.integrationstest.connectors;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

import io.restassured.response.Response;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import suchdienst.integrationstest.common.RestAssuredUtil;

public class ElasticsearchConnector {

    private static final Duration CLEANUP_MAX_DURATION = Duration.ofSeconds(30);
    private static final Duration CLEANUP_POLL_INTERVAL = Duration.ofMillis(200);
    private static final List<String> ACTIVE_IMPORT_REQUEST_STATUSES = List.of("OPEN", "REOPENED");

    public static void deleteInsertedCodelists(String uri, String version) {
        String jsonQuery =
            "{\n" +
            "  \"query\": {\n" +
            "    \"bool\": {\n" +
            "      \"must\": [\n" +
            "        {\n" +
            "          \"match\": {\n" +
            "            \"version\": \"" +
            version +
            "\"\n" +
            "          }\n" +
            "        },\n" +
            "        {\n" +
            "          \"match\": {\n" +
            "             \"uri\": \"" +
            uri +
            "\"\n" +
            "          }\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  }\n" +
            "}\n";
        given(RestAssuredUtil.getElasticsearchRequestSpec())
            .header("Content-Type", "application/json")
            .when()
            .body(jsonQuery)
            .post("/suchdienst_codelistentries1/_delete_by_query")
            .then()
            .statusCode(HttpStatus.SC_OK);
    }

    public static void removeAllData() {
        List<String> indizes = List.of(
            "suchdienst_leistungsbeschreibungen*",
            "suchdienst_onlinedienste*",
            "suchdienst_formulare*",
            "suchdienst_organisationseinheiten*",
            "suchdienst_spezialisierungen*",
            "suchdienst_zustaendigkeiten*",
            "suchdienst_verwaltungsleistungen*",
            "suchdienst_onlinedienste_relationen*",
            "suchdienst_suggestions*"
        );
        removeAllDataByIndex(indizes);
    }

    public static void removeAllDataByIndex(List<String> indizes) {
        awaitBackendIdle();
        String indexString = StringUtils.joinWith(",", indizes.toArray());
        var payloadMap = Map.of("query", Map.of("match_all", Collections.emptyMap()));
        await("Elasticsearch cleanup")
            .atMost(CLEANUP_MAX_DURATION)
            .with()
            .pollInterval(CLEANUP_POLL_INTERVAL)
            .until(
                () ->
                    given(RestAssuredUtil.getElasticsearchRequestSpec())
                        .header("Content-Type", "application/json")
                        .when()
                        .body(payloadMap)
                        .post("/{indexString}/_delete_by_query?refresh=true", indexString)
                        .statusCode(),
                is(HttpStatus.SC_OK)
            );
    }

    private static void awaitBackendIdle() {
        await("Backend Import-Tasks sind abgeschlossen")
            .atMost(CLEANUP_MAX_DURATION)
            .with()
            .pollInterval(CLEANUP_POLL_INTERVAL)
            .until(() -> ImportQueueConnector.getUpToDate() && activeImportRequestCount() == 0, is(true));
    }

    private static int activeImportRequestCount() {
        return ACTIVE_IMPORT_REQUEST_STATUSES.stream()
            .mapToInt(status ->
                ImportQueueConnector.getQueueStatus(null, null, null, null, null, status)
                    .then()
                    .statusCode(HttpStatus.SC_OK)
                    .extract()
                    .jsonPath()
                    .getInt("totalElements")
            )
            .sum();
    }

    public static Response getVerwaltungsleisstungInfo() {
        return given(RestAssuredUtil.getElasticsearchRequestSpec())
            .header("Content-Type", "application/json")
            .when()
            .get("/suchdienst_verwaltungsleistungen_bb2");
    }

    public static Response getToken(String queryString, String analyzer) {
        Map<String, Object> params = new HashMap<>();
        params.computeIfAbsent("analyzer", val -> analyzer);
        params.computeIfAbsent("text", val -> queryString);
        return given(RestAssuredUtil.getElasticsearchRequestSpec())
            .header("Content-Type", "application/json")
            .when()
            .body(params)
            .post("/suchdienst_verwaltungsleistungen_bb2/_analyze");
    }
}
