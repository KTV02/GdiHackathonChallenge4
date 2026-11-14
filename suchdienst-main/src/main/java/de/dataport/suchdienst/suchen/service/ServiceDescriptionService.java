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
package de.dataport.suchdienst.suchen.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.AggregateVariant;
import co.elastic.clients.elasticsearch._types.aggregations.NestedAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import de.dataport.suchdienst.common.model.LeikaKey;
import de.dataport.suchdienst.config.CacheConfiguration;
import de.dataport.suchdienst.elasticsearch.api.ElasticPublicSearchConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.elasticsearch.api.model.PersonalMattersMode;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.importer.model.GenericLeistung;
import de.dataport.suchdienst.importer.model.GenericModelMapper;
import de.dataport.suchdienst.importer.model.jzufi.LeistungEntity;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.Verwaltungsleistung;
import de.dataport.suchdienst.indexer.service.LeikaService;
import de.dataport.suchdienst.suchen.exception.SuchenException;
import de.dataport.suchdienst.suchen.model.out.leistung.AbstractLifeSituationAggOutModel;
import de.dataport.suchdienst.suchen.model.out.leistung.FullLifeSituationAggOutModel;
import de.dataport.suchdienst.suchen.model.out.leistung.LeikaMatch;
import de.dataport.suchdienst.suchen.model.out.leistung.LeikaMatchType;
import de.dataport.suchdienst.suchen.model.out.leistung.LeikaMeta;
import de.dataport.suchdienst.suchen.model.out.leistung.LifeSituationAggOutModel;
import de.dataport.suchdienst.suchen.model.out.leistung.SdOutModelBase;
import de.dataport.suchdienst.suchen.model.out.leistung.SdOutModelV5;
import de.dataport.suchdienst.suchen.model.out.leistung.SdOutModelV7;
import de.dataport.suchdienst.suchen.model.out.leistung.SearchOutModel;
import de.dataport.suchdienst.suchen.model.out.leistung.detail.ServiceDescriptionDetail;
import de.dataport.suchdienst.suchen.model.out.leistung.resultlist.ServiceDescriptionHierarchySearchResult;
import de.dataport.suchdienst.suchen.model.out.leistung.resultlist.ServiceDescriptionSearchHit;
import de.dataport.suchdienst.suchen.model.out.leistung.resultlist.ServiceDescriptionSearchResult;
import de.dataport.suchdienst.suchen.service.legacy.DetailModuleInjection;
import de.dataport.suchdienst.suchen.service.legacy.DetailModuleTitleRenamer;
import de.dataport.suchdienst.suchen.service.legacy.DetailSorting;
import de.dataport.suchdienst.util.ResultPage;
import de.dataport.suchdienst.util.ResultPageWithMeta;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;

/**
 * handle CRUD for Service Descriptions, adding specialization information to Service Description (
 * if present )
 */
@Slf4j
@Service
public class ServiceDescriptionService {

    private final MessageSource messageSource;

    private final ElasticPublicSearchConnector publicSearchConnector;
    private final ElasticReadConnector readConnector;

    private final LeikaService leikaService;

    private final FullLifeSituationCreator fullLifeSituationCreator =
        new FullLifeSituationCreator();
    private final SlimLifeSituationCreator slimLifeSituationCreator =
        new SlimLifeSituationCreator();

    private final VerwaltungsleistungMappingService vlToOutModelService;

    public ServiceDescriptionService(
        ElasticPublicSearchConnector publicSearchConnector,
        ElasticReadConnector readConnector,
        MessageSource messageSource,
        LeikaService leikaService,
        VerwaltungsleistungMappingService vlToOutModelService
    ) {
        this.publicSearchConnector = publicSearchConnector;
        this.readConnector = readConnector;
        this.messageSource = messageSource;
        this.leikaService = leikaService;
        this.vlToOutModelService = vlToOutModelService;
    }

    public ResultPageWithMeta<SdOutModelV7, LeikaMeta> searchByLeikaHierarchy(
        Set<String> ignoreLeikaTypes,
        String ars,
        int page,
        int pageSize,
        LocalDate validDate,
        Locale language,
        List<String> leikaIds
    ) {
        final var leikas = leikaIds.stream().map(LeikaKey::new).collect(Collectors.toSet());

        final var depth = leikas.stream().map(LeikaKey::getDepth).collect(Collectors.toSet());
        if (depth.size() > 1) {
            throw new SuchenException(
                ErrorCodeEnum.BAD_REQUEST,
                messageSource.getMessage(
                    "elastic_read_connector.exception_search_leika_hierarchy_mixed_hierarchy",
                    null,
                    language
                )
            );
        }

        // zunaechst nach exakt diesen Schluesseln suchen
        var result = searchForLeikaHierarchyLevel(
            leikas,
            ignoreLeikaTypes,
            ars,
            page,
            pageSize,
            validDate,
            language,
            SdOutModelV7.class
        ).toMetaResultPage(new LeikaMeta(true));

        if (result.getTotalElements() == 0) {
            // wenn wir nichts gefunden haben suchen wir nach Treffern in der Ebene darunter, also nach
            // detaillierteren Leikas (sofern existent). Dazu machen wir eine Suche nach dem Präfix
            // der gesuchten Leikas
            var pathPrefix = leikas
                .stream()
                .map(LeikaKey::getPathPrefix)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
            if (!pathPrefix.isEmpty()) {
                result = searchForLeikaHierarchyLevel(
                    pathPrefix,
                    ignoreLeikaTypes,
                    ars,
                    page,
                    pageSize,
                    validDate,
                    language,
                    SdOutModelV7.class
                ).toMetaResultPage(new LeikaMeta(false));
            }
        }

        if (result.getTotalElements() == 0) {
            // wenn wir immer noch nichts gefunden haben suchen wir uebergeordnete leikakeys, also nach den
            // vollständigen Schlüsseln der uebergeordneten Leikas
            var parentKeys = leikas
                .stream()
                .map(LeikaKey::getParentKey)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
            // in der Hierarchie nach oben gehen, bis wir etwas finden
            while (!parentKeys.isEmpty()) {
                result = searchForLeikaHierarchyLevel(
                    parentKeys,
                    ignoreLeikaTypes,
                    ars,
                    page,
                    pageSize,
                    validDate,
                    language,
                    SdOutModelV7.class
                ).toMetaResultPage(new LeikaMeta(false));
                if (result.getTotalElements() > 0) {
                    break;
                }
                parentKeys = parentKeys
                    .stream()
                    .map(LeikaKey::getParentKey)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toSet());
            }
        }

        return result;
    }

    @SuppressWarnings("java:S107") // refactoring wäre zu aufwendig
    private <T extends SdOutModelBase> ResultPage<T> searchForLeikaHierarchyLevel(
        Set<LeikaKey> leikas,
        Set<String> typingsToIgnore,
        String ars,
        int page,
        int size,
        LocalDate validDate,
        Locale language,
        Class<T> outModel
    ) {
        HitsMetadata<Verwaltungsleistung> hits =
            publicSearchConnector.searchServiceDescriptionByLeikaHierarchy(
                leikas,
                typingsToIgnore,
                ars,
                page,
                size,
                validDate
            );

        List<T> mapped = hits
            .hits()
            .stream()
            .map(Hit::source)
            .filter(Objects::nonNull)
            .map(vl ->
                mapServiceDescriptionToOutModelBase(vl, ars, language, validDate, false, outModel)
            )
            .toList();

        long totalHits = hits.total() != null ? hits.total().value() : 0;
        return ResultPage.from(mapped, totalHits, page, size);
    }

    @SuppressWarnings("java:S107")
    public PagedModel<ServiceDescriptionSearchHit> searchHitsByLeikaId(
        Collection<String> searchValues,
        String ars,
        boolean useArsHierarchy,
        int page,
        int pageSize,
        Locale language,
        Collection<String> leikaIds
    ) {
        final LocalDate validDate = LocalDate.now();
        List<String> includes = getIncludeFields(true);
        SearchResponse<Verwaltungsleistung> searchResponse =
            publicSearchConnector.searchServiceDescription(
                leikaIds,
                searchValues,
                Set.of(),
                ars,
                useArsHierarchy,
                Set.of(),
                PersonalMattersMode.NONE,
                page,
                pageSize,
                includes,
                false,
                validDate
            );
        if (useArsHierarchy && searchResponse.hits().hits().isEmpty()) {
            searchResponse = publicSearchConnector.searchServiceDescription(
                leikaIds,
                searchValues,
                Set.of(),
                ars,
                false,
                Set.of(),
                PersonalMattersMode.NONE,
                page,
                pageSize,
                includes,
                false,
                validDate
            );
        }

        return new PagedModel<>(
            mapToSearchHitPage(searchResponse.hits(), ars, page, pageSize, language, validDate)
        );
    }

    /**
     * Service-Methode für den Beta-Endpunkt "/v3beta1/servicedescriptions/leikahierarchy".
     * <p>
     * Sucht zunächst nach den angefragten LeiKa-Schlüsseln. Bleibt die Treffermenge leer, wird in der
     * Hierarchie zuerst nach unten (detailliertere Leistungen) und anschließend schrittweise nach oben
     * (allgemeinere Leistungen) ausgewichen. Welche Stufe der Kaskade gegriffen hat, wird über
     * {@link LeikaMatch} an den Aufrufer zurückgegeben.
     * </p>
     * <p>
     * Als Abbruchkriterium der Kaskade wird bewusst die Gesamttrefferzahl und nicht der Inhalt der
     * angefragten Seite ausgewertet. Andernfalls würde eine Anfrage auf eine Seite hinter dem Ende der
     * Treffermenge fälschlich als "keine Treffer" gewertet und die Kaskade eine Ebene weiterlaufen.
     * Die gelieferte Trefferliste und das {@link LeikaMatch} sind dadurch für alle Seiten einer Suche
     * identisch und damit stabil paginierbar.
     * </p>
     */
    public ServiceDescriptionHierarchySearchResult searchHitsByLeikaHierarchy(
        String ars,
        int page,
        int pageSize,
        Locale language,
        List<String> leikaIds
    ) {
        final Set<String> ignoreLeikaTypes = Set.of();
        final LocalDate validDate = LocalDate.now();
        Set<LeikaKey> leikas = leikaIds.stream().map(LeikaKey::new).collect(Collectors.toSet());
        Set<Integer> depth = leikas.stream().map(LeikaKey::getDepth).collect(Collectors.toSet());
        if (depth.size() > 1) {
            throw new SuchenException(
                ErrorCodeEnum.BAD_REQUEST,
                messageSource.getMessage(
                    "elastic_read_connector.exception_search_leika_hierarchy_mixed_hierarchy",
                    null,
                    language
                )
            );
        }

        PageImpl<ServiceDescriptionSearchHit> result = searchHitsForLeikaHierarchyLevel(
            leikas,
            ignoreLeikaTypes,
            ars,
            page,
            pageSize,
            validDate,
            language
        );
        if (hasHits(result)) {
            return new ServiceDescriptionHierarchySearchResult(
                result,
                toLeikaMatch(LeikaMatchType.EXACT, leikas)
            );
        }

        // Keine Treffer zu den angefragten Schlüsseln: eine Ebene nach unten gehen und nach
        // detaillierteren Leikas suchen. Dazu wird das Präfix der angefragten Schlüssel verwendet.
        Set<LeikaKey> pathPrefix = leikas
            .stream()
            .map(LeikaKey::getPathPrefix)
            .flatMap(Optional::stream)
            .collect(Collectors.toSet());
        if (!pathPrefix.isEmpty()) {
            result = searchHitsForLeikaHierarchyLevel(
                pathPrefix,
                ignoreLeikaTypes,
                ars,
                page,
                pageSize,
                validDate,
                language
            );
            if (hasHits(result)) {
                return new ServiceDescriptionHierarchySearchResult(
                    result,
                    toLeikaMatch(LeikaMatchType.DESCENDANT, pathPrefix)
                );
            }
        }

        // Immer noch keine Treffer: in der Hierarchie nach oben gehen, bis etwas gefunden wird.
        Set<LeikaKey> parentKeys = leikas
            .stream()
            .map(LeikaKey::getParentKey)
            .flatMap(Optional::stream)
            .collect(Collectors.toSet());
        while (!parentKeys.isEmpty()) {
            result = searchHitsForLeikaHierarchyLevel(
                parentKeys,
                ignoreLeikaTypes,
                ars,
                page,
                pageSize,
                validDate,
                language
            );
            if (hasHits(result)) {
                return new ServiceDescriptionHierarchySearchResult(
                    result,
                    toLeikaMatch(LeikaMatchType.ANCESTOR, parentKeys)
                );
            }
            parentKeys = parentKeys
                .stream()
                .map(LeikaKey::getParentKey)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
        }

        return new ServiceDescriptionHierarchySearchResult(result, LeikaMatch.none());
    }

    /**
     * Prüft, ob eine Stufe der Hierarchie-Kaskade überhaupt Treffer geliefert hat. Ausgewertet wird die
     * Gesamttrefferzahl und nicht der Inhalt der angefragten Seite, damit das Ergebnis unabhängig von
     * der Paginierung ist.
     */
    private static boolean hasHits(PageImpl<ServiceDescriptionSearchHit> result) {
        return result.getTotalElements() > 0;
    }

    /**
     * Erzeugt das {@link LeikaMatch} zu der Kaskadenstufe, die Treffer geliefert hat.
     * <p>
     * Übergeben werden die Schlüssel der <em>Suchanfrage</em> dieser Stufe, nicht die Schlüssel der
     * gefundenen Leistungen. Dadurch ist das Ergebnis unabhängig von der ausgelieferten Seite. Die
     * Schlüssel werden auf ihre vollständige, 14-stellige Form gebracht, da beim Abstieg in der
     * Hierarchie mit verkürzten Präfixen gesucht wird.
     * </p>
     */
    private static LeikaMatch toLeikaMatch(LeikaMatchType matchType, Set<LeikaKey> effectiveKeys) {
        return new LeikaMatch(
            matchType,
            effectiveKeys.stream().map(LeikaKey::getFullKey).collect(Collectors.toSet())
        );
    }

    @SuppressWarnings("java:S107")
    private PageImpl<ServiceDescriptionSearchHit> searchHitsForLeikaHierarchyLevel(
        Set<LeikaKey> leikas,
        Set<String> typingsToIgnore,
        String ars,
        int page,
        int pageSize,
        LocalDate validDate,
        Locale language
    ) {
        HitsMetadata<Verwaltungsleistung> hits =
            publicSearchConnector.searchServiceDescriptionByLeikaHierarchy(
                leikas,
                typingsToIgnore,
                ars,
                page,
                pageSize,
                validDate
            );
        return mapToSearchHitPage(hits, ars, page, pageSize, language, validDate);
    }

    @SuppressWarnings("java:S107")
    private PageImpl<ServiceDescriptionSearchHit> mapToSearchHitPage(
        HitsMetadata<Verwaltungsleistung> hits,
        String ars,
        int page,
        int pageSize,
        Locale language,
        LocalDate validDate
    ) {
        List<ServiceDescriptionSearchHit> content = hits
            .hits()
            .stream()
            .map(Hit::source)
            .filter(Objects::nonNull)
            .map(vl -> mapToSearchHitSafely(vl, ars, language, validDate))
            .filter(Objects::nonNull)
            .toList();
        long totalHits = hits.total() != null ? hits.total().value() : 0;
        return new PageImpl<>(content, PageRequest.of(page, pageSize), totalHits);
    }

    @SuppressWarnings("java:S107") // refactoring wäre zu aufwendig
    private <T extends SdOutModelBase> SearchOutModel<T> createSearchOutModelFromResponse(
        SearchResponse<Verwaltungsleistung> searchResponse,
        String ars,
        Integer page,
        Integer pageSize,
        Locale language,
        LocalDate validDate,
        boolean applyDynamicParams,
        Class<T> clazz
    ) {
        final var lifeSitAggOutModels = createLifeSituationAggOutModelsFromResponse(
            searchResponse.aggregations(),
            language,
            slimLifeSituationCreator
        );
        var sdOutModels = createSdOutModelsFromResponse(
            searchResponse.hits(),
            ars,
            page,
            pageSize,
            language,
            validDate,
            applyDynamicParams,
            clazz
        );
        return new SearchOutModel<>(sdOutModels, lifeSitAggOutModels);
    }

    /**
     * Service-Methode für den Endpunkt "/v3/servicedescriptions/leikaid"
     * Beinhaltet einen Fallback,
     * welcher bei einer leeren Treffer-Menge eine zusätzliche Anfrage mit useArsHierarchy = false ausführt
     * <br/>
     * <br/>
     * <b>Achtung:</b> Dieser Fallback wurde explizit für FIT-Connect implementiert (PV-10359).
     * Er löst das Problem leerer Treffer-Mengen beim Aufruf mit ARS, die nicht auf Gemeinde-Ebene sind.
     * Durch diese Anpassung wird sichergestellt, dass FIT-Connect auch ohne Anpassung auf deren Seite
     * weiterhin lieferfähig bleibt.
     */
    @SuppressWarnings("java:S107") // refactoring wäre zu aufwendig
    public <T extends SdOutModelBase> ResultPage<T> searchByLeikaIds(
        Collection<String> searchValues,
        String ars,
        boolean useArsHierarchy,
        Integer page,
        Integer pageSize,
        boolean fuzzySearch,
        Set<String> ignoreLeikaTypes,
        Locale language,
        Collection<String> leikaIds,
        boolean withOrgIds,
        LocalDate validDate,
        Class<T> clazz
    ) {
        List<String> includes = getIncludeFields(withOrgIds);

        SearchResponse<Verwaltungsleistung> searchResponse =
            publicSearchConnector.searchServiceDescription(
                leikaIds,
                searchValues,
                ignoreLeikaTypes,
                ars,
                useArsHierarchy,
                Set.of(),
                PersonalMattersMode.NONE,
                page,
                pageSize,
                includes,
                fuzzySearch,
                validDate
            );
        if (useArsHierarchy && searchResponse.hits().hits().isEmpty()) {
            // Wenn eine Anfrage mit ARS-Hierarchy keine Treffermenge ergibt,
            // dann wird als Fallback ohne ARS-Hierarchy gesucht
            searchResponse = publicSearchConnector.searchServiceDescription(
                leikaIds,
                searchValues,
                ignoreLeikaTypes,
                ars,
                false,
                Set.of(),
                PersonalMattersMode.NONE,
                page,
                pageSize,
                includes,
                fuzzySearch,
                validDate
            );
        }

        return createSdOutModelsFromResponse(
            searchResponse.hits(),
            ars,
            page,
            pageSize,
            language,
            validDate,
            false,
            clazz
        );
    }

    /**
     * Laden der Trefferliste für die V7 API
     */
    @SuppressWarnings("java:S107") // refactoring wäre zu aufwendig
    public <T extends SdOutModelBase> SearchOutModel<T> search(
        Collection<String> searchValues,
        String ars,
        boolean useArsHierarchy,
        Set<String> lifeSituationCodes,
        PersonalMattersMode personalMattersMode,
        Integer page,
        Integer pageSize,
        boolean fuzzySearch,
        Set<String> ignoreLeikaTypes,
        Locale language,
        Collection<String> leikaIds,
        LocalDate validDate,
        boolean applyDynamicParams,
        Class<T> clazz
    ) {
        List<String> includes = getIncludeFields(applyDynamicParams);

        var searchResponse = publicSearchConnector.searchServiceDescription(
            leikaIds,
            searchValues,
            ignoreLeikaTypes,
            ars,
            useArsHierarchy,
            lifeSituationCodes,
            personalMattersMode,
            page,
            pageSize,
            includes,
            fuzzySearch,
            validDate
        );
        SearchOutModel<T> searchOutModelFromResponse = createSearchOutModelFromResponse(
            searchResponse,
            ars,
            page,
            pageSize,
            language,
            validDate,
            applyDynamicParams,
            clazz
        );
        // Leeren der OE-IDS, da wir sie bei dieser API nie füllen.
        // In der Zukunft sollen APIs begradigt werden. Dann liefern wir hoffentlich nicht leere Listen in Modellen aus.
        searchOutModelFromResponse
            .getServiceDescriptions()
            .getContent()
            .forEach(sd -> sd.setOrganisationIds(Set.of()));
        return searchOutModelFromResponse;
    }

    /**
     * Lädt die Trefferliste für die Beta-Such-API "/v8beta1/servicedescriptions".
     * Die Such-Methodik ist hier fest vorgegeben: Fuzzy-Suche und dynamische Parameter sind stets
     * aktiv, es werden keine Leika-Typisierungen ausgeschlossen (volle Whitelist) und als
     * Gültigkeitsdatum wird immer der Zeitpunkt der Anfrage verwendet.
     * Die Treffer werden über {@link org.springframework.data.web.PagedModel} paginiert.
     */
    @SuppressWarnings("java:S107") // refactoring wäre zu aufwendig
    public ServiceDescriptionSearchResult searchHits(
        Collection<String> searchValues,
        String ars,
        boolean useArsHierarchy,
        Set<String> lifeSituationCodes,
        PersonalMattersMode personalMattersMode,
        int page,
        int pageSize,
        Locale language,
        Collection<String> leikaIds
    ) {
        final boolean fuzzySearch = true;
        final boolean applyDynamicParams = true;
        final Set<String> ignoreLeikaTypes = Set.of();
        final LocalDate validDate = LocalDate.now();

        List<String> includes = getIncludeFields(applyDynamicParams);

        SearchResponse<Verwaltungsleistung> searchResponse =
            publicSearchConnector.searchServiceDescription(
                leikaIds,
                searchValues,
                ignoreLeikaTypes,
                ars,
                useArsHierarchy,
                lifeSituationCodes,
                personalMattersMode,
                page,
                pageSize,
                includes,
                fuzzySearch,
                validDate
            );

        Set<LifeSituationAggOutModel> facades = createLifeSituationAggOutModelsFromResponse(
            searchResponse.aggregations(),
            language,
            slimLifeSituationCreator
        );

        List<ServiceDescriptionSearchHit> content = searchResponse
            .hits()
            .hits()
            .stream()
            .map(Hit::source)
            .filter(Objects::nonNull)
            .map(vl -> mapToSearchHitSafely(vl, ars, language, validDate))
            .filter(Objects::nonNull)
            .toList();

        long totalHits =
            searchResponse.hits().total() != null ? searchResponse.hits().total().value() : 0;

        PageImpl<ServiceDescriptionSearchHit> pageResult = new PageImpl<>(
            content,
            PageRequest.of(page, pageSize),
            totalHits
        );

        return new ServiceDescriptionSearchResult(pageResult, facades);
    }

    private ServiceDescriptionSearchHit mapToSearchHitSafely(
        Verwaltungsleistung vl,
        String ars,
        Locale language,
        LocalDate validDate
    ) {
        try {
            return vlToOutModelService.mapToV8Beta1(vl, ars, language, validDate);
        } catch (Exception e) {
            /*
             * Die Exception wird protokolliert. Der fehlerhafte Datensatz wird ignoriert, damit
             * alle anderen validen Suchergebnisse weiterhin dargestellt werden.
             */
            log.warn(
                "ServiceDescription konnte nicht zum ServiceDescriptionSearchResult geparsed werden: {}",
                vl,
                e
            );
            return null;
        }
    }

    @NonNull
    private static List<String> getIncludeFields(boolean applyDynamicParams) {
        List<String> includes = new ArrayList<>(
            List.of(
                "id",
                "lastUpdate",
                "ars",
                "leistungsbeschreibung",
                "onlinedienste",
                "spezialisierungen"
            )
        );
        if (applyDynamicParams) {
            // Es werden keine kompletten OE Informationen benötigt, IDs reichen aus.
            // Formulare werden nicht verlangt
            includes.add("organisationseinheiten.id");
        }
        return includes;
    }

    @Cacheable(
        cacheNames = CacheConfiguration.CACHE_NAME_PERSONAL_MATTERS,
        cacheManager = CacheConfiguration.IN_MEM_MGR
    )
    public Set<FullLifeSituationAggOutModel> searchPersonalMatters(String ars, Locale language) {
        Map<String, Aggregate> aggregations = publicSearchConnector.searchByArs(ars);
        return createLifeSituationAggOutModelsFromResponse(
            aggregations,
            language,
            fullLifeSituationCreator
        );
    }

    public Optional<SdOutModelV5> fetchDetailV5(
        String lbid,
        String ars,
        boolean useArsHierarchy,
        Locale searchLanguage,
        boolean applyDynamicParams,
        LocalDate validDate
    ) {
        return publicSearchConnector
            .searchLbByIdAndArs(
                lbid,
                ars,
                useArsHierarchy,
                Locale.GERMAN.equals(searchLanguage),
                validDate
            )
            .map(vl ->
                mapServiceDescriptionToOutModelBase(
                    vl,
                    ars,
                    searchLanguage,
                    validDate,
                    applyDynamicParams,
                    SdOutModelV5.class
                )
            );
    }

    public Optional<GenericLeistung> fetchJzufi(String id, LocalDate validDate) {
        return readConnector
            .searchById(
                id,
                List.of(),
                List.of(),
                IndexNames.LEISTUNGSBESCHREIBUNG,
                validDate,
                LeistungEntity.class
            )
            .map(GenericModelMapper::mapLeistungEntity);
    }

    /**
     * Rekursives Auslesen der Aggregationsergebnisse der Lebenslagen-Hierarchie.
     * Hier nehmen wir für die Struktur der Anfrage (siehe ElasticReadConnector.buildLifeSituationAggregation) an,
     * dass immer eine NestedAgg mit einer darin liegenden TermsAgg geschachtelt ist, darin wiederum eine NestedAgg, rekursiv.
     */
    private <L extends AbstractLifeSituationAggOutModel<L>> Set<L> createLifeSituationAggOutModelsFromResponse(
        Map<String, Aggregate> aggregations,
        Locale locale,
        final LifeSituationCreator<L> creator
    ) {
        if (ObjectUtils.isEmpty(aggregations)) {
            return Set.of();
        }
        var rootLevelNestedAgg = aggregationsOfClass(
            aggregations,
            Aggregate::isNested,
            Aggregate::nested,
            NestedAggregate.class
        ).findFirst();

        var versionsAggregation = aggregationsOfClass(
            aggregations,
            Aggregate::isSterms,
            Aggregate::sterms,
            StringTermsAggregate.class
        ).findFirst();

        if (rootLevelNestedAgg.isPresent() && versionsAggregation.isPresent()) {
            SortedSet<String> versions = versionsAggregation
                .get()
                .buckets()
                .array()
                .stream()
                .map(StringTermsBucket::key)
                .map(FieldValue::stringValue)
                .collect(Collectors.toCollection(() -> new TreeSet<>(Collections.reverseOrder())));

            return recursiveCreateLifeSituationsFromBuckets(
                rootLevelNestedAgg.get(),
                versions,
                locale,
                creator
            );
        } else {
            return Set.of();
        }
    }

    public Optional<ServiceDescriptionDetail> fetchDetailV6(
        String lbid,
        String ars,
        boolean useArsHierarchy,
        Locale searchLanguage
    ) {
        LocalDate validDate = LocalDate.now();
        return publicSearchConnector
            .searchLbByIdAndArs(
                lbid,
                ars,
                useArsHierarchy,
                Locale.GERMAN.equals(searchLanguage),
                validDate
            )
            .map(vl -> vlToOutModelService.mapToDetailV6(vl, ars, searchLanguage, validDate));
    }

    public interface LifeSituationCreator<L extends AbstractLifeSituationAggOutModel<L>> {
        L create(long count, SortedSet<String> versions, String code, Locale locale);
    }

    private class SlimLifeSituationCreator
        implements LifeSituationCreator<LifeSituationAggOutModel>
    {

        @Override
        public LifeSituationAggOutModel create(
            long count,
            SortedSet<String> versions,
            String code,
            Locale locale
        ) {
            final var lifeSit = new LifeSituationAggOutModel();
            fillLifeSituationStandard(lifeSit, count, versions, code, locale);
            return lifeSit;
        }
    }

    private class FullLifeSituationCreator
        implements LifeSituationCreator<FullLifeSituationAggOutModel>
    {

        @Override
        public FullLifeSituationAggOutModel create(
            long count,
            SortedSet<String> versions,
            String code,
            Locale locale
        ) {
            final var lifeSit = new FullLifeSituationAggOutModel();
            fillLifeSituationStandard(lifeSit, count, versions, code, locale);
            lifeSit.setComment(leikaService.getLatestComment(versions, code, locale));
            return lifeSit;
        }
    }

    private void fillLifeSituationStandard(
        AbstractLifeSituationAggOutModel<?> lifeSit,
        long count,
        SortedSet<String> versions,
        String code,
        Locale locale
    ) {
        lifeSit.setCode(code);
        lifeSit.setCount(count);
        // Namen der LifeSituation aus der Codeliste dazu mergen:
        lifeSit.setName(leikaService.getLatestText(versions, code, locale));
    }

    private static <L extends AbstractLifeSituationAggOutModel<L>> SortedSet<L> recursiveCreateLifeSituationsFromBuckets(
        NestedAggregate nestedAgg,
        SortedSet<String> versions,
        Locale locale,
        final LifeSituationCreator<L> creator
    ) {
        // PV-9379: Sortierung nach code aus https://www.xrepository.de/details/urn:xoev-de:fim:codeliste:pvlagen
        var ret = new TreeSet<L>(Comparator.comparing(AbstractLifeSituationAggOutModel::getCode));

        aggregationsOfClass(
            nestedAgg.aggregations(),
            Aggregate::isSterms,
            Aggregate::sterms,
            StringTermsAggregate.class
        ).forEach(terms ->
            terms
                .buckets()
                .array()
                .forEach(bucket -> {
                    final var code = bucket.key().stringValue();
                    final var count = bucket.docCount();
                    final L lifeSit = creator.create(count, versions, code, locale); // PV-9431
                    aggregationsOfClass(
                        bucket.aggregations(),
                        Aggregate::isNested,
                        Aggregate::nested,
                        NestedAggregate.class
                    ).forEach(nested -> {
                        var childLifeSits = recursiveCreateLifeSituationsFromBuckets(
                            nested,
                            versions,
                            locale,
                            creator
                        );
                        lifeSit.setChildren(childLifeSits);
                    });
                    ret.add(lifeSit);
                })
        );
        return ret;
    }

    private static <T extends AggregateVariant> Stream<T> aggregationsOfClass(
        Map<String, Aggregate> aggregations,
        Predicate<Aggregate> filter,
        Function<Aggregate, AggregateVariant> extractor,
        Class<T> clazz
    ) {
        return aggregations.values().stream().filter(filter).map(extractor).map(clazz::cast);
    }

    @SuppressWarnings("java:S107") // refactoring wäre zu aufwendig
    protected <T extends SdOutModelBase> ResultPage<T> createSdOutModelsFromResponse(
        HitsMetadata<Verwaltungsleistung> searchResponse,
        String ars,
        Integer page,
        Integer pageSize,
        Locale language,
        LocalDate validDate,
        boolean applyDynamicParams,
        Class<T> clazz
    ) {
        List<T> serviceDescriptions = searchResponse
            .hits()
            .stream()
            .map(Hit::source)
            .filter(Objects::nonNull)
            .map(hit ->
                mapServiceDescriptionToOutModelBase(
                    hit,
                    ars,
                    language,
                    validDate,
                    applyDynamicParams,
                    clazz
                )
            )
            .toList();
        long totalHits = searchResponse.total() != null ? searchResponse.total().value() : 0;
        return ResultPage.from(serviceDescriptions, totalHits, page, pageSize);
    }

    public <T extends SdOutModelBase> T mapServiceDescriptionToOutModelBase(
        Verwaltungsleistung vl,
        String ars,
        Locale language,
        LocalDate validDate,
        boolean applyDynamicParams,
        Class<T> clazz
    ) {
        T outModel = null;
        try {
            if (clazz.equals(SdOutModelV5.class)) {
                outModel = clazz.cast(
                    vlToOutModelService.mapToV5(vl, ars, language, validDate, applyDynamicParams)
                );
            } else if (clazz.equals(SdOutModelV7.class)) {
                outModel = clazz.cast(
                    vlToOutModelService.mapToV7(vl, ars, language, validDate, applyDynamicParams)
                );
            } else {
                throw new IllegalArgumentException("Unknown class: " + clazz);
            }
        } catch (Exception e) {
            /*
             * Die Exception wird im Hintergrund protokolliert. Der fehlerhafte Datensatz kann daraufhin
             * analysiert und gefixt werden.
             * Für den User werden alle anderen validen Suchergebnisse weiterhin dargestellt und der fehlerhafte
             * ignoriert.
             */
            log.warn(
                "ServiceDescription konnte nicht zum SdOutModel {} geparsed werden: {}",
                clazz.getSimpleName(),
                vl,
                e
            );
        }
        if (outModel != null) {
            DetailModuleInjection.injectServiceModules(outModel, language);
            DetailModuleTitleRenamer.renameModuleTitles(outModel, language);
            DetailSorting.sortModules(outModel);
        }
        return outModel;
    }
}
