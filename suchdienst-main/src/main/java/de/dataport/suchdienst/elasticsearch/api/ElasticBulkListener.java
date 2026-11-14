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
package de.dataport.suchdienst.elasticsearch.api;

import co.elastic.clients.elasticsearch._helpers.bulk.BulkListener;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import de.dataport.suchdienst.elasticsearch.api.model.ElasticBulkCommunicationError;
import de.dataport.suchdienst.elasticsearch.api.model.ElasticBulkError;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("java:S119") // 'Context' zur besseren Verständlichkeit
public class ElasticBulkListener<Context> implements BulkListener<Context> {

    private final Queue<ElasticBulkError> elasticBulkError = new ConcurrentLinkedQueue<>();

    private final Queue<ElasticBulkCommunicationError<Context>> elasticBulkCommunicationErrors =
        new ConcurrentLinkedQueue<>();

    public List<ElasticBulkError> getElasticBulkErrors() {
        return List.copyOf(elasticBulkError);
    }

    public List<ElasticBulkCommunicationError<Context>> getElasticBulkCommunicationErrors() {
        return List.copyOf(elasticBulkCommunicationErrors);
    }

    @Override
    public void beforeBulk(long executionId, BulkRequest request, List<Context> contexts) {
        // unused
    }

    @Override
    public void afterBulk(
        long executionId,
        BulkRequest request,
        List<Context> contexts,
        BulkResponse response
    ) {
        // The request was accepted, but may contain failed items.
        // The "context" list gives the file name for each bulk item.
        log.debug("Bulk request {} completed", executionId);
        for (int i = 0; i < contexts.size(); i++) {
            BulkResponseItem item = response.items().get(i);
            if (item.error() != null) {
                // Inspect the failure cause
                String errorText = String.format(
                    "Konnte Dokument nicht Indexieren '%s' , Grund: '%s'",
                    contexts.get(i),
                    item.error().reason()
                );
                elasticBulkError.add(new ElasticBulkError(errorText, Optional.empty()));
            }
        }
    }

    @Override
    public void afterBulk(
        long executionId,
        BulkRequest request,
        List<Context> contexts,
        Throwable failure
    ) {
        // The request could not be sent
        elasticBulkCommunicationErrors.add(
            new ElasticBulkCommunicationError<>(executionId, request, contexts, failure)
        );
    }
}
