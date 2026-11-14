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
package de.dataport.suchdienst.suchen.service;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.TopHitsAggregate;
import co.elastic.clients.json.JsonData;
import com.google.common.reflect.TypeToken;
import de.dataport.suchdienst.elasticsearch.api.ElasticInternalReadConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.suchen.model.out.statistic.FederalStateStatistic;
import de.dataport.suchdienst.suchen.model.out.statistic.FederalStateStatisticDetail;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StatisticService {

    private final ElasticInternalReadConnector readConnector;

    public StatisticService(ElasticInternalReadConnector readConnector) {
        this.readConnector = readConnector;
    }

    public Set<FederalStateStatistic> getStatistics() {
        Set<FederalStateStatistic> statistics = new LinkedHashSet<>();

        Map<String, Aggregate> aggregationsLB = readConnector.getIndexAggregation(
            IndexNames.LEISTUNGSBESCHREIBUNG
        );

        Map<String, Aggregate> aggregationsOe = readConnector.getIndexAggregation(
            IndexNames.ORGANISATIONSEINHEITEN
        );
        Map<String, Aggregate> aggregationsOd = readConnector.getIndexAggregation(
            IndexNames.ONLINEDIENSTE
        );
        Map<String, Aggregate> aggregationsSp = readConnector.getIndexAggregation(
            IndexNames.SPEZIALISIERUNGEN
        );

        add(statistics, IndexNames.LEISTUNGSBESCHREIBUNG, aggregationsLB);
        add(statistics, IndexNames.ORGANISATIONSEINHEITEN, aggregationsOe);
        add(statistics, IndexNames.ONLINEDIENSTE, aggregationsOd);
        add(statistics, IndexNames.SPEZIALISIERUNGEN, aggregationsSp);

        return statistics
            .stream()
            .sorted(Comparator.comparing(FederalStateStatistic::getArs))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void add(
        Set<FederalStateStatistic> statistics,
        IndexNames indexName,
        Map<String, Aggregate> aggs
    ) {
        try {
            if (!aggs.isEmpty()) {
                Aggregate terms = aggs.get("group_count");
                List<StringTermsBucket> buckets = terms.sterms().buckets().array();
                for (StringTermsBucket bucket : buckets) {
                    String key = bucket.key().stringValue();
                    long count = bucket.docCount();

                    Map<String, Aggregate> innerAggs = bucket.aggregations();
                    TopHitsAggregate topHits = innerAggs.get("top_hits").topHits();
                    JsonData json = topHits.hits().hits().getFirst().fields().get("lastUpdate");
                    TypeToken<List<Instant>> type = new TypeToken<>() {};
                    List<Instant> o = json.to(type.getType());
                    Instant lastUpdate = o.getFirst();

                    FederalStateStatistic federalStateStatistic = statistics
                        .stream()
                        .filter(f -> f.getArs().equals(key))
                        .findFirst()
                        .orElseGet(() -> new FederalStateStatistic(key));

                    FederalStateStatisticDetail detail = new FederalStateStatisticDetail(
                        count,
                        lastUpdate
                    );

                    switch (indexName) {
                        case LEISTUNGSBESCHREIBUNG -> federalStateStatistic.setLbs(detail);
                        case ORGANISATIONSEINHEITEN -> federalStateStatistic.setOes(detail);
                        case ONLINEDIENSTE -> federalStateStatistic.setOds(detail);
                        case SPEZIALISIERUNGEN -> federalStateStatistic.setSps(detail);
                        default -> {
                            // Andere Geschäftsobjekte werden nicht gezählt!
                        }
                    }

                    statistics.add(federalStateStatistic);
                }
            }
        } catch (Exception e) {
            // PV-8946 catch-log-skip
            log.warn("Bei Erstellen der Statistik hat ein Bucket unerwartete Daten enthalten", e);
        }
    }
}
