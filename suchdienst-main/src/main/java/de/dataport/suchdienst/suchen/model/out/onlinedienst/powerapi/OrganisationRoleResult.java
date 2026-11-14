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
import java.util.Set;

/**
 * Eine Rolle einer Organisationseinheit für den betreffenden Onlinedienst.
 */
public record OrganisationRoleResult(
    @Schema(description = "Typ der Rolle") RoleType role,
    @Schema(description = "Leistungsbeschreibungs-ID") String lbId,
    @Schema(description = "LeiKa-Schlüssel") Set<String> leikakeys,
    @Schema(description = "ARS der Organisationseinheit Zuständigkeit") Set<String> ars
) {}
