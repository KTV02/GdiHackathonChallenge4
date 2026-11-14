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
package de.dataport.suchdienst.indexer.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ArsTreeTest {

    static final Set<String> arss = Set.of(
        "020000000000",
        "012345678912",
        "012345678922",
        "012346678912",
        "012357678912",
        "013445678912"
    );
    final String arsGemeinde = "012345678912";
    final String arsGemeindeVerband = StringUtils.rightPad("012345678", 12, '0');
    final String arsKreis = StringUtils.rightPad("01234", 12, '0');
    final String arsRegierungsBezirk = StringUtils.rightPad("012", 12, '0');
    final String arsLand = StringUtils.rightPad("01", 12, '0');

    static ArsTree tree;

    @BeforeAll
    static void setup() {
        tree = new ArsTree(arss);
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "013445678111", // Gemeinde
            "013441111111", // Gemeindeverband
            "013111111111", // Kreis
            "011111111111", // Regierungsbezirk
        }
    )
    void returnIfNoMatch(String ars) {
        assertThat(tree.resolveToGemeinden(ars), containsInAnyOrder(ars));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "987654321000", // Gemeindeverband
            "987650000000", // Kreis
            "987650000000", // Regierungsbezirk (identisch mit Kreis im Beispiel)
            "980000000000", // Land
        }
    )
    void returnIfNoMatchWildCard(String ars) {
        assertThat(tree.resolveToGemeinden(ars), containsInAnyOrder(ars));
    }

    @Test
    void exactMatchStadtstaat() {
        String arsHamburg = "02" + "0".repeat(10);
        assertThat(tree.resolveToGemeinden(arsHamburg), containsInAnyOrder(arsHamburg));
    }

    @Test
    void extractExactMatchGemeinde() {
        assertThat(tree.resolveToGemeinden(arsGemeinde), containsInAnyOrder("012345678912"));
    }

    @Test
    void extractGemeinedeVerband() {
        assertThat(
            tree.resolveToGemeinden(arsGemeindeVerband),
            containsInAnyOrder("012345678912", "012345678922")
        );
    }

    @Test
    void extractKreis() {
        assertThat(
            tree.resolveToGemeinden(arsKreis),
            containsInAnyOrder("012345678912", "012345678922", "012346678912")
        );
    }

    @Test
    void extractRegierungsbezirk() {
        assertThat(
            tree.resolveToGemeinden(arsRegierungsBezirk),
            containsInAnyOrder("012345678912", "012345678922", "012346678912", "012357678912")
        );
    }

    @Test
    void extractLand() {
        assertThat(
            tree.resolveToGemeinden(arsLand),
            containsInAnyOrder(
                "012345678912",
                "012345678922",
                "012346678912",
                "012357678912",
                "013445678912"
            )
        );
    }
}
