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

import lombok.Getter;

@Getter
public enum AccessChannel {
    ONLINE("100"),
    OFFLINE("200"),
    ONLINE_OFFLINE("000");

    private final String code;

    AccessChannel(String code) {
        this.code = code;
    }

    public boolean hasCode(String code) {
        return this.code.equals(code);
    }
}
