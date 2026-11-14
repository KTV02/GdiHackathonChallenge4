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
import de.dataport.suchdienst.clienttest.fixture.LocationFixture;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Testziel: GET /v3/locations/details bindet eine definierte Hausadresse und prüft Parameterabhängigkeiten. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LocationDetailsV3IT {

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
    void bindsControlledHouseAddressAndCoordinates() {
        var result = assertJsonSuccess(
            api.getLocationsByAddressWithHttpInfo(
                LocationFixture.CITY,
                LocationFixture.ZIP,
                LocationFixture.STREET,
                "2a",
                null,
                null,
                null,
                null
            )
        );
        assertEquals(1, result.size());
        LocationFixture.assertHouse(result.getFirst());
    }

    @Test
    void defaultsMatchExplicitDefaultsAndPropertyFilters() {
        var implicit = api.getLocationsByAddress(
            LocationFixture.CITY,
            LocationFixture.ZIP,
            LocationFixture.STREET,
            "2a",
            null,
            null,
            null,
            null
        );
        assertEquals(
            implicit,
            api.getLocationsByAddress(
                LocationFixture.CITY,
                LocationFixture.ZIP,
                LocationFixture.STREET,
                "2a",
                10,
                Set.of(),
                Set.of(),
                true
            )
        );
        assertEquals(
            implicit,
            api.getLocationsByAddress(
                LocationFixture.CITY,
                LocationFixture.ZIP,
                LocationFixture.STREET,
                "2a",
                10,
                Set.of("PLZ", "HAUSNUMMER"),
                Set.of("ARS"),
                false
            )
        );
    }

    @Test
    void returnsEmptyForUnknownAddressAndLeadingSpecialCharacter() {
        for (var city : List.of("missing" + fixture.query(), "+" + fixture.query()))
            assertEquals(
                List.of(),
                assertJsonSuccess(api.getLocationsByAddressWithHttpInfo(city, null, null, null, null, null, null, null))
            );
    }

    @Test
    void requiresCityOrPostcodeAndStreetForHouseNumber() {
        assertApiError(400, () -> api.getLocationsByAddress(null, null, null, null, null, null, null, null));
        assertApiError(400, () ->
            api.getLocationsByAddress(null, null, LocationFixture.STREET, "2a", null, null, null, null)
        );
        assertApiError(400, () ->
            api.getLocationsByAddress(LocationFixture.CITY, null, null, "2a", null, null, null, null)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = { "abc", "123456" })
    void rejectsInvalidPostcode(String postcode) {
        assertApiError(400, () ->
            api.getLocationsByAddress(LocationFixture.CITY, postcode, null, null, null, null, null, null)
        );
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 70 })
    void acceptsTextLengthBoundaries(int length) {
        assertEquals(
            List.of(),
            assertJsonSuccess(
                api.getLocationsByAddressWithHttpInfo(
                    "x".repeat(length),
                    null,
                    "s".repeat(length),
                    "h".repeat(length),
                    25,
                    null,
                    null,
                    null
                )
            )
        );
    }

    @Test
    void acceptsPostcodeOnlyAndLimitBoundaries() {
        for (var limit : List.of(1, 25))
            assertEquals(
                List.of(),
                assertJsonSuccess(
                    api.getLocationsByAddressWithHttpInfo(null, "12345", null, null, limit, null, null, null)
                )
            );
    }

    @Test
    void rejectsOverlongTextAndInvalidLimit() {
        var longText = "x".repeat(71);
        assertApiError(400, () -> api.getLocationsByAddress(longText, null, null, null, null, null, null, null));
        assertApiError(400, () ->
            api.getLocationsByAddress(LocationFixture.CITY, null, longText, null, null, null, null, null)
        );
        assertApiError(400, () ->
            api.getLocationsByAddress(
                LocationFixture.CITY,
                null,
                LocationFixture.STREET,
                longText,
                null,
                null,
                null,
                null
            )
        );
        for (var limit : List.of(0, 26))
            assertApiError(400, () ->
                api.getLocationsByAddress(LocationFixture.CITY, null, null, null, limit, null, null, null)
            );
    }
}
