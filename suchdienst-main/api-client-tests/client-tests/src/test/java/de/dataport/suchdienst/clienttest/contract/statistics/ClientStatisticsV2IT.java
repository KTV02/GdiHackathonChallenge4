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
package de.dataport.suchdienst.clienttest.contract.statistics;

import static de.dataport.suchdienst.clienttest.support.HttpContractAssertions.assertJsonSuccess;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dataport.suchdienst.client.api.StatistikApi;
import de.dataport.suchdienst.clienttest.support.StrictApiClientFactory;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Testziel: GET /v2/client-stats liefert die Statistik eines laufenden Suchdienstes und lässt
 * Status, Zeitstempel sowie Statistikmodelle korrekt binden, auch bei leerem Datenbestand.
 */
class ClientStatisticsV2IT {

    @Test
    void retrievesAndBindsStatisticsFromSuchdienst() {
        String baseUrl = System.getProperty("suchdienst.base-url", "http://localhost:8080");
        var client = StrictApiClientFactory.create(StrictApiClientFactory.builder(), baseUrl);
        Instant beforeRequest = Instant.now();
        var response = new StatistikApi(client).getClientStatsV2WithHttpInfo();

        assertJsonSuccess(response);
        var statistics = response.getBody();
        assertNotNull(statistics);
        assertNotNull(statistics.getTimestamp());
        assertFalse(statistics.getTimestamp().toInstant().isBefore(beforeRequest.minusSeconds(5)));
        assertFalse(statistics.getTimestamp().toInstant().isAfter(Instant.now().plusSeconds(5)));
        assertNotNull(statistics.getStateServices());
        assertNotNull(statistics.getStateStatistics());
        // Ein leerer Index ist zulässig; vorhandene Einträge müssen vollständig gebunden sein.
        for (var state : statistics.getStateStatistics()) {
            assertNotNull(state.getArs());
            assertTrue(state.getArs().matches("\\d{2}"));
            assertNotNull(state.getLbs());
            assertNotNull(state.getOds());
            assertNotNull(state.getOes());
            assertNotNull(state.getSps());
            assertTrue(state.getLbs().getCount() >= 0);
            assertTrue(state.getOds().getCount() >= 0);
            assertTrue(state.getOes().getCount() >= 0);
            assertTrue(state.getSps().getCount() >= 0);
        }
    }
}
