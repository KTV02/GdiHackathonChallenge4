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
package suchdienst.integrationstest.tests.x22.trefferlistev7;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.ErrorValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class TrefferlisteNegativTest {

    @Test
    void arsIsString() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7("P", "TEST");
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest().validateField("ars", "muss mit \"[0-9]*\" übereinstimmen");
    }

    @Test
    void toLongArs() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7("0977201841849", "TEST");
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest().validateField("ars", "Größe muss zwischen 0 und 12 sein");
    }

    @Test
    void toBigSize() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7("097720184184", "TEST", 0, 10000);
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest().validateField("size", "muss kleiner-gleich 9999 sein");
    }

    @Test
    void toSmallSize() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7("097720184184", "TEST", 0, 4);
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest().validateField("size", "muss größer-gleich 5 sein");
    }

    @Test
    void negativePage() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7("097720184184", "TEST", -1, 10);
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest().validateField("page", "muss größer-gleich 0 sein");
    }

    @Test
    void toBigPage() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7("097720184184", "TEST", 101, 10);
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest().validateField("page", "muss kleiner-gleich 100 sein");
    }

    @Test
    void emptyQuery() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7("097720184184", "", 0, 10);
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest().validateField("q[0]", "Größe muss zwischen 2 und 2147483647 sein");
    }
}
