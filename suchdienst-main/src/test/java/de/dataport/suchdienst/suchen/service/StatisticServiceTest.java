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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.TopHitsAggregate;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import de.dataport.suchdienst.elasticsearch.api.ElasticInternalReadConnector;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Service-Test für StatisticService.
 * Testet die Business-Logik der Elasticsearch-Aggregations-Verarbeitung.
 */
@ExtendWith(MockitoExtension.class)
class StatisticServiceTest {

    private final Jackson3JsonpMapper jsonpMapper = new Jackson3JsonpMapper();

    @Mock
    private ElasticInternalReadConnector elasticReadConnector;

    @InjectMocks
    private StatisticService statisticService;

    @Test
    void shouldProcessElasticsearchAggregationsCorrectly() {
        // Arrange - Komplexe Elasticsearch Mock-Strukturen
        // Verwende den konfigurierten JacksonJsonpMapper (siehe Klassenebene)

        // Bucket 1 für ARS "05"
        Hit<JsonData> hit1 = Hit.of(h ->
            h
                .id("id1")
                .index("idx")
                .fields(
                    "lastUpdate",
                    JsonData.of(List.of("2020-10-21T14:36:05.120357Z"), jsonpMapper)
                )
        );
        HitsMetadata<JsonData> metadata1 = HitsMetadata.of(hm -> hm.hits(List.of(hit1)));

        TopHitsAggregate topHits1 = mock(TopHitsAggregate.class);
        when(topHits1.hits()).thenReturn(metadata1);

        StringTermsBucket bucket1 = mock(StringTermsBucket.class);
        when(bucket1.docCount()).thenReturn(5L);
        when(bucket1.key()).thenReturn(FieldValue.of("05"));
        when(bucket1.aggregations()).thenReturn(
            Map.of("top_hits", Aggregate.of(a -> a.topHits(topHits1)))
        );

        // Bucket 2 für ARS "04"
        Hit<JsonData> hit2 = Hit.of(h ->
            h
                .id("id2")
                .index("idx")
                .fields(
                    "lastUpdate",
                    JsonData.of(List.of("2020-10-21T14:36:05.120357Z"), jsonpMapper)
                )
        );
        HitsMetadata<JsonData> metadata2 = HitsMetadata.of(hm -> hm.hits(List.of(hit2)));

        TopHitsAggregate topHits2 = mock(TopHitsAggregate.class);
        when(topHits2.hits()).thenReturn(metadata2);

        StringTermsBucket bucket2 = mock(StringTermsBucket.class);
        when(bucket2.docCount()).thenReturn(4L);
        when(bucket2.key()).thenReturn(FieldValue.of("04"));
        when(bucket2.aggregations()).thenReturn(
            Map.of("top_hits", Aggregate.of(a -> a.topHits(topHits2)))
        );

        // Aggregation
        Aggregate aggregate = mock(Aggregate.class);
        when(aggregate.sterms()).thenReturn(
            StringTermsAggregate.of(v -> v.buckets(b -> b.array(List.of(bucket1, bucket2))))
        );

        Map<String, Aggregate> aggregations = Map.of("group_count", aggregate);

        // Mock ElasticReadConnector für alle Index-Typen
        when(elasticReadConnector.getIndexAggregation(any())).thenReturn(aggregations);

        // Act
        var result = statisticService.getStatistics();

        // Assert
        assertThat(result).isNotNull().hasSize(2);

        // Konvertiere zu Liste für einfacheren Zugriff (sortiert nach ARS)
        var statsList = result.stream().toList();

        // Prüfe State 04
        assertThat(statsList.get(0).getArs()).isEqualTo("04");
        assertThat(statsList.get(0).getLbs().getCount()).isEqualTo(4L);
        assertThat(statsList.get(0).getLbs().getLastUpdate()).isNotNull();
        assertThat(statsList.get(0).getSps().getCount()).isEqualTo(4L);
        assertThat(statsList.get(0).getOes().getCount()).isEqualTo(4L);
        assertThat(statsList.get(0).getOds().getCount()).isEqualTo(4L);

        // Prüfe State 05
        assertThat(statsList.get(1).getArs()).isEqualTo("05");
        assertThat(statsList.get(1).getLbs().getCount()).isEqualTo(5L);
        assertThat(statsList.get(1).getLbs().getLastUpdate()).isNotNull();
        assertThat(statsList.get(1).getSps().getCount()).isEqualTo(5L);
        assertThat(statsList.get(1).getOes().getCount()).isEqualTo(5L);
        assertThat(statsList.get(1).getOds().getCount()).isEqualTo(5L);
    }

    @Test
    void shouldReturnEmptySetWhenNoAggregations() {
        // Arrange
        when(elasticReadConnector.getIndexAggregation(any())).thenReturn(Map.of());

        // Act
        var result = statisticService.getStatistics();

        // Assert
        assertThat(result).isNotNull().isEmpty();
    }
}
