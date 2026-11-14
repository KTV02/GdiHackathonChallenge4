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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.connectors.LocationsConnector;
import suchdienst.integrationstest.mocks.BKGMock;

@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class LocationV3AutocompleteTest {

    private static BKGMock bkgMock;

    @BeforeAll
    static void setup() {
        bkgMock = new BKGMock();
    }

    @AfterEach
    void reset() {
        bkgMock.reset();
    }

    /**
     * <p>Testet, dass alle Daten aus der BKG Anfrage auch ausgegeben werden. Reihenfolge wird eingehalten</p>
     */

    @Test
    void matchWithUmlaut() throws IOException {
        String json = IOUtils.resourceToString("/bkg/SuggestNuer.json", StandardCharsets.UTF_8);
        JsonPath expectedPath = JsonPath.with(json);
        List<String> expectedList = expectedPath.getList("suggestion");
        bkgMock.createSuggestionResponseBkgMock("Nuer", json);
        Response response = LocationsConnector.getLocationsSuggestionsV3("Nuer");
        JsonPath jsonResponsePath = response.jsonPath();
        List<String> suggestionList = jsonResponsePath.getList("name");
        assertThat(response.getStatusCode(), is(HttpStatus.SC_OK));
        assertThat(expectedList, equalTo(suggestionList));
        bkgMock.verifySuggestionBkgMockResponse("Nuer", 1);
    }

    /**
     * <p>Testet, dass Antwort des BKG mit Status 403 vom locations Endpunkt Status 500 zurückgegeben wird</p>
     * <p>BKG wird nur einmal angefragt</p>
     */
    @Test
    void bkgNotAllowed() {
        bkgMock.createSuggestionErrorResponseBkgMock("NotAllowed", 403);
        Response response = LocationsConnector.getLocationsSuggestionsV3("NotAllowed");
        assertThat(response.getStatusCode(), is(500));
        bkgMock.verifySuggestionBkgMockResponse("NotAllowed", 1);
    }

    /**
     * <p>Testet, dass Antwort des BKG mit Status 503 vom locations Endpunkt Status 500 zurückgegeben wird</p>
     * <p>BKG wird nur zweimal angefragt - bei Server Error ist eine erneute Anfrage sinnvoll</p>
     */
    @Test
    void bkgServerError() {
        bkgMock.createSuggestionErrorResponseBkgMock("ServerError", 503);
        Response response = LocationsConnector.getLocationsSuggestionsV3("ServerError");
        assertThat(response.getStatusCode(), is(500));
        // bei ServerError wird ein weiterer Aufruf des BKG ausgeführt
        bkgMock.verifySuggestionBkgMockResponse("ServerError", 2);
    }

    /**
     * <p>Testet, dass bei verzögerter Antwort des BKG vom locations Endpunkt Status 500 zurückgegeben wird</p>
     * <p>BKG würde 200 liefern - Timeout greift vorher</p>
     */
    @Test
    void bkgTimeout() {
        bkgMock.createSuggestionTimeoutResponseBkgMock("TimeOut", 200);
        Response response = LocationsConnector.getLocationsSuggestionsV3("TimeOut");
        assertThat(response.getStatusCode(), is(500));
        bkgMock.verifySuggestionBkgMockResponse("TimeOut", 1);
    }
}
