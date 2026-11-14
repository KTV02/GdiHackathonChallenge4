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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Testziel: GET /v2/locations/reverse erhält Längen-/Breitengradreihenfolge und validiert Koordinatengrenzen. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LocationReverseV2IT {

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
    void bindsControlledReverseAddressAndCoordinateOrder() {
        var result = assertJsonSuccess(
            api.getArsByCoordinatesV3WithHttpInfo(LocationFixture.LATITUDE, LocationFixture.LONGITUDE)
        );
        assertEquals(1, result.size());
        LocationFixture.assertHouse(result.getFirst());
    }

    @Test
    void returnsEmptyForCoordinatesWithoutLocation() {
        assertEquals(List.of(), assertJsonSuccess(api.getArsByCoordinatesV3WithHttpInfo("0", "0")));
    }

    @ParameterizedTest
    @CsvSource({ "90,180", "-90,-180" })
    void acceptsCoordinateBoundaries(String lat, String lon) {
        assertEquals(List.of(), assertJsonSuccess(api.getArsByCoordinatesV3WithHttpInfo(lat, lon)));
    }

    @ParameterizedTest
    @CsvSource({ "90.01,0", "-90.01,0", "0,180.01", "0,-180.01", "invalid,0", "0,invalid", "NaN,0", "0,Infinity" })
    void rejectsMalformedAndOutOfRangeCoordinates(String lat, String lon) {
        assertApiError(400, () -> api.getArsByCoordinatesV3(lat, lon));
    }

    @Test
    void rejectsMissingCoordinatesOnServer() {
        for (var query : List.of("?lat=0", "?lon=0", ""))
            assertApiError(400, () ->
                fixture
                    .client()
                    .getRestClient()
                    .get()
                    .uri("/v2/locations/reverse" + query)
                    .retrieve()
                    .toBodilessEntity()
            );
    }
}
