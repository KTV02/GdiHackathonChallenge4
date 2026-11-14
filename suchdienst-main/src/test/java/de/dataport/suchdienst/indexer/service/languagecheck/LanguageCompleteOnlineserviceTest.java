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
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dataport.suchdienst.indexer.model.mapping.Link;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstVL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LanguageCompleteOnlineserviceTest {

    private LanguageOdCompleteCheckService completeCheckService;

    @BeforeEach
    void setUp() {
        completeCheckService = new LanguageOdCompleteCheckService();
    }

    @Test
    void testMainValues() {
        OnlinedienstVL od = new OnlinedienstVL();
        OnlinedienstContent content = new OnlinedienstContent();
        Map<Locale, OnlinedienstContent> map = new HashMap<>();
        map.put(Locale.ENGLISH, content);

        od.setName("name");
        assertFalse(completeCheckService.checkLanguageComplete(map, od, Locale.ENGLISH));

        content.setName("englishName");
        assertTrue(completeCheckService.checkLanguageComplete(map, od, Locale.ENGLISH));

        od.setDescription("description");
        assertFalse(completeCheckService.checkLanguageComplete(map, od, Locale.ENGLISH));

        content.setDescription("englishDescription");
        assertTrue(completeCheckService.checkLanguageComplete(map, od, Locale.ENGLISH));

        od.setHelpText("helptext");
        assertFalse(completeCheckService.checkLanguageComplete(map, od, Locale.ENGLISH));

        content.setHelpText("englishhelptext");
        assertTrue(completeCheckService.checkLanguageComplete(map, od, Locale.ENGLISH));

        od.getHelpLinks().add(new Link("uri", "title", "text"));
        assertFalse(completeCheckService.checkLanguageComplete(map, od, Locale.ENGLISH));

        content.getHelpLinks().add(Link.of(null, null, null));
        assertTrue(completeCheckService.checkLanguageComplete(map, od, Locale.ENGLISH));
    }
}
