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
package suchdienst.integrationstest.tests.cache;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.LocationsConnector;
import suchdienst.integrationstest.mocks.BKGMock;
import suchdienst.integrationstest.utils.ArtifactWriter;
import suchdienst.integrationstest.utils.RedisUtil;

/**
 * Testziel: Überprüfung der allg. Redis-Cache-Funktionalität (anhand des Location-Endpunkts)
 *
 * Diese Testklasse stellt sicher, dass:
 * - Der Redis-Cache korrekt für wiederholte Location-Anfragen funktioniert
 * - Keine Cache-Zugriffsfehler während der Tests auftreten
 * - Verschiedene Location-Abfragen unabhängig voneinander gecacht werden
 * - Der Cache mehrere schnelle Anfragen ohne Fehler verarbeiten kann
 */
@Isolated
@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class RedisCachingTest {

    private static BKGMock bkgMock;
    private ArtifactWriter writer;

    @BeforeAll
    static void setup() {
        bkgMock = new BKGMock();
    }

    @AfterAll
    static void teardown() throws Exception {
        bkgMock.reset();
        RedisUtil.flushRedisCache();
    }

    /**
     * Testet, dass Redis-Caching ordnungsgemäß für Location-Anfragen funktioniert.
     *
     * Tests verwenden verschiedene Städte, um Cache-Konflikte zwischen Testmethoden zu vermeiden.
     * Mit SAME_THREAD-Ausführungsmodus laufen Tests sequenziell in vorhersehbarer Reihenfolge.
     */
    @Test
    void shouldUseCacheForSubsequentLocationRequests(TestInfo testInfo) throws Exception {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        writer = new ArtifactWriter(testcasename);

        // Setup mock
        String json = IOUtils.resourceToString("/bkg/OrtHamburg.json", StandardCharsets.UTF_8);
        bkgMock.createLocationsResponseBkgMock("Hamburg", json);

        // Erste Anfrage - sollte den BKG-Mock treffen und den Cache füllen
        Response response1 = LocationsConnector.getLocationsV3ByQuery("Hamburg");

        assertThat("Erste Anfrage sollte erfolgreich sein", response1.getStatusCode(), is(HttpStatus.SC_OK));
        JsonPath jsonPath1 = response1.jsonPath();
        List<String> names1 = jsonPath1.getList("name");
        assertThat("Should have location results", names1, is(notNullValue()));
        assertThat("Should have at least one location", names1.size(), is(greaterThan(0)));

        // Überprüfen, dass der Mock für die erste Anfrage aufgerufen wurde
        bkgMock.verifyRSNachgelagertBkgMockResponse("Hamburg", 1);

        // Zweite Anfrage - sollte aus dem Cache bedient werden, ohne den Mock erneut zu treffen
        Response response2 = LocationsConnector.getLocationsV3ByQuery("Hamburg");

        assertThat("Second request should succeed", response2.getStatusCode(), is(HttpStatus.SC_OK));
        JsonPath jsonPath2 = response2.jsonPath();
        List<String> names2 = jsonPath2.getList("name");
        assertThat("Should have location results from cache", names2, is(notNullValue()));
        assertThat("Should have at least one location", names2.size(), is(greaterThan(0)));

        // Verify the results are the same
        assertThat("Cached results should match original", names2, is(names1));

        // Überprüfen, dass der Mock immer noch nur einmal aufgerufen wurde (zweite Anfrage nutzte Cache)
        bkgMock.verifyRSNachgelagertBkgMockResponse("Hamburg", 1);

        // Server-Log abrufen und überprüfen, dass keine Cache-Fehler aufgetreten sind
        List<String> serverLog = writer.getCurrentServerLog();
        assertThat(
            "Should not have any cache errors in server log",
            serverLog,
            everyItem(not(Matchers.containsString("Fehler beim Cache-Zugriff - wird ignoriert.")))
        );
    }

    /**
     * Testet, dass verschiedene Location-Abfragen den Cache unabhängig voneinander nutzen.
     *
     * Überprüft, dass Caching für mehrere verschiedene Locations korrekt funktioniert
     * und dass keine Cache-Zugriffsfehler auftreten.
     */
    @Test
    void shouldCacheDifferentLocationsIndependently(TestInfo testInfo) throws Exception {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        writer = new ArtifactWriter(testcasename);

        // Mocks einrichten
        String hamburgJson = IOUtils.resourceToString("/bkg/OrtHamburg.json", StandardCharsets.UTF_8);
        bkgMock.createLocationsResponseBkgMock("Hamburg", hamburgJson);

        String berlinJson = IOUtils.resourceToString("/bkg/OrtBerlin.json", StandardCharsets.UTF_8);
        bkgMock.createLocationsResponseBkgMock("Berlin", berlinJson);

        // Erste Anfrage für Hamburg
        Response hamburgResponse1 = LocationsConnector.getLocationsV3ByQuery("Hamburg");
        assertThat(hamburgResponse1.getStatusCode(), is(HttpStatus.SC_OK));
        bkgMock.verifyRSNachgelagertBkgMockResponse("Hamburg", 1);

        // Erste Anfrage für Berlin
        Response berlinResponse1 = LocationsConnector.getLocationsV3ByQuery("Berlin");
        assertThat(berlinResponse1.getStatusCode(), is(HttpStatus.SC_OK));
        bkgMock.verifyRSNachgelagertBkgMockResponse("Berlin", 1);

        // Zweite Anfrage für Hamburg - sollte Cache nutzen
        Response hamburgResponse2 = LocationsConnector.getLocationsV3ByQuery("Hamburg");
        assertThat(hamburgResponse2.getStatusCode(), is(HttpStatus.SC_OK));
        bkgMock.verifyRSNachgelagertBkgMockResponse("Hamburg", 1);

        // Zweite Anfrage für Berlin - sollte Cache nutzen
        Response berlinResponse2 = LocationsConnector.getLocationsV3ByQuery("Berlin");
        assertThat(berlinResponse2.getStatusCode(), is(HttpStatus.SC_OK));
        bkgMock.verifyRSNachgelagertBkgMockResponse("Berlin", 1);

        // Überprüfen, dass keine Cache-Fehler im Server-Log vorhanden sind
        List<String> serverLog = writer.getCurrentServerLog();
        assertThat(
            "Should not have any cache errors in server log",
            serverLog,
            everyItem(not(Matchers.containsString("Fehler beim Cache-Zugriff - wird ignoriert.")))
        );
    }

    /**
     * Testet, dass der Cache mit mehreren schnellen Anfragen korrekt funktioniert.
     *
     * Simuliert mehrere quasi-parallele Anfragen, um zu überprüfen, dass der Cache
     * schnelle Zugriffsmuster ohne Fehler verarbeitet.
     */
    @Test
    void shouldHandleMultipleRapidRequestsWithCache(TestInfo testInfo) throws Exception {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        writer = new ArtifactWriter(testcasename);

        // Mock einrichten
        String json = IOUtils.resourceToString("/bkg/OrtMünchen.json", StandardCharsets.UTF_8);
        bkgMock.createLocationsResponseBkgMock("München", json);

        // Erste Anfrage durchführen, um Cache zu füllen
        Response response1 = LocationsConnector.getLocationsV3ByQuery("München");
        assertThat(response1.getStatusCode(), is(HttpStatus.SC_OK));

        // Mehrere schnelle Anfragen durchführen - alle sollten Cache nutzen
        for (int i = 0; i < 5; i++) {
            Response response = LocationsConnector.getLocationsV3ByQuery("München");
            assertThat("Request " + (i + 2) + " should succeed", response.getStatusCode(), is(HttpStatus.SC_OK));
        }

        // Überprüfen, dass der Mock nur einmal aufgerufen wurde (erste Anfrage)
        bkgMock.verifyRSNachgelagertBkgMockResponse("München", 1);

        // Überprüfen, dass keine Cache-Fehler im Server-Log vorhanden sind
        List<String> serverLog = writer.getCurrentServerLog();
        assertThat(
            "Should not have any cache errors in server log",
            serverLog,
            everyItem(not(Matchers.containsString("Fehler beim Cache-Zugriff - wird ignoriert.")))
        );
    }
}
