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
package de.dataport.suchdienst.backoffice.service.contributor;

import static java.util.stream.Collectors.toMap;

import de.dataport.suchdienst.backoffice.model.out.DataOverviewOutModel;
import de.dataport.suchdienst.elasticsearch.api.AliasNames;
import de.dataport.suchdienst.elasticsearch.api.ElasticInternalReadConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexConfiguration;
import de.dataport.suchdienst.util.Bundeslaender;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/**
 * Zählt die Verwaltungsleistungen pro Bundesland.
 * <p>
 * VL-Indizes existieren pro Bundesland als eigener Write-Alias.
 * Die Zählung erfolgt über eine einfache Count-Query pro Alias.
 */
@Component
public class VlCountContributor implements DataOverviewContributor {

    private final ElasticInternalReadConnector elasticReadConnector;
    private final IndexConfiguration indexConfiguration;

    public VlCountContributor(
        ElasticInternalReadConnector elasticReadConnector,
        IndexConfiguration indexConfiguration
    ) {
        this.elasticReadConnector = elasticReadConnector;
        this.indexConfiguration = indexConfiguration;
    }

    @Override
    public Consumer<Map<String, DataOverviewOutModel.Builder>> collectDataParallel(
        Set<String> schemeAgencyIds
    ) {
        Map<String, String> writeAliasToFederalState = AliasNames.ARS_MAPPING.entrySet()
            .stream()
            .collect(
                toMap(
                    e -> indexConfiguration.getCurrentWriteAliasName(e.getValue()),
                    Map.Entry::getKey
                )
            );

        List<String> writeAliases = List.copyOf(writeAliasToFederalState.keySet());
        Map<String, Long> countForWriteAlias = elasticReadConnector.countDocuments(writeAliases);

        return builders ->
            writeAliasToFederalState.forEach((writeAlias, arsFederalState) -> {
                long count = countForWriteAlias.getOrDefault(writeAlias, 0L);
                String schemeAgencyId = Bundeslaender.getByArsFederalstate(
                    arsFederalState
                ).getSchemeAgencyId();
                var builder = builders.get(schemeAgencyId);
                if (builder != null) {
                    builder.vlCount(count);
                }
            });
    }
}
