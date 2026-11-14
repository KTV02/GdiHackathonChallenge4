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
package de.dataport.suchdienst.indexer.parser.x22;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.dataport.suchdienst.indexer.model.mapping.Link;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceBlockSpecialization;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetail;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SpezialisierungContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SpezialisierungVL;
import de.dataport.suchdienst.indexer.parser.x22.locale.LanguageAwareSpParser;
import de.dataport.suchdienst.indexer.parser.x22.modul.LeistungSpezialisierungParser;
import de.dataport.suchdienst.indexer.service.languagecheck.LanguageSpCompleteCheckService;
import de.dataport.suchdienst.util.ServiceModuleCode;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Test für {@link LeistungSpezialisierungParser} (x22): Simuliert deutsche Inhalte über
 * gemockten {@link LanguageAwareSpParser} und prüft die korrekte Übernahme von Titeln/Texten.
 */
class SpecializationModuleParserTest extends AbstractX22ParserTest {

    private LeistungSpezialisierungParser parser;

    @BeforeEach
    void setup() {
        LanguageAwareSpParser languageAwareSpParser = Mockito.mock(LanguageAwareSpParser.class);
        LanguageSpCompleteCheckService completeCheckService = Mockito.mock(
            LanguageSpCompleteCheckService.class
        );

        // Deutsch simulieren: drei Modul-Typen (Ursprungsportal, Leistungsbezeichnung, Bearbeitungsdauer)
        SpezialisierungContent germanContent = new SpezialisierungContent();

        ServiceModuleDetail ursprungsportalDetail = ServiceModuleDetail.builder()
            .title("Ursprungsportal")
            .build();
        ServiceBlockSpecialization ursprungsportalBlock = new ServiceBlockSpecialization(
            false,
            ursprungsportalDetail
        );

        ServiceModuleDetail leistungsbezeichnungDetail = ServiceModuleDetail.builder()
            .text("Auto")
            .build();
        ServiceBlockSpecialization leistungsBlock = new ServiceBlockSpecialization(
            false,
            leistungsbezeichnungDetail
        );

        ServiceModuleDetail bearbeitungsdauerDetail = ServiceModuleDetail.builder()
            .text("Beschreibung Bearbeitungsdauermodul 1 spez")
            .links(List.of(Link.of("http://example", "text", null)))
            .build();
        ServiceBlockSpecialization bearbeitungsdauerBlock = new ServiceBlockSpecialization(
            false,
            bearbeitungsdauerDetail
        );

        Map<String, List<ServiceBlockSpecialization>> details = Map.of(
            ServiceModuleCode.URSPRUNGSPORTAL.getCode(),
            List.of(ursprungsportalBlock),
            ServiceModuleCode.LEISTUNGSBEZEICHNUNG.getCode(),
            List.of(leistungsBlock),
            ServiceModuleCode.BEARBEITUNGSDAUER.getCode(),
            List.of(bearbeitungsdauerBlock)
        );
        germanContent.setDetails(details);

        Mockito.when(
            languageAwareSpParser.parse(Mockito.eq(firstSpRaw()), Mockito.any())
        ).thenReturn(new HashMap<>(Map.of(Locale.GERMAN, germanContent)));
        Mockito.when(
            completeCheckService.checkLanguageComplete(Mockito.any(), Mockito.any(), Mockito.any())
        ).thenReturn(Boolean.TRUE);

        parser = new LeistungSpezialisierungParser(languageAwareSpParser, completeCheckService);
    }

    @Test
    void testSpecializationParsing() {
        SpezialisierungVL spez = parser.parse(firstSpRaw(), "lbid", "spid");

        assertEquals("lbid", spez.getIdLB());
        assertEquals("spid", spez.getId());
        assertEquals(
            "Ursprungsportal",
            spez
                .getDetails()
                .get(ServiceModuleCode.URSPRUNGSPORTAL.getCode())
                .getFirst()
                .serviceBlock()
                .getTitle()
        );
        assertEquals(
            "Auto",
            spez
                .getDetails()
                .get(ServiceModuleCode.LEISTUNGSBEZEICHNUNG.getCode())
                .getFirst()
                .serviceBlock()
                .getText()
        );
        assertEquals(
            "Beschreibung Bearbeitungsdauermodul 1 spez",
            spez
                .getDetails()
                .get(ServiceModuleCode.BEARBEITUNGSDAUER.getCode())
                .getFirst()
                .serviceBlock()
                .getText()
        );
        assertEquals(
            1,
            (
                (ServiceModuleDetail) spez
                    .getDetails()
                    .get(ServiceModuleCode.BEARBEITUNGSDAUER.getCode())
                    .getFirst()
                    .serviceBlock()
            )
                .getLinks()
                .size()
        );
    }
}
