/*-
 * ---license-start
 * Suchdienst Integrationstests
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package suchdienst.integrationstest.connectors;

import static io.restassured.RestAssured.given;

import io.restassured.response.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import suchdienst.integrationstest.common.Constants;
import suchdienst.integrationstest.common.RestAssuredUtil;
import suchdienst.integrationstest.validator.ContentLanguageValidator;

@SuppressWarnings("java:S1192")
public class LocationsConnector {

    public static Response getLocationsV2(String q, Integer limit) {
        return getLocationsV2(q, limit, null);
    }

    public static Response getLocationsV2(String q, Integer limit, Boolean fuzzySearch) {
        Map<String, Object> params = new HashMap<>();
        if (q != null) {
            params.put("q", q);
        }
        if (limit != null) {
            params.put("limit", limit);
        }
        if (fuzzySearch != null) {
            params.put("fuzzysearch", fuzzySearch);
        }

        Response response = given(RestAssuredUtil.getRequestSpec())
            .header("Accept-Language", Constants.LANGUAGE_DE)
            .params(params)
            .when()
            .get("/v2/locations");
        return ContentLanguageValidator.validateContentLanguage(response, Constants.LANGUAGE_DE);
    }

    public static Response getLocationsSuggestionsV2(String q, Boolean fuzzySearch) {
        return getLocationsSuggestionsV2(q, fuzzySearch, false, 11);
    }

    public static Response getLocationsSuggestionsV2(String q, Boolean fuzzySearch, Boolean fuzzyFirst, Integer size) {
        Map<String, Object> params = new HashMap<>();
        if (q != null) {
            params.put("q", q);
        }
        if (fuzzySearch != null) {
            params.put("fuzzySearch", fuzzySearch);
        }
        if (fuzzyFirst != null) {
            params.put("fuzzyFirst", fuzzyFirst);
        }
        if (size != null) {
            params.put("size", size);
        }

        Response response = given(RestAssuredUtil.getRequestSpec())
            .header("Accept-Language", Constants.LANGUAGE_DE)
            .params(params)
            .when()
            .get("/v2/locations/suggestions");
        return ContentLanguageValidator.validateContentLanguage(response, Constants.LANGUAGE_DE);
    }

    public static Response getLocationsSuggestionsV3(String q) {
        Map<String, Object> params = new HashMap<>();
        if (q != null) {
            params.put("q", q);
        }
        params.put("size", 11);

        Response response = given(RestAssuredUtil.getRequestSpec())
            .header("Accept-Language", Constants.LANGUAGE_DE)
            .params(params)
            .when()
            .get("/v3/locations/suggestions");
        return ContentLanguageValidator.validateContentLanguage(response, Constants.LANGUAGE_DE);
    }

    public static Response getLocationsV3Details(String ort, Integer limit) {
        return getLocationsV3Details(ort, null, null, null, limit, List.of(), null, null);
    }

    public static Response getLocationsV3Details(String ort, Integer limit, List<String> distinctBy) {
        return getLocationsV3Details(ort, null, null, null, limit, List.of(), distinctBy, null);
    }

    public static Response getLocationsV3Details(String ort, Integer limit, List<String> distinctBy, Boolean ortsteil) {
        return getLocationsV3Details(ort, null, null, null, limit, List.of(), distinctBy, ortsteil);
    }

    public static Response getLocationsV3Details(
        String ort,
        Integer limit,
        List<String> filterBy,
        List<String> distinctBy
    ) {
        return getLocationsV3Details(ort, null, null, null, limit, filterBy, distinctBy, null);
    }

    public static Response getLocationsV3Details(
        String ort,
        String plz,
        String strasse,
        String hausnummer,
        Integer limit,
        List<String> filterBy,
        List<String> distinctBy,
        Boolean ortsteil
    ) {
        Map<String, Object> params = new HashMap<>();
        if (ort != null) {
            params.put("ort", ort);
        }
        if (plz != null) {
            params.put("plz", plz);
        }
        if (strasse != null) {
            params.put("strasse", strasse);
        }
        if (hausnummer != null) {
            params.put("hausnummer", hausnummer);
        }
        if (limit != null) {
            params.put("limit", limit);
        }
        if (filterBy != null) {
            String filterParams = StringUtils.joinWith(",", filterBy.toArray());
            params.put("filterBy", filterParams);
        }
        if (distinctBy != null) {
            String distinctParams = StringUtils.joinWith(",", distinctBy.toArray());
            params.put("distinctBy", distinctParams);
        }
        if (ortsteil != null) {
            params.put("ortsteil", ortsteil);
        }

        Response response = given(RestAssuredUtil.getRequestSpec())
            .header("Accept-Language", Constants.LANGUAGE_DE)
            .params(params)
            .when()
            .get("/v3/locations/details");
        return ContentLanguageValidator.validateContentLanguage(response, Constants.LANGUAGE_DE);
    }

    public static Response getLocationsV3ByArs(
        String ars,
        Integer limit,
        List<String> filterBy,
        List<String> distinctBy
    ) {
        Map<String, Object> params = new HashMap<>();
        if (ars != null) {
            params.put("ars", ars);
        }
        if (limit != null) {
            params.put("limit", limit);
        }
        if (filterBy != null) {
            String filterParams = StringUtils.joinWith(",", filterBy.toArray());
            params.put("filterBy", filterParams);
        }
        if (distinctBy != null) {
            String distinctParams = StringUtils.joinWith(",", distinctBy.toArray());
            params.put("distinctBy", distinctParams);
        }

        Response response = given(RestAssuredUtil.getRequestSpec())
            .header("Accept-Language", Constants.LANGUAGE_DE)
            .params(params)
            .when()
            .get("/v3/locations/ars");
        return ContentLanguageValidator.validateContentLanguage(response, Constants.LANGUAGE_DE);
    }

    public static Response getLocationsV3ByQuery(String query, Integer limit, Boolean ortsteil) {
        return getLocationsV3ByQuery(query, limit, List.of(), null, ortsteil);
    }

    public static Response getLocationsV3ByQuery(String query) {
        return getLocationsV3ByQuery(query, 10, List.of(), null, null);
    }

    public static Response getLocationsV3ByQuery(
        String query,
        Integer limit,
        List<String> filterBy,
        List<String> distinctBy,
        Boolean ortsteil
    ) {
        Map<String, Object> params = new HashMap<>();
        if (query != null) {
            params.put("q", query);
        }
        if (limit != null) {
            params.put("limit", limit);
        }
        if (filterBy != null) {
            String filterParams = StringUtils.joinWith(",", filterBy.toArray());
            params.put("filterBy", filterParams);
        }
        if (distinctBy != null) {
            String distinctParams = StringUtils.joinWith(",", distinctBy.toArray());
            params.put("distinctBy", distinctParams);
        }
        if (ortsteil != null) {
            params.put("ortsteil", ortsteil);
        }

        Response response = given(RestAssuredUtil.getRequestSpec())
            .header("Accept-Language", Constants.LANGUAGE_DE)
            .params(params)
            .when()
            .get("/v3/locations");
        return ContentLanguageValidator.validateContentLanguage(response, Constants.LANGUAGE_DE);
    }

    public static Response getLocationsV3Reverse(String lon, String lat) {
        Map<String, Object> params = new HashMap<>();
        if (lon != null) {
            params.put("lon", lon);
        }
        if (lat != null) {
            params.put("lat", lat);
        }

        Response response = given(RestAssuredUtil.getRequestSpec())
            .header("Accept-Language", Constants.LANGUAGE_DE)
            .params(params)
            .when()
            .get("/v2/locations/reverse");
        return ContentLanguageValidator.validateContentLanguage(response, Constants.LANGUAGE_DE);
    }
}
