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
package suchdienst.integrationstest.validator.servicedescriptionv8;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import suchdienst.integrationstest.validator.ResultListValidator;

/**
 * Validator für die Such-API "/v8beta1/servicedescriptions"
 * <p>
 * Analog zu {@link suchdienst.integrationstest.validator.servicedescriptionv7.ServiceDescriptionResultListValidator},
 * aber angepasst an das neue V8-Antwortmodell:
 * <ul>
 *     <li>Die Seiteninformationen liegen nicht mehr direkt unter {@code serviceDescriptions}, sondern werden über
 *     {@code org.springframework.data.web.PagedModel} unter {@code serviceDescriptions.page} ausgeliefert.</li>
 *     <li>Es gibt kein {@code description}-Feld mehr, sondern nur noch {@code teaser}.</li>
 *     <li>Es gibt kein rohes {@code ars}-Feld mehr, sondern nur noch {@code arsFederalstate}.</li>
 * </ul>
 */
public class ServiceDescriptionResultListValidator
    extends ResultListValidator<suchdienst.integrationstest.validator.servicedescriptionv8.ServiceDescriptionResultListValidator>
{

    public ServiceDescriptionResultListValidator(Response response) {
        super(response, "serviceDescriptions.");
    }

    @Override
    public ServiceDescriptionResultListValidator validateEmptyResultList() {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "page.totalElements", comparesEqualTo(0));
        return self();
    }

    public ServiceDescriptionResultListValidator validateTotalElements(int expectedCount) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "page.totalElements", is(expectedCount));
        return self();
    }

    public ServiceDescriptionResultListValidator validatePageNumber(int expectedPageNumber) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "page.number", is(expectedPageNumber));
        return self();
    }

    public ServiceDescriptionResultListValidator validatePageSize(int expectedSize) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "page.size", is(expectedSize));
        return self();
    }

    public ServiceDescriptionResultListValidator validateAvailableLanguages(int index, String... expectedLanguages) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(basepath + "content[" + index + "].availableLanguages", hasItem(expectedLanguages[0]));
        return self();
    }

    public ServiceDescriptionResultListValidator personalMattersIsEmpty() {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("facades.personalMatters", is(empty()));
        return self();
    }

    public ServiceDescriptionResultListValidator personalMattersThirdLevelIsEmpty(String secondLevelCode) {
        String firstLevelCode = secondLevelCode.charAt(0) + "000000";
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                "facades.personalMatters.find{it.code=='" +
                    firstLevelCode +
                    "'}.children.find{it.code=='" +
                    secondLevelCode +
                    "'}.children",
                is(empty())
            );
        return self();
    }

    public ServiceDescriptionResultListValidator personalMattersFirstLevelContainsCode(String expectedCode) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("facades.personalMatters.code", hasItem(expectedCode));
        return self();
    }

    public ServiceDescriptionResultListValidator personalMattersSecondLevelContainsCode(String expectedCode) {
        String firstLevelCode = expectedCode.charAt(0) + "000000";
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                "facades.personalMatters.find{it.code=='" + firstLevelCode + "'}.children.code",
                hasItem(expectedCode)
            );
        return self();
    }

    public ServiceDescriptionResultListValidator personalMattersThirdLevelContainsCode(String expectedCode) {
        String firstLevelCode = expectedCode.charAt(0) + "000000";
        String secondLevelCode = expectedCode.substring(0, 3) + "0000";
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                "facades.personalMatters.find{it.code=='" +
                    firstLevelCode +
                    "'}.children.find{it.code=='" +
                    secondLevelCode +
                    "'}.children.code",
                hasItem(expectedCode)
            );
        return self();
    }

    public ServiceDescriptionResultListValidator personalMattersFirstLevelContainsName(String expectedName) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("facades.personalMatters.name", hasItem(expectedName));
        return self();
    }

    public ServiceDescriptionResultListValidator personalMattersFirstLevelCountByCode(
        int expectedCount,
        String firstLevelCode
    ) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("facades.personalMatters.find{it.code=='" + firstLevelCode + "'}.count", equalTo(expectedCount));
        return self();
    }

    public ServiceDescriptionResultListValidator personalMattersFirstLevelSize(int expectedCount) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("facades.personalMatters.size()", equalTo(expectedCount));
        return self();
    }

    public ServiceDescriptionResultListValidator personalMattersSecondLevelContainsNameByCode(
        String expectedName,
        String expectedCode
    ) {
        String firstLevelCode = expectedCode.charAt(0) + "000000";
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                "facades.personalMatters.find{it.code=='" + firstLevelCode + "'}.children.name",
                hasItem(expectedName)
            );
        return self();
    }

    public ServiceDescriptionResultListValidator personalMattersSecondLevelCountByCode(
        int expectedCount,
        String secondLevelCode
    ) {
        String firstLevelCode = secondLevelCode.charAt(0) + "000000";
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                "facades.personalMatters.find{it.code=='" +
                    firstLevelCode +
                    "'}.children.find{it.code=='" +
                    secondLevelCode +
                    "'}.count",
                equalTo(expectedCount)
            );
        return self();
    }

    public ServiceDescriptionResultListValidator personalMattersThirdLevelContainsNameByCode(
        String expectedName,
        String expectedCode
    ) {
        String firstLevelCode = expectedCode.charAt(0) + "000000";
        String secondLevelCode = expectedCode.substring(0, 3) + "0000";
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                "facades.personalMatters.find{it.code=='" +
                    firstLevelCode +
                    "'}.children.find{it.code=='" +
                    secondLevelCode +
                    "'}.children.name",
                hasItem(expectedName)
            );
        return self();
    }

    public ServiceDescriptionResultListValidator personalMattersThirdLevelCountByCode(
        int expectedCount,
        String thirdLevelCode
    ) {
        String firstLevelCode = thirdLevelCode.charAt(0) + "000000";
        String secondLevelCode = thirdLevelCode.substring(0, 3) + "0000";
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                "facades.personalMatters.find{it.code=='" +
                    firstLevelCode +
                    "'}.children.find{it.code=='" +
                    secondLevelCode +
                    "'}.children.find{it.code=='" +
                    thirdLevelCode +
                    "'}.count",
                equalTo(expectedCount)
            );
        return self();
    }

    public ServiceDescriptionResultListValidator serviceDescriptionsContainsId(String expectedId) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("serviceDescriptions.content.id", hasItem(expectedId));
        return self();
    }

    public ServiceDescriptionResultListValidator serviceDescriptionsContainsOdId(String expectedId) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("serviceDescriptions.content.onlineServiceLinks.id.flatten()", hasItem(expectedId));
        return self();
    }

    public ServiceDescriptionResultListValidator serviceDescriptionsNotContainsOdId(String notExpectedId) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("serviceDescriptions.content.onlineServiceLinks.id.flatten()", not(hasItem(notExpectedId)));
        return self();
    }

    public ServiceDescriptionResultListValidator validateOnlineServicePublicDisplayAllowed(
        String lbFullId,
        String onlinedienstId,
        boolean expected
    ) {
        String pathExpression =
            "serviceDescriptions.content.find {it.id == '" +
            lbFullId +
            "'}.onlineServiceLinks.find {it.id == '" +
            onlinedienstId +
            "'}.isPublicDisplayAllowed";
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body(pathExpression, equalTo(expected));
        return self();
    }

    @Override
    public ServiceDescriptionResultListValidator validateCount(int expectedCount) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("serviceDescriptions.content.size()", is(expectedCount));
        return self();
    }
}
