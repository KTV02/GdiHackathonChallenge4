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

public class IndexDescriptionOrganisationseinheiten implements DefaultDescription {

    @Override
    public String getDescription() {
        return "Erstelle Index für die Organisationseinheiten.";
    }

    @Override
    public int getVersion() {
        return 2;
    }

    @Override
    public String getDestIndex() {
        return IndexNames.ORGANISATIONSEINHEITEN.getIndexName() + getVersion();
    }

    @Override
    public IndexNames getAbstractIndexName() {
        return IndexNames.ORGANISATIONSEINHEITEN;
    }

    @Override
    public String pathToMappingFile() {
        return "indexmappings/mapping_organisationseinheiten_1.json";
    }
}
