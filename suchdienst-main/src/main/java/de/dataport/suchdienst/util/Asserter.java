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
package de.dataport.suchdienst.util;

/**
 * Eine Komponente um Assertions auszugeben. Es gibt unterschiedliche Implementierungen für Dev- bzw. Produktivbetrieb.
 */
public interface Asserter {
    /**
     * Zu verwenden als Reaktion auf Situationen, die "eigentlich" nicht auftreten dürfen.
     */
    void neverReached(String msg);
}
