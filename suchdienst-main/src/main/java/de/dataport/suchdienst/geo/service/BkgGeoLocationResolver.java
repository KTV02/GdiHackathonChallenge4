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
package de.dataport.suchdienst.geo.service;

import static de.dataport.suchdienst.geo.util.ArsSearchUtils.buildBkgArsSearchParam;

import de.dataport.suchdienst.common.model.ArsHierarchyEbene;
import de.dataport.suchdienst.config.CacheConfiguration;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.geo.model.internal.GeoLocation;
import de.dataport.suchdienst.geo.model.internal.GeoLocationArs;
import de.dataport.suchdienst.geo.model.internal.GeoLocationDetail;
import de.dataport.suchdienst.geo.model.internal.HouseAddress;
import de.dataport.suchdienst.geo.model.out.GeoSuggestionOutModel;
import de.dataport.suchdienst.indexer.model.mapping.Address;
import de.dataport.suchdienst.suchen.exception.SuchenException;
import de.dataport.suchdienst.util.Exceptions;
import de.dataport.suchdienst.util.MdcTemplate;
import de.dataport.suchdienst.util.StreamUtils;
import de.dataport.suchdienst.util.model.ExternerBereich;
import de.dataport.suchdienst.util.model.MdcKeys;
import de.dataport.suchdienst.util.model.ProtocolType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * Führt Geosearch und Geocoding anhand des Webdienstes vom Bundesamt für Kartografie und Geodäsie aus.
 *
 * @see <a href="https://sg.geodatenzentrum.de/web_public/gdz/dokumentation/deu/geokodierungsdienst.pdf">geodatenzentrum.de</a>
 */
@Slf4j
@Service
public class BkgGeoLocationResolver {

    public static final String TYP_ORT = "typ:ort";
    public static final String TYP_GEONAME = "typ:geoname";

    private static final Map<String, String> GEODATEN_MDC = Map.of(
        MdcKeys.PROTOKOLLTYP,
        ProtocolType.EXTERN.name(),
        MdcKeys.EXTERNER_BEREICH,
        ExternerBereich.GEODATEN.name()
    );

    private final RestTemplate rest;
    private final SuchdienstProperties props;
    private final BkgQueryProvider queryProvider;
    private final CodelistDownloader codelistDownloader;
    private final Executor executor;

    public BkgGeoLocationResolver(
        @Qualifier("bkgRestTemplate") RestTemplate rest,
        SuchdienstProperties props,
        BkgQueryProvider queryProvider,
        CodelistDownloader codelistDownloader,
        @Qualifier("ioExecutor") Executor executor
    ) {
        this.rest = rest;
        this.props = props;
        this.queryProvider = queryProvider;
        this.codelistDownloader = codelistDownloader;
        this.executor = executor;

        if (
            props.getBkg().getKeyResolutionTimeout().toSeconds() <
            props.getBkg().getRequestTimeout().toSeconds()
        ) {
            log.warn(
                "Konfigurationsfehler: 'keyResolutionTimeout' ist niedriger als 'requestTimeout'."
            );
        }
    }

    @Cacheable(
        unless = "#result == null",
        cacheNames = CacheConfiguration.BKG_CACHE_NAME,
        keyGenerator = "addressKeyGenerator",
        cacheManager = CacheConfiguration.REDIS_MGR
    )
    public Optional<GeoLocation> resolve(Address address) {
        try (var _ = MdcTemplate.withProtokolltyp(ProtocolType.EXTERN)) {
            return resolveGeoLocation(address);
        }
    }

    private Optional<GeoLocation> resolveGeoLocation(Address address) {
        try {
            var query = this.queryProvider.getForwardGeocodeQuery();
            var params = this.queryProvider.getForwardGeocodeSearchParams(address);

            if (params.isEmpty()) {
                return Optional.empty();
            }

            log.debug("BKG Request: {}", query);
            var bkgFeatures = getBkgFeatures(query, params);

            if (bkgFeatures.isPresent()) {
                for (BkgFeature feature : bkgFeatures.get()) {
                    var geoLoc = mapBkgGeometry(feature.getGeometry());
                    if (geoLoc.isPresent()) {
                        return geoLoc;
                    }
                }
            }
            log.warn("EXTERN: Geo-Koordinaten wurden nicht gefunden. {}", address);
        } catch (Exception e) {
            // PV-8946 catch-log-skip
            Exceptions.findCause(e, InterruptedException.class).ifPresent(ex ->
                Thread.currentThread().interrupt()
            );
            log.warn(
                "EXTERN - FEHLER: Ermitteln von Geo-Koordinaten fehlgeschlagen. {}",
                address,
                e
            );
        }
        return Optional.empty();
    }

    @Cacheable(
        unless = "#result==null or #result.isEmpty()",
        cacheNames = CacheConfiguration.BKG_SUGGESTION_CACHE_NAME,
        keyGenerator = "suggestionCacheKeyGenerator",
        cacheManager = CacheConfiguration.REDIS_MGR
    )
    public List<GeoSuggestionOutModel> suggest(String input, int count) {
        try (var _ = MdcTemplate.withProtokolltyp(ProtocolType.EXTERN)) {
            return requestSuggestions(input, count);
        }
    }

    private List<GeoSuggestionOutModel> requestSuggestions(String input, int count) {
        var query = this.queryProvider.getSuggestionSearchQuery();
        var params = this.queryProvider.getDefaultSearchParams(input, count, TYP_ORT);

        try {
            ResponseEntity<BkgSuggestion[]> response = rest.getForEntity(
                query,
                BkgSuggestion[].class,
                params
            );

            if (response.getStatusCode() == HttpStatus.OK && response.hasBody()) {
                BkgSuggestion[] body = response.getBody();
                logWithHttpStatus(response.getStatusCode(), () ->
                    log.debug(
                        "BKG Suggest-Ergebnis für query='{}', filter='{}', count={}: {}",
                        input,
                        TYP_ORT,
                        count,
                        Arrays.toString(body)
                    )
                );

                if (body != null) {
                    return (
                        Arrays.stream(body)
                            .map(s -> new GeoSuggestionOutModel(s.suggestion, s.score))
                            // Achtung hier muss eine mutable List erzeugt werden, da sonst keine Deserialisierung mit Redis möglich ist
                            // https://github.com/spring-projects/spring-data-redis/issues/2697
                            .collect(Collectors.toCollection(ArrayList::new))
                    );
                }
            }
        } catch (Exception e) {
            logWithHttpStatus(e, () ->
                log.warn("BKG - Fehler beim Abruf: {}, {}: {}", query, params, e.getMessage(), e)
            );
            throw new SuchenException(
                ErrorCodeEnum.GEO_REQUEST_ERROR,
                "Bei der Anfrage an den Geodienst trat ein Fehler auf."
            );
        }
        return Collections.emptyList();
    }

    public List<GeoLocationDetail> searchReverse(GeoLocation location) {
        var query = this.queryProvider.getReverseGeocodeQuery();
        var params = this.queryProvider.getReverseGeocodeSearchParams(location);
        List<GeoLocationDetail> geoLocationDetailList = new ArrayList<>();
        log.debug("BKG Request: {}", query);
        var bkgFeatures = getBkgFeatures(query, params);

        if (bkgFeatures.isPresent()) {
            for (BkgFeature feature : bkgFeatures.get()) {
                if (feature.getProperties() != null) {
                    geoLocationDetailList.add(mapBkgFeature(feature));
                }
            }
        }
        return geoLocationDetailList;
    }

    @Cacheable(
        unless = "#result==null or #result.isEmpty()",
        keyGenerator = "locationCacheKeyGenerator",
        cacheNames = CacheConfiguration.BKG_SEARCH_ARS_CACHE_NAME,
        cacheManager = CacheConfiguration.REDIS_MGR
    )
    public List<GeoLocationDetail> searchByArs(String ars, int count, boolean filterByPlz) {
        var query = this.queryProvider.getGeoSearchArsQuery();
        var searchArs = buildBkgArsSearchParam(
            ars,
            codelistDownloader.getLatestDestatisArsCodelist()
        );
        var filter =
            searchArs.getArsEbene() == ArsHierarchyEbene.BUNDESLAND ? TYP_GEONAME : TYP_ORT;
        var params = this.queryProvider.getSearchByArsParams(searchArs, count, filter, true);

        logOngoingBkgRequest(params);
        List<GeoLocationDetail> geoLocationDetails = getBkgFeatures(query, params)
            .stream()
            .flatMap(bkgFeatures ->
                bkgFeatures
                    .stream()
                    .map(this::mapBkgFeature)
                    .filter(d -> !filterByPlz || d.getArs().getPlz() != null)
            )
            .toList();
        // Achtung hier muss eine mutable List erzeugt werden, da sonst keine Deserialisierung mit Redis möglich ist
        // https://github.com/spring-projects/spring-data-redis/issues/2697
        return new ArrayList<>(geoLocationDetails);
    }

    @Cacheable(
        unless = "#result==null or #result.isEmpty()",
        keyGenerator = "locationCacheKeyGenerator",
        cacheNames = CacheConfiguration.BKG_SEARCH_QUERY_CACHE_NAME,
        cacheManager = CacheConfiguration.REDIS_MGR
    )
    public List<GeoLocationDetail> searchByQuery(
        String searchQuery,
        int count,
        boolean filterByOrt,
        boolean filterByPlz
    ) {
        var filter = filterByOrt ? TYP_ORT : "!" + TYP_GEONAME;
        var query = this.queryProvider.getGeoSearchQuery();
        var params = this.queryProvider.getDefaultSearchParams(searchQuery, count, filter);

        List<GeoLocationDetail> geoLocationDetails = getBkgFeatures(query, params)
            .map(res -> flatResolveByKeysOfBkgResponse(res, count, filterByPlz, filter))
            .orElse(Collections.emptyList());
        // Achtung hier muss eine mutable List erzeugt werden, da sonst keine Deserialisierung mit Redis möglich ist
        // https://github.com/spring-projects/spring-data-redis/issues/2697
        return new ArrayList<>(geoLocationDetails);
    }

    @Cacheable(
        unless = "#result==null or #result.isEmpty()",
        keyGenerator = "locationCacheKeyGenerator",
        cacheNames = CacheConfiguration.BKG_SEARCH_ADDRESS_CACHE_NAME,
        cacheManager = CacheConfiguration.REDIS_MGR
    )
    public List<GeoLocationDetail> searchByAddress(
        HouseAddress address,
        int count,
        boolean filterByPlz,
        String filter
    ) {
        var query = this.queryProvider.geoSearchStructuredQuery(address.city() != null);
        var params = this.queryProvider.getStructuredQueryParams(address, count);

        logOngoingBkgRequest(params);

        var geoLocationDetailList = getBkgFeatures(query, params)
            .map(res -> flatResolveByKeysOfBkgResponse(res, count, filterByPlz, filter))
            .orElse(Collections.emptyList());
        return new ArrayList<>(geoLocationDetailList);
    }

    private static void logOngoingBkgRequest(Map<String, String> params) {
        log.debug("Issuing BKG request {}", params);
    }

    /**
     * Loggt mit dem HTTP-Status der externen Antwort. Der Status steht erst nach dem Aufruf fest und
     * ist nur fuer diese eine Ausgabe relevant, deshalb gilt er auch nur fuer sie.
     */
    private static void logWithHttpStatus(HttpStatusCode statusCode, Runnable logAction) {
        MdcTemplate.runWith(MdcKeys.HTTP_STATUS, String.valueOf(statusCode.value()), logAction);
    }

    /**
     * Wie {@link #logWithHttpStatus(HttpStatusCode, Runnable)}, nimmt den Status aber aus dem Fehler.
     * Bringt dieser keinen mit (z.B. Timeout), wird ohne den Schluessel geloggt.
     */
    private static void logWithHttpStatus(Throwable error, Runnable logAction) {
        if (error instanceof HttpStatusCodeException httpEx) {
            logWithHttpStatus(httpEx.getStatusCode(), logAction);
        } else {
            logAction.run();
        }
    }

    private Optional<List<BkgFeature>> getBkgFeatures(String query, Map<String, String> params) {
        try (var _ = MdcTemplate.scoped(GEODATEN_MDC)) {
            return requestBkgFeatures(query, params);
        }
    }

    private Optional<List<BkgFeature>> requestBkgFeatures(
        String query,
        Map<String, String> params
    ) {
        try {
            ResponseEntity<BkgResponse> response = rest.getForEntity(
                query,
                BkgResponse.class,
                params
            );

            if (response.getStatusCode() == HttpStatus.OK && response.hasBody()) {
                BkgResponse body = response.getBody();
                logWithHttpStatus(response.getStatusCode(), () ->
                    log.debug("BKG Geosearch-Ergebnis für params={}: {}", params, body)
                );
                return Optional.ofNullable(body).map(BkgResponse::getFeatures);
            } else if (response.getStatusCode() != HttpStatus.OK) {
                logWithHttpStatus(response.getStatusCode(), () ->
                    log.warn(
                        "EXTERN: BKG - Fehler beim Abruf: {}, {}: {}",
                        query,
                        params,
                        response.getStatusCode()
                    )
                );
                throw new SuchenException(
                    ErrorCodeEnum.GEO_REQUEST_ERROR,
                    "Fehler beim Abrufen von Informationen beim BKG."
                );
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            logWithHttpStatus(e, () ->
                log.warn(
                    "EXTERN - FEHLER: BKG - Fehler beim Abruf: {}, {}: {}",
                    query,
                    params,
                    e.getMessage(),
                    e
                )
            );
            throw new SuchenException(
                ErrorCodeEnum.GEO_REQUEST_ERROR,
                "Fehler beim Abrufen von Informationen beim BKG."
            );
        }
    }

    /**
     * Erzeugt auf Basis der BKG-Schlüssel neue Requests und merged diese mit den ursprünglichen. Dies soll in Randfällen
     * wie z.B. Dannewerk die umliegenden und trotzdem irgendwie zugehörigen Gemeinden zusätzlich finden.
     */
    private List<GeoLocationDetail> flatResolveByKeysOfBkgResponse(
        List<BkgFeature> bkgFeatures,
        int count,
        boolean filterByPlz,
        String filter
    ) {
        try (var _ = MdcTemplate.scoped(GEODATEN_MDC)) {
            return requestFlatResolveByKeys(bkgFeatures, count, filterByPlz, filter);
        }
    }

    private List<GeoLocationDetail> requestFlatResolveByKeys(
        List<BkgFeature> bkgFeatures,
        int count,
        boolean filterByPlz,
        String filter
    ) {
        var query = this.queryProvider.getGeoSearchQuery();
        Predicate<GeoLocationDetail> plzFilter = (GeoLocationDetail d) ->
            !filterByPlz || d.getArs().getPlz() != null;

        try {
            Supplier<Stream<GeoLocationDetail>> resultStream = () ->
                bkgFeatures.stream().map(this::mapBkgFeature);
            var plzFilteredResults = resultStream.get().filter(plzFilter).toList();

            // Ist die Ergebnismenge groß genug, sind weitere Anfragen an den BKG unnötig
            // und das vorherige Ergebnis kann verwendet werden
            if (plzFilteredResults.size() >= count) {
                return plzFilteredResults;
            }

            var arsStream = resultStream
                .get()
                .flatMap(geoDetail -> geoDetail.getArs().getTailoredDistinctKeys().stream());

            // Zusammenfassung zu Pairs, um mithilfe des OR Operators die Request-Anzahl zu halbieren
            var futures = StreamUtils.toPairs(arsStream)
                .stream()
                .map(pair ->
                    queryProvider.getSearchByArsParams(pair.getLeft(), pair.getRight(), 0, filter)
                )
                .map(params ->
                    getBkgResponseAsync(query, params)
                        // Das RestTemplate sollte bereits ein Timeout generieren und zum folgenden "exceptionally" führen.
                        // Zur Absicherung gegen Änderungen am RestTemplate wird hier ein weiteres Timeout verwendet.
                        .orTimeout(props.getBkg().getRequestTimeout().toSeconds(), TimeUnit.SECONDS)
                        .exceptionally(e -> null)
                )
                .toList();

            var combinedResponses = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            )
                .thenApply(v -> futures.stream().map(CompletableFuture::join).toList())
                .completeOnTimeout(
                    Collections.emptyList(),
                    props.getBkg().getKeyResolutionTimeout().toSeconds(),
                    TimeUnit.SECONDS
                )
                .thenApply(bkgResponses ->
                    bkgResponses
                        .stream()
                        .filter(Objects::nonNull)
                        .flatMap(bkgFeat -> bkgFeat.getFeatures().stream())
                        .map(this::mapBkgFeature)
                        .filter(plzFilter)
                )
                .thenApply(nestedGeoDetails ->
                    Stream.concat(plzFilteredResults.stream(), nestedGeoDetails).toList()
                );
            return combinedResponses.get();
        } catch (Exception e) {
            log.warn(
                "EXTERN - FEHLER: BKG - Fehler beim Abruf und flachen Auflösen: {}: {}",
                query,
                e.getMessage(),
                e
            );
            // Bewusste Entscheidung, dass in einem Fehlerfall trotzdem noch die vorher gefundenen Ergebnisse geliefert werden können
            return bkgFeatures.stream().map(this::mapBkgFeature).toList();
        }
    }

    private CompletableFuture<BkgResponse> getBkgResponseAsync(
        String query,
        Map<String, String> queryMap
    ) {
        return CompletableFuture.supplyAsync(() -> {
            logOngoingBkgRequest(queryMap);
            ResponseEntity<BkgResponse> response = rest.getForEntity(
                query,
                BkgResponse.class,
                queryMap
            );
            return response.getBody();
        }, executor);
    }

    private GeoLocationDetail mapBkgFeature(BkgFeature feature) {
        var geometry = feature.getGeometry();
        var properties = feature.getProperties();
        var name = feature.getProperties().getTyp().equals("Geoname")
            ? feature.getProperties().getName()
            : StringUtils.join(
                  properties.getOrt(),
                  StringUtils.SPACE,
                  Optional.ofNullable(properties.getOrtsteil())
                      .map(ot -> StringUtils.wrap(ot, "'"))
                      .orElse("")
              ).trim();
        var keys = Stream.of(Optional.ofNullable(properties.getSchluessel()).orElse("").split(","))
            .map(String::trim)
            .toList();

        var ars = GeoLocationArs.builder()
            .name(name)
            .ars(properties.getRs())
            .municipality(properties.getGemeinde())
            .weight(properties.getScore())
            .keys(keys)
            .plz(properties.getPlz())
            .street(properties.getStrasse())
            .housenumber(properties.haus)
            .build();

        var loc = mapBkgGeometry(geometry).orElse(null);
        return new GeoLocationDetail(feature.getId(), ars, loc);
    }

    /**
     * Extrahiert und validiert Koordinaten aus BkgGeometry.
     * Koordinaten werden als Strings zurückgegeben: [longitude, latitude].
     * Dies ist die Single Source of Truth für Koordinaten-Validierung.
     *
     * @param geometry BKG Geometrie mit Koordinaten
     * @return Optional mit String-Array [longitude, latitude] oder empty wenn ungültig
     */
    private Optional<String[]> extractAndValidateCoordinates(BkgGeometry geometry) {
        return Optional.ofNullable(geometry)
            .filter(g -> g.getCoordinates().size() == 2)
            .filter(g -> {
                try {
                    Double.valueOf(g.getCoordinates().getFirst());
                    Double.valueOf(g.getCoordinates().get(1));
                    return true;
                } catch (NumberFormatException e) {
                    log.debug(
                        "BKG-Koordinate konnte nicht geparsed werden: {}",
                        g.getCoordinates(),
                        e
                    );
                    return false;
                }
            })
            .map(g -> new String[] { g.getCoordinates().getFirst(), g.getCoordinates().get(1) });
    }

    /**
     * Extrahiert validierte Koordinaten aus BkgGeometry und erzeugt ein GeoLocation-Objekt.
     * Wird primär von der resolve()-Methode für XZuFi-Kompatibilität verwendet.
     * Nutzt intern extractAndValidateCoordinates() zur Validierung.
     *
     * @param geometry BKG Geometrie mit Koordinaten
     * @return Optional mit GeoLocation-Objekt oder empty wenn ungültig
     */
    private Optional<GeoLocation> mapBkgGeometry(BkgGeometry geometry) {
        return extractAndValidateCoordinates(geometry).map(c ->
            new GeoLocation(c[0], c[1], GeoLocation.EPSG_4326)
        );
    }

    @Data
    static class BkgResponse {

        private List<BkgFeature> features = new ArrayList<>();
    }

    @Data
    static class BkgFeature {

        private String id;
        private BkgGeometry geometry;
        private BkgProperties properties;
    }

    @Data
    static class BkgGeometry {

        private String type;
        private List<String> coordinates = new ArrayList<>();
    }

    @Data
    static class BkgProperties {

        private String rs;
        private String plz;
        private String typ;
        private String ort;
        private String strasse;
        private String haus;
        private String ortsteil;
        private String gemeinde;
        private float score;
        private String schluessel;
        private String name;
    }

    @Data
    static class BkgSuggestion {

        private String suggestion;
        private String type;
        private float score;
        private int count;
        private String highlighted;
    }
}
