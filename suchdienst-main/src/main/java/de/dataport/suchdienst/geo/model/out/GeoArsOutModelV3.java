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

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record GeoArsOutModelV3(
    @NonNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String ars,
    @NonNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String arsFederalstate,
    @NonNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
    @Nullable String gemeinde,
    @Nullable String plz,
    @Nullable String strasse,
    @Nullable String hausnummer,
    @Nullable
    @Schema(description = "Koordinaten [Längengrad, Breitengrad]")
    List<String> koordinaten
) {}
