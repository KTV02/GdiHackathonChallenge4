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
package de.dataport.suchdienst.util.model;

/**
 * Wert des MDC-Schlüssels {@link MdcKeys#PROTOKOLLTYP}. Ordnet eine Log-Ausgabe einem Protokoll zu,
 * damit sie im Log-Backend gefiltert und ausgewertet werden kann.
 */
public enum ProtocolType {
    /** Aktionen der Anwendung selbst: Start, Konfiguration, Shutdown. */
    SYSTEM,

    /** Requests auf die internen Admin-Endpunkte unterhalb von {@code /admin}. */
    ADMIN,

    /**
     *  Requests auf die öffentlichen Endpunkte.
     *  <p>
     *  <b>ACHTUNG!!!</b>
     *  </p>
     *  Dieser Wert wird in externen Logs zum Filtern von Anfragen verwendet. So gibt es z. B. in fluentd einen
     *  Filter, der die Logs mit dem Protokolltyp PUBLIC in einen eigenen Index schreibt. Wird dieser Wert geändert,
     *  muss auch der Filter angepasst werden.
     *
     */
    PUBLIC,

    /** Entgegennahme und Verarbeitung von XZuFi-Daten des Sammlerdienstes. */
    IMPORT,

    /** Ergebnis der Authentifizierung und Autorisierung eines Requests. */
    SICHERHEIT,

    /** Aufrufe an Fremddienste, siehe ergänzend {@link MdcKeys#EXTERNER_BEREICH}. */
    EXTERN,
}
