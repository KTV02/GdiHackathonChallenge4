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
import de.dataport.suchdienst.elasticsearch.evolution.IndexUpdateDescription;

public class TestIndexDeleteLeistungen implements IndexUpdateDescription {

    @Override
    public String getDescription() {
        return "Löscht den alten Test Index Leistungsbeschreibung1";
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public String getDestIndex() {
        return "suchdienst_leistungsbeschreibungen1";
    }

    @Override
    public IndexNames getAbstractIndexName() {
        return IndexNames.LEISTUNGSBESCHREIBUNG;
    }
}
