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
package de.dataport.suchdienst.indexer.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class CodelistEntryTest {

    private static final String TEXT = "foo";

    @Test
    void testAskForEnGivesEnGB() {
        CodelistEntry entry = new CodelistEntry();
        entry.setText(Locale.UK, TEXT);

        assertThat(entry.getText(Locale.ENGLISH), is(TEXT));
    }

    /**
     * PV-9538.
     */
    @Test
    void testAskForEnUSGivesEnGB() {
        CodelistEntry entry = new CodelistEntry();
        entry.setText(Locale.UK, TEXT);

        assertThat(entry.getText(Locale.US), is(TEXT));
    }

    @Test
    void testAskForEnGBGivesEn() {
        CodelistEntry entry = new CodelistEntry();
        entry.setText(Locale.ENGLISH, TEXT);

        assertThat(entry.getText(Locale.UK), is(TEXT));
    }

    @Test
    void testCommentAskForEnGivesEnGB() {
        CodelistEntry entry = new CodelistEntry();
        entry.setComment(Locale.GERMAN, "dummy-de");
        entry.setComment(Locale.UK, TEXT);

        assertThat(entry.getComment(Locale.ENGLISH), is(TEXT));
    }

    @Test
    void testCommentAskForEnGBGivesEn() {
        CodelistEntry entry = new CodelistEntry();
        entry.setComment(Locale.GERMAN, "dummy-de");
        entry.setComment(Locale.UK, TEXT);

        assertThat(entry.getComment(Locale.UK), is(TEXT));
    }

    @Test
    void testCommentAskForEnGBGivesEnGB() {
        CodelistEntry entry = new CodelistEntry();
        entry.setComment(Locale.GERMAN, "dummy-de");
        entry.setComment(Locale.UK, TEXT);
        entry.setComment(Locale.US, "dummy-us");

        assertThat(entry.getComment(Locale.UK), is(TEXT));
    }

    /**
     * Analog zu {@link #testAskForFrGivesDe()} ist Deutsch der Default.
     * Ich finde das gar nicht sinnvoll. Ich würde für Englisch eine höherer Endnutzerzufriedenheit erwarten.
     */
    @Test
    void testCommentAskForJaGivesDe() {
        CodelistEntry entry = new CodelistEntry();
        entry.setComment(Locale.GERMAN, TEXT);
        entry.setComment(Locale.ENGLISH, "dummy-en");

        assertThat(entry.getComment(Locale.JAPANESE), is(TEXT));
    }

    @Test
    void testCommentEmptyNull() {
        CodelistEntry entry = new CodelistEntry();

        assertThat(entry.getComment(Locale.GERMAN), nullValue());
    }

    @Test
    void testAskForDeGivesDe() {
        CodelistEntry entry = new CodelistEntry();
        entry.setText(Locale.GERMAN, TEXT);

        assertThat(entry.getText(Locale.GERMAN), is(TEXT));
    }

    @Test
    void testAskForDeDeGivesDe() {
        CodelistEntry entry = new CodelistEntry();
        entry.setText(Locale.GERMAN, TEXT);

        assertThat(entry.getText(Locale.GERMANY), is(TEXT));
    }

    @Test
    void testCommentAskForDeCHGivesDe() {
        CodelistEntry entry = new CodelistEntry();
        entry.setComment(Locale.UK, "dummy-uk");
        entry.setComment(Locale.GERMAN, TEXT);

        assertThat(entry.getComment(Locale.forLanguageTag("de-CH")), is(TEXT));
    }

    @Test
    void testAskForDeGivesDeDe() {
        CodelistEntry entry = new CodelistEntry();
        entry.setText(Locale.GERMANY, TEXT);

        assertThat(entry.getText(Locale.GERMAN), is(TEXT));
    }

    /**
     * Vgl {@link #testCommentAskForJaGivesDe()}. Das derzeitige Verhalten erscheint mir nicht sinnvoll.
     */
    @Test
    void testAskForFrGivesDe() {
        CodelistEntry entry = new CodelistEntry();
        entry.setText(Locale.GERMAN, TEXT);
        entry.setText(Locale.ENGLISH, "dontFindMe" + TEXT);

        assertThat(entry.getText(Locale.FRENCH), is(TEXT));
    }
}
