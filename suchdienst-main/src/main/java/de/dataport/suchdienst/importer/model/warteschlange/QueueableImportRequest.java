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
package de.dataport.suchdienst.importer.model.warteschlange;

import de.dataport.suchdienst.indexer.model.Identifiable;
import java.time.Instant;
import lombok.Data;

/**
 * ImportRequest View ohne Inhaltsdaten. Diese verwalten wir in der In-Memory Queue (ein Cache) am QueueImportService.
 * Dadurch Ersparnis von RAM. Beim Verarbeiten der ImportRequests laden wir die Xzufi-Daten dann wieder aus dem Index.
 */
@Data
public class QueueableImportRequest implements Identifiable, ImportRequestInfo {

    private String id;
    private final Instant requestAcceptedAt;
    private Instant requestProcessedAt;
    private ImportRequestStatus status;
    private final TransactionMetadata metaData;

    public QueueableImportRequest(
        Instant requestAcceptedAt,
        Instant requestProcessedAt,
        ImportRequestStatus status,
        TransactionMetadata metaData
    ) {
        this.id = metaData.id();
        this.requestAcceptedAt = requestAcceptedAt;
        this.requestProcessedAt = requestProcessedAt;
        this.status = status;
        this.metaData = metaData;
    }
}
