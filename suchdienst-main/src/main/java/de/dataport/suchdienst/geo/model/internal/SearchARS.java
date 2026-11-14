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
package de.dataport.suchdienst.geo.model.internal;

import de.dataport.suchdienst.common.model.ArsHierarchyEbene;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class SearchARS {

    /**
     * Ein Schlüssel zur Verwendung am BKG, der immer mind. aus dem (kompletten) ARS besteht
     */
    @NonNull
    private String schluessel;

    /**
     * Ein angepasster ARS
     */
    private String processedArs;

    private ArsHierarchyEbene arsEbene;
}
