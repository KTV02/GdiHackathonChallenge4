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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;

public class EmployeeValidator {

    private final ValidatableResponse then;

    public EmployeeValidator(Response response) {
        this.then = response.then();
    }

    public EmployeeValidator validateCount(int expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("employees.size()", is(expected));
        return this;
    }

    public EmployeeValidator validateName(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("employees[" + index + "].name", equalTo(expected));
        return this;
    }

    public EmployeeValidator validateLastname(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("employees[" + index + "].lastname", equalTo(expected));
        return this;
    }

    public EmployeeValidator validateRoom(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("employees[" + index + "].room", equalTo(expected));
        return this;
    }

    public EmployeeValidator validateAdditionalLinksCount(int index, int expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("employees[" + index + "].additionalLinks.size()", is(expected));
        return this;
    }

    public EmployeeValidator validateRelationCount(int index, int expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("employees[" + index + "].relations.size()", is(expected));
        return this;
    }

    public EmployeeValidator validateRelationLbId(int index, int relationIndex, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("employees[" + index + "].relations[" + relationIndex + "].lbId", equalTo(expected));
        return this;
    }

    public EmployeeValidator validateRelationArs(int index, int relationIndex, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("employees[" + index + "].relations[" + relationIndex + "].ars", equalTo(expected));
        return this;
    }

    public EmployeeValidator validateLocationUrlCount(int index, int expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("employees[" + index + "].location.urls.size()", is(expected));
        return this;
    }

    public EmployeeValidator validateLocationInformationCount(int index, int expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("employees[" + index + "].location.information.size()", is(expected));
        return this;
    }

    public EmployeeValidator validateLocationAddressCount(int index, int expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("employees[" + index + "].location.addresses.size()", is(expected));
        return this;
    }

    public EmployeeValidator validateLocationCommunicationCount(int index, int expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("employees[" + index + "].location.communications.size()", is(expected));
        return this;
    }

    public EmployeeValidator validateLocationCommunicationCode(int index, int communicationIndex, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                "employees[" + index + "].location.communications[" + communicationIndex + "].code",
                equalTo(expected)
            );
        return this;
    }

    public EmployeeValidator validateLocationCommunicationName(int index, int communicationIndex, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                "employees[" + index + "].location.communications[" + communicationIndex + "].name",
                equalTo(expected)
            );
        return this;
    }

    public EmployeeValidator validateLocationCommunicationValue(int index, int communicationIndex, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                "employees[" + index + "].location.communications[" + communicationIndex + "].value",
                equalTo(expected)
            );
        return this;
    }

    public EmployeeValidator validateLocationCommunicationAdditional(
        int index,
        int communicationIndex,
        String expected
    ) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                "employees[" + index + "].location.communications[" + communicationIndex + "].additional",
                equalTo(expected)
            );
        return this;
    }

    public EmployeeValidator validateHerausgeberBezeichnung(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("employees[" + index + "].publisher.name[0]", equalTo(expected));
        return this;
    }

    public EmployeeValidator validateHerausgeberKurzbezeichnung(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("employees[" + index + "].publisher.shortName[0]", equalTo(expected));
        return this;
    }

    public EmployeeValidator validateHerausgeberBeschreibung(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("employees[" + index + "].publisher.description", equalTo(expected));
        return this;
    }
}
