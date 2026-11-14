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
package de.dataport.suchdienst.indexer.model.mapping;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Um bei der Erstellung von IDs zwischen OEs und LBs zu unterscheiden
 *
 * @author StubbeJa
 */
public enum IdentificationType {
    @JsonProperty("OE")
    OE,

    @JsonProperty("LB")
    LB,

    @JsonProperty("SP")
    SP,

    @JsonProperty("OD")
    OD,

    @JsonProperty("ZSOE")
    ZSOE,

    @JsonProperty("ZSOD")
    ZSOD,

    @JsonProperty("FO")
    FO,

    @JsonProperty("ZSFO")
    ZSFO,
}
