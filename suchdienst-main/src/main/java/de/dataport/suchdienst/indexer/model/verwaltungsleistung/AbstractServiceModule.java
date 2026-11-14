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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.dataport.suchdienst.indexer.model.AccessChannel;
import de.dataport.suchdienst.indexer.model.mapping.ValidityPeriod;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ServiceModule.class, name = "_module"),
    @JsonSubTypes.Type(value = ServiceModuleDetail.class, name = "_moduleDetail"),
    @JsonSubTypes.Type(value = ServiceModuleDate.class, name = "_moduleDate"),
    @JsonSubTypes.Type(value = ServiceModuleLink.class, name = "_moduleLink"),
})
@Data
@SuperBuilder
@NoArgsConstructor
public abstract sealed class AbstractServiceModule
    permits ServiceModule, ServiceModuleDate, ServiceModuleDetail, ServiceModuleLink
{

    private String title;
    private String text;

    @Builder.Default
    private final List<ValidityPeriod> validityPeriods = new ArrayList<>();

    @Setter
    @Builder.Default
    private AccessChannel accessChannel = AccessChannel.ONLINE_OFFLINE;

    public abstract boolean hasContent();
}
