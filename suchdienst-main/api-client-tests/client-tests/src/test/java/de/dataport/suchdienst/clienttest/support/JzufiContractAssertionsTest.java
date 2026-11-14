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

import static de.dataport.suchdienst.clienttest.support.JzufiContractAssertions.assertContains;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;

/**
 * Testziel: Der Teilbaumvergleich erlaubt zusaetzliche DTO-Standardfelder,
 * erkennt aber Feldverluste, andere Werte, Arraylaengen und Reihenfolgen.
 * Fehler nennen den betroffenen JSON-Pfad.
 */
class JzufiContractAssertionsTest {

    private final JsonNode expected = StrictApiClientFactory.mapper().readTree(
        "{\"values\":[{\"code\":\"02\"},{\"code\":\"03\"}]}"
    );

    @Test
    void allowsAdditionalGeneratedFields() {
        assertContains(
            expected,
            StrictApiClientFactory.mapper().readTree(
                "{\"values\":[{\"code\":\"02\",\"default\":[]},{\"code\":\"03\"}],\"optional\":null}"
            )
        );
    }

    @Test
    void comparesBinaryAndIntegerNodesByTheirJsonRepresentation() {
        var mapper = StrictApiClientFactory.mapper();
        assertContains(
            mapper.readTree("{\"binary\":\"AAEC\",\"number\":1}"),
            mapper
                .createObjectNode()
                .put("binary", new byte[] { 0, 1, 2 })
                .put("number", 1L)
        );
    }

    @ParameterizedTest(name = "Vertragsabweichung: {0}")
    @ValueSource(
        strings = {
            "{\"values\":[{\"code\":\"03\"},{\"code\":\"02\"}]}",
            "{\"values\":[{\"code\":\"02\"}]}",
            "{\"values\":\"wrong-shape\"}",
            "{\"values\":[{},{\"code\":\"03\"}]}",
        }
    )
    void rejectsChangedValuesAndShapesWithThePropertyPath(String json) {
        var error = assertThrows(AssertionError.class, () ->
            assertContains(expected, StrictApiClientFactory.mapper().readTree(json))
        );
        assertTrue(error.getMessage().contains("$.values"), error.getMessage());
    }
}
