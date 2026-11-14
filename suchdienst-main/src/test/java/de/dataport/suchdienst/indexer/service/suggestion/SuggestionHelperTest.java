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
package de.dataport.suchdienst.indexer.service.suggestion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SuggestionHelperTest {

    private final DeSuggestionAnalyzer analyzer = new DeSuggestionAnalyzer();

    @Test
    void testGenerateSuggestions() {
        Set<String> result = SuggestionHelper.extractTokens(
            "Hartz IV Hartz 4 Hartz3 ALG(2) ab 18 café àlles irgendwás öffentlich wägä",
            analyzer
        );

        assertAll(() ->
            assertThat(
                result,
                containsInAnyOrder(
                    "hartz IV",
                    "hartz 4",
                    "ALG 2",
                    "hartz 3",
                    "café",
                    "àlles",
                    "öffentlich",
                    "wägä",
                    "irgendwás"
                )
            )
        );
    }

    @Test
    void applySynonyms() {
        assertThat(
            SuggestionHelper.extractTokens("Illusionen", analyzer).iterator().next(),
            is("illusion")
        );
        assertThat(
            SuggestionHelper.extractTokens("illusionen", analyzer).iterator().next(),
            is("illusion")
        );
    }

    @Test
    void testRomanNumeralAtBeginning() {
        assertThat(
            SuggestionHelper.extractTokens("XRechnung", analyzer).iterator().next(),
            is("XRechnung")
        );
        assertThat(
            SuggestionHelper.extractTokens("XRechnung", analyzer).iterator().next(),
            is("XRechnung")
        );
        assertThat(
            SuggestionHelper.extractTokens("X-Rechnung", analyzer).iterator().next(),
            is("X-Rechnung")
        );
        assertThat(
            SuggestionHelper.extractTokens("X Rechnung", analyzer).iterator().next(),
            is("rechnung")
        );
        assertThat(
            SuggestionHelper.extractTokens("X.Rechnung", analyzer).iterator().next(),
            is("rechnung")
        );
    }

    @Test
    void testRomanNumeralWithBindestrich() {
        assertThat(
            SuggestionHelper.extractTokens("X-Rechnung", analyzer).iterator().next(),
            is("X-Rechnung")
        );
        assertThat(
            SuggestionHelper.extractTokens("Space-X", analyzer).iterator().next(),
            is("Space-X")
        );
        assertThat(
            SuggestionHelper.extractTokens("X-Ray", analyzer).iterator().next(),
            is("X-Ray")
        );
    }

    @Test
    void testNumberWithoutWhitespaceExtractTokens() {
        // Zahlen vor Text werden entfernt
        // Zahlen nach Text (ohne Leerzeichen) werden um Leerzeichen ergänzt
        // Zahlen vor Text (ohne Leerzeichen) werden nicht angepasst
        // Zahlen innerhalb von Wörtern werden nicht verändert
        assertThat(
            SuggestionHelper.extractTokens("2 Hartz4 macht 2Probleme", analyzer),
            containsInAnyOrder("hartz 4", "macht", "2Probleme")
        );
        assertThat(
            SuggestionHelper.extractTokens("bureaucracy4you", analyzer),
            containsInAnyOrder("bureaucracy4you")
        );
    }

    @Test
    void testNumberWithoutWhitespaceExtractTokensAddSpaceBetweenWordAndNumber() {
        assertThat(SuggestionHelper.addSpaceBetweenWordAndNumber("alg1"), is("alg 1"));
        assertThat(SuggestionHelper.addSpaceBetweenWordAndNumber("alg12"), is("alg 12"));
        assertThat(SuggestionHelper.addSpaceBetweenWordAndNumber("4leute"), is("4leute"));
        assertThat(SuggestionHelper.addSpaceBetweenWordAndNumber("44leute"), is("44leute"));
        assertThat(
            SuggestionHelper.addSpaceBetweenWordAndNumber("bureaucracy4you"),
            is("bureaucracy4you")
        );
        assertThat(
            SuggestionHelper.addSpaceBetweenWordAndNumber("bureaucracy44you"),
            is("bureaucracy44you")
        );
    }

    @Test
    void testRomanNumeralWithoutWhitespace() {
        assertThat(
            SuggestionHelper.extractTokens("HartzV", analyzer).iterator().next(),
            is("HartzV")
        );
    }
}
