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

import de.dataport.suchdienst.elasticsearch.api.AliasNames;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.elasticsearch.evolution.DefaultDescription;

public class IndexDescriptionOnlinediensteRelationen implements DefaultDescription {

    @Override
    public String getDescription() {
        return "Erstelle Index für die Onlinedienste-Relationen.";
    }

    @Override
    public int getVersion() {
        return 2;
    }

    @Override
    public String getDestIndex() {
        return IndexNames.ONLINEDIENSTE_RELATIONEN.getIndexName() + getVersion();
    }

    @Override
    public IndexNames getAbstractIndexName() {
        return IndexNames.ONLINEDIENSTE_RELATIONEN;
    }

    @Override
    public AliasNames getAbstractAliasName() {
        return AliasNames.ONLINEDIENSTE_RELATIONEN;
    }

    @Override
    public String getAlias() {
        return AliasNames.ONLINEDIENSTE_RELATIONEN.getName() + getVersion();
    }

    @Override
    public String pathToMappingFile() {
        return "indexmappings/mapping_onlinedienste_relationen_1.json";
    }
}
