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
package de.dataport.suchdienst.indexer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import de.dataport.suchdienst.common.model.ARS;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ArsHierarchyTest {

    @Test
    void testPotsdamIstNichtImSaarland() {
        assertThat(
            new ARS("120540000000").getArsHierarchy(),
            Matchers.containsInAnyOrder("120540000000", "120000000000", "000000000000")
        );
    }

    @Test
    void testPaddingZuKurzeArs() {
        assertThat(
            new ARS("02").getArsHierarchy(),
            Matchers.containsInAnyOrder("020000000000", "000000000000")
        );
    }

    @Test
    void testAlleGesetztErstelltJedeEbene() {
        assertThat(
            new ARS("123456789123").getArsHierarchy(),
            Matchers.containsInAnyOrder(
                "123456789123",
                "123456789000",
                "123450000000",
                "123000000000",
                "120000000000",
                "000000000000"
            )
        );
    }

    @ParameterizedTest
    @CsvSource({
        "123456789000,123456789",
        "123450000000,12345",
        "123000000000,123",
        "120000000000,12",
        "091600000000,09160",
        "000000000000,00",
        "123456789999,123456789999",
        "'',00",
        "12345678999999999,12345678999999999",
        "020000000000,02",
        "040110000000,04011",
        "040120000000,04012",
        "110000000000,11",
    })
    void testHierarchyTrim(String input, String expected) {
        assertThat(new ARS(input).hierarchyTrim(), is(expected));
    }
}
