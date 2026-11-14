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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import com.google.common.collect.Lists;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

public abstract class LinklistValidator<T> {

    private final ValidatableResponse then;
    private final String linklistPath;

    @SuppressWarnings("unchecked")
    protected T self() {
        return (T) this;
    }

    public LinklistValidator(Response response, String linklistPath) {
        this.then = response.then();
        this.linklistPath = linklistPath;
    }

    public T validateCount(int expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(linklistPath + ".size()", is(expected));
        return self();
    }

    public T validateUri(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(linklistPath + "[" + index + "].uri", is(expected));
        return self();
    }

    public T validateUriWithParams(int index, String expectedUri, List<String> expectedParams) {
        List<Matcher<String>> paramMatcher = expectedParams
            .stream()
            .map(Matchers::containsString)
            .collect(Collectors.toList());
        paramMatcher.add(startsWith(expectedUri));
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(linklistPath + "[" + index + "].uri", allOf(Lists.newArrayList(paramMatcher)));
        return self();
    }

    public T validateHasUri(String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(linklistPath + ".uri", hasItem(expected));
        return self();
    }

    public T validateContainsNoUri(int index) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(linklistPath + "[" + index + "].uri", is(nullValue()));
        return self();
    }

    public T validateTitle(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(linklistPath + "[" + index + "].title", is(expected));
        return self();
    }

    public T validateContainsNoTitle(int index) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(linklistPath + "[" + index + "].title", is(nullValue()));
        return self();
    }

    public T validateDescription(int index, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(linklistPath + "[" + index + "].description", is(expected));
        return self();
    }

    public T validateHasDescription(String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(linklistPath + ".description", hasItem(expected));
        return self();
    }

    public T validateContainsNoDescription(int index) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(linklistPath + "[" + index + "].description", is(nullValue()));
        return self();
    }

    public T validateReplace(int index, boolean expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(linklistPath + "[" + index + "].replace", is(expected));
        return self();
    }

    public T validateDynamicParametersCount(int index, int expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(linklistPath + "[" + index + "].dynamicParameters.size()", is(expected));
        return self();
    }

    public T validateDynamicParametersTypCode(int index, int parameterIndex, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                linklistPath + "[" + index + "].dynamicParameters[" + parameterIndex + "].typ.code",
                equalTo(expected)
            );
        return self();
    }

    public T validateDynamicParametersParameterName(int index, int parameterIndex, String expected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                linklistPath + "[" + index + "].dynamicParameters[" + parameterIndex + "].parameterName",
                equalTo(expected)
            );
        return self();
    }

    public T validateContainsNoParameterName(int index, int parameterIndex) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                linklistPath + "[" + index + "].dynamicParameters[" + parameterIndex + "].parameterName",
                is(nullValue())
            );
        return self();
    }
}
