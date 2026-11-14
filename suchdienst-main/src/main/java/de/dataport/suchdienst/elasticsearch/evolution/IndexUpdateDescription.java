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
package de.dataport.suchdienst.elasticsearch.evolution;

import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import java.time.Instant;

public interface IndexUpdateDescription {
    default Instant getTimestamp() {
        return Instant.now();
    }

    /**
     * Die Beschreibung was bei dem Update durchgeführt wird
     */
    String getDescription();

    /**
     * Die neue Version vom Index
     */
    int getVersion();

    /**
     * Neuer Name vom Index, wenn der Urpsrungsindex gelöscht wird dann NULL
     */
    String getDestIndex();

    /**
     * Da ein Index bei jedem Reindizieren umbenannt wird muss ein abstrakter Name für diesen
     * existieren, damit unterschieden werden kann, für welchen Index das Update gelten soll. Bsp: Für
     * Leistungsbeschreibungen gibt es die Indizes leistungsbeschreibungen-v1 und
     * leistungsbeschreibungen-v2, man könnte als abstrakten Namen leistungsbeschreibungen wählen.
     * Dieser müsste dann in allen Updates verwendet werden.
     */
    IndexNames getAbstractIndexName();
}
