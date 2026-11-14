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
package de.dataport.suchdienst.indexer.parser.x22.locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LanguageAwareSearchValuesAndSuggestionParserTest {

    @Test
    void testRemoveAllHtml() {
        String content = "Hallo<div>1</div><strong>2</strong><br><br><br />3";
        assertEquals(
            "Hallo123",
            LanguageAwareSearchValuesAndSuggestionParser.removeAllHtml(content)
        );
    }
}
