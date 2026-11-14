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
package de.dataport.suchdienst.suchen.controller.beta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.config.ResourceBundleConfig;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.config.ValidationConfiguration;
import de.dataport.suchdienst.suchen.model.out.common.HyperLink;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi.OnlineServiceIteratorResponse;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi.OnlineServiceResult;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi.ServiceDescriptionInfo;
import de.dataport.suchdienst.suchen.service.OnlineDienstService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Controller-Tests für den Endpunkt {@code GET /v1beta2/onlineservices/onlineservices} in
 * {@link OnlineServiceBetaController}.
 *
 * <p>Schwerpunkte:
 * <ul>
 *   <li>ARS-Trimming vor der Übergabe an den Service</li>
 *   <li>Optionale {@code leikaIds}- und {@code nextPageToken}-Parameter</li>
 *   <li>JSON-Antwortstruktur (onlineservices, hits, totalHits, nextPageToken)</li>
 *   <li>Eingabe-Validierung des {@code ars}-Parameters (Pflichtfeld, Pattern)</li>
 * </ul>
 */
@WebMvcTest(controllers = OnlineServiceBetaController.class)
@EnableWebMvc
@AutoConfigureMockMvc(addFilters = false)
@Import({
    ResourceBundleConfig.class,
    ValidationConfiguration.class,
    PvogRequestContext.class,
    SuchdienstProperties.class,
})
class OnlineServiceBetaControllerRelationsTest {

    private static final String URL = "/v1beta2/onlineservices";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OnlineDienstService onlineDienstService;

    // -------------------------------------------------------------------------
    // Erfolgsfälle – Delegation und Struktur
    // -------------------------------------------------------------------------

    /**
     * Minimalantwort: Suchanfrage liefert keine Ergebnisse → leere onlineservices-Liste,
     * nextPageToken ist null.
     */
    @Test
    void emptyResult_returns200WithEmptyServicesList() throws Exception {
        when(
            onlineDienstService.getOnlineServicesByArsViaOdRelationIndex("09", null, null)
        ).thenReturn(new OnlineServiceIteratorResponse(null, null, null, List.of()));

        mockMvc
            .perform(get(URL).param("ars", "09").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.onlineservices", hasSize(0)))
            .andExpect(jsonPath("$.nextPageToken").doesNotExist());
    }

    /**
     * Vollständige Antwort: onlineservices, hits, totalHits und nextPageToken werden korrekt
     * in das JSON übertragen.
     */
    @Test
    void withResults_mapsAllResponseFields() throws Exception {
        var relation = new ServiceDescriptionInfo(
            "lb-1",
            Set.of("091620000000"),
            Set.of("99014001002000")
        );
        var service = new OnlineServiceResult(
            "od-1",
            "Onlinedienst A",
            "Beschreibung",
            List.of(),
            Instant.now(),
            List.of(relation)
        );
        var response = new OnlineServiceIteratorResponse(
            "next-token-abc",
            1L,
            42L,
            List.of(service)
        );

        when(
            onlineDienstService.getOnlineServicesByArsViaOdRelationIndex("09162", null, null)
        ).thenReturn(response);

        mockMvc
            .perform(get(URL).param("ars", "09162").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.onlineservices", hasSize(1)))
            .andExpect(jsonPath("$.onlineservices[0].id").value("od-1"))
            .andExpect(jsonPath("$.onlineservices[0].name").value("Onlinedienst A"))
            .andExpect(jsonPath("$.onlineservices[0].description").value("Beschreibung"))
            .andExpect(jsonPath("$.onlineservices[0].externalLastUpdate").exists())
            .andExpect(jsonPath("$.onlineservices[0].serviceDescriptions", hasSize(1)))
            .andExpect(jsonPath("$.onlineservices[0].serviceDescriptions[0].lbId").value("lb-1"))
            .andExpect(jsonPath("$.hits").value(1))
            .andExpect(jsonPath("$.totalHits").value(42))
            .andExpect(jsonPath("$.nextPageToken").value("next-token-abc"));
    }

    /**
     * Links werden aus dem Summary korrekt ins JSON übertragen.
     */
    @Test
    void withLinks_mapsLinksInSummary() throws Exception {
        var link = new HyperLink("https://example.com", "Online beantragen", "Link zum Dienst");

        var response = new OnlineServiceIteratorResponse(
            null,
            1L,
            1L,
            List.of(
                new OnlineServiceResult(
                    "od-link",
                    "Dienst mit Link",
                    null,
                    List.of(link),
                    Instant.now(),
                    List.of()
                )
            )
        );

        when(
            onlineDienstService.getOnlineServicesByArsViaOdRelationIndex("09", null, null)
        ).thenReturn(response);

        mockMvc
            .perform(get(URL).param("ars", "09").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.onlineservices[0].links", hasSize(1)))
            .andExpect(jsonPath("$.onlineservices[0].links[0].uri").value("https://example.com"))
            .andExpect(jsonPath("$.onlineservices[0].links[0].title").value("Online beantragen"));
    }

    // -------------------------------------------------------------------------
    // ARS-Trimming
    // -------------------------------------------------------------------------

    /**
     * ARS-Trimming: abschließende Nullen werden entfernt, signifikante Stellen bleiben erhalten.
     * <ul>
     *   <li>{@code "091620000000"} → {@code "09162"} (Gemeinde-ARS mit Nullen → Verbandsgemeindeebene)</li>
     *   <li>{@code "09"} → {@code "09"} (Landes-ARS ohne Nullen bleibt unverändert)</li>
     *   <li>{@code "091620152123"} → {@code "091620152123"} (12 signifikante Stellen, kein Trimming)</li>
     * </ul>
     */
    @ParameterizedTest(name = "ars=\"{0}\" → erwartet=\"{1}\"")
    @CsvSource({ "091620000000, 09162", "09, 09", "091620152123, 091620152123" })
    void ars_isTrimmedCorrectly(String inputArs, String expectedArs) throws Exception {
        when(
            onlineDienstService.getOnlineServicesByArsViaOdRelationIndex(expectedArs, null, null)
        ).thenReturn(new OnlineServiceIteratorResponse(null, null, null, List.of()));

        mockMvc
            .perform(get(URL).param("ars", inputArs).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        ArgumentCaptor<String> arsCaptor = forClass(String.class);
        verify(onlineDienstService).getOnlineServicesByArsViaOdRelationIndex(
            arsCaptor.capture(),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull()
        );
        assertThat(arsCaptor.getValue()).isEqualTo(expectedArs);
    }

    /**
     * Wenn {@code leikaIds} nicht übergeben wird, erhält der Service {@code null}.
     */
    @Test
    void withoutLeikaIds_serviceReceivesNull() throws Exception {
        when(
            onlineDienstService.getOnlineServicesByArsViaOdRelationIndex("09", null, null)
        ).thenReturn(new OnlineServiceIteratorResponse(null, null, null, List.of()));

        mockMvc
            .perform(get(URL).param("ars", "09").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        verify(onlineDienstService).getOnlineServicesByArsViaOdRelationIndex("09", null, null);
    }

    /**
     * Wenn {@code leikaIds} übergeben wird, reicht der Controller sie unverändert an den Service weiter.
     */
    @Test
    void withLeikaIds_idsArePassedToService() throws Exception {
        var leikaIds = List.of("99147006067000", "99147006067001");

        when(
            onlineDienstService.getOnlineServicesByArsViaOdRelationIndex("09", leikaIds, null)
        ).thenReturn(new OnlineServiceIteratorResponse(null, null, null, List.of()));

        mockMvc
            .perform(
                get(URL)
                    .param("ars", "09")
                    .param("leikaIds", leikaIds.toArray(String[]::new))
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk());

        verify(onlineDienstService).getOnlineServicesByArsViaOdRelationIndex("09", leikaIds, null);
    }

    /**
     * Wenn {@code nextPageToken} übergeben wird, reicht der Controller ihn unverändert
     * an den Service weiter.
     */
    @Test
    void withNextPageToken_tokenIsPassedToService() throws Exception {
        String token = "some-opaque-token";
        when(
            onlineDienstService.getOnlineServicesByArsViaOdRelationIndex("09", null, token)
        ).thenReturn(new OnlineServiceIteratorResponse(null, null, null, List.of()));

        mockMvc
            .perform(
                get(URL)
                    .param("ars", "09")
                    .param("nextPageToken", token)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk());

        verify(onlineDienstService).getOnlineServicesByArsViaOdRelationIndex("09", null, token);
    }

    // -------------------------------------------------------------------------
    // Eingabe-Validierung
    // -------------------------------------------------------------------------

    /**
     * Fehlender Pflichtparameter {@code ars} → 400 Bad Request.
     */
    @Test
    void missingArsParam_returns400() throws Exception {
        mockMvc
            .perform(get(URL).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    /**
     * {@code ars} mit einem Buchstaben (kein Ziffern-Pattern) → 400 Bad Request.
     */
    @Test
    void arsWithLetters_returns400() throws Exception {
        mockMvc
            .perform(get(URL).param("ars", "xy").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    /**
     * {@code ars} zu kurz (1 Stelle, Minimum sind 2) → 400 Bad Request.
     */
    @Test
    void arsTooShort_returns400() throws Exception {
        mockMvc
            .perform(get(URL).param("ars", "0").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    /**
     * {@code ars} zu lang (13 Stellen, Maximum sind 12) → 400 Bad Request.
     */
    @Test
    void arsTooLong_returns400() throws Exception {
        mockMvc
            .perform(get(URL).param("ars", "0916201521230").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    /**
     * {@code ars} mit exakt {@code "00"} (beginnt mit "00") → 400 Bad Request.
     */
    @Test
    void arsStartingWithDoubleZero_returns400() throws Exception {
        mockMvc
            .perform(get(URL).param("ars", "00").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    /**
     * {@code ars} mit mehr als 2 Stellen, aber Beginn mit {@code "00"} → 400 Bad Request.
     * Beispiel: {@code "001234"} soll abgelehnt werden.
     */
    @Test
    void arsStartingWithDoubleZeroAndMoreDigits_returns400() throws Exception {
        mockMvc
            .perform(get(URL).param("ars", "001234").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    /**
     * {@code leikaIds} mit ungültiger Länge → 400 Bad Request.
     */
    @Test
    void leikaIdsTooShort_returns400() throws Exception {
        mockMvc
            .perform(
                get(URL)
                    .param("ars", "09")
                    .param("leikaIds", "1234567890123")
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isBadRequest());
    }
}
