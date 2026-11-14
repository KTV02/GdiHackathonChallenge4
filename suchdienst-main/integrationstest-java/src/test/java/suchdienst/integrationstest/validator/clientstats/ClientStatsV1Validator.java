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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;

public class ClientStatsV1Validator {

    private final ValidatableResponse then;

    public ClientStatsV1Validator(Response response) {
        this.then = response.then();
    }

    public ClientStatsV1Validator validateLbCount(String arsPrefix, int cnt) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("stateStatistics.find {it.ars == '" + arsPrefix + "'}", hasEntry("lbs", cnt));
        return this;
    }

    public ClientStatsV1Validator validateOeCount(String arsPrefix, int cnt) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("stateStatistics.find {it.ars == '" + arsPrefix + "'}", hasEntry("oes", cnt));
        return this;
    }

    public ClientStatsV1Validator validateOdCount(String arsPrefix, int cnt) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("stateStatistics.find {it.ars == '" + arsPrefix + "'}", hasEntry("ods", cnt));
        return this;
    }

    public ClientStatsV1Validator validateSpCount(String arsPrefix, int cnt) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("stateStatistics.find {it.ars == '" + arsPrefix + "'}", hasEntry("sps", cnt));
        return this;
    }

    public ClientStatsV1Validator validateLastUpdateNotEmpty(String arsPrefix) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                "stateStatistics.find {it.ars == '" + arsPrefix + "'}",
                allOf(
                    hasEntry(is("lastUpdate"), is(not(emptyOrNullString()))),
                    hasEntry(is("lastUpdateOe"), is(not(emptyOrNullString()))),
                    hasEntry(is("lastUpdateOd"), is(not(emptyOrNullString()))),
                    hasEntry(is("lastUpdateSp"), is(not(emptyOrNullString())))
                )
            );
        return this;
    }
}
