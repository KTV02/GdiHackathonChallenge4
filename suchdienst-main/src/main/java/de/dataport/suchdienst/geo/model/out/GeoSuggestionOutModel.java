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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.dataport.suchdienst.geo.model.WeightedEntityGeoName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(
    example = """
            {
                "name": "Berlingerode",
                "weight": 1.8060234
            }
    """
)
@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeoSuggestionOutModel implements WeightedEntityGeoName {

    private String name;
    private float weight;
}
