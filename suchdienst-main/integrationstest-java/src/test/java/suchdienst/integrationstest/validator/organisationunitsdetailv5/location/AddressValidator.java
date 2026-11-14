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
package suchdienst.integrationstest.validator.organisationunitsdetailv5.location;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.HttpStatus;

@SuppressWarnings("UnusedReturnValue")
public class AddressValidator {

    private final ValidatableResponse then;

    public AddressValidator(Response response) {
        this.then = response.then();
    }

    public AddressValidator validateCount(int expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("location.addresses.size()", is(expected));
        return this;
    }

    public AddressValidator validateCode(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("location.addresses[" + index + "].code", equalTo(expected));
        return this;
    }

    public AddressValidator validateCity(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("location.addresses[" + index + "].city", equalTo(expected));
        return this;
    }

    public AddressValidator validateHouseNumber(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("location.addresses[" + index + "].houseNumber", equalTo(expected));
        return this;
    }

    public AddressValidator validateType(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("location.addresses[" + index + "].type", equalTo(expected));
        return this;
    }

    public AddressValidator validateZip(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("location.addresses[" + index + "].zip", equalTo(expected));
        return this;
    }

    public AddressValidator validateStreet(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("location.addresses[" + index + "].street", equalTo(expected));
        return this;
    }

    public AddressValidator validateAdditional(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("location.addresses[" + index + "].additional", equalTo(expected));
        return this;
    }

    public AddressValidator validateAccessible(int index, String expected) {
        JsonPath json = then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).extract().jsonPath();
        String actual = StringEscapeUtils.unescapeHtml4(json.getString("location.addresses[" + index + "].accessible"));
        assertThat(actual, equalTo(expected));
        return this;
    }

    public AddressValidator validateAccessibleNotExist(int index) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("location.addresses[" + index + "].accessible", is(nullValue()));
        return this;
    }

    public AddressValidator validateOepnv(int index, String expected) {
        JsonPath json = then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).extract().jsonPath();
        String actual = StringEscapeUtils.unescapeHtml4(json.getString("location.addresses[" + index + "].oepnv"));
        assertThat(actual, equalTo(expected));
        return this;
    }

    public AddressValidator validateParking(int index, String expected) {
        JsonPath json = then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).extract().jsonPath();
        String actual = StringEscapeUtils.unescapeHtml4(json.getString("location.addresses[" + index + "].parking"));
        assertThat(actual, equalTo(expected));
        return this;
    }

    public AddressValidator validateParkingNotExist(int index) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("location.addresses[" + index + "].parking", is(nullValue()));
        return this;
    }

    public AddressValidator validateWheelchairAccessible(int index, boolean expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("location.addresses[" + index + "].wheelchairAccessible", is(expected));
        return this;
    }

    public AddressValidator validateLift(int index, boolean expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("location.addresses[" + index + "].lift", is(expected));
        return this;
    }

    public AddressValidator validateGeo(
        int index,
        String expectedLongitude,
        String expectedLatitude,
        String expectedSystem
    ) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("location.addresses[" + index + "].geo.longitude", equalTo(expectedLongitude))
            .body("location.addresses[" + index + "].geo.latitude", equalTo(expectedLatitude))
            .body("location.addresses[" + index + "].geo.system", equalTo(expectedSystem));
        return this;
    }
}
