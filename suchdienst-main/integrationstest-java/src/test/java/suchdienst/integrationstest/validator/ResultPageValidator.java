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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import java.util.Map;
import org.apache.http.HttpStatus;

public class ResultPageValidator {

    private final ValidatableResponse then;

    public ResultPageValidator(Response response) {
        this.then = response.then();
    }

    public ResultPageValidator validateContentCount(int expectedCount) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("content.size()", is(expectedCount));
        return this;
    }

    public ResultPageValidator validateMeta(Map<String, Object> expectedMeta) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON);

        for (Map.Entry<String, Object> entry : expectedMeta.entrySet()) {
            then.body("meta." + entry.getKey(), equalTo(entry.getValue()));
        }
        return this;
    }

    public ResultPageValidator validateFirst(Boolean first) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("first", is(first));
        return this;
    }

    public ResultPageValidator validateLast(Boolean last) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("last", is(last));
        return this;
    }

    public ResultPageValidator validateEmpty(Boolean empty) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("empty", is(empty));
        return this;
    }

    public ResultPageValidator validateNumberOfElements(Integer numberOfElements) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("numberOfElements", equalTo(numberOfElements));
        return this;
    }

    public ResultPageValidator validateTotalElements(Integer totalElements) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("totalElements", equalTo(totalElements));
        return this;
    }

    public ResultPageValidator validateTotalPages(Integer totalPages) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("totalPages", equalTo(totalPages));
        return this;
    }

    public ResultPageValidator validateSort(Boolean sorted, Boolean unsorted, Boolean empty) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("pageable.sort.sorted", equalTo(sorted))
            .body("pageable.sort.unsorted", equalTo(unsorted))
            .body("pageable.sort.empty", equalTo(empty));
        return this;
    }

    public ResultPageValidator validatePageableDefaults(Integer pageNumber, Integer pageSize, Integer offset) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("pageable.unpaged", equalTo(false))
            .body("pageable.pageNumber", equalTo(pageNumber))
            .body("pageable.paged", equalTo(true))
            .body("pageable.pageSize", equalTo(pageSize))
            .body("pageable.offset", equalTo(offset));
        return validateSort(false, true, true);
    }
}
