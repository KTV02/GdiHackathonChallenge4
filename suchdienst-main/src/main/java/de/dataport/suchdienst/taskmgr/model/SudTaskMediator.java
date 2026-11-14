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

import java.util.function.Supplier;

/**
 * Ein SudTaskMediator legt fest, welcher Code bei der Verarbeitung eines Tasks ausgeführt werden soll.
 */
public interface SudTaskMediator<T> {
    /**
     * Legt fest, was bei der Ausführung des Tasks passieren soll.
     *
     * @return Das auszuführende Callable. Gibt null zurück, wenn hier nichts entschieden werden kann
     * ("Mediator ist nicht zuständig").
     */
    Supplier<T> mediate(SudTask<T> task);

    /**
     * Liefert den Mediator-Typ gemäß Task-Manager-Vertrag.
     *
     * @return Typ des Mediators
     */
    Class<T> getMediatorType();
}
