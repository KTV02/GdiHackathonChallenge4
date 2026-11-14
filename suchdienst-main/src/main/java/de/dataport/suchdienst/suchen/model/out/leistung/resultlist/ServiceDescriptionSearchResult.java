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
package de.dataport.suchdienst.suchen.model.out.leistung.resultlist;

import de.dataport.suchdienst.suchen.model.out.leistung.LifeSituationAggOutModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedModel;

/**
 * Ausgabe-Modell der Beta-Such-API (/v8beta1/servicedescriptions). Es klammert die drei zentralen
 * Bestandteile der Antwort: die Seiteninformationen inklusive der Treffer über
 * {@link PagedModel} und die Facetten-Informationen (Lebenslagen-Aggregation).
 */
public record ServiceDescriptionSearchResult(
    @Schema(description = "Treffer der Leistungs-Suche inklusive Seiteninformationen.")
    PagedModel<ServiceDescriptionSearchHit> serviceDescriptions,
    Facades facades
) {
    public ServiceDescriptionSearchResult(
        Page<ServiceDescriptionSearchHit> serviceDescriptions,
        Set<LifeSituationAggOutModel> facades
    ) {
        this(new PagedModel<>(serviceDescriptions), new Facades(facades));
    }

    /**
     * Klammert verschiedene Objekte, die für die clientseitige Facettierung verwendet werden.
     * Aktuell sind das ausschließlich die Lebenslagen.
     */
    public record Facades(
        /*
        Pro Root-Lebenslagen (Bürger, Unternehmen) ein Aggregationswert mitsamt Aggregation auf Unter-Ebenen.
        */
        @Schema(description = "Aggregation der zu den Treffern gehörenden Lebenslagen.")
        Set<LifeSituationAggOutModel> personalMatters
    ) {}
}
