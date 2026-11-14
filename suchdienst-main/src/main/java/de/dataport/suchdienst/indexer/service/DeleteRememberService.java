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
package de.dataport.suchdienst.indexer.service;

import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.elasticsearch.api.ElasticBulkWriteConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticWriteConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.indexer.model.DeletedObject;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service, der sich Löschungen im Suchdienst merkt, die gemerkten Löschungen in Elasticsearch
 * persistiert und später auch wieder löschen kann.
 *
 * @author StubbeJ
 */
@Slf4j
@Service
public class DeleteRememberService {

    /**
     * Schützt den Schreib-Zugriff auf die DeletedObjects
     */
    private final Lock lock = new ReentrantLock();

    private final Set<DeletedObject> deletedObjects = new HashSet<>();
    private final ElasticBulkWriteConnector elasticBulkWriteConnector;
    private final ElasticWriteConnector elasticWriteConnector;
    private final SuchdienstProperties properties;

    public DeleteRememberService(
        ElasticBulkWriteConnector elasticBulkWriteConnector,
        ElasticWriteConnector elasticWriteConnector,
        SuchdienstProperties properties
    ) {
        this.elasticBulkWriteConnector = elasticBulkWriteConnector;
        this.elasticWriteConnector = elasticWriteConnector;
        this.properties = properties;
    }

    public void persistDeletedObjects() {
        this.lock.lock();
        try {
            elasticBulkWriteConnector.bulkIndexThrowIfError(
                deletedObjects,
                IndexNames.DELETED_OBJECTS
            );
            deletedObjects.clear();
        } finally {
            this.lock.unlock();
        }
    }

    public void addDeletedObjects(
        Collection<String> ids,
        IndexNames type,
        Instant requestAcceptedAt
    ) {
        this.lock.lock();
        try {
            ids.forEach(id -> deletedObjects.add(new DeletedObject(id, requestAcceptedAt, type)));
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Diese Aktion wird im Hintergrund vom Scheduler ausgeführt und hat darum ein lokales Logging von Exceptions.
     * Der Suchdienst soll trotz Fehlern hier weiterlaufen.
     */
    public void removeExpiredDeletes() {
        log.info(
            "Entferne Elemente, die älter als {} Tage sind, aus dem Index mit gemerkten Löschungen.",
            properties.getImporter().getMaxQueueStorageRetention().toDays()
        );

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deleteAllBefore = now.minus(
            properties.getImporter().getMaxQueueStorageRetention()
        );

        try {
            elasticWriteConnector.removeDeletedObjectList(deleteAllBefore);
        } catch (Exception e) {
            log.warn("Fehler beim Entfernen der gemerkten Löschungen", e);
        }
    }
}
