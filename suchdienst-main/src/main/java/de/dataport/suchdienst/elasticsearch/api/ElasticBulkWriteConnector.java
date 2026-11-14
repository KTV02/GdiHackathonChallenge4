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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import de.dataport.suchdienst.aop.MasterMethod;
import de.dataport.suchdienst.aop.RetryableWhenBackgroundJob;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.elasticsearch.api.model.ElasticBulkCommunicationError;
import de.dataport.suchdienst.elasticsearch.api.model.ElasticBulkError;
import de.dataport.suchdienst.elasticsearch.api.model.ElasticContextOperation;
import de.dataport.suchdienst.indexer.exception.ImportException;
import de.dataport.suchdienst.indexer.model.Identifiable;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.Verwaltungsleistung;
import de.dataport.suchdienst.indexer.service.IndexerUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Primary
@Service
@RetryableWhenBackgroundJob
@SuppressWarnings("java:S119") // 'Context' zur besseren Verständlichkeit
public class ElasticBulkWriteConnector extends Elastic {

    private final DataSize maxBulkRequestSize;
    private final int maxBulkRequestActions;
    private final int maxConcurrentRequests;

    public ElasticBulkWriteConnector(
        ElasticsearchClient client,
        JsonMapper mapper,
        IndexConfiguration indexConfiguration,
        IndexRefreshMgr indexRefreshMgr,
        SuchdienstProperties properties
    ) {
        super(client, mapper, indexConfiguration, indexRefreshMgr, properties);
        this.maxBulkRequestSize = properties.getElastic().getMaxBulkRequestSize();
        this.maxBulkRequestActions = properties.getElastic().getMaxBulkRequestActions();
        this.maxConcurrentRequests = properties.getElastic().getMaxConcurrentRequests();
    }

    @MasterMethod
    public List<ElasticBulkError> bulkDelete(Set<String> ids, IndexNames indexName) {
        String index = indexConfiguration.getCurrentIndexName(indexName);
        List<ElasticContextOperation<String>> contextOperations = ids
            .stream()
            .map(id ->
                new ElasticContextOperation<>(
                    BulkOperation.of(b -> b.delete(del -> del.index(index).id(id))),
                    id
                )
            )
            .toList();
        return contextOperations.isEmpty() ? List.of() : bulkWithRetry(contextOperations, index);
    }

    @MasterMethod
    public <T> List<ElasticBulkError> bulkIndex(Collection<T> documents, AliasNames aliasName) {
        final String index = indexConfiguration.getCurrentWriteAliasName(aliasName);
        return bulkIndex(documents, index);
    }

    @MasterMethod
    public <T> List<ElasticBulkError> bulkIndex(Collection<T> documents, IndexNames indexName) {
        final String index = indexConfiguration.getCurrentIndexName(indexName);
        return bulkIndex(documents, index);
    }

    @MasterMethod
    public <T> void bulkIndexThrowIfError(Collection<T> documents, AliasNames aliasName)
        throws ImportException {
        final String index = indexConfiguration.getCurrentWriteAliasName(aliasName);
        List<ElasticBulkError> elasticBulkErrors = bulkIndex(documents, index);
        IndexerUtil.throwIfError(log, elasticBulkErrors);
    }

    @MasterMethod
    public <T> void bulkIndexThrowIfError(Collection<T> documents, IndexNames indexName)
        throws ImportException {
        List<ElasticBulkError> elasticBulkErrors = bulkIndex(documents, indexName);
        IndexerUtil.throwIfError(log, elasticBulkErrors);
    }

    @MasterMethod
    private <T> List<ElasticBulkError> bulkIndex(Collection<T> documents, String index) {
        if (documents.isEmpty()) {
            return List.of();
        }

        List<ElasticContextOperation<String>> contextOperations = documents
            .stream()
            .map(this::createElasticContextOperation)
            .toList();

        return bulkWithRetry(contextOperations, index);
    }

    private <T> ElasticContextOperation<String> createElasticContextOperation(T document) {
        // 1. Index-Operation erstellen mit Dokument
        IndexOperation.Builder<T> indexOperationBuilder = new IndexOperation.Builder<T>().document(
            document
        );

        // 2. ID setzen falls verfügbar (verhindert Duplikate)
        String documentId = extractDocumentId(document);
        if (documentId != null) {
            indexOperationBuilder.id(documentId);
        }

        // 3. Kontext für Logging/Debugging bestimmen
        String context = createContextString(document, documentId);

        // 4. Bulk-Operation erstellen
        BulkOperation bulkOperation = BulkOperation.of(bo ->
            bo.index(indexOperationBuilder.build())
        );

        return new ElasticContextOperation<>(bulkOperation, context);
    }

    /**
     * Extrahiert die ID aus einem Dokument falls es das Identifiable Interface implementiert.
     * @return ID des Dokuments oder null falls keine vorhanden
     */
    private <T> String extractDocumentId(T document) {
        return document instanceof Identifiable identifiable ? identifiable.getId() : null;
    }

    /**
     * Erstellt einen Context-String für Logging und Debugging.
     * Priorität: Verwaltungsleistung.contextString() > Dokument-ID > "unknown_context"
     */
    private <T> String createContextString(T document, String documentId) {
        if (document instanceof Verwaltungsleistung verwaltungsleistung) {
            return verwaltungsleistung.contextString();
        }

        if (documentId != null) {
            return documentId;
        }

        return "unknown_context";
    }

    //FIXME Ein vom Framework unterstütztes Retry ist in Arbeit: https://github.com/elastic/elasticsearch-java/issues/478
    // Update: Bulk-Ingester unterstützt jetzt ein Retry für 429 (too many request) fehler (https://github.com/elastic/elasticsearch-java/pull/930)
    // Es ist zu prüfen ob dieser Retry für den Suchdienst ausreicht, damit könnte dann dieser Custom-Code verschwinden.
    private <Context> List<ElasticBulkError> bulkWithRetry(
        List<ElasticContextOperation<Context>> bulk,
        String index
    ) {
        int maxRetry = properties.getElastic().getRetry().getMaxRetries();
        ExponentialRandomBackOff backOff = buildExponentialRandomBackOff();

        List<ElasticBulkError> errors = new ArrayList<>();
        List<ElasticContextOperation<Context>> currentOperations = bulk;
        List<ElasticBulkCommunicationError<Context>> communicationErrors = List.of();

        int counter = 0;
        do {
            if (ObjectUtils.isNotEmpty(communicationErrors)) {
                backOff.sleep();
                log.warn(
                    "Versuch: {} | Retry nach '{}' Verbindungsfehler(n) während BulkRequest",
                    counter,
                    communicationErrors.size()
                );
            }

            ElasticBulkListener<Context> contextElasticBulkListener = executeBulk(
                currentOperations,
                index
            );
            communicationErrors = contextElasticBulkListener.getElasticBulkCommunicationErrors();
            currentOperations = from(communicationErrors);

            errors.addAll(contextElasticBulkListener.getElasticBulkErrors());

            counter++;
        } while (ObjectUtils.isNotEmpty(communicationErrors) && counter <= maxRetry);

        if (ObjectUtils.isNotEmpty(communicationErrors)) {
            log.warn(
                "Retry konnte nicht alle Fehler beheben, Fehler '{}' Versuche '{}'",
                communicationErrors.size(),
                counter
            );
            List<ElasticBulkError> elasticBulkErrors = communicationErrorToBulkError(
                communicationErrors
            );
            errors.addAll(elasticBulkErrors);
        }

        return errors;
    }

    private <Context> List<ElasticBulkError> communicationErrorToBulkError(
        List<ElasticBulkCommunicationError<Context>> unresolvedErrors
    ) {
        return unresolvedErrors
            .stream()
            .map(communicationError -> {
                String errorText = String.format(
                    "BulkRequest '%s' Fehlgeschlagen, Enthaltene Operationen: '%d'",
                    communicationError.executionId(),
                    communicationError.contexts().size()
                );
                return new ElasticBulkError(errorText, Optional.of(communicationError.failure()));
            })
            .toList();
    }

    /**
     * Paketintern, damit Retry-Tests den Rückkanal des BulkIngester gezielt simulieren können,
     * ohne den öffentlichen API-Scope des Connectors zu erweitern.
     */
    <Context> ElasticBulkListener<Context> executeBulk(
        List<ElasticContextOperation<Context>> bulk,
        String index
    ) {
        ElasticBulkListener<Context> listener = new ElasticBulkListener<>();
        BulkRequest.Builder settings = new BulkRequest.Builder().index(index);
        BulkIngester<Context> bulkIngester = BulkIngester.of(bi ->
            bi
                .client(client)
                .maxSize(maxBulkRequestSize.toBytes())
                .maxOperations(maxBulkRequestActions)
                .maxConcurrentRequests(maxConcurrentRequests)
                .listener(listener)
                .globalSettings(settings)
        );
        try (bulkIngester) {
            bulk.forEach(oc -> bulkIngester.add(oc.operation(), oc.context()));
        }
        indexRefreshMgr.markAsDirty(index);
        return listener;
    }

    private <Context> List<ElasticContextOperation<Context>> from(
        List<ElasticBulkCommunicationError<Context>> communicationErrors
    ) {
        List<ElasticContextOperation<Context>> contextOperations = new ArrayList<>();
        for (ElasticBulkCommunicationError<Context> elasticBulkIOError : communicationErrors) {
            List<BulkOperation> operations = elasticBulkIOError.request().operations();
            for (int i = 0; i < operations.size(); i++) {
                BulkOperation bulkOperation = operations.get(i);
                Context context = elasticBulkIOError.contexts().get(i);
                contextOperations.add(new ElasticContextOperation<>(bulkOperation, context));
            }
        }
        return contextOperations;
    }
}
