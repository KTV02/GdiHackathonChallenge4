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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;
import suchdienst.integrationstest.validator.LinklistValidator;

public class OnlineServiceLinkValidator extends LinklistValidator<OnlineServiceLinkValidator> {

    private final ValidatableResponse then;

    public OnlineServiceLinkValidator(Response response) {
        super(response, "onlineServiceLinks");
        this.then = response.then();
    }

    public OnlineServiceLinkValidator validateId(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("onlineServiceLinks[" + index + "].id", equalTo(expected));
        return this;
    }

    public OnlineServiceLinkValidator validateHasId(String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("onlineServiceLinks.id", hasItem(expected));
        return this;
    }

    public OnlineServiceLinkValidator validateValidityPeriodsCount(int index, int expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("onlineServiceLinks[" + index + "].validityPeriods.size()", is(expected));
        return this;
    }

    public OnlineServiceLinkValidator validateValidityPeriodsValidFrom(int index, int indexPeriod, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                "onlineServiceLinks[" + index + "].validityPeriods[" + indexPeriod + "].validFrom",
                equalTo(expected)
            );
        return this;
    }

    public OnlineServiceLinkValidator validateValidityPeriodsValidTo(int index, int indexPeriod, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("onlineServiceLinks[" + index + "].validityPeriods[" + indexPeriod + "].validTo", equalTo(expected));
        return this;
    }

    public OnlineServiceLinkValidator validateExecutionLanguagesCount(int index, int expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("onlineServiceLinks[" + index + "].executionLanguages.size()", is(expected));
        return this;
    }

    public OnlineServiceLinkValidator validateExecutionLanguagesContains(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("onlineServiceLinks[" + index + "].executionLanguages", hasItem(expected));
        return this;
    }

    public OnlineServiceLinkValidator validateExecutionNationalitiesCount(int index, int expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("onlineServiceLinks[" + index + "].executionNationalities.size()", is(expected));
        return this;
    }

    public OnlineServiceLinkValidator validateExecutionNationalitiesContains(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("onlineServiceLinks[" + index + "].executionNationalities.destatisCode", hasItem(expected));
        return this;
    }

    public OnlineServiceLinkValidator validateLevelConfidence(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("onlineServiceLinks[" + index + "].levelOfConfidence", equalTo(expected));
        return this;
    }

    public OnlineServiceLinkValidator validatePaymentMethodsCount(int index, int expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("onlineServiceLinks[" + index + "].paymentMethods.size()", is(expected));
        return this;
    }

    public OnlineServiceLinkValidator validatePaymentMethodsContains(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("onlineServiceLinks[" + index + "].paymentMethods", hasItem(expected));
        return this;
    }

    public OnlineServiceLinkValidator validateIdentificationMethodsCount(int index, int expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("onlineServiceLinks[" + index + "].identificationMethods.size()", is(expected));
        return this;
    }

    public OnlineServiceLinkValidator validateIdentificationMethodsContains(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("onlineServiceLinks[" + index + "].identificationMethods", hasItem(expected));
        return this;
    }

    public OnlineServiceLinkValidator validateHelpText(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("onlineServiceLinks[" + index + "].helpText", equalTo(expected));
        return this;
    }
}
