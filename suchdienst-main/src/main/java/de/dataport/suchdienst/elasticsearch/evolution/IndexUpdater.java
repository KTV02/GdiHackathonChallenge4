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
package de.dataport.suchdienst.elasticsearch.evolution;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import de.dataport.suchdienst.config.InstanceType;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticWriteConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexConfiguration;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.elasticsearch.model.IndexUpdateStatus;
import de.dataport.suchdienst.elasticsearch.model.UpdateDescription;
import de.dataport.suchdienst.elasticsearch.model.UpdateStatus;
import de.dataport.suchdienst.indexer.exception.IndexUpdateException;
import de.dataport.suchdienst.indexer.service.ImportSchedulingService;
import de.dataport.suchdienst.taskmgr.model.SudTaskType;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * triggered on application start , updating the elasticserach index
 */
@Slf4j
@Component
public class IndexUpdater {

    private final MessageSource messageSource;
    private final ElasticsearchClient client;
    private final ElasticWriteConnector elasticWriteConnector;
    private final ElasticReadConnector readConnector;
    private final UpdateConfiguration updateConfiguration;
    private final IndexConfiguration indexConfiguration;
    private final SuchdienstProperties properties;
    private final ImportSchedulingService importSchedulingService;

    public IndexUpdater(
        ElasticsearchClient client,
        ElasticReadConnector readConnector,
        UpdateConfiguration updateConfiguration,
        IndexConfiguration indexConfiguration,
        SuchdienstProperties properties,
        MessageSource messageSource,
        ElasticWriteConnector elasticWriteConnector,
        ImportSchedulingService importSchedulingService
    ) {
        this.client = client;
        this.readConnector = readConnector;
        this.updateConfiguration = updateConfiguration;
        this.indexConfiguration = indexConfiguration;
        this.properties = properties;
        this.messageSource = messageSource;
        this.elasticWriteConnector = elasticWriteConnector;
        this.importSchedulingService = importSchedulingService;
    }

    /**
     * Prüft ob alle Indexupdates durchgeführt wurden.
     */
    private void checkIndexUpdates() {
        updateConfiguration.getDefaultUpdateDescriptions().forEach(update -> {
            HitsMetadata<UpdateDescription> hits = readConnector.searchUpdateHistory(
                Optional.of(update.getVersion()),
                update.getAbstractIndexName(),
                UpdateStatus.CREATED
            );
            if (hits == null || hits.total() == null || hits.total().value() != 1) {
                throw new IndexUpdateException(
                    messageSource.getMessage(
                        "index_updater.exception_update_not_processed",
                        new String[] {
                            String.valueOf(update.getVersion()),
                            update.getAbstractIndexName().getIndexName(),
                            update.getDescription(),
                        },
                        Locale.GERMAN
                    )
                );
            }
        });
    }

    /**
     * Führt alle Updates für die Indizes durch. Z. B. neue Felder hinzufügen oder Reindizierungen
     */
    public void updateElasticSearch() {
        indexConfiguration.setCurrentIndexName(
            IndexNames.UPDATEHISTORY,
            IndexNames.UPDATEHISTORY.getIndexName()
        );
        updateIndexNames();
        if (properties.getInstanceType() == InstanceType.SLAVE) {
            checkIndexUpdates();
            return;
        }
        executePreChecks();
        createManagementIndex();
        long verwaltungsIndicesBeforeCreation = countCreatedIndexVersions(
            IndexNames.VERWALTUNGSLEISTUNGEN_SH
        );
        long onlineRelationIndicesBeforeCreation = countCreatedIndexVersions(
            IndexNames.ONLINEDIENSTE_RELATIONEN
        );
        createDefaultIndexes();
        long verwaltungsIndicesAfterCreation = countCreatedIndexVersions(
            IndexNames.VERWALTUNGSLEISTUNGEN_SH
        );
        long onlineRelationIndicesAfterCreation = countCreatedIndexVersions(
            IndexNames.ONLINEDIENSTE_RELATIONEN
        );
        executeMigrations();
        if (
            verwaltungsIndicesBeforeCreation != 0 &&
            verwaltungsIndicesBeforeCreation < verwaltungsIndicesAfterCreation
        ) {
            log.info("Führe Neuberechnung der Verwaltungsindizes beim Start durch.");
            importSchedulingService.importAllIndexesAsync();
            importSchedulingService.waitForTasks(SudTaskType.VL_RECALC);
        }
        if (
            onlineRelationIndicesBeforeCreation != 0 &&
            onlineRelationIndicesBeforeCreation < onlineRelationIndicesAfterCreation
        ) {
            log.info("Führe Neuberechnung der Onlinedienst-Relationen beim Start durch.");
            importSchedulingService.recalcAllOdRelIndexesAndWait();
        }
        executeDeletions();
    }

    /**
     * Zählt wie häufig die Indizes für einen übergebenen Index durch Indexmigrationen erstellt wurden.
     * Beim Verwaltungsleistungs-Index reicht es nur einen der 16 Indizes zu prüfen, da immer alle zusammen erstellt werden.
     */
    private long countCreatedIndexVersions(IndexNames index) {
        HitsMetadata<UpdateDescription> hits = readConnector.searchUpdateHistory(
            Optional.empty(),
            index,
            UpdateStatus.CREATED
        );
        return hits.total() == null ? 0 : hits.total().value();
    }

    private void createManagementIndex() {
        if (!readConnector.indexExists(IndexNames.UPDATEHISTORY.getIndexName())) {
            try {
                String mapping = loadIndexJson("indexmappings/mapping_updatehistory_1.json");
                createIndexStructure(
                    elasticWriteConnector,
                    mapping,
                    null,
                    IndexNames.UPDATEHISTORY.getIndexName()
                );
                log.info("Updatehistory Index wurde erstellt.");
            } catch (Exception e) {
                // PV-8946 catch-log-rethrow
                log.error("Fehler beim Erstellen des Updatehistoryindex.", e);
                throw new IndexUpdateException("Fehler beim Erstellen des Updatehistoryindex.");
            }
        }
    }

    private void createDefaultIndexes() {
        for (var description : updateConfiguration.getDefaultUpdateDescriptions()) {
            HitsMetadata<UpdateDescription> hits = readConnector.searchUpdateHistory(
                Optional.of(description.getVersion()),
                description.getAbstractIndexName(),
                UpdateStatus.CREATED
            );
            if (hits.total() != null && hits.total().value() == 1) {
                log.info(
                    "Update auf die Version {} im Index {} wurde bereits durchgeführt.",
                    description.getVersion(),
                    description.getAbstractIndexName().getIndexName()
                );
            } else {
                UpdateDescription updateDescriptionObject = prepareUpdateDescription(description);
                if (description.pathToMappingFile() != null) {
                    createIndex(description, updateDescriptionObject);
                    if (description.getAlias() != null) {
                        createAlias(description);
                    }
                }
                IndexUpdateStatus status = new IndexUpdateStatus(
                    "Der Index " + description.getDestIndex() + " wurde erstellt.",
                    UpdateStatus.CREATED
                );
                finishUpdate(status, updateDescriptionObject, description);
            }
        }
        updateIndexNames();
    }

    private void executeDeletions() {
        for (var description : updateConfiguration.getDeleteDescriptions()) {
            HitsMetadata<UpdateDescription> hits = readConnector.searchUpdateHistory(
                Optional.of(description.getVersion()),
                description.getAbstractIndexName(),
                UpdateStatus.DELETED
            );
            if (hits.total() != null && hits.total().value() == 1) {
                log.info("Der Index {} wurde bereits geloescht.", description.getDestIndex());
            } else {
                UpdateDescription updateDescriptionObject = prepareUpdateDescription(description);
                log.info("Loesche den Index {}", description.getDestIndex());
                elasticWriteConnector.index(
                    updateDescriptionObject,
                    IndexNames.UPDATEHISTORY,
                    null
                );
                IndexUpdateStatus status = deleteIndexByDescription(description);
                finishUpdate(status, updateDescriptionObject, description);
            }
        }
    }

    private void executeMigrations() {
        for (var description : updateConfiguration.getMigrationDescriptions()) {
            HitsMetadata<UpdateDescription> hits = readConnector.searchUpdateHistory(
                Optional.of(description.getVersion()),
                description.getAbstractIndexName(),
                UpdateStatus.FINISHED
            );
            if (hits.total() != null && hits.total().value() == 1) {
                log.info(
                    "Update auf die Version {} im Index {} wurde bereits durchgeführt.",
                    description.getVersion(),
                    description.getAbstractIndexName().getIndexName()
                );
            } else {
                int requiredVersion = getMaxExecutedVersion(description);
                if (description.getRequiredPreVersion() == requiredVersion) {
                    migrate(description);
                }
            }
        }
    }

    /**
     * Prüft ob zu viele fehlerhafte Indexupdateversuche durchgeführt wurden und ob keine Doppelten
     * Indexdescriptions vorhanden sind.
     */
    private void executePreChecks() {
        List<String> versions = updateConfiguration
            .getDefaultUpdateDescriptions()
            .stream()
            .map(d -> String.valueOf(d.getVersion()) + d.getAbstractIndexName())
            .toList();
        long countVersions = versions.stream().distinct().count();

        if (versions.size() > countVersions) {
            log.error("Indexupdate fehlgeschlagen da doppelte neue Versionen vorhanden sind.");
            throw new IndexUpdateException(
                messageSource.getMessage(
                    "index_updater.exception_double_version_error",
                    null,
                    Locale.GERMAN
                )
            );
        }

        HitsMetadata<UpdateDescription> entriesFromUpdateHistory =
            readConnector.getLastEntriesFromUpdateHistory(
                Instant.now(),
                properties.getEvolutionRetriesTimeRange()
            );
        if (
            entriesFromUpdateHistory.total() != null &&
            entriesFromUpdateHistory.total().value() > properties.getEvolutionRetries()
        ) {
            throw new IndexUpdateException(
                messageSource.getMessage(
                    "index_updater.exception_to_many_flawed_update_tries",
                    null,
                    Locale.GERMAN
                )
            );
        }
    }

    /**
     * Gibt zu einem Index die vorletzte durchgeführte Versionsnummer zurück da die letzte Versionsnummer dem Erstellen des Indexes gehört
     */
    private int getMaxExecutedVersion(IndexUpdateDescription description) {
        HitsMetadata<UpdateDescription> executedUpdates = readConnector.searchUpdateHistory(
            Optional.empty(),
            description.getAbstractIndexName(),
            UpdateStatus.CREATED
        );
        if (executedUpdates.hits().size() > 1) {
            return executedUpdates
                .hits()
                .get(executedUpdates.hits().size() - 2)
                .source()
                .getVersion();
        } else {
            return -1;
        }
    }

    private void updateIndexNames() {
        if (
            readConnector.indexExists(
                indexConfiguration.getCurrentIndexName(IndexNames.UPDATEHISTORY)
            )
        ) {
            updateConfiguration
                .getDefaultUpdateDescriptions()
                .stream()
                .filter(d -> d.getDestIndex() != null)
                .forEach(d -> {
                    HitsMetadata<UpdateDescription> hits = readConnector.searchUpdateHistory(
                        Optional.of(d.getVersion()),
                        d.getAbstractIndexName(),
                        UpdateStatus.CREATED
                    );
                    if (hits != null && hits.total() != null && hits.total().value() > 0) {
                        indexConfiguration.setCurrentIndexName(
                            d.getAbstractIndexName(),
                            d.getDestIndex()
                        );
                        if (d.getAbstractAliasName() != null) {
                            indexConfiguration.setCurrentAliasName(
                                d.getAbstractAliasName(),
                                d.getAlias()
                            );
                        }
                    }
                });
        }
    }

    private void migrate(IndexMigrationDescription description) {
        UpdateDescription updateDescriptionObject = prepareUpdateDescription(description);

        // Start speichern
        elasticWriteConnector.index(updateDescriptionObject, IndexNames.UPDATEHISTORY, null);

        log.info(
            "Führe Indexupdate durch. Von Index {} in Index {}. Von Version {} zu Version {}, Beschreibung: {}",
            description.getSourceIndex(),
            description.getDestIndex(),
            description.getRequiredPreVersion(),
            description.getVersion(),
            description.getDescription()
        );

        checkIfSourceIndexExist(description, updateDescriptionObject);

        IndexUpdateStatus status = reindexData(description);
        finishUpdate(status, updateDescriptionObject, description);
    }

    private void finishUpdate(
        IndexUpdateStatus status,
        UpdateDescription updateDescriptionObject,
        IndexUpdateDescription description
    ) {
        updateDescriptionObject.setTimestamp(Instant.now());
        updateDescriptionObject.setStatus(status.getStatus());
        if (status.getStatus() == UpdateStatus.ERROR) {
            elasticWriteConnector.index(updateDescriptionObject, IndexNames.UPDATEHISTORY, null);
            log.error("Beim Indexupdate ist ein Fehler aufgetreten: {}", status.getMessage());
            throw new IndexUpdateException(
                messageSource.getMessage(
                    "index_updater.exception_index_update_error",
                    new String[] { status.getMessage() },
                    Locale.GERMAN
                )
            );
        } else if (status.getStatus() == UpdateStatus.WARN) {
            log.warn("Beim Indexupdate ist ein Fehler aufgetreten, es wird trotzdem fortgesetzt.");
        } else if (status.getStatus() == UpdateStatus.DELETED) {
            log.info("Index {} wurde erfolgreich geloescht.", description.getDestIndex());
        } else if (status.getStatus() == UpdateStatus.CREATED) {
            log.info(
                "Der Index {} wurde erstellt.",
                indexConfiguration.getCurrentIndexName(description.getAbstractIndexName())
            );
        } else if (status.getStatus() == UpdateStatus.FINISHED) {
            log.info(
                "Index {} wurde erfolgreich in den Index {} migriert.",
                description.getAbstractIndexName(),
                description.getDestIndex()
            );
        }

        // Ende speichern
        updateDescriptionObject.setDescription(description.getDescription());
        updateDescriptionObject.setVersion(description.getVersion());
        elasticWriteConnector.index(updateDescriptionObject, IndexNames.UPDATEHISTORY, null);
    }

    private void checkIfSourceIndexExist(
        IndexMigrationDescription description,
        UpdateDescription updateDescriptionObject
    ) {
        if (StringUtils.isBlank(description.getSourceIndex())) {
            throw new IndexUpdateException(
                "Im Migrationsschritt mit der Zielversion " +
                    description.getVersion() +
                    ", der erforderlichen Version " +
                    description.getRequiredPreVersion() +
                    " und der Beschreibung " +
                    description.getDescription() +
                    " wurde kein Quellindex angegeben."
            );
        }
        // Prüfen ob der Quellindex existiert
        if (!readConnector.indexExists(description.getSourceIndex())) {
            updateDescriptionObject.setTimestamp(Instant.now());
            updateDescriptionObject.setStatus(UpdateStatus.ERROR);
            elasticWriteConnector.index(updateDescriptionObject, IndexNames.UPDATEHISTORY, null);
            throw new IndexUpdateException(
                messageSource.getMessage(
                    "index_updater.exception_index_not_exist_error",
                    new String[] { description.getSourceIndex() },
                    Locale.GERMAN
                )
            );
        }
    }

    private UpdateDescription prepareUpdateDescription(IndexUpdateDescription description) {
        UpdateDescription updateDescriptionObject = new UpdateDescription();
        updateDescriptionObject.setDescription(description.getDescription());
        updateDescriptionObject.setVersion(description.getVersion());
        updateDescriptionObject.setTimestamp(Instant.now());
        updateDescriptionObject.setIndexName(description.getAbstractIndexName().getIndexName());
        updateDescriptionObject.setStatus(UpdateStatus.STARTED);

        return updateDescriptionObject;
    }

    private void createAlias(DefaultDescription description) {
        indexConfiguration.setCurrentAliasName(
            description.getAbstractAliasName(),
            description.getAlias()
        );
        elasticWriteConnector.createIndexAlias(
            description.getDestIndex(),
            indexConfiguration.getCurrentReadAliasName(description.getAbstractAliasName()),
            false
        );
        elasticWriteConnector.createIndexAlias(
            description.getDestIndex(),
            indexConfiguration.getCurrentWriteAliasName(description.getAbstractAliasName()),
            true
        );
    }

    private void createIndex(
        DefaultDescription description,
        UpdateDescription updateDescriptionObject
    ) {
        // Prüfen ob der Zielindex nicht schon besteht
        if (readConnector.indexExists(description.getDestIndex())) {
            updateDescriptionObject.setTimestamp(Instant.now());
            updateDescriptionObject.setStatus(UpdateStatus.ERROR);
            elasticWriteConnector.index(updateDescriptionObject, IndexNames.UPDATEHISTORY, null);
            log.warn(
                "Der Zielindex {} existiert bereits. Er wird gelöscht und neu erstellt.",
                description.getDestIndex()
            );
            try {
                elasticWriteConnector.deleteIndex(description.getDestIndex());
            } catch (Exception e) {
                // PV-8946 catch-log-rethrow
                log.error("Fehler beim Löschen des Index {}.", description.getDestIndex(), e);
                throw new IndexUpdateException(
                    messageSource.getMessage(
                        "index_updater.exception_create_index_delete_existing_error",
                        new String[] { description.getDestIndex() },
                        Locale.GERMAN
                    )
                );
            }
        }

        StringWriter writer = new StringWriter();
        try (
            InputStream xmlStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream(description.pathToMappingFile())
        ) {
            IOUtils.copy(xmlStream, writer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            // PV-8946 catch-log-rethrow
            log.error(
                "Fehler beim Einlesen der Mapping Struktur {}",
                description.pathToMappingFile(),
                e
            );
            throw new IndexUpdateException(
                messageSource.getMessage(
                    "index_updater.exception_create_index_read_mapping_error",
                    new String[] { description.pathToMappingFile() },
                    Locale.GERMAN
                )
            );
        }
        String mappingStructure = writer.toString();
        String settings = null;

        if (description.pathToSettingsFile() != null) {
            writer = new StringWriter();
            try (
                InputStream xmlStream = this.getClass()
                    .getClassLoader()
                    .getResourceAsStream(description.pathToSettingsFile())
            ) {
                IOUtils.copy(xmlStream, writer, StandardCharsets.UTF_8);
                settings = writer.toString();
            } catch (IOException e) {
                // PV-8946 catch-log-rethrow
                log.error(
                    "Fehler beim Einlesen der Settigns Struktur {}",
                    description.pathToSettingsFile(),
                    e
                );
                throw new IndexUpdateException(
                    messageSource.getMessage(
                        "index_updater.exception_create_index_read_settings_error",
                        new String[] { description.pathToSettingsFile() },
                        Locale.GERMAN
                    )
                );
            }
        }

        try {
            createIndexStructure(
                elasticWriteConnector,
                mappingStructure,
                settings,
                description.getDestIndex()
            );
            indexConfiguration.setCurrentIndexName(
                description.getAbstractIndexName(),
                description.getDestIndex()
            );
        } catch (IOException e) {
            // PV-8946 catch-log-rethrow
            log.error("Fehler beim Erstellen des neuen Index.", e);
            updateDescriptionObject.setTimestamp(Instant.now());
            updateDescriptionObject.setStatus(UpdateStatus.ERROR);
            elasticWriteConnector.index(updateDescriptionObject, IndexNames.UPDATEHISTORY, null);
            throw new IndexUpdateException(
                messageSource.getMessage(
                    "index_updater.exception_create_index_error",
                    null,
                    Locale.GERMAN
                )
            );
        }
    }

    private String loadIndexJson(String pathToFile) {
        StringWriter writer = new StringWriter();
        try (
            InputStream xmlStream = this.getClass().getClassLoader().getResourceAsStream(pathToFile)
        ) {
            IOUtils.copy(xmlStream, writer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            // PV-8946 catch-log-rethrow
            log.error("Fehler beim Einlesen der Mapping Struktur {}", pathToFile, e);
            throw new IndexUpdateException(
                messageSource.getMessage(
                    "index_updater.exception_create_index_read_mapping_error",
                    new String[] { pathToFile },
                    Locale.GERMAN
                )
            );
        }
        return writer.toString();
    }

    private void createIndexStructure(
        ElasticWriteConnector elasticWriteConnector,
        String mapping,
        String settings,
        String indexName
    ) throws IOException {
        elasticWriteConnector.createIndexFromJsonString(mapping, settings, indexName);
    }

    private IndexUpdateStatus reindexData(IndexMigrationDescription description) {
        final var reindexHandler = new ReindexHandler(
            properties,
            client,
            description.getDestIndex()
        );
        final var originalIndexSettings = reindexHandler.getSettings();

        var status = new IndexUpdateStatus(
            "Update erfolgreich durchgeführt",
            UpdateStatus.FINISHED
        );
        try {
            // Optimierung für Reindex-Operationen
            reindexHandler.updateDestIndexSettings(
                IndexSettings.of(b ->
                    b.numberOfReplicas("0").refreshInterval(Time.of(t -> t.time("-1")))
                )
            );
            var taskId = reindexHandler.submitReindexTask(description.getSourceIndex());
            reindexHandler.checkCurrentRunningTasks(taskId);
            reindexHandler.checkFinishedTasks(taskId);
        } catch (Exception e) {
            // PV-8946 catch-log-return-error
            status = new IndexUpdateStatus(
                "Während des Reindex ist ein Fehler aufgetreten: " + e.getMessage(),
                UpdateStatus.ERROR
            );
            log.error("Fehler beim Indexupdate {}", description, e);
        } finally {
            // Wiederherstellen der Index-Einstellungen
            reindexHandler.updateDestIndexSettings(
                IndexSettings.of(b ->
                    b
                        .numberOfReplicas(originalIndexSettings.numberOfReplicas())
                        .refreshInterval(originalIndexSettings.refreshInterval())
                )
            );
        }
        return status;
    }

    private IndexUpdateStatus deleteIndexByDescription(IndexUpdateDescription description) {
        if (StringUtils.isBlank(description.getDestIndex())) {
            return new IndexUpdateStatus(
                "Es wurde kein Index zum Löschen übergeben, der Fehler wird ignoriert.",
                UpdateStatus.DELETED
            );
        }
        if (!readConnector.indexExists(description.getDestIndex())) {
            return new IndexUpdateStatus(
                "Es wurde versucht den Index " +
                    description.getDestIndex() +
                    " zu löschen, dieser existiert allerdings nicht.",
                UpdateStatus.DELETED
            );
        }

        try {
            client.indices().delete(r -> r.index(description.getDestIndex()));
        } catch (IOException e) {
            // PV-8946 catch-log-return-error
            log.error("Fehler beim Löschen des Indexes {}.", description.getDestIndex(), e);
            return new IndexUpdateStatus(
                "Beim Löschen des Index trat ein Fehler auf: " + e.getMessage(),
                UpdateStatus.ERROR
            );
        }
        return new IndexUpdateStatus(
            "Der Index " + description.getDestIndex() + " wurde gelöscht.",
            UpdateStatus.DELETED
        );
    }
}
