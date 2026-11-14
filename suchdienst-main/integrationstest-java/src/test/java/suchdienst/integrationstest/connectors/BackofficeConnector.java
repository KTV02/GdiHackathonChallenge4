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

import io.restassured.response.Response;
import java.util.HashMap;
import java.util.Map;
import suchdienst.integrationstest.common.RestAssuredUtil;

public class BackofficeConnector {

    public static Response getElasticStatusIndices() {
        return getElasticStatusIndices("application/json");
    }

    public static Response getElasticStatusIndices(String acceptHeader) {
        return given(RestAssuredUtil.getRequestSpec())
            .accept(acceptHeader)
            .when()
            .get("/admin/backoffice/elastic-status/indices");
    }

    public static Response getElasticStatusHealth() {
        return getElasticStatusHealth("application/json");
    }

    public static Response getElasticStatusHealth(String acceptHeader) {
        return given(RestAssuredUtil.getRequestSpec())
            .accept(acceptHeader)
            .when()
            .get("/admin/backoffice/elastic-status/health");
    }

    public static Response getElasticStatusServicedescriptions(String q) {
        return getElasticStatusServicedescriptions(q, 0, 10, "application/json");
    }

    public static Response getElasticStatusServicedescriptions(String q, String acceptHeader) {
        return getElasticStatusServicedescriptions(q, 0, 10, acceptHeader);
    }

    public static Response getElasticStatusServicedescriptions(
        String q,
        Integer page,
        Integer size,
        String acceptHeader
    ) {
        Map<String, Object> params = new HashMap<>();
        if (q != null) {
            params.put("q", q);
        }
        if (page != null) {
            params.put("page", page);
        }
        if (size != null) {
            params.put("size", size);
        }

        return given(RestAssuredUtil.getRequestSpec())
            .accept(acceptHeader)
            .params(params)
            .when()
            .get("/admin/backoffice/elastic-search/servicedescriptions");
    }

    public static Response getElasticStatusOrganisations(String q) {
        return getElasticStatusOrganisations(q, 0, 10, "application/json");
    }

    public static Response getElasticStatusOrganisations(String q, String acceptHeader) {
        return getElasticStatusOrganisations(q, 0, 10, acceptHeader);
    }

    public static Response getElasticStatusOrganisations(String q, Integer page, Integer size, String acceptHeader) {
        Map<String, Object> params = new HashMap<>();
        if (q != null) {
            params.put("q", q);
        }
        if (page != null) {
            params.put("page", page);
        }
        if (size != null) {
            params.put("size", size);
        }

        return given(RestAssuredUtil.getRequestSpec())
            .accept(acceptHeader)
            .params(params)
            .when()
            .get("/admin/backoffice/elastic-search/organisations");
    }
}
