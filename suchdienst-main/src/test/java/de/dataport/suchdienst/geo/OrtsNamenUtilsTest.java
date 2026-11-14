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
package de.dataport.suchdienst.geo;

import static de.dataport.suchdienst.geo.util.OrtsNamenUtils.extractOrt;
import static de.dataport.suchdienst.geo.util.OrtsNamenUtils.extractOrtsteil;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class OrtsNamenUtilsTest {

    @Test
    void testExtractOrt() {
        Function<String, String> e = (String ort) -> extractOrt(ort).orElseThrow();

        assertEquals("Kiel", e.apply("Kiel 'Wik'"));
        assertEquals("Kiel", e.apply("Kiel ''"));
        assertEquals("Kiel", e.apply("Kiel"));
        assertEquals("Kiel 'Wik", e.apply("Kiel 'Wik"));

        assertEquals("Bad Schandau", e.apply("Stadt Bad Schandau"));
        assertEquals("Hamburg", e.apply("Hamburg, Freie und Hansestadt"));

        assertThrows(NoSuchElementException.class, () -> e.apply(""));
        assertThrows(NoSuchElementException.class, () -> e.apply(null));
    }

    @Test
    void testExtractOrtWithGemeinde() {
        Function<String, String> e = (String ort) -> extractOrt(ort).orElseThrow();

        assertEquals("Kiel", e.apply("Kiel, Landeshauptstadt"));
        assertEquals("Kiel", e.apply("Kiel, Landeshauptstadt 'Wik'"));
        assertEquals("Kiel", e.apply("Kiel, Landeshauptstadt, 'Wik'"));
        assertEquals("Kiel", e.apply("Kiel 'Wik', Landeshauptstadt"));
        assertEquals("Kiel", e.apply("Kiel, 'Wik', Landeshauptstadt"));
        assertEquals("Kiel", e.apply("Kiel '', Landeshauptstadt"));
    }

    @Test
    void testExtractOrtsteil() {
        Function<String, String> e = (String ort) -> extractOrtsteil(ort).orElseThrow();

        assertEquals("Wik", e.apply("Kiel 'Wik'"));
        assertEquals("", e.apply("Kiel ''"));
        assertThrows(NoSuchElementException.class, () -> e.apply("Kiel"));
        assertThrows(NoSuchElementException.class, () -> e.apply("Kiel 'Wik"));
        assertThrows(NoSuchElementException.class, () -> e.apply(""));
        assertThrows(NoSuchElementException.class, () -> e.apply(null));
    }
}
