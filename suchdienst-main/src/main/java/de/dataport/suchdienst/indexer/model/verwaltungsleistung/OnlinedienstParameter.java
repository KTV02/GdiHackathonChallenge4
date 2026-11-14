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
import lombok.Data;

@Data
public class OnlinedienstParameter {

    @Schema(description = "parameterName, xs:string")
    private String name;

    @Schema(description = "parameterWert, xs:string")
    private String value;
}
