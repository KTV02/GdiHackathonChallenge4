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
package suchdienst.integrationstest.validator.servicedescriptionsdetailv5;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;

public class PersonalMattersValidator {

    private final ValidatableResponse then;

    public PersonalMattersValidator(Response response) {
        this.then = response.then();
    }

    public PersonalMattersValidator firstLevelExactly(String... expectedCodes) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("personalMatters.code", contains(expectedCodes));
        return this;
    }

    public PersonalMattersValidator secondLevelExactly(String level0Name, String... expectedCodes) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("personalMatters.find{it.code=='" + level0Name + "'}.children.code", contains(expectedCodes));
        return this;
    }

    public PersonalMattersValidator thirdLevelExactly(String level0Code, String level1Code, String... expectedCodes) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                "personalMatters.find{it.code=='" +
                    level0Code +
                    "'}.children.find{it.code=='" +
                    level1Code +
                    "'}.children.code",
                contains(expectedCodes)
            );
        return this;
    }

    public PersonalMattersValidator firstLevelNameByCode(String level0Code, String expectedName) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("personalMatters.find{it.code=='" + level0Code + "'}.name", equalTo(expectedName));
        return this;
    }

    public PersonalMattersValidator secondLevelNameByCode(String level1Code, String expectedName) {
        // aus gegebenen level1Code den firstLevelCode ermitteln
        String firstLevelCode = level1Code.charAt(0) + "000000";
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                "personalMatters.find{it.code=='" +
                    firstLevelCode +
                    "'}.children.find{it.code=='" +
                    level1Code +
                    "'}.name",
                equalTo(expectedName)
            );
        return this;
    }

    public PersonalMattersValidator thirdLevelNameByCode(String level2Code, String expectedName) {
        //aus gegebenen level2Code den level0Code und level1Code ermitteln
        String level0Code = level2Code.charAt(0) + "000000";
        String level1Code = level2Code.substring(0, 3) + "0000";
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                "personalMatters.find{it.code=='" +
                    level0Code +
                    "'}.children.find{it.code=='" +
                    level1Code +
                    "'}.children.find{it.code=='" +
                    level2Code +
                    "'}.name",
                equalTo(expectedName)
            );
        return this;
    }

    public PersonalMattersValidator isEmpty() {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("personalMatters", is(empty()));
        return this;
    }
}
