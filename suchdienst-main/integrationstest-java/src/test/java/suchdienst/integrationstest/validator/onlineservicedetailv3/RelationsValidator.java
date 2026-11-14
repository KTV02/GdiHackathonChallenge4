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
package suchdienst.integrationstest.validator.onlineservicedetailv3;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;

public class RelationsValidator {

    private final ValidatableResponse then;

    public RelationsValidator(Response response) {
        this.then = response.then();
    }

    public RelationsValidator validateCount(int expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("relations.size()", is(expected));
        return this;
    }

    public RelationsValidator validateRelationId(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("relations[" + index + "].id", equalTo(expected));
        return this;
    }

    public RelationsValidator validateHasRelationId(String expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("relations.id", hasItem(expected));
        return this;
    }

    public RelationsValidator validateHasNotRelationId(String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("relations.id", not(hasItem(expected)));
        return this;
    }

    public RelationsValidator validateLbId(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("relations[" + index + "].lbId", equalTo(expected));
        return this;
    }

    public RelationsValidator validateArs(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("relations[" + index + "].ars", equalTo(expected));
        return this;
    }

    public RelationsValidator validateParentId(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("relations[" + index + "].parentId", equalTo(expected));
        return this;
    }
}
