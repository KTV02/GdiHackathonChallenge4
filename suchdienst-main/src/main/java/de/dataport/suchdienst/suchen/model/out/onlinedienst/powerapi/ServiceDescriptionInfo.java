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

public record ServiceDescriptionInfo(
    @Schema(description = "Leistungsbeschreibungs-ID") String lbId,
    @Schema(description = "ARS der Onlinedienst Zuständigkeit") Set<String> ars,
    @Schema(description = "referenzLeiKa, xzufi:Code.LeiKaLeistung") Set<String> leikakeys
) {}
