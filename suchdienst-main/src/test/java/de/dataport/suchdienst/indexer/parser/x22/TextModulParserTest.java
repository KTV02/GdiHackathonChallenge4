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
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dataport.suchdienst.indexer.model.mapping.CodeServiceDescription;
import de.dataport.suchdienst.indexer.model.mapping.ValidityPeriod;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModule;
import de.dataport.suchdienst.indexer.parser.x22.modul.TextModulParser;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit-Test für {@link TextModulParser} (x22) ohne Spring-Kontext.
 * Prüft u.a.:
 *  - Filterlogik (Code 05 soll ignoriert werden)
 *  - Mapping von CodeServiceDescription (Volltext)
 *  - Links, Titel/Text und Validitätszeitraum einzelner Module.
 */
class TextModulParserTest extends AbstractX22ParserTest {

    private TextModulParser parser;

    @BeforeEach
    void setUp() {
        parser = new TextModulParser(codelistService);
    }

    @Test
    void testParsing() {
        List<ServiceModule> details = new ArrayList<>();

        firstLbRaw()
            .getModulText()
            .forEach(modultext -> {
                ServiceModule block = parser.parse(modultext, Locale.GERMAN);
                if (block != null) {
                    assertThat(block.getAdditionalInformation(), hasSize(0));
                    details.add(block);
                }
            });

        // Erwartung: Code 03 enthalten, Code 05 herausgefiltert
        assertTrue(
            details.stream().anyMatch(detail -> "Meine Leistung".equals(detail.getText())),
            "Code 03 soll eingelesen werden!"
        );
        assertTrue(
            details.stream().noneMatch(detail -> "Kurze Beschreibung".equals(detail.getText())),
            "Code 05 darf nicht eingelesen werden!"
        );

        ServiceModule block1 = details.get(0); // Code 01 leer
        ServiceModule block2 = details.get(1); // Code 02 normale Bezeichnung
        ServiceModule block3 = details.get(2); // Code 03 gewünschte Leistung
        ServiceModule block4 = details.get(3); // Volltext (Mapping auf VOLLTEXT)
        ServiceModule block5 = details.get(4); // Code 18 mit Links
        ServiceModule block6 = details.get(5); // Abweichende Bezeichnung
        ServiceModule block7 = details.get(6); // Abweichende Bezeichnung 2

        assertAll(
            () -> assertThat(details, hasSize(7)),
            () -> assertThat(block1.getText(), emptyOrNullString()),
            () -> assertThat(block1.getTitle(), equalTo("01")),
            () -> assertThat(block1.getLinks(), hasSize(0)),
            () -> assertThat(block2.getText(), equalTo("Die normale Bezeichnung")),
            () -> assertThat(block2.getTitle(), equalTo("02")),
            () ->
                assertThat(
                    block2.getValidityPeriods().getFirst(),
                    equalTo(
                        new ValidityPeriod(LocalDate.of(2018, 5, 11), LocalDate.of(2019, 5, 11))
                    )
                ),
            () -> assertThat(block3.getText(), equalTo("Meine Leistung")),
            () -> assertThat(block3.getTitle(), equalTo("03")),
            () -> assertThat(block4.getText(), equalTo("Die lange Beschreibung")),
            () -> assertThat(block4.getTitle(), equalTo(CodeServiceDescription.VOLLTEXT.getCode())),
            () -> assertThat(block5.getText(), emptyOrNullString()),
            () -> assertThat(block5.getTitle(), equalTo("18")),
            () -> assertThat(block5.getLinks(), hasSize(2)),
            () -> assertThat(block6.getText(), equalTo("Inhalt in diesem Modul")),
            () -> assertThat(block6.getTitle(), equalTo("Abweichende Bezeichnung")),
            () -> assertThat(block7.getTitle(), equalTo("Abweichende Bezeichnung 2"))
        );
    }
}
