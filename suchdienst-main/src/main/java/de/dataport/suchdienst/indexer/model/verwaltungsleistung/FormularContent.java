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
package de.dataport.suchdienst.indexer.model.verwaltungsleistung;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Klasse für die sprachabhängigen Feldinhalte eines Formulars. Dient sowohl als Superklasse für
 * FormularVL (hält dort die deutschen Feldinhalte) als auch als Holder für die Feldinhalten in anderen Sprachen
 * innerhalb FormularVL.
 */
@Data
public class FormularContent {

    private String name;
    private String description;

    // Links zu den eigentlichen Formulardateien. Wir unterstützen nur XZufi-FormulardateiLink,
    // andere Link-Arten werden ignoriert.
    private List<FormularFileLink> fileLinks = new ArrayList<>();

    public void addFileLink(String mimeType, String uri) {
        fileLinks.add(new FormularFileLink(mimeType, uri));
    }
}
