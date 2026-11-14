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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static suchdienst.integrationstest.connectors.ServiceDescriptionConnector.getPersonalMattersV1;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ExceptionHandlerTest {

    @ParameterizedTest
    @CsvSource({
        "de," +
            "Die übermittelten Request-Parameter entsprechen nicht den Anforderungen.," +
            "Größe muss zwischen 0 und 12 sein; muss mit \"[0-9]*\" übereinstimmen",
        "en," +
            "The transmitted request parameters do not meet the requirements.," +
            "size must be between 0 and 12; must match \"[0-9]*\"",
    })
    @DisplayName("enthält korrekte Validierungsfehlermeldung und korrekte Validierungskriterien")
    void testConstraintException(String language, String expectedMessage, String expectedConstraints) {
        Response response = getPersonalMattersV1("><IMG SRC=javascript:alert(1)><", language);
        response.then().assertThat().statusCode(HttpStatus.SC_BAD_REQUEST).contentType(ContentType.JSON);
        JsonPath jsonPath = response.getBody().jsonPath();
        assertEquals(expectedMessage, jsonPath.getString("message"));
        Map<Object, Object> actualFields = jsonPath.getMap("fields");
        assertEquals(1, actualFields.size());
        assertTrue(actualFields.containsKey("ars"));
        assertEquals(uniqueSplit(expectedConstraints), uniqueSplit((String) actualFields.get("ars")));
    }

    @Test
    @DisplayName("Validierungsfehler werden immer mit Content-Type Application/Json geliefert")
    void testContraintExceptionIsAlwaysOfContentTypeApplicationJson() {
        Response response = getPersonalMattersV1(
            "abc",
            "de",
            "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        );
        response.then().contentType("application/json");
    }

    private static Set<String> uniqueSplit(String input) {
        return Arrays.stream(input.split(";")).map(String::trim).collect(Collectors.toSet());
    }
}
