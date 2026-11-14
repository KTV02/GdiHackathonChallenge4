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
package de.dataport.suchdienst.indexer.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * Tests für {@link XzufiParserUtils#getSupportedLocale(String)}.
 * Prüft typische Accept-Language Header Varianten:
 *  - Happy Path mit einfacher Locale und gewichteten Einträgen
 *  - Fallback bei nicht unterstützten Sprachen
 *  - Robustes Verhalten bei komplett ungültigem Header.
 */
class XzufiParserUtilsTest {

    @Test
    void testHappyPath() {
        assertEquals(Locale.of("de"), XzufiParserUtils.getSupportedLocale("de"));
        assertEquals(
            Locale.of("de"),
            XzufiParserUtils.getSupportedLocale("de,en-US;q=0.7,en;q=0.3")
        );
        assertEquals(
            Locale.of("en"),
            XzufiParserUtils.getSupportedLocale("de;q=0.5,en-US;q=0.7,en;q=0.3")
        );
    }

    @Test
    void testErrorPathDefaultFallback() {
        // Keine der angegebenen Sprachen wird unterstützt -> Standard Fallback Deutsch
        assertEquals(
            Locale.of("de"),
            XzufiParserUtils.getSupportedLocale("fr,en-US;q=0.7;q=0.5,en;q=0.3")
        );
    }

    @Test
    void testExceptionPathDefaultFallback() {
        // Vollständig ungültiger Header -> Fallback
        assertEquals(
            Locale.of("de"),
            XzufiParserUtils.getSupportedLocale("ganz sicher keine gültige locale")
        );
    }
}
