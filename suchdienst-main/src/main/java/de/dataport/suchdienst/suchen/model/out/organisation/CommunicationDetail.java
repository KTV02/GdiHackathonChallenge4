// SPDX-License-Identifier: EUPL-1.2
// Copyright (c) 2026 Dataport AöR.
// Beschreibung: Ausgabe eines Kommunikationssystems mit Sichtbarkeitskennzeichen.
package de.dataport.suchdienst.suchen.model.out.organisation;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CommunicationDetail {

    private String code;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    private String value;
    private String valueAdditional;
    private String additional;

    @JsonProperty("isPublicDisplayAllowed")
    @Schema(
        description = "kennzeichenOeffentlichAnzeigen, xs:boolean",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean publicDisplayAllowed;
}
