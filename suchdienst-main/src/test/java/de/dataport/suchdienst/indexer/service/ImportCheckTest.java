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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.importer.model.XZuFiVersion;
import de.dataport.suchdienst.importer.model.jzufi.LeistungEntity;
import de.dataport.suchdienst.importer.model.jzufi.LeistungEntity22;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequest;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequestStatus;
import de.dataport.suchdienst.importer.model.warteschlange.TransactionMetadata;
import de.dataport.suchdienst.importer.service.DateImportChecker;
import de.dataport.suchdienst.indexer.model.DeletedObject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ImportCheckTest {

    private final ElasticReadConnector readConnector = Mockito.mock(ElasticReadConnector.class);

    @Test
    void testOpen() {
        LeistungEntity lb = new LeistungEntity22(Instant.now());
        when(readConnector.getById(eq(IndexNames.LEISTUNGSBESCHREIBUNG), any(), any())).thenReturn(
            Optional.of(lb)
        );

        ImportRequest request = ImportRequest.createOpen(
            new TransactionMetadata("1", "1", "1", "1", "1"),
            "hallo".getBytes(),
            XZuFiVersion.XZUFI22
        );

        DateImportChecker importChecker = new DateImportChecker(request, readConnector);
        assertTrue(
            importChecker.importAllowed(
                "id",
                IndexNames.LEISTUNGSBESCHREIBUNG,
                LeistungEntity.class
            )
        );
    }

    @Test
    void testReopenProcessNormal() {
        LeistungEntity lb = new LeistungEntity22(Instant.now().minus(1, ChronoUnit.DAYS));
        when(readConnector.getById(eq(IndexNames.LEISTUNGSBESCHREIBUNG), any(), any())).thenReturn(
            Optional.of(lb)
        );

        ImportRequest request = new ImportRequest(
            Instant.now(),
            Instant.now(),
            new TransactionMetadata("1", "1", "1", "1", "1"),
            "hallo".getBytes(),
            XZuFiVersion.XZUFI22,
            ImportRequestStatus.REOPENED
        );
        DateImportChecker importChecker = new DateImportChecker(request, readConnector);
        assertTrue(
            importChecker.importAllowed(
                "id",
                IndexNames.LEISTUNGSBESCHREIBUNG,
                LeistungEntity.class
            )
        );
    }

    @Test
    void testReopenProcessNormalFalse() {
        LeistungEntity lb = new LeistungEntity22(Instant.now().plus(1, ChronoUnit.DAYS));
        when(readConnector.getById(eq(IndexNames.LEISTUNGSBESCHREIBUNG), any(), any())).thenReturn(
            Optional.of(lb)
        );
        ImportRequest request = new ImportRequest(
            Instant.now(),
            Instant.now(),
            new TransactionMetadata("1", "1", "1", "1", "1"),
            "hallo".getBytes(),
            XZuFiVersion.XZUFI22,
            ImportRequestStatus.REOPENED
        );
        DateImportChecker importChecker = new DateImportChecker(request, readConnector);
        assertFalse(
            importChecker.importAllowed(
                "id",
                IndexNames.LEISTUNGSBESCHREIBUNG,
                LeistungEntity.class
            )
        );
    }

    @Test
    void testReopenNoLastUpdate() {
        LeistungEntity lb = new LeistungEntity22(null);
        when(readConnector.getById(eq(IndexNames.LEISTUNGSBESCHREIBUNG), any(), any())).thenReturn(
            Optional.of(lb)
        );
        ImportRequest request = new ImportRequest(
            Instant.now(),
            Instant.now(),
            new TransactionMetadata("1", "1", "1", "1", "1"),
            "hallo".getBytes(),
            XZuFiVersion.XZUFI22,
            ImportRequestStatus.REOPENED
        );
        DateImportChecker importChecker = new DateImportChecker(request, readConnector);
        assertTrue(
            importChecker.importAllowed(
                "id",
                IndexNames.LEISTUNGSBESCHREIBUNG,
                LeistungEntity.class
            )
        );
    }

    // Leistung wurde zwischenzeitlich gelöscht und darf nicht neu importiert werden
    @Test
    void testDeletedBefore() {
        LeistungEntity lb = new LeistungEntity22(Instant.now().minus(1, ChronoUnit.DAYS));
        when(readConnector.getById(eq(IndexNames.LEISTUNGSBESCHREIBUNG), any(), any())).thenReturn(
            Optional.of(lb)
        );
        ImportRequest request = new ImportRequest(
            Instant.now(),
            Instant.now(),
            new TransactionMetadata("1", "1", "1", "1", "1"),
            "hallo".getBytes(),
            XZuFiVersion.XZUFI22,
            ImportRequestStatus.REOPENED
        );
        DeletedObject deletedObject = new DeletedObject(
            "1",
            Instant.now().plus(1, ChronoUnit.DAYS),
            IndexNames.LEISTUNGSBESCHREIBUNG
        );
        DateImportChecker importChecker = new DateImportChecker(request, readConnector);
        when(readConnector.getById(eq(IndexNames.DELETED_OBJECTS), any(), any())).thenReturn(
            Optional.of(deletedObject)
        );
        assertFalse(
            importChecker.importAllowed(
                "id",
                IndexNames.LEISTUNGSBESCHREIBUNG,
                LeistungEntity.class
            )
        );
    }

    @Test
    void testDeletedAfter() {
        LeistungEntity lb = new LeistungEntity22(Instant.now().minus(1, ChronoUnit.DAYS));
        when(readConnector.getById(eq(IndexNames.LEISTUNGSBESCHREIBUNG), any(), any())).thenReturn(
            Optional.of(lb)
        );
        ImportRequest request = new ImportRequest(
            Instant.now(),
            Instant.now(),
            new TransactionMetadata("1", "1", "1", "1", "1"),
            "hallo".getBytes(),
            XZuFiVersion.XZUFI22,
            ImportRequestStatus.REOPENED
        );
        DeletedObject deletedObject = new DeletedObject(
            "1",
            Instant.now().minus(1, ChronoUnit.DAYS),
            IndexNames.LEISTUNGSBESCHREIBUNG
        );
        DateImportChecker importChecker = new DateImportChecker(request, readConnector);
        when(readConnector.getById(eq(IndexNames.DELETED_OBJECTS), any(), any())).thenReturn(
            Optional.of(deletedObject)
        );
        assertTrue(
            importChecker.importAllowed(
                "id",
                IndexNames.LEISTUNGSBESCHREIBUNG,
                LeistungEntity.class
            )
        );
    }
}
