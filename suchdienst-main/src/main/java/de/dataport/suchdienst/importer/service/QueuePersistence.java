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

import de.dataport.suchdienst.importer.model.warteschlange.ImportRequest;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequestStatus;
import de.dataport.suchdienst.importer.model.warteschlange.QueueableImportRequest;
import de.dataport.suchdienst.util.ResultPage;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public interface QueuePersistence {
    Optional<String> write(ImportRequest importRequest);

    Optional<ImportRequest> read(@Nullable String id);

    @SuppressWarnings("java:S107") // Methode ist auch so verständlich und Refactoring wäre hier zu aufwendig
    ResultPage<QueueableImportRequest> readAll(
        int page,
        int pageSize,
        String id,
        String messageId,
        String transactionId,
        LocalDateTime acceptedAfter,
        LocalDateTime acceptedBefore,
        Collection<ImportRequestStatus> statuses
    );

    List<QueueableImportRequest> readAllPending();

    Optional<Boolean> exists(String id);

    boolean remove(@Nullable String id);

    /**
     * Updatet ein Dokument im Index suchdienst_importqueue.
     *
     * @param id                Id des Dokuments
     * @param status            neuer Wert für das Feld 'status'
     * @param updateProcessedAt Fall gesetzt, wird das Feld 'requestProcessedAt' auf den aktuellen Timestamp gesetzt.
     * @return True wenn das update funktioniert hat, sonst false
     */
    boolean changeStatus(
        @Nullable String id,
        ImportRequestStatus status,
        boolean updateProcessedAt
    );

    void invalidate();
}
