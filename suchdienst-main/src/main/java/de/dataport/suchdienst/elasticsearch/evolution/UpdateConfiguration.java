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
package de.dataport.suchdienst.elasticsearch.evolution;

import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.elasticsearch.evolution.description.IndexDescriptionAffectedLbIds;
import de.dataport.suchdienst.elasticsearch.evolution.description.IndexDescriptionArs;
import de.dataport.suchdienst.elasticsearch.evolution.description.IndexDescriptionCodeList;
import de.dataport.suchdienst.elasticsearch.evolution.description.IndexDescriptionDeletedObjects;
import de.dataport.suchdienst.elasticsearch.evolution.description.IndexDescriptionFormulare;
import de.dataport.suchdienst.elasticsearch.evolution.description.IndexDescriptionImportQueue;
import de.dataport.suchdienst.elasticsearch.evolution.description.IndexDescriptionLeikaEntry;
import de.dataport.suchdienst.elasticsearch.evolution.description.IndexDescriptionLeistungsbeschreibungen;
import de.dataport.suchdienst.elasticsearch.evolution.description.IndexDescriptionOnlinedienste;
import de.dataport.suchdienst.elasticsearch.evolution.description.IndexDescriptionOnlinediensteRelationen;
import de.dataport.suchdienst.elasticsearch.evolution.description.IndexDescriptionOrganisationseinheiten;
import de.dataport.suchdienst.elasticsearch.evolution.description.IndexDescriptionSpezialisierungen;
import de.dataport.suchdienst.elasticsearch.evolution.description.IndexDescriptionUpdateIndexSuggestions;
import de.dataport.suchdienst.elasticsearch.evolution.description.IndexDescriptionVerwaltungsleistung;
import de.dataport.suchdienst.elasticsearch.evolution.description.IndexDescriptionZustaendigkeiten;
import de.dataport.suchdienst.elasticsearch.evolution.description.IndexMigrationArsV1V2;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.context.annotation.Configuration;

/**
 * Klasse in der die Beschreibungen für Indexupdates eingetragen werden sollen
 */
@Configuration
public class UpdateConfiguration {

    final ElasticReadConnector elasticReadConnector;

    public UpdateConfiguration(ElasticReadConnector elasticReadConnector) {
        this.elasticReadConnector = elasticReadConnector;
    }

    public List<DefaultDescription> getDefaultUpdateDescriptions() {
        List<DefaultDescription> indexUpdateDescriptions = List.of(
            new IndexDescriptionArs(),
            new IndexDescriptionCodeList(),
            new IndexDescriptionImportQueue(),
            new IndexDescriptionLeikaEntry(),
            new IndexDescriptionDeletedObjects(),
            new IndexDescriptionUpdateIndexSuggestions(),
            new IndexDescriptionLeistungsbeschreibungen(),
            new IndexDescriptionSpezialisierungen(),
            new IndexDescriptionOrganisationseinheiten(),
            new IndexDescriptionOnlinedienste(),
            new IndexDescriptionOnlinediensteRelationen(),
            new IndexDescriptionZustaendigkeiten(),
            new IndexDescriptionAffectedLbIds(),
            new IndexDescriptionFormulare()
        );
        var indexUpdateDescriptionsVerwaltungsleistung = IndexNames.ARS_MAPPING.keySet()
            .stream()
            .map(IndexDescriptionVerwaltungsleistung::new)
            .toList();
        return Stream.concat(
            indexUpdateDescriptions.stream(),
            indexUpdateDescriptionsVerwaltungsleistung.stream()
        ).toList();
    }

    public List<IndexMigrationDescription> getMigrationDescriptions() {
        return List.of(new IndexMigrationArsV1V2());
    }

    public List<IndexUpdateDescription> getDeleteDescriptions() {
        Stream<IndexUpdateDescription> deleteDescriptions = Stream.of();
        Stream<IndexUpdateDescription> vlDeleteDescriptions = IndexNames.ARS_MAPPING.keySet()
            .stream()
            .flatMap(arsFederalstate ->
                Stream
                    // Hier können die DeleteDescriptions auf Verwaltungsleistung-Indizes eingetragen werden.
                    // Diese müssen für alle ARS ausgeführt werden.
                    .empty()
            )
            .map(IndexUpdateDescription.class::cast);
        return Stream.concat(deleteDescriptions, vlDeleteDescriptions).toList();
    }
}
