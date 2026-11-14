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
package de.dataport.suchdienst.backoffice.model.out;

/**
 * Aggregation des Verarbeitungszustandes eines Bundeslandes. Wird verwendet um im Backoffice auf der Seite "Datenübersicht" ein
 * passendes Icon anzuzeigen.
 */
public enum DataOverviewOutModelProcessState {
    /**
     * Es findet keine Verarbeitung statt.
     */
    STABLE,

    /**
     * Es gibt Einträge in der Import-Warteschlange oder in den WaitingTasks im SudTaskMgr.
     */
    WAITING,

    /**
     * Es läuft gerade ein ImportTask.
     */
    RUNNING_IMPORT,

    /**
     * Es läuft gerade ein VlUpdateTask.
     */
    RUNNING_VL_UPDATE,

    /**
     * Es läuft gerade ein VlRecalcTask.
     */
    RUNNING_VL_RECALC,

    /**
     * Es läuft gerade ein OnlineServiceRelationsRecalcTask.
     */
    RUNNING_OD_REL_RECALC,
}
