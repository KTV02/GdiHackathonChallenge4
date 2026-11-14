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

import static java.util.stream.Collectors.toSet;

import de.dataport.suchdienst.config.InstanceType;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.elasticsearch.api.AliasNames;
import de.dataport.suchdienst.elasticsearch.api.ElasticBulkWriteConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticInternalReadConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticWriteConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexConfiguration;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.elasticsearch.api.model.ElasticBulkError;
import de.dataport.suchdienst.error.SuchdienstException;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.importer.model.GenericTransferOperation;
import de.dataport.suchdienst.importer.service.AlwaysAllowedImportChecker;
import de.dataport.suchdienst.indexer.model.AffectedLbIdsEntry;
import de.dataport.suchdienst.indexer.model.measurement.ImportMeasurement;
import de.dataport.suchdienst.indexer.model.measurement.MeasurementType;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.Verwaltungsleistung;
import de.dataport.suchdienst.taskmgr.model.SudTask;
import de.dataport.suchdienst.taskmgr.model.SudTaskMediator;
import de.dataport.suchdienst.taskmgr.model.SudTaskType;
import de.dataport.suchdienst.taskmgr.model.VlRecalcTask;
import de.dataport.suchdienst.taskmgr.model.VlUpdateTask;
import de.dataport.suchdienst.taskmgr.model.out.SudTaskOutModel;
import de.dataport.suchdienst.taskmgr.service.SudTaskMgr;
import de.dataport.suchdienst.util.Bundeslaender;
import de.dataport.suchdienst.util.Exceptions;
import de.dataport.suchdienst.util.LogUtil;
import de.dataport.suchdienst.util.MdcTemplate;
import de.dataport.suchdienst.util.XzufiObjectProvider;
import de.dataport.suchdienst.util.model.MdcKeys;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

@Service
@Slf4j
public class VerwaltungsleistungImportService implements SudTaskMediator<Void> {

    private final SuchdienstProperties properties;
    private final XzufiParserService xzufiParserService;
    private final ElasticIndexService elasticIndexService;
    private final ElasticWriteConnector elasticWriteConnector;
    private final ElasticBulkWriteConnector elasticBulkWriteConnector;
    private final ElasticInternalReadConnector internalReadConnector;
    private final ElasticReadConnector readConnector;
    private final XzufiFetchService xzufiFetchService;
    private final AffectedLbResolver affectedLbResolver;
    private final SuggestionImportService suggImportService;
    private final IndexConfiguration indexConfiguration;
    private final SudTaskMgr sudTaskMgr;

    public VerwaltungsleistungImportService(
        SuchdienstProperties properties,
        XzufiParserService xzufiParserService,
        ElasticIndexService elasticIndexService,
        ElasticWriteConnector elasticWriteConnector,
        ElasticInternalReadConnector internalReadConnector,
        ElasticReadConnector readConnector,
        ElasticBulkWriteConnector elasticBulkWriteConnector,
        XzufiFetchService xzufiFetchService,
        AffectedLbResolver affectedLbResolver,
        SuggestionImportService suggImportService,
        IndexConfiguration indexConfiguration,
        SudTaskMgr sudTaskMgr
    ) {
        this.properties = properties;
        this.xzufiParserService = xzufiParserService;
        this.elasticIndexService = elasticIndexService;
        this.elasticWriteConnector = elasticWriteConnector;
        this.elasticBulkWriteConnector = elasticBulkWriteConnector;
        this.internalReadConnector = internalReadConnector;
        this.readConnector = readConnector;
        this.xzufiFetchService = xzufiFetchService;
        this.affectedLbResolver = affectedLbResolver;
        this.suggImportService = suggImportService;
        this.indexConfiguration = indexConfiguration;
        this.sudTaskMgr = sudTaskMgr;
    }

    /**
     * Führt ein Update der Verwaltungsleistungen im currentIndex durch, für das Bundesland und mit den LbIds aus dem Task.
     */
    private void processVlUpdateTask(VlUpdateTask task) {
        var lbId = task.getLbId();
        var targetArsFederalState = task.getTargetArsFederalState();
        var bundesland = Bundeslaender.getByArsFederalstate(targetArsFederalState);
        // TODO: Nicht das Bundesland loggen und nicht mehr auf die Statischen SchemeAgencyIds zugreifen.
        // Stattdessen den arsFederalState loggen?
        try (
            var _ = MdcTemplate.scoped(
                Map.of(MdcKeys.BUNDESLAND, bundesland.name().toLowerCase(), MdcKeys.LBID, lbId)
            )
        ) {
            try {
                if (Thread.interrupted()) {
                    log.info("Signal zum Abbruch der Verarbeitung empfangen.");
                    return;
                }
                log.info("Verarbeite Leistung '{}'", lbId);
                Instant deleteBefore = Instant.now();
                // Löscht Suggestions, sie werden beim Import neu berechnet und angelegt
                elasticWriteConnector.deleteSuggestionByLb(lbId);
                Optional<GenericTransferOperation> genericTransferOperation =
                    xzufiFetchService.fetchTransferOperation(lbId, targetArsFederalState);

                if (genericTransferOperation.isPresent()) {
                    ImportMeasurement m = importVerwaltungsleistungen(
                        genericTransferOperation.get(),
                        targetArsFederalState
                    );
                    MdcTemplate.runWith(measurementMdc(m), () ->
                        log.info(
                            "{}, ARS '{}', Dokumente: {} | Import von Leistung '{}' abgeschlossen",
                            LogUtil.millisToSeconds(m.getDuration().toMillis()),
                            targetArsFederalState,
                            m.getObjectCount(),
                            lbId
                        )
                    );
                } else {
                    log.info(
                        "ARS '{}', Leistung '{}' wurde gelöscht.",
                        targetArsFederalState,
                        lbId
                    );
                }
                StopWatch sw = new StopWatch();
                sw.start();
                // Bei der Neu-Berechnung könnten sich die ARS-Partitionen durch veränderte Zuständigkeiten verändert haben.
                // Dann würden nicht alle bisherigen Verwaltungsleistungen überschrieben sein.
                // Deshalb löschen wir an dieser Stelle alle älteren Verwaltungsleistungen zu einer LB-ID.
                elasticWriteConnector.deleteVerwaltungsleistungByLb(
                    targetArsFederalState,
                    lbId,
                    deleteBefore
                );
                sw.stop();
                log.debug(
                    "{}, Löschen von Verwaltungsleistungen zur Leistung '{}' vor '{}'",
                    LogUtil.millisToSeconds(sw.getTotalTimeMillis()),
                    lbId,
                    deleteBefore
                );
            } catch (Exception e) {
                log.error("Fehler beim Reimport für die LB-ID {}", lbId, e);
            }
        }
    }

    /**
     * Führt eine vollständige Neuberechnung eines Landesindex mit Alias-Swap durch.
     *
     * <p>Es wird ein neuer Index erzeugt, der Write-Alias auf diesen Index umgelegt,
     * anschließend der Import durchgeführt und bei Erfolg der Read-Alias finalisiert.
     * Bei Fehlern wird ein best-effort Rollback auf den vorherigen Indexzustand ausgeführt.
     *
     * @param targetArsFederalstate ARS-Präfix des Ziel-Bundeslandes
     */
    private void createNewIndexAndImportVerwaltungsleistungen(String targetArsFederalstate) {
        log.info(
            "Starte Neuberechnung der Verwaltungsleistungen für Gebiet '{}'",
            targetArsFederalstate
        );
        var startMillis = System.currentTimeMillis();
        String newIndexName = null;
        String oldIndexName = null;
        AliasNames alias = AliasNames.ARS_MAPPING.get(targetArsFederalstate);
        try {
            // Der Bund gehört zur Menge der Leistungen in den Ländern dazu
            List<String> targetArsFederalStates = List.of(
                targetArsFederalstate,
                Bundeslaender.BUND.getArsFederalstate()
            );
            long totalLbCnt = internalReadConnector.countLeistungsbeschreibungen(
                targetArsFederalStates
            );
            if (totalLbCnt == 0) {
                log.info("Keine Leistungen für ARS '{}' gefunden", targetArsFederalStates);
                return;
            }

            ElasticIndexService.IndexSwapResult newIndexSwapWriteAlias =
                elasticIndexService.createNewIndexSwapWriteAlias(
                    IndexNames.ARS_MAPPING.get(targetArsFederalstate),
                    AliasNames.ARS_MAPPING.get(targetArsFederalstate)
                );
            newIndexName = newIndexSwapWriteAlias.newIndexName();
            oldIndexName = newIndexSwapWriteAlias.oldIndexName();

            importAllVerwaltungsleistungen(targetArsFederalstate, totalLbCnt);

            // Read-Alias schwenken, alten Index löschen
            elasticIndexService.finalizeIndex(newIndexName, oldIndexName, alias);
            var stopMillis = System.currentTimeMillis();
            log.info(
                "{} ms Neuberechnung für Gebiet '{}' beendet",
                stopMillis - startMillis,
                targetArsFederalstate
            );
        } catch (Exception e) {
            Exceptions.findCause(e, InterruptedException.class).ifPresent(ex ->
                Thread.currentThread().interrupt()
            );
            log.warn(
                "Fehler bei der Neuberechnung für Gebiet '{}', lösche Index {}",
                targetArsFederalstate,
                newIndexName,
                e
            );

            // Im Fehlerfall versuchen wir hier wieder den alten Alias-Zustand herzustellen. Dies kann nur klappen,
            // wenn der oldIndex überhaupt noch vorhanden ist und ergibt nur Sinn, wenn es schon den neuen Index gibt.
            elasticIndexService.rollbackIndex(newIndexName, oldIndexName, alias);
        }
    }

    /**
     * Baut die MDC-Einträge für ein Import-Ergebnis, gemeinsam genutzt vom Einzel- und Massenimport-Log.
     */
    private static Map<String, String> measurementMdc(ImportMeasurement measurement) {
        return Map.of(
            MdcKeys.IMPORT_DURATION,
            String.valueOf(measurement.getDuration().toMillis()),
            MdcKeys.OBJECT_COUNT,
            String.valueOf(measurement.getObjectCount())
        );
    }

    private void importAllVerwaltungsleistungen(String targetArsFederalstate, long totalLbCnt) {
        Instant start = Instant.now();
        int lbCnt = 0;
        for (
            Optional<Pair<String, GenericTransferOperation>> oXzufi =
                xzufiFetchService.fetchNextTransferOperation(null, targetArsFederalstate);
            oXzufi.isPresent();
            oXzufi = xzufiFetchService.fetchNextTransferOperation(
                oXzufi.get().getLeft(),
                targetArsFederalstate
            )
        ) {
            String lbId = oXzufi.get().getLeft();
            try (var _ = MdcTemplate.scoped(MdcKeys.LBID, lbId)) {
                var currentGenericTransferOp = oXzufi.get().getRight();
                if (Thread.interrupted()) {
                    log.info("Signal zum Abbruch der Verarbeitung empfangen.");
                    return;
                }
                // Löscht Suggestions, sie werden beim Import neu berechnet und angelegt
                elasticWriteConnector.deleteSuggestionByLb(lbId);
                ImportMeasurement m = importVerwaltungsleistungen(
                    currentGenericTransferOp,
                    targetArsFederalstate
                );
                int currentLbCnt = ++lbCnt;
                MdcTemplate.runWith(measurementMdc(m), () ->
                    log.info(
                        "{}, ARS '{}', Fortschritt: {}/{}, Dokumente: {} | Import von Leistung '{}' abgeschlossen",
                        LogUtil.millisToSeconds(m.getDuration().toMillis()),
                        targetArsFederalstate,
                        currentLbCnt,
                        totalLbCnt,
                        m.getObjectCount(),
                        lbId
                    )
                );
            }
        }
        ImportMeasurement m = ImportMeasurement.builder()
            .type(MeasurementType.XZUFI_VERARBEITUNG_NORMAL)
            .startTime(start)
            .endTime(Instant.now())
            .build();
        log.info(
            "Import von {}/{} Leistungen abgeschlossen. ARS '{}', {}",
            lbCnt,
            totalLbCnt,
            targetArsFederalstate,
            m
        );
    }

    /**
     * Generiert Verwaltungsleistungen aus der TransferOperation und schreibt die VLs.
     * Optional kann eine Einschränkung auf ein Zielgebiet angegeben werden, in diesem Fall werden nur die VLs für dieses
     * Gebiet geschrieben.
     * VLS für unterschiedliche Gebiete können nur bei LigV vorkommen.
     *
     * @param currentGenericTransferOp Die GenericTransferOperation, hieraus werden die VLs geparst
     * @return Import-Metrik
     */
    public ImportMeasurement importVerwaltungsleistungen(
        GenericTransferOperation currentGenericTransferOp,
        String targetArsFederalState
    ) {
        AliasNames aliasName = AliasNames.ARS_MAPPING.get(targetArsFederalState);

        Instant start = Instant.now();
        int objectCount;
        try {
            XzufiObjectProvider operation = new XzufiObjectProvider(
                currentGenericTransferOp,
                new AlwaysAllowedImportChecker()
            );

            // Parsen von XzuFi
            List<Verwaltungsleistung> vls = xzufiParserService.parseXzufi(
                operation,
                targetArsFederalState
            );
            objectCount = vls.size();

            // Schreibt Suggestions
            List<ElasticBulkError> suggestionErrors = List.of();
            if (ObjectUtils.isNotEmpty(vls)) {
                // Die Verwaltungsleistungen enthalten alle die gleiche Leistungsbeschreibung.
                // Daher kann beim Schreiben der Suggestions mit der ersten gearbeitet werden.
                // Dies erspart ein nachträgliches Eliminieren von Duplikaten, die hier schon absehbar sind.
                checkVerwaltungsleistungen(vls, targetArsFederalState);
                LeistungsbeschreibungVL lb = vls.getFirst().getLeistungsbeschreibung();
                suggestionErrors = suggImportService.importSuggestions(lb);
            }

            // Einfügen von Geschäftsobjekten in ElasticSearch
            var vlsErrors = writeVerwaltungsleistungen(vls, aliasName);
            // Verifizieren der Antworten von ElasticSearch
            var errors = new ArrayList<>(vlsErrors);
            errors.addAll(suggestionErrors);
            IndexerUtil.throwIfError(log, errors);
        } catch (Exception e) {
            Exceptions.findCause(e, InterruptedException.class).ifPresent(ex ->
                Thread.currentThread().interrupt()
            );
            String errorCode =
                e instanceof SuchdienstException suchdienstException
                    ? String.valueOf(suchdienstException.getCode().getNumber())
                    : String.valueOf(ErrorCodeEnum.XZUFI_IMPORT_ERROR.getNumber());
            MdcTemplate.runWith(MdcKeys.ERROR_CODE, errorCode, () ->
                log.error("IMPORT - FEHLER: ", e)
            );
            return ImportMeasurement.builder()
                .type(MeasurementType.XZUFI_VERARBEITUNG_NORMAL)
                .startTime(start)
                .endTime(Instant.now())
                .objectCount(0)
                .success(false)
                .build();
        }
        return ImportMeasurement.builder()
            .type(MeasurementType.XZUFI_VERARBEITUNG_NORMAL)
            .startTime(start)
            .endTime(Instant.now())
            .success(true)
            .objectCount(objectCount)
            .build();
    }

    /**
     * Prüfen, ob alle Verwaltungsleistungen nur Ars im arsFederalState haben.
     * Hiermit sichern wir ab, dass die Xzufi-Daten, aus denen die VLs generiert wurden,
     * korrekt für den Anwendungsfall vorgeladen waren. Ein versehentliches Over-Fetching
     * (z.b. von Zuständigkeiten in ein anderes Gebiet) würde dann dazu führen, dass Ars in den VLs auftauchen,
     * die nicht in dem FederalState liegen.
     * Für unsere beiden Standard-Verfahren (Ausführen von VLUpdateTask und VLRecalcTask sollten wir das durch
     * gezieltes Fetching im Griff haben.
     * Es gibt aber auch noch APIs, die einen Upload von XZuFi ermöglichen und VL-Generierung für ein
     * FederalState anbieten. Hier gibt es dann keine Kontrolle durch Fetching.
     */
    //TODO Methode/Prüfung entfernen, wenn sich der XZuFiFetchService bewiesen hat
    private void checkVerwaltungsleistungen(List<Verwaltungsleistung> vls, String arsFederalState) {
        Set<String> arsOutsideFederalState = vls
            .stream()
            .flatMap(vl -> Stream.concat(vl.getArs().stream(), vl.getExactArs().stream()))
            // Filtere alle ARS heraus, die dem Ziel-ARS entsprechen
            .filter(ars -> !ars.startsWith(arsFederalState))
            // Filtere alle ARS heraus, die dem Bund-ARS entsprechen
            .filter(ars -> !ars.startsWith(Bundeslaender.BUND.getArsFederalstate()))
            .collect(toSet());
        if (!arsOutsideFederalState.isEmpty()) {
            // Alle VL haben dieselbe Leistungsbeschreibung
            LeistungsbeschreibungVL lb = vls.getFirst().getLeistungsbeschreibung();
            String lbId = lb.getId();
            log.warn(
                """
                Aus der LB mit lbId {} wurden Verwaltungsleistungen generiert,\s
                in denen Ars auftreten, die nicht innerhalb des Zielgebietes {} liegen: {}.\s
                Möglicherweise besteht ein Problem beim Fetching oder Upload der XZuFi-Daten.
                """,
                lbId,
                arsFederalState,
                arsOutsideFederalState
            );
        }
    }

    /**
     * Leert den Landesindex technisch durch Erzeugen eines frischen Indexes
     * und unmittelbares Finalisieren des Alias-Swaps ohne Datenimport.
     *
     * @param arsFederalstate ARS-Präfix des Bundeslandes
     */
    public void dropAndCreateIndex(String arsFederalstate) {
        IndexNames index = IndexNames.ARS_MAPPING.get(arsFederalstate);
        AliasNames alias = AliasNames.ARS_MAPPING.get(arsFederalstate);
        var swapResult = elasticIndexService.createNewIndexSwapWriteAlias(index, alias);
        elasticIndexService.finalizeIndex(
            swapResult.newIndexName(),
            swapResult.oldIndexName(),
            alias
        );
        log.info(
            "Index geleert, ARS '{}', Neuer Index '{}'",
            arsFederalstate,
            swapResult.newIndexName()
        );
    }

    private List<ElasticBulkError> writeVerwaltungsleistungen(
        List<Verwaltungsleistung> vls,
        AliasNames aliasName
    ) {
        StopWatch sw = new StopWatch();
        sw.start();
        List<ElasticBulkError> errors = elasticBulkWriteConnector.bulkIndex(vls, aliasName);
        sw.stop();
        log.debug(
            "{}, Übertragung von {} Dokumenten ",
            LogUtil.millisToSeconds(sw.lastTaskInfo().getTimeMillis()),
            vls.size()
        );
        return errors;
    }

    /**
     * Befüllt den AffectedLbResolver aus ElasticSearch, dies garantiert Wiederanlauf-Fähigkeit.
     * Wird vom SuchdienstInitializer aufgerufen.
     */
    public void afterStartup() {
        if (InstanceType.MASTER.equals(properties.getInstanceType())) {
            log.info("Befülle AffectedLbResolver nach Applikationsstart");
            var affectedLbIdsEntries = readConnector.getAffectedLbIdsEntries();
            affectedLbResolver.init(affectedLbIdsEntries);
            elasticWriteConnector.clearIndex(
                indexConfiguration.getCurrentIndexName(IndexNames.AFFECTED_LBIDS)
            );
        }
    }

    /**
     * Während des Shutdowns alle noch in Memory befindlichen affectedLbIds speichern, diese werden beim Wiederanlauf geladen.
     * Hierbei auch berücksichtigen, dass es noch nicht fertig ausgeführte VLUpdateTasks geben könnte (die ja ebenfalls nicht persistiert sind),
     * d.h auch die Daten dieser Tasks werden hier persistiert.
     */
    @PreDestroy
    public void preDestroy() {
        record TaskKey(String schemeAgencyId, String producer, String arsFederalState) {
            TaskKey(SudTaskOutModel task) {
                this(task.getSchemeAgencyId(), task.getProducer(), task.getArsFederalState());
            }
        }
        if (InstanceType.MASTER.equals(properties.getInstanceType())) {
            log.info("Shutdown wurde initiiert - speichere VLUpdateTasks...");
            // Alle VLUpdateTasks aus dem TaskMgr dazufügen
            var tasksSnapshot = sudTaskMgr.getTasksSnapshot();

            Map<TaskKey, Set<String>> lbIdByKey = Stream.of(
                tasksSnapshot.getRunningTasks(),
                tasksSnapshot.getWaitingTasks()
            )
                .flatMap(Collection::stream)
                .filter(t -> t.getTaskType() == SudTaskType.VL_UPDATE)
                .collect(
                    Collectors.groupingBy(
                        TaskKey::new,
                        Collectors.mapping(SudTaskOutModel::getUpdateLbId, toSet())
                    )
                );

            List<AffectedLbIdsEntry> entriesToPersist = lbIdByKey
                .entrySet()
                .stream()
                .map(entry ->
                    new AffectedLbIdsEntry(
                        entry.getKey().schemeAgencyId(),
                        entry.getKey().producer(),
                        entry.getKey().arsFederalState(),
                        entry.getValue()
                    )
                )
                .toList();

            elasticBulkWriteConnector.bulkIndex(entriesToPersist, IndexNames.AFFECTED_LBIDS);
            Integer persistedCount = entriesToPersist
                .stream()
                .mapToInt(AffectedLbIdsEntry::size)
                .sum();
            log.info("{} affectedLbIds und VLUpdateTasks gespeichert.", persistedCount);
        }
    }

    @Override
    public Supplier<Void> mediate(SudTask<Void> task) {
        if (task instanceof VlRecalcTask vlRecalcTask) {
            var bundesland = vlRecalcTask.getBundesland();
            return () -> {
                try (
                    var _ = MdcTemplate.scoped(MdcKeys.BUNDESLAND, bundesland.getLowerCaseName())
                ) {
                    createNewIndexAndImportVerwaltungsleistungen(bundesland.getArsFederalstate());
                    return null;
                }
            };
        } else if (task instanceof VlUpdateTask vlUpdateTask) {
            return () -> {
                processVlUpdateTask(vlUpdateTask);
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
