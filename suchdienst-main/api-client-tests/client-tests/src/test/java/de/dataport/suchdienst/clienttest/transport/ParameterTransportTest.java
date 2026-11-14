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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import de.dataport.suchdienst.client.ApiClient;
import de.dataport.suchdienst.client.api.BetaOnlinediensteApi;
import de.dataport.suchdienst.client.api.OrganisationseinheitenApi;
import de.dataport.suchdienst.clienttest.support.StrictApiClientFactory;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

/** Testziel: Gemeinsame Clientmechanik, nicht fachliche Filterung oder serverseitige Cursor-Erzeugung. */
class ParameterTransportTest {

    private MockRestServiceServer server;
    private ApiClient client;

    @BeforeEach
    void setUp() {
        var builder = StrictApiClientFactory.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = StrictApiClientFactory.create(builder, "http://localhost");
    }

    @Test
    void serializesListsAsRepeatedQueryParametersAndOmitsAbsentCursor() {
        server
            .expect(
                requestTo(
                    "http://localhost/v1beta2/onlineservices?ars=01&leikaIds=99100000000001&leikaIds=99100000000002"
                )
            )
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Accept", "application/json"))
            .andRespond(
                withSuccess(
                    "{\"hits\":0,\"totalHits\":0,\"onlineservices\":[],\"nextPageToken\":\"\"}",
                    MediaType.APPLICATION_JSON
                )
            );
        var response = new BetaOnlinediensteApi(client).getOnlineServiceWithRelations(
            "01",
            List.of("99100000000001", "99100000000002"),
            null
        );
        assertEquals("", response.getNextPageToken());
        server.verify();
    }

    @Test
    void returnsOpaqueCursorUnchangedOnNextRequestAndBindsEndToken() {
        // Absichtlich kein gültiger Server-Cursor: Hier zählen unveränderte Übernahme und korrektes URL-Encoding.
        String token = "cursor+/=&?%";
        server
            .expect(requestTo("http://localhost/v1beta2/onlineservices?ars=01"))
            .andRespond(
                withSuccess(
                    "{\"hits\":1,\"totalHits\":2,\"onlineservices\":[{\"id\":\"first\"}],\"nextPageToken\":\"cursor+/=&?%\"}",
                    MediaType.APPLICATION_JSON
                )
            );
        server
            .expect(requestTo("http://localhost/v1beta2/onlineservices?ars=01&nextPageToken=cursor%2B%2F%3D%26%3F%25"))
            .andRespond(
                withSuccess(
                    "{\"hits\":1,\"totalHits\":2,\"onlineservices\":[{\"id\":\"second\"}],\"nextPageToken\":\"\"}",
                    MediaType.APPLICATION_JSON
                )
            );
        var api = new BetaOnlinediensteApi(client);
        var first = api.getOnlineServiceWithRelations("01", null, null);
        assertEquals(token, first.getNextPageToken());
        var second = api.getOnlineServiceWithRelations("01", null, first.getNextPageToken());
        assertEquals(
            List.of("first", "second"),
            List.of(first.getOnlineservices().getFirst().getId(), second.getOnlineservices().getFirst().getId())
        );
        assertEquals(first.getTotalHits(), second.getTotalHits());
        assertEquals("", second.getNextPageToken());
        server.verify();
    }

    @Test
    void encodesUnicodeAndReservedCharactersInPathVariable() {
        String id = "München /+&?=#%";
        server
            .expect(requestTo("http://localhost/v3beta1/onlineservices/M%C3%BCnchen%20%2F%2B%26%3F%3D%23%25/detail"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("{\"id\":\"München /+&?=#%\"}", MediaType.APPLICATION_JSON));
        assertEquals(id, new BetaOnlinediensteApi(client).getOnlineServiceDetail(id).getId());
        server.verify();
    }

    @Test
    void transportsLanguageHeaderEncodedQueryAndIsoDateAndBindsEnglishValues() {
        // Im ungruppierten Export ist Sprache kein Methodenparameter; Standardheader bleibt explizit.
        client.addDefaultHeader("Accept-Language", "en");
        server
            .expect(
                requestTo(
                    "http://localhost/v5/organisationunits/detail?q=M%C3%BCnchen%20%2B%26%3F%3D%23%25&validDate=2026-10-06"
                )
            )
            .andExpect(header("Accept-Language", "en"))
            .andRespond(
                withSuccess(
                    "{\"id\":\"office\",\"title\":\"English office\",\"description\":\"English description\"}",
                    MediaType.APPLICATION_JSON
                ).header("Content-Language", "en")
            );
        var response = new OrganisationseinheitenApi(client).getDetailOeOutModelV5WithHttpInfo(
            "München +&?=#%",
            null,
            null,
            LocalDate.of(2026, 10, 6)
        );
        assertEquals("en", response.getHeaders().getFirst("Content-Language"));
        assertEquals("English office", response.getBody().getTitle());
        assertEquals("English description", response.getBody().getDescription());
        server.verify();
    }
}
