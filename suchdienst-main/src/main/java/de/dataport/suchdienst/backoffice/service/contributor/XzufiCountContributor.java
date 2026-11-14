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
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.ObjLongConsumer;
import org.springframework.stereotype.Component;

/**
 * Aggregiert die Dokumentanzahlen aus den gemeinsam genutzten XZuFi-Indices
 * (LB, OE, OD, SP, ZS) per SchemeAgencyId und schreibt sie in die Builder.
 */
@Component
public class XzufiCountContributor implements DataOverviewContributor {

    // Builder-Setter werden als ObjLongConsumer referenziert; der fluent-Rückgabewert (Builder) wird dabei ignoriert.
    private static final Map<
        IndexNames,
        ObjLongConsumer<DataOverviewOutModel.Builder>
    > FIELD_SETTERS = Map.of(
        IndexNames.LEISTUNGSBESCHREIBUNG,
        DataOverviewOutModel.Builder::lbCount,
        IndexNames.ORGANISATIONSEINHEITEN,
        DataOverviewOutModel.Builder::oeCount,
        IndexNames.ONLINEDIENSTE,
        DataOverviewOutModel.Builder::odCount,
        IndexNames.SPEZIALISIERUNGEN,
        DataOverviewOutModel.Builder::spCount,
        IndexNames.ZUSTAENDIGKEITEN,
        DataOverviewOutModel.Builder::zsCount
    );

    static {
        // Sicherstellen, dass FIELD_SETTERS alle XZUFI_INDICES abdeckt (und umgekehrt)
        if (!FIELD_SETTERS.keySet().equals(Set.copyOf(IndexNames.XZUFI_INDICES))) {
            throw new AssertionError(
                "FIELD_SETTERS und IndexNames.XZUFI_INDICES sind nicht synchron: " +
                    "FIELD_SETTERS=" +
                    FIELD_SETTERS.keySet() +
                    ", XZUFI_INDICES=" +
                    IndexNames.XZUFI_INDICES
            );
        }
    }

    private final ElasticInternalReadConnector elasticReadConnector;

    public XzufiCountContributor(ElasticInternalReadConnector elasticReadConnector) {
        this.elasticReadConnector = elasticReadConnector;
    }

    @Override
    public Consumer<Map<String, DataOverviewOutModel.Builder>> collectDataParallel(
        Set<String> schemeAgencyIds
    ) {
        IndexNames[] indexNames = IndexNames.XZUFI_INDICES.toArray(IndexNames[]::new);
        Map<IndexNames, Map<String, Long>> countsByIndex =
            elasticReadConnector.getAggregatedDocumentCounts(schemeAgencyIds, indexNames);

        return builders ->
            countsByIndex.forEach((indexName, countsByAgencyId) ->
                countsByAgencyId.forEach((schemeAgencyId, count) -> {
                    var builder = builders.get(schemeAgencyId);
                    if (builder != null) {
                        FIELD_SETTERS.get(indexName).accept(builder, count);
                    }
                })
            );
    }
}
