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
package de.dataport.suchdienst.indexer.model.verwaltungsleistung;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class Publisher {

    @Schema(description = "bezeichnung, xoev-lc:String.Latin")
    private List<String> name;

    @Schema(description = "kurzbezeichnung, xoev-lc:String.Latin")
    private List<String> shortName;

    @Schema(description = "beschreibung, xzufi:String.Localized")
    private String description;

    public Publisher() {
        this.name = new ArrayList<>();
        this.shortName = new ArrayList<>();
    }
}
