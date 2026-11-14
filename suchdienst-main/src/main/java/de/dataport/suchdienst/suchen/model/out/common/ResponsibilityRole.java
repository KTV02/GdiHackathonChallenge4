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
package de.dataport.suchdienst.suchen.model.out.common;

import io.swagger.v3.oas.annotations.media.Schema;

public record ResponsibilityRole(
    @Schema(
        description = "Aufgelöster Codelisten Eintrag",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String name,
    @Schema(description = "Code aus Codeliste", requiredMode = Schema.RequiredMode.REQUIRED)
    String code,
    @Schema(description = "URI der Codeliste", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String uri,
    @Schema(description = "Version der Codeliste", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String version
) {}
