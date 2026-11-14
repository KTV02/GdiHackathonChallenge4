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
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;

public class CodelistEntryValidator {

    private final ValidatableResponse then;

    public CodelistEntryValidator(Response response) {
        this.then = response.then();
    }

    public CodelistEntryValidator validateCount(int expectedCount) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("$.size()", is(expectedCount));
        return this;
    }

    public CodelistEntryValidator validateCode(int index, String code) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("code[" + index + "]", equalTo(code));
        return this;
    }

    public CodelistEntryValidator validateId(int index, String id) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("id[" + index + "]", equalTo(id));
        return this;
    }

    public CodelistEntryValidator validateDownloadOk() {
        then.statusCode(HttpStatus.SC_OK);
        return this;
    }

    public CodelistEntryValidator validateStats() {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("hits.total.value", greaterThan(0))
            .body("aggregations.'top_hits#commonest'.hits.total.value", greaterThan(0));
        return this;
    }
}
