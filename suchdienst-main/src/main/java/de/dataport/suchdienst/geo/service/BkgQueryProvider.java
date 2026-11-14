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

import static de.dataport.suchdienst.geo.util.OrtsNamenUtils.extractOrt;
import static de.dataport.suchdienst.geo.util.OrtsNamenUtils.extractOrtsteil;

import de.dataport.suchdienst.common.model.ArsHierarchyEbene;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.geo.model.GeoQueryBuilder;
import de.dataport.suchdienst.geo.model.internal.GeoLocation;
import de.dataport.suchdienst.geo.model.internal.HouseAddress;
import de.dataport.suchdienst.geo.model.internal.SearchARS;
import de.dataport.suchdienst.indexer.model.ArsObject;
import de.dataport.suchdienst.indexer.model.mapping.Address;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Stellt BKG-URLs sowie Methoden, die passende Parameter-Maps liefern, bereit. Vorgesehen zur Verwendung
 * mit Variable-Expansion von {@link org.springframework.web.client.RestTemplate}. URLs und Parameter müssen
 * zueinander passen, was meist zur Folge hat, dass auch Queries in {@link de.dataport.suchdienst.geo.service.BkgQueryBuilder}
 * angepasst werden müssen.
 */
@Slf4j
@Service
@Getter
public class BkgQueryProvider implements BkgQueryBuilder, GeoQueryBuilder {

    private static final String ARS_SEARCH_PREFIX = "rs:";
    private static final String SCHLUESSEL_SEARCH_PREFIX = "schluessel:";
    private static final String SRSNAME_STRING = "srsName";
    private static final String QUERY_STRING = "query";
    private static final String COUNT_STRING = "count";
    private static final String FILTER_STRING = "filter";

    /**
     * Default Wert für Overfetching von Suchanfragen an den BKG
     * Grund:
     * Durch nachträgliche Filterung im Suchdienst können wir in vielen Fällen nicht das Limit einhalten.
     * Dadurch unterscheidet sich die am Controller ausgegebene Treffermenge in nicht nachvollziehbarer Weise bei unterschiedlichen Limit-Werten.
     * Beispiel: Bei Limit 25 wurden 9 Treffer ausgegeben, bei Limit 5 nur 3
     * Erwartet wurden bei einem Limit von 5 entsprechend 5.
     * (PV-11412)
     */
    public static final int REQUEST_COUNT_OVERFETCH = 50;

    private final Map.Entry<String, String> bkgKennung;
    private final Map.Entry<String, String> bkgProps;
    private final String forwardGeocodeQuery;
    private final String reverseGeocodeQuery;
    private final String suggestionSearchQuery;
    private final String geoSearchQuery;
    private final String geoSearchArsQuery;
    private final String geoSearchStructuredQuery;
    private final String geoSearchStructuredOrtQuery;
    private final SuchdienstProperties props;

    public BkgQueryProvider(SuchdienstProperties props) {
        this.props = props;

        if (StringUtils.isNotBlank(props.getBkg().getPassword())) {
            this.bkgKennung = Map.entry("kennung", props.getBkg().getPassword());
        } else {
            this.bkgKennung = Map.entry("kennung", "");
            log.warn("Passwort für den BKG nicht konfiguriert.");
        }

        this.bkgProps = Map.entry(
            "properties",
            "rs,ort,schluessel,plz,ortsteil,score,gemeinde,strasse,haus,name"
        );

        this.forwardGeocodeQuery = createUriString(buildForwardGeocodeQuery(props));
        this.reverseGeocodeQuery = createUriString(buildReverseGeocodeQuery(props));
        this.suggestionSearchQuery = createUriString(buildSuggestionQuery(props));
        this.geoSearchQuery = createUriString(buildGeosearchQuery(props));
        this.geoSearchArsQuery = createUriString(buildGeosearchArsQuery(props));
        this.geoSearchStructuredQuery = createUriString(buildStructuredQuery(props, false));
        this.geoSearchStructuredOrtQuery = createUriString(buildStructuredQuery(props, true));
    }

    public String geoSearchStructuredQuery(boolean includeOrt) {
        return includeOrt ? geoSearchStructuredOrtQuery : geoSearchStructuredQuery;
    }

    public Map<String, String> getForwardGeocodeSearchParams(Address address) {
        Optional<String> oQuery = buildForwardQueryString(address);

        if (oQuery.isEmpty()) {
            log.debug(
                "EXTERN: Geo-Koordinaten werden nicht ermittelt, da die Adresse unvollständig ist. {}",
                address
            );
            return Collections.emptyMap();
        }

        return Map.ofEntries(
            this.bkgKennung,
            Map.entry(SRSNAME_STRING, GeoLocation.EPSG_4326),
            Map.entry(QUERY_STRING, oQuery.get())
        );
    }

    public Map<String, String> getReverseGeocodeSearchParams(GeoLocation location) {
        return Map.ofEntries(
            this.bkgKennung,
            Map.entry("lat", location.getLatitude()),
            Map.entry("lon", location.getLongitude()),
            Map.entry(SRSNAME_STRING, location.getSystem())
        );
    }

    public Map<String, String> getDefaultSearchParams(String input, int count, String filter) {
        return Map.ofEntries(
            this.bkgKennung,
            this.bkgProps,
            Map.entry(COUNT_STRING, String.valueOf(count)),
            Map.entry(QUERY_STRING, escapeSpecialChars(input)),
            Map.entry(FILTER_STRING, filter)
        );
    }

    /**
     * Erstellung der Query-Parameter für die ARS-Suche
     * @param searchARS
     * @param count
     * @param filter
     * @param exactArsSearch bei Verwendung via ARS-Endpunkt
     * @return
     */
    public Map<String, String> getSearchByArsParams(
        SearchARS searchARS,
        int count,
        String filter,
        boolean exactArsSearch
    ) {
        final String queryValue;
        if (exactArsSearch && ArsHierarchyEbene.ORTSTEIL != searchARS.getArsEbene()) {
            queryValue = String.format(
                // wenn exakt, rs und schluessel Verwendung, siehe PV-11938
                "%s%s AND %s%s",
                ARS_SEARCH_PREFIX,
                searchARS.getProcessedArs(),
                SCHLUESSEL_SEARCH_PREFIX,
                searchARS.getSchluessel()
            );
        } else {
            String prefix =
                searchARS.getSchluessel().length() > ArsObject.ARS_LENGTH
                    ? SCHLUESSEL_SEARCH_PREFIX
                    : ARS_SEARCH_PREFIX;
            queryValue = StringUtils.join(
                // wenn der ARS-Parameter länger ist, als der Regionalschlüssel-Standard, wird von einem BKG-Schlüssel ausgegangen
                prefix,
                searchARS.getSchluessel()
            );
        }

        var params = createConditionalSearchByArsParams(count);
        params.put(QUERY_STRING, queryValue);

        if (StringUtils.isNotBlank(filter)) {
            params.put(FILTER_STRING, filter);
        }

        return params;
    }

    public Map<String, String> getSearchByArsParams(
        String ars1,
        String ars2,
        int count,
        String filter
    ) {
        if (ars2 == null) {
            return getSearchByArsParams(new SearchARS(ars1), count, filter, false);
        }

        var params = createConditionalSearchByArsParams(count);
        params.put(
            QUERY_STRING,
            StringUtils.join(ARS_SEARCH_PREFIX, ars1, " OR ", ARS_SEARCH_PREFIX, ars2)
        );

        if (StringUtils.isNotBlank(filter)) {
            params.put(FILTER_STRING, filter);
        }

        return params;
    }

    public Map<String, String> getStructuredQueryParams(HouseAddress address, int count) {
        Map<String, String> parameterMap = new HashMap<>();
        parameterMap.put(this.bkgKennung.getKey(), this.bkgKennung.getValue());
        parameterMap.put(this.bkgProps.getKey(), this.bkgProps.getValue());
        parameterMap.put(COUNT_STRING, String.valueOf(count));

        String streetOrCity = StringUtils.isNotBlank(address.street()) ? "strasse" : "ort";
        String typ = StringUtils.isNotBlank(address.houseNumber()) ? "haus" : streetOrCity;
        String filterString = "typ:" + typ;

        /*
          Problemfeld: Mögliche Mehrdeutigkeit von Query-Parameter "ort/address.city()"
          Nach der Umstellung auf Gemeindesuche (PV-12309) war in "ort/address.city()" die Gemeinde (aus der BKG Antwort) enthalten.
          Dieses Verhalten wurde im Frontend abgestellt (!3023). Die API sollte den Schwenk auf Gemeinde ebenfalls vollziehen; // NOSONAR
          gerade auch um der bisherigen Problematik von Verknüpfung Ort <> Gemeinde (bzw. ARS) Rechnung zu tragen.
          Dann lässt sich der Filter auf "gemeinde" schwenken und Util-Code entfernen.
         */

        if (StringUtils.isNotBlank(address.city())) {
            var ort = extractOrt(address.city())
                .map(this::replaceSpecialChars)
                .orElse(address.city());
            filterString += " AND ort:(" + ort + ")";
            filterString += extractOrtsteil(address.city())
                .map(ortsteil -> " AND ortsteil:" + replaceSpecialChars(ortsteil))
                .orElse("");

            // Der Ort muss prinzipiell nicht enthalten sein
            parameterMap.put("ort", ort);
        }
        // Das Vorhandensein einer PLZ erhöht Geschwindigkeit und Qualität
        String sanitizedPlz = replaceSpecialChars(address.zip());
        if (StringUtils.isNotBlank(sanitizedPlz)) {
            filterString += " AND plz:\"" + sanitizedPlz + "\"";
            parameterMap.put("plz", address.zip());
        } else {
            // Fallback (Ursache unbekannt, evtl. in Zusammenspiel mit filter)
            // "*"" genauso gut/schlecht wie ohne plz Parameter?
            parameterMap.put("plz", "*");
        }
        if (StringUtils.isNotBlank(address.street())) {
            filterString += " AND strasse:\"" + replaceSpecialChars(address.street()) + "\"";
        }
        if (StringUtils.isNotBlank(address.houseNumber())) {
            filterString += " AND haus:\"" + replaceSpecialChars(address.houseNumber()) + "\"";
        }
        // Ein kompletter Filter verbessert die Qualität durch Kontextualisierung (UND-Verknüpfung)
        // und hat sich in der Vergangenheit als robuster als die Query-Kombi plz + strasse + haus erwiesen
        //
        parameterMap.put(FILTER_STRING, filterString);

        return parameterMap;
    }

    private Optional<String> buildForwardQueryString(Address address) {
        var query = new StringBuilder();
        if (StringUtils.isNotBlank(address.getStreet())) {
            if (StringUtils.isBlank(address.getZip()) && StringUtils.isBlank(address.getCity())) {
                // Nur ein Paar aus Straße und Zip oder Ortsangabe ermöglicht eine einigermaßen genaue Bestimmung
                return Optional.empty();
            }
            query.append("strasse:(").append(escapeSpecialChars(address.getStreet())).append(")");

            String houseNo = extractHouseNo(address);
            if (StringUtils.isNotBlank(houseNo)) {
                query.append(" haus:(").append(escapeSpecialChars(houseNo)).append(")");
            }
            String sanitizedZip = removeSpecialChars(address.getZip());
            if (StringUtils.isNotBlank(sanitizedZip)) {
                query.append(" plz:(").append(sanitizedZip).append(")");
            }
            if (StringUtils.isNotBlank(address.getCity())) {
                query.append(" ort:(").append(escapeSpecialChars(address.getCity())).append(")");
            }
            return Optional.of(query.toString());
        }
        return Optional.empty();
    }

    private Map<String, String> createConditionalSearchByArsParams(int count) {
        var params = new HashMap<String, String>();
        params.put(this.bkgKennung.getKey(), this.bkgKennung.getValue());
        params.put(this.bkgProps.getKey(), this.bkgProps.getValue());

        // Wenn nicht definiert, wird davon ausgegangen, dass alles von Interesse ist
        params.put(
            COUNT_STRING,
            count != 0 ? String.valueOf(count) : String.valueOf(REQUEST_COUNT_OVERFETCH)
        );

        return params;
    }

    private String createUriString(UriComponentsBuilder builder) {
        return builder.build().normalize().toUriString();
    }
}
