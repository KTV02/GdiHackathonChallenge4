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
package de.dataport.suchdienst.elasticsearch.api.model;

/**
 * Diese Enum bestimmt, welche Lebenslagen im Suchergebnis (SearchOutModel) enthalten sind. Ist ein Anfrage-Parameter für die Suche.
 * Siehe PV-9055.
 */
public enum PersonalMattersMode {
    /**
     * Das Suchergebnis enthält keinen Lebenslagen-Baum.
     */
    NONE,

    /**
     * Der Lebenslagen-Baum enthält alle Lebenslagen, die zu Ort und Anliegen passen, unabhängig davon, ob durch den Parameter
     * "personalMatters" eine weitere Einschränkung der Suche vorgegeben ist.
     */
    FULL,
}
