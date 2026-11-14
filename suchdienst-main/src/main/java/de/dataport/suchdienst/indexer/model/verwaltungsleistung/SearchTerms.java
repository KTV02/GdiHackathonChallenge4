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

import java.util.Set;

/**
 * Felder für die Suche nach Verwaltungsleistungen.
 * @param searchNames        Fasst die Felder {@code name} und {@code nameOtherDescription} von Leistung und Spezialisierung für Elasticsearch zusammen.
 * @param searchDescriptions Fasst die Inhalte von {@code Beschreibung} und {@code Teaser} von Leistung und Spezialisierung für Elasticsearch zusammen.
 * @param searchValues       Fasst die Inhalte von:
 *                           <ul>
 *                             <li>Leistung: Ursprungsportal-Titel und Begriff im Kontext (Synonyme)</li>
 *                             <li>Spezialisierung: Ursprungsportal-Titel und Begriff im Kontext (Synonyme)</li>
 *                             <li>Onlinedienst: Felder {@code name} und {@code description}</li>
 *                           </ul>
 *                           für Elasticsearch zusammen.
 */
public record SearchTerms(
    Set<String> searchNames,
    Set<String> searchDescriptions,
    Set<String> searchValues
) {}
