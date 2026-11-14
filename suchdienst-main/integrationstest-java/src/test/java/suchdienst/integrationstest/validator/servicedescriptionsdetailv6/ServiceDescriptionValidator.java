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
package suchdienst.integrationstest.validator.servicedescriptionsdetailv6;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;

@SuppressWarnings("UnusedReturnValue")
public class ServiceDescriptionValidator {

    @Builder
    @Getter
    public static class Criterion {

        private String name;
        private String code;
        private List<String> auspraegungen;
    }

    private final ValidatableResponse then;

    public ServiceDescriptionValidator(Response response) {
        this.then = response.then();
    }

    public ServiceDescriptionValidator validateId(String expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("id", equalTo(expected));
        return this;
    }

    public ServiceDescriptionValidator validateName(String expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("name", equalTo(expected));
        return this;
    }

    public ServiceDescriptionValidator validateDetailsCount(String code, int expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("details.findAll{it.code == '" + code + "'}.size()", is(expected));
        return this;
    }

    public ServiceDescriptionValidator validateVolltext(String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("details.findAll{it.xZufiCategory == 'TEXT_MODUL' && it.code == '06'}[0].text", is(expected));
        return this;
    }

    public ServiceDescriptionValidator validateNameContains(String expectedToContain) {
        JsonPath json = then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).extract().jsonPath();
        String actual = StringEscapeUtils.unescapeHtml4(json.getString("name"));
        assertThat(actual, containsStringIgnoringCase(expectedToContain));
        return this;
    }

    public ServiceDescriptionValidator validateNameContainsNot(String notExpectedToContain) {
        JsonPath json = then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).extract().jsonPath();
        String actual = StringEscapeUtils.unescapeHtml4(json.getString("name"));
        assertThat(actual, not(containsStringIgnoringCase(notExpectedToContain)));
        return this;
    }

    public ServiceDescriptionValidator validateReferenzLeiKa(String expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("leikakeys", containsInAnyOrder(expected));
        return this;
    }

    public ServiceDescriptionValidator validateExternalLastUpdate(String expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("externalLastUpdate", equalTo(expected));
        return this;
    }

    public ServiceDescriptionValidator validateLanguageCode(String expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("languageCode", equalTo(expected));
        return this;
    }

    public ServiceDescriptionValidator validateLastUpdateNotEmpty() {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("lastUpdate", not(emptyOrNullString()));
        return this;
    }

    public ServiceDescriptionValidator validateSourcePortalsCount(int expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("details.findAll{it.xZufiCategory == 'URSPRUNGSPORTAL_MODUL'}.size()", is(expected));
        return this;
    }

    public ServiceDescriptionValidator validateSourcePortalFirstUri(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                "details.findAll{it.xZufiCategory == 'URSPRUNGSPORTAL_MODUL'}[" + index + "].links[0].uri",
                equalTo(expected)
            );
        return this;
    }

    public ServiceDescriptionValidator validateSourcePortalFirstTitle(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                "details.findAll{it.xZufiCategory == 'URSPRUNGSPORTAL_MODUL'}[" + index + "].links[0].title",
                equalTo(expected)
            );
        return this;
    }

    public ServiceDescriptionValidator validateSourcePortalTitle(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                "details.findAll{it.xZufiCategory == 'URSPRUNGSPORTAL_MODUL'}[" + index + "].title",
                equalTo(expected)
            );
        return this;
    }

    public ServiceDescriptionValidator validatePeriodCount(int expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("validityPeriods.size()", is(expected));
        return this;
    }

    public ServiceDescriptionValidator validatePeriodValidFrom(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("validityPeriods[" + index + "].validFrom", equalTo(expected));
        return this;
    }

    public ServiceDescriptionValidator validatePeriodValidto(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("validityPeriods[" + index + "].validTo", equalTo(expected));
        return this;
    }

    public ServiceDescriptionValidator validateOrganisationsCount(int expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("organisations.size()", is(expected));
        return this;
    }

    public ServiceDescriptionValidator validateOrganisationsId(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("organisations[" + index + "].id", equalTo(expected));
        return this;
    }

    public ServiceDescriptionValidator validateOrganisationsTitle(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("organisations[" + index + "].title", equalTo(expected));
        return this;
    }

    public ServiceDescriptionValidator validateOrganisationsRole(int index, String expectedCode, String expectedName) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("organisations[" + index + "].role.code", equalTo(expectedCode))
            .body("organisations[" + index + "].role.name", equalTo(expectedName));
        return this;
    }

    public ServiceDescriptionValidator validateOrganisationsCriteria(int index, List<Criterion> expectedCriteria) {
        String jsonPathPrefix = "organisations[" + index + "].criteria";
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

    public ServiceDescriptionValidator validateArsCount(int expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("ars.size()", is(expected));
        return this;
    }

    public ServiceDescriptionValidator validateArs(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("ars[" + index + "]", equalTo(expected));
        return this;
    }

    public ServiceDescriptionValidator validateLeikakeysCount(int expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("leikakeys.size()", is(expected));
        return this;
    }

    public ServiceDescriptionValidator validateLeikakeys(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("leikakeys[" + index + "]", equalTo(expected));
        return this;
    }

    public ServiceDescriptionValidator valdateLanguageCompleteIsEmpty() {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("languageComplete", is(anEmptyMap()));
        return this;
    }

    public ServiceDescriptionValidator validateLanguageCompleteForEN(boolean expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("languageComplete.en", is(expected));
        return this;
    }

    public ServiceDescriptionValidator validateNotFound() {
        then.statusCode(HttpStatus.SC_NOT_FOUND)
            .contentType(ContentType.JSON)
            .body("message", is("Die angegebene Resource konnte nicht gefunden werden."));
        return this;
    }

    public ServiceDescriptionValidator validateEmptyContent() {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("content", Matchers.hasSize(0));
        return this;
    }
}
