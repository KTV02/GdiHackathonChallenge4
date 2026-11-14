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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * Repräsentiert einen Ars mit weiteren zugehörigen Daten
 *
 * @author PVOG
 */
@Data
public class ArsObject implements Identifiable {

    private static final int AGS_SECONDHALF_END = 12;
    private static final int AGS_SECONDHALF_BEGIN = 9;
    private static final int AGS_FIRSTHALF_END = 5;
    private static final int AGS_FIRSTHALF_BEGIN = 0;
    public static final int ARS_LENGTH = 12;

    @Data
    public static class Suggestions {

        private Set<String> input = new HashSet<>();
        private String weight;
    }

    private String name;
    private String ars;
    private String ags;

    private String displayZip;

    private Set<String> zipCodes = new HashSet<>();

    private Suggestions suggest = new Suggestions();

    @Override
    public void setId(String id) {
        setArs(id);
    }

    @Override
    public String getId() {
        return getArs();
    }

    public void setWeight(String w) {
        this.suggest.setWeight(w);
    }

    @JsonProperty("weight")
    public String getWeight() {
        return this.suggest.getWeight();
    }

    @JsonProperty("zipCodeCount")
    public int getZipCodeCount() {
        return zipCodes.size();
    }

    public void setArsAndAgs(String ars) {
        setArs(ars);
        setAgs(computeAgsFromArs(ars));
    }

    public void addSuggestion(String sugg) {
        this.suggest.getInput().add(sugg);
    }

    /**
     * Ersetzt die Postleitzahlen durch die übergebene Menge, sofern diese nicht leer ist.
     * <p>
     * Zusätzlich werden
     * <ul>
     * <li>das Feld {@link #displayZip} nach einer Heuristik angepasst, damit bei großen Städten nicht
     * nur die erste Postleitzahl eingetragen ist</li>
     * <li>die Suggestions neu berechnet (Vorschlag für jede PLZ und den Namen als solches)</li>
     * </ol>
     * </p>
     * Wenn die übergebene Menge leer ist, werden keine Veränderungen vorgenommen.
     *
     * @param zipCodes die Menge von Postleitzahlen, die für den Ort gelten soll
     * @return true gdw. sich die Menge der bisherigen PLZ dadurch verändert hat
     */
    public boolean replaceZipcodes(Set<String> zipCodes) {
        if (zipCodes.isEmpty()) {
            return false;
        }
        if (!this.zipCodes.equals(zipCodes)) {
            this.setZipCodes(zipCodes);
            determineDisplayZip(
                zipCodes.stream().sorted().findFirst().orElseThrow(),
                zipCodes.size()
            );

            final String nameCopy = this.name;
            Set<String> suggs = zipCodes
                .stream()
                .map(zipCode -> zipCode + " " + nameCopy)
                .collect(Collectors.toSet());
            suggs.add(this.name);
            this.suggest.input = suggs;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Diese Methode füllt das Feld {@link #displayZip} nach einer Heuristik.
     *
     * @param representativeZip Eine repräsentative Postleitzahl
     * @param zipCodeCount Die Anzahl der Postleitzahlen
     */
    private void determineDisplayZip(String representativeZip, int zipCodeCount) {
        StringBuilder newValue = new StringBuilder(representativeZip);

        if (zipCodeCount > 1) {
            // Neue vom PO gewünschte Heuristik: Bei mehr als 2 PLZ -> 2 Stellen hinten
            // entfernen...
            newValue = new StringBuilder(representativeZip.substring(0, 3));
            // Rest mit * auffüllen bis 5 Stellen erreicht sind.
            while (newValue.length() < 5) {
                newValue.append("*");
            }
        }
        this.setDisplayZip(newValue.toString());
    }

    private String computeAgsFromArs(String ars) {
        if (ars != null && StringUtils.length(ars) == ARS_LENGTH) {
            return (
                ars.substring(AGS_FIRSTHALF_BEGIN, AGS_FIRSTHALF_END) +
                ars.substring(AGS_SECONDHALF_BEGIN, AGS_SECONDHALF_END)
            );
        }
        throw new IllegalArgumentException("Not a valid ARS " + ars);
    }
}
