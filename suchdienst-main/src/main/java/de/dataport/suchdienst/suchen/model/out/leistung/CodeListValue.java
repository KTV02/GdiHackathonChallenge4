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
package de.dataport.suchdienst.suchen.model.out.leistung;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Locale;

/**
 * Ein weiterer Wert eines Codelist-Eintrags
 */
public record CodeListValue(
    String key,
    String value,
    @Schema(implementation = String.class) Locale locale
) {}
