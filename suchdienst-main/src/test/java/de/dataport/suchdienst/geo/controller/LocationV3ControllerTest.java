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
package de.dataport.suchdienst.geo.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LocationV3ControllerTest {

    private final LocationV3Controller locationV3Controller = new LocationV3Controller(null);

    @Test
    void validateFirstCharacterArray() {
        assertFalse(locationV3Controller.hasSpecialCharacterAtFirstLetter("Hund", "Katze", "Maus"));
        assertTrue(locationV3Controller.hasSpecialCharacterAtFirstLetter(".Hund", "Katze", "Maus"));
        assertTrue(locationV3Controller.hasSpecialCharacterAtFirstLetter("Hund", ".Katze", "Maus"));
        assertTrue(locationV3Controller.hasSpecialCharacterAtFirstLetter("Hund", "Katze", ".Maus"));
    }

    @ParameterizedTest
    @ValueSource(strings = { ".", ",", "-", "'", "€" })
    void validateFirstCharacterDetectDisallowedCharacter(String query) {
        assertTrue(
            locationV3Controller.hasSpecialCharacterAtFirstLetter(query),
            "Sonderzeichen sollen an erster Stelle nicht erlaubt sein!"
        );
    }

    @ParameterizedTest
    @ValueSource(strings = { "a", "z", "A", "Z", "Â", "é" })
    void validateFirstCharacterAllowWords(String query) {
        assertFalse(
            locationV3Controller.hasSpecialCharacterAtFirstLetter(query),
            "Buchstaben müssen an erster Stelle erlaubt sein!"
        );
    }

    @ParameterizedTest
    @ValueSource(strings = { "0", "9", "01", "09" })
    void validateFirstCharacterAllowNumbers(String query) {
        assertFalse(
            locationV3Controller.hasSpecialCharacterAtFirstLetter(query),
            "Zahlen müssen an erster Stelle erlaubt sein!"
        );
    }

    @ParameterizedTest
    @ValueSource(strings = { "a.", "a,", "a-", "a'", "a€" })
    void validateFirstCharacterAllowSpecialCharactersAfterAllowedCharacter(String query) {
        assertFalse(
            locationV3Controller.hasSpecialCharacterAtFirstLetter(query),
            "Sonderzeichen sollen ab der zweiten Stelle erlaubt sein!"
        );
    }
}
