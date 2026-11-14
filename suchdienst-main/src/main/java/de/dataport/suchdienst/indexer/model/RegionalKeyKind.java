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
package de.dataport.suchdienst.indexer.model;

/**
 * Definiert, ob AGS oder ARS verwendet wird.
 */
public enum RegionalKeyKind {
    ARS,
    AGS;

    public String get(ArsObject o) {
        return this == ARS ? o.getArs() : o.getAgs();
    }
}
