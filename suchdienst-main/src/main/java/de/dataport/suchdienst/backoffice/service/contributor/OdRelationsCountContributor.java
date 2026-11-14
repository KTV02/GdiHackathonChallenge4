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

import de.dataport.suchdienst.backoffice.model.out.DataOverviewOutModel;
import de.dataport.suchdienst.elasticsearch.api.AliasNames;
import de.dataport.suchdienst.elasticsearch.api.ElasticInternalReadConnector;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/**
 * Aggregiert die Onlinedienst-Relationen pro SchemeAgencyId aus dem gemeinsam genutzten Write-Alias.
 */
@Component
public class OdRelationsCountContributor implements DataOverviewContributor {

    private final ElasticInternalReadConnector elasticReadConnector;

    public OdRelationsCountContributor(ElasticInternalReadConnector elasticReadConnector) {
        this.elasticReadConnector = elasticReadConnector;
    }

    @Override
    public Consumer<Map<String, DataOverviewOutModel.Builder>> collectDataParallel(
        Set<String> schemeAgencyIds
    ) {
        Map<AliasNames, Map<String, Long>> countsByAlias =
            elasticReadConnector.getAggregatedDocumentCounts(
                schemeAgencyIds,
                AliasNames.ONLINEDIENSTE_RELATIONEN
            );

        Map<String, Long> odRelCounts = countsByAlias.get(AliasNames.ONLINEDIENSTE_RELATIONEN);
        return builders -> {
            if (odRelCounts != null) {
                odRelCounts.forEach((schemeAgencyId, count) -> {
                    var builder = builders.get(schemeAgencyId);
                    if (builder != null) {
                        builder.odRelCount(count);
                    }
                });
            }
        };
    }
}
