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
package suchdienst.integrationstest.validator;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;

@SuppressWarnings("UnusedReturnValue")
public class ErrorValidator {

    private final ValidatableResponse then;

    public ErrorValidator(Response response) {
        this.then = response.then();
    }

    public ErrorValidator validateNotFound() {
        return validateNotFound("Die angegebene Resource konnte nicht gefunden werden.");
    }

    public ErrorValidator validateNotFound(String message) {
        then.statusCode(HttpStatus.SC_NOT_FOUND)
            .contentType(ContentType.JSON)
            .body("code", equalTo("NOT_FOUND"))
            .body("codeNumber", is(1906))
            .body("message", equalTo(message))
            .body("timestamp", not(blankOrNullString()));
        return this;
    }

    public ErrorValidator validateBadRequest() {
        then.statusCode(HttpStatus.SC_BAD_REQUEST)
            .contentType(ContentType.JSON)
            .body("code", equalTo("BAD_REQUEST"))
            .body("codeNumber", is(1901))
            .body("message", containsString("Die übermittelten Request-Parameter entsprechen nicht den Anforderungen."))
            .body("timestamp", not(blankOrNullString()));
        return this;
    }

    public ErrorValidator validateStatusCode(int status) {
        then.statusCode(status);
        return this;
    }

    public ErrorValidator validateConflict(String code, String message) {
        then.statusCode(HttpStatus.SC_CONFLICT)
            .contentType(ContentType.JSON)
            .body("code", equalTo(code))
            .body("codeNumber", is(1204))
            .body("message", equalTo(message))
            .body("timestamp", not(blankOrNullString()));
        return this;
    }

    public ErrorValidator validateField(String key, String expected) {
        then.contentType(ContentType.JSON).body("fields['" + key + "']", equalTo(expected));
        return this;
    }

    public ErrorValidator validateValue(String key, Boolean expected) {
        then.contentType(ContentType.JSON).body(key, is(expected));
        return this;
    }

    public ErrorValidator validateValue(String key, String expected) {
        then.contentType(ContentType.JSON).body(key, equalTo(expected));
        return this;
    }
}
