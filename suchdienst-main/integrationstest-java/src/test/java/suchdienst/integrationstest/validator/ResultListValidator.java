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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import com.google.common.collect.Lists;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;

@SuppressWarnings("UnusedReturnValue")
public abstract class ResultListValidator<T> {

    protected final ValidatableResponse then;
    protected final String basepath;

    @SuppressWarnings("unchecked")
    protected T self() {
        return (T) this;
    }

    public ResultListValidator(Response response) {
        this(response, "");
    }

    public ResultListValidator(Response response, String base) {
        this.then = response.then();
        basepath = base;
    }

    public T validateNoContent() {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "content.id", is(empty()));
        return self();
    }

    public T validateContains(String expectedId) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "content.id", hasItem(expectedId));
        return self();
    }

    public T validateContains(List<String> expectedIds) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "content.id", hasItems(expectedIds.toArray(String[]::new)));
        return self();
    }

    public T validateNotContains(String notExpectedId) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "content.id", not(hasItem(notExpectedId)));
        return self();
    }

    public T validateContainsNoneOf(List<String> unexpectedIds) {
        List<Matcher<Iterable<? super String>>> matcherList = new ArrayList<>();
        for (String id : unexpectedIds) {
            matcherList.add(not(hasItem(id)));
        }

        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "content.id", allOf(Lists.newArrayList(matcherList)));
        return self();
    }

    public T validateIdList(List<String> idList) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "content.id", is(idList));
        return self();
    }

    public T validateODLinkIsFilled() {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "content.onlineServiceLinks", everyItem(is(not(empty()))));
        return self();
    }

    public T validateEmptyResultList() {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "totalElements", comparesEqualTo(0));
        return self();
    }

    public T validateName(int index, String expectedName) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "content[" + index + "].name", equalTo(expectedName));
        return self();
    }

    public T validateDescription(int index, String expectedDescription) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "content[" + index + "].description", equalTo(expectedDescription));
        return self();
    }

    public T validateLanguageCompleteIsEmpty(int index) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "content[" + index + "].languageComplete", is(anEmptyMap()));
        return self();
    }

    public T validateLanguageComplete(int index, String language, Boolean expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "content[" + index + "].languageComplete." + language, is(expected));
        return self();
    }

    public T validateCount(int expectedCount) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "content.size()", is(expectedCount));
        return self();
    }

    public T validateIdsCount(String lbFullId, int expectedCount) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "content.findAll {it.id == '" + lbFullId + "'}.size()", is(expectedCount));
        return self();
    }

    public T validateSourcePortalsCount(int index, int expectedCount) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "content[" + index + "].sourcePortals.size()", is(expectedCount));
        return self();
    }

    public T validateSourcePortalsUri(int index, int portalIndex, String expectedUri) {
        JsonPath json = then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).extract().jsonPath();
        String actual = StringEscapeUtils.unescapeHtml4(
            json.getString(basepath + "content[" + index + "].sourcePortals[" + portalIndex + "].uri")
        );
        assertThat(actual, equalTo(expectedUri));
        return self();
    }

    public T validateSourcePortalsTitle(int index, int portalIndex, String expectedTitle) {
        JsonPath json = then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).extract().jsonPath();
        String actual = StringEscapeUtils.unescapeHtml4(
            json.getString(basepath + "content[" + index + "].sourcePortals[" + portalIndex + "].title")
        );
        assertThat(actual, equalTo(expectedTitle));
        return self();
    }

    public T validateOnlineServiceLinksCount(int index, int expectedCount) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "content[" + index + "].onlineServiceLinks.size()", is(expectedCount));
        return self();
    }

    public T validateOnlineServiceLinkStatus(int indexContent, int indexOnlineServiceLink, String expectedStatus) {
        String path =
            basepath + "content[" + indexContent + "].onlineServiceLinks[" + indexOnlineServiceLink + "].status";
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body(path, equalTo(expectedStatus));
        return self();
    }

    public T validateOnlineServiceLinksCount(String lbFullId, int expectedCount) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "content.find {it.id == '" + lbFullId + "'}.onlineServiceLinks.size()", is(expectedCount));
        return self();
    }

    public T validateOnlineServiceLinksUri(int indexContent, int indexOnlineServiceLinks, String expectedUri) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                basepath + "content[" + indexContent + "].onlineServiceLinks[" + indexOnlineServiceLinks + "].uri",
                equalTo(expectedUri)
            );
        return self();
    }

    public T validateOnlineServiceLinkHasUri(String lbFullId, String expectedUri) {
        String pathExpression = basepath + "content.find {it.id == '" + lbFullId + "'}.onlineServiceLinks.uri";
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body(pathExpression, hasItem(expectedUri));
        return self();
    }

    public T validateOnlineServiceLinksContainsId(String lbFullId, String expectedOdId) {
        String pathExpression = basepath + "content.find {it.id == '" + lbFullId + "'}.onlineServiceLinks.id";
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body(pathExpression, hasItem(expectedOdId));
        return self();
    }

    public T validateOnlineServiceLinksNotContainsId(String lbFullId, String unexpectedOdId) {
        String pathExpression = basepath + "content.find {it.id == '" + lbFullId + "'}.onlineServiceLinks.id";
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(pathExpression, not(hasItem(unexpectedOdId)));
        return self();
    }

    public T validateOnlineServicePublicDisplayAllowed(String lbFullId, String onlinedienstId, boolean expected) {
        String pathExpression =
            basepath +
            "content.find {it.id == '" +
            lbFullId +
            "'}.onlineServiceLinks.find {it.id == '" +
            onlinedienstId +
            "'}.isPublicDisplayAllowed";
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body(pathExpression, equalTo(expected));
        return self();
    }

    public T validateContainsNoLinksUri(int indexContent, int indexOnlineServiceLinks) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                basepath + "content[" + indexContent + "].onlineServiceLinks[" + indexOnlineServiceLinks + "].uri",
                is(nullValue())
            );
        return self();
    }

    public T validateOnlineServiceLinksTitle(int indexContent, int indexOnlineServiceLinks, String expectedTitle) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                basepath + "content[" + indexContent + "].onlineServiceLinks[" + indexOnlineServiceLinks + "].title",
                equalTo(expectedTitle)
            );
        return self();
    }

    public T validateContainsNoLinksTitle(int indexContent, int indexOnlineServiceLinks) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                basepath + "content[" + indexContent + "].onlineServiceLinks[" + indexOnlineServiceLinks + "].title",
                is(nullValue())
            );
        return self();
    }

    public T validateOnlineServiceLinksDescription(
        int indexContent,
        int indexOnlineServiceLinks,
        String expectedDescription
    ) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                basepath +
                    "content[" +
                    indexContent +
                    "].onlineServiceLinks[" +
                    indexOnlineServiceLinks +
                    "].description",
                equalTo(expectedDescription)
            );
        return self();
    }

    public T validateContainsNoLinkDescription(int indexContent, int indexOnlineServiceLinks) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                basepath +
                    "content[" +
                    indexContent +
                    "].onlineServiceLinks[" +
                    indexOnlineServiceLinks +
                    "].description",
                is(nullValue())
            );
        return self();
    }

    public T validateExactMatch(boolean expectedExactValue) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("meta.exact", equalTo(expectedExactValue));
        return self();
    }

    public T validateTeaser(int indexContent, String expectedTeaser) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "content[" + indexContent + "].teaser", equalTo(expectedTeaser));
        return self();
    }

    public T serviceDescriptionsNotContainsId(String expectedId) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "content.id", not(hasItem(expectedId)));
        return self();
    }

    public T validateArs(int indexContent, int indexArs, String expectedArs) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "content[" + indexContent + "].ars[" + indexArs + "]", equalTo(expectedArs));
        return self();
    }

    public T validateArsFederalListSize(int indexContent, int size) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "content[" + indexContent + "].arsFederalstate.size()", is(size));
        return self();
    }

    public T validateArsFederalListContains(int indexContent, List<String> expectedList) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "content[" + indexContent + "].arsFederalstate", hasItems(expectedList.toArray()));
        return self();
    }
}
