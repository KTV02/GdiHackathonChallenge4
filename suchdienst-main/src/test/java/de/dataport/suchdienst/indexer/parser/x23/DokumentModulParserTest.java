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
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;

import de.dataport.suchdienst.indexer.model.mapping.KeyValueObject;
import de.dataport.suchdienst.indexer.model.mapping.ValidityPeriod;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetail;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetailContent;
import de.dataport.suchdienst.indexer.parser.x23.modul.DokumentModulParser;
import de.dataport.suchdienst.util.ServiceModuleCode;
import de.xoev.xzufi.v2_3_1.Dokumentmodul;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test für DokumentModulParser (x23) mit Fokus auf:
 *  - Dokumententyp/Signatur Zusatzinfos
 *  - Unterschied beizubringende vs. hervorgehende Dokumente
 *  - Validitätszeitraum & Link-Anzahl.
 */
class DokumentModulParserTest extends AbstractX23ParserTest {

    private static final Locale GERMAN = Locale.GERMAN;
    private DokumentModulParser parser;

    @BeforeEach
    void setUpParser() {
        parser = new DokumentModulParser(codelistService, messageSource);
    }

    @Test
    @SuppressWarnings("java:S5961") // Wir parsen hier ein großes Dokument
    void testParser() {
        List<Dokumentmodul> dokumentmodule = firstLbRaw().getModulDokument();
        assertThat(dokumentmodule, hasSize(1));
        Dokumentmodul dokumentmodul = dokumentmodule.getFirst();

        List<Map.Entry<ServiceModuleCode, AbstractServiceModule>> entries = parser.parseComposite(
            dokumentmodul,
            GERMAN
        );

        // parseComposite liefert 2 Einträge: Dokumentmodul (96) + Textmodul 08 (ERFORDERLICHE_UNTERLAGEN)
        assertThat(entries, hasSize(2));

        // --- Dokumentmodul (Code 96) ---
        Map.Entry<ServiceModuleCode, AbstractServiceModule> dokumentEntry = entries
            .stream()
            .filter(e -> e.getKey() == ServiceModuleCode.DOKUMENT)
            .findFirst()
            .orElseThrow();
        assertThat(dokumentEntry.getValue(), is(notNullValue()));
        ServiceModuleDetail block = (ServiceModuleDetail) dokumentEntry.getValue();

        Iterator<ServiceModuleDetailContent> iterator = block.getDetails().iterator();
        ServiceModuleDetailContent module1 = iterator.next(); // Beizubringend leerer Text
        ServiceModuleDetailContent module2 = iterator.next(); // Beizubringend mit Beschreibung
        ServiceModuleDetailContent module3 = iterator.next(); // Hervorgehend mit Beschreibung
        ServiceModuleDetailContent module4 = iterator.next(); // Hervorgehend leerer Text

        Iterator<KeyValueObject> iterator1 = module1.getAdditionalInformation().iterator();
        KeyValueObject additionalInfoModule1 = iterator1.next();
        KeyValueObject additionalInfoModule2 = iterator1.next();

        Iterator<KeyValueObject> iterator4 = module4.getAdditionalInformation().iterator();
        KeyValueObject additionalInfoModule4 = iterator4.next();
        KeyValueObject additionalInfoModule41 = iterator4.next();

        assertAll(
            // Da beschreibungEingehend gefüllt ist, gehen Links ins Textmodul 08 → Dokumentmodul hat keine Links
            () -> assertThat(block.getLinks(), hasSize(0)),
            () -> assertThat(block.getDetails(), hasSize(4)),
            () -> assertThat(block.getTitle(), equalTo("Dokumente")),
            // Text (beschreibungEingehend) wird nicht im Dokumentmodul erwartet, sondern im Textmodul 08
            () -> assertThat(block.getText(), is(nullValue())),
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
            () -> assertThat(additionalInfoModule1.textValue(), equalTo("002")),
            () -> assertThat(additionalInfoModule2.textKey(), equalTo("Erforderliche Signatur")),
            () -> assertThat(additionalInfoModule2.textValue(), equalTo("001")),
            () -> assertThat(module2.getTitle(), equalTo("Beizubringendes Dokument Reisepass")),
            () -> assertThat(module2.getText(), equalTo("Der Reisepass ist zwingend vorzulegen")),
            () -> assertThat(module2.getAdditionalInformation(), hasSize(1)),
            () ->
                assertThat(
                    module2.getAdditionalInformation().iterator().next().textValue(),
                    equalTo("001")
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
                    equalTo("999")
                ),
            () ->
                assertThat(
                    module4.getTitle(),
                    equalTo("Hervorgehendes Dokument Ausweis Erstellung")
                ),
            () -> assertThat(module4.getText(), emptyString()),
            () -> assertThat(module4.getAdditionalInformation(), hasSize(2)),
            () -> assertThat(additionalInfoModule4.textValue(), equalTo("003")),
            () -> assertThat(additionalInfoModule41.textValue(), equalTo("001"))
        );

        // --- Textmodul 08 (ERFORDERLICHE_UNTERLAGEN) ---
        Map.Entry<ServiceModuleCode, AbstractServiceModule> textEntry = entries
            .stream()
            .filter(e -> e.getKey() == ServiceModuleCode.ERFORDERLICHE_UNTERLAGEN)
            .findFirst()
            .orElseThrow();
        assertThat(textEntry.getValue(), is(notNullValue()));
        ServiceModule textModule = (ServiceModule) textEntry.getValue();

        assertAll(
            () -> assertThat(textModule.getTitle(), equalTo("Dokumente")),
            () -> assertThat(textModule.getText(), equalTo("Dokumentenbeschreibung")),
            // Links werden ins Textmodul 08 übernommen, da beschreibungEingehend gefüllt ist
            () -> assertThat(textModule.getLinks(), hasSize(1)),
            () ->
                assertThat(
                    textModule.getValidityPeriods().getFirst(),
                    equalTo(
                        new ValidityPeriod(LocalDate.of(2018, 5, 11), LocalDate.of(2019, 5, 11))
                    )
                )
        );
    }

    @Test
    void testParserOhneBeschreibungEingehend() {
        List<Dokumentmodul> dokumentmodule = firstLbRaw().getModulDokument();
        Dokumentmodul dokumentmodul = dokumentmodule.getFirst();

        // beschreibungEingehend leeren → Links bleiben im Dokumentmodul, kein Textmodul 08
        dokumentmodul.getBeschreibungEingehend().clear();

        List<Map.Entry<ServiceModuleCode, AbstractServiceModule>> entries = parser.parseComposite(
            dokumentmodul,
            GERMAN
        );

        // Nur Dokumentmodul (96), kein Textmodul 08 da beschreibungEingehend leer
        assertThat(entries, hasSize(1));

        Map.Entry<ServiceModuleCode, AbstractServiceModule> dokumentEntry = entries.getFirst();
        assertThat(dokumentEntry.getKey(), equalTo(ServiceModuleCode.DOKUMENT));
        ServiceModuleDetail block = (ServiceModuleDetail) dokumentEntry.getValue();

        assertAll(
            // Links bleiben im Dokumentmodul wenn beschreibungEingehend leer ist
            () -> assertThat(block.getLinks(), hasSize(1)),
            () ->
                assertThat(
                    block.getLinks().getFirst().uri(),
                    equalTo("https://www.dokumentenlink.de")
                ),
            () -> assertThat(block.getDetails(), hasSize(4)),
            () -> assertThat(block.getTitle(), equalTo("Dokumente")),
            () -> assertThat(block.getText(), is(nullValue()))
        );

        // Kein Textmodul 08 vorhanden
        boolean hasErforderlicheUnterlagen = entries
            .stream()
            .anyMatch(e -> e.getKey() == ServiceModuleCode.ERFORDERLICHE_UNTERLAGEN);
        assertThat(hasErforderlicheUnterlagen, is(false));
    }
}
