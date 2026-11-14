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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import de.dataport.suchdienst.geo.model.GeoArs;
import de.dataport.suchdienst.geo.model.internal.GeoLocation;
import de.dataport.suchdienst.geo.model.internal.GeoLocationArs;
import de.dataport.suchdienst.geo.model.internal.GeoLocationDetail;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeoArsTest {

    @Test
    void fromGeoLocationShouldMapCoordinates() {
        var geoLocationArs = GeoLocationArs.builder()
            .keys(List.of("0822200000000000"))
            .name("Mannheim")
            .municipality("Mannheim")
            .plz("68165")
            .street("Parkallee")
            .housenumber("2-4")
            .build();

        var geoLocation = new GeoLocation("8.484444", "49.452778");

        var geoLocationDetail = new GeoLocationDetail(
            "0822200000000000",
            geoLocationArs,
            geoLocation
        );

        var result = GeoArs.fromGeoLocation("082220000000", geoLocationDetail);

        assertThat(result.getArs(), is("082220000000"));
        assertThat(result.getName(), is("Mannheim"));
        assertThat(result.getMunicipality(), is("Mannheim"));
        assertThat(result.getPlz(), is("68165"));
        assertThat(result.getStreet(), is("Parkallee"));
        assertThat(result.getHousenumber(), is("2-4"));
        assertThat(result.getLongitude(), is("8.484444"));
        assertThat(result.getLatitude(), is("49.452778"));
    }
}
