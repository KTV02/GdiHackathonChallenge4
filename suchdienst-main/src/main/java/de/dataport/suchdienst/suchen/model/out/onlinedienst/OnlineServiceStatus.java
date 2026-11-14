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
package de.dataport.suchdienst.suchen.model.out.onlinedienst;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true, description = "Status eines Onlinedienstes")
public enum OnlineServiceStatus {
    UNDEFINED,
    PLANNING,
    INITIALIZATION,
    TEST,
    PRODUCTION,
    MAINTENANCE,
    DEACTIVATION,
}
