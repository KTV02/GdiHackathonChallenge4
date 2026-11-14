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

public class TestIndexUpdateDescription3 implements DefaultDescription {

    @Override
    public String getDescription() {
        return "Für Testzwecke 3";
    }

    @Override
    public String getAlias() {
        return null;
    }

    @Override
    public int getVersion() {
        return 3;
    }

    @Override
    public String getDestIndex() {
        return "dest3";
    }

    @Override
    public IndexNames getAbstractIndexName() {
        return IndexNames.LEISTUNGSBESCHREIBUNG;
    }

    @Override
    public AliasNames getAbstractAliasName() {
        return null;
    }

    @Override
    public String pathToMappingFile() {
        return "test-mapping.json";
    }

    @Override
    public String pathToSettingsFile() {
        return null;
    }
}
