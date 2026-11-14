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
package de.dataport.suchdienst.suchen.model.out.formular;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Out-Model für Formulare.
 */
public record FoOutModel(
    @Schema(description = "id, xzufi:id", requiredMode = Schema.RequiredMode.REQUIRED) String id,
    @Schema(description = "Name des Formulars, xzufi:bezeichnung") String name,
    @Schema(description = "Beschreibung des Formulars, xzufi:beschreibung") String description,
    @Schema(description = "MimeType, xzufi:mimeCode") String mimeCode,
    @Schema(
        description = "Link zum Formular, xzufi:uri",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String uri
) {}
