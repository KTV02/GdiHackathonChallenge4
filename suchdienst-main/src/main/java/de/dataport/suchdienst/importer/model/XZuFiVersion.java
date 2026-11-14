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
package de.dataport.suchdienst.importer.model;

import lombok.Getter;

@Getter
public enum XZuFiVersion {
    XZUFI22("2.2.0"),
    XZUFI23("2.3.1");

    private final String version;

    XZuFiVersion(String version) {
        this.version = version;
    }
}
