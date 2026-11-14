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
package suchdienst.integrationstest.validator.organisationunitstitlesv2;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.apache.http.HttpStatus;

public class TitlesValidator {

    @Builder
    @Getter
    public static class Criterion {

        private String name;
        private String code;
        private List<String> auspraegungen;
    }

    private final ValidatableResponse then;

    public TitlesValidator(Response response) {
        this.then = response.then();
    }

    public TitlesValidator validateCount(int expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("$.size()", is(expected));
        return this;
    }

    public TitlesValidator validateId(int index, String expectedId) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("[" + index + "].id", equalTo(expectedId));
        return this;
    }

    public TitlesValidator validateTitle(int index, String expectedTitle) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("[" + index + "].title", equalTo(expectedTitle));
        return this;
    }

    public TitlesValidator validateRole(int index, String expectedCode, String expectedName) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("[" + index + "].role.code", equalTo(expectedCode))
            .body("[" + index + "].role.name", equalTo(expectedName));
        return this;
    }

    public TitlesValidator validateCriteria(int index, List<Criterion> expectedCriteria) {
        String jsonPathPrefix = "[" + index + "].criteria";
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON);
        for (int i = 0; i < expectedCriteria.size(); i++) {
            Criterion c = expectedCriteria.get(i);
            String jsonPathCriterionPrefix = jsonPathPrefix + "[" + i + "]";
            then.body(jsonPathCriterionPrefix + ".name", equalTo(c.getName()));
            then.body(jsonPathCriterionPrefix + ".code", equalTo(c.getCode()));
            then.body(jsonPathCriterionPrefix + ".auspraegungen", equalTo(c.getAuspraegungen()));
        }
        return this;
    }
}
