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
package de.dataport.suchdienst.indexer.model.generic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.dataport.suchdienst.importer.model.GenericLeistungSpezialisierung;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GenericLeistungSpezialisierungTest implements GenericTestHelper {

    @Test
    void constructorWithXZuFi22InitializesCorrectly() {
        de.xoev.xzufi.v2_2_0.LeistungSpezialisierung spezialisierung22 = mock(
            de.xoev.xzufi.v2_2_0.LeistungSpezialisierung.class
        );
        when(spezialisierung22.getId()).thenReturn(getId22());
        when(spezialisierung22.getIdLeistung()).thenReturn(getId22());
        GenericLeistungSpezialisierung genericSpezialisierung = new GenericLeistungSpezialisierung(
            spezialisierung22
        );

        assertTrue(genericSpezialisierung.isXZuFi22());
        assertFalse(genericSpezialisierung.isXZuFi23());
        assertNotNull(genericSpezialisierung.getId());
        assertTrue(genericSpezialisierung.getId().contains(".SP."));
        assertNotNull(genericSpezialisierung.getLbId());
        assertNotNull(genericSpezialisierung.getArsFederalState());
    }

    @Test
    void constructorWithXZuFi23InitializesCorrectly() {
        de.xoev.xzufi.v2_3_1.LeistungSpezialisierung spezialisierung23 = mock(
            de.xoev.xzufi.v2_3_1.LeistungSpezialisierung.class
        );
        when(spezialisierung23.getId()).thenReturn(getId23());
        when(spezialisierung23.getLeistungID()).thenReturn(getId23());
        GenericLeistungSpezialisierung genericSpezialisierung = new GenericLeistungSpezialisierung(
            spezialisierung23
        );

        assertTrue(genericSpezialisierung.isXZuFi23());
        assertFalse(genericSpezialisierung.isXZuFi22());
        assertNotNull(genericSpezialisierung.getId());
        assertTrue(genericSpezialisierung.getId().contains(".SP."));
        assertNotNull(genericSpezialisierung.getLbId());
        assertNotNull(genericSpezialisierung.getArsFederalState());
    }

    @Test
    void extractArsFederalStateReturnsCorrectSetForXZuFi22() {
        de.xoev.xzufi.v2_2_0.LeistungSpezialisierung spezialisierung22 = mock(
            de.xoev.xzufi.v2_2_0.LeistungSpezialisierung.class
        );
        when(spezialisierung22.getId()).thenReturn(getId22());
        var identifikator = mock(de.xoev.xzufi.v2_2_0.Identifikator.class);
        when(spezialisierung22.getIdLeistung()).thenReturn(getId22());
        when(identifikator.getValue()).thenReturn("DE123");
        when(spezialisierung22.getIdGebiet()).thenReturn(List.of(identifikator, identifikator));

        GenericLeistungSpezialisierung genericSpezialisierung = new GenericLeistungSpezialisierung(
            spezialisierung22
        );
        Set<String> result = genericSpezialisierung.getArsFederalState();

        assertEquals(Set.of("DE"), result);
    }

    @Test
    void extractArsFederalStateReturnsCorrectSetForXZuFi23() {
        de.xoev.xzufi.v2_3_1.LeistungSpezialisierung spezialisierung23 = mock(
            de.xoev.xzufi.v2_3_1.LeistungSpezialisierung.class
        );
        when(spezialisierung23.getId()).thenReturn(getId23());
        var identifikator = mock(de.xoev.xzufi.v2_3_1.Identifikator.class);
        when(spezialisierung23.getLeistungID()).thenReturn(getId23());
        when(identifikator.getValue()).thenReturn("DE123");
        when(spezialisierung23.getGebietID()).thenReturn(List.of(identifikator, identifikator));

        GenericLeistungSpezialisierung genericSpezialisierung = new GenericLeistungSpezialisierung(
            spezialisierung23
        );
        Set<String> result = genericSpezialisierung.getArsFederalState();

        assertEquals(Set.of("DE"), result);
    }
}
