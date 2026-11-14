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
package de.dataport.suchdienst.indexer.parser.x22;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.dataport.suchdienst.indexer.model.verwaltungsleistung.FormularContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.FormularFileLink;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.FormularVL;
import de.dataport.suchdienst.indexer.parser.x22.locale.LanguageAwareFoParser;
import de.dataport.suchdienst.indexer.service.languagecheck.LanguageFoCompleteCheckService;
import de.xoev.xzufi.v2_2_0.Formular;
import de.xoev.xzufi.v2_2_0.FormulardateiLink;
import de.xoev.xzufi.v2_2_0.Identifikator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Testet das Einlesen von Formularen aus XZuFi in FormularVL-Objekte.
 * Die Testfälle sind der Dokumentation des Tickets entnommen: <a href="https://jira.dataport.de/browse/PV-14438">PV-14438</a>
 */
class XZuFiFoParserServiceTest {

    @Test
    void parseFormularKeepsSingleXzufi22FileLinkForGermanFormular() {
        // Fall 8: Formular A in de; Link
        LanguageAwareFoParser languageParser = mock(LanguageAwareFoParser.class);
        when(languageParser.parseFormular(any(), any())).thenReturn(contentByLocale(Locale.GERMAN));

        XZuFiFoParserService parser = new XZuFiFoParserService(
            languageParser,
            new LanguageFoCompleteCheckService()
        );
        Formular formular = formular(fileLink("application/pdf", "https://example.org/form.pdf"));

        FormularVL parsed = parser.parseFormular(formular);

        assertUris(parsed, Locale.GERMAN, "https://example.org/form.pdf");
    }

    @Test
    void parseFormularKeepsMultipleXzufi22FileLinksForGermanFormular() {
        // Fall 9: Formular A in de; zwei Links
        LanguageAwareFoParser languageParser = mock(LanguageAwareFoParser.class);
        when(languageParser.parseFormular(any(), any())).thenReturn(contentByLocale(Locale.GERMAN));

        XZuFiFoParserService parser = new XZuFiFoParserService(
            languageParser,
            new LanguageFoCompleteCheckService()
        );
        Formular formular = formular(
            fileLink("application/pdf", "https://example.org/form-1.pdf"),
            fileLink("application/pdf", "https://example.org/form-2.pdf")
        );

        FormularVL parsed = parser.parseFormular(formular);

        assertUris(
            parsed,
            Locale.GERMAN,
            "https://example.org/form-1.pdf",
            "https://example.org/form-2.pdf"
        );
    }

    @Test
    void parseFormularKeepsSingleXzufi22FileLinkForEveryLanguage() {
        // Fall 10: Formular A in de,en; Link
        LanguageAwareFoParser languageParser = mock(LanguageAwareFoParser.class);
        when(languageParser.parseFormular(any(), any())).thenReturn(
            contentByLocale(Locale.GERMAN, Locale.ENGLISH)
        );

        XZuFiFoParserService parser = new XZuFiFoParserService(
            languageParser,
            new LanguageFoCompleteCheckService()
        );
        Formular formular = formular(fileLink("application/pdf", "https://example.org/form.pdf"));

        FormularVL parsed = parser.parseFormular(formular);

        assertUris(parsed, Locale.GERMAN, "https://example.org/form.pdf");
        assertUris(parsed, Locale.ENGLISH, "https://example.org/form.pdf");
    }

    @Test
    void parseFormularKeepsMultipleXzufi22FileLinksForEveryLanguage() {
        // Fall 11: Formular A in de,en; zwei Links
        LanguageAwareFoParser languageParser = mock(LanguageAwareFoParser.class);
        when(languageParser.parseFormular(any(), any())).thenReturn(
            contentByLocale(Locale.GERMAN, Locale.ENGLISH)
        );

        XZuFiFoParserService parser = new XZuFiFoParserService(
            languageParser,
            new LanguageFoCompleteCheckService()
        );
        Formular formular = formular(
            fileLink("application/pdf", "https://example.org/form-1.pdf"),
            fileLink("application/pdf", "https://example.org/form-2.pdf")
        );

        FormularVL parsed = parser.parseFormular(formular);

        assertUris(
            parsed,
            Locale.GERMAN,
            "https://example.org/form-1.pdf",
            "https://example.org/form-2.pdf"
        );
        assertUris(
            parsed,
            Locale.ENGLISH,
            "https://example.org/form-1.pdf",
            "https://example.org/form-2.pdf"
        );
    }

    @Test
    void parseFormularKeepsOnlyOwnLanguageForSeparateXzufi22GermanAndEnglishFormularObjects() {
        // Fall 12: Formular A in de, Link1; Formular B in en, Link2
        LanguageAwareFoParser germanLanguageParser = mock(LanguageAwareFoParser.class);
        when(germanLanguageParser.parseFormular(any(), any())).thenReturn(
            contentByLocale(Locale.GERMAN)
        );
        LanguageAwareFoParser englishLanguageParser = mock(LanguageAwareFoParser.class);
        when(englishLanguageParser.parseFormular(any(), any())).thenReturn(
            contentByLocale(Locale.ENGLISH)
        );

        XZuFiFoParserService germanParser = new XZuFiFoParserService(
            germanLanguageParser,
            new LanguageFoCompleteCheckService()
        );
        XZuFiFoParserService englishParser = new XZuFiFoParserService(
            englishLanguageParser,
            new LanguageFoCompleteCheckService()
        );

        FormularVL germanFormular = germanParser.parseFormular(
            formular(fileLink("application/pdf", "https://example.org/form-de.pdf"))
        );
        FormularVL englishFormular = englishParser.parseFormular(
            formular(fileLink("application/pdf", "https://example.org/form-en.pdf"))
        );

        assertUris(germanFormular, Locale.GERMAN, "https://example.org/form-de.pdf");
        assertUris(englishFormular, Locale.ENGLISH, "https://example.org/form-en.pdf");
    }

    private static Formular formular(FormulardateiLink... fileLinks) {
        Formular formular = new Formular();
        Identifikator id = new Identifikator();
        id.setValue("FORMULAR-1");
        id.setSchemeAgencyID("TEST");
        formular.setId(id);
        formular.getDatei().addAll(List.of(fileLinks));
        return formular;
    }

    private static FormulardateiLink fileLink(String mimeCode, String uri) {
        FormulardateiLink fileLink = new FormulardateiLink();
        fileLink.setMimeCode(mimeCode);
        fileLink.setUri(uri);
        return fileLink;
    }

    private static Map<Locale, FormularContent> contentByLocale(Locale... locales) {
        Map<Locale, FormularContent> contentByLocale = new HashMap<>();
        for (Locale locale : locales) {
            FormularContent content = new FormularContent();
            content.setName("Formular " + locale.toLanguageTag());
            content.setDescription("Beschreibung " + locale.toLanguageTag());
            contentByLocale.put(locale, content);
        }
        return contentByLocale;
    }

    private static List<String> uris(FormularVL formular) {
        return formular.getFileLinks().stream().map(FormularFileLink::getUri).toList();
    }

    private static void assertUris(FormularVL formular, Locale locale, String... expectedUris) {
        assertThat(uris(formular.copyByLanguage(locale).orElseThrow()), contains(expectedUris));
    }
}
