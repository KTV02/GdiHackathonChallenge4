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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;

public class BackofficeElsCodelistValidator {

    private final ValidatableResponse then;

    public BackofficeElsCodelistValidator(Response response) {
        this.then = response.then();
    }

    public BackofficeElsCodelistValidator validateContentCount(int expectedCount) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("content.size()", is(expectedCount));
        return this;
    }

    public BackofficeElsCodelistValidator validateFirst(Boolean first) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("first", is(first));
        return this;
    }

    public BackofficeElsCodelistValidator validateLast(Boolean last) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("last", is(last));
        return this;
    }

    public BackofficeElsCodelistValidator validateTotalElements(Integer totalElements) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("totalElements", equalTo(totalElements));
        return this;
    }

    public BackofficeElsCodelistValidator validateEmpty(Boolean empty) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("empty", is(empty));
        return this;
    }

    public BackofficeElsCodelistValidator validatePageSize(Integer pageSize) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("pageable.pageSize", equalTo(pageSize));
        return this;
    }

    public BackofficeElsCodelistValidator validatePageNumber(Integer pageNumber) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("pageable.pageNumber", equalTo(pageNumber));
        return this;
    }

    public BackofficeElsCodelistValidator validateNumberOfElements(Integer numberOfElements) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("numberOfElements", equalTo(numberOfElements));
        return this;
    }

    public BackofficeElsCodelistValidator validateHasContent(String uri, String version) {
        String pathExpression = "content.findAll {it.uri == '" + uri + "'}.version";
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body(pathExpression, hasItem(version));
        return this;
    }

    public BackofficeElsCodelistValidator validateHasNoContent(String uri, String version) {
        String pathExpression = "content.find {it.uri == '" + uri + "' && it.version == '" + version + "'}";
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body(pathExpression, blankOrNullString());
        return this;
    }

    public BackofficeElsCodelistValidator validateNotEmpty() {
        JsonPath json = then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).extract().jsonPath();
        Integer totalElements = json.getInt("totalElements");
        assertThat(totalElements, greaterThan(0));
        Integer totalPages = json.getInt("totalPages");
        assertThat(totalPages, greaterThan(0));
        Integer numberOfElements = json.getInt("numberOfElements");
        assertThat(numberOfElements, greaterThan(0));
        return this;
    }

    public BackofficeElsCodelistValidator validateMissingAggUriCountGreaterEquals(String uri, int expectedCount) {
        JsonPath json = then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).extract().jsonPath();
        String basepath = "find {it.uri == '" + uri + "'}";
        int count = json.getInt(basepath + ".objectCount");
        assertThat(count, greaterThanOrEqualTo(expectedCount));
        return this;
    }

    public BackofficeElsCodelistValidator validateMissingAggVersionCount(
        String uri,
        String version,
        int expectedVersionCount
    ) {
        JsonPath json = then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).extract().jsonPath();
        String basepath = "find {it.uri == '" + uri + "'}";
        int count = json.getInt(basepath + ".countByVersion." + version);
        assertThat(count, equalTo(expectedVersionCount));
        return this;
    }
}
