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
package de.dataport.suchdienst.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class LocaleUtilsTest {

    @Test
    void testHappyPath() {
        assertEquals(Locale.of("de"), LocaleUtils.getSupportedLocale("de"));
        assertEquals(Locale.of("de"), LocaleUtils.getSupportedLocale("de,en-US;q=0.7,en;q=0.3"));
        assertEquals(
            Locale.of("en"),
            LocaleUtils.getSupportedLocale("de;q=0.5,en-US;q=0.7,en;q=0.3")
        );
    }

    @Test
    void testErrorPathDefaultFallback() {
        assertThrows(IllegalArgumentException.class, () ->
            LocaleUtils.getSupportedLocale("fr,en-US;q=0.7;q=0.5,en;q=0.3")
        );
    }

    @Test
    void testExceptionPathDefaultFallback() {
        assertThrows(IllegalArgumentException.class, () ->
            LocaleUtils.getSupportedLocale("ganz sicher keine gültige locale")
        );
    }
}
