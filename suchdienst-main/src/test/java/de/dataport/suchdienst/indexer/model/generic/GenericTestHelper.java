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
package de.dataport.suchdienst.indexer.model.generic;

public interface GenericTestHelper {
    default de.xoev.xzufi.v2_2_0.Identifikator getId22() {
        var id = new de.xoev.xzufi.v2_2_0.Identifikator();
        id.setValue("testIdValue");
        id.setSchemeAgencyID("testSchemeAgencyID");
        return id;
    }

    default de.xoev.xzufi.v2_3_1.Identifikator getId23() {
        var id = new de.xoev.xzufi.v2_3_1.Identifikator();
        id.setValue("testIdValue");
        id.setSchemeAgencyID("testSchemeAgencyID");
        return id;
    }
}
