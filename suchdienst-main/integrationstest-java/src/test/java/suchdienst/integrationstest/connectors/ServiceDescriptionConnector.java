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
import io.restassured.specification.RequestSpecification;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Singular;
import suchdienst.integrationstest.common.Constants;
import suchdienst.integrationstest.common.Constants.XzufiVersion;
import suchdienst.integrationstest.common.RestAssuredUtil;
import suchdienst.integrationstest.validator.ContentLanguageValidator;

public class ServiceDescriptionConnector {

    public static final Integer DEFAULT_PAGE = 0;
    public static final Integer DEFAULT_SIZE = 20;
    public static final Boolean DEFAULT_FUZZY_SEARCH = Boolean.FALSE;
    public static final List<String> DEFAULT_IGNORE_LEIKA_TYPES = Collections.emptyList();
    private static final LocalDate DEFAULT_VALID_DATE = null;
    public static final List<String> DEFAULT_PERSONAL_MATTERS = null;
    public static final Boolean DEFAULT_FACADES = Boolean.TRUE;
    public static final Boolean DEFAULT_USE_ARS_HIERARCHY = Boolean.TRUE;
    public static final String DEFAULT_LANGUAGE_CODE = Constants.LANGUAGE_DE;
    public static final Boolean DEFAULT_APPLY_DYNAMIC_PARAMS = Boolean.TRUE;

    @Builder
    public static class V7ConnectorProperties {

        private String ars;

        @Singular("q")
        private List<String> q;

        @Builder.Default
        private Integer page = DEFAULT_PAGE;

        @Builder.Default
        private Integer size = DEFAULT_SIZE;

        @Builder.Default
        private Boolean fuzzysearch = DEFAULT_FUZZY_SEARCH;

        @Builder.Default
        private List<String> ignoreLeikaTypes = DEFAULT_IGNORE_LEIKA_TYPES;

        @Builder.Default
        private LocalDate validDate = DEFAULT_VALID_DATE;

        @Builder.Default
        private List<String> personalMatters = DEFAULT_PERSONAL_MATTERS;

        @Builder.Default
        private Boolean facades = DEFAULT_FACADES;

        @Builder.Default
        private Boolean useArsHierarchy = DEFAULT_USE_ARS_HIERARCHY;

        @Builder.Default
        private String languageCode = DEFAULT_LANGUAGE_CODE;

        @Builder.Default
        private Boolean applyDynamicParams = DEFAULT_APPLY_DYNAMIC_PARAMS;
    }

    /**
     * Use {@link #getServiceDescriptionsV7(V7ConnectorProperties)} instead
     */
    @Deprecated(forRemoval = true)
    public static Response getServiceDescriptionsV7(String ars, String q) {
        return getServiceDescriptionsV7(V7ConnectorProperties.builder().ars(ars).q(q).build());
    }

    /**
     * Use {@link #getServiceDescriptionsV7(V7ConnectorProperties)} instead
     */
    @Deprecated(forRemoval = true)
    public static Response getServiceDescriptionsV7(String ars, String q, String languageCode) {
        return getServiceDescriptionsV7(
            V7ConnectorProperties.builder().ars(ars).q(q).fuzzysearch(true).languageCode(languageCode).build()
        );
    }

    /**
     * Use {@link #getServiceDescriptionsV7(V7ConnectorProperties)} instead
     */
    @Deprecated(forRemoval = true)
    public static Response getServiceDescriptionsV7(String ars, String q, Boolean useArsHierarchy) {
        return getServiceDescriptionsV7(
            V7ConnectorProperties.builder().ars(ars).q(q).useArsHierarchy(useArsHierarchy).build()
        );
    }

    /**
     * Use {@link #getServiceDescriptionsV7(V7ConnectorProperties)} instead
     */
    @Deprecated(forRemoval = true)
    public static Response getServiceDescriptionsFuzzyV7(String ars, String q, Boolean fuzzySearch) {
        return getServiceDescriptionsV7(V7ConnectorProperties.builder().ars(ars).q(q).fuzzysearch(fuzzySearch).build());
    }

    /**
     * Use {@link #getServiceDescriptionsV7(V7ConnectorProperties)} instead
     */
    @Deprecated(forRemoval = true)
    public static Response getServiceDescriptionsV7(String ars, String q, List<String> ignoreLeikaTypes) {
        return getServiceDescriptionsV7(
            V7ConnectorProperties.builder().ars(ars).q(q).ignoreLeikaTypes(ignoreLeikaTypes).build()
        );
    }

    /**
     * Use {@link #getServiceDescriptionsV7(V7ConnectorProperties)} instead
     */
    @Deprecated(forRemoval = true)
    public static Response getServiceDescriptionsV7(String ars, String q, int page, int size) {
        return getServiceDescriptionsV7(V7ConnectorProperties.builder().ars(ars).q(q).page(page).size(size).build());
    }

    /**
     * Use {@link #getServiceDescriptionsV7(V7ConnectorProperties)} instead
     */
    @Deprecated(forRemoval = true)
    public static Response getServiceDescriptionsV7(
        String ars,
        List<String> q,
        Integer page,
        Integer size,
        Boolean fuzzysearch,
        List<String> ignoreLeikaTypes,
        LocalDate validDate
    ) {
        return getServiceDescriptionsV7(
            V7ConnectorProperties.builder()
                .ars(ars)
                .q(q)
                .page(page)
                .size(size)
                .fuzzysearch(fuzzysearch)
                .ignoreLeikaTypes(ignoreLeikaTypes)
                .validDate(validDate)
                .build()
        );
    }

    /**
     * Use {@link #getServiceDescriptionsV7(V7ConnectorProperties)} instead
     */
    @Deprecated(forRemoval = true)
    public static Response getServiceDescriptionsV7(
        String ars,
        List<String> q,
        Integer page,
        Integer size,
        Boolean fuzzysearch,
        List<String> ignoreLeikaTypes,
        LocalDate validDate,
        List<String> personalMatters,
        boolean facades
    ) {
        return getServiceDescriptionsV7(
            V7ConnectorProperties.builder()
                .ars(ars)
                .q(q)
                .page(page)
                .size(size)
                .fuzzysearch(fuzzysearch)
                .ignoreLeikaTypes(ignoreLeikaTypes)
                .validDate(validDate)
                .personalMatters(personalMatters)
                .facades(facades)
                .build()
        );
    }

    /**
     * Use {@link #getServiceDescriptionsV7(V7ConnectorProperties)} instead
     */
    @Deprecated(forRemoval = true)
    public static Response getServiceDescriptionsV7(
        String ars,
        List<String> q,
        Integer page,
        Integer size,
        Boolean fuzzysearch,
        List<String> ignoreLeikaTypes,
        LocalDate validDate,
        List<String> personalMatters,
        boolean facades,
        String languageCode
    ) {
        return getServiceDescriptionsV7(
            V7ConnectorProperties.builder()
                .ars(ars)
                .q(q)
                .page(page)
                .size(size)
                .fuzzysearch(fuzzysearch)
                .ignoreLeikaTypes(ignoreLeikaTypes)
                .validDate(validDate)
                .personalMatters(personalMatters)
                .facades(facades)
                .languageCode(languageCode)
                .build()
        );
    }

    public static Response getServiceDescriptionsV7(V7ConnectorProperties connectorProperties) {
        RequestSpecification requestSpecification = given(RestAssuredUtil.getRequestSpec()).header(
            "Accept-Language",
            connectorProperties.languageCode
        );
        requestSpecification = addParam(requestSpecification, "q", connectorProperties.q);
        requestSpecification = addParam(requestSpecification, "page", connectorProperties.page);
        requestSpecification = addParam(requestSpecification, "size", connectorProperties.size);
        requestSpecification = addParam(requestSpecification, "fuzzysearch", connectorProperties.fuzzysearch);
        requestSpecification = addParam(requestSpecification, "ignoreLeikaTypes", connectorProperties.ignoreLeikaTypes);
        if (connectorProperties.validDate != null) {
            requestSpecification = addParam(
                requestSpecification,
                "validDate",
                connectorProperties.validDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            );
        }
        requestSpecification = addParam(requestSpecification, "personalMatters", connectorProperties.personalMatters);
        requestSpecification = addParam(requestSpecification, "facades", connectorProperties.facades);
        requestSpecification = addParam(requestSpecification, "useArsHierarchy", connectorProperties.useArsHierarchy);
        requestSpecification = addParam(
            requestSpecification,
            "applyDynamicParams",
            connectorProperties.applyDynamicParams
        );

        Response response = requestSpecification.when().get("/v7/servicedescriptions/{ars}", connectorProperties.ars);
        return ContentLanguageValidator.validateContentLanguage(response, connectorProperties.languageCode);
    }

    /**
     * PV-13188: Beta-Such-API. Im Gegensatz zur V7ConnectorProperties gibt es hier keine Parameter mehr für
     * fuzzysearch, ignoreLeikaTypes, validDate und applyDynamicParams, da diese Such-Methodik in der Beta-API
     * fest vorgegeben ist (Fuzzy-Suche und dynamische Parameter stets aktiv, volle Leika-Typ-Whitelist,
     * Gültigkeitsdatum stets "jetzt").
     */
    @Builder
    public static class V8ConnectorProperties {

        private String ars;

        @Singular("q")
        private List<String> q;

        @Builder.Default
        private Integer page = DEFAULT_PAGE;

        @Builder.Default
        private Integer size = DEFAULT_SIZE;

        @Builder.Default
        private List<String> personalMatters = DEFAULT_PERSONAL_MATTERS;

        @Builder.Default
        private Boolean facades = DEFAULT_FACADES;

        @Builder.Default
        private Boolean useArsHierarchy = DEFAULT_USE_ARS_HIERARCHY;

        @Builder.Default
        private String languageCode = DEFAULT_LANGUAGE_CODE;

        /**
         * Optionale Client-ID (Header {@code pvog-sud-api-client-id}), über die sich der Suchdienst
         * identifizierte Clients merkt, um z.B. nicht-öffentliche Onlinedienste herauszufiltern.
         */
        private String clientId;
    }

    public static Response getServiceDescriptionsV8(String ars, String q) {
        return getServiceDescriptionsV8(V8ConnectorProperties.builder().ars(ars).q(q).build());
    }

    /**
     * Ruft die Beta-Such-API mit einer expliziten Client-ID (Header {@code pvog-sud-api-client-id}) auf.
     */
    public static Response getServiceDescriptionsV8(String ars, String q, String languageCode, String clientId) {
        return getServiceDescriptionsV8(
            V8ConnectorProperties.builder().ars(ars).q(q).languageCode(languageCode).clientId(clientId).build()
        );
    }

    public static Response getServiceDescriptionsV8(String ars, List<String> q) {
        return getServiceDescriptionsV8(V8ConnectorProperties.builder().ars(ars).q(q).build());
    }

    public static Response getServiceDescriptionsV8(String ars, String q, String languageCode) {
        return getServiceDescriptionsV8(
            V8ConnectorProperties.builder().ars(ars).q(q).languageCode(languageCode).build()
        );
    }

    public static Response getServiceDescriptionsV8(String ars, List<String> q, String languageCode) {
        return getServiceDescriptionsV8(
            V8ConnectorProperties.builder().ars(ars).q(q).languageCode(languageCode).build()
        );
    }

    public static Response getServiceDescriptionsV8(String ars, String q, boolean useArsHierarchy) {
        return getServiceDescriptionsV8(
            V8ConnectorProperties.builder().ars(ars).q(q).useArsHierarchy(useArsHierarchy).build()
        );
    }

    public static Response getServiceDescriptionsV8(String ars, String q, int page, int size) {
        return getServiceDescriptionsV8(V8ConnectorProperties.builder().ars(ars).q(q).page(page).size(size).build());
    }

    public static Response getServiceDescriptionsV8(String ars, List<String> q, int page, int size) {
        return getServiceDescriptionsV8(V8ConnectorProperties.builder().ars(ars).q(q).page(page).size(size).build());
    }

    public static Response getServiceDescriptionsV8(
        String ars,
        List<String> q,
        List<String> personalMatters,
        boolean facades
    ) {
        return getServiceDescriptionsV8(
            V8ConnectorProperties.builder().ars(ars).q(q).personalMatters(personalMatters).facades(facades).build()
        );
    }

    /**
     * Entspricht der V7-Überladung mit den (in V8 entfallenen) Parametern fuzzysearch, ignoreLeikaTypes und
     * validDate. Da diese Such-Methodik in der Beta-API fest vorgegeben ist, entfallen die entsprechenden
     * Argumente hier ersatzlos.
     */
    public static Response getServiceDescriptionsV8(
        String ars,
        List<String> q,
        Integer page,
        Integer size,
        List<String> personalMatters,
        boolean facades
    ) {
        return getServiceDescriptionsV8(
            V8ConnectorProperties.builder()
                .ars(ars)
                .q(q)
                .page(page)
                .size(size)
                .personalMatters(personalMatters)
                .facades(facades)
                .build()
        );
    }

    /**
     * Entspricht der V7-Überladung mit den (in V8 entfallenen) Parametern fuzzysearch, ignoreLeikaTypes und
     * validDate. Da diese Such-Methodik in der Beta-API fest vorgegeben ist, entfallen die entsprechenden
     * Argumente hier ersatzlos.
     */
    public static Response getServiceDescriptionsV8(
        String ars,
        List<String> q,
        Integer page,
        Integer size,
        List<String> personalMatters,
        boolean facades,
        String languageCode
    ) {
        return getServiceDescriptionsV8(
            V8ConnectorProperties.builder()
                .ars(ars)
                .q(q)
                .page(page)
                .size(size)
                .personalMatters(personalMatters)
                .facades(facades)
                .languageCode(languageCode)
                .build()
        );
    }

    private static Response getServiceDescriptionsV8(V8ConnectorProperties connectorProperties) {
        RequestSpecification requestSpecification = given(RestAssuredUtil.getRequestSpec()).header(
            "Accept-Language",
            connectorProperties.languageCode
        );
        requestSpecification = addParam(requestSpecification, "ars", connectorProperties.ars);
        requestSpecification = addParam(requestSpecification, "q", connectorProperties.q);
        requestSpecification = addParam(requestSpecification, "page", connectorProperties.page);
        requestSpecification = addParam(requestSpecification, "size", connectorProperties.size);
        requestSpecification = addParam(requestSpecification, "personalMatters", connectorProperties.personalMatters);
        requestSpecification = addParam(requestSpecification, "facades", connectorProperties.facades);
        requestSpecification = addParam(requestSpecification, "useArsHierarchy", connectorProperties.useArsHierarchy);
        if (connectorProperties.clientId != null) {
            requestSpecification = requestSpecification.header(
                Constants.API_CLIENT_ID_HEADER_NAME,
                connectorProperties.clientId
            );
        }

        Response response = requestSpecification.when().get("/v8beta1/servicedescriptions");
        return ContentLanguageValidator.validateContentLanguage(response, connectorProperties.languageCode);
    }

    /**
     * PV-8448
     */
    public static Response getPersonalMattersV1(String ars, String languageCode) {
        return getPersonalMattersV1(ars, languageCode, null);
    }

    public static Response getPersonalMattersV1(String ars, String languageCode, String accept) {
        RequestSpecification spec = given(RestAssuredUtil.getRequestSpec()).header("Accept-Language", languageCode);
        if (accept != null) {
            spec = spec.header("Accept", accept);
        }
        Response response = spec.when().get("/v1/personalmatters/{ars}", ars);
        return ContentLanguageValidator.validateContentLanguage(response, languageCode);
    }

    @Builder
    public static class V5ConnectorProperties {

        private String ars;

        private String q;

        @Builder.Default
        private String language = Constants.LANGUAGE_DE;

        @Builder.Default
        private LocalDate validDate = DEFAULT_VALID_DATE;

        @Builder.Default
        private Boolean useArsHierarchy = DEFAULT_USE_ARS_HIERARCHY;

        @Builder.Default
        private Boolean applyDynamicParams = false;
    }

    /**
     * Use {@link #getServiceDescriptionsDetailV5(V5ConnectorProperties)} instead
     */
    public static Response getServiceDescriptionsDetailV5(String ars, String q) {
        return getServiceDescriptionsDetailV5(V5ConnectorProperties.builder().ars(ars).q(q).build());
    }

    /**
     * Use {@link #getServiceDescriptionsDetailV5(V5ConnectorProperties)} instead
     */
    public static Response getServiceDescriptionsDetailV5(String ars, String q, boolean useArsHierarchy) {
        return getServiceDescriptionsDetailV5(
            V5ConnectorProperties.builder().ars(ars).q(q).useArsHierarchy(useArsHierarchy).build()
        );
    }

    /**
     * Use {@link #getServiceDescriptionsDetailV5(V5ConnectorProperties)} instead
     */
    public static Response getServiceDescriptionsDetailV5(String ars, String q, String language) {
        return getServiceDescriptionsDetailV5(V5ConnectorProperties.builder().ars(ars).q(q).language(language).build());
    }

    /**
     * Use {@link #getServiceDescriptionsDetailV5(V5ConnectorProperties)} instead
     */
    public static Response getServiceDescriptionsDetailV5(String ars, String q, LocalDate validDate, String language) {
        return getServiceDescriptionsDetailV5(
            V5ConnectorProperties.builder().ars(ars).q(q).validDate(validDate).language(language).build()
        );
    }

    public static Response getServiceDescriptionsDetailV5(V5ConnectorProperties connectorProperties) {
        RequestSpecification requestSpecification = given(RestAssuredUtil.getRequestSpec()).header(
            "Accept-Language",
            connectorProperties.language
        );
        requestSpecification = addParam(requestSpecification, "q", connectorProperties.q);
        if (connectorProperties.validDate != null) {
            requestSpecification = addParam(
                requestSpecification,
                "validDate",
                connectorProperties.validDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            );
        }
        requestSpecification = addParam(requestSpecification, "useArsHierarchy", connectorProperties.useArsHierarchy);
        requestSpecification = addParam(
            requestSpecification,
            "applyDynamicParams",
            connectorProperties.applyDynamicParams
        );

        Response response = requestSpecification
            .when()
            .get("/v5/servicedescriptions/{ars}/detail", connectorProperties.ars);
        return ContentLanguageValidator.validateContentLanguage(response, connectorProperties.language);
    }

    public static Response getServiceDescriptionsDetailV6(String ars, String lbid) {
        return getServiceDescriptionsDetailV6(ars, lbid, Constants.LANGUAGE_DE);
    }

    public static Response getServiceDescriptionsDetailV6(String ars, String lbid, String language) {
        return getServiceDescriptionsDetailV6(ars, lbid, language, null);
    }

    /**
     * Ruft die V6-Detailansicht mit einer expliziten Client-ID (Header {@code pvog-sud-api-client-id}) auf.
     */
    public static Response getServiceDescriptionsDetailV6(String ars, String lbid, String language, String clientId) {
        RequestSpecification requestSpecification = given(RestAssuredUtil.getRequestSpec()).header(
            "Accept-Language",
            language
        );

        requestSpecification = addParam(requestSpecification, "ars", ars);
        requestSpecification = addParam(requestSpecification, "useArsHierarchy", DEFAULT_USE_ARS_HIERARCHY);
        if (clientId != null) {
            requestSpecification = requestSpecification.header(Constants.API_CLIENT_ID_HEADER_NAME, clientId);
        }

        Response response = requestSpecification.when().get("/v6/servicedescriptions/{lbid}/detail", lbid);

        return ContentLanguageValidator.validateContentLanguage(response, language);
    }

    public static Response getServiceDescriptionsLeikaIdV3(String ars, String leikaId) {
        return getServiceDescriptionsLeikaIdV3(
            ars,
            null,
            List.of(leikaId),
            null,
            0,
            20,
            null,
            null,
            Constants.LANGUAGE_DE,
            true
        );
    }

    public static Response getServiceDescriptionsLeikaIdV3(String ars, String leikaId, boolean useArsHierarchy) {
        return getServiceDescriptionsLeikaIdV3(
            ars,
            null,
            List.of(leikaId),
            null,
            0,
            20,
            null,
            null,
            Constants.LANGUAGE_DE,
            useArsHierarchy
        );
    }

    public static Response getServiceDescriptionsLeikaIdV3(String ars, String leikaId, String languageCode) {
        return getServiceDescriptionsLeikaIdV3(
            ars,
            null,
            List.of(leikaId),
            null,
            0,
            20,
            null,
            null,
            languageCode,
            true
        );
    }

    public static Response getServiceDescriptionsLeikaIdV3(
        String ars,
        String leikaId,
        String languageCode,
        String param
    ) {
        return getServiceDescriptionsLeikaIdV3(
            ars,
            List.of(param),
            List.of(leikaId),
            null,
            0,
            20,
            null,
            null,
            languageCode,
            true
        );
    }

    public static Response getServiceDescriptionsLeikaIdV3(String ars, List<String> leikaIds) {
        return getServiceDescriptionsLeikaIdV3(
            ars,
            null,
            leikaIds,
            null,
            0,
            20,
            null,
            null,
            Constants.LANGUAGE_DE,
            true
        );
    }

    public static Response getServiceDescriptionsLeikaIdV3(String ars, List<String> leikaIds, List<String> qList) {
        return getServiceDescriptionsLeikaIdV3(
            ars,
            qList,
            leikaIds,
            null,
            0,
            20,
            null,
            null,
            Constants.LANGUAGE_DE,
            true
        );
    }

    /**
     * Ruft die LeiKa-ID-Suche mit einer expliziten Client-ID (Header {@code pvog-sud-api-client-id}) auf.
     */
    public static Response getServiceDescriptionsLeikaIdV3WithClientId(
        String ars,
        String leikaId,
        String languageCode,
        String clientId
    ) {
        return getServiceDescriptionsLeikaIdV3(
            ars,
            null,
            List.of(leikaId),
            null,
            0,
            20,
            null,
            null,
            languageCode,
            true,
            clientId
        );
    }

    public static Response getServiceDescriptionsLeikaIdV4(String ars, String leikaId) {
        return getServiceDescriptionsLeikaIdV4(
            ars,
            null,
            List.of(leikaId),
            DEFAULT_PAGE,
            DEFAULT_SIZE,
            DEFAULT_LANGUAGE_CODE,
            DEFAULT_USE_ARS_HIERARCHY,
            null
        );
    }

    public static Response getServiceDescriptionsLeikaIdV4(String ars, String leikaId, boolean useArsHierarchy) {
        return getServiceDescriptionsLeikaIdV4(
            ars,
            null,
            List.of(leikaId),
            DEFAULT_PAGE,
            DEFAULT_SIZE,
            DEFAULT_LANGUAGE_CODE,
            useArsHierarchy,
            null
        );
    }

    public static Response getServiceDescriptionsLeikaIdV4(String ars, String leikaId, String languageCode) {
        return getServiceDescriptionsLeikaIdV4(
            ars,
            null,
            List.of(leikaId),
            DEFAULT_PAGE,
            DEFAULT_SIZE,
            languageCode,
            DEFAULT_USE_ARS_HIERARCHY,
            null
        );
    }

    public static Response getServiceDescriptionsLeikaIdV4(
        String ars,
        String leikaId,
        String languageCode,
        String query
    ) {
        return getServiceDescriptionsLeikaIdV4(
            ars,
            List.of(query),
            List.of(leikaId),
            DEFAULT_PAGE,
            DEFAULT_SIZE,
            languageCode,
            DEFAULT_USE_ARS_HIERARCHY,
            null
        );
    }

    public static Response getServiceDescriptionsLeikaIdV4(String ars, List<String> leikaIds) {
        return getServiceDescriptionsLeikaIdV4(
            ars,
            null,
            leikaIds,
            DEFAULT_PAGE,
            DEFAULT_SIZE,
            DEFAULT_LANGUAGE_CODE,
            DEFAULT_USE_ARS_HIERARCHY,
            null
        );
    }

    public static Response getServiceDescriptionsLeikaIdV4(String ars, List<String> leikaIds, List<String> query) {
        return getServiceDescriptionsLeikaIdV4(
            ars,
            query,
            leikaIds,
            DEFAULT_PAGE,
            DEFAULT_SIZE,
            DEFAULT_LANGUAGE_CODE,
            DEFAULT_USE_ARS_HIERARCHY,
            null
        );
    }

    public static Response getServiceDescriptionsLeikaIdV4WithClientId(
        String ars,
        String leikaId,
        String languageCode,
        String clientId
    ) {
        return getServiceDescriptionsLeikaIdV4(
            ars,
            null,
            List.of(leikaId),
            DEFAULT_PAGE,
            DEFAULT_SIZE,
            languageCode,
            DEFAULT_USE_ARS_HIERARCHY,
            clientId
        );
    }

    /**
     * Entspricht der V3-Überladung mit dem (in der Beta-API entfallenen) Parameter validDate. Da als
     * Gültigkeitsdatum in der Beta-API immer der Zeitpunkt der Anfrage verwendet wird, entfällt das
     * entsprechende Argument hier ersatzlos.
     */
    public static Response getServiceDescriptionsLeikaIdV4(
        String ars,
        List<String> q,
        List<String> leikaIds,
        Integer page,
        Integer size,
        String languageCode,
        boolean useArsHierarchy,
        String clientId
    ) {
        Map<String, Object> params = new HashMap<>();
        params.put("ars", ars);
        params.put("q", q);
        params.put("leikaIds", leikaIds);
        params.put("page", page);
        params.put("size", size);
        params.put("useArsHierarchy", useArsHierarchy);
        RequestSpecification requestSpecification = given(RestAssuredUtil.getRequestSpec())
            .header("Accept-Language", languageCode)
            .params(params);
        if (clientId != null) {
            requestSpecification = requestSpecification.header(Constants.API_CLIENT_ID_HEADER_NAME, clientId);
        }
        Response response = requestSpecification.when().get("/v4beta1/servicedescriptions/leikaid");
        return ContentLanguageValidator.validateContentLanguage(response, languageCode);
    }

    public static Response getServiceDescriptionsLeikaIdV3(
        String ars,
        List<String> q,
        List<String> leikaIds,
        Boolean withOrgIds,
        Integer page,
        Integer size,
        List<String> ignoreLeikaTypes,
        LocalDate validDate,
        String languageCode,
        boolean useArsHierarchy
    ) {
        return getServiceDescriptionsLeikaIdV3(
            ars,
            q,
            leikaIds,
            withOrgIds,
            page,
            size,
            ignoreLeikaTypes,
            validDate,
            languageCode,
            useArsHierarchy,
            null
        );
    }

    public static Response getServiceDescriptionsLeikaIdV3(
        String ars,
        List<String> q,
        List<String> leikaIds,
        Boolean withOrgIds,
        Integer page,
        Integer size,
        List<String> ignoreLeikaTypes,
        LocalDate validDate,
        String languageCode,
        boolean useArsHierarchy,
        String clientId
    ) {
        Map<String, Object> params = new HashMap<>();
        params.computeIfAbsent("ars", val -> ars);
        params.computeIfAbsent("q", val -> q);
        params.computeIfAbsent("leikaIds", val -> leikaIds);
        params.computeIfAbsent("withOrgIds", val -> withOrgIds);
        params.computeIfAbsent("page", val -> page);
        params.computeIfAbsent("size", val -> size);
        params.computeIfAbsent("ignoreLeikaTypes", val -> ignoreLeikaTypes);
        params.computeIfAbsent("validDate", val ->
            validDate != null ? validDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null
        );
        params.put("useArsHierarchy", useArsHierarchy);
        RequestSpecification requestSpecification = given(RestAssuredUtil.getRequestSpec())
            .header("Accept-Language", languageCode)
            .params(params);
        if (clientId != null) {
            requestSpecification = requestSpecification.header(Constants.API_CLIENT_ID_HEADER_NAME, clientId);
        }
        Response response = requestSpecification.when().get("/v3/servicedescriptions/leikaid");
        return ContentLanguageValidator.validateContentLanguage(response, languageCode);
    }

    public static Response getServiceDescriptionsLeikaHierarchyV2(String ars, String leikaId) {
        return getServiceDescriptionsLeikaHierarchyV2(ars, List.of(leikaId), 0, 100, null, null);
    }

    /**
     * Ruft die LeiKa-Hierarchie-Suche mit einer expliziten Client-ID (Header {@code pvog-sud-api-client-id}) auf.
     */
    public static Response getServiceDescriptionsLeikaHierarchyV2(String ars, String leikaId, String clientId) {
        return getServiceDescriptionsLeikaHierarchyV2(ars, List.of(leikaId), 0, 100, null, null, clientId);
    }

    public static Response getServiceDescriptionsLeikaHierarchyV3(String ars, String leikaId) {
        return getServiceDescriptionsLeikaHierarchyV3(ars, List.of(leikaId), 0, 100, null);
    }

    public static Response getServiceDescriptionsLeikaHierarchyV3(String ars, String leikaId, String clientId) {
        return getServiceDescriptionsLeikaHierarchyV3(ars, List.of(leikaId), 0, 100, clientId);
    }

    public static Response getServiceDescriptionsLeikaIdV3(
        String ars,
        List<String> q,
        List<String> leikaIds,
        Boolean withOrgIds,
        Integer page,
        Integer size,
        List<String> ignoreLeikaTypes,
        LocalDate validDate,
        String languageCode
    ) {
        return getServiceDescriptionsLeikaIdV3(
            ars,
            q,
            leikaIds,
            withOrgIds,
            page,
            size,
            ignoreLeikaTypes,
            validDate,
            languageCode,
            true
        );
    }

    public static Response getServiceDescriptionsLeikaHierarchyV2(
        String ars,
        List<String> leikaIds,
        Integer page,
        Integer size,
        List<String> ignoreLeikaTypes,
        LocalDate validDate
    ) {
        return getServiceDescriptionsLeikaHierarchyV2(ars, leikaIds, page, size, ignoreLeikaTypes, validDate, null);
    }

    public static Response getServiceDescriptionsLeikaHierarchyV2(
        String ars,
        List<String> leikaIds,
        Integer page,
        Integer size,
        List<String> ignoreLeikaTypes,
        LocalDate validDate,
        String clientId
    ) {
        Map<String, Object> params = new HashMap<>();
        params.computeIfAbsent("ars", val -> ars);
        params.computeIfAbsent("leikaIds", val -> leikaIds);
        params.computeIfAbsent("page", val -> page);
        params.computeIfAbsent("size", val -> size);
        params.computeIfAbsent("ignoreLeikaTypes", val -> ignoreLeikaTypes);
        params.computeIfAbsent("validDate", val ->
            validDate != null ? validDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null
        );

        RequestSpecification requestSpecification = given(RestAssuredUtil.getRequestSpec())
            .header("Accept-Language", Constants.LANGUAGE_DE)
            .params(params);
        if (clientId != null) {
            requestSpecification = requestSpecification.header(Constants.API_CLIENT_ID_HEADER_NAME, clientId);
        }
        Response response = requestSpecification.when().get("/v2/servicedescriptions/leikahierarchy");
        return ContentLanguageValidator.validateContentLanguage(response, Constants.LANGUAGE_DE);
    }

    /**
     * Entspricht der V2-Überladung mit den (in der Beta-API entfallenen) Parametern ignoreLeikaTypes und
     * validDate. Da in der Beta-API keine Leika-Typisierungen ausgeschlossen werden und als
     * Gültigkeitsdatum immer der Zeitpunkt der Anfrage verwendet wird, entfallen die entsprechenden
     * Argumente hier ersatzlos.
     */
    public static Response getServiceDescriptionsLeikaHierarchyV3(
        String ars,
        List<String> leikaIds,
        Integer page,
        Integer size,
        String clientId
    ) {
        Map<String, Object> params = new HashMap<>();
        params.put("ars", ars);
        params.put("leikaIds", leikaIds);
        params.put("page", page);
        params.put("size", size);
        RequestSpecification requestSpecification = given(RestAssuredUtil.getRequestSpec())
            .header("Accept-Language", Constants.LANGUAGE_DE)
            .params(params);
        if (clientId != null) {
            requestSpecification = requestSpecification.header(Constants.API_CLIENT_ID_HEADER_NAME, clientId);
        }
        Response response = requestSpecification.when().get("/v3beta1/servicedescriptions/leikahierarchy");
        return ContentLanguageValidator.validateContentLanguage(response, Constants.LANGUAGE_DE);
    }

    public static Response getServiceDescriptionsSuggestionsV1(String ars, String q, Boolean skipDuplicates) {
        return getServiceDescriptionsSuggestionsV1(ars, q, skipDuplicates, Constants.LANGUAGE_DE);
    }

    public static Response getServiceDescriptionsSuggestionsV1(
        String ars,
        String q,
        Boolean skipDuplicates,
        String language
    ) {
        Map<String, Object> params = new HashMap<>();
        if (q != null) {
            params.put("q", q);
        }
        if (skipDuplicates != null) {
            params.put("skipDuplicates", skipDuplicates);
        }

        Response response = given(RestAssuredUtil.getRequestSpec())
            .header("Accept-Language", language)
            .params(params)
            .when()
            .get("/v1/servicedescriptions/suggestions/{ars}", ars);
        return ContentLanguageValidator.validateContentLanguage(response, language);
    }

    public static Response getServiceDescriptionsArsJzufiV3(String id, XzufiVersion version) {
        Map<String, Object> params = new HashMap<>();
        params.computeIfAbsent("id", val -> id);

        return given(RestAssuredUtil.getRequestSpec())
            .header("Accept", "application/json;xzufi-version=" + version)
            .params(params)
            .when()
            .get("/v3/servicedescriptions/jzufi");
    }

    public static Response getServiceDescriptionsArsJzufi231V1(String id) {
        return getServiceDescriptionsArsJzufi231V1(id, null);
    }

    public static Response getServiceDescriptionsJzufi231V2(String id) {
        return given(RestAssuredUtil.getRequestSpec())
            .pathParam("id", id)
            .when()
            .get("/v2/servicedescriptions/{id}/jzufi-2-3");
    }

    public static Response getServiceDescriptionsArsJzufi231V1(String id, LocalDate validDate) {
        Map<String, Object> params = new HashMap<>();
        if (validDate != null) {
            params.put("validdate", validDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        return given(RestAssuredUtil.getRequestSpec())
            .params(params)
            .when()
            .pathParam("id", id)
            .get("/v1/servicedescriptions/{id}/jzufi-2-3");
    }

    public static Response getServiceDescriptionsArsJzufi220V1(String id) {
        return getServiceDescriptionsArsJzufi220V1(id, null);
    }

    public static Response getServiceDescriptionsArsJzufi220V1(String id, LocalDate validDate) {
        Map<String, Object> params = new HashMap<>();
        if (validDate != null) {
            params.put("validdate", validDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        return given(RestAssuredUtil.getRequestSpec())
            .params(params)
            .when()
            .pathParam("id", id)
            .get("/v1/servicedescriptions/{id}/jzufi-2-2");
    }

    public static Response getServiceDescriptionsExport(String ars, String lastId) {
        return getServiceDescriptionsExport(ars, lastId, null, Constants.LANGUAGE_DE);
    }

    public static Response getServiceDescriptionsExport(
        String ars,
        String lastId,
        LocalDate validDate,
        String languageCode
    ) {
        Map<String, Object> params = new HashMap<>();
        if (ars != null) {
            params.put("ars", ars);
        }
        if (lastId != null) {
            params.put("lastId", lastId);
        }
        if (validDate != null) {
            params.put("validDate", validDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        Response response = given(RestAssuredUtil.getRequestSpec())
            .header("Accept-Language", languageCode)
            .params(params)
            .when()
            .get("/v1/servicedescriptions/export");
        return ContentLanguageValidator.validateContentLanguage(response, languageCode);
    }

    private static RequestSpecification addParam(RequestSpecification spec, String name, Object value) {
        if (value != null) {
            spec.param(name, value);
        }
        return spec;
    }

    private static RequestSpecification addParam(RequestSpecification spec, String name, Collection<?> values) {
        if (values != null) {
            spec.param(name, values);
        }
        return spec;
    }
}
