/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.indexer.parser.x23;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;

import de.dataport.suchdienst.indexer.model.mapping.ValidityPeriod;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDate;
import de.dataport.suchdienst.indexer.parser.x23.modul.FachlicheFreigabeModulParser;
import de.dataport.suchdienst.util.ServiceModuleCode;
import de.xoev.xzufi.v2_3_1.FachlicheFreigabeModul;
import java.time.LocalDate;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FachlicheFreigabeModulParserTest extends AbstractX23ParserTest {

    private FachlicheFreigabeModulParser parser;

    @BeforeEach
    void setUpParser() {
        parser = new FachlicheFreigabeModulParser(messageSource);
    }

    @Test
    void testParsing() {
        ServiceModuleDate block = parser.parse(firstFreigabeModul(), Locale.GERMAN);

        assertAll(
            () -> assertThat(block.getTitle(), equalTo("Fachliche Freigabe")),
            () -> assertThat(block.getText(), equalTo("Testministerium für Verwaltung")),
            () -> assertThat(block.getDate(), equalTo(LocalDate.of(2018, 5, 11))),
            () ->
                assertThat(
                    block.getValidityPeriods(),
                    contains(
                        new ValidityPeriod(LocalDate.of(2018, 5, 11), LocalDate.of(2019, 5, 11))
                    )
                ),
            () -> assertThat(block.hasContent(), is(true))
        );
    }

    @Test
    void testParsingEnglish() {
        ServiceModuleDate block = parser.parse(firstFreigabeModul(), Locale.ENGLISH);

        assertAll(
            () -> assertThat(block.getTitle(), equalTo("Technical approval")),
            () -> assertThat(block.getText(), equalTo("Test Ministry of Administration")),
            () -> assertThat(block.getDate(), equalTo(LocalDate.of(2018, 5, 11))),
            () ->
                assertThat(
                    block.getValidityPeriods(),
                    contains(
                        new ValidityPeriod(LocalDate.of(2018, 5, 11), LocalDate.of(2019, 5, 11))
                    )
                ),
            () -> assertThat(block.hasContent(), is(true))
        );
    }

    @Test
    void identifiesIncludedLanguages() {
        assertThat(
            parser.getIncludedLanguages(firstFreigabeModul()),
            containsInAnyOrder("de", "en")
        );
    }

    @Test
    void identifiesModuleCode() {
        assertThat(parser.getCode(), is(ServiceModuleCode.FACHLICHE_FREIGABE));
    }

    @Test
    void parsesWithoutApprovalDate() {
        FachlicheFreigabeModul modul = firstFreigabeModul();
        modul.setFachlichFreigegebenAm(null);

        ServiceModuleDate block = parser.parse(modul, Locale.GERMAN);

        assertAll(
            () -> assertThat(block.getDate(), nullValue()),
            () -> assertThat(block.getText(), equalTo("Testministerium für Verwaltung")),
            () -> assertThat(block.hasContent(), is(true))
        );
    }

    @Test
    void parsesWithoutApprovingAuthority() {
        FachlicheFreigabeModul modul = firstFreigabeModul();
        modul.getFachlichFreigegebenDurch().clear();

        ServiceModuleDate block = parser.parse(modul, Locale.GERMAN);

        assertAll(
            () -> assertThat(block.getText(), nullValue()),
            () -> assertThat(block.getDate(), equalTo(LocalDate.of(2018, 5, 11))),
            () -> assertThat(block.hasContent(), is(true)),
            () -> assertThat(parser.getIncludedLanguages(modul), empty())
        );
    }

    @Test
    void hasNoContentWithoutApprovalDateAndAuthority() {
        FachlicheFreigabeModul modul = firstFreigabeModul();
        modul.setFachlichFreigegebenAm(null);
        modul.getFachlichFreigegebenDurch().clear();

        ServiceModuleDate block = parser.parse(modul, Locale.GERMAN);

        assertAll(
            () -> assertThat(block.getDate(), nullValue()),
            () -> assertThat(block.getText(), nullValue()),
            () -> assertThat(block.hasContent(), is(false))
        );
    }

    private FachlicheFreigabeModul firstFreigabeModul() {
        var module = firstLbRaw().getModulFachlicheFreigabe();
        assertThat(module, hasSize(1));
        return module.getFirst();
    }
}
