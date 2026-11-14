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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;

public abstract class AdditionalInformationValidator<T extends AdditionalInformationValidator<T>> {

    private final String basepath;

    private final ValidatableResponse then;

    protected AdditionalInformationValidator(Response response, String type) {
        basepath = "additionalInformation.findAll {it.type == '" + type + "'}";
        this.then = response.then();
    }

    protected abstract T self();

    public T validateCount(int expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + ".size()", is(expected));
        return self();
    }

    public T validateTitle(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "[" + index + "].title", equalTo(expected));
        return self();
    }

    public T validateText(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "[" + index + "].text", equalTo(expected));
        return self();
    }

    public T validateTextNotExist(int index) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "[" + index + "].text", is(nullValue()));
        return self();
    }

    public T validateAdditionalInformation(int index, String key, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                basepath + "[" + index + "].additionalInformation.find {it.textKey == '" + key + "'}.textValue",
                equalTo(expected)
            );
        return self();
    }

    public T validateAdditionalInformationNotContains(int index, String textKey) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "[" + index + "].additionalInformation.textKey", not(hasItem(textKey)));
        return self();
    }
}
