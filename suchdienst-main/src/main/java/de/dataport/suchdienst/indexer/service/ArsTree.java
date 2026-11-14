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
package de.dataport.suchdienst.indexer.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * ARS-Hierarchie-Auflösung mit flexibler Baum-Datenstruktur und Performance-Optimierung.
 *
 * <p>Initialisierung über einer Menge von Gemeinde-ARS. Abfragen liefern dann:</p>
 * <ul>
 *   <li>Für ARS auf Ebenen LAND, KREIS, ... → alle darunterliegenden Gemeinde-ARS</li>
 *   <li>Für ARS auf Gemeinde-Level → diese Gemeinde selbst</li>
 *   <li>Für unbekannte ARS → diese ARS unverändert zurück</li>
 * </ul>
 */
@Slf4j
public class ArsTree {

    /** Standardlänge eines ARS-Codes (Amtlicher Regionalschlüssel) */
    private static final int ARS_LENGTH = 12;

    /** Hierarchie-Reihenfolge der politischen Ebenen */
    private static final List<Ebene> HIERARCHY_ORDER = List.of(
        Ebene.LAND,
        Ebene.REGIERUNGS_BEZIRK,
        Ebene.KREIS,
        Ebene.GEMEINDE_VERBAND,
        Ebene.GEMEINDE
    );

    private final ArsNode arsRoot;
    private final Map<String, Set<String>> hierarchyCache;

    public ArsTree(Set<String> gemeindeArs) {
        // 1. Baum-Struktur aufbauen (immer vorhanden für Flexibilität)
        this.arsRoot = new ArsNode(HIERARCHY_ORDER);
        gemeindeArs.forEach(arsRoot::addArs);

        // 2. Performance-Cache vorberechnen
        this.hierarchyCache = precomputeHierarchyCache();

        log.debug(
            "ArsTree initialisiert mit {} Gemeinden, {} Cache-Einträge",
            gemeindeArs.size(),
            hierarchyCache.size()
        );
    }

    /**
     * Liefert alle Gemeinden, die unterhalb des gegebenen ARS liegen.
     * Für unbekannte ARS wird der ursprüngliche ARS als Fallback zurückgegeben.
     *
     * @param ars Der ARS-Code zur Auflösung
     * @return Set der Gemeinde-ARS oder der ursprüngliche ARS als Fallback
     */
    public Set<String> resolveToGemeinden(String ars) {
        if (!isValidArs(ars)) {
            throw new IllegalArgumentException("Ungültiger ARS-Code: " + ars);
        }

        return hierarchyCache.getOrDefault(ars, Set.of(ars));
    }

    private boolean isValidArs(String ars) {
        return ars != null && ars.length() == ARS_LENGTH && ars.matches("\\d{12}");
    }

    private Map<String, Set<String>> precomputeHierarchyCache() {
        Map<String, Set<String>> cache = new HashMap<>();
        Set<String> allGemeinden = arsRoot.getAllLeafs();

        for (String gemeinde : allGemeinden) {
            for (Ebene ebene : HIERARCHY_ORDER) {
                String arsForEbene = buildArsForEbene(gemeinde, ebene);
                cache.computeIfAbsent(arsForEbene, k -> new HashSet<>()).add(gemeinde);
            }
        }

        // Unveränderliche Sets für Thread-Safety
        cache.replaceAll((ars, gemeinden) -> Set.copyOf(gemeinden));
        return Map.copyOf(cache);
    }

    private static String buildArsForEbene(String gemeinde, Ebene ebene) {
        String arsPart = gemeinde.substring(ebene.getStart(), ebene.getEnd() + 1);
        return StringUtils.rightPad(arsPart, ARS_LENGTH, '0');
    }

    /**
     * Definiert die politischen Ebenen des ARS mit ihren Positionen im 12-stelligen ARS-Code.
     */
    @Getter
    public enum Ebene {
        /** Gemeinde: Stellen 1-12 */
        GEMEINDE(0, 11),
        /** Gemeindeverband: Stellen 1-9 */
        GEMEINDE_VERBAND(0, 8),
        /** Kreis: Stellen 1-5 */
        KREIS(0, 4),
        /** Regierungsbezirk: Stellen 1-3 */
        REGIERUNGS_BEZIRK(0, 2),
        /** Land: Stellen 1-2 */
        LAND(0, 1);

        private final int start;
        private final int end;

        Ebene(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    /**
     * Flexibler Baum-Knoten für ARS-Hierarchie.
     * Ermöglicht erweiterte Operationen und ist erweiterbar für zukünftige Anforderungen.
     */
    @EqualsAndHashCode
    @ToString
    public static class ArsNode {

        private final Map<String, ArsNode> children = new HashMap<>();
        private final Ebene currentEbene;

        @ToString.Exclude
        private final List<Ebene> remainingEbenen;

        public ArsNode(List<Ebene> ebenen) {
            if (ebenen.isEmpty()) {
                throw new IllegalArgumentException("Ebenen-Liste darf nicht leer sein");
            }
            this.remainingEbenen = ebenen;
            this.currentEbene = ebenen.getFirst();
        }

        public void addArs(String ars) {
            if (currentEbene == Ebene.GEMEINDE) {
                // Blätter des Baumes erreicht - null signalisiert Blatt-Knoten
                children.put(ars, null);
            } else {
                // Terminierungs-Bedingung für Rekursion: Ebenen werden um eine reduziert
                List<Ebene> nextEbenen = remainingEbenen.subList(1, remainingEbenen.size());
                String arsEbene = getArsEbene(ars);
                ArsNode childNode = children.computeIfAbsent(arsEbene, k ->
                    new ArsNode(nextEbenen)
                );

                // Rekursives hinzufügen der ARS zum Knoten
                childNode.addArs(ars);
            }
        }

        public Set<String> getMatchingLeafs(String ars) {
            String arsEbene = getArsEbene(ars);
            ArsNode matchingChild = children.get(arsEbene);

            if (matchingChild == null) {
                // ARS gehört nicht zu diesem Teil-Baum
                return Set.of();
            }

            if (currentEbene == Ebene.GEMEINDE) {
                // ARS ist im Knoten enthalten → Blatt des Baums erreicht → Gemeinde wird zurückgegeben
                return Set.of(ars);
            } else {
                if (ars.equals(arsEbene)) {
                    // Gebe alle Blätter unterhalb dieser Ebene zurück
                    return matchingChild.getAllLeafs();
                } else {
                    // Suche im passenden Knoten fortsetzen
                    return matchingChild.getMatchingLeafs(ars);
                }
            }
        }

        public Set<String> getAllLeafs() {
            if (currentEbene == Ebene.GEMEINDE) {
                // Blätter des Baumes erreicht - Map-Keys sind die Gemeinde-ARS
                return Set.copyOf(children.keySet());
            } else {
                return children
                    .values()
                    .stream()
                    .map(ArsNode::getAllLeafs)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toUnmodifiableSet());
            }
        }

        private String getArsEbene(String ars) {
            // Extrahiert den Teil aus dem ARS, welcher der jeweiligen politischen Ebene entspricht.
            // Notwendig für die Bildung von Schlüsseln im Baum.
            return buildArsForEbene(ars, currentEbene);
        }
    }
}
