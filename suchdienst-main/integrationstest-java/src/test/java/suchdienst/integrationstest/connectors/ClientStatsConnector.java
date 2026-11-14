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
import suchdienst.integrationstest.common.RestAssuredUtil;

public class ClientStatsConnector {

    public static Response getClientStatsV2() {
        return given(RestAssuredUtil.getRequestSpec()).when().get("/v2/client-stats");
    }
}
