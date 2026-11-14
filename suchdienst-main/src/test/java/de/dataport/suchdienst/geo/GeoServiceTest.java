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
import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

import de.dataport.suchdienst.config.Jackson3Configuration;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.geo.model.GeoArs;
import de.dataport.suchdienst.geo.model.GeoArsDistinctFilters;
import de.dataport.suchdienst.geo.model.GeoArsPropFilters;
import de.dataport.suchdienst.geo.model.internal.GeoLocation;
import de.dataport.suchdienst.geo.model.internal.GeoLocationArs;
import de.dataport.suchdienst.geo.model.internal.GeoLocationDetail;
import de.dataport.suchdienst.geo.model.internal.HouseAddress;
import de.dataport.suchdienst.geo.service.BkgGeoLocationResolver;
import de.dataport.suchdienst.geo.service.BkgQueryProvider;
import de.dataport.suchdienst.geo.service.CodelistDownloader;
import de.dataport.suchdienst.geo.service.GeoService;
import de.dataport.suchdienst.geo.service.caching.LocationCacheKeyGenerator;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
    classes = {
        GeoService.class,
        SuchdienstProperties.class,
        LocationCacheKeyGenerator.class,
        BkgQueryProvider.class,
        // Boot liefert JsonMapper.Builder + Default-Customizer
        JacksonAutoConfiguration.class,
        // unsere echten Customizer/Mapper-Beans
        Jackson3Configuration.class,
    }
)
@MockitoBean(types = { RedisConnectionFactory.class, TaskScheduler.class })
class GeoServiceTest {

    @TestConfiguration
    static class TestConfig {

        @Bean
        public CacheManager cacheManager() {
            return new NoOpCacheManager();
        }
    }

    @Autowired
    private GeoService geoService;

    @Autowired
    @Qualifier("httpJsonMapper")
    private JsonMapper jsonMapper = new JsonMapper();

    @MockitoBean
    private BkgGeoLocationResolver geoLocationResolver;

    @MockitoBean
    private CodelistDownloader codelistDownloader;

    @Test
    void search() {
        var geoDetail1 = new GeoLocationDetail(
            "id",
            GeoLocationArs.builder()
                .name("name")
                .ars("010515163089")
                .municipality("municipality")
                .weight(1f)
                .keys(List.of("key"))
                .plz("plz")
                .street("")
                .build(),
            new GeoLocation("lon", "lat")
        );
        var geoDetail2 = new GeoLocationDetail(
            "id2",
            GeoLocationArs.builder()
                .name("name2")
                .ars("010010000000")
                .municipality("municipality")
                .weight(2f)
                .keys(List.of("key"))
                .plz("plz")
                .street("")
                .build(),
            new GeoLocation("lon", "lat")
        );
        var geoDetail3 = new GeoLocationDetail(
            "id3",
            GeoLocationArs.builder()
                .name("name3")
                .ars("034510004004")
                .municipality("municipality")
                .weight(3f)
                .keys(List.of("key"))
                .plz("plz")
                .street("")
                .build(),
            new GeoLocation("lon", "lat")
        );

        Mockito.when(codelistDownloader.getLatestDestatisArsCodelist()).thenReturn(
            Map.of("034510004004", "", "010010000000", "", "010515163089", "")
        );

        Mockito.when(
            geoLocationResolver.searchByAddress(
                Mockito.any(HouseAddress.class),
                Mockito.anyInt(),
                Mockito.anyBoolean(),
                Mockito.anyString()
            )
        ).thenReturn(List.of(geoDetail1, geoDetail2, geoDetail3));

        var result = geoService.searchByAddress(
            new HouseAddress("", "", null, ""),
            10,
            emptySet(),
            emptySet(),
            true
        );

        assertNotNull(result);
        assertThat(result, hasSize(3));

        assertNotNull(result.getFirst().getLatitude());
        assertNotNull(result.getFirst().getLongitude());
        assertThat(result.getFirst().getLatitude().equals("lat"), is(true));
        assertThat(result.getFirst().getLongitude().equals("lon"), is(true));
    }

    @Test
    void sucheOhneOrtsteile() {
        var geoLocationDetailListe = ladeGeoLocationDetailTestDaten();

        Mockito.when(
            geoLocationResolver.searchByAddress(
                Mockito.any(HouseAddress.class),
                Mockito.anyInt(),
                Mockito.anyBoolean(),
                Mockito.anyString()
            )
        ).thenReturn(geoLocationDetailListe);

        var result = geoService.searchByAddress(
            new HouseAddress("", "", "", ""),
            20,
            emptySet(),
            emptySet(),
            false
        );

        assertThat(result, is(notNullValue()));
        assertThat(result, is(not(empty())));

        result
            .stream()
            .map(GeoArs::getArs)
            .filter(ars -> ars.startsWith("02"))
            .toList()
            .forEach(ars -> assertEquals("020000000000", ars));
    }

    @Test
    void searchWithDistinctBy() {
        var geoDetail1 = new GeoLocationDetail(
            "id",
            GeoLocationArs.builder()
                .name("name")
                .ars("034510004004")
                .municipality("municipality")
                .weight(1f)
                .keys(List.of("key"))
                .plz("12345")
                .street("street")
                .build(),
            new GeoLocation("lon", "lat")
        );
        var geoDetail2 = new GeoLocationDetail(
            "id",
            GeoLocationArs.builder()
                .name("name")
                .ars("034510004004")
                .municipality("municipality")
                .weight(1f)
                .keys(List.of("key"))
                .plz("12345")
                .street("street")
                .build(),
            new GeoLocation("lon", "lat")
        );
        var geoDetail3 = new GeoLocationDetail(
            "id",
            GeoLocationArs.builder()
                .name("name")
                .ars("010010000000")
                .municipality("municipality")
                .weight(1f)
                .keys(List.of("key"))
                .plz("54321")
                .street("street")
                .build(),
            new GeoLocation("lon", "lat")
        );
        var geoDetail4 = new GeoLocationDetail(
            "id",
            GeoLocationArs.builder()
                .name("name")
                .ars("010010000000")
                .municipality("municipality")
                .weight(1f)
                .keys(List.of("key"))
                .plz("54321")
                .street("street")
                .build(),
            new GeoLocation("lon", "lat")
        );

        Mockito.when(codelistDownloader.getLatestDestatisArsCodelist()).thenReturn(
            Map.of("034510004004", "", "010010000000", "")
        );

        Mockito.when(
            geoLocationResolver.searchByAddress(
                Mockito.any(HouseAddress.class),
                Mockito.anyInt(),
                Mockito.anyBoolean(),
                Mockito.anyString()
            )
        ).thenReturn(List.of(geoDetail1, geoDetail2, geoDetail3, geoDetail4));

        var result1 = geoService.searchByAddress(
            new HouseAddress("", "", null, ""),
            10,
            emptySet(),
            Set.of(GeoArsDistinctFilters.ARS),
            true
        );

        assertThat(result1, is(notNullValue()));
        assertThat(result1, hasSize(2));
        assertThat(
            result1.stream().map(GeoArs::getArs).toList(),
            contains("034510004004", "010010000000")
        );

        var result2 = geoService.searchByAddress(
            new HouseAddress("", "", "", ""),
            10,
            emptySet(),
            Set.of(GeoArsDistinctFilters.PLZ),
            true
        );

        assertThat(result2, is(notNullValue()));
        assertThat(result2, hasSize(2));
        assertThat(result2.stream().map(GeoArs::getPlz).toList(), contains("12345", "54321"));
    }

    @Test
    void searchWithFilerBy() {
        var geoDetail1 = new GeoLocationDetail(
            "id",
            GeoLocationArs.builder()
                .name("name")
                .ars("034510004004")
                .municipality("municipality")
                .weight(1f)
                .keys(List.of("key"))
                .plz("12345")
                .street(null)
                .build(),
            new GeoLocation("lon", "lat")
        );
        var geoDetail2 = new GeoLocationDetail(
            "id",
            GeoLocationArs.builder()
                .name("name")
                .ars("034510004004")
                .municipality("municipality")
                .weight(1f)
                .keys(List.of("key"))
                .plz("12345")
                .street("Sesamstrasse")
                .build(),
            new GeoLocation("lon", "lat")
        );
        var geoDetail3 = new GeoLocationDetail(
            "id",
            GeoLocationArs.builder()
                .name("name")
                .ars("034510004004")
                .municipality("municipality")
                .weight(1f)
                .keys(List.of("key"))
                .plz("12345")
                .street(" ")
                .build(),
            new GeoLocation("lon", "lat")
        );

        Mockito.when(codelistDownloader.getLatestDestatisArsCodelist()).thenReturn(
            Map.of("034510004004", "", "010010000000", "")
        );

        Mockito.when(
            geoLocationResolver.searchByAddress(
                Mockito.any(HouseAddress.class),
                Mockito.anyInt(),
                Mockito.anyBoolean(),
                Mockito.anyString()
            )
        ).thenReturn(List.of(geoDetail1, geoDetail2, geoDetail3));

        var result = geoService.searchByAddress(
            new HouseAddress("", "", "street", ""),
            10,
            Set.of(GeoArsPropFilters.STRASSE),
            emptySet(),
            false
        );

        assertThat(result, is(notNullValue()));
        assertThat(result, hasSize(1));
        assertThat(result.stream().map(GeoArs::getStreet).toList(), contains("Sesamstrasse"));

        var result2 = geoService.searchByAddress(
            new HouseAddress("", "12345", null, ""),
            10,
            emptySet(),
            emptySet(),
            false
        );
        assertThat(result2, is(notNullValue()));
        assertThat(result2, hasSize(3));
    }

    @Test
    void searchReverse() {
        var geoDetail = new GeoLocationDetail(
            "DEHHALKAP0008cCh",
            GeoLocationArs.builder()
                .name("Hamburg 'Langenhorn'")
                .ars("024000000013")
                .municipality("Langenhorn")
                .weight(1.4546175f)
                .keys(List.of("0240000000130432A3010"))
                .plz("22417")
                .street("")
                .build(),
            new GeoLocation("10.015471", "53.681325")
        );

        Mockito.when(geoLocationResolver.searchReverse(Mockito.any())).thenReturn(
            List.of(geoDetail)
        );
        Mockito.when(codelistDownloader.getLatestDestatisArsCodelist()).thenReturn(
            Collections.singletonMap("024130432432", "Hamburg 'Langenhorn'")
        );
        var result = geoService.searchReverse(new GeoLocation("10.015471", "53.681325"));

        assertNotNull(result);
        assertThat(result.size(), is(1));
        assertThat(result.getFirst().getName(), equalTo("Hamburg 'Langenhorn'"));
        // test transformed ars
        assertThat(result.getFirst().getArs(), equalTo("024130432432"));

        assertThat(result.getFirst().getLongitude(), equalTo("10.015471"));
        assertThat(result.getFirst().getLatitude(), equalTo("53.681325"));
        verify(codelistDownloader).getLatestDestatisArsCodelist();
    }

    @Test
    @DisplayName(
        "PV-11397: Suche nach mit Suchbegriff 'Ham'. Die Gemeinde 'Börnsen' soll im Ergebnis enthalten sein."
    )
    void sucheHam() {
        var geoLocationDetailListe = ladeGeoLocationDetailTestDaten();

        Mockito.when(
            geoLocationResolver.searchByAddress(
                Mockito.any(HouseAddress.class),
                Mockito.anyInt(),
                Mockito.anyBoolean(),
                Mockito.anyString()
            )
        ).thenReturn(geoLocationDetailListe);

        var result = geoService.searchByAddress(
            new HouseAddress("", "", null, ""),
            20,
            emptySet(),
            emptySet(),
            true
        );

        assertThat(result, is(notNullValue()));
        assertThat(result, is(not(empty())));

        var arsList = result
            .stream()
            .map(GeoArs::getArs)
            .filter(ars -> ars.equals("010535323012"))
            .toList();
        assertEquals(1, arsList.size());
        assertEquals("010535323012", arsList.getFirst());
    }

    @Test
    void searchFederalCity() {
        var geoLocationDetailListe = ladeGeoLocationDetailTestDaten();

        Mockito.when(
            geoLocationResolver.searchByAddress(
                Mockito.any(HouseAddress.class),
                Mockito.anyInt(),
                Mockito.anyBoolean(),
                Mockito.anyString()
            )
        ).thenReturn(geoLocationDetailListe);

        var result = geoService.searchByAddress(
            new HouseAddress("", "", null, ""),
            20,
            emptySet(),
            emptySet(),
            true
        );

        assertThat(result, is(notNullValue()));
        assertThat(result, is(not(empty())));
        assertThat(
            "Liste soll um einen Eintrag erweitert werden",
            result,
            hasSize(geoLocationDetailListe.size() + 1)
        );
        assertThat(
            "Stadtstaat ARS soll hinzugefügt werden",
            result,
            hasItem(FEDERAL_CITY_STATES.get("02"))
        );
        assertThat(
            "Stadtstaat ARS soll an der ersten Stelle stehen",
            result.getFirst(),
            is(FEDERAL_CITY_STATES.get("02"))
        );
    }

    @Test
    void searchByArsKeyPrioritization() {
        var geoDetail1 = new GeoLocationDetail(
            "id",
            GeoLocationArs.builder()
                .name("name_with_ars_in_second_key")
                .ars("010515163089")
                .municipality("municipality")
                .weight(2f)
                .keys(List.of("0100000000000000", "0105151630890000"))
                .plz("plz")
                .street("")
                .build(),
            new GeoLocation("lon", "lat")
        );
        var geoDetail2 = new GeoLocationDetail(
            "id2",
            GeoLocationArs.builder()
                .name("name_with_ars_in_first_key")
                .ars("010515163089")
                .municipality("municipality")
                .weight(1f)
                .keys(List.of("0105151630890000"))
                .plz("plz")
                .street("")
                .build(),
            new GeoLocation("lon", "lat")
        );

        Mockito.when(
            geoLocationResolver.searchByArs(
                Mockito.anyString(),
                Mockito.anyInt(),
                Mockito.anyBoolean()
            )
        ).thenReturn(new ArrayList<>(List.of(geoDetail1, geoDetail2)));

        var result = geoService.searchByArs(
            "010515163089",
            2,
            Set.of(GeoArsPropFilters.ORT),
            Set.of()
        );

        assertThat(
            "Der zweite Eintrag mit passendem Schlüssel an erster Stelle soll priorisiert werden",
            result.getFirst().getName(),
            is("name_with_ars_in_first_key")
        );
        assertThat(
            "An zweiter Stelle soll der Eintrag mit ID 1 stehen, welcher den passenden Schlüssel an zweiter Stelle hat",
            result.getLast().getName(),
            is("name_with_ars_in_second_key")
        );
    }

    @Test
    void searchByArsBremenSorted() {
        var geoDetail1 = new GeoLocationDetail(
            "DEGAC00000018226",
            GeoLocationArs.builder()
                .name("Bremen 'Neustadt'")
                .ars("040110215215")
                .municipality("Bremen")
                .weight(1.6486843f)
                .keys(List.of("0401100000000215"))
                .plz("28201")
                .street(null)
                .housenumber(null)
                .build(),
            new GeoLocation("8.798778", "53.061929")
        );
        var geoDetail2 = new GeoLocationDetail(
            "DEGAC00000018251",
            GeoLocationArs.builder()
                .name("Bremen 'Blockland'")
                .ars("040110000000")
                .municipality("Bremen")
                .weight(1.6486843f)
                .keys(List.of("0401100000000000"))
                .plz("28219")
                .street(null)
                .housenumber(null)
                .build(),
            new GeoLocation("8.782587", "53.153137")
        );
        var geoDetail3 = new GeoLocationDetail(
            "DEGAC00000081629",
            GeoLocationArs.builder()
                .name("Bremen")
                .ars("040110000000")
                .municipality("Bremen")
                .weight(1.6295516f)
                .keys(
                    List.of(
                        "0401100000000000",
                        "0325100370370000",
                        "0336100010010000",
                        "0335600090090000",
                        "0325100470470000",
                        "0335600080080000"
                    )
                )
                .plz(null)
                .street(null)
                .housenumber(null)
                .build(),
            new GeoLocation("8.809338", "53.075606")
        );

        Mockito.when(
            geoLocationResolver.searchByArs(
                Mockito.anyString(),
                Mockito.anyInt(),
                Mockito.anyBoolean()
            )
        ).thenReturn(List.of(geoDetail1, geoDetail2, geoDetail3));

        var result = geoService.searchByArs("040110000000", 3, emptySet(), Set.of());

        assertNotNull(result);
        assertThat(result.size(), is(3));
        assertThat(
            "Das erste Ergebnis in der Liste sollte der Stadtstaat Bremen sein",
            result.getFirst().getName(),
            is("Bremen")
        );
        assertThat(result.getFirst().getArs(), is("040110000000"));
    }

    @Test
    void searchByArsBremenPrependFederalState() {
        var geoDetail1 = new GeoLocationDetail(
            "DEGAC00000018226",
            GeoLocationArs.builder()
                .name("Bremen 'Neustadt'")
                .ars("040110215215")
                .municipality("Bremen")
                .weight(1.6486843f)
                .keys(List.of("0401100000000215"))
                .plz("28201")
                .street(null)
                .housenumber(null)
                .build(),
            new GeoLocation("8.798778", "53.061929")
        );
        var geoDetail2 = new GeoLocationDetail(
            "DEGAC00000018251",
            GeoLocationArs.builder()
                .name("Bremen 'Blockland'")
                .ars("040110000000")
                .municipality("Bremen")
                .weight(1.6486843f)
                .keys(List.of("0401100000000000"))
                .plz("28219")
                .street(null)
                .housenumber(null)
                .build(),
            new GeoLocation("8.782587", "53.153137")
        );
        var geoDetail3 = new GeoLocationDetail(
            "DEBREMEN010",
            GeoLocationArs.builder()
                .name("Bremen 'Rablinghausen'")
                .ars("040110252252")
                .municipality("Bremen")
                .weight(0.83f)
                .keys(List.of("0401102522520000"))
                .plz("28197")
                .street("")
                .build(),
            new GeoLocation("8.8017", "53.0834")
        );

        Mockito.when(
            geoLocationResolver.searchByArs(
                Mockito.anyString(),
                Mockito.anyInt(),
                Mockito.anyBoolean()
            )
        ).thenReturn(List.of(geoDetail1, geoDetail2, geoDetail3));

        var result = geoService.searchByArs("040110000000", 10, emptySet(), Set.of());

        assertNotNull(result);
        assertThat(result.size(), is(4));
        assertThat(
            "Das erste Ergebnis in der Liste sollte der Stadtstaat Bremen aus der statischen Liste FEDERAL_CITY_STATES sein",
            result.getFirst().getName(),
            is("Bremen, Stadt")
        );
        assertThat(result.getFirst().getArs(), is("040110000000"));
    }

    @Test
    void searchByArsBremenShouldBeUntouched() {
        var geoDetail1 = new GeoLocationDetail(
            "DEGAC00000081629",
            GeoLocationArs.builder()
                .name("Bremen")
                .ars("040110000000")
                .municipality("Bremen")
                .weight(1.6295516f)
                .keys(
                    List.of(
                        "0401100000000000",
                        "0325100370370000",
                        "0336100010010000",
                        "0335600090090000",
                        "0325100470470000",
                        "0335600080080000"
                    )
                )
                .plz(null)
                .street(null)
                .housenumber(null)
                .build(),
            new GeoLocation("8.809338", "53.075606")
        );
        var geoDetail2 = new GeoLocationDetail(
            "DEGAC00000018251",
            GeoLocationArs.builder()
                .name("Bremen 'Blockland'")
                .ars("040110000000")
                .municipality("Bremen")
                .weight(1.6486843f)
                .keys(List.of("0401100000000000"))
                .plz("28219")
                .street(null)
                .housenumber(null)
                .build(),
            new GeoLocation("8.782587", "53.153137")
        );
        var geoDetail3 = new GeoLocationDetail(
            "DEGAC00000018226",
            GeoLocationArs.builder()
                .name("Bremen 'Neustadt'")
                .ars("040110215215")
                .municipality("Bremen")
                .weight(1.6486843f)
                .keys(List.of("0401100000000215"))
                .plz("28201")
                .street(null)
                .housenumber(null)
                .build(),
            new GeoLocation("8.798778", "53.061929")
        );

        Mockito.when(
            geoLocationResolver.searchByArs(
                Mockito.anyString(),
                Mockito.anyInt(),
                Mockito.anyBoolean()
            )
        ).thenReturn(List.of(geoDetail1, geoDetail2, geoDetail3));

        var result = geoService.searchByArs("040110000000", 10, emptySet(), Set.of());

        assertNotNull(result);
        assertThat(
            "Das erste Ergebnis in der Liste sollte der Stadtstaat Bremen sein",
            result.getFirst().getName(),
            is("Bremen")
        );
        assertThat(result.get(1).getName(), is("Bremen 'Blockland'"));
        assertThat(result.get(2).getName(), is("Bremen 'Neustadt'"));
    }

    @Test
    void searchWithNullGeoLocation() {
        var geoDetail1 = new GeoLocationDetail(
            "id",
            GeoLocationArs.builder()
                .name("name")
                .ars("010515163089")
                .municipality("municipality")
                .weight(1f)
                .keys(List.of("key"))
                .plz("plz")
                .street("")
                .build(),
            null // null GeoLocation
        );
        var geoDetail2 = new GeoLocationDetail(
            "id2",
            GeoLocationArs.builder()
                .name("name2")
                .ars("010010000000")
                .municipality("municipality")
                .weight(2f)
                .keys(List.of("key"))
                .plz("plz")
                .street("")
                .build(),
            new GeoLocation("10.5", "52.5") // mit Koordinaten
        );

        Mockito.when(codelistDownloader.getLatestDestatisArsCodelist()).thenReturn(
            Map.of("010515163089", "", "010010000000", "")
        );

        Mockito.when(
            geoLocationResolver.searchByAddress(
                Mockito.any(HouseAddress.class),
                Mockito.anyInt(),
                Mockito.anyBoolean(),
                Mockito.anyString()
            )
        ).thenReturn(List.of(geoDetail1, geoDetail2));

        var result = geoService.searchByAddress(
            new HouseAddress("", "", null, ""),
            10,
            emptySet(),
            emptySet(),
            true
        );

        assertNotNull(result);
        assertThat(result, hasSize(2));

        // Erstes Ergebnis ohne Koordinaten
        assertNull(result.getFirst().getLatitude());
        assertNull(result.getFirst().getLongitude());

        // Zweites Ergebnis mit Koordinaten
        assertThat(result.get(1).getLatitude(), equalTo("52.5"));
        assertThat(result.get(1).getLongitude(), equalTo("10.5"));
    }

    @Test
    @DisplayName("suggest: Übergeordneter Ort wird vorangestellt wenn nur Ortsteile vorhanden")
    void suggest_prependsParentLocation_whenOnlyOrtsteileSuggested() {
        var suggestions = new ArrayList<>(
            List.of(
                new de.dataport.suchdienst.geo.model.out.GeoSuggestionOutModel(
                    "07407 Rudolstadt - Ost",
                    2.5f
                ),
                new de.dataport.suchdienst.geo.model.out.GeoSuggestionOutModel(
                    "07407 Rudolstadt - West",
                    2.3f
                ),
                new de.dataport.suchdienst.geo.model.out.GeoSuggestionOutModel(
                    "07407 Rudolstadt - Volkstedt",
                    2.1f
                )
            )
        );
        Mockito.when(geoLocationResolver.suggest("07407 Rudolstadt", 10)).thenReturn(suggestions);

        var result = geoService.suggest("07407 Rudolstadt", 10);

        assertThat(result, hasSize(4));
        assertThat(result.getFirst().getName(), equalTo("07407 Rudolstadt"));
        assertThat(result.getFirst().getWeight(), equalTo(2.5f));
    }

    @Test
    @DisplayName(
        "suggest: Übergeordneter Ort wird NICHT vorangestellt wenn er bereits vorhanden ist"
    )
    void suggest_doesNotPrependParentLocation_whenAlreadyPresent() {
        var suggestions = new ArrayList<>(
            List.of(
                new de.dataport.suchdienst.geo.model.out.GeoSuggestionOutModel(
                    "07407 Rudolstadt",
                    3.0f
                ),
                new de.dataport.suchdienst.geo.model.out.GeoSuggestionOutModel(
                    "07407 Rudolstadt - Ost",
                    2.5f
                ),
                new de.dataport.suchdienst.geo.model.out.GeoSuggestionOutModel(
                    "07407 Rudolstadt - West",
                    2.3f
                )
            )
        );
        Mockito.when(geoLocationResolver.suggest("07407 Rudolstadt", 10)).thenReturn(suggestions);

        var result = geoService.suggest("07407 Rudolstadt", 10);

        assertThat(result, hasSize(3));
        assertThat(result.getFirst().getName(), equalTo("07407 Rudolstadt"));
    }

    @Test
    @DisplayName("suggest: Keine Änderung wenn keine Ortsteile vorhanden")
    void suggest_noChange_whenNoOrtsteile() {
        var suggestions = new ArrayList<>(
            List.of(
                new de.dataport.suchdienst.geo.model.out.GeoSuggestionOutModel(
                    "07407 Rudolstadt",
                    3.0f
                ),
                new de.dataport.suchdienst.geo.model.out.GeoSuggestionOutModel(
                    "07422 Rudolstadt",
                    2.5f
                )
            )
        );
        Mockito.when(geoLocationResolver.suggest("Rudolstadt", 10)).thenReturn(suggestions);

        var result = geoService.suggest("Rudolstadt", 10);

        assertThat(result, hasSize(2));
    }

    @Test
    @DisplayName("suggest: Kein Voranstellen wenn gezielt nach Ortsteil gesucht wird")
    void suggest_doesNotPrepend_whenSearchingForOrtsteil() {
        var suggestions = new ArrayList<>(
            List.of(
                new de.dataport.suchdienst.geo.model.out.GeoSuggestionOutModel(
                    "07407 Rudolstadt - Ost",
                    2.5f
                ),
                new de.dataport.suchdienst.geo.model.out.GeoSuggestionOutModel(
                    "07407 Rudolstadt - Oststadt",
                    2.3f
                )
            )
        );
        Mockito.when(geoLocationResolver.suggest("Rudolstadt Ost", 10)).thenReturn(suggestions);

        var result = geoService.suggest("Rudolstadt Ost", 10);

        // Da die Eingabe den Ortsteil adressiert, wird "Rudolstadt" NICHT vorangestellt
        assertThat(result, hasSize(2));
        assertThat(result.getFirst().getName(), equalTo("07407 Rudolstadt - Ost"));
    }

    @Test
    @DisplayName("suggest: Liste wird bei vorangestelltem Ort auf Limit gekürzt")
    void suggest_trimsToLimit_whenParentPrepended() {
        var suggestions = new ArrayList<>(
            List.of(
                new de.dataport.suchdienst.geo.model.out.GeoSuggestionOutModel(
                    "07407 Rudolstadt - Ost",
                    2.5f
                ),
                new de.dataport.suchdienst.geo.model.out.GeoSuggestionOutModel(
                    "07407 Rudolstadt - West",
                    2.3f
                ),
                new de.dataport.suchdienst.geo.model.out.GeoSuggestionOutModel(
                    "07407 Rudolstadt - Volkstedt",
                    2.1f
                )
            )
        );
        Mockito.when(geoLocationResolver.suggest("07407 Rudolstadt", 3)).thenReturn(suggestions);

        var result = geoService.suggest("07407 Rudolstadt", 3);

        assertThat(result, hasSize(3));
        assertThat(result.getFirst().getName(), equalTo("07407 Rudolstadt"));
        // Der letzte (niedrigst bewertete) Eintrag wurde entfernt
        assertThat(
            result
                .stream()
                .map(de.dataport.suchdienst.geo.model.out.GeoSuggestionOutModel::getName)
                .toList(),
            not(hasItem("07407 Rudolstadt - Volkstedt"))
        );
    }

    private List<GeoLocationDetail> ladeGeoLocationDetailTestDaten() {
        InputStream is = GeoServiceTest.class.getResourceAsStream(
            "/geo/geoLocationDetailTestDaten.json"
        );
        String json = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));

        return jsonMapper.readValue(json, new TypeReference<>() {});
    }
}
