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
import org.apache.commons.lang3.ObjectUtils;
import suchdienst.integrationstest.common.Constants;
import suchdienst.integrationstest.common.Constants.XzufiVersion;
import suchdienst.integrationstest.common.RestAssuredUtil;
import suchdienst.integrationstest.validator.ContentLanguageValidator;

public class RelationsConnector {

    public static Response getRelationsLbIdsArs231V2(List<String> lbIds, String ars) {
        return getRelationsLbIdsArs231V2(lbIds, ars, null, null, null);
    }

    public static Response getRelationsLbIdsArs231V2(
        List<String> lbIds,
        String ars,
        Boolean useArsHierarchy,
        Integer page,
        Integer size
    ) {
        Map<String, Object> params = new HashMap<>();
        params.computeIfAbsent("lbids", _ -> lbIds);
        params.computeIfAbsent("ars", _ -> ars);
        params.computeIfAbsent("usearshierarchy", _ -> useArsHierarchy);
        params.computeIfAbsent("page", _ -> page);
        params.computeIfAbsent("size", _ -> size);
        return given(RestAssuredUtil.getRequestSpec()).params(params).when().get("/v2/relations/jzufi-2-3");
    }

    // V3
    public static Response getRelationsLbIdsArsV3(List<String> lbIds, String ars, XzufiVersion xzufiVersion) {
        return getRelationsLbIdsArsV3(lbIds, ars, 0, 20, xzufiVersion);
    }

    public static Response getRelationsLbIdsArs220V1(List<String> lbIds, String ars) {
        return getRelationsLbIdsArsMultiversionV1(lbIds, ars, 0, 20, XzufiVersion.V2_2_0);
    }

    public static Response getRelationsLbIdsArs231V1(List<String> lbIds, String ars) {
        return getRelationsLbIdsArsMultiversionV1(lbIds, ars, 0, 20, XzufiVersion.V2_3_1);
    }

    private static Response getRelationsLbIdsArsMultiversionV1(
        List<String> lbIds,
        String ars,
        Integer page,
        Integer size,
        XzufiVersion xzufiVersion
    ) {
        Map<String, Object> params = new HashMap<>();
        params.computeIfAbsent("lbids", _ -> ObjectUtils.isEmpty(lbIds) ? null : lbIds);
        params.computeIfAbsent("ars", _ -> ars);
        params.computeIfAbsent("page", _ -> page);
        params.computeIfAbsent("size", _ -> size);
        params.put("validdate", null);
        params.put("usearshierarchy", true);

        String path = "";
        switch (xzufiVersion) {
            case V2_2_0 -> path = "/v1/relations/jzufi-2-2";
            case V2_3_1 -> path = "/v1/relations/jzufi-2-3";
        }

        Response response = given(RestAssuredUtil.getRequestSpec())
            .header("Accept-Language", Constants.LANGUAGE_DE)
            .params(params)
            .when()
            .get(path);

        return ContentLanguageValidator.validateContentLanguage(response, Constants.LANGUAGE_DE);
    }

    public static Response getRelationsLbIdsArsV3(
        List<String> lbIds,
        String ars,
        int page,
        int size,
        XzufiVersion xzufiVersion
    ) {
        return getRelationsLbIdsArsV3(lbIds, ars, page, size, xzufiVersion, true);
    }

    public static Response getRelationsLbIdsArsV3(
        List<String> lbIds,
        String ars,
        boolean useArsHierarchy,
        XzufiVersion xzufiVersion
    ) {
        return getRelationsLbIdsArsV3(lbIds, ars, 0, 20, xzufiVersion, useArsHierarchy);
    }

    private static Response getRelationsLbIdsArsV3(
        List<String> lbIds,
        String ars,
        Integer page,
        Integer size,
        XzufiVersion xzufiVersion,
        boolean useArsHierarchy
    ) {
        Map<String, Object> params = new HashMap<>();
        params.computeIfAbsent("lbids", _ -> ObjectUtils.isEmpty(lbIds) ? null : lbIds);
        params.computeIfAbsent("page", _ -> page);
        params.computeIfAbsent("size", _ -> size);
        params.put("validdate", null);
        params.put("usearshierarchy", useArsHierarchy);

        Response response = given(RestAssuredUtil.getRequestSpec())
            .header("Accept-Language", Constants.LANGUAGE_DE)
            .header("Accept", "application/json;xzufi-version=" + xzufiVersion)
            .params(params)
            .when()
            .get("/v3/relations/{ars}/jzufi", ars);

        return ContentLanguageValidator.validateContentLanguage(response, Constants.LANGUAGE_DE);
    }
}
