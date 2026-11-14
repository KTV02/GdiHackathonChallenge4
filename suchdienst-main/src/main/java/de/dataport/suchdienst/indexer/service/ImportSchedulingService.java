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

import static de.dataport.suchdienst.taskmgr.model.SudTaskSubmissionResultState.ACCEPTED;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.elasticsearch.api.AliasNames;
import de.dataport.suchdienst.elasticsearch.api.ElasticInternalReadConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ExceptionType;
import de.dataport.suchdienst.importer.model.GenericTransferOperation;
import de.dataport.suchdienst.indexer.exception.ImportException;
import de.dataport.suchdienst.indexer.model.measurement.ImportMeasurement;
import de.dataport.suchdienst.indexer.service.odrelationimport.OdRelationImportService;
import de.dataport.suchdienst.taskmgr.model.SudTaskSubmissionResult;
import de.dataport.suchdienst.taskmgr.model.SudTaskType;
import de.dataport.suchdienst.taskmgr.model.VlRecalcTask;
import de.dataport.suchdienst.taskmgr.service.SudTaskMgr;
import de.dataport.suchdienst.util.Bundeslaender;
import de.dataport.suchdienst.util.Exceptions;
import de.dataport.suchdienst.util.MdcTemplate;
import de.dataport.suchdienst.util.model.MdcKeys;
import de.xoev.xzufi.v2_2_0.TransferOperation040502;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class ImportSchedulingService {

    private final SuchdienstProperties properties;
    private final TaskScheduler scheduler;
    private final ElasticInternalReadConnector readConnector;
    private final VerwaltungsleistungImportService verwaltungsleistungImportService;
    private final OdRelationImportService odRelationImportService;
    private final LeikaService leikaService;
    private final MessageSource messageSource;
    private final ElasticIndexService elasticIndexService;
    private final SudTaskMgr sudTaskMgr;
    private final XmlBinder xmlBinder;

    public ImportSchedulingService(
        SuchdienstProperties properties,
        @Qualifier("singleTaskExecutor") TaskScheduler scheduler,
        ElasticInternalReadConnector readConnector,
        VerwaltungsleistungImportService verwaltungsleistungImportService,
        OdRelationImportService odRelationImportService,
        LeikaService leikaService,
        MessageSource messageSource,
        ElasticIndexService elasticIndexService,
        SudTaskMgr sudTaskMgr,
        XmlBinder xmlBinder
    ) {
        this.properties = properties;
        this.scheduler = scheduler;
        this.readConnector = readConnector;
        this.verwaltungsleistungImportService = verwaltungsleistungImportService;
        this.odRelationImportService = odRelationImportService;
        this.leikaService = leikaService;
        this.messageSource = messageSource;
        this.elasticIndexService = elasticIndexService;
        this.sudTaskMgr = sudTaskMgr;
        this.xmlBinder = xmlBinder;
    }

    public void schedulePeriodicImport() {
        scheduleVlImport();
        scheduleOdRelImport();
    }

    private void scheduleVlImport() {
        CronTrigger trigger = new CronTrigger(
            properties.getImporter().getCronTriggerVerwaltungsleistungReImport()
        );
        scheduler.schedule(() -> {
            try {
                leikaService.importLeistungskatalog();
                recalcAllVlIndexesAndWait();
            } catch (Exception e) {
                Exceptions.findCause(e, InterruptedException.class).ifPresent(_ ->
                    Thread.currentThread().interrupt()
                );
                log.warn("Fehler beim periodischen Import der Verwaltungsleistungen", e);
            }
        }, trigger);
    }

    private void scheduleOdRelImport() {
        CronTrigger trigger = new CronTrigger(
            properties.getImporter().getCronTriggerOnlineServiceRelationsImport()
        );
        scheduler.schedule(() -> {
            try {
                recalcAllOdRelIndexesAndWait();
            } catch (Exception e) {
                Exceptions.findCause(e, InterruptedException.class).ifPresent(_ ->
                    Thread.currentThread().interrupt()
                );
                log.warn("Fehler beim periodischen Import der Onlinedienste-Relationen", e);
            }
        }, trigger);
    }

    /**
     * Veranlasst eine Neuberechnung aller VL Indexes,
     * wartet dann auf den Abschluss der Verarbeitung, um den Platz im Scheduler weiterhin zu belegen
     * und keine konkurrierenden Aufgaben zuzulassen.
     */
    public void recalcAllVlIndexesAndWait() {
        elasticIndexService.cleanRemainingIndices();
        this.importAllIndexesAsync();
        this.waitForTasks(SudTaskType.VL_RECALC);
    }

    /**
     * Lastet für jedes Bundesland einen SudTask zur Neuberechnung des VL-Index ein.
     */
    public void importAllIndexesAsync() {
        log.info(
            "Starte Berechnung der Verwaltungsleistungen fuer Gebiete '{}'",
            IndexNames.ARS_MAPPING.keySet()
        );
        IndexNames.ARS_MAPPING.keySet().forEach(this::submitVlRecalcTask);
    }

    /**
     * Lastet für ein Bundesland einen SudTask zur Neuberechnung des VL-Index ein.
     */
    public boolean importIndexAsync(String arsFederalState) {
        var submissionResult = submitVlRecalcTask(arsFederalState);
        return submissionResult.state() == ACCEPTED;
    }

    private SudTaskSubmissionResult<Void> submitVlRecalcTask(String arsFederalstate) {
        var bundesland = Bundeslaender.getByArsFederalstate(arsFederalstate);
        var task = new VlRecalcTask(bundesland);
        return sudTaskMgr.submit(task);
    }

    /**
     * Führt eine vollständige Neuberechnung des OD-Relation-Index aus.
     *
     * <p>Ablauf: alte Rest-Indizes bereinigen, neuen Zielindex erstellen, Write-Alias umschwenken,
     * Recalc-Tasks für alle Redaktionssysteme abwarten und danach den Read-Alias finalisieren.
     * Bei Fehlern wird ein Rollback auf den vorherigen Indexzustand versucht.
     */
    public void recalcAllOdRelIndexesAndWait() {
        elasticIndexService.cleanRemainingIndices();

        // Neuen Index erstellen und Write-Alias schwenken, bevor Tasks gestartet werden
        var indexSwap = elasticIndexService.createNewIndexSwapWriteAlias(
            IndexNames.ONLINEDIENSTE_RELATIONEN,
            AliasNames.ONLINEDIENSTE_RELATIONEN
        );

        try {
            this.importAllOnlineServiceRelationsAsync();
            this.waitForTasks(SudTaskType.OD_RE_RECALC);

            // Read-Alias schwenken, alten Index löschen
            elasticIndexService.finalizeIndex(
                indexSwap.newIndexName(),
                indexSwap.oldIndexName(),
                AliasNames.ONLINEDIENSTE_RELATIONEN
            );
        } catch (Exception e) {
            Exceptions.findCause(e, InterruptedException.class).ifPresent(_ ->
                Thread.currentThread().interrupt()
            );
            log.warn(
                "Fehler beim Komplett-Recalc der OD-Relationen, Rollback auf alten Index '{}'",
                indexSwap.oldIndexName(),
                e
            );
            elasticIndexService.rollbackIndex(
                indexSwap.newIndexName(),
                indexSwap.oldIndexName(),
                AliasNames.ONLINEDIENSTE_RELATIONEN
            );
        }
    }

    /**
     * Lastet für jedes Redaktionssystem einen SudTask zur Neuberechnung des OD-Relation-Index ein.
     */
    public void importAllOnlineServiceRelationsAsync() {
        var agencyIdToProducer = readConnector.getDataOverviewAgencyIdAndProducer(
            List.of(IndexNames.ONLINEDIENSTE)
        );
        log.info(
            "Starte Berechnung der Onlinedienste-Relationen fuer Redaktionssysteme '{}'",
            agencyIdToProducer
        );

        agencyIdToProducer.forEach(odRelationImportService::importAsync);
    }

    public boolean importOnlineServiceRelationsAsync(String schemeAgencyId, String producer) {
        odRelationImportService.deleteRelationsForSchemeAgencyId(schemeAgencyId, producer);
        var submissionResult = odRelationImportService.importAsync(schemeAgencyId, producer);
        return submissionResult.state() == ACCEPTED;
    }

    public void uploadFile(MultipartFile file, String arsFederalstate) throws IOException {
        byte[] xzufiFile = file.getBytes();
        scheduler.schedule(() -> performFileUpload(xzufiFile, arsFederalstate), Instant.now());
    }

    public void deleteAll() {
        log.info("Lösche alle Verwaltungsleistungen");
        AliasNames.ARS_MAPPING.keySet().forEach(this::deleteFederalState);
    }

    public void deleteFederalState(String arsFederalState) {
        scheduler.schedule(
            () -> verwaltungsleistungImportService.dropAndCreateIndex(arsFederalState),
            Instant.now()
        );
    }

    public ImportMeasurement importVerwaltungsleistungenFromByteArray(
        byte[] xZufi,
        String arsFederalState
    ) {
        TransferOperation040502 transferOperation = xmlBinder.unmarshallXzufi(xZufi);
        var genericTransferOp = new GenericTransferOperation(transferOperation);
        return verwaltungsleistungImportService.importVerwaltungsleistungen(
            genericTransferOp,
            arsFederalState
        );
    }

    private void performFileUpload(byte[] file, String arsFederalState) {
        importVerwaltungsleistungenFromByteArray(file, arsFederalState);
    }

    public void importFiles(String path, String arsFederalstate) {
        scheduler.schedule(() -> performFileImport(path, arsFederalstate), Instant.now());
    }

    public void waitForTasks(SudTaskType taskType) {
        sudTaskMgr.waitForTasks(task -> task.getTaskType() == taskType);
    }

    private void performFileImport(String path, String arsFederalState) {
        Long start = System.currentTimeMillis();
        File directory = new File(path);

        if (!directory.isDirectory()) {
            throw new ImportException(
                ErrorCodeEnum.XZUFI_IMPORT_ERROR,
                messageSource.getMessage(
                    "xzufi_import_resource.exception_path_is_no_dir_error",
                    null,
                    Locale.GERMAN
                ),
                ExceptionType.TECHNISCH
            );
        }

        try (Stream<Path> files = Files.walk(Paths.get(path))) {
            Set<File> filesSet = files
                .map(Path::toFile)
                .filter(f -> f.isFile() && f.getName().endsWith(".xml"))
                .collect(Collectors.toSet());
            for (File file : filesSet) {
                log.debug("Importiere {}", file);
                importVerwaltungsleistungenFromFile(arsFederalState, file);
            }
        } catch (IOException e) {
            log.error("Fehler beim Import von {}: ", path, e);
        }

        Long end = System.currentTimeMillis();
        log.debug("Gesamtdauer: {}", end - start);
    }

    private void importVerwaltungsleistungenFromFile(String arsFederalState, File file) {
        try (var _ = MdcTemplate.scoped(MdcKeys.XZUFIFILE, file.getName())) {
            ImportMeasurement m = importVerwaltungsleistungenFromByteArray(
                Files.readAllBytes(file.toPath()),
                arsFederalState
            );
            log.debug("Import-Statistik fuer {} {}", file, keyValue("statistics", m));
        } catch (Exception e) {
            log.warn("Ignoriere Fehler beim Import von {}:", file, e);
        }
    }
}
