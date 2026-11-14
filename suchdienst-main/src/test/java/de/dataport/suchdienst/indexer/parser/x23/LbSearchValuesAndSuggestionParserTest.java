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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertAll;

import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungVL;
import de.dataport.suchdienst.indexer.parser.ParserSelector;
import de.dataport.suchdienst.indexer.parser.x23.locale.LanguageAwareLbParser;
import de.dataport.suchdienst.indexer.parser.x23.locale.LanguageAwareSearchValuesAndSuggestionParser;
import de.dataport.suchdienst.indexer.parser.x23.modul.BearbeitungsdauerModulParser;
import de.dataport.suchdienst.indexer.parser.x23.modul.DokumentModulParser;
import de.dataport.suchdienst.indexer.parser.x23.modul.FachlicheFreigabeModulParser;
import de.dataport.suchdienst.indexer.parser.x23.modul.FristHelper;
import de.dataport.suchdienst.indexer.parser.x23.modul.FristModulParser;
import de.dataport.suchdienst.indexer.parser.x23.modul.KostenModulParser;
import de.dataport.suchdienst.indexer.parser.x23.modul.TextModulIndividuellParser;
import de.dataport.suchdienst.indexer.parser.x23.modul.TextModulParser;
import de.dataport.suchdienst.indexer.parser.x23.modul.UrsprungsportalModulParser;
import de.dataport.suchdienst.indexer.service.languagecheck.LanguageLbCompleteCheckService;
import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test für die Generierung von Suchvorschlägen und Suchwerten in x23.
 * Prüft ob typische Token / Normalisierungen (Stemming, Lowercase) korrekt erfasst werden.
 */
class LbSearchValuesAndSuggestionParserTest extends AbstractX23ParserTest {

    private XZuFiLbParserService parserService;

    @BeforeEach
    void setUpService() throws IOException {
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
        var languageAwareLbParser = new LanguageAwareLbParser(
            searchValuesAndSuggestion,
            ParserSelector.of(parserConfig)
        );
        parserService = new XZuFiLbParserService(
            codelistService,
            languageAwareLbParser,
            new LanguageLbCompleteCheckService()
        );
    }

    @Test
    void testParsing() {
        LeistungsbeschreibungVL sd = parserService.parseLeistungen(firstLbRaw());
        @SuppressWarnings("deprecation")
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
