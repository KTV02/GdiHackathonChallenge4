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
package de.dataport.suchdienst.importer.model.warteschlange;

import de.dataport.suchdienst.importer.model.jzufi.MetaData;
import java.time.Instant;

public interface ImportRequestInfo extends MetaData {
    /**
     * @return Der Zeitpunkt, an dem der Import-Auftrag angenommen wurde.
     */
    Instant getRequestAcceptedAt();

    /**
     * @return Der Zeitpunkt, an dem der Auftrag ausgeführt wurde. Weil Aufträge nach Fehlern wiederholt werden können,
     * wird dieser Wert auch upgedatet, enthält also den letzten Ausführungszeitpunkt.
     */
    Instant getRequestProcessedAt();

    ImportRequestStatus getStatus();

    void setStatus(ImportRequestStatus status);
}
