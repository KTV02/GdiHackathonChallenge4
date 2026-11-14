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
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;

import de.dataport.suchdienst.indexer.model.mapping.KeyValueObject;
import de.dataport.suchdienst.indexer.model.mapping.ValidityPeriod;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetail;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetailContent;
import de.dataport.suchdienst.indexer.parser.x22.modul.DokumentModulParser;
import de.xoev.xzufi.v2_2_0.Dokumentmodul;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Testet das Dokumentmodul (x22) inkl.:
 *  - Titel & Beschreibung des Blocks
 *  - Zusatzinformationen wie Dokumententyp / Signatur
 *  - Unterschied Beizubringendes vs. Hervorgehendes Dokument.
 */
class DokumentModulParserTest extends AbstractX22ParserTest {

    private DokumentModulParser parser;

    @BeforeEach
    void setup() {
        parser = new DokumentModulParser(codelistService, messageSource);
    }

    @Test
    @SuppressWarnings("java:S5961") // Wir parsen hier ein großes Dokument
    void testParser() {
        Dokumentmodul dokumentModul = firstLbRaw().getModulDokument();
        ServiceModuleDetail block = parser.parse(dokumentModul, Locale.GERMAN);

        // Iterator zur strukturierten Validierung
        Iterator<ServiceModuleDetailContent> iterator = block.getDetails().iterator();
        ServiceModuleDetailContent module1 = iterator.next(); // Beizubringend ohne Beschreibung
        ServiceModuleDetailContent module2 = iterator.next(); // Beizubringend mit Beschreibung
        ServiceModuleDetailContent module3 = iterator.next(); // Hervorgehend mit Beschreibung
        ServiceModuleDetailContent module4 = iterator.next(); // Hervorgehend ohne Beschreibung

        Iterator<KeyValueObject> iterator1 = module1.getAdditionalInformation().iterator();
        KeyValueObject additionalInfoModule1 = iterator1.next();
        KeyValueObject additionalInfoModule2 = iterator1.next();

        Iterator<KeyValueObject> iterator4 = module4.getAdditionalInformation().iterator();
        KeyValueObject additionalInfoModule4 = iterator4.next();
        KeyValueObject additionalInfoModule41 = iterator4.next();

        assertAll(
            () -> assertThat(block.getLinks(), hasSize(1)),
            () -> assertThat(block.getDetails(), hasSize(4)),
            () -> assertThat(block.getTitle(), equalTo("Dokumente")),
            () -> assertThat(block.getText(), equalTo("Dokumentenbeschreibung")),
            () ->
                assertThat(
                    block.getValidityPeriods().getFirst(),
                    equalTo(
                        new ValidityPeriod(LocalDate.of(2018, 5, 11), LocalDate.of(2019, 5, 11))
                    )
                ),
            () -> assertThat(module1.getTitle(), equalTo("Beizubringendes Dokument Ausweis")),
            () -> assertThat(module1.getText(), emptyString()),
            () -> assertThat(module1.getAdditionalInformation(), hasSize(2)),
            () -> assertThat(additionalInfoModule1.textKey(), equalTo("Dokumententyp")),
            () -> assertThat(additionalInfoModule1.textValue(), equalTo("003")),
            () -> assertThat(additionalInfoModule2.textKey(), equalTo("Erforderliche Signatur")),
            () -> assertThat(additionalInfoModule2.textValue(), equalTo("001")),
            () -> assertThat(module2.getTitle(), equalTo("Beizubringendes Dokument Reisepass")),
            () -> assertThat(module2.getText(), equalTo("Der Reisepass ist zwingend vorzulegen")),
            () -> assertThat(module2.getAdditionalInformation(), hasSize(1)),
            () ->
                assertThat(
                    module2.getAdditionalInformation().iterator().next().textValue(),
                    equalTo("002")
                ),
            () ->
                assertThat(
                    module3.getTitle(),
                    equalTo("Hervorgehendes Dokument Reisepass Erstellung")
                ),
            () -> assertThat(module3.getText(), equalTo("Der Reisepass wird erstellt")),
            () -> assertThat(module3.getAdditionalInformation(), hasSize(1)),
            () ->
                assertThat(
                    module3.getAdditionalInformation().iterator().next().textValue(),
                    equalTo("004")
                ),
            () ->
                assertThat(
                    module4.getTitle(),
                    equalTo("Hervorgehendes Dokument Ausweis Erstellung")
                ),
            () -> assertThat(module4.getText(), emptyString()),
            () -> assertThat(module4.getAdditionalInformation(), hasSize(2)),
            () -> assertThat(additionalInfoModule4.textValue(), equalTo("005")),
            () -> assertThat(additionalInfoModule41.textValue(), equalTo("001"))
        );
    }
}
