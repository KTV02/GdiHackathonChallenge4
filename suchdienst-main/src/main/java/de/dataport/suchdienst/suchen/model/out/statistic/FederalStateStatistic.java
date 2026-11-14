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

import lombok.Data;

@Data
public class FederalStateStatistic {

    private final String ars;
    private FederalStateStatisticDetail lbs = new FederalStateStatisticDetail();
    private FederalStateStatisticDetail ods = new FederalStateStatisticDetail();
    private FederalStateStatisticDetail oes = new FederalStateStatisticDetail();
    private FederalStateStatisticDetail sps = new FederalStateStatisticDetail();
}
