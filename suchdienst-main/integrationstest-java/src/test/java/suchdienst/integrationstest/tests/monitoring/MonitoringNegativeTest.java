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
package suchdienst.integrationstest.tests.monitoring;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import suchdienst.integrationstest.connectors.MonitoringConnector;
import suchdienst.integrationstest.validator.ErrorValidator;

/**
 * Testziel: Prueft das Fehlerverhalten der Monitoring-API
 */
class MonitoringNegativeTest {

    @DisplayName("monitorBadRequestSize0")
    @ParameterizedTest(name = "{displayName} {0}")
    @ValueSource(strings = { "lb", "oe", "od", "fo", "sp", "zsoe", "zsod", "zsfo" })
    void monitorBadRequestSize0(String bo) {
        Response response = MonitoringConnector.getBo(bo, 0, null);
        new ErrorValidator(response).validateBadRequest();
    }

    @DisplayName("monitorBadRequestNoSize")
    @ParameterizedTest(name = "{displayName} {0}")
    @ValueSource(strings = { "lb", "oe", "od", "fo", "sp", "zsoe", "zsod", "zsfo" })
    void monitorBadRequestNoSize(String bo) {
        Response response = MonitoringConnector.getBo(bo, null, null);
        new ErrorValidator(response).validateBadRequest();
    }
}
