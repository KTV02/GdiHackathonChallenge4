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
import de.dataport.suchdienst.indexer.model.mapping.Link;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.springframework.util.StringUtils;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Jacksonized
public non-sealed class ServiceModuleDetail extends AbstractServiceModule {

    @Builder.Default
    private final List<Link> links = new ArrayList<>();

    @Builder.Default
    private final Collection<ServiceModuleDetailContent> details = new ArrayList<>();

    @JsonIgnore
    @Override
    public boolean hasContent() {
        return (
            !links.isEmpty() ||
            details.stream().anyMatch(ServiceModuleDetailContent::hasContent) ||
            StringUtils.hasText(super.getText())
        );
    }
}
