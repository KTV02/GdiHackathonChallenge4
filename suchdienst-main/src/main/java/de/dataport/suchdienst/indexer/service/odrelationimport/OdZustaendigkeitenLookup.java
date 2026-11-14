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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lookup aller OD-Zuständigkeiten eines Batches, gruppiert nach Onlinedienst-ID (1. Ebene)
 * und Leistungsbeschreibungs-ID (2. Ebene).
 *
 * <p>Jeder Blatt-Eintrag enthält die {@link ConvertedZustaendigkeit}-Instanzen
 * mit ihrem ARS (Amtlicher Regionalschlüssel). Der Lookup wird einmalig pro Batch-Seite
 * aufgebaut und an Assembler-Pipeline und ARS-Matching weitergereicht.</p>
 *
 * @param groupedByOnlineServiceIdAndLeistungId Zuständigkeiten gruppiert nach {@code parentId} (= OD-ID)
 *                      und {@code lbId} (= Leistungsbeschreibungs-ID)
 */
record OdZustaendigkeitenLookup(
    Map<String, Map<String, List<ConvertedZustaendigkeit>>> groupedByOnlineServiceIdAndLeistungId
) {
    /** Stellt sicher, dass der übergebene Lookup nicht nachträglich verändert werden kann. */
    OdZustaendigkeitenLookup {
        groupedByOnlineServiceIdAndLeistungId = Collections.unmodifiableMap(
            groupedByOnlineServiceIdAndLeistungId
        );
    }

    /**
     * Liefert die Zuständigkeiten eines einzelnen Onlinedienstes, gruppiert nach LB-ID.
     *
     * @param odId Onlinedienst-ID
     * @return Zuständigkeiten nach LB-ID; leere Map, falls keine vorhanden
     */
    public Map<String, List<ConvertedZustaendigkeit>> forOnlinedienst(String odId) {
        return groupedByOnlineServiceIdAndLeistungId.getOrDefault(odId, Map.of());
    }

    /**
     * Sammelt alle LB-IDs, die über alle Onlinedienste hinweg vorkommen.
     *
     * @return Menge aller vorkommenden LB-IDs
     */
    public Set<String> allLbIds() {
        return groupedByOnlineServiceIdAndLeistungId
            .values()
            .stream()
            .flatMap(byLbId -> byLbId.keySet().stream())
            .collect(Collectors.toSet());
    }
}
