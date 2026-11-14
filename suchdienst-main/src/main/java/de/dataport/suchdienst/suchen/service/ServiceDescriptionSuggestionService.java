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
package de.dataport.suchdienst.suchen.service;

import co.elastic.clients.elasticsearch.core.search.CompletionSuggest;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import de.dataport.suchdienst.elasticsearch.api.ElasticPublicSearchConnector;
import de.dataport.suchdienst.indexer.model.LbSuggestion;
import de.dataport.suchdienst.suchen.model.out.leistung.ServiceDescriptionSuggestion;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ServiceDescriptionSuggestionService {

    private final ElasticPublicSearchConnector publicSearchConnector;

    public ServiceDescriptionSuggestionService(ElasticPublicSearchConnector publicSearchConnector) {
        this.publicSearchConnector = publicSearchConnector;
    }

    public List<ServiceDescriptionSuggestion> searchSuggestions(
        String searchValue,
        String ars,
        Boolean skipDuplicates,
        Locale locale
    ) {
        List<CompletionSuggest<LbSuggestion>> suggestion =
            publicSearchConnector.suggestServiceDescription(
                searchValue,
                ars,
                skipDuplicates,
                locale
            );

        List<ServiceDescriptionSuggestion> suggestions = new ArrayList<>();

        for (CompletionSuggest<LbSuggestion> entry : suggestion) {
            for (CompletionSuggestOption<LbSuggestion> option : entry.options()) {
                LbSuggestion sug = option.source();
                if (Objects.isNull(sug)) {
                    continue;
                }

                ServiceDescriptionSuggestion obj = new ServiceDescriptionSuggestion();
                obj.setSuggestedText(sug.getSuggest());
                addSuggestion(suggestions, obj, skipDuplicates);
                if (suggestions.size() == 5) {
                    // Mehr als 5 wollen wir nicht anzeigen, siehe PV-7587.
                    return suggestions;
                }
            }
        }
        return suggestions;
    }

    /**
     * Fügt den Vorschlag hinzu, wenn er noch nicht vorhanden ist. Falls der Vorschlag schon vorhanden
     * ist, wird derjenige übernommen in dem Großbuchstaben enthalten sind. Der andere wird ggf. gelöscht.
     */
    private void addSuggestion(
        List<ServiceDescriptionSuggestion> suggestions,
        ServiceDescriptionSuggestion obj,
        boolean skipDuplicates
    ) {
        if (!skipDuplicates) {
            suggestions.add(obj);
        } else {
            Optional<ServiceDescriptionSuggestion> equalSuggestion = suggestions
                .stream()
                .filter(existingSuggestion ->
                    existingSuggestion.getSuggestedText().equalsIgnoreCase(obj.getSuggestedText())
                )
                .findAny();
            if (equalSuggestion.isEmpty()) {
                suggestions.add(obj);
            } else if (!obj.getSuggestedText().toLowerCase().equals(obj.getSuggestedText())) {
                suggestions.remove(equalSuggestion.get());
                suggestions.add(obj);
            }
        }
    }
}
