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

public class IndexDescriptionSpezialisierungen implements DefaultDescription {

    @Override
    public String getDescription() {
        return "Erstelle Index für die Spezialisierungen.";
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public String getDestIndex() {
        return IndexNames.SPEZIALISIERUNGEN.getIndexName() + getVersion();
    }

    @Override
    public IndexNames getAbstractIndexName() {
        return IndexNames.SPEZIALISIERUNGEN;
    }

    @Override
    public String pathToMappingFile() {
        return "indexmappings/mapping_spezialisierungen_1.json";
    }
}
