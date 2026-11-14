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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SourcePortal {

    @Schema(description = "titel, xs:string")
    private String title;

    @Schema(description = "uri, xs:anyURI", requiredMode = Schema.RequiredMode.REQUIRED)
    private String uri;

    @Schema(
        description = "Ersetzungs-Flag, falls das Modul aus einer Spezialisierung kommt",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean replace;
}
