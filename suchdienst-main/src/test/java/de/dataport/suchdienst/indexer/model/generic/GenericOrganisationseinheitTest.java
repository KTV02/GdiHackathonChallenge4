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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.dataport.suchdienst.importer.model.GenericOrganisationseinheit;
import org.junit.jupiter.api.Test;

class GenericOrganisationseinheitTest implements GenericTestHelper {

    @Test
    void constructorWithXZuFi22InitializesCorrectly() {
        de.xoev.xzufi.v2_2_0.OrganisationseinheitErweitert organisationseinheit22 = mock(
            de.xoev.xzufi.v2_2_0.OrganisationseinheitErweitert.class
        );
        when(organisationseinheit22.getId()).thenReturn(getId22());
        GenericOrganisationseinheit genericOrganisationseinheit = new GenericOrganisationseinheit(
            organisationseinheit22
        );

        assertNotNull(genericOrganisationseinheit.getId());
        assertTrue(genericOrganisationseinheit.getId().contains(".OE."));
    }

    @Test
    void constructorWithXZuFi23InitializesCorrectly() {
        de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert organisationseinheit23 = mock(
            de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert.class
        );
        when(organisationseinheit23.getId()).thenReturn(getId23());
        GenericOrganisationseinheit genericOrganisationseinheit = new GenericOrganisationseinheit(
            organisationseinheit23
        );

        assertNotNull(genericOrganisationseinheit.getId());
        assertTrue(genericOrganisationseinheit.getId().contains(".OE."));
    }

    @Test
    void getOrConvertXZuFi22ReturnsCorrectObject() {
        de.xoev.xzufi.v2_2_0.OrganisationseinheitErweitert organisationseinheit22 = mock(
            de.xoev.xzufi.v2_2_0.OrganisationseinheitErweitert.class
        );
        when(organisationseinheit22.getId()).thenReturn(getId22());
        GenericOrganisationseinheit genericOrganisationseinheit = new GenericOrganisationseinheit(
            organisationseinheit22
        );

        assertNotNull(genericOrganisationseinheit.getOrConvertXZuFi22());
    }

    @Test
    void getOrConvertXZuFi23ReturnsCorrectObject() {
        de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert organisationseinheit23 = mock(
            de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert.class
        );
        when(organisationseinheit23.getId()).thenReturn(getId23());
        GenericOrganisationseinheit genericOrganisationseinheit = new GenericOrganisationseinheit(
            organisationseinheit23
        );

        assertNotNull(genericOrganisationseinheit.getOrConvertXZuFi23());
    }
}
