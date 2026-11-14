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

import de.dataport.suchdienst.importer.model.RoleType;
import de.dataport.suchdienst.indexer.model.OnlinedienstRelationen.OrganisationseinheitRollenZuordnung.Role;
import de.dataport.suchdienst.indexer.model.OnlinedienstRelationen.OrganisationseinheitRollenZuordnung.Role.Source;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Aggregiert Rollenbeiträge aus verschiedenen Quellen zu finalen OD-OE-Rollen.
 *
 * <p>Die Aggregation führt identische Beiträge über OE-ID, Quelle, LB-Kontext und Rollentyp
 * zusammen und vereinigt dabei die zugehörigen ARS-Werte. Die Klasse selbst ist zustandslos;
 * Aggregation erfolgt ausschließlich über Funktionsaufrufe mit explizitem Input und Output.</p>
 */
final class RoleContributions {

    private RoleContributions() {}

    /**
     * Materialisiert die aggregierten Beiträge als Rollen, gruppiert nach OE-ID.
     */
    static Map<String, List<Role>> toRolesByOeId(
        List<RoleContribution> contributions,
        LeikaLookup leikas
    ) {
        Map<RoleKey, RoleContribution> aggregatedByKey = aggregateByKey(contributions);
        Map<String, List<Role>> result = new LinkedHashMap<>();
        for (Map.Entry<RoleKey, RoleContribution> entry : aggregatedByKey.entrySet()) {
            RoleKey key = entry.getKey();
            RoleContribution aggregated = entry.getValue();
            Role role = new Role(
                aggregated.roleType(),
                key.source(),
                key.lbId(),
                copyNonNull(leikas.forLeistung(key.lbId())),
                aggregated.ars()
            );
            result.computeIfAbsent(key.oeId(), ignored -> new ArrayList<>()).add(role);
        }
        return result;
    }

    private static Map<RoleKey, RoleContribution> aggregateByKey(
        List<RoleContribution> contributions
    ) {
        return contributions
            .stream()
            .collect(
                Collectors.toMap(
                    RoleContribution::toKey,
                    contribution -> contribution,
                    RoleContributions::mergeContributions,
                    LinkedHashMap::new
                )
            );
    }

    private static RoleContribution mergeContributions(
        RoleContribution left,
        RoleContribution right
    ) {
        return new RoleContribution(
            left.oeId(),
            left.source(),
            left.lbId(),
            left.roleType(),
            union(left.ars(), right.ars())
        );
    }

    /**
     * Einzelner Rollenbeitrag vor der Aggregation.
     */
    record RoleContribution(
        String oeId,
        Source source,
        String lbId,
        RoleType roleType,
        Set<String> ars
    ) {
        RoleContribution {
            ars = copyNonNull(ars);
        }

        private RoleKey toKey() {
            return new RoleKey(oeId, source, lbId, roleType);
        }
    }

    private record RoleKey(String oeId, Source source, String lbId, RoleType roleType) {}

    /**
     * Entfernt {@code null}-Einträge und liefert eine unveränderliche Kopie zurück.
     */
    private static Set<String> copyNonNull(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return values.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
    }

    private static Set<String> union(Set<String> left, Set<String> right) {
        return Stream.concat(left.stream(), right.stream()).collect(Collectors.toUnmodifiableSet());
    }
}
