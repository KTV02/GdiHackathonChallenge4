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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertAll;

import de.dataport.suchdienst.indexer.model.CodelistKey;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstParameter;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstVL;
import de.dataport.suchdienst.indexer.parser.x23.locale.LanguageAwareOdParser;
import de.dataport.suchdienst.indexer.service.languagecheck.LanguageOdCompleteCheckService;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OdParserTest extends AbstractX23ParserTest {

    private XZuFiOdParserService xZufiOdParserService;

    public OdParserTest() {
        super("x23/onlinedienst.xzufi");
    }

    @BeforeEach
    void setUpService() {
        var languageAwareOdParser = new LanguageAwareOdParser(codelistService);
        var languageCheck = new LanguageOdCompleteCheckService();
        xZufiOdParserService = new XZuFiOdParserService(
            codelistService,
            languageAwareOdParser,
            languageCheck
        );
    }

    @Test
    void testLogoOfOdParser() {
        OnlinedienstVL parsedOd = xZufiOdParserService.parseOnlinedienst(firstOdRaw());
        assertAll(
            () -> assertThat(parsedOd, notNullValue()),
            () -> assertThat(parsedOd.getLogos(), hasSize(1)), // In XZuFi2.3 gibt es immer nur ein Logo
            () -> assertThat(parsedOd.getLogos().iterator().next(), notNullValue()),
            () -> assertThat(parsedOd.getLogos().iterator().next(), is("http://link.zum/logo1"))
        );
    }

    @Test
    void parsesOnlinedienstStatus() {
        OnlinedienstVL parsedOd = xZufiOdParserService.parseOnlinedienst(firstOdRaw());

        assertThat(
            parsedOd.getOnlinedienstStatus(),
            is(new CodelistKey("urn:xoev-de:fim:codeliste:xzufi.onlinedienststatus", "1", "00"))
        );
    }

    @Test
    void separatesEfaAndCustomParameters() {
        OnlinedienstVL parsedOd = xZufiOdParserService.parseOnlinedienst(firstOdRaw());
        assertThat(
            parsedOd.getParametersCustom().stream().map(OnlinedienstParameter::getName).toList(),
            containsInAnyOrder(
                "Test_individueller_ParameterName",
                "Test_individueller_ParameterName_DE",
                "Test_individueller_ParameterName_SHARED"
            )
        );
        assertThat(
            parsedOd.getParametersEfa().stream().map(OnlinedienstParameter::getName).toList(),
            containsInAnyOrder(
                "efa.contact.url",
                "efa.datenschutzerklaerung.url",
                "efa.accessibility.url"
            )
        );
    }

    @Test
    void assignsEfaParametersToTheirDeclaredLanguages() {
        OnlinedienstVL parsedOd = xZufiOdParserService.parseOnlinedienst(firstOdRaw());
        OnlinedienstContent englishContent = parsedOd.getOtherLanguages().get(Locale.ENGLISH);

        assertThat(parsedOd.getLanguages(), containsInAnyOrder(Locale.GERMAN, Locale.ENGLISH));
        assertThat(
            parsedOd.getParametersEfa().stream().map(OnlinedienstParameter::getName).toList(),
            containsInAnyOrder(
                "efa.contact.url",
                "efa.datenschutzerklaerung.url",
                "efa.accessibility.url"
            )
        );
        assertThat(
            englishContent.getParametersEfa().stream().map(OnlinedienstParameter::getName).toList(),
            containsInAnyOrder("efa.contact.url", "efa.impressum.url", "efa.accessibility.url")
        );
        assertThat(
            englishContent
                .getParametersCustom()
                .stream()
                .map(OnlinedienstParameter::getName)
                .toList(),
            containsInAnyOrder(
                "Test_individueller_ParameterName",
                "Test_individueller_ParameterName_EN",
                "Test_individueller_ParameterName_SHARED"
            )
        );
    }
}
