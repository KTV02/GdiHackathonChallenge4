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
package de.dataport.suchdienst.indexer.service.languagecheck;

import static org.junit.jupiter.api.Assertions.assertFalse;

import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceBlockSpecialization;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SpezialisierungContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SpezialisierungVL;
import de.dataport.suchdienst.util.ServiceModuleCode;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LanguageCompleteSpTest {

    private LanguageSpCompleteCheckService completeCheckService;

    @BeforeEach
    void setUp() {
        completeCheckService = new LanguageSpCompleteCheckService();
    }

    @Test
    void testMainValues() {
        SpezialisierungVL sp = new SpezialisierungVL();
        sp.getDetails().put(
            ServiceModuleCode.VOLLTEXT.getCode(),
            List.of(
                new ServiceBlockSpecialization(
                    true,
                    ServiceModule.builder().title("title").text("text").build()
                )
            )
        );

        Map<Locale, SpezialisierungContent> otherLanguages = new HashMap<>();
        SpezialisierungContent otherLanguageContent = new SpezialisierungContent();
        otherLanguages.put(Locale.ENGLISH, otherLanguageContent);
        assertFalse(completeCheckService.checkLanguageComplete(otherLanguages, sp, Locale.ENGLISH));
    }
}
