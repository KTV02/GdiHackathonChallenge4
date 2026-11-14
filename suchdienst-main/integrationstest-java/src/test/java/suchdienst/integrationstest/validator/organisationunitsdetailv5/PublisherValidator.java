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

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;

public class PublisherValidator {

    private final ValidatableResponse then;

    public PublisherValidator(Response response) {
        this.then = response.then();
    }

    public PublisherValidator validateHerausgeberBezeichnung(String expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("publisher.name[0]", equalTo(expected));
        return this;
    }

    public PublisherValidator validateHerausgeberKurzbezeichnung(String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("publisher.shortName[0]", equalTo(expected));
        return this;
    }

    public PublisherValidator validateHerausgeberBeschreibung(String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("publisher.description", equalTo(expected));
        return this;
    }
}
