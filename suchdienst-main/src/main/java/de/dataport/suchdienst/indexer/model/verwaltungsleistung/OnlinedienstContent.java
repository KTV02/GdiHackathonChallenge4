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
import de.dataport.suchdienst.indexer.model.mapping.Link;
import de.dataport.suchdienst.indexer.model.mapping.LinkDynamic;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;

@Data
public class OnlinedienstContent implements HasSearchTerms {

    private String name;
    private String description;
    private String helpText;
    private List<LinkDynamic> links = new ArrayList<>();
    private List<Link> helpLinks = new ArrayList<>();
    private Publisher publisher;
    private Set<String> paymentMethods = new HashSet<>();
    private Set<String> identificationMethods = new HashSet<>();
    private List<OnlinedienstParameter> parametersEfa = new ArrayList<>();
    private List<OnlinedienstParameter> parametersCustom = new ArrayList<>();

    /**
     * Feld für die Suche nach Leistungen.
     * Wird zur Aggregation während der Verarbeitung der Geschäftsobjekte benötigt und nicht in Elasticsearch gespeichert.
     * Stattdessen wird eine aggregierte Version in {@link Verwaltungsleistung#searchTerms} gespeichert.
     */
    @JsonIgnore
    private SearchTerms searchTerms;
}
