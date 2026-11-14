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
package suchdienst.integrationstest.validator.locationsv2;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;

public class LocationsValidator {

    private final ValidatableResponse then;

    public LocationsValidator(Response response) {
        this.then = response.then();
    }

    public LocationsValidator validateNonEmptyBody() {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("$.size()", greaterThan(0));
        return this;
    }

    public LocationsValidator validateCount(int expectedCount) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("$.size()", is(expectedCount));
        return this;
    }

    public LocationsValidator validateName(int index, String expectedName) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("[" + index + "].name", equalTo(expectedName));
        return this;
    }

    public LocationsValidator validateArs(int index, String expectedArs) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("[" + index + "].ars", equalTo(expectedArs));
        return this;
    }

    public LocationsValidator validateZip(int index, String expectedZip) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("[" + index + "].zip", equalTo(expectedZip));
        return this;
    }

    public LocationsValidator validateZipNotEmpty(int index) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("[" + index + "].zip", not(empty()));
        return this;
    }

    public LocationsValidator validateContainsName(String town) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("name", hasItem(town));
        return this;
    }
}
