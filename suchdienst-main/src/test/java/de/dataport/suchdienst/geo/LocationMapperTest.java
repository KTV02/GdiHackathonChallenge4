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
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import de.dataport.suchdienst.geo.model.GeoArs;
import de.dataport.suchdienst.geo.model.mapper.LocationMapper;
import org.junit.jupiter.api.Test;

class LocationMapperTest {

    @Test
    void mapGeoArsV3ShouldMapCoordinatesWhenCoordinatesArePresent() {
        var geoArs = GeoArs.builder()
            .ars("0822200000000000")
            .arsFederalstate("08")
            .name("Mannheim")
            .municipality("Mannheim")
            .plz("68165")
            .street("Parkallee")
            .housenumber("2-4")
            .longitude("8.484444")
            .latitude("49.452778")
            .build();

        var result = LocationMapper.INSTANCE.mapGeoArsV3(geoArs);

        assertThat(result, is(notNullValue()));
        assertThat(result.koordinaten(), is(notNullValue()));
        assertThat(result.koordinaten().size(), is(2));
        assertThat(result.koordinaten().getFirst(), is("8.484444"));
        assertThat(result.koordinaten().get(1), is("49.452778"));
    }

    @Test
    void mapGeoArsV3ShouldReturnNullCoordinatesWhenLongitudeIsMissing() {
        var geoArs = GeoArs.builder()
            .ars("0822200000000000")
            .arsFederalstate("08")
            .name("Mannheim")
            .municipality("Mannheim")
            .plz("68165")
            .street("Parkallee")
            .housenumber("2-4")
            .latitude("49.452778")
            .build();

        var result = LocationMapper.INSTANCE.mapGeoArsV3(geoArs);

        assertThat(result, is(notNullValue()));
        assertThat(result.koordinaten(), is(nullValue()));
    }

    @Test
    void mapGeoArsV3ShouldReturnNullCoordinatesWhenLatitudeIsMissing() {
        var geoArs = GeoArs.builder()
            .ars("082220000000")
            .arsFederalstate("08")
            .name("Mannheim")
            .longitude("8.484444")
            .build();

        var result = LocationMapper.INSTANCE.mapGeoArsV3(geoArs);

        assertThat(result, is(notNullValue()));
        assertThat(result.koordinaten(), is(nullValue()));
    }
}
