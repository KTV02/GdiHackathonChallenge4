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

import static de.dataport.suchdienst.geo.service.BkgQueryProvider.REQUEST_COUNT_OVERFETCH;
import static de.dataport.suchdienst.util.StreamUtils.combineFiltersConjunctionally;
import static de.dataport.suchdienst.util.StreamUtils.distinctByKeys;

import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.geo.model.GeoArs;
import de.dataport.suchdienst.geo.model.GeoArsDistinctFilters;
import de.dataport.suchdienst.geo.model.GeoArsPropFilters;
import de.dataport.suchdienst.geo.model.internal.GeoLocation;
import de.dataport.suchdienst.geo.model.internal.GeoLocationArs;
import de.dataport.suchdienst.geo.model.internal.GeoLocationDetail;
import de.dataport.suchdienst.geo.model.internal.HouseAddress;
import de.dataport.suchdienst.geo.model.out.GeoSuggestionOutModel;
import de.dataport.suchdienst.indexer.model.ArsObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GeoService {

    private final BkgGeoLocationResolver geoLocationResolver;
    private final SuchdienstProperties props;
    private final CodelistDownloader codelistDownloader;

    public GeoService(
        SuchdienstProperties props,
        BkgGeoLocationResolver geoLocationResolver,
        CodelistDownloader codelistDownloader
    ) {
        this.props = props;
        this.geoLocationResolver = geoLocationResolver;
        this.codelistDownloader = codelistDownloader;
    }

    /**
     * Die Methode implementiert die Suche im BKG Service.
     * Es werden nur Ergebnisse mit ARS-Schlüssel ausgeliefert.
     * </p>
     * Enthält das Ergebnis der BKG Suche die Stadtstaaten Berlin, Bremen und Hamburg, dann werden von dem BKG Service
     * <strong>nur</strong> die Ortsteile der Stadtstaaten zurückgegeben.</br>
     * Deswegen müssen in einem zweiten Schritt gegebenenfalls die Stadtstaaten als Ganzes dem BKG Ergebnis zugemischt werden. Dies geschieht in der Methode {@link FederalCityState#findMatchingFederalCityState(List)}.</br>
     * Soll die Methode keine Ortsteile zurückliefern dann können diese erst in einem nachfolgenden dritten Verarbeitungsschritt aus dem BKG Ergebnis herausgefiltert werden.
     *
     * @param ortsteil Ist der Wert <i>false</i> dann werden von der Methode keine Ortsteile zurückgeliefert.
     */
    public List<GeoArs> searchByQuery(
        String searchQuery,
        int limit,
        Set<GeoArsPropFilters> filterBy,
        Set<GeoArsDistinctFilters> distinctBy,
        boolean ortsteil
    ) {
        var filterByPlz = filterBy.contains(GeoArsPropFilters.PLZ);
        var filterByOrt = filterBy.contains(GeoArsPropFilters.ORT);
        var distinctByRefs = getDistinctByRefs(distinctBy);
        var filterByRefs = getFilterByRefs(filterBy);
        int count = determineFetchCount(limit, filterByOrt, distinctBy);

        var geoLocationList = geoLocationResolver.searchByQuery(
            searchQuery,
            count,
            filterByOrt,
            filterByPlz
        );
        var arsList = geoLocationDetailsToPvogArs(geoLocationList, ortsteil);

        if (limit > 1) {
            prependMatchingFederalCityState(arsList);
        }

        return arsList
            .stream()
            .filter(getDistinctFilter(distinctByRefs))
            .filter(getPropFilter(filterByRefs))
            .limit(limit)
            .toList();
    }

    /**
     * Die Methode implementiert die Suche im BKG Service.
     * Es werden nur Ergebnisse mit ARS-Schlüssel ausgeliefert.
     * </p>
     * Enthält das Ergebnis der BKG Suche die Stadtstaaten Berlin, Bremen und Hamburg, dann werden von dem BKG Service
     * <strong>nur</strong> die Ortsteile der Stadtstaaten zurückgegeben.</br>
     * Deswegen müssen in einem zweiten Schritt gegebenenfalls die Stadtstaaten als Ganzes dem BKG Ergebnis zugemischt werden. Dies geschieht in der Methode {@link FederalCityState#findMatchingFederalCityState(List)}.</br>
     * Soll die Methode keine Ortsteile zurückliefern dann können diese erst in einem nachfolgenden dritten Verarbeitungsschritt aus dem BKG Ergebnis herausgefiltert werden.
     *
     * @param ortsteil Ist der Wert <i>false</i> dann werden von der Methode keine Ortsteile zurückgeliefert.
     */
    public List<GeoArs> searchByAddress(
        HouseAddress address,
        int limit,
        Set<GeoArsPropFilters> filterBy,
        Set<GeoArsDistinctFilters> distinctBy,
        boolean ortsteil
    ) {
        var filterByPlz = filterBy.contains(GeoArsPropFilters.PLZ);
        var distinctByRefs = getDistinctByRefs(distinctBy);
        var filterByRefs = getFilterByRefs(filterBy);
        int count = determineFetchCount(limit, filterByPlz, distinctBy);

        var geoLocationList = geoLocationResolver.searchByAddress(
            address,
            count,
            filterByPlz,
            "typ:(haus strasse ort)"
        );

        var arsList = geoLocationDetailsToPvogArs(geoLocationList, ortsteil);

        if (limit > 1) {
            prependMatchingFederalCityState(arsList);
        }

        return arsList
            .stream()
            .filter(getDistinctFilter(distinctByRefs))
            .filter(getPropFilter(filterByRefs))
            .limit(limit)
            .toList();
    }

    /**
     * Die Methode implementiert die Suche im BKG Service.
     * Es werden nur Ergebnisse mit ARS-Schlüssel ausgeliefert.
     * Methode unterdrückt keine Ortsteile
     */
    public List<GeoArs> searchByArs(
        String ars,
        int limit,
        Set<GeoArsPropFilters> filterBy,
        Set<GeoArsDistinctFilters> distinctBy
    ) {
        var filterByPlz = filterBy.contains(GeoArsPropFilters.PLZ);
        var distinctByRefs = getDistinctByRefs(distinctBy);
        var filterByRefs = getFilterByRefs(filterBy);
        int count = determineFetchCount(limit, filterByPlz, distinctBy);

        var geoLocationList = geoLocationResolver.searchByArs(ars, count, filterByPlz);

        // Priorisiere Ergebnisse mit erstem Schluessel = ARS
        if (geoLocationList.size() >= 2) {
            GeoLocationArs firstArs = geoLocationList.get(0).getArs();
            GeoLocationArs secondArs = geoLocationList.get(1).getArs();

            if (
                !firstArs.getKeys().getFirst().contains(firstArs.getArs()) &&
                secondArs.getKeys().getFirst().contains(firstArs.getArs())
            ) {
                Collections.swap(geoLocationList, 0, 1);
            }
        }

        var arsList = geoLocationDetailsToPvogArs(geoLocationList, true);

        if (limit > 1) {
            prependMatchingFederalCityState(arsList);
        }

        return arsList
            .stream()
            .filter(getDistinctFilter(distinctByRefs))
            .filter(getPropFilter(filterByRefs))
            .limit(limit)
            .toList();
    }

    private int determineFetchCount(
        int limit,
        boolean filterByPlz,
        Set<GeoArsDistinctFilters> distinctBy
    ) {
        // Bei mehrstufigen Suchanfragen (filterByPlz) oder nachträglicher Reduktion (distinctBy),
        // wird durch Overfetching eine stabilere Antwortmenge erreicht
        return filterByPlz || ObjectUtils.isNotEmpty(distinctBy) ? REQUEST_COUNT_OVERFETCH : limit;
    }

    private @NonNull Set<Function<GeoArs, String>> getDistinctByRefs(
        Set<GeoArsDistinctFilters> distinctBy
    ) {
        return distinctBy
            .stream()
            .map(
                prop ->
                    switch (prop) {
                        case ARS -> (Function<GeoArs, String>) GeoArs::getArs;
                        case NAME -> (Function<GeoArs, String>) GeoArs::getName;
                        case PLZ -> (Function<GeoArs, String>) GeoArs::getPlz;
                        case STRASSE -> (Function<GeoArs, String>) GeoArs::getStreet;
                    }
            )
            .collect(Collectors.toSet());
    }

    private @NonNull Set<Function<GeoArs, String>> getFilterByRefs(
        Set<GeoArsPropFilters> propFilter
    ) {
        return propFilter
            .stream()
            .map(
                prop ->
                    switch (prop) {
                        case PLZ -> (Function<GeoArs, String>) GeoArs::getPlz;
                        case STRASSE -> (Function<GeoArs, String>) GeoArs::getStreet;
                        case HAUSNUMMER -> (Function<GeoArs, String>) GeoArs::getHousenumber;
                        case ORT -> null;
                    }
            )
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private List<GeoArs> geoLocationDetailsToPvogArs(
        List<GeoLocationDetail> geoLocationDetails,
        boolean ortsteil
    ) {
        Map<String, String> arsCodelist = ortsteil
            ? codelistDownloader.getLatestDestatisArsCodelist()
            : Collections.emptyMap();

        return geoLocationDetails
            .stream()
            .map(detail -> {
                Optional<GeoArs> geoArs = transformToPvogArs(detail, ortsteil, arsCodelist);
                // Koordinaten aus GeoLocationDetail ins GeoArs mappen
                return geoArs.map(ars -> {
                    mapCoordinates(ars, detail);
                    return ars;
                });
            })
            .flatMap(Optional::stream)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    public List<GeoSuggestionOutModel> suggest(String input, int count) {
        var suggestions = geoLocationResolver.suggest(input, count);
        prependParentLocationIfMissing(suggestions, input, count);
        return suggestions;
    }

    /**
     * Stellt sicher, dass bei Suggestions, die nur Ortsteile enthalten (z.B. "07407 Rudolstadt - Ost"),
     * der übergeordnete Ort (z.B. "07407 Rudolstadt") an erster Stelle eingefügt wird, sofern er nicht bereits vorhanden ist.
     *
     * <p>Da die BKG-Ergebnisse nach Score sortiert sind, wird der übergeordnete Ort des
     * relevantesten (ersten) Treffers verwendet. Die Erkennung basiert auf dem Trennzeichen " - "
     * (Leerzeichen-Bindestrich-Leerzeichen), welches vom BKG als Separator zwischen Ort und Ortsteil verwendet wird.</p>
     *
     * <p>Vorangestellt wird nur, wenn die Suchanfrage tatsächlich auf den übergeordneten Ort abzielt,
     * d.h. die Eingabe im übergeordneten Ortsnamen enthalten ist. Sucht der Nutzer gezielt nach einem
     * Ortsteil (z.B. "Rudolstadt Ost"), bleibt die Liste unverändert.</p>
     *
     * @param suggestions mutable Liste von Suggestions, wird ggf. verändert
     * @param input       ursprüngliche Suchanfrage des Nutzers
     * @param count       maximale Anzahl der Ergebnisse
     */
    private void prependParentLocationIfMissing(
        List<GeoSuggestionOutModel> suggestions,
        String input,
        int count
    ) {
        if (suggestions == null || suggestions.isEmpty() || StringUtils.isBlank(input)) {
            return;
        }

        var topSuggestion = suggestions.getFirst();
        var topName = topSuggestion.getName();
        if (topName == null || !topName.contains(" - ")) {
            return;
        }

        var parentName = topName.substring(0, topName.indexOf(" - "));

        // Nur voranstellen, wenn die Suchanfrage auf den übergeordneten Ort abzielt
        // und nicht bereits in den Ortsteil-Teil hineinreicht.
        if (!parentName.toLowerCase().contains(input.trim().toLowerCase())) {
            return;
        }

        boolean parentAlreadyPresent = suggestions
            .stream()
            .map(GeoSuggestionOutModel::getName)
            .anyMatch(parentName::equals);

        if (!parentAlreadyPresent) {
            suggestions.addFirst(new GeoSuggestionOutModel(parentName, topSuggestion.getWeight()));
            // Liste auf angefordertes Limit kürzen, da durch das Voranstellen ein Eintrag hinzugekommen ist
            if (suggestions.size() > count) {
                suggestions.removeLast();
            }
        }
    }

    public List<GeoArs> searchReverse(GeoLocation location) {
        List<GeoLocationDetail> bkgResultList = geoLocationResolver.searchReverse(location);
        List<GeoArs> geoArsList = new ArrayList<>();
        Map<String, String> arsCodelist = codelistDownloader.getLatestDestatisArsCodelist();

        bkgResultList.forEach(geoLocationDetail -> {
            Optional<GeoArs> pvogArs = transformToPvogArs(geoLocationDetail, true, arsCodelist);
            pvogArs.ifPresent(element -> {
                mapCoordinates(element, geoLocationDetail);
                if (
                    !props.getBkg().isNonCompliantXoevArsFilterActive() ||
                    arsCodelist.containsKey(element.getArs())
                ) {
                    geoArsList.add(element);
                }
            });
        });

        return geoArsList;
    }

    private <P, T> Predicate<P> getDistinctFilter(Set<Function<P, T>> distinctBy) {
        if (distinctBy.isEmpty()) {
            return any -> true;
        }
        return distinctByKeys(distinctBy);
    }

    private <P> Predicate<P> getPropFilter(Set<Function<P, String>> filterBy) {
        if (filterBy.isEmpty()) {
            return any -> true;
        }
        return combineFiltersConjunctionally(
            filterBy.stream().map(this::propIsNotBlank).collect(Collectors.toSet())
        );
    }

    private <T> Predicate<T> propIsNotBlank(Function<T, String> propAccessor) {
        return geoArs -> {
            var propValue = propAccessor.apply(geoArs);
            return StringUtils.isNotBlank(propValue);
        };
    }

    /**
     * Transformiert GeoLocationArs in PvogArs.
     *
     * @param geoLocationDetail  BKG Suchergebnis
     * @return Aus geoLocationDetail transformiertes PvogArs-Objekt. Kann "empty" sein, falls ARS ungültig ist.
     */
    private Optional<GeoArs> transformToPvogArs(
        GeoLocationDetail geoLocationDetail,
        boolean ortsteil,
        Map<String, String> arsCodelist
    ) {
        String ars = geoLocationDetail.getArs().getArs();
        if (StringUtils.isEmpty(ars) || ars.length() != ArsObject.ARS_LENGTH) {
            return Optional.empty();
        }

        return geoLocationDetail
            .getArs()
            .getKeys()
            .stream()
            .findFirst()
            .map(key ->
                GeoArs.fromGeoLocation(
                    bkgPropertiesToPvogArs(ars, key, ortsteil, arsCodelist),
                    geoLocationDetail
                )
            );
    }

    private String bkgPropertiesToPvogArs(
        String ars,
        String key,
        boolean ortsteil,
        Map<String, String> arsCodelist
    ) {
        // Wenn keine Ortsteil-ARS ausgegeben werden sollen, können wir hier direkt den Stadtstaat-ARS zurückgeben
        if (!ortsteil) {
            String arsFederalState = ars.startsWith("04")
                ? ars.substring(0, 5)
                : ars.substring(0, 2);
            GeoArs geoArs = FederalCityState.FEDERAL_CITY_STATES.get(arsFederalState);

            // gib den originalen ARS zurück, falls kein Stadtstaat-ARS gefunden wurde
            return geoArs != null ? geoArs.getArs() : ars;
        }

        // Wenn in XRepo existent Rückgabe von Ortsteil-ARS, ansonsten Original-ARS
        return FederalCityState.transformFromBkgKeyToArs(key)
            .filter(arsCodelist::containsKey)
            .orElse(ars);
    }

    /**
     * Mappt Koordinaten aus GeoLocationDetail ins GeoArs-Objekt
     */
    private void mapCoordinates(GeoArs geoArs, GeoLocationDetail geoLocationDetail) {
        GeoLocation location = geoLocationDetail.getLocation();
        if (location != null) {
            geoArs.setLatitude(location.getLatitude());
            geoArs.setLongitude(location.getLongitude());
        }
    }

    /**
     * Stellt sicher, dass bei Stadtstaaten (Hamburg, Bremen, Bremerhaven, Berlin) ein
     * übergeordneter Eintrag ohne Ortsteil an erster Stelle der Ergebnisliste steht.
     *
     * <ul>
     *   <li>Stadtstaat-Eintrag bereits an erster Stelle → nichts zu tun</li>
     *   <li>Stadtstaat-Eintrag vorhanden, aber nicht vorne → nach vorne verschieben</li>
     *   <li>Kein Stadtstaat-Eintrag vorhanden → statischen Eintrag aus
     *       {@link FederalCityState#FEDERAL_CITY_STATES} einfügen</li>
     * </ul>
     *
     * @param arsList mutable Liste von GeoArs-Ergebnissen, wird ggf. verändert
     */
    private void prependMatchingFederalCityState(List<GeoArs> arsList) {
        FederalCityState.findMatchingFederalCityState(arsList).ifPresent(fedState -> {
            var cityEntry = FederalCityState.findFederalCityEntry(arsList, fedState);

            cityEntry.ifPresentOrElse(
                entry -> {
                    if (arsList.getFirst() != entry) {
                        // Stadtstaat ist vorhanden, aber nicht vorne – nach vorne verschieben
                        arsList.remove(entry);
                        arsList.addFirst(entry);
                    }
                },
                // Kein Stadtstaat-Eintrag in der Liste – statischen einfügen
                () -> arsList.addFirst(fedState)
            );
        });
    }
}
