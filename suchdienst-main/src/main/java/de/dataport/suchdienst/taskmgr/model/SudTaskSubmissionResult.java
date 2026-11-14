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

import java.util.concurrent.CompletableFuture;

/**
 * Ergebnis des Einstellens ("submit") eines Tasks in den Mgr.
 *
 * @param state  Der State gibt an, ob der Task akzeptiert oder abgelehnt wurde.
 * @param future Bietet Zugriff auf das Berechnungsergebnis des Tasks
 * @param <T>    Typ des Berechnungsergebnisses
 */
public record SudTaskSubmissionResult<T>(
    SudTaskSubmissionResultState state,
    CompletableFuture<T> future
) {}
