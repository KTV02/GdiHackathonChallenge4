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
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class IndexDescriptionVerwaltungsleistung implements DefaultDescription {

    private final String arsFederalstate;

    @Override
    public String getDescription() {
        return "Legt Index für die Verwaltungsleistungen an.";
    }

    @Override
    public int getVersion() {
        return 9;
    }

    @Override
    public String getDestIndex() {
        return IndexNames.ARS_MAPPING.get(arsFederalstate).getIndexName() + getVersion();
    }

    @Override
    public IndexNames getAbstractIndexName() {
        return IndexNames.ARS_MAPPING.get(arsFederalstate);
    }

    @Override
    public AliasNames getAbstractAliasName() {
        return AliasNames.ARS_MAPPING.get(arsFederalstate);
    }

    @Override
    public String getAlias() {
        return AliasNames.ARS_MAPPING.get(arsFederalstate).getName() + getVersion();
    }

    @Override
    public String pathToMappingFile() {
        return "indexmappings/mapping_verwaltungsleistung_1.json";
    }

    @Override
    public String pathToSettingsFile() {
        return "indexmappings/settings_verwaltungsleistungen_1.json";
    }
}
