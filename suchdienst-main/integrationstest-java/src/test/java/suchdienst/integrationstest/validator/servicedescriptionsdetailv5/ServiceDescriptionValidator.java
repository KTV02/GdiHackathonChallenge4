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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import java.util.List;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;

@SuppressWarnings("UnusedReturnValue")
public class ServiceDescriptionValidator {

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

    public ServiceDescriptionValidator validateDescription(String expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("description", equalTo(expected));
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

    public ServiceDescriptionValidator validateTimestampNotEmpty() {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("timestamp", not(emptyOrNullString()));
        return this;
    }

    public ServiceDescriptionValidator validateLastUpdateNotEmpty() {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("lastUpdate", not(emptyOrNullString()));
        return this;
    }

    public ServiceDescriptionValidator validateSourcePortalsCount(int expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("sourcePortals.size()", is(expected));
        return this;
    }

    public ServiceDescriptionValidator validateSourcePortalUri(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("sourcePortals[" + index + "].uri", equalTo(expected));
        return this;
    }

    public ServiceDescriptionValidator validateSourcePortalTitle(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("sourcePortals[" + index + "].title", equalTo(expected));
        return this;
    }

    public ServiceDescriptionValidator validateSourcePortalReplace(int index, boolean expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("sourcePortals[" + index + "].replace", is(expected));
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

    public ServiceDescriptionValidator validateOrganisationIdsCount(int expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("organisationIds.size()", is(expected));
        return this;
    }

    public ServiceDescriptionValidator validateOrganisationIds(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("organisationIds[" + index + "]", equalTo(expected));
        return this;
    }

    public ServiceDescriptionValidator validateOrganisationIdsContains(String expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("organisationIds", hasItem(expected));
        return this;
    }

    public ServiceDescriptionValidator validateAddresseesCount(int expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("addressees.size()", is(expected));
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

    public ServiceDescriptionValidator validateArsFederalstateCount(int expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("arsFederalstate.size()", is(expected));
        return this;
    }

    public ServiceDescriptionValidator validateArsFederalstateListContains(List<String> expectedList) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("arsFederalstate", hasItems(expectedList.toArray()));
        return this;
    }

    public ServiceDescriptionValidator validateArsFederalstate(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("arsFederalstate[" + index + "]", equalTo(expected));
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

    public ServiceDescriptionValidator validateSynonymsCount(int expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("synonyms.size()", is(expected));
        return this;
    }

    public ServiceDescriptionValidator validateSynonyms(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("synonyms[" + index + "]", equalTo(expected));
        return this;
    }

    public ServiceDescriptionValidator validateSynonymsContains(String expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("synonyms", hasItem(expected));
        return this;
    }

    public ServiceDescriptionValidator validateSuggestCount(int expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("suggest.size()", is(expected));
        return this;
    }

    public ServiceDescriptionValidator validateSuggest(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("suggest[" + index + "]", equalTo(expected));
        return this;
    }

    public ServiceDescriptionValidator validateSuggestContains(String expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("suggest", hasItem(expected));
        return this;
    }

    public ServiceDescriptionValidator validateSdgInformationCount(int expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("sdgInformation.size()", is(expected));
        return this;
    }

    public ServiceDescriptionValidator validateSdgInformationCode(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("sdgInformation[" + index + "].code", equalTo(expected));
        return this;
    }

    public ServiceDescriptionValidator validateSdgInformationTextsByKey(int index, String key, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("sdgInformation[" + index + "].texts." + key, equalTo(expected));
        return this;
    }

    public ServiceDescriptionValidator validateTypingCount(int expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("typing.size()", is(expected));
        return this;
    }

    public ServiceDescriptionValidator validateTypingCode(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("typing[" + index + "].code", equalTo(expected));
        return this;
    }

    public ServiceDescriptionValidator validateTypingName(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("typing[" + index + "].name", equalTo(expected));
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
