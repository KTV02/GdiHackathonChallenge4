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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.elasticsearch.api.model.ElasticBulkError;
import de.dataport.suchdienst.elasticsearch.api.model.ElasticContextOperation;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Prüft den selbst implementierten Bulk-Retry separat vom Framework-Retry.
 * Entscheidend ist hier die Trennung zwischen Kommunikationsfehlern, die erneut versucht werden,
 * und fachlichen Bulk-Fehlern, die sofort gesammelt zurückgegeben werden.
 */
@SuppressWarnings("java:S119") // Generic Namen mit nur einem Buchstaben machen den Code schlechter lesbar
class ElasticBulkWriteConnectorRetryTest {

    @Test
    void retriesCommunicationErrorsUntilSuccess() {
        TestElasticBulkWriteConnector connector = createConnector(2);
        connector.queueListener(
            listenerWithCommunicationError(List.of("id-1", "id-2"), new IOException("io-1"))
        );
        connector.queueListener(new ElasticBulkListener<>());

        List<ElasticBulkError> errors = connector.bulkDelete(
            Set.of("id-1", "id-2"),
            IndexNames.ONLINEDIENSTE
        );

        assertThat(errors, is(empty()));
        assertThat(connector.executionAttempts(), is(2));
        assertThat(
            connector.contextsPerAttempt(),
            contains(List.of("id-1", "id-2"), List.of("id-1", "id-2"))
        );
    }

    @Test
    void doesNotRetryBulkItemErrors() {
        TestElasticBulkWriteConnector connector = createConnector(2);
        connector.queueListener(listenerWithBulkItemError(List.of("id-1"), "item error"));

        List<ElasticBulkError> errors = connector.bulkDelete(
            Set.of("id-1"),
            IndexNames.ONLINEDIENSTE
        );

        assertThat(connector.executionAttempts(), is(1));
        assertThat(errors, hasSize(1));
        assertThat(
            errors.getFirst().errorText(),
            startsWith("Konnte Dokument nicht Indexieren 'id-1'")
        );
        assertThat(errors.getFirst().oFailure(), is(Optional.empty()));
    }

    @Test
    void translatesCommunicationErrorsAfterRetryExhaustion() {
        TestElasticBulkWriteConnector connector = createConnector(2);
        connector.queueListener(
            listenerWithCommunicationError(List.of("id-1"), new IOException("io-1"))
        );
        connector.queueListener(
            listenerWithCommunicationError(List.of("id-1"), new IOException("io-2"))
        );
        connector.queueListener(
            listenerWithCommunicationError(List.of("id-1"), new IOException("io-3"))
        );

        List<ElasticBulkError> errors = connector.bulkDelete(
            Set.of("id-1"),
            IndexNames.ONLINEDIENSTE
        );

        assertThat(connector.executionAttempts(), is(3));
        assertThat(errors, hasSize(1));
        assertThat(errors.getFirst().errorText(), startsWith("BulkRequest '1' Fehlgeschlagen"));
        assertThat(errors.getFirst().oFailure().isPresent(), is(true));
        assertThat(errors.getFirst().oFailure().orElseThrow(), instanceOf(IOException.class));
    }

    private static TestElasticBulkWriteConnector createConnector(int maxRetries) {
        IndexConfiguration indexConfiguration = mock(IndexConfiguration.class);
        when(indexConfiguration.getCurrentIndexName(any(IndexNames.class))).thenReturn(
            "test-index"
        );

        SuchdienstProperties properties = new SuchdienstProperties();
        properties.getElastic().getRetry().setMaxRetries(maxRetries);
        properties.getElastic().getRetry().setInitialInterval(Duration.ofMillis(1L));
        properties.getElastic().getRetry().setMaxInterval(Duration.ofMillis(2L));
        properties.getElastic().getRetry().setMultiplier(2d);

        return new TestElasticBulkWriteConnector(indexConfiguration, properties);
    }

    private static ElasticBulkListener<String> listenerWithCommunicationError(
        List<String> contexts,
        Throwable failure
    ) {
        ElasticBulkListener<String> listener = new ElasticBulkListener<>();
        listener.afterBulk(1L, bulkRequest(contexts), contexts, failure);
        return listener;
    }

    private static ElasticBulkListener<String> listenerWithBulkItemError(
        List<String> contexts,
        String reason
    ) {
        ElasticBulkListener<String> listener = new ElasticBulkListener<>();
        BulkResponse response = BulkResponse.of(b ->
            b
                .errors(true)
                .took(1L)
                .items(
                    contexts
                        .stream()
                        .map(context ->
                            BulkResponseItem.of(item ->
                                item
                                    .operationType(
                                        co.elastic.clients.elasticsearch.core.bulk.OperationType.Index
                                    )
                                    .index("test-index")
                                    .id(context)
                                    .status(500)
                                    .error(error -> error.type("test_error").reason(reason))
                            )
                        )
                        .toList()
                )
        );
        listener.afterBulk(1L, bulkRequest(contexts), contexts, response);
        return listener;
    }

    private static BulkRequest bulkRequest(List<String> contexts) {
        BulkRequest.Builder builder = new BulkRequest.Builder();
        for (String context : contexts) {
            builder.operations(op -> op.delete(del -> del.index("test-index").id(context)));
        }
        return builder.build();
    }

    private static final class TestElasticBulkWriteConnector extends ElasticBulkWriteConnector {

        private final Queue<ElasticBulkListener<String>> listeners = new ArrayDeque<>();
        private final List<List<String>> contextsPerAttempt = new ArrayList<>();

        private TestElasticBulkWriteConnector(
            IndexConfiguration indexConfiguration,
            SuchdienstProperties properties
        ) {
            super(
                mock(ElasticsearchClient.class),
                mock(JsonMapper.class),
                indexConfiguration,
                mock(IndexRefreshMgr.class),
                properties
            );
        }

        void queueListener(ElasticBulkListener<String> listener) {
            listeners.add(listener);
        }

        int executionAttempts() {
            return contextsPerAttempt.size();
        }

        List<List<String>> contextsPerAttempt() {
            return contextsPerAttempt;
        }

        @Override
        public ExponentialRandomBackOff buildExponentialRandomBackOff() {
            return new ExponentialRandomBackOff(0L, 0L, 1d) {
                @Override
                public void sleep() {
                    // no-op for deterministic tests
                }
            };
        }

        @Override
        <Context> ElasticBulkListener<Context> executeBulk(
            List<ElasticContextOperation<Context>> bulk,
            String index
        ) {
            // Die Sortierung hält die Assertions stabil, weil Set-basierte BulkDelete-Aufrufe keine
            // fachlich garantierte Reihenfolge der IDs haben.
            contextsPerAttempt.add(
                bulk
                    .stream()
                    .map(operation -> String.valueOf(operation.context()))
                    .sorted()
                    .toList()
            );
            @SuppressWarnings("unchecked")
            ElasticBulkListener<Context> listener =
                (ElasticBulkListener<Context>) listeners.remove();
            return listener;
        }
    }
}
