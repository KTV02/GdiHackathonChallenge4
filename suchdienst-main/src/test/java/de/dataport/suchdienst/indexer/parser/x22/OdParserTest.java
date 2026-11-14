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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNull;

import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstParameter;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstVL;
import de.dataport.suchdienst.indexer.parser.x22.locale.LanguageAwareOdParser;
import de.dataport.suchdienst.indexer.service.languagecheck.LanguageOdCompleteCheckService;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OdParserTest extends AbstractX22ParserTest {

    public OdParserTest() {
        super("x22/onlinedienst.xzufi");
    }

    private XZuFiOdParserService parser;

    @BeforeEach
    void setup() {
        // Arrange mocks
        LanguageAwareOdParser languageAwareOdParser = Mockito.mock(LanguageAwareOdParser.class);
        LanguageOdCompleteCheckService completeCheck = Mockito.mock(
            LanguageOdCompleteCheckService.class
        );
        Mockito.when(
            completeCheck.checkLanguageComplete(Mockito.any(), Mockito.any(), Mockito.any())
        ).thenReturn(Boolean.TRUE);

        // Synthese einfacher Sprachinhalte (nur Deutsch)
        OnlinedienstContent germanContent = new OnlinedienstContent();
        OnlinedienstContent englishContent = new OnlinedienstContent();
        Mockito.when(
            languageAwareOdParser.parseOnlineService(Mockito.any(), Mockito.any())
        ).thenAnswer(inv ->
            new HashMap<>(Map.of(Locale.GERMAN, germanContent, Locale.ENGLISH, englishContent))
        );

        parser = new XZuFiOdParserService(codelistService, languageAwareOdParser, completeCheck);
    }

    @Test
    void testLogoOfOdParser() {
        // Act
        OnlinedienstVL parsed = parser.parseOnlinedienst(firstOdRaw());

        // Assert
        assertAll(
            () -> assertThat(parsed, notNullValue()),
            () -> assertThat(parsed.getLogos(), hasSize(2)),
            () -> assertThat(parsed.getLogos().iterator().next(), notNullValue()),
            () -> assertThat(parsed.getLogos().iterator().next(), is("http://link.zum/logo1"))
        );
    }

    @Test
    void leavesOnlinedienstStatusEmpty() {
        OnlinedienstVL parsed = parser.parseOnlinedienst(firstOdRaw());

        assertNull(parsed.getOnlinedienstStatus());
    }

    @Test
    void usesConverterToSeparateAndLocalizeParameters() {
        OnlinedienstVL parsed = parser.parseOnlinedienst(firstOdRaw());
        OnlinedienstContent englishContent = parsed.getOtherLanguages().get(Locale.ENGLISH);

        assertThat(parsed.getLanguages(), containsInAnyOrder(Locale.GERMAN, Locale.ENGLISH));
        assertThat(
            parsed.getParametersCustom().stream().map(OnlinedienstParameter::getValue).toList(),
            containsInAnyOrder("Custom value", "Custom DE")
        );
        assertThat(
            parsed.getParametersEfa().stream().map(OnlinedienstParameter::getValue).toList(),
            containsInAnyOrder("Datenschutz DE")
        );
        assertThat(
            englishContent
                .getParametersCustom()
                .stream()
                .map(OnlinedienstParameter::getValue)
                .toList(),
            containsInAnyOrder("Custom value", "Custom EN")
        );
        assertThat(
            englishContent
                .getParametersEfa()
                .stream()
                .map(OnlinedienstParameter::getValue)
                .toList(),
            containsInAnyOrder("Privacy EN")
        );
    }
}
