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
import de.dataport.suchdienst.indexer.model.mapping.KeyValueObject;
import de.dataport.suchdienst.indexer.model.mapping.Link;
import de.dataport.suchdienst.indexer.model.mapping.ValidityPeriod;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.springframework.util.StringUtils;

@Data
@Builder
@Jacksonized
public class ServiceModuleDetailContent {

    private String title;
    private String text;

    @Builder.Default
    private final List<Link> links = new ArrayList<>();

    @Builder.Default
    private final List<ValidityPeriod> validityPeriods = new ArrayList<>();

    @Builder.Default
    private final Collection<KeyValueObject> additionalInformation = new ArrayList<>();

    @JsonIgnore
    public boolean hasContent() {
        return (
            StringUtils.hasText(getTitle()) ||
            StringUtils.hasText(getText()) ||
            !getLinks().isEmpty() ||
            !additionalInformation.isEmpty()
        );
    }
}
