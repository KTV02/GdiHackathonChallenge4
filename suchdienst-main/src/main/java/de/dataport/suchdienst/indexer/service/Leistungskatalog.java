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

import static java.util.stream.Collectors.toMap;

import de.dataport.suchdienst.indexer.model.CodelistKey;
import de.dataport.suchdienst.indexer.model.LeikaEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Der Leistungskatalog enthält ein mapping von Leika-Schlüsseln auf Lebenslagen (1:N) in Form von Codelisten-Einträgen.
 * Für jeden Leika-Schlüssel enthält der Leistungskatalog alle Hierarchie-Ebenen der zugeordneten Lebenslagen.
 */
@Slf4j
public class Leistungskatalog {

    public static final String PV_LAGEN_URI = "urn:xoev-de:fim:codeliste:pvlagen";

    private final Map<String, LeikaEntry> leikaEntriesByLeikaId;

    @Getter
    private final SortedSet<String> codelistVersions;

    public Leistungskatalog(List<LeikaEntry> leikaEntries) {
        leikaEntriesByLeikaId = leikaEntries
            .stream()
            .collect(toMap(LeikaEntry::getId, Function.identity()));
        codelistVersions = determineLeistungskatalogCodeListVersions(leikaEntriesByLeikaId);
    }

    public LeikaEntry getLeikaEntry(String leikaKey) {
        return leikaEntriesByLeikaId.get(leikaKey);
    }

    public boolean isEmpty() {
        return leikaEntriesByLeikaId.isEmpty();
    }

    /**
     * Liefert diejenige PVLagen Codelisten Version, die im Leistungskatalog verwendet werden.
     * Sortiert von der aktuellsten Version zur ältesten Version
     */
    private SortedSet<String> determineLeistungskatalogCodeListVersions(
        Map<String, LeikaEntry> leistungskatalog
    ) {
        return leistungskatalog
            .values()
            .stream()
            .map(LeikaEntry::getCodelistKeys)
            .flatMap(Collection::stream)
            .flatMap(Collection::stream)
            .map(CodelistKey::version)
            .collect(Collectors.toCollection(() -> new TreeSet<>(Collections.reverseOrder())));
    }
}
