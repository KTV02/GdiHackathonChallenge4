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
package suchdienst.integrationstest.validator.admin;

import static org.hamcrest.Matchers.equalTo;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;

public class CodelistEntryByIdValidator {

    private final ValidatableResponse then;

    public CodelistEntryByIdValidator(Response response) {
        this.then = response.then();
    }

    public CodelistEntryByIdValidator validateUri(String uri) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("uri", equalTo(uri));
        return this;
    }

    public CodelistEntryByIdValidator validateVersion(String version) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("version", equalTo(version));
        return this;
    }
}
