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
package de.dataport.suchdienst.taskmgr.model;

import java.util.Comparator;
import lombok.Getter;

/**
 * Arten von asynchronen Suchdienst-Operationen. Siehe PV-9812.
 */
@Getter
public enum SudTaskType {
    /**
     * Neuberechnung des OnlineService-Relations-Index.
     */
    OD_RE_RECALC(5),

    /**
     * Neuerstellen eines Verwaltungsleistungs-Index.
     */
    VL_RECALC(10),

    /**
     * Updaten des Verwaltungsleistungs-Index, basierend auf beim Import geänderten Leistungen.
     */
    VL_UPDATE(20),

    /**
     * Verarbeiten von Push-Nachrichten in die Jzufi-Indizes.
     */
    IMPORT(30);

    public static final Comparator<SudTaskType> COMPARE_BY_PRIORITY = Comparator.comparingInt(
        SudTaskType::getPriority
    ).reversed();

    /**
     * Steuert die Reihenfolge der Abarbeitung, höhere Zahlen bedeuten höhere Priorität.
     */
    private final int priority;

    SudTaskType(int priority) {
        this.priority = priority;
    }
}
