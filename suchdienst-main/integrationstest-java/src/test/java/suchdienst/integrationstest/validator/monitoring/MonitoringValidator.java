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
package suchdienst.integrationstest.validator.monitoring;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.oneOf;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;

public class MonitoringValidator {

    private final ValidatableResponse then;

    public MonitoringValidator(Response response) {
        this.then = response.then();
    }

    public MonitoringValidator validateMetadataFormat(String boId) {
        String uuidPattern = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
        then.statusCode(200)
            .contentType(ContentType.JSON)
            .body("queryResponse.content.find{it.id=='" + boId + "'}.metaData.id", matchesPattern(uuidPattern))
            .body(
                "queryResponse.content.find{it.id=='" + boId + "'}.metaData.transactionId",
                matchesPattern(uuidPattern)
            )
            .body(
                "queryResponse.content.find{it.id=='" + boId + "'}.metaData.transactionPosition",
                is(oneOf("0", "1", "2", "3"))
            );

        return this;
    }

    public MonitoringValidator validateContainsId(String expectedId) {
        then.statusCode(200).contentType(ContentType.JSON).body("queryResponse.content.id", hasItem(expectedId));
        return this;
    }

    public MonitoringValidator validateNotContainsId(String expectedId) {
        then.statusCode(200)
            .contentType(ContentType.JSON)
            .body("queryResponse.content.id", not(hasItem(expectedId)));
        return this;
    }

    public MonitoringValidator validateMessageId(String boId, String expectedMessageId) {
        then.statusCode(200)
            .contentType(ContentType.JSON)
            .body("queryResponse.content.find{it.id=='" + boId + "'}.metaData", hasEntry("id", expectedMessageId));
        return this;
    }

    public MonitoringValidator validateTransactionId(String boId, String expectedTransactionId) {
        then.statusCode(200)
            .contentType(ContentType.JSON)
            .body(
                "queryResponse.content.find{it.id=='" + boId + "'}.metaData",
                hasEntry("transactionId", expectedTransactionId)
            );
        return this;
    }

    public MonitoringValidator validateTransactionPosition(String boId, String expectedTransactionPosition) {
        then.statusCode(200)
            .contentType(ContentType.JSON)
            .body(
                "queryResponse.content.find{it.id=='" + boId + "'}.metaData",
                hasEntry("transactionPosition", expectedTransactionPosition)
            );
        return this;
    }

    public MonitoringValidator validateLastId(String expectedId) {
        then.statusCode(200).contentType(ContentType.JSON).body("queryResponse.lastId", is(expectedId));
        return this;
    }
}
