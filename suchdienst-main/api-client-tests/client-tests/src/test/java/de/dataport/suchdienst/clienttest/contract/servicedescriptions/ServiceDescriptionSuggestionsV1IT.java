/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.clienttest.contract.servicedescriptions;

import static de.dataport.suchdienst.clienttest.support.HttpContractAssertions.assertApiError;
import static de.dataport.suchdienst.clienttest.support.HttpContractAssertions.assertJsonSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dataport.suchdienst.client.api.LeistungenApi;
import de.dataport.suchdienst.client.model.ServiceDescriptionSuggestion;
import de.dataport.suchdienst.clienttest.fixture.SearchFixture;
import de.dataport.suchdienst.clienttest.fixture.SearchFixtureExtension;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Testziel: GET /v1/servicedescriptions/suggestions/{ars} liefert höchstens fünf passende Fixture-Vorschläge und validiert Eingaben. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SearchFixtureExtension.class)
class ServiceDescriptionSuggestionsV1IT {

    private SearchFixture fixture;
    private LeistungenApi services;

    @BeforeAll
    void initializeApis(SearchFixture sharedFixture) {
        fixture = sharedFixture;
        services = new LeistungenApi(fixture.client());
    }

    @Test
    void returnsAtMostFiveImportedSuggestions() {
        var suggestions = assertJsonSuccess(
            services.getLbSuggestionsWithHttpInfo(SearchFixture.ARS, fixture.query(), true)
        );
        var expected = IntStream.range(0, SearchFixture.COUNT).mapToObj(fixture::title).collect(Collectors.toSet());
        IntStream.range(0, SearchFixture.COUNT).forEach(index ->
            expected.add(fixture.query() + " Beschreibung " + index)
        );
        // Neben Volltiteln erzeugt die Autovervollständigung einzelne Titel-Tokens.
        expected.add(fixture.query());
        assertEquals(5, suggestions.size());
        var actual = suggestions
            .stream()
            .map(ServiceDescriptionSuggestion::getSuggestedText)
            .collect(Collectors.toSet());
        assertEquals(5, actual.size());
        assertTrue(expected.containsAll(actual), "Unerwartete Vorschläge: " + actual);
    }

    @Test
    void completesImportedFullTitles() {
        var suggestions = assertJsonSuccess(
            services.getLbSuggestionsWithHttpInfo(SearchFixture.ARS, fixture.query() + " Leistung", true)
        );
        var expected = IntStream.range(0, SearchFixture.COUNT).mapToObj(fixture::title).collect(Collectors.toSet());
        var actual = suggestions
            .stream()
            .map(ServiceDescriptionSuggestion::getSuggestedText)
            .collect(Collectors.toSet());
        assertEquals(5, suggestions.size());
        assertEquals(5, actual.size());
        assertTrue(expected.containsAll(actual), "Unerwartete Volltitel: " + actual);
    }

    @Test
    void returnsEmptySuggestionsForUnmatchedQuery() {
        assertTrue(services.getLbSuggestions(SearchFixture.ARS, "unmatched" + fixture.query(), true).isEmpty());
    }

    @Test
    void defaultDuplicateHandlingMatchesExplicitDefault() {
        assertEquals(
            services.getLbSuggestions(SearchFixture.ARS, fixture.query(), false),
            services.getLbSuggestions(SearchFixture.ARS, fixture.query(), null)
        );
    }

    @ParameterizedTest(name = "Gültige Query-Länge {0}")
    @ValueSource(ints = { 1, 50 })
    void acceptsQueryLengthBoundaries(int length) {
        String query = length == 1 ? fixture.query().substring(0, 1) : "z".repeat(length);
        var suggestions = assertJsonSuccess(services.getLbSuggestionsWithHttpInfo(SearchFixture.ARS, query, false));
        assertTrue(suggestions.size() <= 5);
    }

    @ParameterizedTest(name = "Ungültige Query-Länge {0}")
    @ValueSource(ints = { 0, 51 })
    void rejectsInvalidQueryLength(int length) {
        assertApiError(400, () -> services.getLbSuggestions(SearchFixture.ARS, "z".repeat(length), false));
    }

    @Test
    void rejectsMissingQueryOnServer() {
        // Rohes HTTP weist serverseitige Pflichtparameterprüfung nach.
        assertApiError(400, () ->
            fixture
                .client()
                .getRestClient()
                .get()
                .uri("/v1/servicedescriptions/suggestions/{ars}", SearchFixture.ARS)
                .retrieve()
                .toBodilessEntity()
        );
    }

    @Test
    void rejectsMalformedArs() {
        assertApiError(400, () -> services.getLbSuggestions("invalid", fixture.query(), false));
    }
}
