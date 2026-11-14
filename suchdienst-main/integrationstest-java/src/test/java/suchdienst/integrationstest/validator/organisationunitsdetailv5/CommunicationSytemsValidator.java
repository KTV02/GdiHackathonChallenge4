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

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;

public class CommunicationSytemsValidator {

    private final ValidatableResponse then;

    public CommunicationSytemsValidator(Response response) {
        this.then = response.then();
    }

    public CommunicationSytemsValidator validateCount(int expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("communicationSystems.size()", is(expected));
        return this;
    }

    public CommunicationSytemsValidator validateName(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("communicationSystems[" + index + "].name", is(expected));
        return this;
    }

    public CommunicationSytemsValidator validateValue(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("communicationSystems[" + index + "].value", is(expected));
        return this;
    }

    public CommunicationSytemsValidator validateAdditional(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("communicationSystems[" + index + "].additional", equalTo(expected));
        return this;
    }
}
