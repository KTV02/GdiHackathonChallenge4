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
package de.dataport.suchdienst.indexer.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.config.ResourceBundleConfig;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.elasticsearch.api.ElasticBulkWriteConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticWriteConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexConfiguration;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.indexer.service.ArsService;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ArsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ ResourceBundleConfig.class, PvogRequestContext.class, SuchdienstProperties.class })
@ActiveProfiles({ "default", "dev", "openapi_show_internal" })
class ArsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ElasticBulkWriteConnector connectorMigration;

    @MockitoBean
    private ArsService arsService;

    @MockitoBean
    private ElasticWriteConnector elasticWriteConnector;

    @MockitoBean
    private IndexConfiguration indexConfiguration;

    @Test
    void testUploadArsFile() throws Exception {
        when(connectorMigration.bulkIndex(anyList(), any(IndexNames.class))).then(t -> {
            assertEquals(11043, ((List<?>) t.getArgument(0)).size());
            assertEquals("ars", t.getArgument(1));
            return null;
        });

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("ARS_Liste.ASC").getFile());

        MockMultipartFile multiPartFile = new MockMultipartFile(
            "file",
            Files.readAllBytes(file.toPath())
        );

        mockMvc
            .perform(
                multipart("/admin/ars")
                    .file(multiPartFile)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
            )
            .andExpect(status().isNoContent());

        mockMvc
            .perform(multipart("/admin/ars").contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isBadRequest());
    }
}
