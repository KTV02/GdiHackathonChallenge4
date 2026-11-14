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

import static de.dataport.suchdienst.geo.service.FederalCityState.FEDERAL_CITY_STATES;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dataport.suchdienst.geo.model.GeoArs;
import de.dataport.suchdienst.geo.model.internal.GeoLocationArs;
import de.dataport.suchdienst.geo.service.FederalCityState;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BkgFederalCityStatesTest {

    @Test
    void testBkgArsMappingHamburg() {
        var geoArs1 = GeoLocationArs.builder()
            .name("HH Hamm")
            .ars("021000000006")
            .municipality("Hamburg")
            .weight(0)
            .keys(List.of("0210000000060117"))
            .plz("20097")
            .street("Hammerbrookstraße")
            .build();
        var ars1 = getAllArs(geoArs1);
        assertThat(ars1, contains("021060117117"));

        var geoArs2 = GeoLocationArs.builder()
            .name("HH Steilshoop")
            .ars("025000000007")
            .municipality("Hamburg")
            .weight(0)
            .keys(List.of("0250000000070515"))
            .plz("22177")
            .street("Fabriciusstraße")
            .build();
        var ars2 = getAllArs(geoArs2);
        assertThat(ars2, contains("025070515515"));

        var geoArs3 = GeoLocationArs.builder()
            .name("HH Hafencity")
            .ars("021000000002")
            .municipality("Hamburg")
            .weight(0)
            .keys(List.of("0210000000020103", "0210000000020104", "0210000000010101"))
            .plz("22177")
            .street("Steilshooper Allee")
            .build();
        var ars3 = getAllArs(geoArs3);
        assertThat(ars3, contains("021020103103", "021020104104", "021010101101"));

        // Gemeinde Moorburg, 21079
        var geoArsMoorburg1 = GeoLocationArs.builder()
            .name("HH Moorburg")
            .ars("027000000011")
            .municipality("Hamburg")
            .weight(0)
            .keys(List.of("0270000000110712"))
            .plz("21079")
            .street("Moorburger Elbdeich")
            .build();
        var arsMoorburg1 = getAllArs(geoArsMoorburg1);
        assertThat(arsMoorburg1, contains("027110712712"));

        // Gemeinde Altenwerder, 21129
        var geoArsMoorburg2 = GeoLocationArs.builder()
            .name("HH Moorburg")
            .ars("027000000012")
            .municipality("Hamburg")
            .weight(0)
            .keys(List.of("0270000000120713"))
            .plz("21129")
            .street("Moorburger Hauptdreich")
            .build();
        var arsMoorburg2 = getAllArs(geoArsMoorburg2);
        assertThat(arsMoorburg2, contains("027120713713"));
    }

    @Test
    void testBkgArsMappingBremen() {
        var geoArs1 = GeoLocationArs.builder()
            .name("Bremerhaven Überseehafen")
            .ars("040120000000")
            .municipality("Bremerhaven")
            .weight(0)
            .keys(List.of("0401200000000111", "0401100000000123"))
            .plz("27580")
            .street("Cherbourger Strasse")
            .build();
        var ars1 = getAllArs(geoArs1);
        assertThat(ars1, hasItem("040120111111"));

        var geoArs2 = GeoLocationArs.builder()
            .name("HB Altstadt")
            .ars("040110000000")
            .municipality("Bremen")
            .weight(0)
            .keys(List.of("0401100000000111"))
            .plz("28195")
            .street("Eduard-Schopf-Allee")
            .build();
        var ars2 = getAllArs(geoArs2);
        assertThat(ars2, contains("040110111111"));
    }

    @Test
    void testBkgArsMappingBerlin() {
        var geoArs1 = GeoLocationArs.builder()
            .name("Berlin Buch")
            .ars("110000000003")
            .municipality("Berlin")
            .weight(0)
            .keys(List.of("1100000000000309", "1206001811810040"))
            .plz("10439")
            .street("Bornholmer Straße")
            .build();
        var ars1 = getAllArs(geoArs1);
        assertThat(ars1, hasItem("110030003003"));

        var geoArs2 = GeoLocationArs.builder()
            .name("Berlin Friedrichshain")
            .ars("110000000002")
            .municipality("Berlin")
            .weight(0)
            .keys(List.of("1100000000000201"))
            .plz("10179")
            .street("Spandauer Straße")
            .build();
        var ars2 = getAllArs(geoArs2);
        assertThat(ars2, hasItem("110020002002"));
    }

    @Test
    void testFederalCityStateDetection() {
        assertTrue(FederalCityState.isFederalCityStateArs("02"));
        assertTrue(FederalCityState.isFederalCityStateArs("04011"));
        assertTrue(FederalCityState.isFederalCityStateArs("11"));

        assertThat(
            FederalCityState.getMatchingFederalCityState("02").get().getName(),
            containsString("Hamburg, Freie und Hansestadt")
        );
        assertThat(
            FederalCityState.getMatchingFederalCityState("04011").get().getName(),
            containsString("Bremen, Stadt")
        );
        assertThat(
            FederalCityState.getMatchingFederalCityState("11").get().getName(),
            containsString("Berlin, Stadt")
        );
    }

    @Test
    void testFederalCityStateIdentification() {
        assertThat(
            FederalCityState.findMatchingFederalCityState(
                new ArrayList<>(
                    List.of(GeoArs.builder().ars("021231231231").arsFederalstate("02").build())
                )
            ).get(),
            is(FEDERAL_CITY_STATES.get("02"))
        );
        assertThat(
            FederalCityState.findMatchingFederalCityState(
                new ArrayList<>(
                    List.of(GeoArs.builder().ars("040111231231").arsFederalstate("04").build())
                )
            ).get(),
            is(FEDERAL_CITY_STATES.get("04011"))
        );
        assertThat(
            FederalCityState.findMatchingFederalCityState(
                new ArrayList<>(
                    List.of(GeoArs.builder().ars("110111231231").arsFederalstate("11").build())
                )
            ).get(),
            is(FEDERAL_CITY_STATES.get("11"))
        );
    }

    private static List<String> getAllArs(GeoLocationArs geoLocationArs) {
        return geoLocationArs
            .getKeys()
            .stream()
            .map(FederalCityState::transformFromBkgKeyToArs)
            .flatMap(Optional::stream)
            .toList();
    }
}
