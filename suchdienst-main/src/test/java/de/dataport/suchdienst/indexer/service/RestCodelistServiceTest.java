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
package de.dataport.suchdienst.indexer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.dataport.suchdienst.config.SuchdienstProperties;
import de.xrepository.genericodeservices.CodeListDocument;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class RestCodelistServiceTest {

    @Mock
    private AntivirusScannerService antivirusScannerService;

    @Mock
    private RestTemplate restTemplate;

    private RestCodelistService restCodelistService;

    @BeforeEach
    void setUp() throws JAXBException {
        // Erstelle einen echten JsonMapper und instanziiere das Service-Objekt explizit.
        JsonMapper jsonMapper = new JsonMapper();
        restCodelistService = new RestCodelistService(
            new SuchdienstProperties(),
            antivirusScannerService,
            Mockito.mock(MessageSource.class),
            restTemplate,
            jsonMapper
        );
    }

    @Test
    void testDownloadCodeMap() throws IOException {
        String codelist = Files.readString(
            Path.of("src/test/resources/codelistResponseCorrect.xml"),
            StandardCharsets.UTF_8
        );

        Mockito.when(restTemplate.getForEntity(Mockito.any(), Mockito.eq(String.class))).thenReturn(
            ResponseEntity.ok(codelist)
        );
        Mockito.when(antivirusScannerService.scanFileContent(Mockito.anyString())).thenReturn(true);

        CodeListDocument response = restCodelistService.downloadCodeMap(
            "urn:de:xzufi:codeliste:leistungstextmodulleika",
            "20150601"
        );
        assertEquals(
            "02",
            response.getSimpleCodeList().getRow().getFirst().getValue().get(0).getSimpleValue()
        );
        assertEquals(
            "Leistungsbezeichnung",
            response.getSimpleCodeList().getRow().getFirst().getValue().get(1).getSimpleValue()
        );
    }

    @Test
    void testGetCurrentVersionNameFromCodelist() throws IOException {
        String codelist = Files.readString(
            Path.of("src/test/resources/codelistResponseCurrentVersion.json"),
            StandardCharsets.UTF_8
        );

        Mockito.when(
            restTemplate.exchange(
                Mockito.any(),
                Mockito.eq(HttpMethod.GET),
                Mockito.any(),
                Mockito.eq(String.class)
            )
        ).thenReturn(ResponseEntity.ok(codelist));
        Mockito.when(antivirusScannerService.scanFileContent(Mockito.anyString())).thenReturn(true);

        String version = restCodelistService.getCurrentVersionNameFromCodelist(
            "urn:de:xzufi:codeliste:leistungstextmodulleika"
        );
        assertEquals("20240216", version);
    }
}
