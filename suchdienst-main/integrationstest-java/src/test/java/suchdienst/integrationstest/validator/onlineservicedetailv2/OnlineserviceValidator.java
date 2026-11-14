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
package suchdienst.integrationstest.validator.onlineservicedetailv2;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.HttpStatus;

public class OnlineserviceValidator {

    private final ValidatableResponse then;

    public OnlineserviceValidator(Response response) {
        this.then = response.then();
    }

    public OnlineserviceValidator validateName(String expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("name", equalTo(expected));
        return this;
    }

    public OnlineserviceValidator validateDescription(String expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("description", equalTo(expected));
        return this;
    }

    public OnlineserviceValidator validateDescriptionContains(String expectedToContain) {
        JsonPath json = then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).extract().jsonPath();
        String actual = StringEscapeUtils.unescapeHtml4(json.getString("description"));
        assertThat(actual, containsStringIgnoringCase(expectedToContain));
        return this;
    }

    public OnlineserviceValidator validateDescriptionContainsNot(String notExpectedToContain) {
        JsonPath json = then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).extract().jsonPath();
        String actual = StringEscapeUtils.unescapeHtml4(json.getString("description"));
        assertThat(actual, not(containsStringIgnoringCase(notExpectedToContain)));
        return this;
    }

    public OnlineserviceValidator validateHelpText(String expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("helpText", equalTo(expected));
        return this;
    }

    public OnlineserviceValidator validatePaymentMethodCount(int expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("paymentMethods.size()", is(expected));
        return this;
    }

    public OnlineserviceValidator validatePaymentMethodContains(String expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("paymentMethods", hasItem(expected));
        return this;
    }

    public OnlineserviceValidator validateLevelConfidence(String expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("levelOfConfidence", equalTo(expected));
        return this;
    }

    public OnlineserviceValidator validateIdentificationMethodsCount(int expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("identificationMethods.size()", is(expected));
        return this;
    }

    public OnlineserviceValidator validateIdentificationMethodsContains(String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("identificationMethods", hasItem(expected));
        return this;
    }

    public OnlineserviceValidator validateExecutionLanguagesCount(int expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("executionLanguages.size()", is(expected));
        return this;
    }

    public OnlineserviceValidator validateExecutionLanguagesContains(String expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("executionLanguages", hasItem(expected));
        return this;
    }

    public OnlineserviceValidator validateLanguagesCount(int expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("languages.size()", is(expected));
        return this;
    }

    public OnlineserviceValidator validateLanguagesContains(String expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("languages", hasItem(expected));
        return this;
    }

    public OnlineserviceValidator validateLanguageComplete(String language, boolean expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("languageComplete." + language, is(expected));
        return this;
    }

    public OnlineserviceValidator validateLanguageCompleteIsEmpty() {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("languageComplete", is(anEmptyMap()));
        return this;
    }

    public OnlineserviceValidator validateExecutionNationalitiesCount(int expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("executionNationalities.size()", is(expected));
        return this;
    }

    public OnlineserviceValidator validateExecutionNationalitiesContains(String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("executionNationalities.destatisCode", hasItem(expected));
        return this;
    }

    /**
     * Validator findet nur Anwendung bei `/v2/onlineservices/detail`
     */
    public OnlineserviceValidator validateLogo(String expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("logos", hasItem(expected));
        return this;
    }

    public OnlineserviceValidator validateParametersCount(int expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("parameters.size()", is(expected));
        return this;
    }

    public OnlineserviceValidator validateParameterNamesContains(String expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("parameters.name", hasItem(expected));
        return this;
    }

    public OnlineserviceValidator validateParameterValuesContains(String expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("parameters.value", hasItem(expected));
        return this;
    }
}
