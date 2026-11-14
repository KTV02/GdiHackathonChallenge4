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
package de.dataport.suchdienst.suchen.controller;

import static org.hamcrest.Matchers.hasEntry;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.config.ResourceBundleConfig;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.config.ValidationConfiguration;
import de.dataport.suchdienst.suchen.service.ServiceDescriptionCsvService;
import de.dataport.suchdienst.suchen.service.ServiceDescriptionService;
import de.dataport.suchdienst.suchen.service.ServiceDescriptionSuggestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@WebMvcTest(controllers = ServiceDescriptionController.class)
@EnableWebMvc
@AutoConfigureMockMvc(addFilters = false)
@Import({
    ResourceBundleConfig.class,
    ValidationConfiguration.class,
    PvogRequestContext.class,
    SuchdienstProperties.class,
})
@MockitoBean(
    types = {
        ServiceDescriptionService.class,
        ServiceDescriptionSuggestionService.class,
        ServiceDescriptionCsvService.class,
    }
)
class ConstraintViolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testMultipleErrors() throws Exception {
        mockMvc
            .perform(
                get("/v7/servicedescriptions/091610000000")
                    .param("q", "x")
                    .param("page", "900")
                    .header("Accept-Language", "de")
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.fields",
                    hasEntry("page", "muss kleiner-gleich 100 sein")
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.fields",
                    hasEntry("q[0]", "Größe muss zwischen 2 und 2147483647 sein")
                )
            );
    }

    @Test
    void testList() throws Exception {
        mockMvc
            .perform(
                get("/v7/servicedescriptions/091610000000")
                    .param("q", "x")
                    .param("q", "y")
                    .header("Accept-Language", "de")
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.fields",
                    hasEntry("q[0]", "Größe muss zwischen 2 und 2147483647 sein")
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.fields",
                    hasEntry("q[1]", "Größe muss zwischen 2 und 2147483647 sein")
                )
            );
    }

    @Test
    void testMissingTooLongArs() throws Exception {
        mockMvc
            .perform(
                get("/v7/servicedescriptions/0916100000001")
                    .param("q", "suchwert")
                    .param("page", "1")
                    .header("Accept-Language", "de")
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.fields",
                    hasEntry("ars", "Größe muss zwischen 0 und 12 sein")
                )
            );
    }

    @Test
    void testMinSize() throws Exception {
        mockMvc
            .perform(
                get("/v7/servicedescriptions/091610000000")
                    .param("q", "suchwert")
                    .param("page", "-1")
                    .header("Accept-Language", "de")
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.fields",
                    hasEntry("page", "muss größer-gleich 0 sein")
                )
            );
    }
}
