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
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import java.util.ArrayList;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.Value;

// SeDeBlockModule_Detail wird im Java-Client zu SeDeBlockModuleDetail normalisiert.
// Ein eigener Schemaname verhindert die Kollision mit diesem gruppierten Modul.
@Schema(name = "SeDeBlockGroupedModule")
@SchemaProperty(
    name = "@type",
    schema = @Schema(
        type = "string",
        pattern = "^_moduleDetail$(?![\\s\\S])",
        requiredMode = Schema.RequiredMode.REQUIRED,
        accessMode = Schema.AccessMode.READ_ONLY
    )
)
@EqualsAndHashCode(callSuper = true)
@Value
public class SeDeBlockModuleDetail extends SeDeBlock {

    Collection<SeDeBlockModule> details = new ArrayList<>();

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    boolean filled;

    @Override
    public boolean isFilled() {
        return filled;
    }

    @Override
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public boolean isPlaceholder() {
        return false;
    }
}
