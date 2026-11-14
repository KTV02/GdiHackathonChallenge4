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

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class LanguageDeterminationComponent {

    private final DeSuggestionAnalyzer germanAnalyzer;
    private final EnSuggestionAnalyzer englishAnalyzer;

    public LanguageDeterminationComponent(
        DeSuggestionAnalyzer germanAnalyzer,
        EnSuggestionAnalyzer englishAnalyzer
    ) {
        this.germanAnalyzer = germanAnalyzer;
        this.englishAnalyzer = englishAnalyzer;
    }

    public AbstractSuggestionAnalyzer determineAnalyzer(Locale locale) {
        return Locale.GERMAN.getLanguage().equals(locale.getLanguage())
            ? germanAnalyzer
            : englishAnalyzer;
    }
}
