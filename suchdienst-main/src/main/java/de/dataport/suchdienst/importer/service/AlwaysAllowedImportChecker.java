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
package de.dataport.suchdienst.importer.service;

import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.indexer.model.Identifiable;

public class AlwaysAllowedImportChecker implements ImportChecker {

    @Override
    public <T extends Identifiable> boolean importAllowed(
        String id,
        IndexNames indexname,
        Class<T> clazz
    ) {
        return true;
    }
}
