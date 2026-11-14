/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.importer.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticWriteConnector;
import de.dataport.suchdienst.importer.model.warteschlange.QueueableImportRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Testet die dynamische Ermittlung von noOfBuckets/bucketSize in {@link ElasticQueuePersistenceService#readAllPending()},
 * die dafür sorgt, dass Redaktionssysteme mit wenigen offenen Einträgen nicht dauerhaft von Systemen mit großem
 * Backlog verdrängt werden (siehe queueGlobalMaxDocs/queueMaxBuckets in {@link SuchdienstProperties.Importer}).
 */
class ElasticQueuePersistenceServiceTest {

    private ElasticReadConnector readConnector;
    private SuchdienstProperties properties;
    private ElasticQueuePersistenceService service;

    @BeforeEach
    void setUp() {
        readConnector = mock(ElasticReadConnector.class);
        ElasticWriteConnector writeConnector = mock(ElasticWriteConnector.class);
        properties = new SuchdienstProperties();
        // Defaults: queueGlobalMaxDocs=3000, queueMaxBuckets=20

        service = new ElasticQueuePersistenceService(readConnector, writeConnector, properties);

        @SuppressWarnings("unchecked")
        SearchResponse<QueueableImportRequest> emptyResponse = mock(SearchResponse.class);
        @SuppressWarnings("unchecked")
        HitsMetadata<QueueableImportRequest> emptyHits = mock(HitsMetadata.class);
        when(emptyHits.hits()).thenReturn(List.of());
        when(emptyResponse.hits()).thenReturn(emptyHits);
        when(readConnector.getImportQueueCollapsingSchemeAgencyId(anyInt(), anyInt())).thenReturn(
            emptyResponse
        );
    }

    @Test
    void usesGlobalMaxAsBucketSizeWhenOnlyOneSchemeAgencyIdIsPending() {
        when(readConnector.countDistinctSchemeAgencyIdsInQueue()).thenReturn(1L);

        service.readAllPending();

        verify(readConnector).getImportQueueCollapsingSchemeAgencyId(1, 3000);
    }

    @Test
    void distributesGlobalMaxOverActualNumberOfSchemeAgencyIds() {
        when(readConnector.countDistinctSchemeAgencyIdsInQueue()).thenReturn(10L);

        service.readAllPending();

        verify(readConnector).getImportQueueCollapsingSchemeAgencyId(10, 300);
    }

    @Test
    void capsNoOfBucketsAtConfiguredMaximum() {
        when(readConnector.countDistinctSchemeAgencyIdsInQueue()).thenReturn(50L);

        service.readAllPending();

        // 50 > queueMaxBuckets (20) -> gedeckelt auf 20 Buckets, bucketSize = 3000 / 20
        verify(readConnector).getImportQueueCollapsingSchemeAgencyId(20, 150);
    }

    @Test
    void treatsEmptyQueueAsSingleBucketToAvoidDivisionByZero() {
        when(readConnector.countDistinctSchemeAgencyIdsInQueue()).thenReturn(0L);

        service.readAllPending();

        verify(readConnector).getImportQueueCollapsingSchemeAgencyId(1, 3000);
    }

    @Test
    void returnsEmptyListWhenNoPendingEntriesExist() {
        when(readConnector.countDistinctSchemeAgencyIdsInQueue()).thenReturn(0L);

        List<QueueableImportRequest> result = service.readAllPending();

        assertThat(result, is(List.of()));
    }

    @Test
    void fallsBackToSingleBucketWhenConfiguredMaximumIsInvalid() {
        properties.getImporter().setQueueMaxBuckets(0);
        when(readConnector.countDistinctSchemeAgencyIdsInQueue()).thenReturn(10L);

        service.readAllPending();

        verify(readConnector).getImportQueueCollapsingSchemeAgencyId(1, 3000);
    }

    @Test
    void usesAtLeastOneDocumentPerBucketWhenGlobalMaximumIsSmallerThanBucketCount() {
        properties.getImporter().setQueueGlobalMaxDocs(10);
        when(readConnector.countDistinctSchemeAgencyIdsInQueue()).thenReturn(20L);

        service.readAllPending();

        verify(readConnector).getImportQueueCollapsingSchemeAgencyId(20, 1);
    }
}
