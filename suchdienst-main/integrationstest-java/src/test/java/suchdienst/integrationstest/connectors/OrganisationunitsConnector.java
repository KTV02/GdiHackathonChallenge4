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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import suchdienst.integrationstest.common.Constants;
import suchdienst.integrationstest.common.Constants.XzufiVersion;
import suchdienst.integrationstest.common.RestAssuredUtil;
import suchdienst.integrationstest.validator.ContentLanguageValidator;

public class OrganisationunitsConnector {

    /**
     * @param q Die ID der Organisationseinheit
     * @return RestAssured Response Objekt mit Antwort vom Suchdienst
     */
    public static Response getOrganisationunitsDetailV5(String q) {
        Response response = getOrganisationunitsDetailV5(q, null, null, null, Constants.LANGUAGE_DE);
        return ContentLanguageValidator.validateContentLanguage(response, Constants.LANGUAGE_DE);
    }

    /**
     * @param q    Die ID der Organisationseinheit
     * @param lbId Filtert Kontaktpersonen und Rollen anhand der Zuständigkeit für die Leistung mit dieser ID
     * @return RestAssured Response Objekt mit Antwort vom Suchdienst
     */
    public static Response getOrganisationunitsDetailV5(String q, String lbId) {
        Response response = getOrganisationunitsDetailV5(q, null, lbId, null, Constants.LANGUAGE_DE);
        return ContentLanguageValidator.validateContentLanguage(response, Constants.LANGUAGE_DE);
    }

    /**
     * @param q         Die ID der Organisationseinheit
     * @param validDate Das Gültigkeitsdatum in der Form yyyy-MM-dd
     * @return RestAssured Response Objekt mit Antwort vom Suchdienst
     */
    public static Response getOrganisationunitsDetailV5(String q, LocalDate validDate) {
        Response response = getOrganisationunitsDetailV5(q, null, null, validDate, Constants.LANGUAGE_DE);
        return ContentLanguageValidator.validateContentLanguage(response, Constants.LANGUAGE_DE);
    }

    /**
     * @param q    Die ID der Organisationseinheit
     * @param ars  Filtert die Kontaktpersonen anhand der räumlichen Zuständigkeit
     * @param lbId Filtert Kontaktpersonen und Rollen anhand der Zuständigkeit für die Leistung mit dieser ID
     * @return RestAssured Response Objekt mit Antwort vom Suchdienst
     */
    public static Response getOrganisationunitsDetailV5(String q, String ars, String lbId) {
        Response response = getOrganisationunitsDetailV5(q, ars, lbId, null, Constants.LANGUAGE_DE);
        return ContentLanguageValidator.validateContentLanguage(response, Constants.LANGUAGE_DE);
    }

    /**
     * @param q            Die ID der Organisationseinheit
     * @param ars          Filtert die Kontaktpersonen anhand der räumlichen Zuständigkeit
     * @param lbId         Filtert Kontaktpersonen und Rollen anhand der Zuständigkeit für die Leistung mit dieser ID
     * @param validDate    Das Gültigkeitsdatum in der Form yyyy-MM-dd
     * @param languageCode Setzt den Accept-Language Header der Anfrage
     * @return RestAssured Response Objekt mit Antwort vom Suchdienst
     */
    public static Response getOrganisationunitsDetailV5(
        String q,
        String ars,
        String lbId,
        LocalDate validDate,
        String languageCode
    ) {
        var requestSpecification = createRequestSpecification(languageCode);
        requestSpecification = addParam(requestSpecification, "q", q);
        requestSpecification = addParam(requestSpecification, "ars", ars);
        requestSpecification = addParam(requestSpecification, "lbId", lbId);
        requestSpecification = addParam(
            requestSpecification,
            "validDate",
            validDate,
            OrganisationunitsConnector::formatLocalDate
        );

        Response response = requestSpecification.when().get("/v5/organisationunits/detail");
        return ContentLanguageValidator.validateContentLanguage(response, languageCode);
    }

    public static Response getOrganisationunitsJzufiV2(String id, XzufiVersion xzufiversion) {
        Map<String, Object> params = new HashMap<>();
        params.computeIfAbsent("id", val -> id);

        return given(RestAssuredUtil.getRequestSpec())
            .header("Accept", "application/json;xzufi-version=" + xzufiversion)
            .params(params)
            .when()
            .get("/v2/organisationunits/jzufi");
    }

    public static Response getOrganisationunitsJzufi220V1(String id) {
        return getOrganisationunitsJzufi220V1(id, null);
    }

    public static Response getOrganisationunitsJzufi220V1(String id, LocalDate validDate) {
        Map<String, Object> params = new HashMap<>();
        if (validDate != null) {
            params.put("validdate", validDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        return given(RestAssuredUtil.getRequestSpec())
            .pathParam("id", id)
            .params(params)
            .when()
            .get("/v1/organisationunits/{id}/jzufi-2-2");
    }

    public static Response getOrganisationunitsJzufi231V1(String id) {
        return getOrganisationunitsJzufi231V1(id, null);
    }

    public static Response getOrganisationunitsJzufi231V2(String id) {
        return given(RestAssuredUtil.getRequestSpec())
            .pathParam("id", id)
            .when()
            .get("/v2/organisationunits/{id}/jzufi-2-3");
    }

    public static Response getOrganisationunitsJzufi231V1(String id, LocalDate validDate) {
        Map<String, Object> params = new HashMap<>();
        if (validDate != null) {
            params.put("validdate", validDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        return given(RestAssuredUtil.getRequestSpec())
            .pathParam("id", id)
            .params(params)
            .when()
            .get("/v1/organisationunits/{id}/jzufi-2-3");
    }

    public static Response getOrganisationunitsTitlesV2(String ars, String lbid) {
        Response response = getOrganisationunitsTitlesV2(ars, lbid, null, Constants.LANGUAGE_DE);
        return ContentLanguageValidator.validateContentLanguage(response, Constants.LANGUAGE_DE);
    }

    public static Response getOrganisationunitsTitlesV2(
        String ars,
        String lbid,
        LocalDate validDate,
        String languageCode
    ) {
        var requestSpecification = createRequestSpecification(languageCode);
        requestSpecification = addParam(requestSpecification, "ars", ars);
        requestSpecification = addParam(requestSpecification, "lbId", lbid);
        requestSpecification = addParam(
            requestSpecification,
            "validDate",
            validDate,
            OrganisationunitsConnector::formatLocalDate
        );

        Response response = requestSpecification.when().get("/v2/organisationunits/titles");
        return ContentLanguageValidator.validateContentLanguage(response, languageCode);
    }

    public static Response getArsByOeIdAndLeikaId(String leikaId, String oeId) {
        return given(RestAssuredUtil.getRequestSpec())
            .header("Accept", "application/json")
            .params("leikaId", leikaId)
            .when()
            .get("/v1/organisationunits/{oeId}/ars", oeId);
    }

    private static RequestSpecification createRequestSpecification(String languageCode) {
        return given(RestAssuredUtil.getRequestSpec()).header("Accept-Language", languageCode);
    }

    private static RequestSpecification addParam(RequestSpecification spec, String name, String value) {
        if (value != null) {
            spec = spec.param(name, value);
        }
        return spec;
    }

    private static <T> RequestSpecification addParam(
        RequestSpecification spec,
        String name,
        T value,
        Function<T, String> formatter
    ) {
        if (value != null) {
            spec = spec.param(name, formatter.apply(value));
        }
        return spec;
    }

    private static String formatLocalDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
