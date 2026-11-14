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
package de.dataport.suchdienst.geo.model.out;

import org.jspecify.annotations.Nullable;

public record GeoArsOutModelV2(
    String ars,
    String arsFederalstate,
    @Nullable String municipality,
    String name,
    @Nullable String plz
) {}
