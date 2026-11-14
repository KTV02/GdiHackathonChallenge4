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
package de.dataport.suchdienst.util;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;

@Getter
public enum Bundeslaender {
    BUND("B100019", "00", "bd", "Bund"),
    SCHLESWIG_HOLSTEIN("L100012", "01", "sh", "Schleswig-Holstein"),
    HAMBURG("S100002", "02", "hh", "Hamburg"),
    NIEDERSACHSEN("L100040", "03", "ni", "Niedersachsen"),
    BREMEN("S100003", "04", "hb", "Bremen"),
    NORDRHEIN_WESTFALEN("L100002", "05", "nw", "Nordrhein-Westfalen"),
    HESSEN("L100001", "06", "he", "Hessen"),
    RHEINLAND_PFALZ("L100039", "07", "rp", "Rheinland-Pfalz"),
    BADEN_WUERTTEMBERG("L100022", "08", "bw", "Baden-Württemberg"),
    BAYERN("L100042", "09", "by", "Bayern"),
    SAARLAND("L100010", "10", "sl", "Saarland"),
    BERLIN("L100108", "11", "be", "Berlin"),
    BRANDENBURG("L100041", "12", "bb", "Brandenburg"),
    MECKLENBURG_VORPOMMERN("L100027", "13", "mv", "Mecklenburg-Vorpommern"),
    SACHSEN("L100009", "14", "sn", "Sachsen"),
    SACHSEN_ANHALT("L100008", "15", "st", "Sachsen-Anhalt"),
    THUERINGEN("L100038", "16", "th", "Thüringen");

    private final String schemeAgencyId;

    private final String arsFederalstate;

    private final String kuerzel;

    private final String federalstateName;

    Bundeslaender(
        String schemeAgencyId,
        String arsFederalstate,
        String kuerzel,
        String federalstateName
    ) {
        this.schemeAgencyId = schemeAgencyId;
        this.arsFederalstate = arsFederalstate;
        this.kuerzel = kuerzel;
        this.federalstateName = federalstateName;
    }

    /**
     * Liefert für eine SchemeAgencyId den Ars zurück. Ist der Ars zur SchemeAgencyId unbekannt wird ein leeres Optional zurück gegeben.
     */
    public static Optional<String> getArsBySchemeAgencyId(String schemeAgencyId) {
        var opt = findBySchemeAgencyId(schemeAgencyId);
        return opt.map(Bundeslaender::getArsFederalstate);
    }

    public static Bundeslaender getByArsFederalstate(String arsFederalState) {
        return Arrays.stream(Bundeslaender.values())
            .filter(v -> v.arsFederalstate.equals(arsFederalState))
            .findFirst()
            .orElseThrow(() ->
                new IllegalStateException(
                    String.format("Unbekannte ArsFederalstate %s", arsFederalState)
                )
            );
    }

    private static Optional<Bundeslaender> findBySchemeAgencyId(String schemeAgencyId) {
        return Arrays.stream(Bundeslaender.values())
            .filter(v -> v.schemeAgencyId.equals(schemeAgencyId))
            .findFirst();
    }

    public static boolean isBund(String schemeAgencyId) {
        return Bundeslaender.BUND.schemeAgencyId.equals(schemeAgencyId);
    }

    public String getLowerCaseName() {
        return name().toLowerCase();
    }
}
