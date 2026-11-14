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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.HttpStatus;

public class OrganisationValidator {

    private final ValidatableResponse then;

    public OrganisationValidator(Response response) {
        this.then = response.then();
    }

    public OrganisationValidator validateTitle(String expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("title", equalTo(expected));
        return this;
    }

    public OrganisationValidator validateDescription(String expected) {
        JsonPath json = then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).extract().jsonPath();
        String actual = StringEscapeUtils.unescapeHtml4(json.getString("description"));
        assertThat(actual, equalTo(expected));
        return this;
    }

    public OrganisationValidator validateDescriptionContains(String expectedToContain) {
        JsonPath json = then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).extract().jsonPath();
        String actual = StringEscapeUtils.unescapeHtml4(json.getString("description"));
        assertThat(actual, containsStringIgnoringCase(expectedToContain));
        return this;
    }

    public OrganisationValidator validateDescriptionContainsNot(String notExpectedToContain) {
        JsonPath json = then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).extract().jsonPath();
        String actual = StringEscapeUtils.unescapeHtml4(json.getString("description"));
        assertThat(actual, not(containsStringIgnoringCase(notExpectedToContain)));
        return this;
    }

    public OrganisationValidator validateShortDescription(String expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("shortDescription", equalTo(expected));
        return this;
    }

    public OrganisationValidator validateShortDescriptionNotExists() {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("shortDescription", is(nullValue()));
        return this;
    }

    public OrganisationValidator validateLanguageCompleteIsEmpty() {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("languageComplete", is(anEmptyMap()));
        return this;
    }

    public OrganisationValidator validateLanguageComplete(String language, Boolean expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("languageComplete." + language, is(expected));
        return this;
    }

    /**
     * Validator findet nur Anwendung bei `/v3/organisationunits/detail`
     */
    public OrganisationValidator validateRoleCode(String expectedCode) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("role.code", equalTo(expectedCode));
        return this;
    }

    /**
     * Validator findet nur Anwendung bei `/v3/organisationunits/detail`
     */
    public OrganisationValidator validateRoleName(String expectedName) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("role.name", equalTo(expectedName));
        return this;
    }
}
