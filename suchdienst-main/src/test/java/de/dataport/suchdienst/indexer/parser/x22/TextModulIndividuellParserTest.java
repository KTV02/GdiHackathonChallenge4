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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;

import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.indexer.model.mapping.ValidityPeriod;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModule;
import de.dataport.suchdienst.indexer.parser.x22.modul.TextModulIndividuellParser;
import de.xoev.xzufi.v2_2_0.TextmodulIndividuell;
import java.time.LocalDate;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit-Test (x22) für {@link TextModulIndividuellParser} ohne Spring.
 * Validiert zwei Fälle:
 *  - Modul mit Text + Zeitraum + Link
 *  - Modul ohne Text + keine Zusatzinfos/Links.
 */
class TextModulIndividuellParserTest extends AbstractX22ParserTest {

    private TextModulIndividuellParser parser;

    @BeforeEach
    void setup() {
        SuchdienstProperties props = new SuchdienstProperties();
        props.setDummyCodeLists(java.util.Set.of()); // Keine Dummy-Liste notwendig
        parser = new TextModulIndividuellParser(codelistService, messageSource, props);
    }

    @Test
    void testParsing() {
        var textIndividuellList = firstLbRaw().getModulTextIndividuell();
        TextmodulIndividuell modul1 = textIndividuellList.getFirst();
        TextmodulIndividuell modul2 = textIndividuellList.get(1);

        // Modul 1: Vollständige Daten
        ServiceModule block1 = parser.parse(modul1, Locale.GERMAN);
        assertAll(
            () -> assertThat(block1.getText(), equalTo("Text in einem individuellen Textmodul")),
            () -> assertThat(block1.getTitle(), equalTo("001")),
            () ->
                assertThat(
                    block1.getValidityPeriods().getFirst(),
                    equalTo(
                        new ValidityPeriod(LocalDate.of(2018, 5, 11), LocalDate.of(2019, 5, 11))
                    )
                ),
            () -> assertThat(block1.getLinks(), hasSize(1))
        );

        // Modul 2: Minimale Daten (kein Text, keine Links)
        ServiceModule block2 = parser.parse(modul2, Locale.GERMAN);
        assertAll(
            () -> assertThat(block2.getText(), equalTo(null)),
            () -> assertThat(block2.getTitle(), equalTo("00123")),
            () -> assertThat(block2.getLinks(), hasSize(0)),
            () -> assertThat(block2.getAdditionalInformation(), hasSize(0))
        );
    }
}
