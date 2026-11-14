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
package de.dataport.suchdienst.taskmgr.model;

/**
 * Nicht-generische, typ-erased Sicht auf einen {@link SudTask}.
 *
 * <p>Wird von Kontexten verwendet, die einen {@link SudTask} rein zu Verwaltungs-/
 * Scheduling-Zwecken (Warteschlangen-Verwaltung, Iteration, Konfliktprüfung) benötigen, ohne
 * den konkreten Future-Ergebnistyp {@code T} zu kennen oder zu benötigen (z. B.
 * {@link de.dataport.suchdienst.taskmgr.service.TaskBucket}). Dadurch entfallen dort generische
 * Wildcard-Rückgabetypen ({@code SudTask<?>}), die von SonarQube (java:S1452) bemängelt werden.
 * An den wenigen Stellen, an denen der konkrete Typ {@code T} tatsächlich benötigt wird (z. B.
 * zur Ausführung des Tasks), wird gezielt auf {@code SudTask<?>} zurückgecastet.
 */
public interface SudTaskHandle {
    SudTaskType getTaskType();
}
