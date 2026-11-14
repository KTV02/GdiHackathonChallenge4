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
package suchdienst.integrationstest.tests.bkg.locationsv2;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.connectors.LocationsConnector;
import suchdienst.integrationstest.validator.ErrorValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class LocationsNegativeTest {

    @Test
    void negativLimit() {
        Response response = LocationsConnector.getLocationsV2("suchParameter", -10);
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest().validateField("limit", "muss größer-gleich 1 sein");
    }

    @Test
    void zeroLimit() {
        Response response = LocationsConnector.getLocationsV2("suchParameter", 0);
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest().validateField("limit", "muss größer-gleich 1 sein");
    }

    @Test
    void emptySearchParam() {
        Response response = LocationsConnector.getLocationsV2("", 5);
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest().validateField("q", "Größe muss zwischen 2 und 2147483647 sein");
    }
}
