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
package de.dataport.suchdienst.elasticsearch.evolution;

/**
 * Wenn eine "Standardänderung" durchgeführt werden soll, dann sollte von dieser Klasse abgeleitet
 * werden. Standardänderungen sind Änderungen bei denen ein ganz normaler Reindex ausgeführt wird.
 */
public interface IndexMigrationDescription extends IndexUpdateDescription {
    /**
     * Liefert den Indexnamen dem die Daten kommen
     */
    String getSourceIndex();

    /**
     * Version die für diese Indexdescription vorausgesetzt wird
     */
    int getRequiredPreVersion();
}
