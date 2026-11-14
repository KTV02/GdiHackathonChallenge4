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

/**
 * Validiert Felder an der "nestedForms" Property einer API-Response.
 * Die Validation von null-Werten wird unterstützt, in diesem Fall ist die Erwartung, dass das Feld in der Response fehlt.
 */
public class NestedFormsValidator {

    private final ValidatableResponse then;

    public NestedFormsValidator(Response response) {
        this.then = response.then();
    }

    public NestedFormsValidator validateCount(int expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("nestedForms.size()", is(expected));
        return this;
    }

    public NestedFormsValidator validateId(int index, String expected) {
        validateNestedFormFieldValue(index, "id", expected);
        return this;
    }

    public NestedFormsValidator validateUri(int index, String expected) {
        validateNestedFormFieldValue(index, "uri", expected);
        return this;
    }

    public NestedFormsValidator validateMimeCode(int index, String expected) {
        validateNestedFormFieldValue(index, "mimeCode", expected);
        return this;
    }

    public NestedFormsValidator validateName(int index, String expected) {
        validateNestedFormFieldValue(index, "name", expected);
        return this;
    }

    public NestedFormsValidator validateDescription(int index, String expected) {
        validateNestedFormFieldValue(index, "description", expected);
        return this;
    }

    private ValidatableResponse validateNestedFormFieldValue(int index, String field, String expected) {
        String fieldPath = "nestedForms" + "[" + index + "]." + field;
        ValidatableResponse validatableResponse = then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON);
        if (expected != null) {
            return validatableResponse.body(fieldPath, equalTo(expected));
        } else {
            return validatableResponse.body(fieldPath, nullValue());
        }
    }
}
