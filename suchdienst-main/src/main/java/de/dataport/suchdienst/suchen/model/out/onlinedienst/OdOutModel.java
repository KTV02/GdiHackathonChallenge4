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
package de.dataport.suchdienst.suchen.model.out.onlinedienst;

import com.fasterxml.jackson.annotation.JsonFormat;
import de.dataport.suchdienst.suchen.model.out.common.AdditionalLink;
import de.dataport.suchdienst.suchen.model.out.common.ValidityPeriodOutModel;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OdOutModel extends AdditionalLink {

    @Schema(description = "id, xzufi:Identifikator")
    private String id;

    /**
     * @deprecated Da es mehr als ein Gültigkeitsdatum geben kann, ist dieses Feld deprecated
     * In neuen OutModels soll es unterdrückt werden, damit es in Zukunft entfernt werden kann.
     */
    @Schema(
        deprecated = true,
        description = "Da es mehr als ein Gültigkeitsdatum geben kann, ist dieses Feld deprecated"
    )
    @Deprecated(forRemoval = true, since = "4.0.9")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate validFrom;

    /**
     * @deprecated Da es mehr als ein Gültigkeitsdatum geben kann, ist dieses Feld deprecated
     * In neuen OutModels soll es unterdrückt werden, damit es in Zukunft entfernt werden kann.
     */
    @Schema(
        deprecated = true,
        description = "Da es mehr als ein Gültigkeitsdatum geben kann, ist dieses Feld deprecated"
    )
    @Deprecated(forRemoval = true, since = "4.0.9")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate validTo;

    @Schema(description = "gueltigkeit, xzufi:Zeitraum")
    private List<ValidityPeriodOutModel> validityPeriods;

    @ArraySchema(schema = @Schema(implementation = String.class), uniqueItems = true)
    private Set<String> executionLanguages;

    @Schema(
        description = "durchfuehrungStaatsangehoerigkeit, xzufi:Code.StaatsangehoerigkeitDestatis"
    )
    private Set<NationalityCode> executionNationalities;

    @Schema(description = "vertrauensniveau, xzufi:Code.Vertrauensniveau", example = "00")
    private String levelOfConfidence;
}
