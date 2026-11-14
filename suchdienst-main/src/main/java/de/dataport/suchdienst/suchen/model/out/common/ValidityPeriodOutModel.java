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
package de.dataport.suchdienst.suchen.model.out.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ValidityPeriodOutModel {

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(
        description = "beginn, xs:date",
        format = "date",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDate validFrom;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(
        description = "ende, xs:date",
        format = "date",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDate validTo;
}
