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

import de.dataport.suchdienst.importer.model.GenericOrganisationseinheit;
import java.util.Collections;
import java.util.Map;

/**
 * Stammdaten der Organisationseinheiten, indexiert nach OE-ID.
 *
 * <p>Enthält Name, Anschriften und die XZuFi-Repräsentation jeder OE. Der Lookup wird
 * einmalig pro Batch-Seite aufgebaut und zum Anreichern der Relation-Ergebnisse verwendet.</p>
 *
 * @param groupedByOrganisationId OE-Stammdaten indexiert nach Organisationseinheit-ID
 */
record OeStammdatenLookup(Map<String, GenericOrganisationseinheit> groupedByOrganisationId) {
    /** Stellt sicher, dass der übergebene Lookup nicht nachträglich verändert werden kann. */
    OeStammdatenLookup {
        groupedByOrganisationId = Collections.unmodifiableMap(groupedByOrganisationId);
    }

    /**
     * Liefert die Stammdaten einer Organisationseinheit.
     *
     * @param oeId Organisationseinheit-ID
     * @return Stammdaten oder {@code null}, falls die OE nicht im Lookup enthalten ist
     */
    public GenericOrganisationseinheit get(String oeId) {
        return groupedByOrganisationId.get(oeId);
    }
}
