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
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Enthält einen Baum von Lebenslagen und Unterlagen, wird seit PV-8450 mit den Details zu einer Leistungsbeschreibung ausgeliefert.
 */
@Data
@AllArgsConstructor
public class LifeSituationOutModel {

    /**
     * Code der Lebenslage gemäß Codeliste.
     */
    @Schema(
        description = "Code der Lebenslage gemäß Codeliste",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String code;

    /**
     * Der für den Client internationalisierte Name der Lebenslage
     */
    @Schema(
        description = "Der für den Client internationalisierte Name der Lebenslage",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;

    /**
     * Menge der Unter-Lebenslagen.
     */
    @Schema(description = "Menge der Unter-Lebenslagen")
    private Collection<LifeSituationOutModel> children; // PV-8997: Collection statt Set wg. swagger
}
