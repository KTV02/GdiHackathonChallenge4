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
package de.dataport.suchdienst.suchen.model.out.leistung.detail;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.NationalityCode;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.OnlineServiceStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class OnlineServiceLink {

    @Schema(description = "id, xzufi:Identifikator")
    private String id;

    @Schema(description = "uri, xs:anyURI", requiredMode = Schema.RequiredMode.REQUIRED)
    private String uri;

    @Schema(description = "titel, xs:string")
    private String title;

    @Schema(description = "beschreibung, xs:string")
    private String description;

    @Schema(description = "Status des Onlinedienstes")
    private OnlineServiceStatus status;

    @JsonProperty("isPublicDisplayAllowed")
    @Schema(
        description = "kennzeichenOeffentlichAnzeigen, xs:boolean",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean publicDisplayAllowed;

    @ArraySchema(schema = @Schema(implementation = String.class), uniqueItems = true)
    private Set<String> executionLanguages;

    @Schema(
        description = "durchfuehrungStaatsangehoerigkeit, xzufi:Code.StaatsangehoerigkeitDestatis"
    )
    private Set<NationalityCode> executionNationalities;
}
