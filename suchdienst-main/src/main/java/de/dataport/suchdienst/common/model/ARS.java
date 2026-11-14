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
package de.dataport.suchdienst.common.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * Repraesentiert einen amtlichen Regionalschluessel. TODO erstmal nur als zentrale Stelle fuer
 * getArsHierarchy, perspektivisch ausbauen mit Validierung usw.
 */
public class ARS {

    public static final String BUND = "0".repeat(12);

    private final String arsKey;

    /**
     * Enthält die Ebenen, die zum Abtrennen ganzer "0" Hierarchy-Bereiche notwendig ist.
     * Siehe {@link ARS#hierarchyTrim()}
     */
    private static final List<Ebene> HIERARCHY_TRIM_LIST = List.of(
        Ebene.BEZIRK,
        Ebene.LANDKREIS,
        Ebene.VERBAND,
        Ebene.GEMEINDE
    );

    /**
     * Definiert die politischen Ebenen des ARS.
     */
    enum Ebene {
        // von hinten nach vorne definiert, damit wir direkt darueber iterieren koennen
        GEMEINDE(9, 11),
        VERBAND(5, 8),
        LANDKREIS(3, 4),
        BEZIRK(2, 2),
        LAND(0, 1);

        /**
         * Index innerhalb des ARS, in dem diese Ebene beginnt
         */
        private final int start;
        /**
         * Index innerhalb des ARS, in dem diese Ebene endet
         */
        private final int end;

        Ebene(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    public ARS(String arsKey) {
        this.arsKey = StringUtils.rightPad(arsKey, 12, '0');
    }

    public static ARS of(String arsKey) {
        return new ARS(arsKey);
    }

    /**
     * Liefert eine Menge von ARS-Schlüsseln, welche das Gebiet selber und alle Gebiete "oberhalb"
     * dieser ARS in der politischen Hierarchie darstellen. So sind oberhalb von Gemeinden z.B. die ARS
     * des Landkreises, ggf. Regierungsbezirk, Bundesland angesiedelt.
     *
     * @return die Hierarchie, niemals NULL
     */
    public SequencedCollection<String> getArsHierarchy() {
        // Hier ist eine geordnete Menge wichtig, da die speziellste (Gemeinde) ARS als erstes erwartet wird.
        SequencedCollection<String> hierarchy = new LinkedHashSet<>();
        if (StringUtils.isBlank(arsKey)) {
            return hierarchy;
        }
        hierarchy.add(arsKey);
        char[] tmp = arsKey.toCharArray();
        for (Ebene e : Ebene.values()) {
            Arrays.fill(tmp, e.start, e.end + 1, '0');
            hierarchy.add(new String(tmp));
        }
        return hierarchy;
    }

    /**
     * Verbindet die ARS-Hierarchy mit den IDs der Leistungen.
     */
    public Collection<String> getConcatedArsHierachy(boolean useHierarchy, String... lbIds) {
        Collection<String> arsToConcat = useHierarchy ? getArsHierarchy() : List.of(arsKey);
        return arsToConcat
            .stream()
            .flatMap(arsHierarchy -> {
                Set<String> lbIdsWithArs = new HashSet<>();
                for (String lbId : lbIds) {
                    lbIdsWithArs.add(lbId + "-" + arsHierarchy);
                }
                return lbIdsWithArs.stream();
            })
            .collect(Collectors.toSet());
    }

    /**
     * Entfernt die maximale Anzahl an Nullen vom Ende der ARS.
     * Berücksichtigt dabei die Hierarchy-Ebenen dergestalt,
     * dass eine Ebene nur dann entfernt wird, wenn sie komplett aus Nullen besteht
     */
    public String hierarchyTrim() {
        for (Ebene ebene : HIERARCHY_TRIM_LIST) {
            if (arsKey.endsWith("0".repeat(12 - ebene.start))) {
                return arsKey.substring(0, ebene.start);
            }
        }
        return arsKey;
    }

    /**
     * Prüft, ob zwei ARS-Schlüssel in einer hierarchischen Beziehung stehen.
     * Dies ist der Fall, wenn einer der getrimmten Schlüssel ein Präfix des anderen ist.
     *
     * <p>Aufwärts: {@code matchesHierarchy("091620000000", "091")} → {@code true}
     * <p>Abwärts: {@code matchesHierarchy("091", "091620000000")} → {@code true}
     *
     * @param ars1 Erster ARS-Schlüssel
     * @param ars2 Zweiter ARS-Schlüssel
     * @return {@code true} wenn die Schlüssel hierarchisch verwandt sind
     */
    public static boolean matchesHierarchy(String ars1, String ars2) {
        if (StringUtils.isBlank(ars1) || StringUtils.isBlank(ars2)) {
            return false;
        }
        String trimmed1 = ARS.of(ars1).hierarchyTrim();
        String trimmed2 = ARS.of(ars2).hierarchyTrim();
        return trimmed1.startsWith(trimmed2) || trimmed2.startsWith(trimmed1);
    }
}
