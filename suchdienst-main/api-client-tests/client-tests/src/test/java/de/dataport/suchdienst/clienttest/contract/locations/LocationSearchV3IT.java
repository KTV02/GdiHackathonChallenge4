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
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dataport.suchdienst.client.api.OrteBkgApi;
import de.dataport.suchdienst.client.model.GeoArsOutModelV3;
import de.dataport.suchdienst.clienttest.fixture.LocationFixture;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Testziel: GET /v3/locations bindet kontrollierte Orts-/Adressdaten und prüft Defaults, Filter und Eingabegrenzen. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LocationSearchV3IT {

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
    void bindsControlledLocationsAndCoordinates() {
        var locations = assertJsonSuccess(api.getLocationsByQueryWithHttpInfo(fixture.query(), null, null, null, null));
        assertEquals(4, locations.size());
        LocationFixture.assertTown(locations.getFirst());
        LocationFixture.assertHouse(locations.get(2));
        fixture.verifyQuery("/geosearch", fixture.query());
    }

    @Test
    void defaultsMatchExplicitDefaults() {
        assertEquals(
            api.getLocationsByQuery(fixture.query(), null, null, null, null),
            api.getLocationsByQuery(fixture.query(), 10, Set.of(), Set.of(), true)
        );
    }

    @Test
    void filtersRequiredPropertiesAndDistinctPostcodes() {
        var houses = assertJsonSuccess(
            api.getLocationsByQueryWithHttpInfo(fixture.query(), 25, Set.of("STRASSE", "HAUSNUMMER"), null, true)
        );
        assertEquals(2, houses.size());
        assertTrue(houses.stream().allMatch(hit -> hit.getStrasse() != null && hit.getHausnummer() != null));
        var postcodes = assertJsonSuccess(
            api.getLocationsByQueryWithHttpInfo(fixture.query(), 25, Set.of("PLZ"), Set.of("PLZ"), true)
        );
        assertEquals(
            Set.of("68165", "68167"),
            postcodes.stream().map(GeoArsOutModelV3::getPlz).collect(Collectors.toSet())
        );
        assertEquals(2, postcodes.size());
        assertEquals(1, api.getLocationsByQuery(fixture.query(), 25, null, Set.of("ARS"), false).size());
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 25 })
    void acceptsLimitBoundaries(int limit) {
        assertEquals(
            limit == 1 ? 1 : 4,
            assertJsonSuccess(api.getLocationsByQueryWithHttpInfo(fixture.query(), limit, null, null, null)).size()
        );
    }

    @Test
    void returnsEmptyForUnknownQueryAndLeadingSpecialCharacter() {
        for (var input : List.of("missing" + fixture.query(), "+" + fixture.query())) {
            assertEquals(
                List.of(),
                assertJsonSuccess(api.getLocationsByQueryWithHttpInfo(input, null, null, null, null))
            );
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 70 })
    void acceptsQueryLengthBoundaries(int length) {
        assertEquals(
            List.of(),
            assertJsonSuccess(api.getLocationsByQueryWithHttpInfo("x".repeat(length), null, null, null, null))
        );
    }

    @Test
    void rejectsInvalidQueryLimitAndFilter() {
        for (var input : List.of("", "x".repeat(71)))
            assertApiError(400, () -> api.getLocationsByQuery(input, null, null, null, null));
        for (var limit : List.of(0, 26))
            assertApiError(400, () -> api.getLocationsByQuery(fixture.query(), limit, null, null, null));
        assertApiError(400, () -> api.getLocationsByQuery(fixture.query(), null, Set.of("unknown"), null, null));
    }

    @Test
    void rejectsMissingQueryOnServer() {
        assertApiError(400, () ->
            fixture.client().getRestClient().get().uri("/v3/locations").retrieve().toBodilessEntity()
        );
    }
}
