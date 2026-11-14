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
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;

import de.dataport.suchdienst.indexer.model.mapping.KeyValueObject;
import de.dataport.suchdienst.indexer.model.mapping.ValidityPeriod;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetail;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetailContent;
import de.dataport.suchdienst.indexer.parser.x22.modul.KostenModulParser;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Testet das Kostenmodul (x22) in Deutsch und Englisch:
 *  - Transformation von Kostenbereichen / Einzelkosten
 *  - Zusätzliche Informationen (z.B. Vorkasse) und Links
 *  - Handling leerer Texte / nicht gelisteter Werte.
 */
class KostenModulParserTest extends AbstractX22ParserTest {

    private KostenModulParser parser;

    @BeforeEach
    void setup() {
        parser = new KostenModulParser(codelistService, messageSource);
    }

    @Test
    void testParsingEnglish() {
        ServiceModuleDetail block = parser.parse(firstLbRaw().getModulKosten(), Locale.ENGLISH);
        Iterator<ServiceModuleDetailContent> iterator = block.getDetails().iterator();
        ServiceModuleDetailContent kosten1 = iterator.next();
        assertThat(kosten1.getTitle(), equalTo("002: 3.00 Euro"));
    }

    @Test
    @SuppressWarnings("java:S5961") // Wir parsen hier ein großes Dokument
    void testParsingGerman() {
        ServiceModuleDetail block = parser.parse(firstLbRaw().getModulKosten(), Locale.GERMAN);

        Iterator<ServiceModuleDetailContent> iterator = block.getDetails().iterator();
        ServiceModuleDetailContent kosten1 = iterator.next();
        ServiceModuleDetailContent kosten2 = iterator.next();
        ServiceModuleDetailContent kosten3 = iterator.next();
        ServiceModuleDetailContent kosten4 = iterator.next();
        ServiceModuleDetailContent kosten5 = iterator.next();
        ServiceModuleDetailContent kosten6 = iterator.next();
        ServiceModuleDetailContent kosten7 = iterator.next();
        ServiceModuleDetailContent kosten8 = iterator.next();
        ServiceModuleDetailContent kosten9 = iterator.next();
        ServiceModuleDetailContent kosten10 = iterator.next();

        Iterator<KeyValueObject> iterator1 = kosten1.getAdditionalInformation().iterator();
        KeyValueObject additionalInformation13 = iterator1.next();

        assertAll(
            // Block-Metadaten
            () -> assertThat(block.getTitle(), equalTo("Kosten & Gebühren")),
            () -> assertThat(block.getText(), equalTo("Diese Kosten treten einfach auf")),
            () -> assertThat(block.getDetails(), hasSize(10)),
            () -> assertThat(block.getLinks(), hasSize(1)),
            () ->
                assertThat(
                    block.getValidityPeriods().getFirst(),
                    equalTo(
                        new ValidityPeriod(LocalDate.of(2018, 5, 11), LocalDate.of(2019, 5, 11))
                    )
                ),
            // Einzelkosten mit Zusatzinfo
            () -> assertThat(kosten1.getTitle(), equalTo("002: 3,00 Euro")),
            () -> assertThat(kosten1.getText(), equalTo("Das muss bezahlt werden!")),
            () -> assertThat(kosten1.getLinks(), hasSize(1)),
            () -> assertThat(kosten1.getAdditionalInformation(), hasSize(1)),
            () -> assertThat(additionalInformation13.textKey(), equalTo("Vorkasse")),
            () -> assertThat(additionalInformation13.textValue(), equalTo("Nein")),
            // Nicht gelistete Werte ohne Text
            () -> assertThat(kosten2.getTitle(), equalTo("TestNichtGelisteterWert: 5,00 Euro")),
            () -> assertThat(kosten2.getText(), nullValue()),
            () -> assertThat(kosten2.getLinks(), hasSize(0)),
            () -> assertThat(kosten2.getAdditionalInformation(), hasSize(0)),
            () -> assertThat(kosten3.getTitle(), equalTo("TestNichtGelisteterWert: 8,00 Euro")),
            () -> assertThat(kosten3.getText(), nullValue()),
            () -> assertThat(kosten3.getLinks(), hasSize(0)),
            () -> assertThat(kosten3.getAdditionalInformation(), hasSize(0)),
            // Kostenfrei Varianten
            () -> assertThat(kosten4.getTitle(), equalTo("003: Einfach für umsonst")),
            () -> assertThat(kosten4.getText(), nullValue()),
            () -> assertThat(kosten4.getLinks(), hasSize(0)),
            () -> assertThat(kosten4.getAdditionalInformation(), hasSize(1)),
            () ->
                assertThat(
                    kosten4.getAdditionalInformation().iterator().next().textValue(),
                    equalTo("Ja")
                ),
            () ->
                assertThat(
                    kosten5.getTitle(),
                    equalTo("TestNichtGelisteterWert: Einfach für umsonst")
                ),
            () -> assertThat(kosten5.getText(), nullValue()),
            () -> assertThat(kosten5.getLinks(), hasSize(0)),
            () -> assertThat(kosten5.getAdditionalInformation(), hasSize(0)),
            () -> assertThat(kosten6.getTitle(), equalTo("004: kostenfrei")),
            () -> assertThat(kosten6.getText(), nullValue()),
            () -> assertThat(kosten6.getLinks(), hasSize(0)),
            () -> assertThat(kosten6.getAdditionalInformation(), hasSize(0)),
            // Leerer Betrag
            () -> assertThat(kosten7.getTitle(), equalTo("005: ")),
            () -> assertThat(kosten7.getText(), nullValue()),
            () -> assertThat(kosten7.getLinks(), hasSize(0)),
            () -> assertThat(kosten7.getAdditionalInformation(), hasSize(0)),
            // Kostenbereich + Zusatzinfo
            () -> assertThat(kosten8.getTitle(), equalTo("006: 3,00 Euro - 8,00 Euro")),
            () -> assertThat(kosten8.getText(), nullValue()),
            () -> assertThat(kosten8.getLinks(), hasSize(0)),
            () -> assertThat(kosten8.getAdditionalInformation(), hasSize(1)),
            () ->
                assertThat(
                    kosten8.getAdditionalInformation().iterator().next().textKey(),
                    emptyOrNullString()
                ),
            () ->
                assertThat(
                    kosten8.getAdditionalInformation().iterator().next().textValue(),
                    equalTo("Je nachdem wie der Sachbearbeiter drauf ist")
                ),
            // Ober-/Untergrenze
            () -> assertThat(kosten9.getTitle(), equalTo("007: Obergrenze: 3,00 Euro")),
            () -> assertThat(kosten9.getText(), nullValue()),
            () -> assertThat(kosten9.getLinks(), hasSize(0)),
            () -> assertThat(kosten9.getAdditionalInformation(), hasSize(0)),
            () -> assertThat(kosten10.getTitle(), equalTo("008: Untergrenze: 3,00 Euro")),
            () -> assertThat(kosten10.getText(), nullValue()),
            () -> assertThat(kosten10.getLinks(), hasSize(0)),
            () -> assertThat(kosten10.getAdditionalInformation(), hasSize(0))
        );
    }
}
