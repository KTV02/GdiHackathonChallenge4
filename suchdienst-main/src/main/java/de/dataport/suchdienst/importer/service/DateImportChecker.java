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

import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.importer.model.jzufi.UpdatableEntity;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequest;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequestStatus;
import de.dataport.suchdienst.indexer.model.DeletedObject;
import de.dataport.suchdienst.indexer.model.Identifiable;
import java.time.Instant;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DateImportChecker implements ImportChecker {

    private final ImportRequest request;
    private final ElasticReadConnector readConnector;

    public DateImportChecker(ImportRequest request, ElasticReadConnector readConnector) {
        this.readConnector = readConnector;
        this.request = request;
    }

    @Override
    public <T extends Identifiable> boolean importAllowed(
        String id,
        IndexNames indexname,
        Class<T> clazz
    ) {
        if (request.getStatus() == ImportRequestStatus.REOPENED) {
            Instant requestTimestamp = request.getRequestAcceptedAt();
            Optional<DeletedObject> objectDeleted = readConnector.getById(
                IndexNames.DELETED_OBJECTS,
                id,
                DeletedObject.class
            );
            // Wenn requestAcceptedAt aus der Warteschlange vor dem Löschen war dann nicht updaten. Es könnte
            // passieren, dass das Löschen fehlgeschlagen ist und die IDs trotzdem im Index stehen, dann ist der
            // Zeitstempel aber gleich und das Löschen kann erneut durchgeführt werden
            if (
                objectDeleted.isPresent() &&
                requestTimestamp.isBefore(objectDeleted.get().getDeletedAt())
            ) {
                log.info("Import von wieder eröffnetem Element mit der ID {} nicht erlaubt.", id);
                return false;
            }

            Optional<T> objectFromElastic = readConnector.getById(indexname, id, clazz);
            if (objectFromElastic.isEmpty()) {
                // Objekt ist noch nicht vorhanden, der Import kann durchgeführt werden
                return true;
            }

            if (objectFromElastic.get() instanceof UpdatableEntity updatableEntity) {
                var lastUpdate = updatableEntity.getLastUpdate();
                if (lastUpdate == null || lastUpdate.isBefore(requestTimestamp)) {
                    // Objekt ist vorhanden, der Zeitstempel ist aber vor dem Zeitstempel der Warteschlangennachricht
                    return true;
                }
            }
        } else {
            return true;
        }
        log.info("Import von wieder eröffnetem Element mit der ID {} nicht erlaubt.", id);
        return false;
    }
}
