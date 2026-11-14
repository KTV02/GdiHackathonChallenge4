/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.clienttest.transport;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import de.dataport.suchdienst.client.api.JzuFiApi;
import de.dataport.suchdienst.client.api.StatistikApi;
import de.dataport.suchdienst.client.model.ApiErrorModel;
import de.dataport.suchdienst.client.model.ErrorModel;
import de.dataport.suchdienst.clienttest.support.StrictApiClientFactory;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Testziel: Nicht-2xx-Antworten erhalten Status und Body, dessen striktes Binding keine unbekannten Enums toleriert.
 * Der generierte Client wirft eine HTTP-Exception statt eines Fehler-DTOs. Deren Body
 * wird hier anschließend ausdrücklich an das dokumentierte Fehlermodell gebunden.
 */
class HttpErrorTransportTest {

    private MockRestServiceServer server;
    private StatistikApi statisticsApi;
    private JzuFiApi relationsApi;

    @BeforeEach
    void setUp() {
        var builder = StrictApiClientFactory.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        var client = StrictApiClientFactory.create(builder, "http://localhost");
        statisticsApi = new StatistikApi(client);
        relationsApi = new JzuFiApi(client);
    }

    @ParameterizedTest
    @CsvSource({ "400, BAD_REQUEST", "404, NOT_FOUND", "500, UNKNOWN_ERROR" })
    void preservesHttpErrorStatusAndBody(int status, String code) {
        String body = "{\"code\": \"" + code + "\", \"message\": \"SDK-Testfehler\"}";
        expectError(status, body);
        var exception = assertThrows(RestClientResponseException.class, statisticsApi::getClientStatsV2);
        assertEquals(status, exception.getStatusCode().value());
        assertEquals(body, exception.getResponseBodyAsString());
        var error = StrictApiClientFactory.mapper().readValue(exception.getResponseBodyAsString(), ErrorModel.class);
        assertEquals(code, error.getCode().toString());
        assertEquals("SDK-Testfehler", error.getMessage());
        server.verify();
    }

    @Test
    void rejectsUnknownEnumInPreservedHttpErrorBody() {
        expectError(400, "{\"code\": \"UNKNOWN_SDK_CODE\"}");
        var exception = assertThrows(RestClientResponseException.class, statisticsApi::getClientStatsV2);
        assertThrows(JacksonException.class, () ->
            StrictApiClientFactory.mapper().readValue(exception.getResponseBodyAsString(), ErrorModel.class)
        );
        server.verify();
    }

    @Test
    void bindsDocumentedRelationServerErrorIncludingRequestId() {
        var document = YAMLMapper.builder()
            .build()
            .readTree(Path.of(System.getProperty("openapi.spec")).toFile());
        assertEquals(
            "#/components/schemas/ApiErrorModel",
            document
                .path("paths")
                .path("/v2/relations/jzufi-2-3")
                .at("/get/responses/500/content/application~1json/schema/$ref")
                .asString()
        );
        String body = """
        {"timestamp":"2026-10-23T10:15:30Z","code":"UNKNOWN_ERROR","codeNumber":1900,
         "message":"Es ist ein interner Fehler aufgetreten","requestId":"sdk-error-request"}
        """;
        server
            .expect(requestTo(startsWith("http://localhost/v2/relations/jzufi-2-3?")))
            .andExpect(method(HttpMethod.GET))
            .andExpect(queryParam("ars", "01"))
            .andExpect(queryParam("lbids", "lb"))
            .andRespond(withStatus(HttpStatusCode.valueOf(500)).contentType(MediaType.APPLICATION_JSON).body(body));

        var exception = assertThrows(RestClientResponseException.class, () ->
            relationsApi.getZsTrByLbIdAndArs231V2("01", List.of("lb"), false, 0, 5)
        );
        assertEquals(500, exception.getStatusCode().value());
        assertEquals(body, exception.getResponseBodyAsString());
        var error = StrictApiClientFactory.mapper().readValue(exception.getResponseBodyAsString(), ApiErrorModel.class);
        assertEquals(ApiErrorModel.CodeEnum.UNKNOWN_ERROR, error.getCode());
        assertEquals("sdk-error-request", error.getRequestId());
        assertEquals(1900, error.getCodeNumber());
        server.verify();
    }

    private void expectError(int status, String body) {
        server
            .expect(requestTo("http://localhost/v2/client-stats"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatusCode.valueOf(status)).contentType(MediaType.APPLICATION_JSON).body(body));
    }
}
