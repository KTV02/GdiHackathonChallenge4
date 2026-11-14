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

/**
 * OE-Zuständigkeiten (typ=ORGANISATION) gruppiert nach Leistungsbeschreibungs-ID.
 *
 * <p>Jeder Eintrag enthält {@link ConvertedZustaendigkeit}-Objekte mit {@code parentId}
 * (= OE-ID), {@code ars} und lazy abrufbarem XZuFi-2.3.1-Objekt (für Rollen-Extraktion).
 * Der Lookup wird einmalig pro Batch-Seite aufgebaut und für den ARS-Abgleich genutzt.</p>
 *
 * @param groupedByLeistungId OE-Zuständigkeiten gruppiert nach Leistungsbeschreibungs-ID
 */
record OeZustaendigkeitenLookup(Map<String, List<ConvertedZustaendigkeit>> groupedByLeistungId) {
    /** Stellt sicher, dass der übergebene Lookup nicht nachträglich verändert werden kann. */
    OeZustaendigkeitenLookup {
        groupedByLeistungId = Collections.unmodifiableMap(groupedByLeistungId);
    }

    /**
     * Liefert die OE-Zuständigkeiten für eine gegebene Leistungsbeschreibungs-ID.
     *
     * @param lbId Leistungsbeschreibungs-ID
     * @return OE-Zuständigkeiten; leere Liste, falls keine vorhanden
     */
    public List<ConvertedZustaendigkeit> forLeistung(String lbId) {
        return groupedByLeistungId.getOrDefault(lbId, List.of());
    }
}
