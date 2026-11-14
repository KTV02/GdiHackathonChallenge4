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

import de.dataport.suchdienst.indexer.model.mapping.ValidityPeriod;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetail;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetailContent;
import de.dataport.suchdienst.indexer.parser.x23.modul.FristHelper;
import de.dataport.suchdienst.indexer.parser.x23.modul.FristModulParser;
import de.xoev.xzufi.v2_3_1.Fristmodul;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * FristModulParser Test (x23): Prüft Titel, Beschreibung, Validitätszeitraum, Link-Anzahl
 * sowie Aufbereitung einzelner Fristen (Dauer, Stichtag, nicht gelisteter Wert).
 */
class FristModulParserTest extends AbstractX23ParserTest {

    private FristModulParser parser;
    private FristHelper fristHelper;

    @BeforeEach
    void setUpParser() throws Exception {
        fristHelper = new FristHelper(codelistService);
        parser = new FristModulParser(codelistService, fristHelper, messageSource);
    }

    @Test
    void testParser() {
        List<Fristmodul> fristmodule = firstLbRaw().getModulFrist();
        assertThat(fristmodule, hasSize(1));
        Fristmodul fristmodul = fristmodule.getFirst();
        ServiceModuleDetail block = parser.parse(fristmodul, Locale.GERMAN);

        Iterator<ServiceModuleDetailContent> iterator = block.getDetails().iterator();
        ServiceModuleDetailContent frist1 = iterator.next(); // Dauer
        ServiceModuleDetailContent frist2 = iterator.next(); // Zeitraum
        ServiceModuleDetailContent frist3 = iterator.next(); // Nicht gelisteter Wert

        assertAll(
            () -> assertThat(block.getTitle(), equalTo("Fristen")),
            () -> assertThat(block.getText(), equalTo("Beschreibung der Frist in diesem Modul")),
            () ->
                assertThat(
                    block.getValidityPeriods().getFirst(),
                    equalTo(
                        new ValidityPeriod(LocalDate.of(2018, 5, 11), LocalDate.of(2019, 5, 11))
                    )
                ),
            () -> assertThat(block.getDetails(), hasSize(3)),
            () -> assertThat(block.getLinks(), hasSize(1)),
            () -> assertThat(frist1.getTitle(), equalTo("002")),
            () -> assertThat(frist1.getText(), equalTo("Frist2 ist hier beschrieben: 3 Monate")),
            () -> assertThat(frist2.getTitle(), equalTo("003")),
            () -> assertThat(frist2.getText(), equalTo("01.01.2018 - 31.12.2018")),
            () -> assertThat(frist3.getTitle(), equalTo("TEST nicht gelisteter Wert")),
            () -> assertThat(frist3.getText(), equalTo("Frist3 ist hier beschrieben: 3 Jahre"))
        );
    }
}
