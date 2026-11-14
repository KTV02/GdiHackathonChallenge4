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
import de.dataport.suchdienst.elasticsearch.evolution.IndexMigrationDescription;

public class TestIndexMigrationDescription implements IndexMigrationDescription {

    @Override
    public String getSourceIndex() {
        return "alt1";
    }

    @Override
    public int getRequiredPreVersion() {
        return 1;
    }

    @Override
    public String getDescription() {
        return "mig1";
    }

    @Override
    public int getVersion() {
        return 3;
    }

    @Override
    public String getDestIndex() {
        return "neu";
    }

    @Override
    public IndexNames getAbstractIndexName() {
        return IndexNames.LEISTUNGSBESCHREIBUNG;
    }
}
