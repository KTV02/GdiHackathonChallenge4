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
package suchdienst.integrationstest.validator.clientstats;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;

public class ClientStatsV2Validator {

    private final ValidatableResponse then;

    public ClientStatsV2Validator(Response response) {
        this.then = response.then();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    public ClientStatsV2Validator validateLbCount(String arsPrefix, int expectedCount) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("stateStatistics.find {it.ars == '" + arsPrefix + "'}.lbs", hasEntry("count", expectedCount))
            .body(
                "stateStatistics.find {it.ars == '" + arsPrefix + "'}.lbs",
                hasEntry(is("lastUpdate"), is(not(emptyOrNullString())))
            );
        return this;
    }

    public ClientStatsV2Validator validateLbLastNotEmpty(String arsPrefix) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                "stateStatistics.find {it.ars == '" + arsPrefix + "'}.lbs",
                hasEntry(is("lastUpdate"), is(not(emptyOrNullString())))
            );
        return this;
    }

    public ClientStatsV2Validator validateOeCount(String arsPrefix, int expectedCount) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("stateStatistics.find {it.ars == '" + arsPrefix + "'}.oes", hasEntry("count", expectedCount))
            .body(
                "stateStatistics.find {it.ars == '" + arsPrefix + "'}.oes",
                hasEntry(is("lastUpdate"), is(not(emptyOrNullString())))
            );
        return this;
    }

    public ClientStatsV2Validator validateOeLastUpdateNotEmpty(String arsPrefix) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                "stateStatistics.find {it.ars == '" + arsPrefix + "'}.oes",
                hasEntry(is("lastUpdate"), is(not(emptyOrNullString())))
            );
        return this;
    }

    public ClientStatsV2Validator validateOdCount(String arsPrefix, int expectedCount) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("stateStatistics.find {it.ars == '" + arsPrefix + "'}.ods", hasEntry("count", expectedCount))
            .body(
                "stateStatistics.find {it.ars == '" + arsPrefix + "'}.ods",
                hasEntry(is("lastUpdate"), is(not(emptyOrNullString())))
            );
        return this;
    }

    public ClientStatsV2Validator validateOdLastUpdateNotEmpty(String arsPrefix) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                "stateStatistics.find {it.ars == '" + arsPrefix + "'}.ods",
                hasEntry(is("lastUpdate"), is(not(emptyOrNullString())))
            );
        return this;
    }

    public ClientStatsV2Validator validateSpCount(String arsPrefix, int expectedCount) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("stateStatistics.find {it.ars == '" + arsPrefix + "'}.sps", hasEntry("count", expectedCount))
            .body(
                "stateStatistics.find {it.ars == '" + arsPrefix + "'}.sps",
                hasEntry(is("lastUpdate"), is(not(emptyOrNullString())))
            );
        return this;
    }

    public ClientStatsV2Validator validateSpLastUpdateNotEmpty(String arsPrefix) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                "stateStatistics.find {it.ars == '" + arsPrefix + "'}.sps",
                hasEntry(is("lastUpdate"), is(not(emptyOrNullString())))
            );
        return this;
    }

    public ClientStatsV2Validator validateArsNotPresent(String arsPrefix) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("stateStatistics.find {it.ars == '" + arsPrefix + "'}", is(nullValue()));
        return this;
    }
}
