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

import de.dataport.suchdienst.importer.model.GenericLeistung;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GenericLeistungTest implements GenericTestHelper {

    @Test
    void constructorWithXZuFi22InitializesCorrectly() {
        de.xoev.xzufi.v2_2_0.Leistung leistung22 = mock(de.xoev.xzufi.v2_2_0.Leistung.class);
        when(leistung22.getId()).thenReturn(getId22());
        GenericLeistung genericLeistung = new GenericLeistung(leistung22);

        assertTrue(genericLeistung.isXZuFi22());
        assertFalse(genericLeistung.isXZuFi23());
        assertNotNull(genericLeistung.getId());
        assertTrue(genericLeistung.getId().contains(".LB."));
    }

    @Test
    void constructorWithXZuFi23InitializesCorrectly() {
        de.xoev.xzufi.v2_3_1.Leistung leistung23 = mock(de.xoev.xzufi.v2_3_1.Leistung.class);
        when(leistung23.getId()).thenReturn(getId23());
        GenericLeistung genericLeistung = new GenericLeistung(leistung23);

        assertTrue(genericLeistung.isXZuFi23());
        assertFalse(genericLeistung.isXZuFi22());
        assertNotNull(genericLeistung.getId());
        assertTrue(genericLeistung.getId().contains(".LB."));
    }

    @Test
    void extractArsFederalStateReturnsCorrectSetForXZuFi22() {
        de.xoev.xzufi.v2_2_0.Leistung leistung22 = mock(de.xoev.xzufi.v2_2_0.Leistung.class);
        when(leistung22.getId()).thenReturn(getId22());
        var identifikator = mock(de.xoev.xzufi.v2_2_0.Identifikator.class);
        when(identifikator.getValue()).thenReturn("DE123");
        when(leistung22.getGueltigkeitGebietID()).thenReturn(List.of(identifikator, identifikator));

        GenericLeistung genericLeistung = new GenericLeistung(leistung22);
        Set<String> result = genericLeistung.getArsFederalState();

        assertEquals(Set.of("DE"), result);
    }

    @Test
    void extractArsFederalStateReturnsCorrectSetForXZuFi23() {
        de.xoev.xzufi.v2_3_1.Leistung leistung23 = mock(de.xoev.xzufi.v2_3_1.Leistung.class);
        when(leistung23.getId()).thenReturn(getId23());
        var identifikator = mock(de.xoev.xzufi.v2_3_1.Identifikator.class);
        when(identifikator.getValue()).thenReturn("DE123");
        when(leistung23.getGueltigkeitGebietID()).thenReturn(List.of(identifikator, identifikator));

        GenericLeistung genericLeistung = new GenericLeistung(leistung23);
        Set<String> result = genericLeistung.getArsFederalState();

        assertEquals(Set.of("DE"), result);
    }
}
