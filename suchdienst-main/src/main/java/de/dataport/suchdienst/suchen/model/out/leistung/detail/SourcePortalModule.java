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
package de.dataport.suchdienst.suchen.model.out.leistung.detail;

import io.swagger.v3.oas.annotations.media.Schema;

public record SourcePortalModule(
    @Schema(description = "titel, xs:string") String title,
    @Schema(description = "uri, xs:anyURI", requiredMode = Schema.RequiredMode.REQUIRED) String uri
) {}
