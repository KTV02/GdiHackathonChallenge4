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

import static java.util.stream.Collectors.toSet;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.MultiTermsBucket;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.cat.HealthResponse;
import co.elastic.clients.elasticsearch.cat.IndicesResponse;
import co.elastic.clients.elasticsearch.core.MsearchRequest;
import co.elastic.clients.elasticsearch.core.MsearchResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.msearch.MultiSearchResponseItem;
import co.elastic.clients.elasticsearch.core.msearch.RequestItem;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.elasticsearch.indices.GetAliasRequest;
import co.elastic.clients.elasticsearch.indices.GetAliasResponse;
import co.elastic.clients.elasticsearch.indices.GetIndexRequest;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import co.elastic.clients.elasticsearch.indices.GetIndicesSettingsRequest;
import co.elastic.clients.elasticsearch.indices.GetMappingRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.elasticsearch.indices.IndexState;
import co.elastic.clients.elasticsearch.indices.get_alias.IndexAliases;
import de.dataport.suchdienst.aop.RetryableWhenBackgroundJob;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.elasticsearch.api.exception.ElasticSearchException;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequestStatus;
import de.dataport.suchdienst.util.Bundeslaender;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RetryableWhenBackgroundJob
public class ElasticInternalReadConnector extends Elastic {

    public static final String KEY_COUNT_AGGREGATION = "valueCount";

    private static final String QUERY_TERM_VERSION = "version";
    private static final String QUERY_TERM_ARS_FEDERAL_STATE = "arsFederalState";
    private static final String QUERY_TERM_STATUS = "status";
    private static final String QUERY_TERM_SCHEME_AGENCY_ID = "metaData.schemeAgencyId";
    private static final String QUERY_TERM_PRODUCER = "metaData.producer";
    private static final String FIELD_NAME_LAST_UPDATE = "lastUpdate";

    private final MessageSource messageSource;

    public ElasticInternalReadConnector(
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

    public String clusterHealth() throws IOException {
        HealthResponse health = client.cat().health();
        return toJson(health);
    }

    public String clusterIndices() throws IOException {
        IndicesResponse indices = client.cat().indices();
        return toJson(indices);
    }

    public long countLeistungsbeschreibungen(List<String> arss) {
        Query query = QueryHelper.terms(QUERY_TERM_ARS_FEDERAL_STATE, arss);
        return countDocs(IndexNames.LEISTUNGSBESCHREIBUNG, query);
    }

    public long countPendingImportQueue(String schemeAgencyId) {
        Query schemaAgencyQuery = QueryHelper.terms(QUERY_TERM_SCHEME_AGENCY_ID, schemeAgencyId);
        Query importStatusQuery = buildQueryForPendingImportRequestStatus();
        Query query = QueryBuilders.bool()
            .filter(schemaAgencyQuery)
            .filter(importStatusQuery)
            .build()
            ._toQuery();
        return countDocs(IndexNames.IMPORTQUEUE, query);
    }

    public long countDocsSchemeAgencyIdBy(IndexNames index, String schemeAgencyId) {
        Query schemaAgencyQuery = QueryHelper.terms(QUERY_TERM_SCHEME_AGENCY_ID, schemeAgencyId);
        Query query = QueryBuilders.bool().filter(schemaAgencyQuery).build()._toQuery();
        return countDocs(index, query);
    }

    @Override
    public long countDocs(IndexNames index, Query query) {
        return super.countDocs(index, query);
    }

    public String getCodeListAggregations() {
        SourceConfig source = new SourceConfig.Builder()
            .filter(f -> f.includes("code", "uri", "text", QUERY_TERM_VERSION))
            .build();
        try {
            indexRefreshMgr.ensureFresh(IndexNames.CODELISTENTRIES);
            SearchResponse<JsonNode> search = client.search(
                s ->
                    s
                        .size(0)
                        .aggregations("commonest", agg ->
                            agg.topHits(top -> top.source(source).size(5))
                        )
                        .aggregations("occurrences", agg -> agg.terms(t -> t.field("uri").size(5)))
                        .aggregations("unique", agg -> agg.cardinality(c -> c.field("uri"))),
                JsonNode.class
            );
            return toJson(search);
        } catch (IOException e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                "Fehler beim Laden der Aggregation der Codelisten.",
                e
            );
        }
    }

    public Map<String, Aggregate> getIndexAggregation(IndexNames indexName) {
        List<String> bundeslandSchemeAgencyIds = Arrays.stream(Bundeslaender.values())
            .map(Bundeslaender::getSchemeAgencyId)
            .toList();
        Query termsQuery = QueryHelper.terms(
            QUERY_TERM_SCHEME_AGENCY_ID,
            bundeslandSchemeAgencyIds
        );

        Aggregation topHitsAggregation = Aggregation.of(a ->
            a.topHits(t ->
                t
                    .sort(s -> s.field(f -> f.field(FIELD_NAME_LAST_UPDATE).order(SortOrder.Desc)))
                    .source(s -> s.fetch(false))
                    .docvalueFields(f -> f.field(FIELD_NAME_LAST_UPDATE))
                    .size(1)
            )
        );
        Aggregation termsAggregation = Aggregation.of(a ->
            a
                .terms(t -> t.field(QUERY_TERM_ARS_FEDERAL_STATE).size(20))
                .aggregations("top_hits", topHitsAggregation)
        );
        SearchRequest request = SearchRequest.of(r ->
            r
                .index(indexConfiguration.getCurrentIndexName(indexName))
                .size(0)
                .query(termsQuery)
                .aggregations("group_count", termsAggregation)
        );

        try {
            indexRefreshMgr.ensureFresh(indexName);
            SearchResponse<Void> response = client.search(request, Void.class);
            return response.aggregations();
        } catch (IOException e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                String.format("Fehler bei der IndexAggregation, indexName='%s'", indexName),
                e
            );
        }
    }

    public TypeMapping getIndexMappings(String indexName) throws IOException {
        GetMappingRequest mappingRequest = GetMappingRequest.of(builder ->
            builder.index(indexName)
        );
        return client.indices().getMapping(mappingRequest).get(indexName).mappings();
    }

    public IndexSettings getIndexSettings(String indexName) throws IOException {
        GetIndicesSettingsRequest settingsRequest = GetIndicesSettingsRequest.of(builder ->
            builder.index(indexName)
        );
        IndexState indexState = client.indices().getSettings(settingsRequest).get(indexName);

        IndexSettings loadedSettings = indexState == null ? null : indexState.settings();
        if (loadedSettings == null) {
            throw new ElasticSearchException(
                ErrorCodeEnum.NOT_FOUND,
                "Settings zum Index " + indexName + " wurden nicht gefunden."
            );
        }
        if (loadedSettings.index() == null) {
            throw new ElasticSearchException(
                ErrorCodeEnum.SEARCH_UNEXPECTED_RESULT,
                "Die geladenen Settings aus dem Index " + indexName + " enthalten kein Indexfeld."
            );
        }
        return IndexSettings.of(builder ->
            builder
                .analysis(loadedSettings.index().analysis())
                .numberOfShards(loadedSettings.index().numberOfShards())
                .numberOfReplicas(loadedSettings.index().numberOfReplicas())
        );
    }

    public Set<String> getIndicesByIndexName(IndexNames indexName) {
        GetIndexRequest request = GetIndexRequest.of(builder ->
            builder.index(indexConfiguration.getCurrentIndexName(indexName) + "*")
        );
        try {
            GetIndexResponse response = client.indices().get(request);
            return response.indices().keySet();
        } catch (IOException e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                "Fehler beim Lesen der Indexnamen",
                e
            );
        }
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

    public Optional<IndexAliases> getAliasForIndex(String index) {
        var aliasRequest = GetAliasRequest.of(builder -> builder.index(index));
        try {
            GetAliasResponse response = client.indices().getAlias(aliasRequest);
            return Optional.ofNullable(response.get(index));
        } catch (IOException e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                "Bei der Suche nach dem Index " + index + " ist ein Fehler aufgetreten.",
                e
            );
        }
    }

    public <T> HitsMetadata<T> searchByIndex(
        IndexNames index,
        int page,
        int size,
        @Nullable String include,
        @Nullable String exclude,
        List<String> fields,
        Class<T> clazz
    ) {
        return this.searchByIndex(index, null, false, page, size, include, exclude, fields, clazz);
    }

    @SuppressWarnings("java:S107") // refactoring wäre hier zu aufwendig
    public <T> HitsMetadata<T> searchByIndex(
        IndexNames index,
        String query,
        boolean escapeQuery,
        int page,
        int size,
        @Nullable String include,
        @Nullable String exclude,
        List<String> fields,
        Class<T> clazz
    ) {
        SearchRequest.Builder requestBuilder = new SearchRequest.Builder();
        requestBuilder.source(s -> s.filter(QueryHelper.sourceFilter(include, exclude)));
        requestBuilder.from(page * size);
        requestBuilder.size(size);
        return searchByIndex(index, requestBuilder, query, escapeQuery, fields, clazz);
    }

    private <T> HitsMetadata<T> searchByIndex(
        IndexNames index,
        SearchRequest.Builder requestBuilder,
        String query,
        boolean escapeQuery,
        List<String> fields,
        Class<T> clazz
    ) {
        try {
            requestBuilder.index(indexConfiguration.getCurrentIndexName(index));
            requestBuilder.sort(s -> s.score(v -> v));
            if (StringUtils.isNotEmpty(query)) {
                String finalQuery = escapeQuery ? QueryHelper.escape(query) : query;
                MultiMatchQuery multiMatchQuery = MultiMatchQuery.of(mmq ->
                    mmq.query(finalQuery).fields(fields)
                );
                requestBuilder.query(q -> q.multiMatch(multiMatchQuery));
            }
            indexRefreshMgr.ensureFresh(index);
            SearchRequest request = requestBuilder.build();
            return client.search(request, clazz).hits();
        } catch (IOException e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                messageSource.getMessage(
                    "elastic_read_connector.exception_search_by_index_error",
                    null,
                    Locale.GERMAN
                ),
                e
            );
        }
    }

    public Map<String, String> getDataOverviewAgencyIdAndProducer(List<IndexNames> indexNames) {
        List<String> indexNameStrings = indexNames
            .stream()
            .map(indexConfiguration::getCurrentIndexName)
            .toList();
        try {
            Aggregation bySchemeAgencyId = Aggregation.of(agg ->
                agg.multiTerms(mt ->
                    mt
                        .size(100)
                        .terms(t -> t.field(QUERY_TERM_SCHEME_AGENCY_ID))
                        .terms(t -> t.field(QUERY_TERM_PRODUCER))
                )
            );
            SearchRequest request = SearchRequest.of(r ->
                r.aggregations("bySchemeAgencyId", bySchemeAgencyId).size(0).index(indexNameStrings)
            );
            SearchResponse<Void> response = client.search(request, Void.class);
            Aggregate aggregate = response.aggregations().get("bySchemeAgencyId");
            List<MultiTermsBucket> buckets = aggregate.multiTerms().buckets().array();
            return buckets
                .stream()
                .collect(
                    Collectors.toMap(
                        bucket -> bucket.key().getFirst().stringValue(),
                        bucket -> bucket.key().get(1).stringValue()
                    )
                );
        } catch (Exception e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                "Beim Aggregieren der SchemeAgencyId und Producer in den JZuFi-Indices ist ein Fehler aufgetreten.",
                e
            );
        }
    }

    public Map<IndexNames, Map<String, Long>> getAggregatedDocumentCounts(
        Set<String> schemeAgencyIds,
        IndexNames... indexNames
    ) {
        List<String> indexStrings = Arrays.stream(indexNames)
            .map(indexConfiguration::getCurrentIndexName)
            .toList();
        MsearchResponse<Void> response = getAggregatedDocumentCounts(schemeAgencyIds, indexStrings);
        return parseAggregatedCounts(response, indexNames);
    }

    public Map<AliasNames, Map<String, Long>> getAggregatedDocumentCounts(
        Set<String> schemeAgencyIds,
        AliasNames... aliasNames
    ) {
        List<String> indexStrings = Arrays.stream(aliasNames)
            .map(indexConfiguration::getCurrentWriteAliasName)
            .toList();
        MsearchResponse<Void> response = getAggregatedDocumentCounts(schemeAgencyIds, indexStrings);
        return parseAggregatedCounts(response, aliasNames);
    }

    private MsearchResponse<Void> getAggregatedDocumentCounts(
        Set<String> schemeAgencyIds,
        List<String> indexStrings
    ) {
        Map<String, Aggregation> aggregations = createSchemeAgencyIdAggregations(schemeAgencyIds);

        List<RequestItem> items = indexStrings
            .stream()
            .map(indexName ->
                RequestItem.of(item ->
                    item
                        .header(header -> header.index(indexName))
                        .body(body -> body.size(0).aggregations(aggregations))
                )
            )
            .toList();
        try {
            return client.msearch(r -> r.searches(items), Void.class);
        } catch (IOException e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                "Beim Zählen der Geschäftsobjekte in den JZuFi-Indices für die Datenübersicht ist ein Fehler aufgetreten.",
                e
            );
        }
    }

    @SafeVarargs
    private <T> Map<T, Map<String, Long>> parseAggregatedCounts(
        MsearchResponse<Void> response,
        T... keys
    ) {
        var result = new LinkedHashMap<T, Map<String, Long>>();
        var responses = response.responses();
        if (responses.size() != keys.length) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                "Erwartete %d Responses, bekam %d".formatted(keys.length, responses.size())
            );
        }
        for (int i = 0; i < responses.size(); i++) {
            var currentResponse = responses.get(i);
            var aggregations = currentResponse.result().aggregations();
            var counts = new HashMap<String, Long>();
            aggregations.forEach((schemeAgencyId, aggregate) -> {
                var filtered = aggregate.filter().aggregations();
                var docCount = filtered.get(KEY_COUNT_AGGREGATION).valueCount().value();
                counts.put(schemeAgencyId, Math.round(docCount));
            });
            result.put(keys[i], counts);
        }
        return result;
    }

    public Map<String, Long> countDocuments(List<String> writeAliases) {
        try {
            List<RequestItem> items = writeAliases
                .stream()
                .map(alias ->
                    RequestItem.of(item ->
                        item
                            .header(header -> header.index(alias))
                            .body(body ->
                                body
                                    .size(0)
                                    .query(QueryBuilders.matchAll().build()._toQuery())
                                    .trackTotalHits(t -> t.enabled(true))
                            )
                    )
                )
                .toList();
            MsearchRequest request = MsearchRequest.of(r -> r.searches(items));
            MsearchResponse<Void> msearchResponse = client.msearch(request, Void.class);
            List<MultiSearchResponseItem<Void>> responses = msearchResponse.responses();

            return IntStream.range(0, writeAliases.size())
                .boxed()
                .collect(
                    Collectors.toMap(writeAliases::get, i ->
                        responses.get(i).result().hits().total().value()
                    )
                );
        } catch (Exception e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                "Beim Zählen der Verwaltungsleistungen für die Datenübersicht ist ein Fehler aufgetreten.",
                e
            );
        }
    }

    public Map<String, Long> getDataOverviewImportQueue(Set<String> schemeAgencyIds) {
        Map<String, Aggregation> aggregations = createSchemeAgencyIdAggregations(schemeAgencyIds);

        var statusQuery = buildQueryForPendingImportRequestStatus();
        SearchRequest request = SearchRequest.of(r ->
            r
                .query(statusQuery)
                .aggregations(aggregations)
                .size(0)
                .index(indexConfiguration.getCurrentIndexName(IndexNames.IMPORTQUEUE))
        );

        try {
            indexRefreshMgr.ensureFresh(IndexNames.IMPORTQUEUE);
            SearchResponse<Void> response = client.search(request, Void.class);
            return response
                .aggregations()
                .entrySet()
                .stream()
                .collect(
                    Collectors.toMap(Map.Entry::getKey, e ->
                        Optional.ofNullable(
                            e
                                .getValue()
                                .filter()
                                .aggregations()
                                .get(KEY_COUNT_AGGREGATION)
                                .valueCount()
                                .value()
                        )
                            .map(Double::longValue)
                            .orElse(0L)
                    )
                );
        } catch (Exception e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                "Beim Zählen der offenen ImportRequests für die Datenübersicht ist ein Fehler aufgetreten.",
                e
            );
        }
    }

    private static Query buildQueryForPendingImportRequestStatus() {
        var statusNames = ImportRequestStatus.getPending()
            .stream()
            .map(ImportRequestStatus::name)
            .collect(toSet());
        return QueryHelper.terms(QUERY_TERM_STATUS, statusNames);
    }

    private static Map<String, Aggregation> createSchemeAgencyIdAggregations(
        Collection<String> schemeAgencyIds
    ) {
        return schemeAgencyIds
            .stream()
            .distinct()
            .collect(
                Collectors.toMap(java.util.function.Function.identity(), id ->
                    Aggregation.of(agg ->
                        agg
                            .filter(QueryHelper.term(QUERY_TERM_SCHEME_AGENCY_ID, id))
                            .aggregations(
                                KEY_COUNT_AGGREGATION,
                                Aggregation.of(countAgg ->
                                    countAgg.valueCount(val ->
                                        val.field(QUERY_TERM_SCHEME_AGENCY_ID)
                                    )
                                )
                            )
                    )
                )
            );
    }
}
