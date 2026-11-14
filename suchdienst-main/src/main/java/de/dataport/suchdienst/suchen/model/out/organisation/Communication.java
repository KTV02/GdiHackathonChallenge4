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
package de.dataport.suchdienst.suchen.model.out.organisation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class Communication {

    private String code;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    private String value;
    private String valueAdditional;
    private String additional;
}
