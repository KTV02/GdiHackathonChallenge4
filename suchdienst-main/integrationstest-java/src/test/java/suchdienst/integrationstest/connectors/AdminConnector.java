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
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import org.apache.http.HttpStatus;
import suchdienst.integrationstest.common.Constants;
import suchdienst.integrationstest.common.RestAssuredUtil;

public class AdminConnector {

    public static void uploadArs(File uploadFile) {
        given(RestAssuredUtil.getRequestSpec())
            .multiPart(uploadFile)
            .post("/admin/ars")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    public static void uploadCodelistSucceeded(String codelistString) {
        uploadCodelist(codelistString).then().statusCode(HttpStatus.SC_OK);
    }

    public static Response uploadCodelist(String codelistString) {
        return given(RestAssuredUtil.getRequestSpec())
            .multiPart(
                "file",
                "codelist.xml",
                codelistString.getBytes(StandardCharsets.UTF_8),
                "application/octet-stream"
            )
            .post("/admin/codelist/upload");
    }

    public static Response getBackofficeElsCodelists(Integer page, Integer size) {
        HashMap<String, Object> params = new HashMap<>();
        if (page != null) {
            params.put("page", page);
        }
        if (size != null) {
            params.put("size", size);
        }

        return given(RestAssuredUtil.getRequestSpec())
            .header("Accept-Language", Constants.LANGUAGE_DE)
            .params(params)
            .when()
            .get("/admin/backoffice/elastic-search/codelists");
    }

    public static Response getBackofficeElsCodelistsStats() {
        return given(RestAssuredUtil.getRequestSpec())
            .header("Accept-Language", Constants.LANGUAGE_DE)
            .when()
            .get("/admin/backoffice/elastic-search/codelists/stats");
    }

    public static Response getCodelistEntry(String uri, String version) {
        HashMap<String, Object> params = new HashMap<>();
        if (uri != null) {
            params.put("uri", uri);
        }
        if (version != null) {
            params.put("version", version);
        }

        return given(RestAssuredUtil.getRequestSpec())
            .header("Accept-Language", Constants.LANGUAGE_DE)
            .params(params)
            .when()
            .get("/admin/codelist/entries");
    }

    public static Response getCodelistEntryById(String id) {
        return given(RestAssuredUtil.getRequestSpec())
            .header("Accept-Language", Constants.LANGUAGE_DE)
            .when()
            .get("/admin/codelist/{id}", id);
    }

    public static Response downloadEntry(String uri, String version) {
        HashMap<String, Object> params = new HashMap<>();
        if (uri != null) {
            params.put("uri", uri);
        }
        if (version != null) {
            params.put("version", version);
        }

        return given(RestAssuredUtil.getRequestSpec())
            .header("Accept-Language", Constants.LANGUAGE_DE)
            .params(params)
            .when()
            .get("/admin/codelist/download");
    }

    public static void uploadPlz(File file) {
        given(RestAssuredUtil.getRequestSpec())
            .multiPart(file)
            .param("delimiter", ";")
            .param("keyKind", "AGS")
            .param("rsColumn", "0")
            .param("zipColumn", "1")
            .param("headerLine", "false")
            .post("/admin/ars/zip")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    public static void updateLeistungskatalog() {
        given(RestAssuredUtil.getRequestSpec())
            .header("Accept-Language", Constants.LANGUAGE_DE)
            .when()
            .post("/admin/leistungskatalog/update")
            .then()
            .statusCode(HttpStatus.SC_OK);
    }

    public static Response dataOverview() {
        return given(RestAssuredUtil.getRequestSpec())
            .header("Accept-Language", Constants.LANGUAGE_DE)
            .when()
            .get("/admin/dataoverview");
    }

    public static Response recalculateOnlineServiceRelations() {
        return given(RestAssuredUtil.getRequestSpec())
            .header("Accept-Language", Constants.LANGUAGE_DE)
            .when()
            .post("/admin/onlineservices/relations/calculate/all");
    }
}
