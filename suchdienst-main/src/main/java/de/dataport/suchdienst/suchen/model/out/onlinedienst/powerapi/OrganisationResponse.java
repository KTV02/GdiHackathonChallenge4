/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Antwortmodell für die Abfrage von Organisationseinheiten eines Onlinedienstes.
 */
public record OrganisationResponse(
    @Schema(description = "Liste der zuständigen Organisationseinheiten")
    List<OrganisationResult> organisations,

    @Schema(description = "versionsinformation, xzufi:Versionsinformation")
    Instant externalLastUpdate
) {}
