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
import java.util.Map;
import java.util.Set;

/**
 * Zuordnung von Leistungsbeschreibungs-IDs zu ihren Leistungskatalog-Schlüsseln (Leika-IDs).
 *
 * <p>Leika-Schlüssel sind die bundeseinheitlichen Identifikatoren des Leistungskatalogs
 * (z. B. {@code 99001001000000}). Der Lookup wird einmalig pro Batch-Seite aufgebaut.</p>
 *
 * @param groupedByLeistungId Leika-IDs gruppiert nach Leistungsbeschreibungs-ID
 */
record LeikaLookup(Map<String, Set<String>> groupedByLeistungId) {
    /** Stellt sicher, dass der übergebene Lookup nicht nachträglich verändert werden kann. */
    LeikaLookup {
        groupedByLeistungId = Collections.unmodifiableMap(groupedByLeistungId);
    }

    /**
     * Liefert die Leika-IDs für eine gegebene Leistungsbeschreibungs-ID.
     *
     * @param lbId Leistungsbeschreibungs-ID
     * @return Zugeordnete Leika-IDs; leeres Set, falls keine vorhanden
     */
    public Set<String> forLeistung(String lbId) {
        return groupedByLeistungId.getOrDefault(lbId, Set.of());
    }
}
