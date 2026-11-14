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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.Data;

@Data
public class ResponsibilityCriterion {

    private String name;
    private String code;
    private String uri;
    private String version;
    private List<String> auspraegungen;

    private Map<Locale, String> otherLanguages = new HashMap<>();
}
