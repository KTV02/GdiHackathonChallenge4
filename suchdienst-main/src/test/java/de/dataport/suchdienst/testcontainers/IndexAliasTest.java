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
package de.dataport.suchdienst.testcontainers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.AliasDefinition;
import co.elastic.clients.elasticsearch.indices.DeleteAliasRequest;
import co.elastic.clients.elasticsearch.indices.get_alias.IndexAliases;
import de.dataport.suchdienst.elasticsearch.api.ElasticInternalReadConnector;
import de.dataport.suchdienst.elasticsearch.api.exception.ElasticSearchException;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.geo.service.GeoService;
import de.dataport.suchdienst.importer.service.QueueImportService;
import de.dataport.suchdienst.indexer.service.ArsService;
import de.dataport.suchdienst.indexer.service.ElasticIndexService;
import de.dataport.suchdienst.indexer.service.LeikaService;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoBeans;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@MockitoBeans(
    value = {
        // Kein ARS/PLZ Import
        @MockitoBean(types = ArsService.class),
        // Kein Leistungskatalog Import
        @MockitoBean(types = LeikaService.class),
        // Keine Queue-Verarbeitung
        @MockitoBean(types = QueueImportService.class),
        // Keine initialen Codelist-Importe über GeoService
        @MockitoBean(types = GeoService.class),
    }
)
class IndexAliasTest extends AbstractElasticsearchContainerTest {

    @Autowired
    private ElasticIndexService elasticIndexService;

    @Autowired
    private ElasticsearchClient client;

    @Autowired
    private ElasticInternalReadConnector elasticReadConnector;

    @Test
    void checkAliasTest() {
        String indexNameSL = "suchdienst_verwaltungsleistungen_sl9";
        String readAliasNameSL = "read_alias_suchdienst_verwaltungsleistungen_sl9";

        String indexNameBY = "suchdienst_verwaltungsleistungen_by9";
        String readAliasNamBY = "read_alias_suchdienst_verwaltungsleistungen_by9";
        String writeAliasNamBY = "write_alias_suchdienst_verwaltungsleistungen_by9";

        //
        // Setup
        // Loesche read alias fuer saarland und bayern, write alias fuer bayern
        deleteIndexAlias(List.of(readAliasNameSL, readAliasNamBY, writeAliasNamBY));
        checkAliasExistence(indexNameSL, readAliasNameSL, false);
        checkAliasExistence(indexNameBY, readAliasNamBY, false);
        checkAliasExistence(indexNameBY, writeAliasNamBY, false);

        // Treatment
        // fehlende aliase neu erstellen - zu testende Komponente
        elasticIndexService.checkAndRepairIndicesAliasConfig();

        //
        // Test
        // check alias neu angelegt
        checkAliasExistence(indexNameSL, readAliasNameSL, true);
        checkAliasExistence(indexNameBY, readAliasNamBY, true);
        checkAliasExistence(indexNameBY, writeAliasNamBY, true);
    }

    private void deleteIndexAlias(List<String> aliasNames) {
        aliasNames.forEach(aliasName -> {
            try {
                DeleteAliasRequest deleteAliasRequest = DeleteAliasRequest.of(builder ->
                    builder.name(aliasName).index("suchdienst_*")
                );
                client.indices().deleteAlias(deleteAliasRequest);
            } catch (IOException ex) {
                throw new ElasticSearchException(
                    ErrorCodeEnum.ELSE_IO_ERROR,
                    "Fehler beim Löeschen des Indexalias " + aliasName,
                    ex
                );
            }
        });
    }

    private void checkAliasExistence(String indexName, String aliasName, boolean existence) {
        Optional<IndexAliases> aliasForIndex = elasticReadConnector.getAliasForIndex(indexName);
        Optional<AliasDefinition> oAliasDefinition = aliasForIndex
            .map(IndexAliases::aliases)
            .map(aliases -> aliases.get(aliasName));

        if (existence) {
            assertTrue(oAliasDefinition.isPresent());
        } else {
            assertFalse(oAliasDefinition.isPresent());
        }
    }
}
