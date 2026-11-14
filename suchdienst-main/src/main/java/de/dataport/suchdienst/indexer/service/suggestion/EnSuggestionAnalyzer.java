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

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EnSuggestionAnalyzer extends AbstractSuggestionAnalyzer {

    public EnSuggestionAnalyzer() {
        super(EnglishAnalyzer.getDefaultStopSet());
    }
}
