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

import de.dataport.suchdienst.importer.model.GenericFormular;
import de.dataport.suchdienst.importer.model.GenericModelMapper;
import de.dataport.suchdienst.importer.model.jzufi.FormularEntity22;
import de.dataport.suchdienst.importer.model.jzufi.FormularEntity23;
import de.xoev.xzufi.v2_2_0.Identifikator;
import org.junit.jupiter.api.Test;

class GenericFormularTest implements GenericTestHelper {

    @Test
    void constructorWithXZuFi22InitializesCorrectly() {
        de.xoev.xzufi.v2_2_0.Formular formular22 = mock(de.xoev.xzufi.v2_2_0.Formular.class);
        when(formular22.getId()).thenReturn(getId22());
        GenericFormular genericFormular = new GenericFormular(formular22);

        assertTrue(genericFormular.isXZuFi22());
        assertFalse(genericFormular.isXZuFi23());
        assertNotNull(genericFormular.getId());
        assertTrue(genericFormular.getId().contains(".FO."));
    }

    @Test
    void constructorWithXZuFi23InitializesCorrectly() {
        de.xoev.xzufi.v2_3_1.Formular formular23 = mock(de.xoev.xzufi.v2_3_1.Formular.class);
        when(formular23.getId()).thenReturn(getId23());
        GenericFormular genericFormular = new GenericFormular(formular23);

        assertTrue(genericFormular.isXZuFi23());
        assertFalse(genericFormular.isXZuFi22());
        assertNotNull(genericFormular.getId());
        assertTrue(genericFormular.getId().contains(".FO."));
    }

    @Test
    void fromEntityCreatesGenericFormularFromFormularEntity22() {
        de.xoev.xzufi.v2_2_0.Formular jzufi22 = mock(de.xoev.xzufi.v2_2_0.Formular.class);
        Identifikator id22 = new Identifikator();
        id22.setValue("testId");
        id22.setSchemeAgencyID("schemeId");
        when(jzufi22.getId()).thenReturn(id22);

        FormularEntity22 entity22 = mock(FormularEntity22.class);
        when(entity22.getJzufi()).thenReturn(jzufi22);

        GenericFormular genericFormular = GenericModelMapper.mapFormularEntity(entity22);

        assertTrue(genericFormular.isXZuFi22());
        assertEquals("schemeId.FO.testId", genericFormular.getId());
    }

    @Test
    void fromEntityCreatesGenericFormularFromFormularEntity23() {
        de.xoev.xzufi.v2_3_1.Formular jzufi23 = mock(de.xoev.xzufi.v2_3_1.Formular.class);
        de.xoev.xzufi.v2_3_1.Identifikator id23 = new de.xoev.xzufi.v2_3_1.Identifikator();
        id23.setValue("testId");
        id23.setSchemeAgencyID("schemeId");
        when(jzufi23.getId()).thenReturn(id23);

        FormularEntity23 entity23 = mock(FormularEntity23.class);
        when(entity23.getJzufi()).thenReturn(jzufi23);

        GenericFormular genericFormular = GenericModelMapper.mapFormularEntity(entity23);

        assertTrue(genericFormular.isXZuFi23());
        assertEquals("schemeId.FO.testId", genericFormular.getId());
    }

    @Test
    void getOrConvertXZuFi22ReturnsCorrectObject() {
        de.xoev.xzufi.v2_2_0.Formular formular22 = mock(de.xoev.xzufi.v2_2_0.Formular.class);
        when(formular22.getId()).thenReturn(getId22());
        GenericFormular genericFormular = new GenericFormular(formular22);

        assertEquals(formular22, genericFormular.getOrConvertXZuFi22());
    }

    @Test
    void getOrConvertXZuFi23ReturnsCorrectObject() {
        de.xoev.xzufi.v2_3_1.Formular formular23 = mock(de.xoev.xzufi.v2_3_1.Formular.class);
        when(formular23.getId()).thenReturn(getId23());
        GenericFormular genericFormular = new GenericFormular(formular23);

        assertEquals(formular23, genericFormular.getOrConvertXZuFi23());
    }
}
