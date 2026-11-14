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

import com.fasterxml.jackson.annotation.JsonView;
import de.dataport.suchdienst.suchen.model.views.JsonViews;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

/**
 * Repräsentiert einen Ars mit weiteren zugehörigen Daten
 *
 * @author PVOG
 */
@Schema(
    description = "Amtlicher Regionalschlüssel (ARS) mit weiteren zugehörigen Daten",
    example = """
    {
            "name": "Neumünster, Stadt",
            "ars": "010040000000",
            "ags": "01004000",
            "zip": "24534",
            "displayZip": "24534",
            "id": "010040000000",
            "zipCodeCount": 1
        }
    """
)
public record ArsObject(
    String id,
    String name,
    String ars,
    String ags,
    @Deprecated @Schema(deprecated = true) String zip,
    @Schema(
        description = "Hält die Postleitzahl oder ein entsprechendes Template, wenn der ARS mehrere PLZ hat."
    )
    String displayZip,
    @Schema(description = "Anzahl der bekannten PLZ für diesen Ort") int zipCodeCount,
    Coordinate location,
    @Schema(
        deprecated = true,
        description = "interne Datenstruktur für die Vorschlagssuche. Wird perspektivisch aus der API entfernt"
    )
    @Deprecated(forRemoval = true, since = "4.1.0")
    @JsonView(JsonViews.Detail.class)
    Suggestions suggest
) {
    public record Suggestions(Set<String> input, String weight) {}

    public record Coordinate(float lon, float lat) {}
}
