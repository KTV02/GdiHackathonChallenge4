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
package suchdienst.integrationstest.validator.pushxzufi22queue;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;

public class QueueValidator {

    private final ValidatableResponse then;

    public QueueValidator(Response response) {
        this.then = response.then();
    }

    public QueueValidator validateTotalElements(Integer totalElements) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("totalElements", equalTo(totalElements));
        return this;
    }

    public QueueValidator validateContentCount(Integer count) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("content.size()", equalTo(count));
        return this;
    }

    public QueueValidator validateContains(String expectedId) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("content.id", hasItem(expectedId));
        return this;
    }

    public QueueValidator validateNotContains(String notExpectedId) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("content.id", not(hasItem(notExpectedId)));
        return this;
    }
}
