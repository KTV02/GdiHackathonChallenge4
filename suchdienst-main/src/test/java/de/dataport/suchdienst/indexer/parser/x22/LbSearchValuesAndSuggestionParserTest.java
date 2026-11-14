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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertAll;

import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungVL;
import de.dataport.suchdienst.indexer.parser.ParserSelector;
import de.dataport.suchdienst.indexer.parser.x22.locale.LanguageAwareLbParser;
import de.dataport.suchdienst.indexer.parser.x22.locale.LanguageAwareSearchValuesAndSuggestionParser;
import de.dataport.suchdienst.indexer.parser.x22.modul.BearbeitungsdauerModulParser;
import de.dataport.suchdienst.indexer.parser.x22.modul.DokumentModulParser;
import de.dataport.suchdienst.indexer.parser.x22.modul.FachlicheFreigabeModulParser;
import de.dataport.suchdienst.indexer.parser.x22.modul.FristHelper;
import de.dataport.suchdienst.indexer.parser.x22.modul.FristModulParser;
import de.dataport.suchdienst.indexer.parser.x22.modul.KostenModulParser;
import de.dataport.suchdienst.indexer.parser.x22.modul.TextModulIndividuellParser;
import de.dataport.suchdienst.indexer.parser.x22.modul.TextModulParser;
import de.dataport.suchdienst.indexer.parser.x22.modul.UrsprungsportalModulParser;
import de.dataport.suchdienst.indexer.service.languagecheck.LanguageLbCompleteCheckService;
import de.dataport.suchdienst.indexer.service.suggestion.DeSuggestionAnalyzer;
import de.dataport.suchdienst.indexer.service.suggestion.EnSuggestionAnalyzer;
import de.dataport.suchdienst.indexer.service.suggestion.LanguageDeterminationComponent;
import de.xoev.xzufi.v2_2_0.Leistung;
import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LbSearchValuesAndSuggestionParserTest extends AbstractX22ParserTest {

    private XZuFiLbParserService parser;

    @BeforeEach
    void setup() throws IOException {
        // Arrange: echte Parser-Kette minimal instanziert (ohne Spring Kontext)
        var deAnalyzer = new DeSuggestionAnalyzer();
        var enAnalyzer = new EnSuggestionAnalyzer();
        var languageDetermination = new LanguageDeterminationComponent(deAnalyzer, enAnalyzer);
        var synonymSuggestionParser = new LanguageAwareSearchValuesAndSuggestionParser(
            languageDetermination
        );

        // ParserSelector benötigt Registry -> nutzen echte Parser-Implementierungen über ParserConfig Konstruktion
        // Für die ParserConfig brauchen wir die einzelnen Modulparser; bestehende Unit-Tests instanziieren sie nicht zentral.
        // Wir mocken nur Codelist-Zugriffe; Modulparser verarbeiten Strukturen direkt.
        var kostenParser = new KostenModulParser(codelistService, messageSource);
        var props = new SuchdienstProperties();
        var textIndividuellParser = new TextModulIndividuellParser(
            codelistService,
            messageSource,
            props
        );
        var dokumentParser = new DokumentModulParser(codelistService, messageSource);
        var fristHelper = new FristHelper(codelistService);
        var fristParser = new FristModulParser(codelistService, fristHelper, messageSource);
        var bearbeitungsdauerParser = new BearbeitungsdauerModulParser(fristHelper, messageSource);
        var textParser = new TextModulParser(codelistService);
        var ursprungParser = new UrsprungsportalModulParser(messageSource);
        var fachlicheFreigabeParser = new FachlicheFreigabeModulParser(messageSource);
        var parserConfig = new ParserConfig(
            kostenParser,
            textIndividuellParser,
            dokumentParser,
            fristParser,
            bearbeitungsdauerParser,
            textParser,
            ursprungParser,
            fachlicheFreigabeParser
        );
        var parserSelector = ParserSelector.of(parserConfig);
        var languageAwareLbParser = new LanguageAwareLbParser(
            synonymSuggestionParser,
            parserSelector
        );
        var completeCheckService = new LanguageLbCompleteCheckService();

        parser = new XZuFiLbParserService(
            codelistService,
            languageAwareLbParser,
            completeCheckService
        );
    }

    @Test
    void testParsingRealExtraction() throws Exception {
        Leistung raw = firstLbRaw();
        LeistungsbeschreibungVL sd = parser.parseLeistung(raw);
        Set<String> suggestions = sd.getSuggestions();

        assertAll(
            () -> assertThat(sd, notNullValue()),
            () ->
                assertThat(
                    suggestions,
                    containsInAnyOrder(
                        "Meine Leistung",
                        "normal",
                        "leistung",
                        "bezeichnung",
                        "gutachter",
                        "Die normale Bezeichnung"
                    )
                )
        );
    }
}
