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
package de.dataport.suchdienst.suchen.model.out.leistung;

import de.dataport.suchdienst.util.ResultPage;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Dieses Ausgabe-Modell wird seit PV-8658 für die Suche nach Leistungen inkl. Facettierung nach Lebenslagen verwendet.
 * Es enthält neben den Suchtreffern pro Root-Lebenslage eine Aggregation mit Zählern der in der Suche gefundenen Lebenslagen.
 */
@Data
public class SearchOutModel<T extends SdOutModelBase> {

    /**
     * Treffer der Leistungs-Suche, paged.
     */
    private ResultPage<T> serviceDescriptions;

    private final Facades facades;

    public SearchOutModel(
        ResultPage<T> sdOutModels,
        Set<LifeSituationAggOutModel> lifeSituationAggOutModels
    ) {
        this.serviceDescriptions = sdOutModels;
        this.facades = new Facades(lifeSituationAggOutModels);
    }

    /**
     * Klammert verschiedene Objekte, die für die clientseitige Facettierung verwendet werden. Erst einmal gibt es hier nur
     * die Lebenslagen.
     */
    @Data
    @AllArgsConstructor
    static class Facades {

        /**
         * Pro Root-Lebenslagen (Bürger, Unternehmen) ein Aggregationswert mitsamt Aggregation auf Unter-Ebenen.
         */
        private Set<LifeSituationAggOutModel> personalMatters;
    }
}
