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
package de.dataport.suchdienst.clienttest.contract.locations;

import static de.dataport.suchdienst.clienttest.support.HttpContractAssertions.assertApiError;
import static de.dataport.suchdienst.clienttest.support.HttpContractAssertions.assertJsonSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.dataport.suchdienst.client.api.OrteBkgApi;
import de.dataport.suchdienst.client.model.GeoSuggestionOutModel;
import de.dataport.suchdienst.clienttest.fixture.LocationFixture;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Testziel: GET /v3/locations/suggestions bindet Namen/Gewichte, Präfixtransport, Defaults und Grenzen. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LocationSuggestionsV3IT {

    private LocationFixture fixture;
    private OrteBkgApi api;

    @BeforeAll
    void startGeocoder() {
        fixture = new LocationFixture();
        api = new OrteBkgApi(fixture.client());
    }

    @AfterAll
    void stopGeocoder() {
        if (fixture != null) fixture.close();
    }

    @Test
    void bindsControlledNamesAndWeights() {
        var result = assertJsonSuccess(api.autocompleteLocationsWithHttpInfo(fixture.query(), null));
        assertEquals(List.of("SDK-Ort", "SDK-Ort Süd"), result.stream().map(GeoSuggestionOutModel::getName).toList());
        assertEquals(2.5f, result.getFirst().getWeight());
        assertEquals(1.75f, result.get(1).getWeight());
        fixture.verifyQuery("/suggest", fixture.query());
    }

    @Test
    void defaultLimitMatchesExplicitDefault() {
        assertEquals(api.autocompleteLocations(fixture.query(), null), api.autocompleteLocations(fixture.query(), 10));
    }

    @Test
    void prependsMissingParentAndRespectsLimit() {
        var input = "parent" + fixture.query();
        fixture.stubSuggestions(
            input,
            List.of(
                Map.of("suggestion", input + " - Ost", "score", 2.5),
                Map.of("suggestion", input + " - West", "score", 1.75)
            )
        );
        var result = assertJsonSuccess(api.autocompleteLocationsWithHttpInfo(input, 2));
        assertEquals(List.of(input, input + " - Ost"), result.stream().map(GeoSuggestionOutModel::getName).toList());
    }

    @Test
    void transportsUnicodeSpacesAndEscapedSpecialCharacters() {
        var input = fixture.query() + " Süd & West";
        var escaped = fixture.query() + " Süd \\& West";
        fixture.stubSuggestions(escaped, List.of(Map.of("suggestion", "SDK Süd & West", "score", 2.0)));
        assertEquals(
            "SDK Süd & West",
            assertJsonSuccess(api.autocompleteLocationsWithHttpInfo(input, 10)).getFirst().getName()
        );
        fixture.verifyQuery("/suggest", escaped);
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 25 })
    void acceptsLimitBoundaries(int limit) {
        assertEquals(
            limit == 1 ? 1 : 2,
            assertJsonSuccess(api.autocompleteLocationsWithHttpInfo(fixture.query(), limit)).size()
        );
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 70 })
    void acceptsQueryLengthBoundaries(int length) {
        assertEquals(List.of(), assertJsonSuccess(api.autocompleteLocationsWithHttpInfo("x".repeat(length), null)));
    }

    @Test
    void returnsEmptyForUnknownPrefixAndLeadingSpecialCharacter() {
        for (var input : List.of("missing" + fixture.query(), "+" + fixture.query()))
            assertEquals(List.of(), assertJsonSuccess(api.autocompleteLocationsWithHttpInfo(input, null)));
    }

    @Test
    void rejectsInvalidQueryAndLimit() {
        for (var input : List.of("", "x".repeat(71))) assertApiError(400, () -> api.autocompleteLocations(input, null));
        for (var limit : List.of(0, 26)) assertApiError(400, () -> api.autocompleteLocations(fixture.query(), limit));
    }

    @Test
    void rejectsMissingQueryOnServer() {
        assertApiError(400, () ->
            fixture.client().getRestClient().get().uri("/v3/locations/suggestions").retrieve().toBodilessEntity()
        );
    }
}
