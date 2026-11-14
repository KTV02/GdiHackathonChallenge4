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
import static org.hamcrest.Matchers.equalTo;

import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.model.LeikaEntry;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LeistungskatalogTest {

    private static final String PV_LAGEN_VERION = "20211001";
    private static final String PV_LAGEN_FUTURE_VERSION = "20990101";
    public static final String PV_LAGEN_URI = "urn:xoev-de:fim:codeliste:pvlagen";

    public static final CodelistEntry BUERGER = createCodelistEntry("1000000");
    public static final CodelistEntry GEBURT = createCodelistEntry("1010000");
    public static final CodelistEntry NACH_DER_GEBURT = createCodelistEntry("1010200");
    public static final CodelistEntry VOR_DER_GEBURT = createCodelistEntry("1010100");
    public static final CodelistEntry GESUNDHEIT_UND_VORSORGE = createCodelistEntry("1130000");
    public static final CodelistEntry PFLEGE = createCodelistEntry(
        "1130400",
        PV_LAGEN_FUTURE_VERSION
    );

    @Test
    void testGetCodelistVersions() {
        var leikaEntry1 = new LeikaEntry(
            "LeikaKey1",
            Set.of(
                List.of(
                    NACH_DER_GEBURT.getKey(),
                    VOR_DER_GEBURT.getKey(),
                    GEBURT.getKey(),
                    BUERGER.getKey()
                ),
                List.of(PFLEGE.getKey(), GESUNDHEIT_UND_VORSORGE.getKey(), BUERGER.getKey())
            )
        );
        var leikaEntry2 = new LeikaEntry(
            "LeikaKey2",
            Set.of(
                List.of(
                    NACH_DER_GEBURT.getKey(),
                    VOR_DER_GEBURT.getKey(),
                    GEBURT.getKey(),
                    BUERGER.getKey()
                )
            )
        );
        var leistungskatalog = new Leistungskatalog(List.of(leikaEntry1, leikaEntry2));

        var versions = leistungskatalog.getCodelistVersions();
        // Prüfe Inhalt ungeachtet der Reihenfolge (Set-Equals) und zusätzlich explizit Reihenfolge via first/last
        assertThat(versions, equalTo(Set.of(PV_LAGEN_FUTURE_VERSION, PV_LAGEN_VERION)));
        assertThat(
            "Neueste Version sollte zuerst (TreeSet reverseOrder)",
            versions.first(),
            equalTo(PV_LAGEN_FUTURE_VERSION)
        );
        assertThat("Älteste Version sollte zuletzt", versions.last(), equalTo(PV_LAGEN_VERION));
    }

    @Test
    void testEmptyLeistungskatalog() {
        var leistungskatalog = new Leistungskatalog(List.of());
        assertThat("Sollte leer sein", leistungskatalog.isEmpty(), equalTo(true));
        assertThat(
            "Versionsmenge sollte leer sein",
            leistungskatalog.getCodelistVersions().isEmpty(),
            equalTo(true)
        );
    }

    private static CodelistEntry createCodelistEntry(String code) {
        return createCodelistEntry(code, PV_LAGEN_VERION);
    }

    private static CodelistEntry createCodelistEntry(String code, String version) {
        var entry = new CodelistEntry();
        entry.setCode(code);
        entry.setUri(PV_LAGEN_URI);
        entry.setVersion(version);
        return entry;
    }
}
