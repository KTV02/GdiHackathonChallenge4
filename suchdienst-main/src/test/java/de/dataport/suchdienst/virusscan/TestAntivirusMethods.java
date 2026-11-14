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
package de.dataport.suchdienst.virusscan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.dataport.suchdienst.aop.ElasticAspect;
import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.config.ResourceBundleConfig;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.elasticsearch.api.ElasticWriteConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexConfiguration;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.indexer.controller.ArsController;
import de.dataport.suchdienst.indexer.controller.CodelistController;
import de.dataport.suchdienst.indexer.controller.XzufiImportController;
import de.dataport.suchdienst.indexer.model.RegionalKeyKind;
import de.dataport.suchdienst.indexer.service.AntivirusScannerService;
import de.dataport.suchdienst.indexer.service.ArsService;
import de.dataport.suchdienst.indexer.service.CodelistService;
import de.dataport.suchdienst.indexer.service.XZufiImportService;
import de.dataport.suchdienst.suchen.exception.SuchenException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;

/**
 * Test zur Überprüfung der Antivirus-Scanning-Funktionalität in verschiedenen Upload-Endpunkten.
 * Verwendet @WebMvcTest für schnellere Tests durch nur teilweises Laden des Spring-Contextes.
 */
@WebMvcTest(
    controllers = { ArsController.class, CodelistController.class, XzufiImportController.class }
)
@AutoConfigureMockMvc(addFilters = false)
@Import({ ResourceBundleConfig.class, PvogRequestContext.class, ElasticAspect.class })
@MockitoBean(
    types = { SuchdienstProperties.class, IndexConfiguration.class, ElasticWriteConnector.class }
)
@MockitoBean(name = "singleTaskExecutor", types = TaskScheduler.class)
@EnableAspectJAutoProxy
@ActiveProfiles("development")
class TestAntivirusMethods {

    @Autowired
    private ArsController arsController;

    @Autowired
    private CodelistController codelistController;

    @Autowired
    private XzufiImportController xzufiImportController;

    @MockitoBean
    private AntivirusScannerService scannerService;

    @MockitoBean
    private ArsService arsService;

    @MockitoBean
    private CodelistService codelistService;

    @MockitoBean
    private XZufiImportService xZufiImportService;

    @BeforeEach
    void setUpMocks() {
        // Services werden gemockt - keine echten Operationen
        Mockito.doNothing()
            .when(arsService)
            .importArsList(Mockito.any(InputStream.class), Mockito.any());
        Mockito.doNothing().when(arsService).importPostcodes(Mockito.any(), Mockito.any());
        Mockito.when(
            codelistService.saveCodelistInElasticSearch(Mockito.any(InputStream.class))
        ).thenReturn(java.util.Collections.emptyList());
    }

    @Test
    void testArsResourceUploadArsFile() throws IOException {
        MockMultipartFile file = new MockMultipartFile("ArsFile", loadFile("ARS_Liste.ASC"));
        testUploadFunction(() -> arsController.uploadArsFile(file));
    }

    @Test
    void testArsResourceUploadPostcodesFile() throws IOException {
        // Verwende echte GZIP-Daten statt ungepackter CSV, um ZipException zu vermeiden
        byte[] gzipped = gzip("zip;rs\n01067;01001");
        MockMultipartFile file = new MockMultipartFile("PostcodesFile", gzipped);
        testUploadFunction(() ->
            arsController.uploadPostcodesFile(file, ";", 0, 0, RegionalKeyKind.ARS, true)
        );
    }

    @Test
    void testCodelistResourceUploadCodelist() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
            "ExcelFile",
            loadFile("antivirus/Kommunikationskanal-0.1.xml")
        );
        testUploadFunction(() -> codelistController.uploadCodelist(file));
    }

    @Test
    void testXzufiImportResource() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
            "ExcelFile",
            loadFile("antivirus/testAntivirusXzufi.zip")
        );
        MediaType contentType = MediaType.parseMediaType(
            "%s; %s".formatted(MediaType.MULTIPART_FORM_DATA_VALUE, "xzufi-version=2.2.0")
        );
        testUploadFunction(() -> xzufiImportController.uploadFileZip(contentType, file));
    }

    private void testUploadFunction(Runnable function) throws IOException {
        Mockito.when(scannerService.scanFile(Mockito.any())).thenReturn(false);
        SuchenException ex = assertThrows(SuchenException.class, function::run);
        assertEquals(ErrorCodeEnum.VIRUS_DETECTED, ex.getCode());

        Mockito.when(scannerService.scanFile(Mockito.any())).thenReturn(true);
        function.run();
        Mockito.verify(scannerService, Mockito.times(2)).scanFile(Mockito.any(MultipartFile.class));
    }

    private byte[] loadFile(String path) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("Resource not found: " + path);
            }
            return in.readAllBytes();
        }
    }

    private byte[] gzip(String data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(baos)) {
            gos.write(data.getBytes(StandardCharsets.UTF_8));
        }
        return baos.toByteArray();
    }
}
