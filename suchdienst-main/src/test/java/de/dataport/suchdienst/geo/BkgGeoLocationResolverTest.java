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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import de.dataport.suchdienst.SuchdienstInitializer;
import de.dataport.suchdienst.config.CacheConfiguration;
import de.dataport.suchdienst.geo.model.internal.GeoLocation;
import de.dataport.suchdienst.geo.service.BkgGeoLocationResolver;
import de.dataport.suchdienst.geo.service.CodelistDownloader;
import de.dataport.suchdienst.geo.service.FederalCityState;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(
    properties = {
        "de.dataport.suchdienst.bkg.password=test-pwd",
        "spring.main.allow-bean-definition-overriding=true",
        "management.health.elasticsearch.enabled=false",
        "spring.profiles.active=default,dev,openapi_show_internal",
    }
)
@MockitoBean(types = SuchdienstInitializer.class)
class BkgGeoLocationResolverTest {

    @TestConfiguration
    static class TestOverrides {

        @Bean(name = CacheConfiguration.REDIS_MGR)
        CacheManager redisCacheManager() {
            var mgr = new SimpleCacheManager();
            mgr.setCaches(
                List.of(
                    new ConcurrentMapCache(CacheConfiguration.BKG_SUGGESTION_CACHE_NAME),
                    new ConcurrentMapCache(CacheConfiguration.BKG_SEARCH_ARS_CACHE_NAME),
                    new ConcurrentMapCache(CacheConfiguration.BKG_SEARCH_QUERY_CACHE_NAME)
                )
            );
            mgr.initializeCaches();
            return mgr;
        }
    }

    @Autowired
    @Qualifier("bkgRestTemplate")
    private RestTemplate bkgRestTemplate;

    @Autowired
    private BkgGeoLocationResolver geoLocationResolver;

    @MockitoBean
    private CodelistDownloader codelistDownloader;

    @Autowired
    private CacheManager redisCacheManager;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void init() {
        mockServer = MockRestServiceServer.createServer(bkgRestTemplate);
        Mockito.when(codelistDownloader.getLatestDestatisArsCodelist()).thenReturn(
            java.util.Collections.emptyMap()
        );
        // Alle relevanten Caches leeren
        for (String name : List.of(
            CacheConfiguration.BKG_SUGGESTION_CACHE_NAME,
            CacheConfiguration.BKG_SEARCH_ARS_CACHE_NAME,
            CacheConfiguration.BKG_SEARCH_QUERY_CACHE_NAME
        )) {
            var cache = redisCacheManager.getCache(name);
            if (cache != null) cache.clear();
        }
    }

    @Test
    void testCacheViaSuggestions() throws IOException {
        String json = IOUtils.resourceToString("/bkg/suggestion-hh.json", Charset.defaultCharset());

        mockServer
            .expect(
                ExpectedCount.once(),
                MockRestRequestMatchers.requestTo(Matchers.containsString("suggest"))
            )
            .andRespond(
                MockRestResponseCreators.withStatus(org.springframework.http.HttpStatus.OK)
                    .body(json)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            );

        var suggestionsOne = geoLocationResolver.suggest("Hamburg", 12);
        var suggestionsTwo = geoLocationResolver.suggest("Hamburg", 12);

        assertAll(
            () -> assertFalse(suggestionsOne.isEmpty()),
            () -> assertThat(suggestionsOne.size(), equalTo(12)),
            () -> assertEquals(suggestionsOne, suggestionsTwo)
        );

        mockServer.verify();
    }

    @Test
    void testArsWithZipSearch() throws IOException {
        String json = IOUtils.resourceToString("/bkg/search-munich.json", Charset.defaultCharset());

        mockServer
            .expect(
                ExpectedCount.once(),
                MockRestRequestMatchers.requestTo(
                    Matchers.allOf(
                        Matchers.containsString("search"),
                        Matchers.containsString("rs:09162*"),
                        Matchers.containsString("schluessel:091620000000")
                    )
                )
            )
            .andRespond(
                MockRestResponseCreators.withStatus(org.springframework.http.HttpStatus.OK)
                    .body(json)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            );

        var searchOne = geoLocationResolver.searchByArs("091620000000", 12, false);

        assertAll(
            () ->
                assertThat(
                    searchOne
                        .stream()
                        .map(gld -> gld.getArs().getArs())
                        .toList(),
                    everyItem(is("091620000000"))
                ),
            () -> assertThat(searchOne.size(), equalTo(10))
        );

        mockServer.verify();
    }

    @ParameterizedTest
    @CsvSource({
        "091620000000,09162*",
        "09162,09162*",
        "091620000,09162*",
        "091600000,09160*",
        "09,09*",
        "100000000000,10*",
        "10,10*",
        "000,00*",
    })
    void testArsConversion(String inputArs, String formattedArs) throws IOException {
        String json = IOUtils.resourceToString("/bkg/search-munich.json", Charset.defaultCharset());

        mockServer
            .expect(
                ExpectedCount.once(),
                MockRestRequestMatchers.requestTo(
                    Matchers.allOf(
                        Matchers.containsString("search"),
                        Matchers.containsString("rs:" + formattedArs),
                        Matchers.containsString("schluessel:" + inputArs)
                    )
                )
            )
            .andRespond(
                MockRestResponseCreators.withStatus(org.springframework.http.HttpStatus.OK)
                    .body(json)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            );

        var searchOne = geoLocationResolver.searchByArs(inputArs, 12, false);

        assertAll(
            () ->
                assertThat(
                    searchOne
                        .stream()
                        .map(gld -> gld.getArs().getArs())
                        .toList(),
                    everyItem(is("091620000000"))
                ),
            () -> assertThat(searchOne.size(), equalTo(10))
        );

        mockServer.verify();
    }

    @Test
    void testSearchReverse() throws IOException {
        String json = IOUtils.resourceToString(
            "/bkg/searchReverse-langenhorn.json",
            Charset.defaultCharset()
        );

        mockServer
            .expect(
                ExpectedCount.once(),
                MockRestRequestMatchers.requestTo(
                    Matchers.allOf(
                        Matchers.containsString("distance"),
                        Matchers.containsString("10.015471")
                    )
                )
            )
            .andRespond(
                MockRestResponseCreators.withStatus(org.springframework.http.HttpStatus.OK)
                    .body(json)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            );

        var listGeolocationDetail = geoLocationResolver.searchReverse(
            new GeoLocation("10.015471", "53.681325")
        );
        var first = listGeolocationDetail.getFirst().getArs();

        assertThat(listGeolocationDetail.size(), equalTo(1));
        assertThat(first.getArs(), equalTo("024000000013"));
        assertThat(FederalCityState.isFederalCityStateArs("024000000013"), is(true));
        assertThat(first.getName(), equalTo("Hamburg 'Langenhorn'"));
        mockServer.verify();
    }
}
