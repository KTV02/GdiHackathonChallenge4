/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.suchen.model.out.leistung.resultlist;

import de.dataport.suchdienst.suchen.model.out.leistung.LeikaMatch;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedModel;

/**
 * Ausgabe-Modell der Beta-Suche über die LeiKa-Hierarchie
 * (/v3beta1/servicedescriptions/leikahierarchy). Neben den paginierten Treffern wird über
 * {@link LeikaMatch} transportiert, ob die Treffer exakt zu den angefragten LeiKa-Schlüsseln passen
 * oder über die Hierarchie ermittelt wurden.
 */
public record ServiceDescriptionHierarchySearchResult(
    @Schema(description = "Treffer der Leistungs-Suche inklusive Seiteninformationen.")
    PagedModel<ServiceDescriptionSearchHit> serviceDescriptions,

    @Schema(
        description = "Gibt an, wie die Treffer zu den angefragten LeiKa-Schlüsseln ermittelt wurden."
    )
    LeikaMatch leikaMatch
) {
    public ServiceDescriptionHierarchySearchResult(
        Page<ServiceDescriptionSearchHit> serviceDescriptions,
        LeikaMatch leikaMatch
    ) {
        this(new PagedModel<>(serviceDescriptions), leikaMatch);
    }
}
