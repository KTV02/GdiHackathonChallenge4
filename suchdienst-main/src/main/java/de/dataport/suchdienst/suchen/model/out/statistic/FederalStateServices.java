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
package de.dataport.suchdienst.suchen.model.out.statistic;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.Data;

@Schema(description = "Statistik für die Dienste nach Bundesland")
@Data
public class FederalStateServices {

    private Instant timestamp;
    private HashMap<String, Integer> stateServices;
    private Set<FederalStateStatistic> stateStatistics;

    public FederalStateServices(
        Map<String, Integer> stateServices,
        Set<FederalStateStatistic> stateStatistics
    ) {
        this();
        this.stateServices = new HashMap<>(stateServices);
        this.stateStatistics = stateStatistics;
    }

    public FederalStateServices() {
        this.timestamp = Instant.now();
    }
}
