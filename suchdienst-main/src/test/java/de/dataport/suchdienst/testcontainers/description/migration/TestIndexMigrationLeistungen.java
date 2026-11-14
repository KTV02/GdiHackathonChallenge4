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
package de.dataport.suchdienst.testcontainers.description.migration;

import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.elasticsearch.evolution.IndexMigrationDescription;

public class TestIndexMigrationLeistungen implements IndexMigrationDescription {

    @Override
    public String getDescription() {
        return "Aktualisiert den Test Leistungsindex von 1 auf 2";
    }

    @Override
    public int getRequiredPreVersion() {
        return 1;
    }

    @Override
    public int getVersion() {
        return 2;
    }

    @Override
    public String getDestIndex() {
        return IndexNames.LEISTUNGSBESCHREIBUNG.getIndexName() + getVersion();
    }

    @Override
    public String getSourceIndex() {
        return "suchdienst_leistungsbeschreibungen1";
    }

    @Override
    public IndexNames getAbstractIndexName() {
        return IndexNames.LEISTUNGSBESCHREIBUNG;
    }
}
