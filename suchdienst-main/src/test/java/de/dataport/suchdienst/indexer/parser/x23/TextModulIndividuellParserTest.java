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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;

import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.indexer.model.mapping.ValidityPeriod;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModule;
import de.dataport.suchdienst.indexer.parser.x23.modul.TextModulIndividuellParser;
import de.xoev.xzufi.v2_3_1.TextmodulIndividuell;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TextModulIndividuellParserTest extends AbstractX23ParserTest {

    private TextModulIndividuellParser parser;

    @BeforeEach
    void setUpParser() {
        parser = new TextModulIndividuellParser(
            codelistService,
            messageSource,
            new SuchdienstProperties()
        );
    }

    @Test
    void testParsing() {
        Iterator<TextmodulIndividuell> iterator = firstLbRaw().getModulTextIndividuell().iterator();
        TextmodulIndividuell modultextindividuell1 = iterator.next();
        TextmodulIndividuell modultextindividuell2 = iterator.next();

        ServiceModule block1 = parser.parse(modultextindividuell1, Locale.GERMAN);
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

        ServiceModule block2 = parser.parse(modultextindividuell2, Locale.GERMAN);
        assertAll(
            () -> assertThat(block2.getText(), equalTo(null)),
            () -> assertThat(block2.getTitle(), equalTo("00123")),
            () -> assertThat(block2.getLinks(), hasSize(0)),
            () -> assertThat(block2.getAdditionalInformation(), hasSize(0))
        );
    }
}
