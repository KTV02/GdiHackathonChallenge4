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
import de.dataport.suchdienst.elasticsearch.api.ElasticInternalReadConnector;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/**
 * Aggregiert die offenen Einträge in der ImportQueue pro SchemeAgencyId.
 */
@Component
public class ImportQueueCountContributor implements DataOverviewContributor {

    private final ElasticInternalReadConnector elasticReadConnector;

    public ImportQueueCountContributor(ElasticInternalReadConnector elasticReadConnector) {
        this.elasticReadConnector = elasticReadConnector;
    }

    @Override
    public Consumer<Map<String, DataOverviewOutModel.Builder>> collectDataParallel(
        Set<String> schemeAgencyIds
    ) {
        Map<String, Long> importQueueCounts = elasticReadConnector.getDataOverviewImportQueue(
            schemeAgencyIds
        );

        return builders ->
            builders.forEach((schemeAgencyId, builder) ->
                builder.importQueueCount(importQueueCounts.getOrDefault(schemeAgencyId, 0L))
            );
    }
}
