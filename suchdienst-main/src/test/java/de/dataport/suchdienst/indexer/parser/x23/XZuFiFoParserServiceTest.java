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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.dataport.suchdienst.indexer.model.verwaltungsleistung.FormularContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.FormularFileLink;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.FormularVL;
import de.dataport.suchdienst.indexer.parser.x23.locale.LanguageAwareFoParser;
import de.dataport.suchdienst.indexer.service.languagecheck.LanguageFoCompleteCheckService;
import de.xoev.xzufi.v2_3_1.Formular;
import de.xoev.xzufi.v2_3_1.FormulardateiLink;
import de.xoev.xzufi.v2_3_1.HyperlinkErweitert;
import de.xoev.xzufi.v2_3_1.Identifikator;
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
    void parseFormularKeepsGermanFileLinkForGermanFormular() {
        // Fall 1: Formular A in de; ein Dateilink in de
        LanguageAwareFoParser languageParser = mock(LanguageAwareFoParser.class);
        when(languageParser.parseFormular(any(), any())).thenReturn(contentByLocale(Locale.GERMAN));

        XZuFiFoParserService parser = new XZuFiFoParserService(
            languageParser,
            new LanguageFoCompleteCheckService()
        );
        Formular formular = formular(
            fileLink("application/pdf", "https://example.org/form-de.pdf", "de")
        );

        FormularVL parsed = parser.parseFormular(formular);

        assertUris(parsed, Locale.GERMAN, "https://example.org/form-de.pdf");
    }

    @Test
    void parseFormularKeepsMultipleGermanFileLinksForGermanFormular() {
        // Fall 2: Formular A in de; zwei Dateilinks in de
        LanguageAwareFoParser languageParser = mock(LanguageAwareFoParser.class);
        when(languageParser.parseFormular(any(), any())).thenReturn(contentByLocale(Locale.GERMAN));

        XZuFiFoParserService parser = new XZuFiFoParserService(
            languageParser,
            new LanguageFoCompleteCheckService()
        );
        Formular formular = formular(
            fileLink("application/pdf", "https://example.org/form-de-1.pdf", "de"),
            fileLink("application/pdf", "https://example.org/form-de-2.pdf", "de")
        );

        FormularVL parsed = parser.parseFormular(formular);

        assertUris(
            parsed,
            Locale.GERMAN,
            "https://example.org/form-de-1.pdf",
            "https://example.org/form-de-2.pdf"
        );
    }

    @Test
    void parseFormularUsesLanguageIndependentXzufi23FileLinkForEveryFormularLanguage() {
        // Fall 3: Formular A in de,en; ein Dateilink in de,en
        LanguageAwareFoParser languageParser = mock(LanguageAwareFoParser.class);
        when(languageParser.parseFormular(any(), any())).thenReturn(
            contentByLocale(Locale.GERMAN, Locale.ENGLISH)
        );

        XZuFiFoParserService parser = new XZuFiFoParserService(
            languageParser,
            new LanguageFoCompleteCheckService()
        );
        Formular formular = formular(
            fileLink("application/pdf", "https://example.org/form.pdf", null)
        );

        FormularVL parsed = parser.parseFormular(formular);

        assertUris(parsed, Locale.GERMAN, "https://example.org/form.pdf");
        assertUris(parsed, Locale.ENGLISH, "https://example.org/form.pdf");
    }

    @Test
    void parseFormularAssignsXzufi23FileLinksToMatchingLanguages() {
        // Fall 4: Formular A in de,en; Link1 in de, Link2 in en
        LanguageAwareFoParser languageParser = mock(LanguageAwareFoParser.class);
        when(languageParser.parseFormular(any(), any())).thenReturn(
            contentByLocale(Locale.GERMAN, Locale.ENGLISH)
        );

        XZuFiFoParserService parser = new XZuFiFoParserService(
            languageParser,
            new LanguageFoCompleteCheckService()
        );
        Formular formular = formular(
            fileLink("application/pdf", "https://example.org/form-de.pdf", "de"),
            fileLink("application/pdf", "https://example.org/form-en.pdf", "en")
        );

        FormularVL parsed = parser.parseFormular(formular);

        assertUris(parsed, Locale.GERMAN, "https://example.org/form-de.pdf");
        assertUris(parsed, Locale.ENGLISH, "https://example.org/form-en.pdf");
    }

    @Test
    void parseFormularDropsEnglishVariantIfOnlyGermanFileLinkExists() {
        // Fall 5: Formular A in de,en; Link1 in de
        LanguageAwareFoParser languageParser = mock(LanguageAwareFoParser.class);
        when(languageParser.parseFormular(any(), any())).thenReturn(
            contentByLocale(Locale.GERMAN, Locale.ENGLISH)
        );

        XZuFiFoParserService parser = new XZuFiFoParserService(
            languageParser,
            new LanguageFoCompleteCheckService()
        );
        Formular formular = formular(
            fileLink("application/pdf", "https://example.org/form-de.pdf", "de")
        );

        FormularVL parsed = parser.parseFormular(formular);

        assertUris(parsed, Locale.GERMAN, "https://example.org/form-de.pdf");
        assertThat(parsed.copyByLanguage(Locale.ENGLISH).isEmpty(), is(true));
    }

    @Test
    void parseFormularKeepsOnlyOwnLanguageForSeparateGermanAndEnglishFormularObjects() {
        // Fall 6: Formular A in de, Link1 in de; Formular B in en, Link2 in en
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
            formular(fileLink("application/pdf", "https://example.org/form-de.pdf", "de"))
        );
        FormularVL englishFormular = englishParser.parseFormular(
            formular(fileLink("application/pdf", "https://example.org/form-en.pdf", "en"))
        );

        assertUris(germanFormular, Locale.GERMAN, "https://example.org/form-de.pdf");
        assertThat(germanFormular.copyByLanguage(Locale.ENGLISH).isEmpty(), is(true));
        assertUris(englishFormular, Locale.ENGLISH, "https://example.org/form-en.pdf");
        assertThat(englishFormular.copyByLanguage(Locale.GERMAN).isEmpty(), is(true));
    }

    @Test
    void parseFormularDropsLanguageVariantWithoutMatchingFileLink() {
        // Fall 7: Formular A in de; Link nur en
        LanguageAwareFoParser languageParser = mock(LanguageAwareFoParser.class);
        when(languageParser.parseFormular(any(), any())).thenReturn(contentByLocale(Locale.GERMAN));

        XZuFiFoParserService parser = new XZuFiFoParserService(
            languageParser,
            new LanguageFoCompleteCheckService()
        );
        Formular formular = formular(
            fileLink("application/pdf", "https://example.org/form-en.pdf", "en")
        );

        FormularVL parsed = parser.parseFormular(formular);

        assertThat(parsed.getLanguages(), is(empty()));
        assertThat(parsed.copyByLanguage(Locale.GERMAN).isEmpty(), is(true));
    }

    @Test
    void parseFormularKeepsOnlyMatchingFileLinkIfFormularHasGermanAndEnglishFileLinks() {
        // Fall 7: Formular A in de; Link in de,en
        LanguageAwareFoParser languageParser = mock(LanguageAwareFoParser.class);
        when(languageParser.parseFormular(any(), any())).thenReturn(contentByLocale(Locale.GERMAN));

        XZuFiFoParserService parser = new XZuFiFoParserService(
            languageParser,
            new LanguageFoCompleteCheckService()
        );
        Formular formular = formular(
            fileLink("application/pdf", "https://example.org/form-de.pdf", "de"),
            fileLink("application/pdf", "https://example.org/form-en.pdf", "en")
        );

        FormularVL parsed = parser.parseFormular(formular);

        assertUris(parsed, Locale.GERMAN, "https://example.org/form-de.pdf");
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

    private static FormulardateiLink fileLink(String mimeCode, String uri, String languageCode) {
        HyperlinkErweitert link = new HyperlinkErweitert();
        link.setUri(uri);
        link.setLanguageCode(languageCode);

        FormulardateiLink fileLink = new FormulardateiLink();
        fileLink.setMimeCode(mimeCode);
        fileLink.setLink(link);
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
