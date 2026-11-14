/*-
 * ---license-start
 * Suchdienst Integrationstests
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package suchdienst.integrationstest.tests.codelist;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.connectors.LocationsConnector;
import suchdienst.integrationstest.mocks.BKGMock;
import suchdienst.integrationstest.mocks.CodelistMock;

@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class ArsCodelistFallbackWithCacheClearingTest {

    private static BKGMock bkgMock;
    private static CodelistMock codelistMock;

    private static final String ARS_CACHE_NAME = "ars";

    @BeforeAll
    static void setup() {
        bkgMock = new BKGMock();
        codelistMock = new CodelistMock();
    }

    @BeforeEach
    void clearCacheBeforeEachTest() throws Exception {
        codelistMock.createCodelistVersionResponse();
        codelistMock.createBevoelkerungsstatistik();

        // Del Redis cache key to ensure clean state before each test
        ProcessBuilder pb = new ProcessBuilder(
            "redis-cli",
            "-h",
            System.getenv().getOrDefault("REDIS_HOST", "localhost"),
            "-p",
            System.getenv().getOrDefault("REDIS_PORT", "6379"),
            "DEL",
            ARS_CACHE_NAME
        );

        final Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new RuntimeException("Could not start redis-cli: " + e.getMessage(), e);
        }

        boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("redis-cli DEL timed out");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            System.out.println("Warning: Redis CLI command failed with exit code " + exitCode);
        }
    }

    @AfterEach
    void resetMocks() {
        bkgMock.reset();
        codelistMock.reset();
    }

    /**
     * XRepository available - should fetch from external service
     *
     * With cache cleared before this test, the application will need to fetch
     * ARS data fresh, hitting our mock.
     */
    @Test
    void shouldFetchFromXRepository_whenCacheIsCleared() throws Exception {
        String json = IOUtils.resourceToString("/bkg/OrtHamburg.json", StandardCharsets.UTF_8);
        bkgMock.createGeocodeResponseBkgMock("Hamburg", json);

        // when - trigger a request that needs ARS data
        // This should cause CodelistDownloader to fetch from XRepository since cache is empty
        Response response = LocationsConnector.getLocationsV3Details("Hamburg", 10);

        assertThat(response.getStatusCode(), is(HttpStatus.SC_OK));
        JsonPath jsonPath = response.jsonPath();
        List<String> names = jsonPath.getList("name");
        assertThat("Should have location results", names, is(notNullValue()));
        assertThat("Should have at least one location", names.size(), is(greaterThan(0)));

        // and - XRepository was actually called (not using old cached data)
        codelistMock.verifyVersionEndpointCalled();
        codelistMock.verifyGenericodeEndpointCalled();
    }

    /**
     * XRepository returns 503 - should fallback to seed
     *
     * With cache cleared and mock configured for failure, the application
     * should load the seed file instead.
     */
    @Test
    void shouldFallbackToSeed_whenXRepositoryReturns503AndCacheIsCleared() throws Exception {
        codelistMock.simulateXRepositoryOutage();
        String json = IOUtils.resourceToString("/bkg/OrtBerlin.json", StandardCharsets.UTF_8);
        bkgMock.createGeocodeResponseBkgMock("Berlin", json);

        // when - trigger a request that needs ARS data
        Response response = LocationsConnector.getLocationsV3Details("Berlin", 10);

        // then - request still succeeds (using seed data)
        assertThat("Should work even with XRepository down", response.getStatusCode(), is(HttpStatus.SC_OK));
        JsonPath jsonPath = response.jsonPath();
        List<String> names = jsonPath.getList("name");
        assertThat("Should have location results from seed", names, is(notNullValue()));
        assertThat("Should have at least one location", names.size(), is(greaterThan(0)));

        // and - XRepository was attempted but failed
        codelistMock.verifyVersionEndpointCalled();

        // Verify response contains resolved location names (from seed)
        // The seed file should contain enough data to resolve basic German locations
        assertThat(
            "Should have resolved location names",
            names.stream().anyMatch(name -> name != null && !name.isEmpty()),
            is(true)
        );
    }

    /**
     * Verify cache is actually being used after initial load
     *
     * After the first request populates the cache, subsequent requests
     * should NOT trigger new XRepository calls.
     */
    @Test
    void shouldUseCache_afterInitialLoad() throws Exception {
        codelistMock.createCodelistVersionResponse();
        codelistMock.createBevoelkerungsstatistik();

        String json = IOUtils.resourceToString("/bkg/OrtMünchen.json", StandardCharsets.UTF_8);
        bkgMock.createGeocodeResponseBkgMock("München", json);

        // when - make first request (should hit XRepository)
        Response response1 = LocationsConnector.getLocationsV3Details("München", 10);
        assertThat(response1.getStatusCode(), is(HttpStatus.SC_OK));

        int callsAfterFirstRequest = codelistMock.getTotalRequestCount();
        assertThat("Should have called XRepository for first request", callsAfterFirstRequest, is(greaterThan(0)));

        // and - make second request with SAME location
        Response response2 = LocationsConnector.getLocationsV3Details("München", 10);
        assertThat(response2.getStatusCode(), is(HttpStatus.SC_OK));

        int callsAfterSecondRequest = codelistMock.getTotalRequestCount();

        // then - no additional XRepository calls (using cached ARS data)
        assertThat(
            "Should not make additional XRepository calls for cached data",
            callsAfterSecondRequest,
            is(callsAfterFirstRequest)
        );
    }
}
