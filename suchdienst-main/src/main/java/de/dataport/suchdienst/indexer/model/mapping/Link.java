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

/**
 * Representation eines {@link de.xoev.xzufi.v2_3_1.HyperlinkErweitert}.
 * Wird typischerweise verwendet um "weiterführende Links" zu speichern.
 */
public record Link(String uri, String title, String description) {
    public static Link of(String uri, String title, String description) {
        return new Link(uri, title, description);
    }
}
