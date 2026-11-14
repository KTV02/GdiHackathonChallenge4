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
package de.dataport.suchdienst.indexer.model.verwaltungsleistung;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.springframework.util.StringUtils;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Jacksonized
public non-sealed class ServiceModuleDate extends AbstractServiceModule {

    private LocalDate date;

    @JsonIgnore
    @Override
    public boolean hasContent() {
        return date != null || StringUtils.hasText(super.getText());
    }
}
