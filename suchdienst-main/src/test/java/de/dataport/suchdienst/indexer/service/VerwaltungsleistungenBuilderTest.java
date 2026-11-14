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

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import de.dataport.suchdienst.indexer.model.mapping.ResponsibilityRole;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.FormularVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SpezialisierungVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.Verwaltungsleistung;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class VerwaltungsleistungenBuilderTest {

    public static final String NRW = "050000000000";

    /**
     * Dieser Test stellt die einfachste Situation nach.
     * Es liegt hier eine LB vor mit einer OE einer OD und einem FO, alle auf Landesebene:
     * OE1: 050000000000
     * OD1: 050000000000
     * FO1: 050000000000
     * Der Algorithmus soll hier eine VL mit allen 3 Ars der Hierarchie liefern. Der exakte Ars soll 050000000000 sein.
     */
    @Test
    void testEasyCase() {
        var arsTree = new ArsTree(Set.of("051234567890", "051234567891"));
        var vlBuilder = new VerwaltungsleistungenBuilder(arsTree, "05", false);
        var lb = new LeistungsbeschreibungVL();
        lb.setId("lb-id");

        var oe1 = new OrganisationseinheitVL();
        oe1.setId("oe1-id");
        oe1.setName("Oe1");
        var respRole = new ResponsibilityRole();
        respRole.setName("Zuständige Stelle und Ansprechpunkt");
        respRole.setCode("01");
        oe1.setRelations(Map.of(NRW, List.of(respRole)));
        var sortedOes = List.of(oe1);

        var od1 = new OnlinedienstVL();
        od1.setId("od1-id");
        od1.setName("Od1");
        od1.setArs(Set.of(NRW));
        var sortedOds = List.of(od1);

        var fo1 = new FormularVL();
        fo1.setId("fo1-id");
        fo1.setName("Fo1");
        fo1.setArs(Set.of(NRW));
        var sortedFos = List.of(fo1);

        List<SpezialisierungVL> sortedSps = List.of();

        var vls = vlBuilder.buildVerwaltungsleistungen(
            lb,
            sortedOes,
            sortedOds,
            sortedFos,
            sortedSps
        );
        assertThat(vls, notNullValue());
        assertThat(vls.size(), equalTo(1));

        assertThat(
            vls
                .stream()
                .anyMatch(
                    vlHasValues(
                        Set.of("oe1-id"),
                        Set.of("od1-id"),
                        Set.of("fo1-id"),
                        Set.of("051234567890", "051234567891"),
                        Set.of(NRW)
                    )
                ),
            is(true)
        );
    }

    /**
     * Dieser Test stellt die Beispielsituation aus PV-10016 nach.
     * Es liegt hier eine LB vor mit einer OE auf Landesebene und zwei ODS auf Gemeindeebene.
     * OE1: 050000000000
     * OD1: 051234567890
     * OD2: 051234567891
     * Der Algorithmus soll hier 5 VLs liefern:
     * - Eine VL mit OE1 und OD1, Ars muss 051234567890 sein. Kein exakter ARS, da die OE/OD hier auf unterschiedlichem Level sind
     * - Eine VL mit OE1 und OD2, Ars muss 051234567891 sein. Kein exakter ARS, da die OE/OD hier auf unterschiedlichem Level sind
     * - Eine VL mit OE1 und OD1, Ars null, ExakterArs muss 051234567890 sein
     * - Eine VL mit OE1 und OD2, Ars null, ExakterArs muss 051234567891 sein
     * - Eine VL mit OE1 ohne OD, Ars alle unterhalb von 050000000000, bis auf 051234567890, 051234567891
     */
    @Test
    void testExtraLbsForExactArsSearch() {
        var arsTree = new ArsTree(
            Set.of("051234567890", "051234567891", "051234567892", "051234567893", "051234567894")
        );
        var vlBuilder = new VerwaltungsleistungenBuilder(arsTree, "05", false);
        var lb = new LeistungsbeschreibungVL();
        lb.setId("lb-id");

        var oe1 = new OrganisationseinheitVL();
        oe1.setId("oe1-id");
        oe1.setName("Oe1");
        var respRole = new ResponsibilityRole();
        respRole.setName("Zuständige Stelle und Ansprechpunkt");
        respRole.setCode("01");
        oe1.setRelations(Map.of(NRW, List.of(respRole)));
        var sortedOes = List.of(oe1);

        var od1 = new OnlinedienstVL();
        od1.setId("od1-id");
        od1.setName("Od1");
        od1.setArs(Set.of("051234567890"));
        var od2 = new OnlinedienstVL();
        od2.setId("od2-id");
        od2.setName("Od2");
        od2.setArs(Set.of("051234567891"));
        var sortedOds = List.of(od1, od2);

        List<SpezialisierungVL> sortedSps = List.of();
        List<FormularVL> sortedFos = List.of();

        var vls = vlBuilder.buildVerwaltungsleistungen(
            lb,
            sortedOes,
            sortedOds,
            sortedFos,
            sortedSps
        );
        assertThat(vls, notNullValue());
        assertThat(vls.size(), equalTo(5));
        assertThat(
            vls
                .stream()
                .anyMatch(
                    vlHasValues(
                        emptySet(),
                        Set.of("od1-id"),
                        Set.of(),
                        Set.of(),
                        Set.of("051234567890")
                    )
                ),
            is(true)
        );
        assertThat(
            vls
                .stream()
                .anyMatch(
                    vlHasValues(
                        emptySet(),
                        Set.of("od2-id"),
                        Set.of(),
                        Set.of(),
                        Set.of("051234567891")
                    )
                ),
            is(true)
        );
        assertThat(
            vls
                .stream()
                .anyMatch(
                    vlHasValues(
                        Set.of("oe1-id"),
                        emptySet(),
                        Set.of(),
                        Set.of("051234567892", "051234567893", "051234567894"),
                        Set.of(NRW)
                    )
                ),
            is(true)
        );
        assertThat(
            vls
                .stream()
                .anyMatch(
                    vlHasValues(
                        Set.of("oe1-id"),
                        Set.of("od1-id"),
                        Set.of(),
                        Set.of("051234567890"),
                        emptySet()
                    )
                ),
            is(true)
        );
        assertThat(
            vls
                .stream()
                .anyMatch(
                    vlHasValues(
                        Set.of("oe1-id"),
                        Set.of("od2-id"),
                        Set.of(),
                        Set.of("051234567891"),
                        emptySet()
                    )
                ),
            is(true)
        );
    }

    /**
     * Ein Test mit OE, OD und FO unter Berücksichtigung von exakt ARS.
     * Es liegt hier eine LB vor mit einer OE auf Landesebene und eine OD und einem FO auf Gemeindeebene.
     * OE: 050000000000
     * OD: 051234567890
     * FO: 051234567891
     * Der Algorithmus soll hier 4 VLs liefern (seit der Anforderung, dass keine VLs ohne OE oder OD erstellt werden):
     * - Eine VL mit OE und OD, Ars muss 051234567890 sein. Kein exakter ARS, da die OE/OD hier auf unterschiedlichem Level sind -->OK
     * - Eine VL mit OE und FO, Ars muss 051234567891 sein. Kein exakter ARS, da die OE/FO hier auf unterschiedlichem Level sind -> OK
     * - Eine VL ohne OE, mit OD, Ars null, ExakterArs muss 051234567890 sein --> OK
     * - Eine VL mit OE, ohne FO, ohne OD, Ars alle unterhalb von 050000000000, bis auf 051234567890, 051234567891 --> OK
     * NICHT MEHR: Eine VL ohne OE mit FO, da VLs ohne OE oder OD nicht mehr erstellt werden
     */
    @Test
    void testExtraLbsForExactArsSearchWithForms() {
        var arsTree = new ArsTree(
            Set.of("051234567890", "051234567891", "051234567892", "051234567893", "051234567894")
        );
        var vlBuilder = new VerwaltungsleistungenBuilder(arsTree, "05", false);
        var lb = new LeistungsbeschreibungVL();
        lb.setId("lb-id");

        var oe = new OrganisationseinheitVL();
        oe.setId("oe-id");
        oe.setName("Oe");
        var respRole = new ResponsibilityRole();
        respRole.setName("Zuständige Stelle und Ansprechpunkt");
        respRole.setCode("01");
        oe.setRelations(Map.of(NRW, List.of(respRole)));
        var sortedOes = List.of(oe);

        var od = new OnlinedienstVL();
        od.setId("od-id");
        od.setName("Od");
        od.setArs(Set.of("051234567890"));
        var sortedOds = List.of(od);

        var fo = new FormularVL();
        fo.setId("fo-id");
        fo.setName("Fo");
        fo.setArs(Set.of("051234567891"));
        var sortedFos = List.of(fo);

        List<SpezialisierungVL> sortedSps = List.of();

        var vls = vlBuilder.buildVerwaltungsleistungen(
            lb,
            sortedOes,
            sortedOds,
            sortedFos,
            sortedSps
        );
        assertThat(vls, notNullValue());
        assertThat(vls.size(), equalTo(4));
        assertThat(
            vls
                .stream()
                .anyMatch(
                    vlHasValues(
                        emptySet(),
                        Set.of("od-id"),
                        Set.of(),
                        Set.of(),
                        Set.of("051234567890")
                    )
                ),
            is(true)
        );
        assertThat(
            vls
                .stream()
                .anyMatch(
                    vlHasValues(
                        Set.of("oe-id"),
                        emptySet(),
                        Set.of(),
                        Set.of("051234567892", "051234567893", "051234567894"),
                        Set.of(NRW)
                    )
                ),
            is(true)
        );
        assertThat(
            vls
                .stream()
                .anyMatch(
                    vlHasValues(
                        Set.of("oe-id"),
                        Set.of("od-id"),
                        Set.of(),
                        Set.of("051234567890"),
                        emptySet()
                    )
                ),
            is(true)
        );
        assertThat(
            vls
                .stream()
                .anyMatch(
                    vlHasValues(
                        Set.of("oe-id"),
                        Set.of(),
                        Set.of("fo-id"),
                        Set.of("051234567891"),
                        emptySet()
                    )
                ),
            is(true)
        );
    }

    private Predicate<Verwaltungsleistung> vlHasValues(
        Set<String> oeIds,
        Set<String> odIds,
        Set<String> foIds,
        Set<String> ars,
        Set<String> exactArs
    ) {
        return vl -> {
            if (!ars.equals(vl.getArs())) {
                return false;
            }
            if (!exactArs.equals(vl.getExactArs())) {
                return false;
            }
            var vlOeIds = vl
                .getOrganisationseinheiten()
                .stream()
                .map(OrganisationseinheitVL::getId)
                .collect(toSet());
            if (!oeIds.equals(vlOeIds)) {
                return false;
            }
            var vlFoIds = vl.getFormulare().stream().map(FormularVL::getId).collect(toSet());
            if (!foIds.equals(vlFoIds)) {
                return false;
            }
            var vlOdIds = vl
                .getOnlinedienste()
                .stream()
                .map(OnlinedienstVL::getId)
                .collect(toSet());
            return odIds.equals(vlOdIds);
        };
    }
}
