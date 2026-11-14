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
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.ResponseBody;
import co.elastic.clients.elasticsearch.core.search.SourceFilter;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.GetAliasRequest;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.JsonpSerializable;
import co.elastic.clients.json.jackson.Jackson3JsonpGenerator;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.elasticsearch.api.exception.ElasticSearchException;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import jakarta.json.stream.JsonGenerator;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.ObjectUtils;
import tools.jackson.databind.json.JsonMapper;

public abstract class Elastic {

    static final String OBJECT_ID = "objectId";
    public static final int WINDOW_SIZE = 10000; // max. Page Size
    public static final int ENTITY_FETCH_PARTITION_SIZE = 1000;

    public static final int ELASTIC_MAX_TERMS_COUNT = 65536;

    protected final ElasticsearchClient client;
    protected final JsonMapper mapper;
    protected final IndexConfiguration indexConfiguration;
    protected final IndexRefreshMgr indexRefreshMgr;
    protected final SuchdienstProperties properties;

    protected Elastic(
        ElasticsearchClient client,
        JsonMapper mapper,
        IndexConfiguration indexConfiguration,
        IndexRefreshMgr indexRefreshMgr,
        SuchdienstProperties properties
    ) {
        this.client = client;
        this.mapper = mapper;
        this.indexConfiguration = indexConfiguration;
        this.indexRefreshMgr = indexRefreshMgr;
        this.properties = properties;
    }

    String toJson(JsonpSerializable obj) {
        StringWriter stringWriter = new StringWriter();
        try (
            JsonGenerator generator = new Jackson3JsonpGenerator(
                mapper.createGenerator(stringWriter)
            )
        ) {
            JsonpMapper jsonMapper = new Jackson3JsonpMapper();
            obj.serialize(generator, jsonMapper);
            generator.flush(); // Wichtig: Daten in den Writer schreiben
            return stringWriter.toString();
        }
    }

    <T> HitsMetadata<T> emptyHits() {
        return HitsMetadata.of(b ->
            b
                .hits(Collections.emptyList())
                .total(v -> v.value(0L).relation(TotalHitsRelation.Eq))
                .maxScore(0.0)
        );
    }

    public <T> List<T> extractSource(ResponseBody<T> response) {
        return response.hits().hits().stream().map(Hit::source).filter(Objects::nonNull).toList();
    }

    public ExponentialRandomBackOff buildExponentialRandomBackOff() {
        SuchdienstProperties.Retry retry = properties.getElastic().getRetry();
        long interval = retry.getInitialInterval().toMillis();
        long maxInterval = retry.getMaxInterval().toMillis();
        double multiplier = retry.getMultiplier();
        return new ExponentialRandomBackOff(interval, maxInterval, multiplier);
    }

    protected long countDocs(IndexNames index, Query query) {
        indexRefreshMgr.ensureFresh(index);
        try {
            CountResponse count = client.count(cr ->
                cr.index(indexConfiguration.getCurrentIndexName(index)).query(query)
            );
            return count.count();
        } catch (IOException e) {
            var msg = String.format(
                "Fehler beim Zählen der Dokumente im Index '%s'",
                index.getIndexName()
            );
            throw new ElasticSearchException(ErrorCodeEnum.ELSE_IO_ERROR, msg, e);
        }
    }

    protected boolean indexExistsInElastic(String indexName) {
        try {
            return client
                .indices()
                .exists(ExistsRequest.of(builder -> builder.index(indexName)))
                .value();
        } catch (IOException e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                "Fehler bei der Prüfung ob der Index " + indexName + " existiert",
                e
            );
        }
    }

    public String getReadIndexnameFromAlias(AliasNames alias) {
        String currentReadAliasName = indexConfiguration.getCurrentReadAliasName(alias);
        var aliasRequest = GetAliasRequest.of(builder -> builder.name(currentReadAliasName));
        try {
            var aliasResponse = client.indices().getAlias(aliasRequest).aliases();
            if (ObjectUtils.isNotEmpty(aliasResponse)) {
                if (aliasResponse.size() == 1) {
                    return aliasResponse.keySet().iterator().next();
                }
                for (var entry : aliasResponse.entrySet()) {
                    boolean hasReadFlag = entry
                        .getValue()
                        .aliases()
                        .values()
                        .stream()
                        .anyMatch(a -> Boolean.FALSE.equals(a.isWriteIndex()));
                    if (hasReadFlag) {
                        return entry.getKey();
                    }
                }
            }
        } catch (IOException e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                "Bei der Suche nach dem Alias " + alias + " ist ein Fehler aufgetreten.",
                e
            );
        }
        throw new ElasticSearchException(
            ErrorCodeEnum.NOT_FOUND,
            "Zu dem Indexalias " + currentReadAliasName + " wurde kein Index gefunden."
        );
    }

    public static class QueryHelper {

        private QueryHelper() {}

        public static Query ids(List<String> ids) {
            return QueryBuilders.ids().values(ids).build()._toQuery();
        }

        public static Query ids(String id, String... ids) {
            return QueryBuilders.ids().values(id, ids).build()._toQuery();
        }

        public static Query term(String field, String value) {
            return QueryBuilders.term().field(field).value(FieldValue.of(value)).build()._toQuery();
        }

        public static Query terms(String field, String... values) {
            return terms(field, Arrays.stream(values).toList());
        }

        public static Query terms(String field, Collection<String> values) {
            List<FieldValue> fields = values.stream().map(FieldValue::of).toList();
            return QueryBuilders.terms()
                .field(field)
                .terms(v -> v.value(fields))
                .build()
                ._toQuery();
        }

        public static Query prefix(String field, String value) {
            return QueryBuilders.prefix().field(field).value(value).build()._toQuery();
        }

        public static SourceFilter sourceFilter(String include, String exclude) {
            SourceFilter.Builder sourceFilterBuilder = new SourceFilter.Builder();
            if (Objects.nonNull(include)) {
                sourceFilterBuilder.includes(include);
            }
            if (Objects.nonNull(exclude)) {
                sourceFilterBuilder.excludes(exclude);
            }
            return sourceFilterBuilder.build();
        }

        public static SourceFilter sourceFilter(List<String> includes, List<String> excludes) {
            SourceFilter.Builder sourceFilterBuilder = new SourceFilter.Builder();
            if (ObjectUtils.isNotEmpty(includes)) {
                sourceFilterBuilder.includes(includes);
            }
            if (ObjectUtils.isNotEmpty(excludes)) {
                sourceFilterBuilder.excludes(excludes);
            }
            return sourceFilterBuilder.build();
        }

        public static Collection<String> escape(Collection<String> values) {
            return values.stream().map(QueryHelper::escape).toList();
        }

        /**
         * Übernommen aus org.apache.lucene.queryparser.classic.QueryParserBase
         * Returns a String where those characters that QueryParser expects to be escaped are escaped by a
         * preceding <code>\</code>.
         */
        public static String escape(String s) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                // These characters are part of the query syntax and must be escaped
                if (
                    c == '\\' ||
                    c == '+' ||
                    c == '-' ||
                    c == '!' ||
                    c == '(' ||
                    c == ')' ||
                    c == ':' ||
                    c == '^' ||
                    c == '[' ||
                    c == ']' ||
                    c == '\"' ||
                    c == '{' ||
                    c == '}' ||
                    c == '~' ||
                    c == '*' ||
                    c == '?' ||
                    c == '|' ||
                    c == '&' ||
                    c == '/'
                ) {
                    sb.append('\\');
                }
                sb.append(c);
            }
            return sb.toString();
        }
    }
}
