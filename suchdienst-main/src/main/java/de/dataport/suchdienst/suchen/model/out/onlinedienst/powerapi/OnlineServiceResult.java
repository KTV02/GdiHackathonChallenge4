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

import de.dataport.suchdienst.suchen.model.out.common.HyperLink;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Ein einzelner Onlinedienst-Eintrag innerhalb einer paginierten Ergebnisliste.
 *
 * <p>Enthält neben den Kerndaten des Onlinedienstes die zugehörigen
 * Leistungsrelationen ({@link ServiceDescriptionInfo}).</p>
 */
public record OnlineServiceResult(
    @Schema(description = "id, xzufi:Identifikator") String id,
    @Schema(description = "Name des Onlinedienstes") String name,
    @Schema(description = "Beschreibung des Onlinedienstes") String description,
    @Schema(description = "Links, unter denen der Onlinedienst erreichbar ist")
    List<HyperLink> links,
    @Schema(description = "versionsinformation, xzufi:Versionsinformation")
    Instant externalLastUpdate,
    @Schema(description = "Mit dem Onlinedienst verknüpfte Leistungen")
    List<ServiceDescriptionInfo> serviceDescriptions
) {}
