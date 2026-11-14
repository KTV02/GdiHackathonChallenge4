/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2025 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.geo;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import de.dataport.suchdienst.geo.model.internal.GeoLocation;
import de.dataport.suchdienst.geo.service.CoordinateTransformationService;
import org.hamcrest.number.IsCloseTo;
import org.junit.jupiter.api.Test;

class CoordinateTransformationTest {

    private final CoordinateTransformationService service = new CoordinateTransformationService();

    private static final GeoLocation MUC_31468 = new GeoLocation(
        "4469078.42348559",
        "5331879.03409528",
        "EPSG:31468"
    );
    private static final GeoLocation MUC_DEFAULT = new GeoLocation(
        "11.583197211938153",
        "48.12419674064079",
        GeoLocation.DEFAULT_CRS
    );

    @Test
    void testConvertFromEpsg31468() {
        GeoLocation transform = service.transform(MUC_31468, GeoLocation.DEFAULT_CRS).get();
        assertThat(transform, is(MUC_DEFAULT));
    }

    @Test
    void testConvertToEpsg31468() {
        GeoLocation transform = service.transform(MUC_DEFAULT, MUC_31468.getSystem()).get();
        // wegen Umrechnungsungenauigkeiten kommt hier nicht exakt das Ursprungsergebnis
        // raus:
        assertAll(
            () ->
                assertThat(
                    Double.valueOf(transform.getLatitude()),
                    IsCloseTo.closeTo(Double.parseDouble(MUC_31468.getLatitude()), 0.001d)
                ),
            () ->
                assertThat(
                    Double.valueOf(transform.getLongitude()),
                    IsCloseTo.closeTo(Double.parseDouble(MUC_31468.getLongitude()), 0.001d)
                )
        );
    }
}
