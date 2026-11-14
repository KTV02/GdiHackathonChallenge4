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

/** Testziel: GET /v3/locations/ars bindet kontrollierte Ortsdaten mit Defaults, Filtern und ARS-Grenzen. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LocationArsV3IT {

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
    void bindsControlledArsLocationsAndCoordinates() {
        var result = assertJsonSuccess(api.getLocationsByArsWithHttpInfo(LocationFixture.ARS, null, null, null));
        assertEquals(4, result.size());
        LocationFixture.assertTown(result.getFirst());
        LocationFixture.assertHouse(result.get(2));
    }

    @Test
    void defaultLimitAndFiltersMatchExplicitDefaults() {
        assertEquals(
            api.getLocationsByArs(LocationFixture.ARS, null, null, null),
            api.getLocationsByArs(LocationFixture.ARS, 10, Set.of(), Set.of())
        );
    }

    @Test
    void filtersRequiredPostcodesAndDistinctValues() {
        var result = assertJsonSuccess(
            api.getLocationsByArsWithHttpInfo(LocationFixture.ARS, 25, Set.of("PLZ"), Set.of("PLZ"))
        );
        assertEquals(2, result.size());
        assertEquals(
            Set.of("68165", "68167"),
            result.stream().map(GeoArsOutModelV3::getPlz).collect(Collectors.toSet())
        );
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 25 })
    void acceptsLimitBoundaries(int limit) {
        assertEquals(
            limit == 1 ? 1 : 4,
            assertJsonSuccess(api.getLocationsByArsWithHttpInfo(LocationFixture.ARS, limit, null, null)).size()
        );
    }

    @Test
    void acceptsArsLengthBoundariesAndReturnsEmptyForUnknownArs() {
        for (var ars : List.of("99", "999990000000"))
            assertEquals(List.of(), assertJsonSuccess(api.getLocationsByArsWithHttpInfo(ars, null, null, null)));
    }

    @ParameterizedTest
    @ValueSource(strings = { "1", "1234567890123", "invalid" })
    void rejectsInvalidArs(String ars) {
        assertApiError(400, () -> api.getLocationsByArs(ars, null, null, null));
    }

    @Test
    void rejectsInvalidLimitAndFilter() {
        for (var limit : List.of(0, 26))
            assertApiError(400, () -> api.getLocationsByArs(LocationFixture.ARS, limit, null, null));
        assertApiError(400, () -> api.getLocationsByArs(LocationFixture.ARS, null, null, Set.of("unknown")));
    }

    @Test
    void rejectsMissingArsOnServer() {
        assertApiError(400, () ->
            fixture.client().getRestClient().get().uri("/v3/locations/ars").retrieve().toBodilessEntity()
        );
    }
}
