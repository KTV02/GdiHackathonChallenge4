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
package de.dataport.suchdienst.geo.util;

import java.util.Arrays;
import java.util.Optional;

public final class OrtsNamenUtils {

    private OrtsNamenUtils() {
        // Utility class
    }

    public static Optional<String> extractOrt(String input) {
        if (input == null) {
            return Optional.empty();
        }

        var withoutOrtsteil = removeOrtsteilString(input);
        var ort = removeZusaetzeString(withoutOrtsteil);

        if (ort == null || ort.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(ort);
    }

    public static Optional<String> extractOrtsteil(String input) {
        if (input == null) {
            return Optional.empty();
        }
        var ortsteil = extractBetweenSingleQuotesString(input);
        if (ortsteil == null) {
            return Optional.empty();
        }
        return Optional.of(ortsteil);
    }

    private static String removeOrtsteilString(String input) {
        var ortsteil = extractBetweenSingleQuotesString(input);
        if (ortsteil == null) {
            return input;
        }
        return input.replace("'" + ortsteil + "'", "").trim();
    }

    private static String extractBetweenSingleQuotesString(String input) {
        int startIndex = input.indexOf('\'');
        if (startIndex == -1) {
            return null;
        }
        int endIndex = input.indexOf('\'', startIndex + 1);
        if (endIndex == -1) {
            return null;
        }
        return input.substring(startIndex + 1, endIndex);
    }

    private static String removeZusaetzeString(String input) {
        // naives Entfernen von Zusätzen in Ortsnamen
        String result = Arrays.stream(input.split(",")).findFirst().map(String::trim).orElse(input);
        return result.replaceAll("\\b(Stadt|Gemeinde)\\b", "").trim();
    }
}
