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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import de.dataport.suchdienst.config.ResourceBundleConfig;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.config.ThreadPoolTaskConfiguration;
import de.dataport.suchdienst.elasticsearch.api.ElasticPublicSearchConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexConfiguration;
import de.dataport.suchdienst.elasticsearch.api.IndexRefreshMgr;
import de.dataport.suchdienst.elasticsearch.api.model.PersonalMattersMode;
import de.dataport.suchdienst.suchen.exception.SuchenException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
    classes = {
        JacksonAutoConfiguration.class,
        ElasticPublicSearchConnector.class,
        SuchdienstProperties.class,
        ResourceBundleConfig.class,
    }
)
@MockitoBean(types = { IndexRefreshMgr.class, ThreadPoolTaskConfiguration.class })
class EmptySearchTest {

    @MockitoBean
    private IndexConfiguration indexCfg;

    @MockitoBean
    private ElasticsearchClient esClient;

    @Autowired
    private ElasticPublicSearchConnector readConn;

    @BeforeEach
    void setupMocks() throws IOException {
        when(indexCfg.getCurrentIndexName(any())).thenReturn("index");
        when(indexCfg.getCurrentReadAliasName(any())).thenReturn("index");
        final SearchResponse response = mock(SearchResponse.class);
        when(response.hits()).thenReturn(HitsMetadata.of(h -> h.hits(List.of())));
        when(esClient.search(any(SearchRequest.class), any())).then(invocation -> response);
    }

    @Test
    // searchServiceDescription ist die einzige Methode innerhalb des assertThrows, elche ien Runtime Exception wirft
    @SuppressWarnings("java:S5778")
    void checkEmptyParams() {
        final SuchenException ex = assertThrows(SuchenException.class, () ->
            searchServiceDescription(emptyList(), emptyList(), emptySet())
        );
        assertThat(
            ex.getMessage(),
            is("Suche muss mindestens Suchbegriffe, Leika-IDs oder Lebenslagen enthalten!")
        );
    }

    @Test
    void checkLifeSituation() {
        assertDoesNotThrow(() ->
            searchServiceDescription(emptyList(), emptyList(), singleton("0100"))
        );
    }

    @Test
    void checkSearchValue() {
        assertDoesNotThrow(() ->
            searchServiceDescription(emptyList(), singleton("foo"), emptySet())
        );
    }

    @Test
    void checkLeikaId() {
        assertDoesNotThrow(() ->
            searchServiceDescription(singleton("foo"), emptyList(), emptySet())
        );
    }

    private void searchServiceDescription(
        Collection<String> leikaIds,
        Collection<String> searchValues,
        Set<String> lifeSituationCodes
    ) {
        readConn.searchServiceDescription(
            leikaIds,
            searchValues,
            emptySet(),
            null,
            true,
            lifeSituationCodes,
            PersonalMattersMode.NONE,
            0,
            1,
            null,
            false,
            LocalDate.now()
        );
    }
}
