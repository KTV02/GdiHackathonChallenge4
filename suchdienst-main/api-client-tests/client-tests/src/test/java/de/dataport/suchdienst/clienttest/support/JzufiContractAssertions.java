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
package de.dataport.suchdienst.clienttest.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import tools.jackson.databind.JsonNode;

/** Strukturvergleich nach dem HTTP-Binding; ersetzt nicht den generierten DTO-Konverter. */
public final class JzufiContractAssertions {

    private JzufiContractAssertions() {}

    public static JsonNode tree(Object boundDto) {
        return StrictApiClientFactory.mapper().valueToTree(boundDto);
    }

    /** Generierte Standardfelder sind erlaubt; gesendete Werte, Arraylaengen und Reihenfolgen bleiben verbindlich. */
    public static void assertContains(JsonNode expected, JsonNode actual) {
        var mapper = StrictApiClientFactory.mapper();
        // valueToTree behaelt BinaryNode/LongNode; auf HTTP-Ebene sind dies Base64-Strings bzw. JSON-Zahlen.
        assertContains(
            mapper.readTree(mapper.writeValueAsString(expected)),
            mapper.readTree(mapper.writeValueAsString(actual)),
            "$"
        );
    }

    private static void assertContains(JsonNode expected, JsonNode actual, String path) {
        if (expected.isObject()) {
            assertTrue(actual.isObject(), path + ": Objekt erwartet");
            expected.forEachEntry((name, value) -> assertContains(value, actual.path(name), path + "." + name));
        } else if (expected.isArray()) {
            assertTrue(actual.isArray(), path + ": Array erwartet");
            assertEquals(expected.size(), actual.size(), path);
            for (int index = 0; index < expected.size(); index++) {
                assertContains(expected.get(index), actual.path(index), path + "[" + index + "]");
            }
        } else {
            assertEquals(expected, actual, path);
        }
    }

    public static void assertIdentifier(String fullId, JsonNode identifier) {
        var parts = fullId.split("\\.", 3);
        assertEquals(parts[0], identifier.path("schemeAgencyID").asString());
        assertEquals(parts[2], identifier.path("value").asString());
    }

    public static void assertGermanValue(String expected, JsonNode localizedValues) {
        assertTrue(localizedValues.isArray());
        for (var value : localizedValues) {
            if ("de".equals(value.path("languageCode").asString())) {
                assertEquals(expected, value.path("value").asString());
                return;
            }
        }
        throw new AssertionError("Deutscher Wert fehlt: " + expected);
    }
}
