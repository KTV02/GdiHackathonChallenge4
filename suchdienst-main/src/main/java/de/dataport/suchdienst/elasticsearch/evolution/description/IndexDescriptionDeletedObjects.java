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
package de.dataport.suchdienst.elasticsearch.evolution.description;

import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.elasticsearch.evolution.DefaultDescription;

public class IndexDescriptionDeletedObjects implements DefaultDescription {

    @Override
    public String getDescription() {
        return "Erstellt den Index für die gelöschten Objekte.";
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public String getDestIndex() {
        return IndexNames.DELETED_OBJECTS.getIndexName() + getVersion();
    }

    @Override
    public IndexNames getAbstractIndexName() {
        return IndexNames.DELETED_OBJECTS;
    }

    @Override
    public String pathToMappingFile() {
        return "indexmappings/mapping_deletedobjects_1.json";
    }
}
