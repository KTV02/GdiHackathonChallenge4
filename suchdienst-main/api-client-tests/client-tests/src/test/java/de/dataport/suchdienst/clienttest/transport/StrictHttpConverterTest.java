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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import de.dataport.suchdienst.client.api.JzuFiApi;
import de.dataport.suchdienst.client.api.StatistikApi;
import de.dataport.suchdienst.client.model.Jzufi23Zustaendigkeit;
import de.dataport.suchdienst.client.model.Jzufi23ZustaendigkeitOrganisationseinheit;
import de.dataport.suchdienst.clienttest.support.StrictApiClientFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

/**
 * Testziel: Der tatsächliche RestClient-Konverter bindet bekannte Antworten und lehnt unbekannte Felder ab.
 * JZuFi-Untertypen ohne eigene Felder werden allein anhand von @type erkannt.
 */
class StrictHttpConverterTest {

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

    @Test
    void deserializesResponseThroughGeneratedApi() throws IOException {
        expectStatistics(new ClassPathResource("binding/statistics.json").getContentAsString(StandardCharsets.UTF_8));
        var response = statisticsApi.getClientStatsV2WithHttpInfo();
        assertEquals(200, response.getStatusCode().value());
        assertEquals("01", response.getBody().getStateStatistics().iterator().next().getArs());
        server.verify();
    }

    @Test
    void bindsRelationSubtypeWithoutSubtypeFieldsThroughGeneratedApi() {
        server
            .expect(requestTo(startsWith("http://localhost/v2/relations/jzufi-2-3?")))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Accept", "application/json"))
            .andExpect(queryParam("ars", "01"))
            .andExpect(queryParam("lbids", "lb"))
            .andRespond(
                withSuccess(
                    """
                    {
                      "content": [{
                        "zustaendigkeit": [
                          {"@type":"Zustaendigkeit","leistungID":[{"value":"lb"}],"gebietID":[{"value":"gebiet"}]},
                          {"@type":"ZustaendigkeitOrganisationseinheit","leistungID":[{"value":"lb"}],"gebietID":[{"value":"gebiet"}]}
                        ]
                      }],
                      "page":{"number":0,"size":5,"totalElements":1,"totalPages":1}
                    }
                    """,
                    MediaType.APPLICATION_JSON
                )
            );
        var response = relationsApi.getZsTrByLbIdAndArs231V2WithHttpInfo("01", List.of("lb"), false, 0, 5);
        assertEquals(200, response.getStatusCode().value());
        var responsibilities = response.getBody().getContent().getFirst().getZustaendigkeit();
        assertEquals(2, responsibilities.size());
        assertEquals(Jzufi23Zustaendigkeit.class, responsibilities.getFirst().getClass());
        assertEquals(Jzufi23ZustaendigkeitOrganisationseinheit.class, responsibilities.get(1).getClass());
        for (var responsibility : responsibilities) {
            assertEquals("lb", responsibility.getLeistungID().getFirst().getValue());
            assertEquals("gebiet", responsibility.getGebietID().getFirst().getValue());
        }
        server.verify();
    }

    @Test
    void rejectsUnknownResponsePropertyThroughHttpConverter() {
        expectStatistics("{\"unexpected\": true}");
        RestClientException exception = assertThrows(RestClientException.class, statisticsApi::getClientStatsV2);
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        assertInstanceOf(UnrecognizedPropertyException.class, cause, () -> "Unerwartete Fehlerursache: " + exception);
        server.verify();
    }

    private void expectStatistics(String response) {
        server
            .expect(requestTo("http://localhost/v2/client-stats"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Accept", "application/json"))
            .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }
}
