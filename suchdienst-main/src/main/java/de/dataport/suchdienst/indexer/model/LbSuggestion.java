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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LbSuggestion {

    private String lbId;
    private String suggest;

    private Collection<String> arsFederalstate;
    private Collection<String> langArsFederalstate;

    /**
     * Default language equals German
     */
    public LbSuggestion(String lbId, Collection<String> arsFederalstate, String suggest) {
        this.lbId = lbId;
        this.arsFederalstate = arsFederalstate;
        this.suggest = suggest;
        this.langArsFederalstate = arsFederalstateToLang(arsFederalstate, "de");
    }

    public LbSuggestion(
        String lbId,
        Collection<String> arsFederalstate,
        String suggest,
        String lang
    ) {
        Objects.requireNonNull(lang);
        this.lbId = lbId;
        this.arsFederalstate = arsFederalstate;
        this.suggest = suggest;
        this.langArsFederalstate = arsFederalstateToLang(arsFederalstate, lang);
    }

    private Collection<String> arsFederalstateToLang(
        Collection<String> arsFederalstate,
        String lang
    ) {
        if (arsFederalstate != null) {
            return arsFederalstate
                .stream()
                .map(ars -> lang + "-" + ars)
                .collect(Collectors.toSet());
        }

        return Collections.emptySet();
    }

    public static List<LbSuggestion> fromLanguageEntry(
        Locale locale,
        Set<String> suggestions,
        String lbId,
        Collection<String> arsFederalstate
    ) {
        if (suggestions == null || locale == null) {
            return Collections.emptyList();
        }

        return suggestions
            .stream()
            .map(suggest -> new LbSuggestion(lbId, arsFederalstate, suggest, locale.getLanguage()))
            .toList();
    }
}
