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
package de.dataport.suchdienst.indexer.model;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * Beschreibt die Struktur einer CSV-Datei mit Postleitzahlen und Regionalschluesseln (ARS oder
 * AGS).
 */
@Data
@Builder
@JsonDeserialize(builder = ZipcodeCsvDefinition.ZipcodeCsvDefinitionBuilder.class)
public class ZipcodeCsvDefinition {

    /**
     * Das verwendete Trennzeichen.
     */
    @Builder.Default
    private String delimiter = ";";

    /**
     * Ob die Datei eine Header-Zeile enthaelt.
     */
    private boolean headerLine;
    /**
     * Index (0-basiert) der Regionalschluessel-Spalte
     */
    private int rsColumn;
    /**
     * Index der PLZ-Spalte.
     */
    private int zipColumn;
    /**
     * ARS oder AGS?
     */
    private RegionalKeyKind rsKind;

    public void addZip(Map<String, Set<String>> rsToZips, String line) {
        String[] columns = line.split(delimiter);
        rsToZips.computeIfAbsent(columns[rsColumn], k -> new HashSet<>()).add(columns[zipColumn]);
    }

    public boolean validateLine(String line) {
        int lastColumnIndex = StringUtils.countMatches(line, delimiter);
        return lastColumnIndex >= Math.max(rsColumn, zipColumn);
    }
}
