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
package suchdienst.integrationstest.validator.organisationunitsdetailv5;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;

public class InternetAddressesValidator {

    private final ValidatableResponse then;

    public InternetAddressesValidator(Response response) {
        this.then = response.then();
    }

    public InternetAddressesValidator validateCount(int expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("internetAddresses.size()", is(expected));
        return this;
    }

    public InternetAddressesValidator validateUri(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("internetAddresses[" + index + "].uri", equalTo(expected));
        return this;
    }

    public InternetAddressesValidator validateTitle(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("internetAddresses[" + index + "].title", equalTo(expected));
        return this;
    }

    public InternetAddressesValidator validateTitleNotExist(int index) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("internetAddresses[" + index + "].title", is(nullValue()));
        return this;
    }

    public InternetAddressesValidator validateDescription(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("internetAddresses[" + index + "].description", equalTo(expected));
        return this;
    }

    public InternetAddressesValidator validateDescriptionNotExist(int index) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("internetAddresses[" + index + "].description", is(nullValue()));
        return this;
    }
}
