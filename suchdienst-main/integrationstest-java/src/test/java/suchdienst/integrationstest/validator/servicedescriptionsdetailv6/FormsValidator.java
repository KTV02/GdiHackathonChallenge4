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
package suchdienst.integrationstest.validator.servicedescriptionsdetailv6;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;

/**
 * Validiert Felder an der "forms" Property einer API-Response.
 * Die Validation von null-Werten wird unterstützt, in diesem Fall ist die Erwartung, dass das Feld in der Response fehlt.
 */
public class FormsValidator {

    private final ValidatableResponse then;

    public FormsValidator(Response response) {
        this.then = response.then();
    }

    public FormsValidator validateCount(int expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("forms.size()", is(expected));
        return this;
    }

    public FormsValidator validateId(int index, String expected) {
        validateFormFieldValue(index, "id", expected);
        return this;
    }

    public FormsValidator validateUri(int index, String expected) {
        validateFormFieldValue(index, "uri", expected);
        return this;
    }

    public FormsValidator validateMimeCode(int index, String expected) {
        validateFormFieldValue(index, "mimeCode", expected);
        return this;
    }

    public FormsValidator validateName(int index, String expected) {
        validateFormFieldValue(index, "name", expected);
        return this;
    }

    public FormsValidator validateDescription(int index, String expected) {
        validateFormFieldValue(index, "description", expected);
        return this;
    }

    public FormsValidator validateIds(String... ids) {
        then.body("forms.id", contains(ids));
        return this;
    }

    public FormsValidator validateHasId(String expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("forms.id", hasItem(expected));
        return this;
    }

    public FormsValidator validateNames(String... names) {
        then.body("forms.name", contains(names));
        return this;
    }

    private ValidatableResponse validateFormFieldValue(int index, String field, String expected) {
        String fieldPath = "forms" + "[" + index + "]." + field;
        ValidatableResponse validatableResponse = then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON);
        if (expected != null) {
            return validatableResponse.body(fieldPath, equalTo(expected));
        } else {
            return validatableResponse.body(fieldPath, nullValue());
        }
    }
}
