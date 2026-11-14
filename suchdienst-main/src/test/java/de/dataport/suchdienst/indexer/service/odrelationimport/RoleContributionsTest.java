/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.indexer.service.odrelationimport;

import static org.assertj.core.api.Assertions.assertThat;

import de.dataport.suchdienst.importer.model.RoleType;
import de.dataport.suchdienst.indexer.model.OnlinedienstRelationen.OrganisationseinheitRollenZuordnung.Role;
import de.dataport.suchdienst.indexer.model.OnlinedienstRelationen.OrganisationseinheitRollenZuordnung.Role.Source;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RoleContributionsTest {

    @Test
    void mergesMatchingRoleContributionsAndUnifiesArs() {
        List<RoleContributions.RoleContribution> contributions = List.of(
            new RoleContributions.RoleContribution(
                "de.bund.OE.oe-1",
                Source.ZS_OE,
                "de.bund.LB.lb-1",
                RoleType.COMPETENT_OFFICE,
                Set.of("091620000000")
            ),
            new RoleContributions.RoleContribution(
                "de.bund.OE.oe-1",
                Source.ZS_OE,
                "de.bund.LB.lb-1",
                RoleType.COMPETENT_OFFICE,
                Set.of("091620152000")
            )
        );

        Map<String, List<Role>> rolesByOeId = RoleContributions.toRolesByOeId(
            contributions,
            new LeikaLookup(Map.of("de.bund.LB.lb-1", Set.of("99001001000000")))
        );

        assertThat(rolesByOeId).containsOnlyKeys("de.bund.OE.oe-1");
        assertThat(rolesByOeId.get("de.bund.OE.oe-1")).containsExactly(
            new Role(
                RoleType.COMPETENT_OFFICE,
                Source.ZS_OE,
                "de.bund.LB.lb-1",
                Set.of("99001001000000"),
                Set.of("091620000000", "091620152000")
            )
        );
    }

    @Test
    void keepsDistinctSourcesAsSeparateRoles() {
        List<RoleContributions.RoleContribution> contributions = List.of(
            new RoleContributions.RoleContribution(
                "de.bund.OE.oe-1",
                Source.ZS_OE,
                "de.bund.LB.lb-1",
                RoleType.COMPETENT_OFFICE,
                Set.of("091620000000")
            ),
            new RoleContributions.RoleContribution(
                "de.bund.OE.oe-1",
                Source.OD,
                "de.bund.LB.lb-1",
                RoleType.COMPETENT_OFFICE,
                Set.of("020000000000")
            )
        );

        List<Role> roles = RoleContributions.toRolesByOeId(
            contributions,
            new LeikaLookup(Map.of())
        ).get("de.bund.OE.oe-1");

        assertThat(roles).containsExactly(
            new Role(
                RoleType.COMPETENT_OFFICE,
                Source.ZS_OE,
                "de.bund.LB.lb-1",
                Set.of(),
                Set.of("091620000000")
            ),
            new Role(
                RoleType.COMPETENT_OFFICE,
                Source.OD,
                "de.bund.LB.lb-1",
                Set.of(),
                Set.of("020000000000")
            )
        );
    }

    @Test
    void removesNullArsValuesFromContributions() {
        Set<String> arsWithNull = new LinkedHashSet<>();
        arsWithNull.add("091620000000");
        arsWithNull.add(null);

        List<RoleContributions.RoleContribution> contributions = List.of(
            new RoleContributions.RoleContribution(
                "de.bund.OE.oe-1",
                Source.ZS_OE,
                "de.bund.LB.lb-1",
                RoleType.UNKNOWN,
                arsWithNull
            )
        );

        List<Role> roles = RoleContributions.toRolesByOeId(
            contributions,
            new LeikaLookup(Map.of("de.bund.LB.lb-1", Set.of("99001001000000")))
        ).get("de.bund.OE.oe-1");

        assertThat(roles).containsExactly(
            new Role(
                RoleType.UNKNOWN,
                Source.ZS_OE,
                "de.bund.LB.lb-1",
                Set.of("99001001000000"),
                Set.of("091620000000")
            )
        );
    }
}
