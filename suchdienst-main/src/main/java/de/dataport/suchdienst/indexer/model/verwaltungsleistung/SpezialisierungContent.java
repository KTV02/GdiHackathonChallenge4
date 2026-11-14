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

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class SpezialisierungContent implements HasSearchTerms {

    private Map<String, List<ServiceBlockSpecialization>> details = new HashMap<>();

    /**
     * Feld für die Suche nach Leistungen.
     * Wird zur Aggregation während der Verarbeitung der Geschäftsobjekte benötigt und nicht in Elasticsearch gespeichert.
     * Stattdessen wird eine aggregierte Version in {@link Verwaltungsleistung#searchTerms} gespeichert.
     */
    @JsonIgnore
    private SearchTerms searchTerms;
}
