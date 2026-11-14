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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import suchdienst.integrationstest.common.Constants;
import suchdienst.integrationstest.common.RestAssuredUtil;
import suchdienst.integrationstest.tests.onlineservicerelations.model.OnlineServiceResult;
import suchdienst.integrationstest.tests.onlineservicerelations.model.OnlineServicesIteratorResponse;
import suchdienst.integrationstest.validator.ContentLanguageValidator;

public class OnlineserviceConnector {

    public static Response getOnlineservicesJzufi231V2(String id) {
        return given(RestAssuredUtil.getRequestSpec())
            .pathParam("id", id)
            .when()
            .get("/v2/onlineservices/{id}/jzufi-2-3");
    }

    public static Response getOnlineserviceDetailV2(String q) {
        return getOnlineserviceDetailV2(q, Constants.LANGUAGE_DE);
    }

    public static Response getOnlineserviceDetailV2(String q, String language) {
        Map<String, Object> params = new HashMap<>();
        if (q != null) {
            params.put("q", q);
        }

        Response response = given(RestAssuredUtil.getRequestSpec())
            .header("Accept-Language", language)
            .params(params)
            .when()
            .get("/v2/onlineservices/detail");
        return ContentLanguageValidator.validateContentLanguage(response, language);
    }

    public static Response getOnlineserviceDetailV3(String odId) {
        return getOnlineserviceDetailV3(odId, Constants.LANGUAGE_DE);
    }

    public static Response getOnlineserviceDetailV3(String odId, String language) {
        Response response = given(RestAssuredUtil.getRequestSpec())
            .header("Accept-Language", language)
            .pathParam("odId", odId)
            .when()
            .get("/v3beta1/onlineservices/{odId}/detail");
        return ContentLanguageValidator.validateContentLanguage(response, language);
    }

    public static List<OnlineServiceResult> getAllOnlineServicesByArs(String arsPrefix) {
        return getAllOnlineServicesByArs(arsPrefix, null);
    }

    public static List<OnlineServiceResult> getAllOnlineServicesByArs(String arsPrefix, List<String> leikaIds) {
        String nextPageToken = null;
        List<OnlineServiceResult> allItems = new ArrayList<>();
        List<OnlineServiceResult> currentItems;
        do {
            OnlineServicesIteratorResponse model = getOnlineServicesByArs(arsPrefix, leikaIds, nextPageToken).as(
                OnlineServicesIteratorResponse.class
            );
            currentItems = model.onlineservices();
            allItems.addAll(currentItems);
            nextPageToken = model.nextPageToken();
        } while (StringUtils.isNotBlank(nextPageToken));
        return allItems;
    }

    public static Response getOnlineServicesByArs(String arsPrefix, List<String> leikaIds, String nextPageToken) {
        Map<String, Object> params = new HashMap<>();
        params.computeIfAbsent("ars", _ -> arsPrefix);
        params.computeIfAbsent("leikaIds", _ -> leikaIds);
        params.computeIfAbsent("nextPageToken", _ -> nextPageToken);

        return given(RestAssuredUtil.getRequestSpec()).params(params).when().get("/v1beta2/onlineservices");
    }

    public static Response getOrganisationsByOdId(String odId) {
        return given(RestAssuredUtil.getRequestSpec()).when().get("/v1beta2/onlineservices/{odId}/organisations", odId);
    }

    public static Response getOnlineservicesJzufi220V1(String id) {
        return getOnlineservicesJzufi220V1(id, null);
    }

    public static Response getOnlineservicesJzufi220V1(String id, LocalDate validDate) {
        Map<String, Object> params = new HashMap<>();
        if (validDate != null) {
            params.put("validdate", validDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        return given(RestAssuredUtil.getRequestSpec())
            .pathParam("id", id)
            .params(params)
            .when()
            .get("/v1/onlineservices/{id}/jzufi-2-2");
    }

    public static Response getOnlineservicesJzufi231V1(String id) {
        return getOnlineservicesJzufi231V1(id, null);
    }

    public static Response getOnlineservicesJzufi231V1(String id, LocalDate validDate) {
        Map<String, Object> params = new HashMap<>();
        if (validDate != null) {
            params.put("validdate", validDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        return given(RestAssuredUtil.getRequestSpec())
            .pathParam("id", id)
            .params(params)
            .when()
            .get("/v1/onlineservices/{id}/jzufi-2-3");
    }
}
