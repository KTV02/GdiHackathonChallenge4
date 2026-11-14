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

import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Enthält die Daten zu einer Lebenslage und deren Sublagen. Seit PV-8656 sind Lebenslagen Bestandteil der Leistungsbeschreibung.
 */
@Data
@NoArgsConstructor
public class LifeSituation {

    /**
     * Code der Lebenslage gemäß Codeliste.
     */
    private String code;

    /**
     * Menge der Unter-Lebenslagen.
     */
    @ToString.Exclude
    private Set<LifeSituation> children = new HashSet<>();

    public LifeSituation(String code) {
        this.code = code;
    }
}
