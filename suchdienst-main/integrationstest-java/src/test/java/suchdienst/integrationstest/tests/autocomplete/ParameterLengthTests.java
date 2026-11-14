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
package suchdienst.integrationstest.tests.autocomplete;

import io.restassured.response.Response;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import suchdienst.integrationstest.connectors.LocationsConnector;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.ErrorValidator;

/**
 * Testziel: Prüfung der Zeichenlängen an den Autovervollständigungsendpunkten
 */
class ParameterLengthTests {

    @Test
    void minAndMaxLengthLocations() {
        // The minimum length (1) and the maximum length (50) of the query parameter will be tested

        String generatedStringValid1 = RandomStringUtils.random(1, true, true);
        Response responseValid1 = LocationsConnector.getLocationsSuggestionsV2(generatedStringValid1, true, false, 10);
        new ErrorValidator(responseValid1).validateStatusCode(HttpStatus.SC_OK);

        String generatedStringValid2 = RandomStringUtils.random(50, true, true);
        Response responseValid2 = LocationsConnector.getLocationsSuggestionsV2(generatedStringValid2, true, false, 10);
        new ErrorValidator(responseValid2).validateStatusCode(HttpStatus.SC_OK);

        String generatedStringInvalid1 = RandomStringUtils.random(0, true, true);
        Response responseInvalid1 = LocationsConnector.getLocationsSuggestionsV2(
            generatedStringInvalid1,
            true,
            false,
            10
        );
        new ErrorValidator(responseInvalid1).validateBadRequest();

        String generatedStringInvalid2 = RandomStringUtils.random(51, true, true);
        Response responseInvalid2 = LocationsConnector.getLocationsSuggestionsV2(
            generatedStringInvalid2,
            true,
            false,
            10
        );
        new ErrorValidator(responseInvalid2).validateBadRequest();
    }

    @Test
    void minAndMaxLengthSuggestions() {
        // The minimum length (1) and the maximum length (50) of the query parameter will be tested

        String ars = "123456789000";

        String generatedStringValid1 = RandomStringUtils.random(1, true, true);
        Response responseValid1 = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            ars,
            generatedStringValid1,
            false
        );

        new ErrorValidator(responseValid1).validateStatusCode(HttpStatus.SC_OK);

        String generatedStringValid2 = RandomStringUtils.random(50, true, true);
        Response responseValid2 = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            ars,
            generatedStringValid2,
            false
        );
        new ErrorValidator(responseValid2).validateStatusCode(HttpStatus.SC_OK);

        String generatedStringInvalid1 = RandomStringUtils.random(0, true, true);
        Response responseInvalid1 = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            ars,
            generatedStringInvalid1,
            false
        );
        new ErrorValidator(responseInvalid1).validateBadRequest();

        String generatedStringInvalid2 = RandomStringUtils.random(51, true, true);
        Response responseInvalid2 = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            ars,
            generatedStringInvalid2,
            false
        );
        new ErrorValidator(responseInvalid2).validateBadRequest();
    }
}
