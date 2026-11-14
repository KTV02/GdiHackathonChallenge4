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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;
import suchdienst.integrationstest.common.Constants;

public class PagedModelValidator {

    private final ValidatableResponse then;

    public PagedModelValidator(Response response, Constants.XzufiVersion xzufiVersion) {
        this.then = response.then().contentType("application/json;xzufi-version=" + xzufiVersion);
    }

    public PagedModelValidator validateContentCount(int expectedCount) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("content.size()", is(expectedCount));
        return this;
    }

    public PagedModelValidator validatePage(Integer page) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("page.number", equalTo(page));
        return this;
    }

    public PagedModelValidator validateSize(Integer size) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("page.size", equalTo(size));
        return this;
    }

    public PagedModelValidator validateTotalElements(Integer totalElements) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("page.totalElements", equalTo(totalElements));
        return this;
    }

    public PagedModelValidator validateTotalPages(Integer totalPages) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("page.totalPages", equalTo(totalPages));
        return this;
    }
}
