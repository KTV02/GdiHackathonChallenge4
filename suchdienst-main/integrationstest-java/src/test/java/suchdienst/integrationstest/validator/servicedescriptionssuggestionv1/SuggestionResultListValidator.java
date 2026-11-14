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
package suchdienst.integrationstest.validator.servicedescriptionssuggestionv1;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import com.google.common.collect.Lists;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;

public class SuggestionResultListValidator {

    private final ValidatableResponse then;

    public SuggestionResultListValidator(Response response) {
        this.then = response.then();
    }

    public SuggestionResultListValidator validateCount(int expectedCount) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("suggestedText", hasSize(expectedCount));
        return this;
    }

    public SuggestionResultListValidator validateContainsAnyOf(List<String> itemList) {
        List<Matcher<Iterable<? super String>>> matcherList = new ArrayList<>();
        for (String item : itemList) {
            matcherList.add(hasItem(item));
        }
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("suggestedText", anyOf(Lists.newArrayList(matcherList)));
        return this;
    }

    public SuggestionResultListValidator validateListContains(List<String> expectedList) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("suggestedText", hasItems(expectedList.toArray()));
        return this;
    }

    public SuggestionResultListValidator validateNotContainsHtmlTag(String tag) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("suggestedText", not(hasItem(containsString(tag))));
        return this;
    }

    public SuggestionResultListValidator validateNotContains(String unexpected) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("suggestedText", not(hasItem(unexpected)));
        return this;
    }

    public SuggestionResultListValidator validateContainsNoneOf(List<String> unexpectedItems) {
        List<Matcher<Iterable<? super String>>> matcherList = new ArrayList<>();
        for (String item : unexpectedItems) {
            matcherList.add(not(hasItem(item)));
        }

        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("suggestedText", allOf(Lists.newArrayList(matcherList)));
        return this;
    }
}
