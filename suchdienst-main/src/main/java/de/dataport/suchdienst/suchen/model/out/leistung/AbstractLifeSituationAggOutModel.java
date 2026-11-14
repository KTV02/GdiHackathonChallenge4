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
package de.dataport.suchdienst.suchen.model.out.leistung;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collection;
import lombok.Data;

/**
 * PV-9431: Extrahiert aus {@link LifeSituationAggOutModel}.
 */
@Data
public abstract class AbstractLifeSituationAggOutModel<T extends AbstractLifeSituationAggOutModel<T>> {

    /**
     * Code der Lebenslage gemäß Codeliste.
     */
    @Schema(description = "Code der Lebenslage gemäß Codeliste")
    private String code;

    /**
     * Der für den Client internationalisierte Name der Lebenslage
     */
    @Schema(description = "Der für den Client internationalisierte Name der Lebenslage")
    private String name;

    /**
     * Anzahl der Lebenslagen aggregiert auf die Hierarchie-Ebene.
     */
    @Schema(description = "Anzahl der Lebenslagen aggregiert auf die Hierarchie-Ebene")
    private long count;

    /**
     * Menge der Unter-Lebenslagen.
     */
    @Schema(description = "Die untergeordneten Lebenslagen")
    private Collection<T> children; // PV-8997: Collection statt Set wg. swagger
}
