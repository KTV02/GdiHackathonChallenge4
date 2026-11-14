/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.indexer.service.odrelationimport;

import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import de.dataport.suchdienst.elasticsearch.api.AliasNames;
import de.dataport.suchdienst.elasticsearch.api.ElasticBulkWriteConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticWriteConnector;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.indexer.exception.ImportException;
import de.dataport.suchdienst.indexer.model.OnlinedienstRelationen;
import de.dataport.suchdienst.taskmgr.model.OdRelRecalcTask;
import de.dataport.suchdienst.taskmgr.model.SudTask;
import de.dataport.suchdienst.taskmgr.model.SudTaskMediator;
import de.dataport.suchdienst.taskmgr.model.SudTaskSubmissionResult;
import de.dataport.suchdienst.taskmgr.service.SudTaskMgr;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service zur Berechnung und Persistierung von Onlinedienst-Relationen.
 *
 * <p>Die Verarbeitung erfolgt seitenweise über die Onlinedienst-Quelle. Pro Batch werden
 * Zuständigkeiten und zugehörige Leika-IDs aufgelöst, in ein Relation-Objekt überführt und
 * anschließend per Bulk-Indexing in den aktuellen Write-Alias geschrieben.</p>
 */
@Service
@Slf4j
public class OdRelationImportService implements SudTaskMediator<Void> {

    private static final int WINDOW_SIZE = 2_000;

    private final ElasticWriteConnector writeConnector;
    private final ElasticBulkWriteConnector bulkWriteConnector;
    private final OdRelationDataLoader dataLoader;
    private final OdRelationAssembler assembler;
    private final SudTaskMgr taskMgr;

    public OdRelationImportService(
        ElasticWriteConnector writeConnector,
        ElasticBulkWriteConnector bulkWriteConnector,
        OdRelationDataLoader dataLoader,
        OdRelationAssembler assembler,
        SudTaskMgr taskMgr
    ) {
        this.writeConnector = writeConnector;
        this.bulkWriteConnector = bulkWriteConnector;
        this.dataLoader = dataLoader;
        this.assembler = assembler;
        this.taskMgr = taskMgr;
    }

    /**
     * Plant die Neuberechnung der Onlinedienst-Relationen für ein Redaktionssystem asynchron ein.
     *
     * @param schemeAgencyId Eindeutige Kennung des Redaktionssystems
     * @param producer Anzeigename des Redaktionssystems für Logging und Monitoring
     * @return Ergebnis der Task-Einlastung (z. B. angenommen oder abgelehnt)
     */
    public SudTaskSubmissionResult<Void> importAsync(String schemeAgencyId, String producer) {
        return taskMgr.submit(new OdRelRecalcTask(schemeAgencyId, producer));
    }

    /**
     * Löscht alle bestehenden Onlinedienst-Relationen eines Redaktionssystems.
     *
     * @param schemeAgencyId Eindeutige Kennung des Redaktionssystems
     * @param producer Anzeigename des Redaktionssystems für Logging und Monitoring
     */
    public void deleteRelationsForSchemeAgencyId(String schemeAgencyId, String producer) {
        DeleteByQueryResponse deleteByQueryResponse =
            writeConnector.deleteOnlineServiceRelationsBySchemeAgencyId(schemeAgencyId, producer);
        Long deleted = deleteByQueryResponse.deleted();
        log.info(
            "Gelöschte Onlinedienst-Relationen für Redaktionssystem '{}': {}",
            producer,
            deleted
        );
    }

    /**
     * Führt den Import für ein Redaktionssystem aus und mappt unerwartete Fehler in eine
     * domänenspezifische {@link ImportException}.
     *
     * @param schemeAgencyId Eindeutige Kennung des Redaktionssystems
     * @param producer Anzeigename des Redaktionssystems
     */
    private void importSchemeAgencyId(String schemeAgencyId, String producer) {
        try {
            runBatchedImport(schemeAgencyId, producer);
        } catch (Exception e) {
            throw new ImportException(
                ErrorCodeEnum.XZUFI_IMPORT_ERROR,
                "Fehler beim Import der Onlinedienst-Relationen für Redaktionssystem '" +
                    producer +
                    "'",
                e
            );
        }
    }

    /**
     * Importiert alle OD-Relationen für ein gegebenes Redaktionssystem.
     * Schreibt per Write-Alias in den jeweils aktuellen Index — funktioniert sowohl für
     * den Komplett-Recalc (Write-Alias zeigt auf neuen Index) als auch für den
     * Einzel-RS-Update (Write-Alias zeigt auf den bestehenden Index).
     */
    private void runBatchedImport(String schemeAgencyId, String producer) throws ImportException {
        Instant start = Instant.now();
        log.info(
            "Starte Berechnung des Onlinedienst-Relation-Index für Redaktionssystem '{}'",
            producer
        );

        OdRelationImportProgress progress = new OdRelationImportProgress(
            start,
            dataLoader.countOnlinedienste(schemeAgencyId)
        );

        String fromId = null;
        boolean hasMore = true;
        while (hasMore) {
            Instant batchStart = Instant.now();
            ImportPageResult result = importFromId(schemeAgencyId, fromId);
            Instant batchEnd = Instant.now();

            fromId = result.lastId();
            // War die Seite kleiner als WINDOW_SIZE, gibt es keine weiteren Batches
            hasMore = result.importedCount() == WINDOW_SIZE;

            logBatchProgress(producer, progress.recordBatch(result, batchStart, batchEnd));
        }

        logCompletion(producer, progress.finish(Instant.now()));
    }

    private void logBatchProgress(
        String producer,
        OdRelationImportProgress.BatchSnapshot batchSnapshot
    ) {
        log.info(
            "OD-Relation ({}) Batch #{} importiert: {} Datensätze in {}ms | Gesamt: {}/{} | Durchsatz: {}/s | Letzte ID: {}",
            producer,
            batchSnapshot.batchNumber(),
            batchSnapshot.importedCount(),
            batchSnapshot.batchDuration().toMillis(),
            batchSnapshot.totalImported(),
            batchSnapshot.totalDocs(),
            OdRelationImportProgress.formatThroughput(
                batchSnapshot.totalImported(),
                batchSnapshot.totalDuration()
            ),
            batchSnapshot.lastId()
        );
    }

    private void logCompletion(
        String producer,
        OdRelationImportProgress.CompletionSnapshot completion
    ) {
        if (completion.isEmpty()) {
            log.warn(
                "Keine Onlinedienst-Relationen für Redaktionssystem '{}' zum Importieren gefunden. Dauer: {}",
                producer,
                OdRelationImportProgress.formatDuration(completion.totalDuration())
            );
            return;
        }

        log.info(
            "Berechnung der Onlinedienst-Zuständigkeiten für Redaktionssystem '{}' abgeschlossen. Gesamt: {} Datensätze in {} Batches | Dauer: {} | Durchschnitt: {}/s",
            producer,
            completion.totalImported(),
            completion.batchCount(),
            OdRelationImportProgress.formatDuration(completion.totalDuration()),
            OdRelationImportProgress.formatThroughput(
                completion.totalImported(),
                completion.totalDuration()
            )
        );
    }

    /**
     * Importiert genau eine Seite Onlinedienste ab einer gegebenen Cursor-ID.
     *
     * @param schemeAgencyId Eindeutige Kennung des Redaktionssystems
     * @param fromId Cursor-ID für die nächste Seite; {@code null} startet am Anfang
     * @return Ergebnis der verarbeiteten Seite inkl. nächster Cursor-ID
     * @throws ImportException Bei Fehlern in der Datenverarbeitung oder Persistierung
     */
    private ImportPageResult importFromId(String schemeAgencyId, String fromId)
        throws ImportException {
        OdRelationDataLoader.BatchData batch = dataLoader.loadBatch(
            schemeAgencyId,
            fromId,
            WINDOW_SIZE
        );
        if (batch == null) {
            return ImportPageResult.empty();
        }

        List<OnlinedienstRelationen> relationEntities = assembler.createRelationEntities(
            batch.odsById().values(),
            batch.context()
        );
        bulkWriteConnector.bulkIndexThrowIfError(
            relationEntities,
            AliasNames.ONLINEDIENSTE_RELATIONEN
        );

        return new ImportPageResult(batch.lastId(), relationEntities.size());
    }

    /**
     * Ergebnis einer importierten Seite inkl. Cursor für die nächste Seite.
     *
     * @param lastId Letzte verarbeitete OD-ID; {@code null} signalisiert Ende
     * @param importedCount Anzahl der importierten Datensätze in dieser Seite
     */
    record ImportPageResult(String lastId, int importedCount) {
        /**
         * Liefert ein leeres Ergebnis als Endesignal für die Batch-Schleife.
         *
         * @return Leeres Seiten-Ergebnis
         */
        static ImportPageResult empty() {
            return new ImportPageResult(null, 0);
        }
    }

    @Override
    public Supplier<Void> mediate(SudTask<Void> task) {
        if (task instanceof OdRelRecalcTask recalcTask) {
            return () -> {
                importSchemeAgencyId(recalcTask.getSchemeAgencyId(), recalcTask.getProducer());
                return null;
            };
        }
        return null;
    }

    @Override
    public Class<Void> getMediatorType() {
        return Void.class;
    }
}
