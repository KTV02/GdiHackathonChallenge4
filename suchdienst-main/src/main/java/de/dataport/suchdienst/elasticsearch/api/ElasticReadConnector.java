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

import static de.dataport.suchdienst.config.CacheConfiguration.CACHE_NAME_CODELIST_ENTRIES;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOptionsBuilders;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeRelation;
import co.elastic.clients.elasticsearch.core.ClosePointInTimeResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.OpenPointInTimeResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.mget.MultiGetError;
import co.elastic.clients.elasticsearch.core.mget.MultiGetResponseItem;
import co.elastic.clients.elasticsearch.core.search.FieldCollapse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.InnerHits;
import co.elastic.clients.elasticsearch.core.search.PointInTimeReference;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.util.NamedValue;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import de.dataport.suchdienst.aop.RetryableWhenBackgroundJob;
import de.dataport.suchdienst.config.CacheConfiguration;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.elasticsearch.api.exception.ElasticSearchException;
import de.dataport.suchdienst.elasticsearch.api.exception.ElasticsearchConnectionException;
import de.dataport.suchdienst.elasticsearch.model.UpdateDescription;
import de.dataport.suchdienst.elasticsearch.model.UpdateStatus;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ExceptionType;
import de.dataport.suchdienst.importer.model.jzufi.OnlinedienstEntity;
import de.dataport.suchdienst.importer.model.jzufi.SpezialisierungEntity;
import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequestStatus;
import de.dataport.suchdienst.importer.model.warteschlange.QueueableImportRequest;
import de.dataport.suchdienst.indexer.exception.ImportException;
import de.dataport.suchdienst.indexer.model.AffectedLbIdsEntry;
import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.model.CodelistKey;
import de.dataport.suchdienst.indexer.model.Identifiable;
import de.dataport.suchdienst.indexer.model.LeikaEntry;
import de.dataport.suchdienst.indexer.model.mapping.RelationTyp;
import de.dataport.suchdienst.indexer.service.LeikaService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Handles all Read operations to the Elastic API
 */
@Slf4j
@Component
@RetryableWhenBackgroundJob
public class ElasticReadConnector extends Elastic {

    public static final String KEY_COUNT_AGGREGATION = "valueCount";

    private static final String QUERY_TERM_VERSION = "version";
    private static final String QUERY_TERM_ARS_FEDERAL_STATE = "arsFederalState";
    private static final String QUERY_TERM_STATUS = "status";
    private static final String QUERY_TERM_SCHEME_AGENCY_ID = "metaData.schemeAgencyId";
    private static final String FIELD_REQUEST_ACCEPTED_AT = "requestAcceptedAt";
    private static final String DEFAULT_PIT_TIME = "30s";

    private static final String FIELD_NAME_VERSIONS = "versions";

    private final MessageSource messageSource;

    protected ElasticReadConnector(
        ElasticsearchClient client,
        JsonMapper mapper,
        IndexConfiguration indexConfiguration,
        IndexRefreshMgr indexRefreshMgr,
        SuchdienstProperties properties,
        MessageSource messageSource
    ) {
        super(client, mapper, indexConfiguration, indexRefreshMgr, properties);
        this.messageSource = messageSource;
    }

    public boolean codeListExists(String uri, String version) {
        if (uri == null || uri.isEmpty() || version == null || version.isEmpty()) {
            throw new ImportException(
                ErrorCodeEnum.XZUFI_UNEXPECTED_FORMAT,
                messageSource.getMessage(
                    "elastic_read_connector.exception_uri_or_version_missing_error",
                    null,
                    Locale.GERMAN
                ),
                ExceptionType.FACHLICH
            );
        }
        indexRefreshMgr.ensureFresh(IndexNames.CODELISTENTRIES);
        try {
            Query termUri = QueryHelper.term("uri", uri);
            Query termVersion = QueryHelper.term(QUERY_TERM_VERSION, version);
            BoolQuery query = QueryBuilders.bool().filter(termUri).filter(termVersion).build();
            SearchRequest request = new SearchRequest.Builder()
                .index(indexConfiguration.getCurrentIndexName(IndexNames.CODELISTENTRIES))
                .query(q -> q.bool(query))
                .source(s -> s.fetch(false))
                .build();
            SearchResponse<Void> response = client.search(request, Void.class);
            return response.hits().total() != null && response.hits().total().value() > 0L;
        } catch (IOException e) {
            var msg = String.format(
                "Fehler beim Laden der Codeliste mit URI '%s', Version '%s'",
                uri,
                version
            );
            throw new ElasticSearchException(ErrorCodeEnum.ELSE_IO_ERROR, msg, e);
        }
    }

    public List<OnlinedienstEntity> fetchOnlinedienstEntities(
        String schemeAgencyId,
        int pageSize,
        String fromId
    ) {
        Query schemaAgencyQuery = QueryHelper.terms(QUERY_TERM_SCHEME_AGENCY_ID, schemeAgencyId);
        Query query = QueryBuilders.bool().filter(schemaAgencyQuery).build()._toQuery();
        var searchResult = searchAfter(
            IndexNames.ONLINEDIENSTE,
            fromId,
            pageSize,
            query,
            SourceConfig.of(sc -> sc.fetch(true)),
            false,
            OnlinedienstEntity.class
        );

        return searchResult.hits().stream().map(Hit::source).filter(Objects::nonNull).toList();
    }

    private static Query buildQueryForPendingImportRequestStatus() {
        var statusNames = ImportRequestStatus.getPending()
            .stream()
            .map(ImportRequestStatus::name)
            .collect(toSet());
        return QueryHelper.terms(QUERY_TERM_STATUS, statusNames);
    }

    public boolean indexExists(String indexName) {
        if (indexName == null) {
            throw new IllegalArgumentException(
                messageSource.getMessage(
                    "elastic_read_connector.exception_indexname_null_error",
                    null,
                    Locale.GERMAN
                )
            );
        }
        return indexExistsInElastic(indexName);
    }

    /**
     * MultiGET auf Dokumente per ID. Diese Methode verwendet einen Retry, um nicht geladene Objekte nachladen zu können.
     * Für zeitkritische Anfragen bitte {@link ElasticReadConnector#getByIds(IndexNames, Set, List, List, Class)} verwenden.
     */
    public <T extends Identifiable> List<T> getByIdsWithRetry(
        IndexNames indexName,
        Set<String> ids,
        List<String> includes,
        List<String> excludes,
        Class<T> clazz
    ) throws ElasticSearchException {
        if (ObjectUtils.isEmpty(ids)) {
            return List.of();
        }

        int maxRetry = properties.getElastic().getRetry().getMaxRetries();
        ExponentialRandomBackOff backOff = buildExponentialRandomBackOff();

        Set<String> currentIds = new HashSet<>(ids);
        final List<T> successResults = new ArrayList<>();
        List<MultiGetError> failures = new ArrayList<>();

        int counter = 0;
        do {
            if (ObjectUtils.isNotEmpty(failures)) {
                backOff.sleep();
                log.warn(
                    "Versuch: {} | Retry nach '{}' Verbindungsfehler(n) während GetByIds",
                    counter,
                    failures.size()
                );
                currentIds = failures.stream().map(MultiGetError::id).collect(Collectors.toSet());
            }

            GetByIdResponse<T> response = getByIds(
                indexName,
                currentIds,
                includes,
                excludes,
                clazz
            );

            successResults.addAll(response.successResults());
            failures = response.failureItems();

            counter++;
        } while (ObjectUtils.isNotEmpty(failures) && counter <= maxRetry);

        if (!failures.isEmpty()) {
            MultiGetError firstError = failures.getFirst();
            throw new ElasticSearchException(
                ErrorCodeEnum.FAILURE_RESPONSE,
                "MGet Retry Exhausted: %d/%d Objekten konnten nicht geladen werden, Erstes Fehlerobjekt: %s".formatted(
                    failures.size(),
                    ids.size(),
                    firstError.toString()
                )
            );
        }

        return successResults;
    }

    public record GetByIdResponse<T>(List<T> successResults, List<MultiGetError> failureItems) {}

    /**
     * MultiGET auf Dokumente per ID.
     * Für denselben Aufruf mit Retry-Mechanismus bitte {@link ElasticReadConnector#getByIdsWithRetry(IndexNames, Set, List, List, Class)} verwenden.
     */
    public <T extends Identifiable> GetByIdResponse<T> getByIds(
        IndexNames indexName,
        Set<String> ids,
        List<String> includes,
        List<String> excludes,
        Class<T> clazz
    ) throws ElasticSearchException {
        final String index = indexConfiguration.getCurrentIndexName(indexName);
        final List<T> successResults = new ArrayList<>();
        final List<MultiGetError> failures = new ArrayList<>();

        try {
            MgetResponse<T> mget = client.mget(
                mg ->
                    mg
                        .index(index)
                        .ids(new ArrayList<>(ids))
                        .sourceIncludes(includes)
                        .sourceExcludes(excludes)
                        .realtime(true),
                clazz
            );
            List<MultiGetResponseItem<T>> results = mget.docs();
            results.forEach(result -> {
                if (result.isResult()) {
                    if (result.result().found()) {
                        successResults.add(result.result().source());
                    }
                } else if (result.isFailure()) {
                    failures.add(result.failure());
                }
            });
        } catch (JacksonException e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.JSON_IO_ERROR,
                messageSource.getMessage(
                    "elastic_read_connector.exception_search_by_ids_error",
                    new String[] {
                        String.valueOf(ids.size()),
                        StringUtils.abbreviate(ids.toString(), 1024),
                        indexName.getIndexName(),
                    },
                    Locale.GERMAN
                ),
                e
            );
        } catch (IOException e) {
            throw new ElasticsearchConnectionException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                messageSource.getMessage(
                    "elastic_read_connector.exception_search_by_ids_error",
                    new String[] {
                        String.valueOf(ids.size()),
                        StringUtils.abbreviate(ids.toString(), 1024),
                        indexName.getIndexName(),
                    },
                    Locale.GERMAN
                ),
                e
            );
        }
        return new GetByIdResponse<>(successResults, failures);
    }

    public <T> List<T> searchAllPit(
        IndexNames indexName,
        int size,
        Query query,
        SourceConfig config,
        Class<T> clazz
    ) {
        List<SearchResponse<T>> responses = searchAfterPointInTimeAll(
            indexName,
            size,
            query,
            config,
            clazz
        );
        return responses
            .stream()
            .flatMap(r -> r.hits().hits().stream())
            .map(Hit::source)
            .toList();
    }

    private <T> List<SearchResponse<T>> searchAfterPointInTimeAll(
        IndexNames indexName,
        int size,
        Query query,
        SourceConfig config,
        Class<T> clazz
    ) {
        indexRefreshMgr.ensureFresh(indexName);

        String pointInTimeId = openPointInTime(indexName);

        List<SearchResponse<T>> cumulativeObjects = new ArrayList<>();
        SearchResponse<T> currentResponse;
        List<FieldValue> currentSort = Collections.emptyList();
        try {
            do {
                currentResponse = searchAfterPointInTime(
                    pointInTimeId,
                    currentSort,
                    size,
                    query,
                    config,
                    clazz
                );
                pointInTimeId = currentResponse.pitId();
                List<Hit<T>> hits = currentResponse.hits().hits();
                if (isNotEmpty(hits)) {
                    currentSort = Iterables.getLast(hits).sort();
                    cumulativeObjects.add(currentResponse);
                }
            } while (currentResponse.hits().hits().size() == size);
            return cumulativeObjects;
        } catch (Exception e) {
            //PV-8946 catch-rethrow
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                String.format(
                    "Fehler bei Suche über PointInTime ID '%s' in Index '%s'",
                    pointInTimeId,
                    indexConfiguration.getCurrentIndexName(indexName)
                ),
                e
            );
        } finally {
            closePointInTime(pointInTimeId);
        }
    }

    public String openPointInTime(IndexNames... indexNames) {
        List<String> indexes = new ArrayList<>();
        for (IndexNames indexName : indexNames) {
            indexes.add(indexConfiguration.getCurrentIndexName(indexName));
        }
        try {
            OpenPointInTimeResponse response = client.openPointInTime(pit ->
                pit.index(indexes).keepAlive(t -> t.time(DEFAULT_PIT_TIME))
            );
            return response.id();
        } catch (Exception _) {
            //PV-8946 catch-rethrow
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                "Fehler beim Starten von PointInTime"
            );
        }
    }

    private <T> SearchResponse<T> searchAfterPointInTime(
        String pointInTimeId,
        List<FieldValue> sortFrom,
        int size,
        Query query,
        SourceConfig config,
        Class<T> clazz
    ) throws IOException {
        PointInTimeReference pit = PointInTimeReference.of(p ->
            p.id(pointInTimeId).keepAlive(t -> t.time(DEFAULT_PIT_TIME))
        );
        SortOptions sort = SortOptions.of(s -> s.field(v -> v.field("_shard_doc")));

        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        searchRequestBuilder.query(query).size(size).source(config).sort(sort).pit(pit);

        if (isNotEmpty(sortFrom)) {
            searchRequestBuilder.searchAfter(sortFrom);
        }
        SearchRequest searchRequest = searchRequestBuilder.build();

        return client.search(searchRequest, clazz);
    }

    public boolean closePointInTime(String id) {
        try {
            ClosePointInTimeResponse closePointInTimeResponse = client.closePointInTime(pit ->
                pit.id(id)
            );
            return closePointInTimeResponse.succeeded();
        } catch (Exception _) {
            //PV-8946 catch-rethrow
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                "Fehler beim Starten von PointInTime"
            );
        }
    }

    /**
     * Liefert einen Codelist-Eintrag aus Elasticsearch.
     * Achtung: Die Signatur muss zu {@link LeikaService#updateLocalPvLagenCache()} passend bleiben
     */
    @Cacheable(
        // Der Cache läuft nicht ab. Es wird davon ausgegangen, dass sich nur in Sonderfällen die Belegung zu
        // 'URI/Version/Code' ändert.
        // Sollte dieser Fall jemals eintreten, würde ein Container-Neustart das Problem beheben
        cacheNames = CACHE_NAME_CODELIST_ENTRIES,
        unless = "#result == null",
        cacheManager = CacheConfiguration.IN_MEM_MGR
    )
    public CodelistEntry getCodelistEntry(CodelistKey codelistKey) {
        try {
            Optional<CodelistEntry> entry = getById(
                IndexNames.CODELISTENTRIES,
                codelistKey.uri() + "_" + codelistKey.version() + "_" + codelistKey.code(),
                CodelistEntry.class
            );
            return entry.orElse(null);
        } catch (Exception e) {
            // PV-8946 catch-rethrow
            var msg = String.format(
                "Fehler beim Laden des Codelisten-Eintrags, uri='%s', version='%s', code='%s'",
                codelistKey.uri(),
                codelistKey.version(),
                codelistKey.code()
            );
            throw new ElasticSearchException(ErrorCodeEnum.ELSE_IO_ERROR, msg, e);
        }
    }

    /**
     * Laden aller Codelist-Einträge in Chunks.
     *
     * @return Die zugehörigen Codelisten-Einträge. Im Fehlerfall wird hier ausnahmsweise eine leere Liste zurückgegeben.
     * Die Aufrufer kümmern sich um Workarounds.
     */
    public List<CodelistEntry> getCodeListEntries(String uri, String version) {
        try {
            Query boolQuery = QueryBuilders.bool()
                .filter(QueryHelper.term("uri", uri))
                .filter(QueryHelper.term(QUERY_TERM_VERSION, version))
                .build()
                ._toQuery();
            SourceConfig config = SourceConfig.of(c -> c.fetch(true));
            return searchAfterAll(
                IndexNames.CODELISTENTRIES,
                100,
                boolQuery,
                config,
                CodelistEntry.class
            );
        } catch (Exception e) {
            // PV-8946 catch-log-skip
            log.warn("Fehler beim Laden der Codeliste mit URI {}, Version {}", uri, version, e);
            return Collections.emptyList();
        }
    }

    /**
     * @see ElasticReadConnector#getById(String, String, Class)
     */
    public <T> Optional<T> getById(IndexNames indexName, String id, Class<T> clazz)
        throws ElasticSearchException {
        final String index = indexConfiguration.getCurrentIndexName(indexName);
        return getById(index, id, clazz);
    }

    /**
     * @see ElasticReadConnector#getById(String, String, Class)
     */
    public <T> Optional<T> getById(AliasNames aliasName, String id, Class<T> clazz)
        throws ElasticSearchException {
        final String index = indexConfiguration.getCurrentReadAliasName(aliasName);
        return getById(index, id, clazz);
    }

    /**
     * Search in Index per ID
     *
     * @param indexName ElasticIndex to be searched
     * @param id        ID of Object
     * @param clazz     java magic
     * @param <T>       Generic Type
     * @return Optional of Class <T>
     */
    private <T> Optional<T> getById(String indexName, String id, Class<T> clazz)
        throws ElasticSearchException {
        try {
            GetResponse<T> response = client.get(
                g -> g.index(indexName).id(id).realtime(true),
                clazz
            );
            if (response.found()) {
                T source = response.source();
                assert source != null;
                return Optional.of(source);
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            //PV-8946 catch-rethrow
            final ErrorCodeEnum code;
            if (e instanceof JacksonException) {
                code = ErrorCodeEnum.JSON_IO_ERROR;
            } else {
                code = ErrorCodeEnum.ELSE_IO_ERROR;
            }
            throw new ElasticSearchException(
                code,
                messageSource.getMessage(
                    "elastic_read_connector.exception_search_by_id_error",
                    new String[] { id, indexName },
                    Locale.GERMAN
                ),
                e
            );
        }
    }

    @SuppressWarnings("java:S107") // refactoring wäre hier zu aufwendig
    public SearchResponse<QueueableImportRequest> getImportQueue(
        int page,
        int pageSize,
        String id,
        String messageId,
        String transactionId,
        LocalDateTime acceptedAfter,
        LocalDateTime acceptedBefore,
        Collection<ImportRequestStatus> statuses
    ) {
        BoolQuery.Builder boolQueryBuilder = QueryBuilders.bool();
        // Filter
        if (isNotEmpty(id)) {
            boolQueryBuilder.filter(QueryHelper.ids(id));
        }
        if (isNotEmpty(messageId)) {
            boolQueryBuilder.filter(QueryHelper.term("metaData.id", messageId));
        }
        if (isNotEmpty(transactionId)) {
            boolQueryBuilder.filter(QueryHelper.term("metaData.transactionId", transactionId));
        }
        if (acceptedAfter != null) {
            Query rangeQuery = QueryBuilders.range()
                .date(d -> d.field(FIELD_REQUEST_ACCEPTED_AT).gt(acceptedAfter.toString()))
                .build()
                ._toQuery();

            boolQueryBuilder.filter(rangeQuery);
        }
        if (acceptedBefore != null) {
            Query rangeQuery = QueryBuilders.range()
                .date(d -> d.field(FIELD_REQUEST_ACCEPTED_AT).lt(acceptedBefore.toString()))
                .build()
                ._toQuery();
            boolQueryBuilder.filter(rangeQuery);
        }
        if (!statuses.isEmpty()) {
            var statusNames = statuses.stream().map(ImportRequestStatus::name).collect(toSet());
            boolQueryBuilder.filter(QueryHelper.terms(QUERY_TERM_STATUS, statusNames));
        }
        Query boolQuery = boolQueryBuilder.build()._toQuery();
        indexRefreshMgr.ensureFresh(IndexNames.IMPORTQUEUE);
        try {
            return client.search(
                s ->
                    s
                        .index(indexConfiguration.getCurrentIndexName(IndexNames.IMPORTQUEUE))
                        .query(boolQuery)
                        .from(page * pageSize)
                        .size(pageSize)
                        .source(c -> c.filter(f -> f.excludes("xzufi")))
                        .sort(sort ->
                            sort.field(f -> f.field(FIELD_REQUEST_ACCEPTED_AT).order(SortOrder.Asc))
                        )
                        .trackTotalHits(t -> t.enabled(true)),
                QueueableImportRequest.class
            );
        } catch (IOException e) {
            // PV-8946 catch-rethrow
            var msg = String.format(
                "Fehler beim Lesen aus ImportQueue, id='%s, messageId='%s', transactionId='%s'",
                id,
                messageId,
                transactionId
            );
            throw new ElasticSearchException(ErrorCodeEnum.IMPORT_QUEUE, msg, e);
        }
    }

    /**
     * Ermittelt die Anzahl der tatsächlich in der Import-Queue vorhandenen distinct SchemeAgencyIds
     * unter den offenen (pending) Einträgen. Wird genutzt, um die Bucket-Size in
     * {@link #getImportQueueCollapsingSchemeAgencyId(int, int)} dynamisch an die tatsächliche Anzahl
     * der Redaktionssysteme anzupassen, siehe {@code ElasticQueuePersistenceService#readAllPending()}.
     */
    public long countDistinctSchemeAgencyIdsInQueue() {
        Query query = buildQueryForPendingImportRequestStatus();
        indexRefreshMgr.ensureFresh(IndexNames.IMPORTQUEUE);
        try {
            String aggregationKey = "distinctSchemeAgencyIdCount";
            SearchResponse<Void> response = client.search(
                s ->
                    s
                        .index(indexConfiguration.getCurrentIndexName(IndexNames.IMPORTQUEUE))
                        .query(query)
                        .size(0)
                        .aggregations(aggregationKey, agg ->
                            agg.cardinality(c -> c.field(QUERY_TERM_SCHEME_AGENCY_ID))
                        ),
                Void.class
            );
            Aggregate aggregate = response.aggregations().get(aggregationKey);
            return aggregate.cardinality().value();
        } catch (IOException e) {
            // PV-8946 catch-rethrow
            throw new ElasticSearchException(
                ErrorCodeEnum.IMPORT_QUEUE,
                "Fehler beim Ermitteln der Anzahl distinct SchemeAgencyIds in der Import-Queue",
                e
            );
        }
    }

    /**
     * Liefert im Ergebnis zu jeder SchemeAgencyId maximal bucketSize ImportRequests, sortiert nach
     * requestAcceptedAt.
     *
     * @param noOfBuckets maximale Anzahl der Buckets, die geliefert werden
     * @param bucketSize  maximale Anzahl pro SchemeAgencyId
     */
    public SearchResponse<QueueableImportRequest> getImportQueueCollapsingSchemeAgencyId(
        int noOfBuckets,
        int bucketSize
    ) {
        var query = buildQueryForPendingImportRequestStatus();
        var sourceConfig = SourceConfig.of(sc ->
            sc.filter(QueryHelper.sourceFilter(null, "xzufi"))
        );
        var sortByRequestAcceptedAt = SortOptions.of(s ->
            s.field(f -> f.field(FIELD_REQUEST_ACCEPTED_AT).order(SortOrder.Asc))
        );
        var innerHits = InnerHits.of(ih ->
            ih
                .name("most_recent")
                .size(bucketSize)
                .sort(sortByRequestAcceptedAt)
                .source(sourceConfig)
        );
        var collapse = FieldCollapse.of(fc ->
            fc.field(QUERY_TERM_SCHEME_AGENCY_ID).innerHits(List.of(innerHits))
        );
        indexRefreshMgr.ensureFresh(IndexNames.IMPORTQUEUE);
        try {
            return client.search(
                s ->
                    s
                        .index(indexConfiguration.getCurrentIndexName(IndexNames.IMPORTQUEUE))
                        .query(query)
                        .size(noOfBuckets)
                        .collapse(collapse)
                        .source(sourceConfig)
                        .sort(sortByRequestAcceptedAt),
                QueueableImportRequest.class
            );
        } catch (IOException e) {
            // PV-8946 catch-rethrow
            throw new ElasticSearchException(
                ErrorCodeEnum.IMPORT_QUEUE,
                "Fehler beim Lesen aus ImportQueue",
                e
            );
        }
    }

    /**
     * Liefert alle UpdateHistory-Einträge zurück die auf ERROR stehen und neuer als timeRange-Minuten
     * sind
     *
     * @param timestamp Der Timestamp, von dem timeRange-Minuten abgezogen werden sollen
     * @param timeRange Die Minuten die vom timestamp abgezogen werden sollen
     * @return UpdateHistory-Einträge
     */
    public HitsMetadata<UpdateDescription> getLastEntriesFromUpdateHistory(
        Instant timestamp,
        int timeRange
    ) {
        String index = indexConfiguration.getCurrentIndexName(IndexNames.UPDATEHISTORY);
        if (!indexExists(index)) {
            // Falls der Updatehistory-Index noch nicht angelegt wurde
            return emptyHits();
        }

        Query statusQuery = QueryHelper.term(QUERY_TERM_STATUS, UpdateStatus.ERROR.name());
        Query rangeQuery = QueryBuilders.range()
            .date(d ->
                d.field("timestamp").gte(timestamp.minus(timeRange, ChronoUnit.MINUTES).toString())
            )
            .build()
            ._toQuery();
        Query boolQuery = QueryBuilders.bool()
            .filter(statusQuery)
            .filter(rangeQuery)
            .build()
            ._toQuery();
        SourceConfig source = SourceConfig.of(c -> c.fetch(false));

        try {
            indexRefreshMgr.ensureFresh(IndexNames.UPDATEHISTORY);
            SearchResponse<UpdateDescription> response = client.search(
                s -> s.index(index).query(boolQuery).source(source),
                UpdateDescription.class
            );

            return response.hits();
        } catch (IOException e) {
            // PV-8946 catch-rethrow
            throw new UncheckedIOException("Fehler beim Laden der UpdateHistory.", e);
        }
    }

    /**
     * Laden aller LeikaEntrys in Chunks.
     */
    public List<LeikaEntry> getLeikaEntries() {
        var matchAllQuery = QueryBuilders.matchAll().build()._toQuery();
        SourceConfig config = SourceConfig.of(c -> c.fetch(true));
        return searchAfterAll(
            IndexNames.LEISTUNGSKATALOG,
            100,
            matchAllQuery,
            config,
            LeikaEntry.class
        );
    }

    /**
     * Laden aller AffectedLbIdsEntries.
     */
    public List<AffectedLbIdsEntry> getAffectedLbIdsEntries() {
        var matchAllQuery = QueryBuilders.matchAll().build()._toQuery();
        SourceConfig config = SourceConfig.of(c -> c.fetch(true));
        return searchAfterAll(
            IndexNames.AFFECTED_LBIDS,
            50,
            matchAllQuery,
            config,
            AffectedLbIdsEntry.class
        );
    }

    public boolean isIndexEmpty(IndexNames indexName) throws IOException {
        Query matchAllQuery = QueryBuilders.matchAll().build()._toQuery();
        SearchResponse<Void> search = client.search(
            r ->
                r
                    .index(indexConfiguration.getCurrentIndexName(indexName))
                    .query(matchAllQuery)
                    .size(0),
            Void.class
        );
        return search.hits().total().value() == 0L;
    }

    public List<String> searchAllIdsPit(IndexNames indexName, int size, Query query) {
        SourceConfig config = SourceConfig.of(s -> s.fetch(false));
        List<SearchResponse<Void>> responses = searchAfterPointInTimeAll(
            indexName,
            size,
            query,
            config,
            Void.class
        );
        return responses
            .stream()
            .flatMap(r -> r.hits().hits().stream())
            .map(Hit::id)
            .toList();
    }

    /**
     * @param id      die ID nach der gesucht werden soll
     * @param include welche Felder in der Antwort enthalten sein sollen
     * @param exclude welche Felder nicht enthalten sein sollen
     * @return den gefundenen Treffer {@link Optional#empty()}
     */
    public <T> Optional<T> searchById(
        String id,
        List<String> include,
        List<String> exclude,
        IndexNames indexName,
        LocalDate validDate,
        Class<T> clazz
    ) {
        try {
            BoolQuery.Builder queryBuilderBool = new BoolQuery.Builder();
            queryBuilderBool.filter(QueryHelper.ids(id));
            addValidFilter(queryBuilderBool, validDate);
            Query query = queryBuilderBool.build()._toQuery();

            SourceConfig sourceConfig = SourceConfig.of(c ->
                c.filter(QueryHelper.sourceFilter(include, exclude))
            );

            SearchResponse<T> search = client.search(
                r ->
                    r
                        .index(indexConfiguration.getCurrentIndexName(indexName))
                        .source(sourceConfig)
                        .query(query),
                clazz
            );
            return search
                .hits()
                .hits()
                .stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .findFirst();
        } catch (IOException e) {
            // PV-8946 catch-rethrow
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                messageSource.getMessage(
                    "elastic_read_connector.exception_search_by_id_error",
                    new String[] { id, indexName.getIndexName() },
                    Locale.GERMAN
                ),
                e
            );
        }
    }

    private void addValidFilter(BoolQuery.Builder builder, LocalDate validDate) {
        if (validDate != null) {
            String date = validDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Query query = QueryBuilders.range(r ->
                r.date(d ->
                    d.field("validityPeriods").gte(date).lte(date).relation(RangeRelation.Contains)
                )
            );
            builder.filter(query);
        }
    }

    public <T extends Identifiable> List<T> searchAfterAll(
        IndexNames indexName,
        int size,
        Query query,
        SourceConfig config,
        Class<T> clazz
    ) {
        List<T> result = new ArrayList<>();
        List<Hit<T>> currentHits;
        String fromId = null;
        do {
            currentHits = searchAfter(indexName, fromId, size, query, config, false, clazz).hits();

            if (isNotEmpty(currentHits)) {
                fromId = Iterables.getLast(currentHits).id();
                result.addAll(
                    currentHits.stream().map(Hit::source).filter(Objects::nonNull).toList()
                );
            }
        } while (currentHits.size() == size);
        return result;
    }

    public <T extends Identifiable> HitsMetadata<T> searchAfter(
        AliasNames aliasName,
        String fromId,
        int size,
        Query query,
        SourceConfig config,
        boolean trackTotalHits,
        Class<T> clazz
    ) {
        String index = getReadIndexnameFromAlias(aliasName);
        SortOptions sort = SortOptions.of(s -> s.field(v -> v.field("id").order(SortOrder.Asc)));
        return searchAfter(index, fromId, size, query, config, sort, trackTotalHits, clazz);
    }

    public <T extends Identifiable> HitsMetadata<T> searchAfter(
        IndexNames indexName,
        String fromId,
        int size,
        Query query,
        SourceConfig config,
        boolean trackTotalHits,
        Class<T> clazz
    ) {
        String index = indexConfiguration.getCurrentIndexName(indexName);
        SortOptions sort = SortOptions.of(s -> s.field(v -> v.field("id").order(SortOrder.Asc)));
        return searchAfter(index, fromId, size, query, config, sort, trackTotalHits, clazz);
    }

    @SuppressWarnings("java:S107") // refactoring wäre hier zu aufwendig
    private <T> HitsMetadata<T> searchAfter(
        String index,
        String fromId,
        int size,
        @NonNull Query query,
        SourceConfig config,
        SortOptions sort,
        boolean trackTotalHits,
        Class<T> clazz
    ) throws ElasticSearchException {
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        searchRequestBuilder.index(index).size(size).query(query).source(config).sort(sort);
        if (isNotEmpty(fromId)) {
            // Im ersten Durchlauf ist die fromId leer
            searchRequestBuilder.searchAfter(FieldValue.of(fromId));
        }
        if (trackTotalHits) {
            searchRequestBuilder.trackTotalHits(t -> t.enabled(true));
        } else {
            searchRequestBuilder.trackTotalHits(t -> t.enabled(false));
        }
        SearchRequest searchRequest = searchRequestBuilder.build();

        try {
            indexRefreshMgr.ensureFresh(index);
            SearchResponse<T> response = client.search(searchRequest, clazz);
            return response.hits();
        } catch (Exception e) {
            //PV-8946 catch-rethrow
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                String.format(
                    "Fehler bei Suche über searchAfter nach ID '%s' in Index '%s', Query: %s",
                    fromId,
                    index,
                    searchRequest.query().toString()
                ),
                e
            );
        }
    }

    private record RelationIdentifier(String lbId, String parentId, Set<String> arsFederalState) {}

    /**
     * Sucht Zuständigkeiten für verschiedene Szenarien.
     * 1. Suche nach allen ZS für LBId
     * 2. Suche nach ZS für bestimmte Parents (OD oder OE)
     *
     * @param lbId            LBId
     * @param parentIds       liefert alle ZS, egal zu welchen Parents
     * @param arsFederalState Optionale Einschränkung auf Gebiete
     * @param includes        Einschränkungen für das Fetching
     * @return Gefundene Zuständigkeiten
     */
    public List<ZustaendigkeitEntity> searchRelations(
        @Nullable String lbId,
        @NonNull List<String> parentIds,
        @Nullable Set<String> arsFederalState,
        List<String> includes
    ) {
        List<RelationIdentifier> relationIdentifiers = parentIds
            .stream()
            .map(parentId -> new RelationIdentifier(lbId, parentId, arsFederalState))
            .toList();
        return searchRelations(relationIdentifiers, includes);
    }

    /**
     * Sucht Zuständigkeiten im zu suchdienst_zustaendigkeiten Index zu:
     * 1. Leistungen (arsFederalState = null)
     * 2. Kombination aus Leistung und Bundesland
     * Achtung: Das Abrufen aller Zuständigkeiten zu einem Bundesland (über arsFederalState = null) wäre theoretisch möglich,
     * sollte aber aus Gründen der Performance vermieden werden.
     *
     * @param lbId            LBId
     * @param arsFederalState Einschränkung auf Gebiete
     * @return Gefundene Zuständigkeiten
     */
    public List<ZustaendigkeitEntity> searchRelations(
        String lbId,
        @NonNull Set<String> arsFederalState
    ) {
        RelationIdentifier relationIdentifier = new RelationIdentifier(lbId, null, arsFederalState);
        return searchRelations(List.of(relationIdentifier), List.of());
    }

    /**
     * Sucht nach Zuständigkeiten im
     *
     * @param relationIdentifiers - Triple (lbID, parentId, arsFederalState)
     *                            Triple-Komponenten dürfen auch null sein, dann werden sie nicht in die Suche einbezogen.
     * @return Die gefundenen Zuständigkeiten. Wenn alle Triple komplett mit null besetzt sind, wird eine leere Menge zurückgegeben.
     */
    private List<ZustaendigkeitEntity> searchRelations(
        List<RelationIdentifier> relationIdentifiers,
        List<String> includes
    ) {
        List<Query> queries = relationIdentifiers
            .stream()
            .map(identifier -> {
                String lbId = identifier.lbId();
                String parentId = identifier.parentId();
                Set<String> arsFederalState = identifier.arsFederalState();

                BoolQuery.Builder boolBuilder = QueryBuilders.bool();

                if (ObjectUtils.isNotEmpty(parentId)) {
                    Query parentTerm = QueryHelper.term("parentId", parentId);
                    boolBuilder.filter(parentTerm);
                }
                if (ObjectUtils.isNotEmpty(lbId)) {
                    Query lbTerm = QueryHelper.term("lbId", lbId);
                    boolBuilder.filter(lbTerm);
                }
                if (ObjectUtils.isNotEmpty(arsFederalState)) {
                    Query arsFedStateTerm = QueryHelper.terms(
                        QUERY_TERM_ARS_FEDERAL_STATE,
                        arsFederalState
                    );
                    boolBuilder.filter(arsFedStateTerm);
                }

                BoolQuery boolQuery = boolBuilder.build();
                // Wurden keine Filter hinzugefügt, wird keine Suche ausgeführt
                return ObjectUtils.isNotEmpty(boolQuery.filter()) ? boolQuery._toQuery() : null;
            })
            .filter(Objects::nonNull)
            .toList();

        if (ObjectUtils.isNotEmpty(queries)) {
            Query query = QueryBuilders.bool()
                .should(queries)
                .minimumShouldMatch("1")
                .build()
                ._toQuery();
            SourceConfig config = SourceConfig.of(source ->
                source.filter(f -> f.includes(includes))
            );
            return searchAfterAll(
                IndexNames.ZUSTAENDIGKEITEN,
                1000,
                query,
                config,
                ZustaendigkeitEntity.class
            );
        } else {
            return List.of();
        }
    }

    /**
     * Sucht OE-Zuständigkeiten (typ=ORGANISATION) für eine Menge von LB-IDs.
     *
     * @param lbIds       Menge der Leistungsbeschreibungs-IDs
     * @return Gefundene OE-Zuständigkeiten
     */
    public List<ZustaendigkeitEntity> searchOeZustaendigkeitenByLbIds(Set<String> lbIds) {
        if (ObjectUtils.isEmpty(lbIds)) {
            return List.of();
        }

        Query query = QueryBuilders.bool()
            .filter(QueryHelper.terms("lbId", lbIds))
            .filter(QueryHelper.term("typ", RelationTyp.ORGANISATION.name()))
            .build()
            ._toQuery();

        SourceConfig config = SourceConfig.of(source -> source.fetch(true));

        return searchAfterAll(
            IndexNames.ZUSTAENDIGKEITEN,
            1000,
            query,
            config,
            ZustaendigkeitEntity.class
        );
    }

    public List<SpezialisierungEntity> searchSpecializationsWithSearchAfter(
        String lbId,
        String arsFederalState
    ) {
        BoolQuery.Builder boolQueryBuilder = QueryBuilders.bool();
        boolQueryBuilder.filter(Elastic.QueryHelper.term("idLB", lbId));
        boolQueryBuilder.filter(
            Elastic.QueryHelper.terms(QUERY_TERM_ARS_FEDERAL_STATE, arsFederalState)
        );

        SourceConfig config = SourceConfig.of(s -> s.fetch(true));
        return searchAfterAll(
            IndexNames.SPEZIALISIERUNGEN,
            500,
            boolQueryBuilder.build()._toQuery(),
            config,
            SpezialisierungEntity.class
        );
    }

    public HitsMetadata<UpdateDescription> searchUpdateHistory(
        Optional<Integer> version,
        IndexNames indexName,
        UpdateStatus updateStatus
    ) {
        if (
            !indexExists(indexConfiguration.getCurrentIndexName(IndexNames.UPDATEHISTORY)) &&
            indexName == IndexNames.UPDATEHISTORY
        ) {
            // Falls der Updatehistory-Index noch nicht angelegt wurde
            return emptyHits();
        }
        List<Query> termQueries = Lists.newArrayList(
            QueryHelper.term("indexName", indexName.getIndexName()),
            QueryHelper.term(QUERY_TERM_STATUS, updateStatus.name())
        );
        version.ifPresent(v ->
            termQueries.add(QueryHelper.term(QUERY_TERM_VERSION, String.valueOf(v)))
        );
        Query boolQuery = QueryBuilders.bool(b -> b.filter(termQueries));
        SortOptions sortOptions = SortOptionsBuilders.field(builder ->
            builder.field(QUERY_TERM_VERSION).order(SortOrder.Asc)
        );
        try {
            indexRefreshMgr.ensureFresh(IndexNames.UPDATEHISTORY);
            SearchResponse<UpdateDescription> response = client.search(
                r ->
                    r
                        .index(indexConfiguration.getCurrentIndexName(IndexNames.UPDATEHISTORY))
                        .query(boolQuery)
                        .sort(sortOptions),
                UpdateDescription.class
            );
            return response.hits();
        } catch (IOException e) {
            // PV-8946 catch-rethrow
            var errMsg = String.format(
                "Fehler bei der Suche in Upatehistory, indexName='%s', version='%s'",
                indexName,
                version
            );
            throw new ElasticSearchException(ErrorCodeEnum.ELSE_IO_ERROR, errMsg, e);
        }
    }

    public boolean templateExists(String name) {
        try {
            BooleanResponse response = client.indices().existsTemplate(t -> t.name(name));
            return response.value();
        } catch (ElasticsearchException e) {
            if (e.status() == 404) {
                return false;
            } else {
                // PV-8946 catch-rethrow
                var errorMsg = messageSource.getMessage(
                    "elastic_read_connector.exception_template_exists",
                    new Object[] { name, e.status(), e.getMessage() },
                    Locale.GERMAN
                );
                throw new ElasticSearchException(ErrorCodeEnum.ELSE_IO_ERROR, errorMsg);
            }
        } catch (IOException e) {
            // PV-8946 catch-rethrow
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                String.format("Fehler beim Lesen von Template '%s'.", name),
                e
            );
        }
    }

    public boolean exists(IndexNames indexName, String id) throws ElasticSearchException {
        var index = indexConfiguration.getCurrentIndexName(indexName);

        final co.elastic.clients.elasticsearch.core.ExistsRequest existsRequest =
            new co.elastic.clients.elasticsearch.core.ExistsRequest.Builder()
                .index(index)
                .id(id)
                .build();
        try {
            return client.exists(existsRequest).value();
        } catch (Exception e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                messageSource.getMessage(
                    "elastic_read_connector.exception_exists_error",
                    new String[] { id, indexName.getIndexName() },
                    Locale.GERMAN
                ),
                e
            );
        }
    }

    public List<String> aggregatePvLagenVersions() {
        Aggregation aggregation = Aggregation.of(a ->
            a.terms(t ->
                t
                    .field("leistungsbeschreibung.lifeSituationVersions")
                    // Ordnet die Ergebnisse absteigend nach dem Term der Aggregation
                    // z.B. 20230308 vor 20211001
                    .order(List.of(NamedValue.of("_key", SortOrder.Desc)))
                    .size(10)
            )
        );
        SearchRequest request = SearchRequest.of(r ->
            r
                .aggregations(FIELD_NAME_VERSIONS, aggregation)
                .size(0)
                .index(indexConfiguration.getCurrentReadAliasNames())
        );
        try {
            SearchResponse<Void> response = client.search(request, Void.class);
            Aggregate aggregate = response.aggregations().get(FIELD_NAME_VERSIONS);
            Stream<String> versions = aggregate
                .sterms()
                .buckets()
                .array()
                .stream()
                .map(StringTermsBucket::key)
                .map(FieldValue::stringValue)
                .filter(StringUtils::isNotEmpty);
            return versions.toList();
        } catch (Exception e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                "Beim Aggregieren der Versions-Informationen aus dem Leistungskatalog-Index ist ein Fehler aufgetreten.",
                e
            );
        }
    }
}
