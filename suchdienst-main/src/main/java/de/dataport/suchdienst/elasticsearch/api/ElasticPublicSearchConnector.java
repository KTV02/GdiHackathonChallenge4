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

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeRelation;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.CompletionContext;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggest;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggester;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.elasticsearch.core.search.SourceFilter;
import co.elastic.clients.elasticsearch.core.search.SuggestFuzziness;
import co.elastic.clients.elasticsearch.core.search.Suggester;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import co.elastic.clients.util.NamedValue;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import de.dataport.suchdienst.aop.RetryableWhenBackgroundJob;
import de.dataport.suchdienst.common.model.ARS;
import de.dataport.suchdienst.common.model.LeikaKey;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.elasticsearch.api.exception.ElasticSearchException;
import de.dataport.suchdienst.elasticsearch.api.model.PersonalMattersMode;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity;
import de.dataport.suchdienst.indexer.model.ArsObject;
import de.dataport.suchdienst.indexer.model.LbSuggestion;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.Verwaltungsleistung;
import de.dataport.suchdienst.suchen.exception.SuchenException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RetryableWhenBackgroundJob
public class ElasticPublicSearchConnector extends Elastic {

    private static final String QUERY_SORT_ID = "id";
    private static final String QUERY_TERM_ARS = "ars";
    private static final String QUERY_TERM_LEISTUNGSBESCHREIBUNG_ID = "leistungsbeschreibung.id";
    private static final String FIELD_NAME_ORGANISATIONSEINHEITEN = "organisationseinheiten";
    private static final String FIELD_NAME_LEISTUNG = "leistungsbeschreibung";
    private static final String FIELD_NAME_SPEZIALISIERUNG = "spezialisierungen";
    private static final String FIELD_NAME_ONLINEDIENST = "onlinedienste";
    private static final String FIELD_NAME_LAST_UPDATE = "lastUpdate";
    private static final String FIELD_NAME_VERSIONS = "versions";
    private static final String FUZZY_AUTO = "AUTO";
    private static final String FUZZY_ZERO = "0";
    private static final String FUZZY_FIVE_EIGHT = "AUTO:5,8";
    private static final int REQUIRED_LOC_COMPL_HITS = 55;
    private static final String QUERY_STRING_OTHER_LANGUAGES =
        "leistungsbeschreibung.otherLanguages";

    private final MessageSource messageSource;

    public ElasticPublicSearchConnector(
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

    public Optional<Instant> getMaxLastUpdate(String ars, LocalDate validDate) {
        BoolQuery.Builder queryBuilderBool = QueryBuilders.bool();
        queryBuilderBool.filter(getArsQuery(ars, true));
        addValidFilter(queryBuilderBool, FIELD_NAME_LEISTUNG, validDate);
        Query query = queryBuilderBool.build()._toQuery();

        SearchRequest request = SearchRequest.of(r ->
            r
                .index(
                    indexConfiguration.getCurrentReadAliasName(
                        AliasNames.getAliasNameByArs(ars.substring(0, 2))
                    )
                )
                .fields(f -> f.field(FIELD_NAME_LAST_UPDATE))
                .source(s -> s.fetch(false))
                .sort(s -> s.field(v -> v.field(FIELD_NAME_LAST_UPDATE).order(SortOrder.Desc)))
                .size(1)
                .query(query)
        );
        try {
            SearchResponse<Void> search = client.search(request, Void.class);
            if (search.hits().hits().isEmpty()) {
                return Optional.empty();
            }
            TypeToken<List<Instant>> type = new TypeToken<>() {};
            return Optional.ofNullable(
                search.hits().hits().getFirst().fields().get(FIELD_NAME_LAST_UPDATE)
            )
                .map(json -> json.<List<Instant>>to(type.getType()))
                .map(List::getFirst);
        } catch (IOException e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                "Fehler beim Laden von MaxLastUpdate.",
                e
            );
        }
    }

    public Map<String, Aggregate> searchByArs(String ars) {
        if (ars == null) {
            throw new SuchenException(
                ErrorCodeEnum.BAD_REQUEST,
                messageSource.getMessage(
                    "elastic_read_connector.exception_ars_missing",
                    null,
                    Locale.GERMAN
                )
            );
        }
        Query arsQuery = getArsQuery(ars, true);
        SearchRequest request = SearchRequest.of(r ->
            r
                .index(
                    indexConfiguration.getCurrentReadAliasName(
                        AliasNames.getAliasNameByArs(ars.substring(0, 2))
                    )
                )
                .size(0)
                .query(arsQuery)
                .aggregations(buildLifeSituationAggregation())
                .aggregations(buildLifeSituationVersionsAggregation())
        );

        try {
            SearchResponse<Void> response = client.search(request, Void.class);
            return response.aggregations();
        } catch (IOException e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                "Fehler bei der Suche nach Leistungsbeschreibungen.",
                e
            );
        }
    }

    public Optional<Verwaltungsleistung> searchLbByIdAndArs(
        String lbid,
        String ars,
        boolean useArsHierarchy,
        boolean excludeOtherThanGerman,
        LocalDate validDate
    ) {
        String escapedArs = QueryHelper.escape(ars);
        BoolQuery.Builder bool = QueryBuilders.bool();
        bool.filter(QueryHelper.term(QUERY_TERM_LEISTUNGSBESCHREIBUNG_ID, lbid));
        bool.filter(getArsQuery(escapedArs, useArsHierarchy));
        addValidFilter(bool, FIELD_NAME_LEISTUNG, validDate);

        String exclude = excludeOtherThanGerman ? QUERY_STRING_OTHER_LANGUAGES : null;

        String index = indexConfiguration.getCurrentReadAliasName(
            AliasNames.getAliasNameByArs(ars.substring(0, 2))
        );
        indexRefreshMgr.ensureFresh(index);
        SearchRequest request = SearchRequest.of(r ->
            r
                .index(index)
                .query(bool.build()._toQuery())
                .source(s -> s.filter(QueryHelper.sourceFilter(null, exclude)))
        );
        try {
            SearchResponse<Verwaltungsleistung> response = client.search(
                request,
                Verwaltungsleistung.class
            );
            return response.hits().hits().stream().findFirst().map(Hit::source);
        } catch (IOException e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                messageSource.getMessage(
                    "elastic_read_connector.exception_search_lb_by_id_and_ars_error",
                    new String[] { lbid, escapedArs },
                    Locale.GERMAN
                ),
                e
            );
        }
    }

    public Optional<OnlinedienstVL> searchOnlinedienst(
        final String odId,
        final LocalDate validDate
    ) {
        List<String> index = indexConfiguration.getCurrentReadAliasNames();

        BoolQuery.Builder queryBuilderBool = new BoolQuery.Builder();
        Query queryOeIds = QueryHelper.term("onlinedienste.id", odId);
        queryBuilderBool.filter(queryOeIds);
        addValidFilter(queryBuilderBool, FIELD_NAME_ONLINEDIENST, validDate);
        Query query = queryBuilderBool.build()._toQuery();
        SourceConfig sourceConfig = SourceConfig.of(c ->
            c.filter(QueryHelper.sourceFilter(FIELD_NAME_ONLINEDIENST, null))
        );

        try {
            SearchRequest request = SearchRequest.of(r ->
                r.index(index).query(query).source(sourceConfig).size(1)
            );
            SearchResponse<Verwaltungsleistung> search = client.search(
                request,
                Verwaltungsleistung.class
            );
            if (search.hits().hits().isEmpty()) {
                return Optional.empty();
            }
            Verwaltungsleistung vl = search.hits().hits().getFirst().source();
            return vl
                .getOnlinedienste()
                .stream()
                .filter(od -> od.getId().equals(odId))
                .findAny();
        } catch (IOException e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                String.format(
                    "Fehler bei Suche über nach Verwaltungsleistungen über OD-ID '%s' in Index '%s', Query: %s",
                    odId,
                    index,
                    query
                ),
                e
            );
        }
    }

    public Optional<Verwaltungsleistung> searchVerwaltungsleistungByLbOe(
        final List<String> includes,
        final List<String> excludes,
        @Nullable final LocalDate validDate,
        @Nullable final String lbId,
        @NonNull final String oeId
    ) {
        var queryBool = createLbByOeIdArsQuery(lbId, oeId, null, validDate);
        SourceConfig sourceConfig = SourceConfig.of(c ->
            c.filter(QueryHelper.sourceFilter(includes, excludes))
        );
        try {
            HitsMetadata<Verwaltungsleistung> hits = client
                .search(
                    s ->
                        s
                            .index(indexConfiguration.getCurrentReadAliasNames())
                            .query(queryBool)
                            .source(sourceConfig)
                            .size(1),
                    Verwaltungsleistung.class
                )
                .hits();
            if (hits.hits().isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(hits.hits().getFirst().source());
        } catch (IOException e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                "Fehler bei der Suche nach der Organisationseinheit '%s' zur Leistungsbeschreibung '%s.".formatted(
                    oeId,
                    lbId
                ),
                e
            );
        }
    }

    public List<ArsObject> searchLocation(
        String searchValue,
        int page,
        int size,
        boolean fuzzysearch
    ) throws SuchenException {
        String escapedText = QueryHelper.escape(searchValue);
        String fuzzy = fuzzysearch ? FUZZY_AUTO : FUZZY_ZERO;
        Query queryArs = MultiMatchQuery.of(m ->
            m.query(escapedText).fields("zipCodes", "ars")
        )._toQuery();
        Query queryName = MultiMatchQuery.of(m ->
            m.query(escapedText).fields("name").fuzziness(fuzzy)
        )._toQuery();
        Query queryBool = BoolQuery.of(b ->
            b.should(queryArs).should(queryName).minimumShouldMatch("1")
        )._toQuery();
        try {
            SearchResponse<ArsObject> response = client.search(
                request ->
                    request
                        .index(indexConfiguration.getCurrentIndexName(IndexNames.ARS))
                        .query(queryBool)
                        .sort(s -> s.score(v -> v))
                        .sort(s -> s.field(v -> v.field("weight").order(SortOrder.Desc)))
                        .from(page * size)
                        .size(size),
                ArsObject.class
            );
            return extractSource(response);
        } catch (Exception e) {
            throw new SuchenException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                "Fehler bei der Suche nach Orten.",
                e
            );
        }
    }

    public List<OrganisationseinheitVL> searchOrganisation(
        String lbId,
        String ars,
        LocalDate validDate,
        List<String> includes
    ) {
        SourceFilter source = QueryHelper.sourceFilter(includes, List.of());

        var boolBuilder = QueryBuilders.bool().filter(
            QueryHelper.term(FIELD_NAME_LEISTUNG + ".id", lbId),
            arsQueryWithBund(ars)
        );
        addValidFilter(boolBuilder, FIELD_NAME_ORGANISATIONSEINHEITEN, validDate);
        Query query = boolBuilder.build()._toQuery();

        indexRefreshMgr.ensureFresh(IndexNames.getIndexNameByArs(ars.substring(0, 2)));
        try {
            SearchResponse<Verwaltungsleistung> search = client.search(
                r ->
                    r
                        .index(
                            indexConfiguration.getCurrentReadAliasName(
                                AliasNames.getAliasNameByArs(ars.substring(0, 2))
                            )
                        )
                        .query(query)
                        .source(s -> s.filter(source)),
                Verwaltungsleistung.class
            );
            return extractSource(search)
                .stream()
                .findFirst()
                .map(Verwaltungsleistung::getOrganisationseinheiten)
                .orElseGet(List::of);
        } catch (IOException e) {
            throw new SuchenException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                "Fehler bei der Suche nach Organisationseinheiten",
                e
            );
        }
    }

    public HitsMetadata<ZustaendigkeitEntity> searchRelationByLbIdAndArs(
        List<String> lbIds,
        String ars,
        boolean useArsHierarchy,
        LocalDate validDate,
        int page,
        int pageSize
    ) {
        Collection<String> concatenatedArsHierarchy = new ARS(ars).getConcatedArsHierachy(
            useArsHierarchy,
            lbIds.toArray(String[]::new)
        );
        BoolQuery.Builder queryBuilder = QueryBuilders.bool().filter(
            QueryHelper.terms("lbIdWithArs", concatenatedArsHierarchy)
        );
        addValidFilter(queryBuilder, validDate);

        SearchRequest request = SearchRequest.of(r ->
            r
                .index(indexConfiguration.getCurrentIndexName(IndexNames.ZUSTAENDIGKEITEN))
                .sort(s -> s.field(f -> f.field(QUERY_SORT_ID).order(SortOrder.Asc)))
                .from(page * pageSize)
                .size(pageSize)
                .query(queryBuilder.build()._toQuery())
        );

        try {
            SearchResponse<ZustaendigkeitEntity> response = client.search(
                request,
                ZustaendigkeitEntity.class
            );
            return response.hits();
        } catch (IOException e) {
            var errorMsg = String.format(
                "Fehler beim Laden der Zuständigkeiten mit lbIds '%s' und Ars '%s'",
                StringUtils.abbreviate(lbIds.toString(), 1024),
                ars
            );
            throw new ElasticSearchException(ErrorCodeEnum.ELSE_IO_ERROR, errorMsg, e);
        }
    }

    @SuppressWarnings("java:S107") // refactoring wäre hier zu aufwendig
    public SearchResponse<Verwaltungsleistung> searchServiceDescription(
        Collection<String> leikaIds,
        @NonNull Collection<String> searchValues,
        Set<String> typingsToIgnore,
        String ars,
        boolean useArsHierarchy,
        Set<String> lifeSituationCodes,
        PersonalMattersMode personalMattersMode,
        int page,
        int size,
        List<String> includes,
        boolean fuzzysearch,
        LocalDate validDate
    ) {
        checkSearchCriteriaPresent(leikaIds, searchValues, lifeSituationCodes);

        BoolQuery.Builder boolBuilder = QueryBuilders.bool();
        if (ObjectUtils.isNotEmpty(searchValues)) {
            Set<String> escapedSearchValues = searchValues
                .stream()
                .map(QueryHelper::escape)
                .collect(Collectors.toSet());
            addSearchValuesToBuilder(escapedSearchValues, fuzzysearch, boolBuilder);
        }

        var lifeSitQuery = lifeSituationQuery(lifeSituationCodes);
        if (lifeSitQuery != null && personalMattersMode == PersonalMattersMode.NONE) {
            boolBuilder.filter(lifeSitQuery);
        }

        if (ars != null) {
            boolBuilder.filter(getArsQuery(ars, useArsHierarchy));
        }

        if (ObjectUtils.isNotEmpty(leikaIds)) {
            boolBuilder.minimumShouldMatch("1");
            leikaIds.forEach(leikaId ->
                boolBuilder.should(
                    QueryHelper.term("leistungsbeschreibung.leikakeys", QueryHelper.escape(leikaId))
                )
            );
        }
        addValidFilter(boolBuilder, FIELD_NAME_LEISTUNG, validDate);

        var leikasToInclude = Sets.difference(properties.getLeikaTypingSet(), typingsToIgnore);
        boolBuilder.filter(QueryHelper.terms("leistungsbeschreibung.typing.code", leikasToInclude));

        SearchRequest.Builder searchBuilder = new SearchRequest.Builder();
        searchBuilder
            .from(page * size)
            .size(size)
            .sort(
                serviceDescriptionsStandardSorting(
                    QUERY_TERM_LEISTUNGSBESCHREIBUNG_ID,
                    ObjectUtils.isNotEmpty(searchValues)
                )
            )
            .query(boolBuilder.build()._toQuery());

        searchBuilder.source(s -> s.filter(QueryHelper.sourceFilter(includes, null)));

        if (lifeSitQuery != null && personalMattersMode == PersonalMattersMode.FULL) {
            searchBuilder.postFilter(lifeSitQuery);
        }

        if (personalMattersMode != PersonalMattersMode.NONE) {
            searchBuilder.aggregations(buildLifeSituationAggregation());
            searchBuilder.aggregations(buildLifeSituationVersionsAggregation());
        }

        if (StringUtils.isEmpty(ars)) {
            searchBuilder.index(indexConfiguration.getCurrentReadAliasNames());
        } else {
            searchBuilder.index(
                indexConfiguration.getCurrentReadAliasName(
                    AliasNames.getAliasNameByArs(ars.substring(0, 2))
                )
            );
        }

        try {
            SearchRequest request = searchBuilder.build();
            return client.search(request, Verwaltungsleistung.class);
        } catch (IOException e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                "Fehler bei der Suche nach Leistungsbeschreibungen.",
                e
            );
        }
    }

    public HitsMetadata<Verwaltungsleistung> searchServiceDescriptionByLeikaHierarchy(
        Set<LeikaKey> leikas,
        Set<String> typingsToIgnore,
        String ars,
        int page,
        int size,
        LocalDate validDate
    ) {
        BoolQuery.Builder queryBuilder = new BoolQuery.Builder();
        if (isNotEmpty(leikas)) {
            Set<String> escapedLeikaKeys = leikas
                .stream()
                .map(LeikaKey::getSearchKey)
                .map(QueryHelper::escape)
                .collect(Collectors.toSet());
            Query leikaHierarchyKeys = QueryHelper.terms(
                FIELD_NAME_LEISTUNG + ".leikaHierarchyKeys",
                escapedLeikaKeys
            );
            queryBuilder.filter(leikaHierarchyKeys);
        }

        BoolQuery.Builder arsQueryBuilder = QueryBuilders.bool();
        arsQueryBuilder.filter(getArsQuery(ars, true));
        queryBuilder.filter(arsQueryBuilder.build()._toQuery());

        var leikasToInclude = Sets.difference(properties.getLeikaTypingSet(), typingsToIgnore);
        queryBuilder.filter(
            QueryHelper.terms(FIELD_NAME_LEISTUNG + ".typing.code", leikasToInclude)
        );

        addValidFilter(queryBuilder, FIELD_NAME_LEISTUNG, validDate);

        Query query = queryBuilder.build()._toQuery();
        var includes = List.of(
            "id",
            FIELD_NAME_LAST_UPDATE,
            "ars",
            FIELD_NAME_LEISTUNG,
            FIELD_NAME_ONLINEDIENST,
            FIELD_NAME_SPEZIALISIERUNG,
            "organisationseinheiten.id"
        );
        SourceConfig sourceConfig = SourceConfig.of(s -> s.filter(f -> f.includes(includes)));

        SortOptions sort = SortOptions.of(so ->
            so.field(f -> f.field(FIELD_NAME_LEISTUNG + ".id").order(SortOrder.Asc))
        );

        try {
            return client
                .search(
                    s ->
                        s
                            .index(
                                indexConfiguration.getCurrentReadAliasName(
                                    AliasNames.getAliasNameByArs(ars.substring(0, 2))
                                )
                            )
                            .query(query)
                            .from(page * size)
                            .source(sourceConfig)
                            .size(size)
                            .sort(sort),
                    Verwaltungsleistung.class
                )
                .hits();
        } catch (Exception e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                messageSource.getMessage(
                    "elastic_read_connector.exception_search_leika_hierarchy_error",
                    null,
                    Locale.GERMAN
                ),
                e
            );
        }
    }

    public HitsMetadata<Verwaltungsleistung> searchServiceDescriptionsByArsWithSearchAfter(
        String ars,
        LocalDate validDate,
        String lastId,
        List<String> includes,
        List<String> excludes,
        int size
    ) {
        BoolQuery.Builder boolQueryBuilder = QueryBuilders.bool();
        boolQueryBuilder.filter(getArsQuery(ars, true));
        addValidFilter(boolQueryBuilder, FIELD_NAME_LEISTUNG, validDate);
        Query query = boolQueryBuilder.build()._toQuery();

        String indexName = indexConfiguration.getCurrentReadAliasName(
            AliasNames.getAliasNameByArs(ars.substring(0, 2))
        );
        SourceConfig config = SourceConfig.of(s ->
            s.filter(f -> f.includes(includes).excludes(excludes))
        );
        SortOptions sort = SortOptions.of(s ->
            s.field(v -> v.field(QUERY_TERM_LEISTUNGSBESCHREIBUNG_ID).order(SortOrder.Asc))
        );

        return searchAfter(indexName, lastId, size, query, config, sort, Verwaltungsleistung.class);
    }

    public List<CompletionSuggest<ArsObject>> suggestLocation(
        String text,
        int size,
        boolean fuzzy
    ) {
        try {
            String escapedText = QueryHelper.escape(text);

            SuggestFuzziness fuzziness = fuzzy
                ? SuggestFuzziness.of(sf -> sf.fuzziness("1"))
                : SuggestFuzziness.of(sf -> sf.fuzziness("0"));

            CompletionSuggester completionSuggester = CompletionSuggester.of(s ->
                s.field("suggest").fuzzy(fuzziness).size(Math.max(REQUIRED_LOC_COMPL_HITS, size))
            );

            Suggester suggester = Suggester.of(s ->
                s.suggesters("suggest-location", fs ->
                    fs.prefix(escapedText).completion(completionSuggester)
                )
            );

            SearchRequest request = SearchRequest.of(r ->
                r
                    .index(indexConfiguration.getCurrentIndexName(IndexNames.ARS))
                    .sort(s -> s.score(v -> v))
                    .sort(s -> s.field(v -> v.field("weight").order(SortOrder.Desc)))
                    .suggest(suggester)
            );

            SearchResponse<ArsObject> search = client.search(request, ArsObject.class);
            return search
                .suggest()
                .getOrDefault("suggest-location", List.of())
                .stream()
                .filter(Suggestion::isCompletion)
                .map(Suggestion::completion)
                .toList();
        } catch (IOException e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                String.format(
                    "Bei der Autovervollständigung von Orten ist ein Fehler aufgetreten, text='%s'",
                    text
                ),
                e
            );
        }
    }

    public List<CompletionSuggest<LbSuggestion>> suggestServiceDescription(
        String text,
        String ars,
        boolean skipDuplicates,
        Locale locale
    ) {
        String escapedText = QueryHelper.escape(text);
        String escapedArs = QueryHelper.escape(ars);
        String arsLangCategory = String.format(
            "%s-%s",
            locale.getLanguage(),
            escapedArs.substring(0, 2)
        );

        List<CompletionContext> listArs = List.of(
            CompletionContext.of(cc -> cc.context(c -> c.category(arsLangCategory))),
            CompletionContext.of(cc -> cc.context(c -> c.category(locale.getLanguage() + "-00")))
        );
        CompletionSuggester completionSuggester = CompletionSuggester.of(s ->
            s
                .field("suggest")
                .size(20)
                .skipDuplicates(skipDuplicates)
                .contexts("langArsFederalstate", listArs)
        );

        Suggester suggester = Suggester.of(s ->
            s.suggesters("suggest-servicedescription", cs ->
                cs.text(escapedText).completion(completionSuggester)
            )
        );

        SearchRequest request = SearchRequest.of(r ->
            r
                .index(indexConfiguration.getCurrentIndexName(IndexNames.SUGGESTIONS))
                .suggest(suggester)
                .sort(s -> s.score(v -> v))
        );

        try {
            indexRefreshMgr.ensureFresh(IndexNames.SUGGESTIONS);
            SearchResponse<LbSuggestion> response = client.search(request, LbSuggestion.class);
            return response
                .suggest()
                .getOrDefault("suggest-servicedescription", List.of())
                .stream()
                .filter(Suggestion::isCompletion)
                .map(Suggestion::completion)
                .toList();
        } catch (IOException e) {
            String errMsg = String.format(
                "Bei der Autovervollständigung von Leistungsbeschreibungen ist ein Fehler aufgetreten, text='%s', ars='%s'",
                text,
                ars
            );
            throw new ElasticSearchException(ErrorCodeEnum.ELSE_IO_ERROR, errMsg, e);
        }
    }

    private static Query getArsQuery(String ars, boolean useArsHierarchy) {
        if (useArsHierarchy) {
            return arsQueryWithBund(StringUtils.rightPad(ars, 12, "0"));
        }
        return QueryHelper.terms("exactArs", List.of(StringUtils.rightPad(ars, 12, "0")));
    }

    private static Query arsQueryWithBund(String ars) {
        return QueryHelper.terms(QUERY_TERM_ARS, List.of(ars, ARS.BUND));
    }

    private void addValidFilter(BoolQuery.Builder builder, LocalDate validDate) {
        addValidFilter(builder, null, validDate);
    }

    private void addValidFilter(BoolQuery.Builder builder, String objectName, LocalDate validDate) {
        if (validDate != null) {
            String field = StringUtils.isEmpty(objectName)
                ? "validityPeriods"
                : objectName + ".validityPeriods";

            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String date = validDate.format(timeFormatter);

            Query query = QueryBuilders.range(r ->
                r.date(d -> d.field(field).gte(date).lte(date).relation(RangeRelation.Contains))
            );
            builder.filter(query);
        }
    }

    private Map<String, Aggregation> buildLifeSituationAggregation() {
        Aggregation e6 = Aggregation.of(a ->
            a.terms(t ->
                t.field("leistungsbeschreibung.lifeSituations.children.children.code").size(100)
            )
        );
        Aggregation e5 = Aggregation.of(a ->
            a
                .nested(n -> n.path("leistungsbeschreibung.lifeSituations.children.children"))
                .aggregations("e6", e6)
        );
        Aggregation e4 = Aggregation.of(a ->
            a
                .terms(t -> t.field("leistungsbeschreibung.lifeSituations.children.code").size(100))
                .aggregations("e5", e5)
        );
        Aggregation e3 = Aggregation.of(a ->
            a
                .nested(n -> n.path("leistungsbeschreibung.lifeSituations.children"))
                .aggregations("e4", e4)
        );
        Aggregation e2 = Aggregation.of(a ->
            a
                .terms(t -> t.field("leistungsbeschreibung.lifeSituations.code").size(10))
                .aggregations("e3", e3)
        );
        Aggregation root = Aggregation.of(a ->
            a.nested(n -> n.path("leistungsbeschreibung.lifeSituations")).aggregations("e2", e2)
        );
        return Map.of("root", root);
    }

    private Map<String, Aggregation> buildLifeSituationVersionsAggregation() {
        Aggregation aggregation = Aggregation.of(a ->
            a.terms(t ->
                t
                    .field("leistungsbeschreibung.lifeSituationVersions")
                    .order(List.of(NamedValue.of("_key", SortOrder.Desc)))
                    .size(10)
            )
        );
        return Map.of(FIELD_NAME_VERSIONS, aggregation);
    }

    private void checkSearchCriteriaPresent(
        Collection<String> leikaIds,
        Collection<String> searchValues,
        Set<String> lifeSituationCodes
    ) {
        if (
            searchValues.isEmpty() &&
            (leikaIds == null || leikaIds.isEmpty()) &&
            (lifeSituationCodes == null || lifeSituationCodes.isEmpty())
        ) {
            throw new SuchenException(
                ErrorCodeEnum.BAD_REQUEST,
                messageSource.getMessage(
                    "elastic_read_connector.exception_bad_request_error",
                    null,
                    Locale.GERMAN
                )
            );
        }
    }

    private void addSearchValuesToBuilder(
        Collection<String> searchValues,
        boolean fuzzysearch,
        BoolQuery.Builder boolBuilder
    ) {
        List<Query> list = searchValues
            .stream()
            .filter(StringUtils::isNotBlank)
            .map(s -> searchValueBuilder(s, fuzzysearch))
            .toList();
        boolBuilder.must(list);
    }

    private Query lifeSituationQuery(Set<String> lifeSituationCodes) {
        if (lifeSituationCodes != null && !lifeSituationCodes.isEmpty()) {
            return QueryHelper.terms(
                "leistungsbeschreibung.lifeSituationCodes",
                lifeSituationCodes
            );
        }
        return null;
    }

    private static List<SortOptions> serviceDescriptionsStandardSorting(
        String sortId,
        boolean addSortByScore
    ) {
        if (addSortByScore) {
            return List.of(
                SortOptions.of(s -> s.score(v -> v)),
                SortOptions.of(s -> s.field(v -> v.field(sortId).order(SortOrder.Asc)))
            );
        }
        return List.of(SortOptions.of(s -> s.field(v -> v.field(sortId).order(SortOrder.Asc))));
    }

    private Query searchValueBuilder(String searchValue, boolean fuzzysearch) {
        BoolQuery.Builder bool = QueryBuilders.bool();

        if (fuzzysearch) {
            bool.should(searchFieldQuery(1F, FUZZY_FIVE_EIGHT, searchValue));
            bool.should(searchFieldQuery(2F, FUZZY_ZERO, searchValue));
        } else {
            bool.should(searchFieldQuery(1F, FUZZY_ZERO, searchValue));
        }
        return bool.build()._toQuery();
    }

    private Query searchFieldQuery(float boostMultiplikator, String fuzziness, String searchValue) {
        final SuchdienstProperties.Boost boost = properties.getBoost();

        MultiMatchQuery multiMatchQuery = QueryBuilders.multiMatch()
            .query(searchValue)
            .fields(
                createBoostField("searchTerms.de.searchNames", boost.getName()),
                createBoostField(
                    "searchTerms.de.searchNames.decompounder",
                    boost.getDecompounder()
                ),
                createBoostField("searchTerms.de.searchValues", boost.getSynonyms()),
                createBoostField("searchTerms.de.searchDescriptions", boost.getDescription()),
                createBoostField("searchTerms.en.searchNames", boost.getEnglish()),
                createBoostField("searchTerms.en.searchValues", boost.getEnglish()),
                createBoostField("searchTerms.en.searchDescriptions", boost.getEnglish())
            )
            .fuzziness(fuzziness)
            .operator(Operator.And)
            .boost(boostMultiplikator)
            .build();
        return multiMatchQuery._toQuery();
    }

    String createBoostField(String field, float boost) {
        return String.format(
            Locale.US,
            "%s^%s",
            field,
            new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US)).format(boost)
        );
    }

    private Query createLbByOeIdArsQuery(
        String lbId,
        String oeId,
        String ars,
        LocalDate validDate
    ) {
        BoolQuery.Builder queryBuilderBool = new BoolQuery.Builder();
        if (StringUtils.isNotBlank(lbId)) {
            Query queryLbIds = QueryHelper.term(FIELD_NAME_LEISTUNG + ".id", lbId);
            queryBuilderBool.filter(queryLbIds);
        }
        if (StringUtils.isNotBlank(oeId)) {
            Query queryOeIds = QueryHelper.term(FIELD_NAME_ORGANISATIONSEINHEITEN + ".id", oeId);
            queryBuilderBool.filter(queryOeIds);
        }
        if (StringUtils.isNotBlank(ars)) {
            Query queryArs = arsQueryWithBund(ars);
            queryBuilderBool.filter(queryArs);
        }
        addValidFilter(queryBuilderBool, FIELD_NAME_ORGANISATIONSEINHEITEN, validDate);
        addValidFilter(queryBuilderBool, FIELD_NAME_LEISTUNG, validDate);
        return queryBuilderBool.build()._toQuery();
    }

    private <T> HitsMetadata<T> searchAfter(
        String index,
        String fromId,
        int size,
        @NonNull Query query,
        SourceConfig config,
        SortOptions sort,
        Class<T> clazz
    ) {
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        searchRequestBuilder.index(index).size(size).query(query).source(config).sort(sort);
        if (isNotEmpty(fromId)) {
            searchRequestBuilder.searchAfter(FieldValue.of(fromId));
        }
        searchRequestBuilder.trackTotalHits(t -> t.enabled(false));
        SearchRequest searchRequest = searchRequestBuilder.build();

        try {
            indexRefreshMgr.ensureFresh(index);
            SearchResponse<T> response = client.search(searchRequest, clazz);
            return response.hits();
        } catch (Exception e) {
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
}
