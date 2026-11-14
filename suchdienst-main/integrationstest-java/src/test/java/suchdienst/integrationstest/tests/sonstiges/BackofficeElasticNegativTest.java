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

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.connectors.BackofficeConnector;
import suchdienst.integrationstest.validator.ErrorValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class BackofficeElasticNegativTest {

    @Test
    void successIndices() {
        Response response = BackofficeConnector.getElasticStatusIndices();
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateStatusCode(200);
    }

    @Test
    void wrongAcceptIndices() {
        Response response = BackofficeConnector.getElasticStatusIndices("application/xml");
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateStatusCode(406);
    }

    @Test
    void successHealth() {
        Response response = BackofficeConnector.getElasticStatusHealth();
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateStatusCode(200);
    }

    @Test
    void wrongAcceptHealth() {
        Response response = BackofficeConnector.getElasticStatusHealth("application/xml");
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateStatusCode(406);
    }
}
