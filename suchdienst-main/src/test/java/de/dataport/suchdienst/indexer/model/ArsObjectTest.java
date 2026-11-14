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
package de.dataport.suchdienst.indexer.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArsObjectTest {

    private ArsObject fixture;

    @BeforeEach
    void init() {
        fixture = new ArsObject();
    }

    @Test
    void testAgsConversion() {
        // https://de.wikipedia.org/wiki/Amtlicher_Gemeindeschl%C3%BCssel
        fixture.setArsAndAgs("120645410340"); // Neuhardenberg
        // https://de.wikipedia.org/wiki/Neuhardenberg
        assertThat(fixture.getAgs(), is("12064340"));

        fixture.setArsAndAgs("020000000000");
        assertThat(fixture.getAgs(), is("02000000"));
    }

    @Test
    void replacesZipCodesAndUsesTheZipForDisplayAndSuggestions() {
        fixture.setName("Musterstadt");

        boolean wasReplaced = fixture.replaceZipcodes(Set.of("12345"));

        assertThat(wasReplaced, is(true));
        assertThat(fixture.getZipCodes(), is(Set.of("12345")));
        assertThat(fixture.getDisplayZip(), is("12345"));
        assertThat(fixture.getSuggest().getInput(), is(Set.of("Musterstadt", "12345 Musterstadt")));
    }

    @Test
    void usesZipTemplateWhenMultipleZipCodesAreAssigned() {
        fixture.setName("Musterstadt");

        boolean wasReplaced = fixture.replaceZipcodes(Set.of("12345", "98765"));

        assertThat(wasReplaced, is(true));
        assertThat(fixture.getDisplayZip(), is("123**"));
        assertThat(
            fixture.getSuggest().getInput(),
            is(Set.of("Musterstadt", "12345 Musterstadt", "98765 Musterstadt"))
        );
    }

    @Test
    void ignoresAnEmptyZipCodeSet() {
        fixture.setName("Musterstadt");

        boolean wasReplaced = fixture.replaceZipcodes(Set.of());

        assertThat(wasReplaced, is(false));
        assertThat(fixture.getZipCodes(), is(Set.of()));
        assertThat(fixture.getDisplayZip(), is((String) null));
        assertThat(fixture.getSuggest().getInput(), is(Set.of()));
    }
}
