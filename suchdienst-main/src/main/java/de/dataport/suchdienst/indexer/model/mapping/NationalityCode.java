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

import lombok.Builder;
import lombok.Data;

/**
 * Repräsentiert eine Nationalität.
 */
@Builder
@Data
public class NationalityCode {

    /**
     * Der Code vom Destatis, der im XZuFI geliefert wird.
     */
    private String destatisCode;

    /**
     * Der zugehörige ISO2-Code, sofern vorhanden. Kann leer sein, weil es in der Destatis-Liste
     * Einträge gibt, die keinen ISO-Code haben (z.B. Palästina) oder weil der Code nicht aufgelöst
     * werden konnte.
     */
    private String iso2Code;
}
