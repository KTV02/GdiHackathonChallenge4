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

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.AliasDefinition;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.elasticsearch.indices.get_alias.IndexAliases;
import de.dataport.suchdienst.elasticsearch.api.AliasNames;
import de.dataport.suchdienst.elasticsearch.api.ElasticInternalReadConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticWriteConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexConfiguration;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.elasticsearch.api.exception.ElasticSearchException;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ElasticIndexService {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
        "yyyy.MM.dd_HH.mm.ss"
    ).withZone(ZoneId.systemDefault());

    private final ElasticInternalReadConnector readConnector;
    private final ElasticWriteConnector writeConnector;
    private final IndexConfiguration indexConfiguration;

    public ElasticIndexService(
        ElasticInternalReadConnector readConnector,
        ElasticWriteConnector elasticWriteConnector,
        IndexConfiguration indexConfiguration
    ) {
        this.readConnector = readConnector;
        this.writeConnector = elasticWriteConnector;
        this.indexConfiguration = indexConfiguration;
    }

    /**
     * Löscht alle Verwaltungsleistung-Indizes, die nicht im Read-Alias für das jeweilige Bundesland stehen.
     * Dies soll verhindern, dass Indizes, die bei einem Abbruch einer Neuberechnung liegen geblieben sind,
     * für immer bestehen bleiben.
     */
    public void cleanRemainingIndices() {
        for (AliasNames alias : AliasNames.values()) {
            IndexNames currentIndexName = AliasNames.getIndexName(alias);
            try {
                Set<String> existingIndexNames = readConnector.getIndicesByIndexName(
                    currentIndexName
                );
                if (existingIndexNames.size() > 1) {
                    String currentReadIndex = readConnector.getReadIndexnameFromAlias(alias);
                    writeConnector.updateWriteIndexAlias(
                        currentReadIndex,
                        currentIndexName.getIndexName() + "*",
                        alias
                    );
                    existingIndexNames.forEach(indexName -> {
                        if (!currentReadIndex.equals(indexName)) {
                            writeConnector.deleteIndex(indexName);
                        }
                    });
                }
            } catch (Exception e) {
                log.warn("Beim Löschen von unvollständigen Bürgerindices", e);
            }
        }
    }

    /**
     * Prüft zu jeder ARS im {@link IndexNames#ARS_MAPPING} die Alias Konfiguration in Elasticsearch.
     * Wird ein READ oder WRITE Alias nicht gefunden, wird dieser angelegt
     */
    public void checkAndRepairIndicesAliasConfig() {
        for (AliasNames alias : AliasNames.values()) {
            IndexNames currentIndexName = AliasNames.getIndexName(alias);

            // Hinweis: Indices in Elastic können ein Datum enthalten, hole den konkreten Namen
            Set<String> indices = readConnector.getIndicesByIndexName(currentIndexName);
            if (indices.size() != 1) {
                // Zu diesem Zeitpunkt darf es zu indexName in der Implementierung nur einen korrespondierenden Index geben
                throw new IllegalStateException(
                    String.format(
                        "Für den Index '%s' wurde mehr als ein IndexName gefunden",
                        currentIndexName
                    )
                );
            }
            String index = indices.iterator().next();

            // AliasNames werden von der Backend-Implementierung vorgegeben,
            // Read/Write Namen enthalten ein Prefix und sollten in Elasticsearch bekannt sein, das wird nun verifiziert
            String expectedReadAlias = indexConfiguration.getCurrentReadAliasName(alias);
            String expectedWriteAlias = indexConfiguration.getCurrentWriteAliasName(alias);

            try {
                // Hole hinterlegten Alias zu existierendem Index und
                // extrahiere die AliasDefinition für Read und Write Alias
                Optional<IndexAliases> aliasForIndex = readConnector.getAliasForIndex(index);
                Optional<AliasDefinition> oReadAliasDefinition = aliasForIndex
                    .map(IndexAliases::aliases)
                    .map(aliases -> aliases.get(expectedReadAlias));
                Optional<AliasDefinition> oWriteAliasDefinition = aliasForIndex
                    .map(IndexAliases::aliases)
                    .map(aliases -> aliases.get(expectedWriteAlias));

                if (oReadAliasDefinition.isEmpty()) {
                    log.warn(
                        "READ Alias für Index {} wurde nicht gefunden und wird neu angelegt.",
                        index
                    );
                    writeConnector.createIndexAlias(index, expectedReadAlias, false);
                }
                if (oWriteAliasDefinition.isEmpty()) {
                    log.warn(
                        "WRITE Alias für Index {} wurde nicht gefunden und wird neu angelegt.",
                        index
                    );
                    writeConnector.createIndexAlias(index, expectedWriteAlias, true);
                }
            } catch (Exception e) {
                log.warn("Beim Index Reparieren ist ein Fehler aufgetreten", e);
            }
        }
    }

    /**
     * Erzeugt einen zeitgestempelten, eindeutigen Indexnamen auf Basis des konfigurierten Basisnamens.
     *
     * @param index Technischer Index-Typ, dessen Basisname aus der Konfiguration gelesen wird
     * @return Vollständiger Indexname im Format {@code <basename>_yyyy.MM.dd_HH.mm.ss}
     */
    public String createNewIndexName(IndexNames index) {
        return (
            indexConfiguration.getCurrentIndexName(index) + "_" + formatter.format(Instant.now())
        );
    }

    /**
     * Erstellt einen neuen Index auf Basis des aktuell über den Read-Alias gefundenen Indexes
     * (Mappings/Settings werden übernommen) und schwenkt den Write-Alias auf den neuen Index.
     *
     * @param indexName Logischer Index, aus dem der neue Name erzeugt wird
     * @param alias Alias-Kennung zur Ermittlung von Read-/Write-Aliasnamen
     * @return Paar aus altem (bisheriger Read-Index) und neuem Indexnamen
     */
    public IndexSwapResult createNewIndexSwapWriteAlias(IndexNames indexName, AliasNames alias) {
        String oldIndexName = readConnector.getReadIndexnameFromAlias(alias);
        String newIndexName = createNewIndexName(indexName);
        String writeAlias = indexConfiguration.getCurrentWriteAliasName(alias);
        try {
            IndexSettings indexSettings = readConnector.getIndexSettings(oldIndexName);
            TypeMapping indexMappings = readConnector.getIndexMappings(oldIndexName);
            writeConnector.createIndexWithUpdatedWriteAlias(
                newIndexName,
                writeAlias,
                indexSettings,
                indexMappings
            );
        } catch (IOException e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                "Fehler beim Lesen von IndexSettings oder IndexMappings aus dem Index " +
                    oldIndexName,
                e
            );
        }

        log.info(
            "Neuer Index '{}' erstellt, Write-Alias geschwenkt (alter Index: '{}')",
            newIndexName,
            oldIndexName
        );
        return new IndexSwapResult(oldIndexName, newIndexName);
    }

    /**
     * Finalisiert einen bereits gestarteten Index-Swap, indem der Read-Alias auf den neuen Index
     * umgehängt und anschließend der alte Index gelöscht wird.
     *
     * @param newIndexName Zielindex für den Read-Alias
     * @param oldIndexName Bisheriger Read-Index, der nach Umschalten entfernt werden soll
     * @param alias Alias-Kennung zur Auflösung des Read-Aliasnamens
     */
    public void finalizeIndex(String newIndexName, String oldIndexName, AliasNames alias) {
        writeConnector.updateReadIndexAlias(newIndexName, oldIndexName, alias);
        try {
            writeConnector.deleteIndex(oldIndexName);
        } catch (Exception e) {
            log.warn(
                "Fehler beim Löschen des alten Index '{}' nach Finalisierung. " +
                    "Wird beim nächsten cleanRemainingIndices aufgeräumt.",
                oldIndexName,
                e
            );
        }
    }

    /**
     * Führt einen best-effort Rollback eines fehlgeschlagenen Rebuilds aus:
     * Write-Alias zurück auf den alten Index, danach neuen Index löschen.
     *
     * <p>Der Rollback wird nur ausgeführt, wenn sowohl alter als auch neuer Index existieren.
     *
     * @param newIndexName Neu erzeugter Index, der verworfen werden soll
     * @param oldIndexName Vorheriger stabiler Index, auf den zurückgeschwenkt wird
     * @param alias Alias-Kennung zur Auflösung des Write-Aliasnamens
     */
    public void rollbackIndex(String newIndexName, String oldIndexName, AliasNames alias) {
        log.warn(
            "Rollback OD-Relation-Index: Write-Alias zurück auf '{}', lösche '{}'",
            oldIndexName,
            newIndexName
        );
        boolean oldIndexExists = readConnector.indexExists(oldIndexName);
        boolean newIndexExists = readConnector.indexExists(newIndexName);

        if (oldIndexExists && newIndexExists) {
            writeConnector.updateWriteIndexAlias(oldIndexName, newIndexName, alias);
            writeConnector.deleteIndex(newIndexName);
        }
    }

    /**
     * Ergebnis eines Index-Swaps mit alter und neuer Indexbezeichnung.
     *
     * @param oldIndexName Vor dem Swap aktiver Index
     * @param newIndexName Für den Rebuild erzeugter Zielindex
     */
    public record IndexSwapResult(String oldIndexName, String newIndexName) {}
}
