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
package suchdienst.integrationstest.validator.locationssuggestionsv2;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import java.util.List;
import org.apache.http.HttpStatus;

@SuppressWarnings("UnusedReturnValue")
public class LocationsResultListValidator {

    private final ValidatableResponse then;

    public LocationsResultListValidator(Response response) {
        this.then = response.then();
    }

    public LocationsResultListValidator validateContains(String expectedId) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("name", hasItem(expectedId));
        return this;
    }

    public LocationsResultListValidator validateContainsZIP(String expectedZIP) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("zip", hasItem(expectedZIP));
        return this;
    }

    public LocationsResultListValidator validateNotContains(String notExpectedId) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("name", not(hasItem(notExpectedId)));
        return this;
    }

    public LocationsResultListValidator validateIsEmpty() {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("name", empty());
        return this;
    }

    public LocationsResultListValidator validateOrderBetween2Items(String frueher, String spaeter) {
        JsonPath json = then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).extract().jsonPath();
        List<String> suggestions = json.getList("name");
        int frueherPos = suggestions.indexOf(frueher);
        //check, dass frueher in Liste auftaucht (-1 bedeutet nicht gefunden)
        assertThat(frueherPos, not(is(-1)));
        int spaeterPos = suggestions.indexOf(spaeter);
        //check, dass spaeter in Liste auftaucht (-1 bedeutet nicht gefunden)
        assertThat(spaeterPos, not(is(-1)));
        assertThat(spaeterPos, greaterThan(frueherPos));
        return this;
    }
}
