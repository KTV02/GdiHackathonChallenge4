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
package de.dataport.suchdienst.indexer.model;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Enthält zu einer SchemeAgencyId die Menge der dort geänderten LBs.
 * Diese Entries speichern wir im Index #link {@link de.dataport.suchdienst.elasticsearch.api.IndexNames}, um den State des
 * #link {@link de.dataport.suchdienst.indexer.service.AffectedLbResolver} zu persistieren und damit
 * Wiederanlauffähigkeit und Konsistenz für die Updates des VL-Index zu erreichen.
 */
@Data
@AllArgsConstructor
public class AffectedLbIdsEntry implements Identifiable {

    private String schemeAgencyId;
    private String producer;
    private final String arsFederalState;
    private final Set<String> lbIds;

    @Override
    public String getId() {
        return this.schemeAgencyId;
    }

    @Override
    public void setId(String id) {
        this.schemeAgencyId = id;
    }

    public int size() {
        return lbIds.size();
    }
}
