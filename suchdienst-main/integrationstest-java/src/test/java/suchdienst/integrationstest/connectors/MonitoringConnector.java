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

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.Map;
import suchdienst.integrationstest.common.RestAssuredUtil;

public class MonitoringConnector {

    public static Response getBo(String bo) {
        return getBo(bo, 1000, null);
    }

    public static Response getBo(String bo, Integer size, String lastId) {
        Map<String, Object> params = new HashMap<>();
        params.computeIfAbsent("size", val -> size);
        params.computeIfAbsent("lastId", val -> lastId);
        return given(RestAssuredUtil.getRequestSpec()).params(params).when().get("/monitoring/{bo}", bo);
    }

    public static Response getLastResponse(String bo) {
        boolean lastResponse;
        String lastId = null;
        Response response;
        do {
            response = getBo(bo, 1000, lastId);
            JsonPath json = response.then().statusCode(200).contentType(ContentType.JSON).extract().body().jsonPath();

            lastResponse = json.getBoolean("queryResponse.lastResponse");
            lastId = json.getString("queryResponse.lastId");
        } while (!lastResponse);
        return response;
    }

    public static Response isImportActive(String schemeAgencyId) {
        Map<String, Object> params = Map.of("scheme_agency_id", schemeAgencyId);

        return given(RestAssuredUtil.getRequestSpec()).params(params).when().get("/monitoring/import-active");
    }

    public static String getLastId(String bo) {
        return getLastResponse(bo)
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .extract()
            .body()
            .jsonPath()
            .getString("queryResponse.lastId");
    }
}
