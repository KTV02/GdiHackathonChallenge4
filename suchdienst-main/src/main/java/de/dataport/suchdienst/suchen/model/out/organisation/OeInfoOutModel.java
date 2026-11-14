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
package de.dataport.suchdienst.suchen.model.out.organisation;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.dataport.suchdienst.suchen.model.out.common.ResponsibilityRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(
    description = "Organisationseinheit Rolle und Zuständigkeit",
    example = """
            {
                "id": "L100042.OE.868",
                "title": "Landgericht München I",
                "isPublicDisplayAllowed": true,
                "role": {
                    "name": "Zuständige Stelle und Ansprechpunkt",
                    "code": "01"
                },
                "criteria": [
                    {
                        "name": "Kriterium",
                        "code": "01",
                        "auspraegungen": [
                            "A",
                            "B",
                            "C"
                        ]
                    }
                ]
            }
    """
)
public record OeInfoOutModel(
    @Schema(description = "id, xzufi:Identifikator", requiredMode = Schema.RequiredMode.REQUIRED)
    String id,
    @Schema(description = "name, xzufi:NameOrganisation") String title,
    @Schema(description = "Kommt aus XZuFi 2.2.0 \"ZustaendigkeitOrganisationseinheit:rolle\"")
    ResponsibilityRole role,
    @Schema(description = "Kommt aus XZuFi 2.2.0 \"ZustaendigkeitOrganisationseinheit:kriterium\"")
    List<ResponsibilityCriterion> criteria,
    @JsonProperty("isPublicDisplayAllowed")
    @Schema(
        description = "kennzeichenOeffentlichAnzeigen, xs:boolean",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    Boolean publicDisplayAllowed
) {}
