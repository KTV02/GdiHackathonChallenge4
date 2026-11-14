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

import com.sun.istack.Nullable;
import de.dataport.pvog.testdatengenerator.generator.model.SaDMessage;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpStatus;
import suchdienst.integrationstest.common.RestAssuredUtil;

public class ImportQueueConnector {

    public static String pushXzufi(SaDMessage message) {
        String producer = message.getProducer();
        String nachrichtenId = message.getNachrichtenId();
        String transaktionsId = message.getTransaktionsId();
        String transaktionsPosition = message.getTransaktionsPosition();
        String payload = message.getXzufi();
        String schemeAgencyId = message.getSchemeAgencyId();
        String contentType = "application/xml;charset=UTF-8;xzufi-version=%s".formatted(message.getXzufiVersion());
        return given(RestAssuredUtil.getRequestSpec(ContentType.XML))
            .header("X-PVOG-NACHRICHTUUID", nachrichtenId)
            .header("X-PVOG-TRANSAKTIONID", transaktionsId)
            .header("X-PVOG-TRANSAKTIONPOSITION", transaktionsPosition)
            .header("X-PVOG-PRODUCER", producer)
            .header("X-PVOG-AGENCYID", schemeAgencyId)
            .header("Content-Type", contentType)
            .body(payload)
            .post("/push/xzufi-22/enqueue")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .extract()
            .jsonPath()
            .getString("id");
    }

    public static String pushXzufi(
        String xzufiXml,
        String nachrichtenUuid,
        String transaktionsId,
        String transaktionPosition
    ) {
        return given(RestAssuredUtil.getRequestSpec(ContentType.XML))
            .header("X-PVOG-NACHRICHTUUID", nachrichtenUuid)
            .header("X-PVOG-TRANSAKTIONID", transaktionsId)
            .header("X-PVOG-TRANSAKTIONPOSITION", transaktionPosition)
            .header("X-PVOG-PRODUCER", "redsys")
            .header("X-PVOG-AGENCYID", "L100042")
            .header("Content-Type", "application/xml;charset=UTF-8;xzufi-version=2.2.0")
            .body(xzufiXml)
            .post("/push/xzufi-22/enqueue")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .extract()
            .jsonPath()
            .getString("id");
    }

    public static JsonPath getQueueStatus(String id, String status) {
        return given(RestAssuredUtil.getRequestSpec(ContentType.ANY))
            .params("id", id, "status", status)
            .get("/push/xzufi-22/queue")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .extract()
            .jsonPath();
    }

    public static JsonPath getQueueStatusByAcceptedAfter(String status, String acceptedAfter) {
        return given(RestAssuredUtil.getRequestSpec(ContentType.ANY))
            .params("status", status, "acceptedAfter", acceptedAfter)
            .get("/push/xzufi-22/queue")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .extract()
            .jsonPath();
    }

    public static Response getQueueStatus(
        @Nullable String id,
        @Nullable String messageId,
        @Nullable String transactionId,
        @Nullable String acceptedAfter,
        @Nullable String acceptedBefore,
        @Nullable String status
    ) {
        Map<String, String> params = new HashMap<>();
        params.computeIfAbsent("id", val -> id);
        params.computeIfAbsent("messageId", val -> messageId);
        params.computeIfAbsent("transactionId", val -> transactionId);
        params.computeIfAbsent("acceptedAfter", val -> acceptedAfter);
        params.computeIfAbsent("acceptedBefore", val -> acceptedBefore);
        params.computeIfAbsent("status", val -> status);
        return given(RestAssuredUtil.getRequestSpec(ContentType.ANY)).params(params).get("/push/xzufi-22/queue");
    }

    public static Response updateStatus(String id, String status) {
        return given(RestAssuredUtil.getRequestSpec(ContentType.JSON))
            .queryParam("id", id)
            .queryParam("status", status)
            .post("/push/xzufi-22/queue/updateStatus");
    }

    public static void reopen(String id) {
        reopen(List.of(id));
    }

    public static void reopen(List<String> idList) {
        Map<String, List<String>> params = new HashMap<>();
        if (idList != null) {
            params.put("ids", idList);
        }

        given(RestAssuredUtil.getRequestSpec())
            .params(params)
            .post("/push/xzufi-22/queue/reopen")
            .then()
            .statusCode(HttpStatus.SC_ACCEPTED);
    }

    public static Response reopenWithResponse(List<String> idList) {
        Map<String, List<String>> params = new HashMap<>();
        if (idList != null) {
            params.put("ids", idList);
        }

        return given(RestAssuredUtil.getRequestSpec()).params(params).post("/push/xzufi-22/queue/reopen");
    }

    public static Response delete(String id) {
        return given(RestAssuredUtil.getRequestSpec(ContentType.ANY)).param("id", id).delete("/push/xzufi-22/queue");
    }

    public static Boolean getUpToDate() {
        return given(RestAssuredUtil.getRequestSpec())
            .get("/admin/verwaltungsleistung/uptodate")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .extract()
            .body()
            .as(Boolean.class);
    }

    public static Response requestToken(String id) {
        return given(RestAssuredUtil.getRequestSpec(ContentType.ANY, ContentType.TEXT)).get(
            "/push/xzufi-22/queue/{id}/request-download-import-request-token",
            id
        );
    }

    public static Response downLoadByToken(String downloadToken) {
        return given(RestAssuredUtil.getRequestSpec(ContentType.TEXT, ContentType.BINARY)).get(
            "/admin/download/xzufi?downloadToken=" + downloadToken
        );
    }
}
