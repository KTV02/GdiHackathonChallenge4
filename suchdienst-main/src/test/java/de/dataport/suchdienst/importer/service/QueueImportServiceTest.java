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

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequestStatus;
import de.dataport.suchdienst.importer.model.warteschlange.QueueableImportRequest;
import de.dataport.suchdienst.importer.model.warteschlange.TransactionMetadata;
import de.dataport.suchdienst.indexer.service.DeleteRememberService;
import de.dataport.suchdienst.indexer.service.XZufiImportService;
import de.dataport.suchdienst.taskmgr.service.SudTaskMgr;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.TaskScheduler;

class QueueImportServiceTest {

    /**
     * Testet, ob ein Eintrag in der ImportQueue verarbeitet wird, wenn die SchemeAgencyId darin nicht
     * bekannt ist.
     */
    @Test
    void testImportQueueWhenUnknownSchemeAgencyId() {
        var mockSudProperties = Mockito.mock(SuchdienstProperties.class);
        var mockQueuePersistence = Mockito.mock(QueuePersistence.class);
        var mockXzufiImportService = Mockito.mock(XZufiImportService.class);
        var mockTaskScheduler = Mockito.mock(TaskScheduler.class);
        var mockMessageSource = Mockito.mock(MessageSource.class);
        var mockReadConnector = Mockito.mock(ElasticReadConnector.class);
        var mockDeleteRememberService = Mockito.mock(DeleteRememberService.class);
        var mockSudTaskMgr = Mockito.mock(SudTaskMgr.class);

        QueueImportService queueImportService = new QueueImportService(
            mockSudProperties,
            mockQueuePersistence,
            mockXzufiImportService,
            mockTaskScheduler,
            mockMessageSource,
            mockReadConnector,
            mockDeleteRememberService,
            mockSudTaskMgr
        );

        // Die ImportQueue so mocken, dass ein Eintrag mit einer ungültigen SchemeAgencyId geladen wird:
        var txnMetaDataWithInvalidSchemeAgencyId = new TransactionMetadata(
            "nachrichtuuid",
            "txnId",
            "1",
            "invalidSchemeAgencyId",
            "Sammlerdienst"
        );
        when(mockQueuePersistence.readAllPending()).thenReturn(
            List.of(
                new QueueableImportRequest(
                    Instant.now(),
                    null,
                    ImportRequestStatus.OPEN,
                    txnMetaDataWithInvalidSchemeAgencyId
                )
            )
        );

        queueImportService.processQueue();

        // Erwartet ist, dass der Status der Nachricht über die QueuePersistence nicht geändert wird:
        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ImportRequestStatus> statusCaptor = ArgumentCaptor.forClass(
            ImportRequestStatus.class
        );
        verify(mockQueuePersistence, times(0)).changeStatus(
            idCaptor.capture(),
            statusCaptor.capture(),
            anyBoolean()
        );
    }
}
