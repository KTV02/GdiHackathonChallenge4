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
package de.dataport.suchdienst.importer.service;

import co.elastic.clients.elasticsearch._types.WriteResponseBase;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.InnerHitsResult;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticWriteConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.elasticsearch.api.exception.ElasticSearchException;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequest;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequestStatus;
import de.dataport.suchdienst.importer.model.warteschlange.QueueableImportRequest;
import de.dataport.suchdienst.util.ResultPage;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ElasticQueuePersistenceService implements QueuePersistence {

    private final ElasticReadConnector readConnector;
    private final ElasticWriteConnector elasticWriteConnector;
    private final SuchdienstProperties properties;

    public ElasticQueuePersistenceService(
        ElasticReadConnector readConnector,
        ElasticWriteConnector elasticWriteConnector,
        SuchdienstProperties properties
    ) {
        this.readConnector = readConnector;
        this.elasticWriteConnector = elasticWriteConnector;
        this.properties = properties;
    }

    @Override
    public Optional<String> write(ImportRequest importRequest) {
        if (importRequest == null) {
            return Optional.empty();
        }

        log.debug("Schreibe in Elastic Import-Queue [{}]", importRequest.getMetaData().id());
        return Optional.ofNullable(
            elasticWriteConnector.index(
                importRequest,
                IndexNames.IMPORTQUEUE,
                importRequest.getMetaData().id()
            )
        ).map(WriteResponseBase::id);
    }

    @Override
    public Optional<ImportRequest> read(String id) {
        if (id == null) {
            return Optional.empty();
        }

        log.debug("Lese aus Elastic Import-Queue [{}]", id);
        try {
            return readConnector.getById(IndexNames.IMPORTQUEUE, id, ImportRequest.class);
        } catch (ElasticSearchException ex) {
            // PV-8946 catch-log-skip
            log.warn("ImportRequest '{}' konnte nicht gelesen werden", id, ex);
            return Optional.empty();
        }
    }

    @Override
    public List<QueueableImportRequest> readAllPending() {
        // Um zu verhindern, dass wenige Redaktionssysteme mit großem Backlog alle anderen dauerhaft verdrängen,
        // wird die Bucket-Size dynamisch an die Anzahl tatsächlich vorhandener SchemeAgencyIds angepasst:
        // Bei wenigen Systemen werden mehr, bei vielen Systemen weniger Einträge pro System geladen - die Gesamtmenge (queueGlobalMaxDocs) bleibt dabei konstant.
        // Innerhalb eines Redaktionssystems bleiben die Einträge stabil aufsteigend nach requestAcceptedAt sortiert (siehe ElasticReadConnector#getImportQueueCollapsingSchemeAgencyId);
        // die Reihenfolge der Systeme selbst richtet sich nach deren jeweils ältestem offenen Eintrag und variiert daher zwischen Aufrufen.
        int maxBuckets = Math.max(1, properties.getImporter().getQueueMaxBuckets());
        long distinctSchemeAgencyIdCount = readConnector.countDistinctSchemeAgencyIdsInQueue();
        int noOfBuckets = (int) Math.clamp(distinctSchemeAgencyIdCount, 1L, maxBuckets);
        int bucketSize = Math.max(
            1,
            properties.getImporter().getQueueGlobalMaxDocs() / noOfBuckets
        );

        var response = readConnector.getImportQueueCollapsingSchemeAgencyId(
            noOfBuckets,
            bucketSize
        );

        Stream<QueueableImportRequest> rqStream = response
            .hits()
            .hits()
            .stream()
            .map(Hit::innerHits)
            .map(ih -> ih.get("most_recent"))
            .filter(Objects::nonNull)
            .map(InnerHitsResult::hits)
            .map(HitsMetadata::hits)
            .flatMap(Collection::stream)
            .map(Hit::source)
            .filter(Objects::nonNull)
            .map(jsonData -> jsonData.to(QueueableImportRequest.class));

        return rqStream.toList();
    }

    @Override
    public ResultPage<QueueableImportRequest> readAll(
        int page,
        int pageSize,
        String id,
        String messageId,
        String transactionId,
        LocalDateTime acceptedAfter,
        LocalDateTime acceptedBefore,
        Collection<ImportRequestStatus> statuses
    ) {
        try {
            SearchResponse<QueueableImportRequest> response = readConnector.getImportQueue(
                page,
                pageSize,
                id,
                messageId,
                transactionId,
                acceptedAfter,
                acceptedBefore,
                statuses
            );

            List<QueueableImportRequest> plainList = response
                .hits()
                .hits()
                .stream()
                .map(Hit::source)
                .toList();
            return ResultPage.from(plainList, response.hits().total().value(), page, pageSize);
        } catch (Exception exception) {
            // PV-8946 catch-rethrow
            var msg = String.format(
                "Fehler beim Lesen der Import-Queue, id='%s', messageId='%s', transactionId='%s'",
                id,
                messageId,
                transactionId
            );
            throw new ElasticSearchException(ErrorCodeEnum.IMPORT_QUEUE, msg, exception);
        }
    }

    @Override
    public Optional<Boolean> exists(String id) {
        try {
            return Optional.of(readConnector.exists(IndexNames.IMPORTQUEUE, id));
        } catch (ElasticSearchException _) {
            return Optional.empty();
        }
    }

    @Override
    public boolean remove(String id) {
        try {
            log.info("Entferne aus Elastic Import-Queue [{}]", id);
            elasticWriteConnector.deleteFromIndex(IndexNames.IMPORTQUEUE, id);
            return true;
        } catch (ElasticSearchException exception) {
            //PV-8946 catch-log-return-error
            log.error("Fehler beim Löschen aus der Elastic Import-Queue nach Import", exception);
        }

        return false;
    }

    /**
     * Diese Aktion wird regelmäßig im Hintergrund ausgeführt, daher eigenes Exception-Logging.
     */
    @Override
    public void invalidate() {
        log.info(
            "Invalidiere Elemente der persistierten Queue, die älter als {} Tage sind",
            properties.getImporter().getMaxQueueStorageRetention().toDays()
        );

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deleteAllBefore = now.minus(
            properties.getImporter().getMaxQueueStorageRetention()
        );

        try {
            elasticWriteConnector.invalidateImportRequests(deleteAllBefore);
        } catch (Exception e) {
            log.warn("Fehler beim Invalidieren der persistierten Queue", e);
        }
    }

    @Override
    public boolean changeStatus(String id, ImportRequestStatus status, boolean updateProcessedAt) {
        log.debug("Markiere ImportRequest {} als {}", id, status);
        Optional<UpdateResponse<ImportRequest>> updateResponse =
            elasticWriteConnector.updateImportRequestStatus(id, status, updateProcessedAt);

        return updateResponse.isPresent();
    }
}
