/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2025 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.importer.controller;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.config.ResourceBundleConfig;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.importer.service.DownloadImportRequestTokenService;
import de.dataport.suchdienst.importer.service.QueueImportService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(PushImportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ ResourceBundleConfig.class, PvogRequestContext.class, SuchdienstProperties.class })
@MockitoBean(types = { QueueImportService.class, DownloadImportRequestTokenService.class })
@ActiveProfiles({ "default", "dev", "openapi_show_internal" })
class PushImportControllerTest {

    @Autowired
    public MockMvc mockMvc;

    @Autowired
    public ObjectMapper mapper;

    @Test
    void pushXzufiToQueueMissingProducerTest() throws Exception {
        // Testet das Fehlen eines Headers
        mockMvc
            .perform(
                post("/push/xzufi-22/enqueue")
                    .contentType(MediaType.APPLICATION_XML)
                    .header("X-PVOG-NACHRICHTUUID", "nachrichtenId")
                    .header("X-PVOG-TRANSAKTIONID", "transaktionsId")
                    .header("X-PVOG-TRANSAKTIONPOSITION", "3")
                    .header("X-PVOG-AGENCYID", "schemeAgencyId")
                    .accept(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8)
                    .content("<xml version=\"1.0\" encoding=\"utf8\"/>")
            )
            .andExpect(status().isBadRequest())
            .andExpect(header().string("Content-Type", "application/json"))
            .andExpect(jsonPath("$.code", is("MISSING_HEADER")))
            .andExpect(jsonPath("$.codeNumber", is(1908)))
            .andExpect(
                jsonPath(
                    "$.message",
                    is("Erforderlicher Header Parameter 'X-PVOG-PRODUCER' wurde nicht übermittelt.")
                )
            )
            .andExpect(jsonPath("$.timestamp", not(emptyOrNullString())));
    }

    @Test
    void pushXzufiToQueueNoBodyTest() throws Exception {
        // Testet das Fehlen eines Bodys
        mockMvc
            .perform(
                post("/push/xzufi-22/enqueue")
                    .contentType(MediaType.APPLICATION_XML)
                    .header("X-PVOG-NACHRICHTUUID", "nachrichtenId")
                    .header("X-PVOG-TRANSAKTIONID", "transaktionsId")
                    .header("X-PVOG-TRANSAKTIONPOSITION", "3")
                    .header("X-PVOG-PRODUCER", "producer")
                    .header("X-PVOG-AGENCYID", "schemeAgencyId")
                    .accept(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8)
            )
            .andExpect(status().isBadRequest())
            .andExpect(header().string("Content-Type", "application/json"))
            .andExpect(jsonPath("$.code", is("MESSAGE_NOT_READABLE")))
            .andExpect(jsonPath("$.codeNumber", is(1909)))
            .andExpect(jsonPath("$.message", is("Fehler beim Lesen der Anfrage.")))
            .andExpect(jsonPath("$.timestamp", not(emptyOrNullString())));
    }

    @Test
    void pushXzufiToQueueOkTest() throws Exception {
        // Positiv Testfall
        mockMvc
            .perform(
                post("/push/xzufi-22/enqueue")
                    .contentType(MediaType.APPLICATION_XML)
                    .header("X-PVOG-NACHRICHTUUID", "nachrichtenId")
                    .header("X-PVOG-TRANSAKTIONID", "transaktionsId")
                    .header("X-PVOG-TRANSAKTIONPOSITION", "3")
                    .header("X-PVOG-PRODUCER", "producer")
                    .header("X-PVOG-AGENCYID", "schemeAgencyId")
                    .accept(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8)
                    .content("<xml version=\"1.0\" encoding=\"utf8\"/>")
            )
            .andExpect(status().isOk());
    }
}
