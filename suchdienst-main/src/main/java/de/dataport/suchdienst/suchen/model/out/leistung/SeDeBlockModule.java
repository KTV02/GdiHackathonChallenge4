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

import de.dataport.suchdienst.suchen.model.out.common.KeyValueObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import java.util.ArrayList;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.Value;

@SchemaProperty(
    name = "@type",
    schema = @Schema(
        type = "string",
        pattern = "^_module$(?![\\s\\S])",
        requiredMode = Schema.RequiredMode.REQUIRED,
        accessMode = Schema.AccessMode.READ_ONLY
    )
)
@EqualsAndHashCode(callSuper = true)
@Value
public class SeDeBlockModule extends SeDeBlock {

    @Schema(description = "Zusatzinformationen aus Modulen")
    Collection<KeyValueObject> additionalInformation = new ArrayList<>();

    boolean filled;
    boolean placeholder;
}
