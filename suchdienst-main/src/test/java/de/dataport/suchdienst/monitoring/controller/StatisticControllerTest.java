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
package de.dataport.suchdienst.monitoring.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.config.ResourceBundleConfig;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.suchen.controller.StatisticController;
import de.dataport.suchdienst.suchen.model.out.statistic.FederalStateStatistic;
import de.dataport.suchdienst.suchen.model.out.statistic.FederalStateStatisticDetail;
import de.dataport.suchdienst.suchen.service.StatisticService;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Controller-Test für StatisticController mit @WebMvcTest.
 * Testet nur den HTTP-Layer, Business-Logik wird in StatisticServiceTest getestet.
 */
@WebMvcTest(StatisticController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ ResourceBundleConfig.class, PvogRequestContext.class, SuchdienstProperties.class })
class StatisticControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatisticService statisticService;

    @Test
    void shouldReturnClientStatistics() throws Exception {
        // Arrange - Mock Service-Response
        var testStatistics = createTestStatistics();
        when(statisticService.getStatistics()).thenReturn(testStatistics);

        // Act & Assert - HTTP-Layer testen
        mockMvc
            .perform(get("/v2/client-stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.stateServices").isEmpty())
            .andExpect(jsonPath("$.stateStatistics").isArray())
            .andExpect(jsonPath("$.stateStatistics", hasSize(2)))
            .andExpect(jsonPath("$.stateStatistics[0].ars").value("04"))
            .andExpect(jsonPath("$.stateStatistics[0].lbs.count").value(4))
            .andExpect(jsonPath("$.stateStatistics[0].lbs.lastUpdate").exists())
            .andExpect(jsonPath("$.stateStatistics[0].sps.count").value(4))
            .andExpect(jsonPath("$.stateStatistics[0].sps.lastUpdate").exists())
            .andExpect(jsonPath("$.stateStatistics[0].oes.count").value(4))
            .andExpect(jsonPath("$.stateStatistics[0].oes.lastUpdate").exists())
            .andExpect(jsonPath("$.stateStatistics[0].ods.count").value(4))
            .andExpect(jsonPath("$.stateStatistics[0].ods.lastUpdate").exists())
            .andExpect(jsonPath("$.stateStatistics[1].ars").value("05"))
            .andExpect(jsonPath("$.stateStatistics[1].lbs.count").value(5))
            .andExpect(jsonPath("$.stateStatistics[1].lbs.lastUpdate").exists())
            .andExpect(jsonPath("$.stateStatistics[1].sps.count").value(5))
            .andExpect(jsonPath("$.stateStatistics[1].sps.lastUpdate").exists())
            .andExpect(jsonPath("$.stateStatistics[1].oes.count").value(5))
            .andExpect(jsonPath("$.stateStatistics[1].oes.lastUpdate").exists())
            .andExpect(jsonPath("$.stateStatistics[1].ods.count").value(5))
            .andExpect(jsonPath("$.stateStatistics[1].ods.lastUpdate").exists());
    }

    /**
     * Erstellt Test-Statistik-Daten (einfache Version für Controller-Test).
     * Die komplexe Elasticsearch-Aggregations-Logik wird in StatisticServiceTest getestet.
     */
    private Set<FederalStateStatistic> createTestStatistics() {
        var timestamp = Instant.parse("2020-10-21T14:36:05.120357Z");

        Set<FederalStateStatistic> statistics = new LinkedHashSet<>();

        // State 04
        var state04 = new FederalStateStatistic("04");
        state04.setLbs(new FederalStateStatisticDetail(4L, timestamp));
        state04.setSps(new FederalStateStatisticDetail(4L, timestamp));
        state04.setOes(new FederalStateStatisticDetail(4L, timestamp));
        state04.setOds(new FederalStateStatisticDetail(4L, timestamp));
        statistics.add(state04);

        // State 05
        var state05 = new FederalStateStatistic("05");
        state05.setLbs(new FederalStateStatisticDetail(5L, timestamp));
        state05.setSps(new FederalStateStatisticDetail(5L, timestamp));
        state05.setOes(new FederalStateStatisticDetail(5L, timestamp));
        state05.setOds(new FederalStateStatisticDetail(5L, timestamp));
        statistics.add(state05);

        return statistics;
    }
}
