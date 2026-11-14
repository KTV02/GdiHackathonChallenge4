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
package de.dataport.suchdienst.indexer.parser.x23;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetail;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SpezialisierungVL;
import de.dataport.suchdienst.indexer.parser.ParserSelector;
import de.dataport.suchdienst.indexer.parser.x23.locale.LanguageAwareSearchValuesAndSuggestionParser;
import de.dataport.suchdienst.indexer.parser.x23.locale.LanguageAwareSpParser;
import de.dataport.suchdienst.indexer.parser.x23.modul.BearbeitungsdauerModulParser;
import de.dataport.suchdienst.indexer.parser.x23.modul.DokumentModulParser;
import de.dataport.suchdienst.indexer.parser.x23.modul.FachlicheFreigabeModulParser;
import de.dataport.suchdienst.indexer.parser.x23.modul.FristHelper;
import de.dataport.suchdienst.indexer.parser.x23.modul.FristModulParser;
import de.dataport.suchdienst.indexer.parser.x23.modul.KostenModulParser;
import de.dataport.suchdienst.indexer.parser.x23.modul.LeistungSpezialisierungParser;
import de.dataport.suchdienst.indexer.parser.x23.modul.TextModulIndividuellParser;
import de.dataport.suchdienst.indexer.parser.x23.modul.TextModulParser;
import de.dataport.suchdienst.indexer.parser.x23.modul.UrsprungsportalModulParser;
import de.dataport.suchdienst.indexer.service.languagecheck.LanguageSpCompleteCheckService;
import de.dataport.suchdienst.util.ServiceModuleCode;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpecializationModuleParserTest extends AbstractX23ParserTest {

    private LeistungSpezialisierungParser parser;

    @BeforeEach
    void setUpParser() throws IOException {
        var fristHelper = new FristHelper(codelistService);
        var kostenParser = new KostenModulParser(codelistService, messageSource);
        var dokumentParser = new DokumentModulParser(codelistService, messageSource);
        var fristParser = new FristModulParser(codelistService, fristHelper, messageSource);
        var bearbParser = new BearbeitungsdauerModulParser(
            fristHelper,
            messageSource,
            codelistService
        );
        var textParser = new TextModulParser(codelistService);
        var textIndParser = new TextModulIndividuellParser(
            codelistService,
            messageSource,
            new de.dataport.suchdienst.config.SuchdienstProperties()
        );
        var ursprungParser = new UrsprungsportalModulParser(messageSource);
        var fachlParser = new FachlicheFreigabeModulParser(messageSource);

        var parserConfig = new ParserConfig(
            kostenParser,
            textIndParser,
            dokumentParser,
            fristParser,
            bearbParser,
            textParser,
            ursprungParser,
            fachlParser
        );
        var searchValuesAndSuggestion = new LanguageAwareSearchValuesAndSuggestionParser(
            new de.dataport.suchdienst.indexer.service.suggestion.LanguageDeterminationComponent(
                new de.dataport.suchdienst.indexer.service.suggestion.DeSuggestionAnalyzer(),
                new de.dataport.suchdienst.indexer.service.suggestion.EnSuggestionAnalyzer()
            )
        );
        var spParser = new LanguageAwareSpParser(
            searchValuesAndSuggestion,
            ParserSelector.of(parserConfig)
        );
        parser = new LeistungSpezialisierungParser(spParser, new LanguageSpCompleteCheckService());
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
