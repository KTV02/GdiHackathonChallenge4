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
package de.dataport.suchdienst.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RequestUtilsTest {

    @ParameterizedTest(name = "sanitizeUserInput: {2}")
    @MethodSource("sanitizeUserInputProvider")
    void sanitizeUserInput(String input, String expected, String msg) {
        assertEquals(expected, RequestUtils.sanitizeUserInput(input));
    }

    private static Stream<Arguments> sanitizeUserInputProvider() {
        return Stream.of(
            Arguments.of(null, null, "null bleibt unverändert"),
            Arguments.of("", "", "leer bleibt unverändert"),
            Arguments.of(
                "abc \t123",
                "abc \t123",
                "alphanumerisch, Ziffern und whitespace bleiben unverändert"
            ),
            Arguments.of(
                "äöüÄÖÜß",
                "äöüÄÖÜß",
                "Umlaute und ß sind Buchstaben und bleiben unverändert"
            ),
            Arguments.of("🙂", "*", "emoji werden durch * ersetzt"),
            Arguments.of("!@#", "***", "Sonderzeichen werden ersetzt")
        );
    }
}
