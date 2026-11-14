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

public class BankAccountValidator {

    private final ValidatableResponse then;

    public BankAccountValidator(Response response) {
        this.then = response.then();
    }

    public BankAccountValidator validateCount(int expected) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("bankAccounts.size()", is(expected));
        return this;
    }

    public BankAccountValidator validateReceiver(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("bankAccounts[" + index + "].receiver", equalTo(expected));
        return this;
    }

    public BankAccountValidator validateIban(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("bankAccounts[" + index + "].iban", equalTo(expected));
        return this;
    }

    public BankAccountValidator validateBankName(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("bankAccounts[" + index + "].bankName", equalTo(expected));
        return this;
    }

    public BankAccountValidator validateBic(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("bankAccounts[" + index + "].bic", equalTo(expected));
        return this;
    }
}
