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

import de.dataport.suchdienst.importer.model.GenericOnlinedienst;
import de.xoev.xzufi.v2_3_1.OnlinedienstZahlungsweise;
import org.junit.jupiter.api.Test;

class GenericOnlinedienstTest implements GenericTestHelper {

    @Test
    void constructorWithXZuFi22InitializesCorrectly() {
        de.xoev.xzufi.v2_2_0.OnlinedienstErweitert onlinedienst22 = mock(
            de.xoev.xzufi.v2_2_0.OnlinedienstErweitert.class
        );
        when(onlinedienst22.getId()).thenReturn(getId22());
        GenericOnlinedienst genericOnlinedienst = new GenericOnlinedienst(onlinedienst22);

        assertNotNull(genericOnlinedienst.getId());
        assertTrue(genericOnlinedienst.getId().contains(".OD."));
        assertTrue(genericOnlinedienst.isOeffentlichAnzeigen());
    }

    @Test
    void constructorWithXZuFi23InitializesCorrectly() {
        de.xoev.xzufi.v2_3_1.OnlinedienstErweitert onlinedienst23 = mock(
            de.xoev.xzufi.v2_3_1.OnlinedienstErweitert.class
        );
        when(onlinedienst23.getId()).thenReturn(getId23());
        GenericOnlinedienst genericOnlinedienst = new GenericOnlinedienst(onlinedienst23);

        assertNotNull(genericOnlinedienst.getId());
        assertTrue(genericOnlinedienst.getId().contains(".OD."));
    }

    @Test
    void xZuFi23UsesKennzeichenOeffentlichAnzeigen() {
        de.xoev.xzufi.v2_3_1.OnlinedienstErweitert onlinedienst23 = mock(
            de.xoev.xzufi.v2_3_1.OnlinedienstErweitert.class
        );
        when(onlinedienst23.getId()).thenReturn(getId23());
        when(onlinedienst23.isKennzeichenOeffentlichAnzeigen()).thenReturn(false);

        GenericOnlinedienst genericOnlinedienst = new GenericOnlinedienst(onlinedienst23);

        assertFalse(genericOnlinedienst.isOeffentlichAnzeigen());
    }

    @Test
    void getOrConvertXZuFi22ReturnsCorrectObject() {
        de.xoev.xzufi.v2_2_0.OnlinedienstErweitert onlinedienst22 = mock(
            de.xoev.xzufi.v2_2_0.OnlinedienstErweitert.class
        );
        when(onlinedienst22.getId()).thenReturn(getId22());
        GenericOnlinedienst genericOnlinedienst = new GenericOnlinedienst(onlinedienst22);

        var od23 = genericOnlinedienst.getOrConvertXZuFi22();
        assertNotNull(od23);
        assertEquals("testSchemeAgencyID", od23.getId().getSchemeAgencyID());
    }

    @Test
    void getOrConvertXZuFi23ReturnsCorrectObject() {
        de.xoev.xzufi.v2_3_1.OnlinedienstErweitert onlinedienst23 = mock(
            de.xoev.xzufi.v2_3_1.OnlinedienstErweitert.class
        );
        when(onlinedienst23.getId()).thenReturn(getId23());
        when(onlinedienst23.getZahlungsweisen()).thenReturn(new OnlinedienstZahlungsweise());
        GenericOnlinedienst genericOnlinedienst = new GenericOnlinedienst(onlinedienst23);

        var od23 = genericOnlinedienst.getOrConvertXZuFi22();
        assertNotNull(od23);
        assertEquals("testSchemeAgencyID", od23.getId().getSchemeAgencyID());
    }
}
