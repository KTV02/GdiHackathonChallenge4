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
package de.dataport.suchdienst;

import static de.dataport.suchdienst.elasticsearch.api.IndexNames.ARS;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import de.dataport.suchdienst.config.InstanceType;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticWriteConnector;
import de.dataport.suchdienst.elasticsearch.api.exception.ElasticSearchException;
import de.dataport.suchdienst.elasticsearch.evolution.IndexUpdater;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.importer.service.QueueImportService;
import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.model.RegionalKeyKind;
import de.dataport.suchdienst.indexer.model.ZipcodeCsvDefinition;
import de.dataport.suchdienst.indexer.service.ArsService;
import de.dataport.suchdienst.indexer.service.ArsTreeService;
import de.dataport.suchdienst.indexer.service.CodelistService;
import de.dataport.suchdienst.indexer.service.ElasticIndexService;
import de.dataport.suchdienst.indexer.service.ImportSchedulingService;
import de.dataport.suchdienst.indexer.service.LeikaService;
import de.dataport.suchdienst.indexer.service.VerwaltungsleistungImportService;
import de.dataport.suchdienst.indexer.service.odrelationimport.OdRelationImportService;
import de.dataport.suchdienst.suchen.exception.SuchenException;
import de.dataport.suchdienst.taskmgr.model.SudTaskMediator;
import de.dataport.suchdienst.taskmgr.service.SudTaskMgr;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Initialisiert ElasticSearch, legt also insbesondere die notwendigen Indexstrukturen an.
 * Vergleichbar mit dem, was Liquibase tut.
 */
@Slf4j
@Component
public class SuchdienstInitializer implements ApplicationContextAware {

    private final MessageSource messageSource;

    private final ElasticWriteConnector elasticWriteConnector;
    private final ElasticReadConnector readConnector;
    private final SuchdienstProperties properties;
    private final ArsService arsService;
    private final IndexUpdater indexUpdater;
    private final CodelistService codeListService;
    private final LeikaService leikaService;
    private final ApplicationEventPublisher eventPublisher;
    private final QueueImportService queueImportService;

    private final ImportSchedulingService importSchedulingService;
    private final ElasticIndexService elasticIndexService;
    private final SudTaskMgr sudTaskMgr;
    private final VerwaltungsleistungImportService verwaltungsleistungImportService;
    private final OdRelationImportService odRelationImportService;
    private final ArsTreeService arsTreeService;

    private ApplicationContext applicationContext;

    public SuchdienstInitializer(
        ElasticWriteConnector elasticWriteConnector,
        ElasticReadConnector readConnector,
        SuchdienstProperties properties,
        ArsService arsService,
        IndexUpdater indexUpdater,
        CodelistService codeListService,
        MessageSource messageSource,
        LeikaService leikaService,
        ApplicationEventPublisher eventPublisher,
        QueueImportService queueImportService,
        ImportSchedulingService importSchedulingService,
        ElasticIndexService elasticIndexService,
        SudTaskMgr sudTaskMgr,
        VerwaltungsleistungImportService verwaltungsleistungImportService,
        OdRelationImportService odRelationImportService,
        ArsTreeService arsTreeService
    ) {
        this.elasticWriteConnector = elasticWriteConnector;
        this.readConnector = readConnector;
        this.properties = properties;
        this.arsService = arsService;
        this.indexUpdater = indexUpdater;
        this.codeListService = codeListService;
        this.messageSource = messageSource;
        this.leikaService = leikaService;
        this.eventPublisher = eventPublisher;
        this.queueImportService = queueImportService;
        this.importSchedulingService = importSchedulingService;
        this.elasticIndexService = elasticIndexService;
        this.sudTaskMgr = sudTaskMgr;
        this.verwaltungsleistungImportService = verwaltungsleistungImportService;
        this.odRelationImportService = odRelationImportService;
        this.arsTreeService = arsTreeService;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @EventListener
    public void onApplicationEvent(ApplicationStartedEvent event) {
        AvailabilityChangeEvent.publish(this.eventPublisher, this, LivenessState.CORRECT);
        AvailabilityChangeEvent.publish(this.eventPublisher, this, ReadinessState.REFUSING_TRAFFIC);
        log.info(
            "Suchdienst ist gestartet, liveness: {}, readiness: {}",
            LivenessState.CORRECT,
            ReadinessState.REFUSING_TRAFFIC
        );
    }

    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            log.info("Suchdienst führt Initialisierungen durch ...");
            afterStartup();
            AvailabilityChangeEvent.publish(
                this.eventPublisher,
                this,
                ReadinessState.ACCEPTING_TRAFFIC
            );
            log.info("Initialisierungen beendet, readiness: {}", ReadinessState.ACCEPTING_TRAFFIC);
        } catch (InterruptedException e) {
            // PV-8946 catch-log-rethrow
            Thread.currentThread().interrupt();
            log.error("Fehler beim Starten der Anwendung.", e);
            System.exit(SpringApplication.exit(applicationContext));
        } catch (Exception e) {
            // PV-8946 catch-log-rethrow
            log.error("Fehler beim Starten der Anwendung.", e);
            System.exit(SpringApplication.exit(applicationContext));
        }
    }

    /**
     * Führt diverse Initialisierungen durch, die für den SuD notwendig sind. Hier sollten weitere Initialisierungen - wenn nötig - hinzugefügt
     * werden, dies ist zu bevorzugen vor einem verteilten ApplicationListener Mechanismus, da es hier transparenter ist und auch über die
     * Reihenfolge eine Kontrolle ausgeübt werden kann.
     * Erst nach diesen Initialisierungen ist der SuD "ready", kann also Anfragen annehmen.
     *
     * @throws IOException          Fehler bei Else Operation
     * @throws InterruptedException Fehler bei Warten auf Else
     */
    private void afterStartup() throws IOException, InterruptedException {
        if (properties.getInstanceType() == null) {
            throw new SuchenException(
                ErrorCodeEnum.NO_INSTANCETYPE,
                messageSource.getMessage(
                    "suchdienst_initializer.exception_no_instance_type_error",
                    null,
                    Locale.GERMAN
                )
            );
        }
        waitForElasticSearch();
        if (properties.getInstanceType() == InstanceType.MASTER) {
            createIndexTemplates();
        }

        // Anmelden der "Algorithmen" für die Ausführung von SudTasks
        List<SudTaskMediator<?>> mediators = Arrays.asList(
            verwaltungsleistungImportService,
            queueImportService,
            odRelationImportService
        );
        sudTaskMgr.setMediators(mediators);

        indexUpdater.updateElasticSearch();
        if (properties.getInstanceType() == InstanceType.MASTER) {
            importArsList();
            initializeArsTree();
            importStandardCodelists();
            leikaService.intializeLeistungskatalog();
            queueImportService.afterStartup();
            verwaltungsleistungImportService.afterStartup();
            importSchedulingService.schedulePeriodicImport();
            elasticIndexService.cleanRemainingIndices();
            elasticIndexService.checkAndRepairIndicesAliasConfig();
        }
        leikaService.updateLocalPvLagenCache();
    }

    private void initializeArsTree() {
        arsTreeService.refreshArsTree();
        arsTreeService.schedulePeriodicArsRefresh();
    }

    private void importStandardCodelists() {
        try {
            Resource[] codeLists = new PathMatchingResourcePatternResolver().getResources(
                "classpath:/codelisten/*.xml"
            );
            List<CodelistEntry> allEntries = Arrays.stream(codeLists)
                .map(codeListService::parseCodelistDocument)
                .filter(ObjectUtils::isNotEmpty)
                // Filtert Codelisten heraus, welche bereits gespeichert wurden,
                // so bleiben manuelle Updates über das Backoffice erhalten und werden nicht überschrieben
                .filter(
                    list ->
                        !readConnector.codeListExists(
                            list.getFirst().getUri(),
                            list.getFirst().getVersion()
                        )
                )
                .flatMap(Collection::stream)
                .toList();
            codeListService.saveCodelistInElasticSearch(allEntries);
            log.info("{} Standard-Codelisteinträge importiert.", allEntries.size());
        } catch (IOException | UncheckedIOException e) {
            // PV-8946 catch-log-skip
            log.warn("Kann Standard-Codelisten nicht laden: {}", e.getMessage(), e);
        }
    }

    private void createIndexTemplates() throws IOException {
        if (!readConnector.templateExists("suchdiensttemplate")) {
            elasticWriteConnector.createIndexTemplate(
                "suchdiensttemplate",
                0L,
                1,
                1,
                "1s",
                List.of("suchdienst_*")
            );
        }
    }

    private void importArsList() throws IOException {
        if (readConnector.isIndexEmpty(ARS)) {
            log.info("ARS & PLZ Liste importieren");
            try (InputStream stream = getClass().getResourceAsStream("/ars/GV100AD_31072026.txt")) {
                arsService.importArsList(stream, StandardCharsets.UTF_8);
            }
            log.info("ARS Liste fertig importiert");
            try (
                var gzis = new GZIPInputStream(
                    getClass().getResourceAsStream("/ars/plz-nach-hk-de-2026.csv.gz")
                );
                var isr = new InputStreamReader(gzis, StandardCharsets.UTF_8);
                var br = new BufferedReader(isr)
            ) {
                Stream<String> lines = br.lines();
                var zipDef = ZipcodeCsvDefinition.builder()
                    .delimiter(";")
                    .headerLine(false)
                    .rsColumn(0)
                    .zipColumn(1)
                    .rsKind(RegionalKeyKind.AGS)
                    .build();
                arsService.importPostcodes(lines, zipDef);
            }
            log.info("PLZ Liste fertig importiert");
        }
    }

    /*
     * Der Thread.sleep() in der Schleife ist hier bewusst so implementiert (Poll-Intervall
     * gegen einen externen Dienst mit konfigurierbarer Obergrenze) und kein "busy-waiting".
     */
    @SuppressWarnings("BusyWait")
    private void waitForElasticSearch() throws InterruptedException {
        int tries = 0;
        while (true) {
            try {
                if (elasticWriteConnector.ping()) {
                    break;
                }
            } catch (IOException | ElasticsearchException e) {
                // PV-8946 catch-log-skip
                // Warten
                log.warn("Warten auf ElasticSearch...", e);
            }
            if (tries >= properties.getMaxRetriesWaitingForElasticSearch()) {
                throw new ElasticSearchException(
                    ErrorCodeEnum.ELSE_IO_ERROR,
                    messageSource.getMessage(
                        "suchdienst_initializer.exception_else_io_error",
                        new String[] {
                            Integer.toString(
                                properties.getWaitIntervallElasticSearch() *
                                    properties.getMaxRetriesWaitingForElasticSearch()
                            ),
                        },
                        Locale.GERMAN
                    )
                );
            }
            tries++;
            log.info("Warte auf ElasticSearch...");
            Thread.sleep(properties.getWaitIntervallElasticSearch() * 1000L);
        }
    }
}
