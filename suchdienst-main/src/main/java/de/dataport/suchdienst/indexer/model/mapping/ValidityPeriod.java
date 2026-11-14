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
package de.dataport.suchdienst.indexer.model.mapping;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record ValidityPeriod(
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("gte")
    @Schema(description = "beginn, xs:date")
    LocalDate validFrom,
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("lte")
    @Schema(description = "ende, xs:date")
    LocalDate validTo
) {
    public boolean isValidDate(LocalDate date) {
        return (
            date == null ||
            ((validFrom == null || !date.isBefore(validFrom)) &&
                (validTo == null || !date.isAfter(validTo)))
        );
    }
}
