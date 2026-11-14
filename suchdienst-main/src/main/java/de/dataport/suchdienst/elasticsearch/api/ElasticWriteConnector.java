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
package de.dataport.suchdienst.elasticsearch.api;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.BulkIndexByScrollFailure;
import co.elastic.clients.elasticsearch._types.Conflicts;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SlicesCalculation;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.UpdateResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteAliasRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexTemplateResponse;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.elasticsearch.indices.PutAliasRequest;
import co.elastic.clients.elasticsearch.indices.PutIndexTemplateRequest;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesRequest;
import co.elastic.clients.elasticsearch.indices.put_index_template.IndexTemplateMapping;
import co.elastic.clients.elasticsearch.indices.update_aliases.Action;
import com.google.common.collect.Iterables;
import de.dataport.suchdienst.aop.MasterMethod;
import de.dataport.suchdienst.aop.RetryableWhenBackgroundJob;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.elasticsearch.api.exception.ElasticSearchException;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequest;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequestStatus;
import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Primary
@Service
@RetryableWhenBackgroundJob
public class ElasticWriteConnector extends Elastic {

    private static final String SUCHDIENST_INDEX_PATTERN = "suchdienst_*";

    private final ElasticReadConnector readConnector;

    public ElasticWriteConnector(
        ElasticsearchClient client,
        JsonMapper mapper,
        IndexConfiguration indexConfiguration,
        IndexRefreshMgr indexRefreshMgr,
        ElasticReadConnector readConnector,
        SuchdienstProperties properties
    ) {
        super(client, mapper, indexConfiguration, indexRefreshMgr, properties);
        this.readConnector = readConnector;
    }

    public boolean ping() throws IOException {
        return client.ping().value();
    }

    @MasterMethod
    public Optional<UpdateResponse<ImportRequest>> updateImportRequestStatus(
        String id,
        ImportRequestStatus status,
        boolean updateProcessedAt
    ) {
        Optional<UpdateResponse<ImportRequest>> oResponse = Optional.empty();
        ImportRequest statusUpdate;
        // Update Timestamp?
        if (updateProcessedAt) {
            statusUpdate = ImportRequest.createStatusUpdate(status, Instant.now());
        } else {
            statusUpdate = ImportRequest.createStatusUpdate(status, null);
        }

        try {
            UpdateResponse<ImportRequest> response = client.update(
                r ->
                    r
                        .index(indexConfiguration.getCurrentIndexName(IndexNames.IMPORTQUEUE))
                        .id(id)
                        .doc(statusUpdate),
                ImportRequest.class
            );
            oResponse = Optional.of(response);
            indexRefreshMgr.markAsDirty(IndexNames.IMPORTQUEUE);
        } catch (Exception e) {
            // PV-8946 catch-log-skip
            log.warn("ImportRequest {} konnte nicht als {} markiert werden.", id, status, e);
        }
        return oResponse;
    }

    @MasterMethod
    public <T> IndexResponse index(T obj, IndexNames indexName, String id) {
        IndexRequest.Builder<T> builder = new IndexRequest.Builder<>();
        builder.index(indexConfiguration.getCurrentIndexName(indexName));
        builder.document(obj);
        if (ObjectUtils.isNotEmpty(id)) {
            builder.id(id);
        }
        try {
            IndexResponse response = client.index(builder.build());
            indexRefreshMgr.markAsDirty(indexName);
            return response;
        } catch (IOException e) {
            // PV-8946 catch-rethrow
            String objectName = Objects.isNull(id)
                ? obj.getClass().getSimpleName()
                : id + " " + obj.getClass().getSimpleName();
            String errorText = String.format(
                "Fehler beim Einfügen in den Index '%s'. Folgendes Objekt sollte eingefügt werden: %s",
                indexName,
                objectName
            );
            throw new ElasticSearchException(ErrorCodeEnum.ELSE_IO_ERROR, errorText, e);
        }
    }

    @MasterMethod
    public DeleteByQueryResponse clearIndex(String index) {
        try {
            return deleteByQuery(index, QueryBuilders.matchAll().build()._toQuery());
        } catch (ElasticSearchException e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                "Fehler beim Löschen im Index ",
                e
            );
        }
    }

    @MasterMethod
    public void invalidateImportRequests(LocalDateTime processedBefore) {
        try {
            //Filter
            RangeQuery requestProcessedAt = RangeQuery.of(r ->
                r.date(d -> d.field("requestProcessedAt").lt(processedBefore.toString()))
            );
            var processedStatusValues = ImportRequestStatus.getProcessed()
                .stream()
                .map(s -> FieldValue.of(s.name()))
                .toList();
            var statusIsProcessed = TermsQuery.of(t ->
                t.field("status").terms(v -> v.value(processedStatusValues))
            );
            Query boolQuery = BoolQuery.of(b ->
                b.filter(f -> f.terms(statusIsProcessed)).filter(f -> f.range(requestProcessedAt))
            )._toQuery();
            List<String> idsToRemove = readConnector.searchAllIdsPit(
                IndexNames.IMPORTQUEUE,
                100,
                boolQuery
            );
            if (ObjectUtils.isNotEmpty(idsToRemove)) {
                int partitionSize = 100;
                Iterable<List<String>> idsToRemovePartitions = Iterables.partition(
                    idsToRemove,
                    partitionSize
                );
                log.info(
                    "Lösche '{}' Elemente in Blöcken von '{}'",
                    idsToRemove.size(),
                    partitionSize
                );
                for (List<String> idsToRemovePartition : idsToRemovePartitions) {
                    log.info("Lösche folgende Elemente: {}", idsToRemovePartition);
                    deleteByIdsQuery(IndexNames.IMPORTQUEUE, idsToRemovePartition);
                }
            }
        } catch (Exception e) {
            // PV-8946 catch-rethrow
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                String.format(
                    "Fehler beim Lesen der Import-Warteschlange. Konnte expired Imports nicht lesen, processedBefore='%s'",
                    processedBefore
                ),
                e
            );
        }
    }

    @MasterMethod
    public DeleteByQueryResponse deleteVerwaltungsleistungByLb(
        String arsFederalstate,
        String lbId,
        Instant deleteBefore
    ) {
        AliasNames aliasName = AliasNames.ARS_MAPPING.get(arsFederalstate);
        Query termQuery = QueryBuilders.term(t -> t.field("leistungsbeschreibung.id").value(lbId));
        Query rangeQuery = QueryBuilders.range(r ->
            r.date(d -> d.field("lastUpdate").lte(deleteBefore.toString()))
        );
        Query bool = QueryBuilders.bool(b -> b.filter(List.of(termQuery, rangeQuery)));
        return deleteByQuery(indexConfiguration.getCurrentWriteAliasName(aliasName), bool);
    }

    @MasterMethod
    public DeleteByQueryResponse deleteOnlineServiceRelationsBySchemeAgencyId(
        String schemeAgencyId,
        String producer
    ) {
        Query schemaAgencyQuery = Elastic.QueryHelper.term(
            "metaData.schemeAgencyId",
            schemeAgencyId
        );
        Query produceryQuery = Elastic.QueryHelper.term("metaData.producer", producer);
        Query query = QueryBuilders.bool(b -> b.filter(List.of(schemaAgencyQuery, produceryQuery)));
        return deleteByQuery(
            indexConfiguration.getCurrentWriteAliasName(AliasNames.ONLINEDIENSTE_RELATIONEN),
            query
        );
    }

    @MasterMethod
    public DeleteByQueryResponse deleteSuggestionByLb(String... lbIds) {
        try {
            return deleteByQuery(
                indexConfiguration.getCurrentIndexName(IndexNames.SUGGESTIONS),
                QueryHelper.terms("lbId", lbIds)
            );
        } catch (ElasticSearchException e) {
            //PV-8946 catch-rethrow
            String errorText = String.format(
                "Fehler beim Löschen aus Suggestions mit den LB-IDs %s",
                StringUtils.abbreviate(Arrays.toString(lbIds), 1024)
            );
            throw new ElasticSearchException(ErrorCodeEnum.ELSE_IO_ERROR, errorText, e);
        }
    }

    @MasterMethod
    public DeleteByQueryResponse deleteByIdsQuery(IndexNames index, List<String> ids) {
        String indexName = indexConfiguration.getCurrentIndexName(index);
        try {
            return deleteByQuery(indexName, QueryHelper.ids(ids));
        } catch (ElasticSearchException e) {
            String errorText = String.format(
                "Fehler beim Löschen aus %s bei den IDs %s",
                indexName,
                StringUtils.abbreviate(ids.toString(), 1024)
            );
            throw new ElasticSearchException(ErrorCodeEnum.ELSE_IO_ERROR, errorText, e);
        }
    }

    @MasterMethod
    public DeleteByQueryResponse removeDeletedObjectList(LocalDateTime deleteBefore) {
        try {
            Query query = QueryBuilders.range(r ->
                r.date(d -> d.field("deletedAt").lt(deleteBefore.toString()))
            );
            return deleteByQuery(
                indexConfiguration.getCurrentIndexName(IndexNames.DELETED_OBJECTS),
                query
            );
        } catch (ElasticSearchException e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                String.format(
                    "Fehler beim Entfernen von gemerkten Löschungen, deleteBefore='%s'",
                    deleteBefore
                ),
                e
            );
        }
    }

    @MasterMethod
    private DeleteByQueryResponse deleteByQuery(String indexName, Query query) {
        try {
            indexRefreshMgr.ensureFresh(indexName);
            DeleteByQueryResponse deleteByQueryResponse = client.deleteByQuery(del ->
                del
                    .index(indexName)
                    .query(query)
                    .slices(s -> s.computed(SlicesCalculation.Auto))
                    .scrollSize(5000L)
                    // Bei der nächtlichen Neuberechnung der Verwaltungsleistungen laufen pro
                    // Bundesland parallele VlRecalcTasks. BUND-Leistungsbeschreibungen werden
                    // dabei in jedem Bundesland-Task verarbeitet, wodurch zwei Threads
                    // gleichzeitig Suggestions zur selben lbId löschen/neu schreiben können.
                    // Conflicts.Proceed sorgt dafür, dass deleteByQuery zwischenzeitlich
                    // bereits ersetzte oder gelöschte Dokumente überspringt, statt mit
                    // version_conflict_engine_exception abzubrechen. Das Ergebnis ist
                    // fachlich identisch, da der Nachbar-Thread dieselben Quelldaten schreibt.
                    .conflicts(Conflicts.Proceed)
            );
            logIfError(deleteByQueryResponse, indexName);
            indexRefreshMgr.markAsDirty(indexName);
            return deleteByQueryResponse;
        } catch (IOException e) {
            throw new ElasticSearchException(ErrorCodeEnum.ELSE_IO_ERROR, "Fehler beim Löschen", e);
        }
    }

    @MasterMethod
    public DeleteResponse deleteFromIndex(IndexNames index, String id) {
        String indexName = indexConfiguration.getCurrentIndexName(index);
        try {
            return client.delete(r -> r.index(indexName).id(id));
        } catch (IOException e) {
            String errorText = String.format(
                "Fehler beim Löschen aus %s bei der ID '%s'",
                indexName,
                id
            );
            throw new ElasticSearchException(ErrorCodeEnum.ELSE_IO_ERROR, errorText, e);
        }
    }

    @MasterMethod
    public DeleteIndexTemplateResponse deleteIndexTemplate(String name) throws IOException {
        return client.indices().deleteIndexTemplate(f -> f.name(name));
    }

    /**
     * Legt einen Alias auf einem bestehenden Index an.
     *
     * @param indexName Zielindex
     * @param aliasName Aliasname
     * @param writeIndex {@code true}, wenn der Alias als Write-Index markiert werden soll
     */
    @MasterMethod
    public void createIndexAlias(String indexName, String aliasName, boolean writeIndex) {
        var aliasRequest = PutAliasRequest.of(builder ->
            builder.index(indexName).name(aliasName).isWriteIndex(writeIndex)
        );
        try {
            client.indices().putAlias(aliasRequest);
        } catch (Exception e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                "Fehler beim Erstellen des Indexalias " + aliasName + " auf den Index " + indexName,
                e
            );
        }
    }

    /**
     * Erstellt einen neuen Index mit übergebenen Mappings/Settings und setzt den Write-Alias auf diesen Index.
     *
     * <p>Dafür wird der bestehende Alias zunächst global auf passenden Suchdienst-Indizes entfernt
     * und anschließend beim Erstellen des neuen Indexes neu gesetzt.
     *
     * @param indexName Der neue Indexname, der zum Schreiben genutzt werden soll
     * @param aliasName Alias, der auf den neuen Index zeigen soll
     * @param indexSettings Indexsettings für den neuen Index
     * @param indexMappings Indexmappings für den neuen Index
     */
    @MasterMethod
    public void createIndexWithUpdatedWriteAlias(
        String indexName,
        String aliasName,
        IndexSettings indexSettings,
        TypeMapping indexMappings
    ) {
        deleteIndexAlias(aliasName);
        createIndex(indexName, aliasName, indexSettings, indexMappings);
    }

    private void createIndex(
        String indexName,
        String aliasName,
        IndexSettings indexSettings,
        TypeMapping indexMappings
    ) {
        try {
            CreateIndexRequest createRequest = CreateIndexRequest.of(builder ->
                builder
                    .index(indexName)
                    .settings(indexSettings)
                    .mappings(indexMappings)
                    .aliases(aliasName, builderAlias -> builderAlias)
            );
            client.indices().create(createRequest);
        } catch (Exception e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                "Fehler beim Erstellen des Indexes " + indexName + " mit dem Alias " + aliasName,
                e
            );
        }
    }

    /**
     * Entfernt einen Alias von allen Suchdienst-Indizes.
     *
     * @param aliasName Aliasname, der entfernt werden soll
     */
    private void deleteIndexAlias(String aliasName) {
        try {
            DeleteAliasRequest deleteAliasRequest = DeleteAliasRequest.of(builder ->
                builder.name(aliasName).index(SUCHDIENST_INDEX_PATTERN)
            );
            client.indices().deleteAlias(deleteAliasRequest);
        } catch (IOException ex) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                "Fehler beim Löschen des Indexalias " + aliasName,
                ex
            );
        }
    }

    @MasterMethod
    public DeleteIndexResponse deleteIndex(String indexName) {
        if (indexName == null) {
            throw new IllegalArgumentException("Der Indexname darf nicht null sein.");
        }
        try {
            DeleteIndexRequest request = DeleteIndexRequest.of(builder -> builder.index(indexName));
            return client.indices().delete(request);
        } catch (IOException e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                "Fehler beim Löschen des Index " + indexName,
                e
            );
        }
    }

    /**
     * Schwenkt den konfigurierten Write-Alias atomar von einem Index auf einen anderen um.
     *
     * @param addIndexName Index, der den Alias erhalten soll
     * @param removeIndexName Index, von dem der Alias entfernt werden soll
     * @param alias Alias-Kennung zur Auflösung des Write-Aliasnamens
     */
    @MasterMethod
    public void updateWriteIndexAlias(
        String addIndexName,
        String removeIndexName,
        AliasNames alias
    ) {
        String aliasName = indexConfiguration.getCurrentWriteAliasName(alias);
        updateIndexnameInAlias(addIndexName, removeIndexName, aliasName);
    }

    /**
     * Schwenkt den konfigurierten Read-Alias atomar von einem Index auf einen anderen um.
     *
     * @param addIndexName Index, der den Alias erhalten soll
     * @param removeIndexName Index, von dem der Alias entfernt werden soll
     * @param alias Alias-Kennung zur Auflösung des Read-Aliasnamens
     */
    @MasterMethod
    public void updateReadIndexAlias(
        String addIndexName,
        String removeIndexName,
        AliasNames alias
    ) {
        String aliasName = indexConfiguration.getCurrentReadAliasName(alias);
        updateIndexnameInAlias(addIndexName, removeIndexName, aliasName);
    }

    /**
     * Führt die Alias-Umschaltung per {@code updateAliases} in einem Request aus
     * (Remove alter Index + Add neuer Index).
     *
     * @param addIndexName Index, auf den umgeschwenkt wird
     * @param removeIndexName Index, von dem entfernt wird
     * @param aliasName Technischer Aliasname
     */
    private void updateIndexnameInAlias(
        String addIndexName,
        String removeIndexName,
        String aliasName
    ) {
        Objects.requireNonNull(addIndexName, "addIndexName darf nicht null sein");
        Objects.requireNonNull(removeIndexName, "removeIndexName darf nicht null sein");
        Objects.requireNonNull(aliasName, "aliasName darf nicht null sein");

        UpdateAliasesRequest updateRequest = UpdateAliasesRequest.of(actionBuilder ->
            actionBuilder.actions(
                Action.of(builder ->
                    builder.remove(removeBuilder ->
                        removeBuilder.alias(aliasName).index(removeIndexName)
                    )
                ),
                Action.of(builder ->
                    builder.add(addBuilder -> addBuilder.alias(aliasName).index(addIndexName))
                )
            )
        );
        try {
            client.indices().updateAliases(updateRequest);
        } catch (Exception e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                "Fehler beim Umsetzen des IndexAlias " +
                    aliasName +
                    " vom Index " +
                    removeIndexName +
                    " auf den Index " +
                    addIndexName,
                e
            );
        }
    }

    @MasterMethod
    public CreateIndexResponse createIndexFromJsonString(
        String mappingJson,
        String settingsJson,
        String indexName
    ) throws IOException {
        CreateIndexRequest.Builder builder = new CreateIndexRequest.Builder();
        builder.index(indexName);
        if (settingsJson != null) {
            try (var reader = new StringReader(settingsJson)) {
                builder.settings(s -> s.withJson(reader));
            }
        }
        try (var reader = new StringReader(mappingJson)) {
            builder.mappings(m -> m.withJson(reader));
        }
        CreateIndexRequest createIndexRequest = builder.build();
        return client.indices().create(createIndexRequest);
    }

    @MasterMethod
    public void createIndexTemplate(
        String name,
        Long priority,
        int shards,
        int replicas,
        String refreshInterval,
        List<String> indexPatterns
    ) throws IOException {
        IndexTemplateMapping indexTemplateMapping = IndexTemplateMapping.of(itm ->
            itm
                .settings(s ->
                    s
                        .numberOfShards(String.valueOf(shards))
                        .numberOfReplicas(String.valueOf(replicas))
                        .refreshInterval(Time.of(t -> t.time(refreshInterval)))
                )
                // Verhindert, dass aus dem Model automatisch ein Mapping angelegt wird
                .mappings(m -> m.dynamic(DynamicMapping.False))
        );

        PutIndexTemplateRequest putIndexTemplateRequest = PutIndexTemplateRequest.of(
            indexTemplateRequest ->
                indexTemplateRequest
                    .name(name)
                    .indexPatterns(indexPatterns)
                    .template(indexTemplateMapping)
                    .priority(priority)
        );

        client.indices().putIndexTemplate(putIndexTemplateRequest);
    }

    private void logIfError(DeleteByQueryResponse deleteByQueryResponse, String indexName) {
        List<BulkIndexByScrollFailure> failures = deleteByQueryResponse.failures();
        if (ObjectUtils.isNotEmpty(failures)) {
            Long total = deleteByQueryResponse.total();
            List<Pair<String, String>> errorList = failures
                .stream()
                .map(BulkIndexByScrollFailure::cause)
                .filter(Objects::nonNull)
                .map(cause -> Pair.of(cause.type(), cause.reason()))
                .toList();
            log.warn(
                "Fehler bei Ausführung von deleteByQuery: Fehler '{}', Total '{}', Index '{}', Errors: '{}'",
                failures.size(),
                total,
                indexName,
                errorList,
                new RuntimeException("Fehler in deleteByQuery")
            );
        }
    }
}
