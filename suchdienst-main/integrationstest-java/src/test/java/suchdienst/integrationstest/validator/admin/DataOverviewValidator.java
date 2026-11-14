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
package suchdienst.integrationstest.validator.admin;

import static org.hamcrest.Matchers.hasEntry;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;

public class DataOverviewValidator {

    private final ValidatableResponse then;

    public DataOverviewValidator(Response response) {
        this.then = response.then();
    }

    public DataOverviewValidator validateAchemeAgencyId(String name, String expected) {
        return validateKey(name, expected, "schemeAgencyId");
    }

    public DataOverviewValidator validateLbCount(String name, int expected) {
        return validateKey(name, expected, "lbCount");
    }

    public DataOverviewValidator validateOeCount(String name, int expected) {
        return validateKey(name, expected, "oeCount");
    }

    public DataOverviewValidator validateOdCount(String name, int expected) {
        return validateKey(name, expected, "odCount");
    }

    public DataOverviewValidator validateSpCount(String name, int expected) {
        return validateKey(name, expected, "spCount");
    }

    public DataOverviewValidator validateZsCount(String name, int expected) {
        return validateKey(name, expected, "zsCount");
    }

    public DataOverviewValidator validateVlCount(String name, int expected) {
        return validateKey(name, expected, "vlCount");
    }

    private DataOverviewValidator validateKey(String name, int expected, String key) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("find {it.name == '" + name + "'}", hasEntry(key, expected));
        return this;
    }

    private DataOverviewValidator validateKey(String name, String expected, String key) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("find {it.name == '" + name + "'}", hasEntry(key, expected));
        return this;
    }
}
