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

/**
 * Beschreibt das Resultat der Einstellung eines Task in den Mgr.
 */
public enum SudTaskSubmissionResultState {
    /**
     * Der Task wurde angenommen und in den Mgr eingestellt.
     */
    ACCEPTED,

    /**
     * Der Task wurde ignoriert, da es bereits ein Duplikat im Mgr gibt.
     */
    IGNORED_DUE_DUPLICATE,
}
