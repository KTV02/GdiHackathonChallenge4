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
package de.dataport.suchdienst.indexer.model.mapping;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation eines Links mit der Möglichkeit dynamischer Parameter.
 */
public record LinkDynamic(
    String uri,
    String title,
    String description,
    List<DynamischerParameter> dynamicParameters
) {
    public static LinkDynamic of(String uri, String title, String description) {
        return new LinkDynamic(uri, title, description, new ArrayList<>());
    }

    public static LinkDynamic of(
        String uri,
        String title,
        String description,
        List<DynamischerParameter> dynamicParameters
    ) {
        return new LinkDynamic(uri, title, description, dynamicParameters);
    }
}
