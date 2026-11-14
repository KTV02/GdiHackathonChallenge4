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

import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.model.mapping.ValidityPeriod;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetail;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetailContent;
import de.dataport.suchdienst.indexer.parser.x22.modul.BearbeitungsdauerModulParser;
import de.dataport.suchdienst.indexer.parser.x22.modul.FristHelper;
import de.dataport.suchdienst.indexer.service.CodelistService;
import de.xoev.xzufi.v2_2_0.Bearbeitungsdauermodul;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Testet die Aufbereitung des Bearbeitungsdauermoduls (x22):
 *  - Titel/Text des Hauptblocks
 *  - Anzahl und Inhalt der Detail-Einträge (Dauer & Stichtag Varianten)
 *  - Validitätszeitraum und Link-Anzahl.
 */
class BearbeitungsdauerModulParserTest extends AbstractX22ParserTest {

    private BearbeitungsdauerModulParser parser;

    @BeforeEach
    void setup() throws Exception {
        // Arrange
        CodelistService codelistService = Mockito.mock(CodelistService.class);
        Mockito.when(
            codelistService.getCodelistEntry(Mockito.any(), Mockito.any(), Mockito.any())
        ).thenAnswer(inv -> {
            CodelistEntry e = new CodelistEntry();
            e.setText(Locale.GERMAN, "Stunde");
            e.setText(Locale.ENGLISH, "hour");
            return e;
        });
        FristHelper fristHelper = new FristHelper(codelistService);

        parser = new BearbeitungsdauerModulParser(fristHelper, messageSource);
    }

    @Test
    void testParsing() {
        Bearbeitungsdauermodul modul = firstLbRaw().getModulBearbeitungsdauer();
        ServiceModuleDetail block = parser.parse(modul, Locale.GERMAN);

        // Detail-Iterator für strukturierte Assertions
        Iterator<ServiceModuleDetailContent> iterator = block.getDetails().iterator();
        ServiceModuleDetailContent blockDauer = iterator.next();
        ServiceModuleDetailContent blockDauer2 = iterator.next();
        ServiceModuleDetailContent blockStichtag = iterator.next();
        ServiceModuleDetailContent blockStichtag2 = iterator.next();
        ServiceModuleDetailContent blockStichtag3 = iterator.next();

        assertAll(
            () -> assertThat(block.getTitle(), equalTo("Bearbeitungsdauer")),
            () -> assertThat(block.getText(), equalTo("Bearbeitungsdauer für Frist Dauer")),
            () -> assertThat(block.getLinks(), hasSize(1)),
            () -> assertThat(block.getDetails(), hasSize(5)),
            () ->
                assertThat(
                    block.getValidityPeriods().getFirst(),
                    equalTo(
                        new ValidityPeriod(LocalDate.of(2018, 5, 11), LocalDate.of(2019, 5, 11))
                    )
                ),
            () -> assertThat(blockDauer.getTitle(), equalTo("Fristdauer")),
            () -> assertThat(blockDauer.getText(), equalTo("5 - 17 Stunden")),
            () -> assertThat(blockDauer.getAdditionalInformation(), hasSize(0)),
            () -> assertThat(blockDauer2.getTitle(), equalTo("Fristdauer ohne bis")),
            () -> assertThat(blockDauer2.getText(), equalTo("5 Stunden")),
            () -> assertThat(blockDauer2.getAdditionalInformation(), hasSize(0)),
            () -> assertThat(blockStichtag.getTitle(), equalTo("Friststichtag")),
            () -> assertThat(blockStichtag.getText(), equalTo("25.11.2018 - 30.11.2018")),
            () -> assertThat(blockStichtag.getAdditionalInformation(), hasSize(0)),
            () -> assertThat(blockStichtag2.getTitle(), equalTo("Friststichtag MonatTag")),
            () -> assertThat(blockStichtag2.getText(), equalTo("12.11. - 30.11.")),
            () -> assertThat(blockStichtag3.getTitle(), equalTo("Friststichtag Tag")),
            () -> assertThat(blockStichtag3.getText(), equalTo("12. - 30."))
        );
    }
}
