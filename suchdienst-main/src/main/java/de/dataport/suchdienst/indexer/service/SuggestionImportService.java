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

import de.dataport.suchdienst.elasticsearch.api.ElasticBulkWriteConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.elasticsearch.api.model.ElasticBulkError;
import de.dataport.suchdienst.indexer.model.LbSuggestion;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungVL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SuggestionImportService {

    private final ElasticBulkWriteConnector elasticBulkWriteConnector;

    public SuggestionImportService(ElasticBulkWriteConnector elasticBulkWriteConnector) {
        this.elasticBulkWriteConnector = elasticBulkWriteConnector;
    }

    public List<ElasticBulkError> importSuggestions(LeistungsbeschreibungVL lb) {
        // German Suggestions
        Stream<LbSuggestion> germanSuggestions = Stream.ofNullable(lb.getSuggestions())
            .flatMap(Collection::stream)
            .map(sug -> new LbSuggestion(lb.getId(), lb.getArsFederalstate(), sug));
        // English Suggestions
        Stream<LbSuggestion> otherSuggestions = Optional.ofNullable(lb.getOtherLanguages())
            .map(Map::entrySet)
            .orElseGet(Collections::emptySet)
            .stream()
            .flatMap(e ->
                LbSuggestion.fromLanguageEntry(
                    e.getKey(),
                    e.getValue().getSuggestions(),
                    lb.getId(),
                    lb.getArsFederalstate()
                ).stream()
            );
        List<LbSuggestion> suggestionsToImport = Stream.concat(
            germanSuggestions,
            otherSuggestions
        ).toList();

        if (!suggestionsToImport.isEmpty()) {
            return elasticBulkWriteConnector.bulkIndex(suggestionsToImport, IndexNames.SUGGESTIONS);
        }
        return List.of();
    }
}
