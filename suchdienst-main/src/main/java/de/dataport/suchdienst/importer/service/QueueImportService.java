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

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import de.dataport.suchdienst.config.InstanceType;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.elasticsearch.api.exception.ElasticSearchException;
import de.dataport.suchdienst.error.ApiWarnException;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ExceptionType;
import de.dataport.suchdienst.importer.model.XZuFiVersion;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequest;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequestInfo;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequestStatus;
import de.dataport.suchdienst.importer.model.warteschlange.QueueableImportRequest;
import de.dataport.suchdienst.importer.model.warteschlange.TransactionMetadata;
import de.dataport.suchdienst.indexer.model.measurement.ImportMeasurement;
import de.dataport.suchdienst.indexer.model.measurement.ImportStatistic;
import de.dataport.suchdienst.indexer.service.DeleteRememberService;
import de.dataport.suchdienst.indexer.service.XZufiImportService;
import de.dataport.suchdienst.suchen.exception.SuchenException;
import de.dataport.suchdienst.taskmgr.model.ImportTask;
import de.dataport.suchdienst.taskmgr.model.SudTask;
import de.dataport.suchdienst.taskmgr.model.SudTaskMediator;
import de.dataport.suchdienst.taskmgr.model.SudTaskType;
import de.dataport.suchdienst.taskmgr.service.SudTaskMgr;
import de.dataport.suchdienst.util.MdcTemplate;
import de.dataport.suchdienst.util.ResultPage;
import de.dataport.suchdienst.util.model.MdcKeys;
import de.dataport.suchdienst.util.model.ProtocolType;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class QueueImportService implements SudTaskMediator<Void> {

    private final MessageSource messageSource;
    private final SuchdienstProperties properties;
    private final QueuePersistence queuePersistence;
    private final XZufiImportService importService;
    private final ElasticReadConnector readConnector;
    private final DeleteRememberService deleteRememberService;

    // Hier sind die ImportRequests (allerdings ohne Xzufi-Daten) gecacht. Die Verarbeitung lädt die Xzufi-Daten dann.
    private final LinkedBlockingQueue<ImportRequestInfo> queue;

    private final TaskScheduler scheduler;

    private boolean shutdownRequested;

    private final ImportStatistic importStatistic;

    private final SudTaskMgr sudTaskMgr;

    public QueueImportService(
        SuchdienstProperties properties,
        QueuePersistence queuePersistence,
        XZufiImportService importService,
        @Qualifier("singleTaskExecutor") TaskScheduler scheduler,
        MessageSource messageSource,
        ElasticReadConnector readConnector,
        DeleteRememberService deleteRememberService,
        SudTaskMgr sudTaskMgr
    ) {
        this.properties = properties;
        this.queuePersistence = queuePersistence;
        this.importService = importService;
        this.scheduler = scheduler;
        this.messageSource = messageSource;
        this.readConnector = readConnector;
        this.deleteRememberService = deleteRememberService;
        this.sudTaskMgr = sudTaskMgr;
        this.queue = new LinkedBlockingQueue<>();
        this.importStatistic = new ImportStatistic();
    }

    /**
     * Befüllt die ImportQueue aus der DB und startet deren Abarbeitung.
     * Wird vom SuchdienstInitializer zu einer geeigneten Zeit aufgerufen.
     */
    public void afterStartup() {
        if (InstanceType.MASTER.equals(properties.getInstanceType())) {
            log.info("Befülle Import-Queue nach Applikationsstart");
            refillQueue();

            log.info("Starte Import-Scheduler, um Queue abzuarbeiten");
            // Worker-Job für den Import der Nachrichten aus der Queue:
            scheduler.scheduleWithFixedDelay(
                this::processQueue,
                properties.getImporter().getDelayBetweenQueuePolls()
            );
            // Expire-Job
            scheduler.scheduleAtFixedRate(
                () -> {
                    queuePersistence.invalidate();
                    deleteRememberService.removeExpiredDeletes();
                },
                Instant.now().plus(properties.getImporter().getInitialInvalidationDelay()),
                properties.getImporter().getDelayBetweenQueueInvalidations()
            );
        }
    }

    public void refillQueue() {
        if (queue.isEmpty()) {
            try {
                List<QueueableImportRequest> persistedRequests = queuePersistence.readAllPending();
                queue.addAll(persistedRequests);
            } catch (IllegalStateException exception) {
                // PV-8946 catch-log-skip
                log.warn(
                    "Beim Wiederbefüllen der Queue werden mehr Elemente geschrieben als diese fassen kann",
                    exception
                );
            } catch (ElasticSearchException e) {
                log.warn(
                    "Beim Wiederbefüllen der Queue trat ein Fehler beim Zugriff auf Elasticsearch auf",
                    e
                );
            }
        }
    }

    /**
     * Schreibt ein Xzufi in die ImportQueue.
     *
     * @return Die Id des geschriebenen ES-Dokuments.
     */
    public String enqueueImportRequest(
        TransactionMetadata metadata,
        byte[] xzufiToImport,
        XZuFiVersion xzufiVersion
    ) {
        ImportRequest request = ImportRequest.createOpen(metadata, xzufiToImport, xzufiVersion);

        try {
            return queuePersistence
                .write(request)
                .map(id -> {
                    log.debug("Import in die persistente Queue aufgenommen [{}]", id);
                    return id;
                })
                .orElseThrow(this::apiWarnAboutEnqueueImportRequestFailure);
        } catch (Exception exception) {
            // PV-8946 catch-log-rethrow
            log.warn("Import kann nicht in die Queue aufgenommen werden", exception);
            throw apiWarnAboutEnqueueImportRequestFailure();
        }
    }

    private ApiWarnException apiWarnAboutEnqueueImportRequestFailure() {
        return new ApiWarnException(
            ErrorCodeEnum.IMPORT_QUEUE,
            messageSource.getMessage(
                "push_import_resource.exception_other_error",
                null,
                Locale.GERMAN
            ),
            ExceptionType.TECHNISCH
        );
    }

    @SuppressWarnings("java:S107") // Methode ist auch so verständlich und Refactoring wäre hier zu aufwendig
    public ResultPage<QueueableImportRequest> getQueueStatus(
        int page,
        int pageSize,
        String id,
        String messageId,
        String transactionId,
        LocalDateTime acceptedAfter,
        LocalDateTime acceptedBefore,
        ImportRequestStatus status
    ) {
        Set<ImportRequestStatus> statuses = status != null ? singleton(status) : emptySet();
        return queuePersistence.readAll(
            page,
            pageSize,
            id,
            messageId,
            transactionId,
            acceptedAfter,
            acceptedBefore,
            statuses
        );
    }

    public Optional<ImportRequest> getDetails(String id) {
        return queuePersistence.read(id);
    }

    public Optional<Boolean> exists(String id) {
        return queuePersistence.exists(id);
    }

    public boolean deleteFromQueue(String id) {
        return queuePersistence.remove(id);
    }

    public boolean changeStatus(String id, ImportRequestStatus status) {
        return queuePersistence.changeStatus(id, status, false);
    }

    @PreDestroy
    private void onShutdown() {
        log.info("Shutdown wurde initiiert.");
        shutdownRequested = true;
    }

    void processQueue() {
        refillQueue();
        for (ImportRequestInfo request = queue.poll(); request != null; request = queue.poll()) {
            if (shutdownRequested) {
                log.info("Shutdown: Die weitere Verarbeitung der Queue wird abgebrochen");
                break;
            }
            try {
                submitImportTask(request);
            } catch (Exception e) {
                var logMsg = String.format(
                    "Die Verarbeitung der Nachricht '%s' ist gescheitert: %s. " +
                        "Die Nachricht wird ignoriert, der Status auf %s gesetzt, und die Verarbeitung der Queue wird fortgesetzt.",
                    request.getMetaData(),
                    e.getMessage(),
                    ImportRequestStatus.ERROR
                );
                log.warn(logMsg, e);
                changeStatus(request.getMetaData().id(), ImportRequestStatus.ERROR);
            }
        }
        sudTaskMgr.waitForTasks(task -> task.getTaskType().equals(SudTaskType.IMPORT));
    }

    private void submitImportTask(ImportRequestInfo request) {
        var metaData = request.getMetaData();
        sudTaskMgr.submit(
            new ImportTask(metaData.schemeAgencyId(), metaData.producer(), metaData.id())
        );
    }

    private void processImportTask(ImportTask task) {
        var requestId = task.getRequestId();
        var optImportRequest = queuePersistence.read(requestId);
        if (optImportRequest.isEmpty()) {
            MdcTemplate.runWith(
                MdcKeys.ERROR_CODE,
                String.valueOf(ErrorCodeEnum.IMPORT_QUEUE.getNumber()),
                () ->
                    log.error(
                        "IMPORT - FEHLER: Konnte Queue-Eintrag mit der ID {} nicht finden.",
                        requestId
                    )
            );
        }
        assert optImportRequest.isPresent();
        var importRequest = optImportRequest.get();
        final TransactionMetadata metaData = importRequest.getMetaData();
        var mdcMap = Map.of(
            MdcKeys.TRANSAKTION_ID,
            metaData.transactionId(),
            MdcKeys.PROTOKOLLTYP,
            ProtocolType.IMPORT.name(),
            MdcKeys.NACHRICHT_UUID,
            metaData.id()
        );
        MdcTemplate.runWith(mdcMap, runImportTask(requestId, importRequest));
    }

    private Runnable runImportTask(String requestId, ImportRequest importRequest) {
        TransactionMetadata metaData = importRequest.getMetaData();
        return () -> {
            importStatistic.logImportStatisticStart(metaData);

            log.info(
                "Import gestartet, RequestId: {}, Metadaten: {}, Date: {}",
                requestId,
                metaData,
                importRequest.getRequestAcceptedAt()
            );
            ImportMeasurement stat = importService.importXZufiFile(
                importRequest,
                new DateImportChecker(importRequest, readConnector)
            );
            if (stat.isSuccess()) {
                log.info("Import erfolgreich: {} ", stat);
                if (!queuePersistence.changeStatus(requestId, ImportRequestStatus.DONE, true)) {
                    log.warn(
                        "Import erfolgreich; jedoch ist der Versuch fehlgeschlagen den persistierten Import als Done zu markieren [{}]",
                        requestId
                    );
                }
            } else {
                log.error("Import fehlgeschlagen [{}]", requestId);
                queuePersistence.changeStatus(requestId, ImportRequestStatus.ERROR, true);
            }
            importStatistic.logImportStatisticStop(metaData);
        };
    }

    public void reopenImportRequests(Set<String> requestIds) {
        Set<String> conflictIds = new HashSet<>();
        Set<String> notFoundIds = new HashSet<>();
        for (String requestId : requestIds) {
            queuePersistence.read(requestId).ifPresentOrElse(
                req -> {
                    var reqStatus = req.getStatus();
                    if (reqStatus != ImportRequestStatus.ERROR) {
                        conflictIds.add(requestId);
                    }
                },
                () -> notFoundIds.add(requestId)
            );
        }
        if (!conflictIds.isEmpty()) {
            throw new ApiWarnException(
                ErrorCodeEnum.CONFLICT,
                "Es dürfen nur Warteschlangenelemente mit dem Status Error erneut geöffnet werden. Folgende Elemente hatten nicht den Status Error: " +
                    String.join(",", conflictIds) +
                    ", es wird kein Element wiedereröffnet.",
                ExceptionType.FACHLICH
            );
        } else if (!notFoundIds.isEmpty()) {
            throw new SuchenException(
                ErrorCodeEnum.NOT_FOUND,
                "Die Elemente mit den IDs " +
                    String.join(",", notFoundIds) +
                    " konnten nicht gefunden werden, es wird kein Element wiedereröffnet."
            );
        } else {
            requestIds.forEach(requestId ->
                queuePersistence.changeStatus(requestId, ImportRequestStatus.REOPENED, false)
            );
        }
    }

    @Override
    public Supplier<Void> mediate(SudTask<Void> task) {
        if (task instanceof ImportTask importTask) {
            return () -> {
                processImportTask(importTask);
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
