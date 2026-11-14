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

import static org.hamcrest.Matchers.is;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;

public class UrlValidator {

    private final ValidatableResponse then;

    public UrlValidator(Response response) {
        this.then = response.then();
    }

    public UrlValidator validateCount(int expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("location.urls.size()", is(expected));
        return this;
    }
}
