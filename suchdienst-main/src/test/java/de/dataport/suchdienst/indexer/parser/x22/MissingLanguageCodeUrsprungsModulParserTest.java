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
package de.dataport.suchdienst.indexer.parser.x22;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import de.xoev.xzufi.v2_2_0.Identifikator;
import de.xoev.xzufi.v2_2_0.LeistungSpezialisierung;
import de.xoev.xzufi.v2_2_0.Leistungsmodul;
import de.xoev.xzufi.v2_2_0.LeistungsmodulSpezialisierung;
import de.xoev.xzufi.v2_2_0.Textmodul;
import de.xoev.xzufi.v2_2_0.UrsprungsportalModul;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

class MissingLanguageCodeUrsprungsModulParserTest extends AbstractX22ParserTest {

    public MissingLanguageCodeUrsprungsModulParserTest() {
        super("x22/xzufi-bayern-00002.xml");
    }

    @Test
    void testSpecializationParsingLangNull() {
        LeistungSpezialisierung first = firstSpRaw();

        // Gebiet-Identifikatoren
        List<String> gebiete = first.getIdGebiet().stream().map(Identifikator::getValue).toList();
        assertEquals(List.of("010030000000"), gebiete);

        // Spezialmodule sortiert nach Position
        List<Leistungsmodul> module = first
            .getModulSpezialisiert()
            .stream()
            .map(LeistungsmodulSpezialisierung::getModul)
            .sorted(Comparator.comparing(Leistungsmodul::getPositionDarstellung))
            .toList();

        UrsprungsportalModul m0 = (UrsprungsportalModul) module.getFirst();
        assertEquals(1, m0.getPositionDarstellung());
        assertEquals("Wirtschaftsförderung/-koordination in Lübeck, Hansestadt", m0.getTitel());
        assertNull(m0.getLanguageCode());

        Textmodul m1 = (Textmodul) module.get(1);
        assertEquals(4, m1.getPositionDarstellung());
        Textmodul m2 = (Textmodul) module.get(2);
        assertEquals(5, m2.getPositionDarstellung());
        Textmodul m3 = (Textmodul) module.get(3);
        assertEquals(14, m3.getPositionDarstellung());
    }
}
