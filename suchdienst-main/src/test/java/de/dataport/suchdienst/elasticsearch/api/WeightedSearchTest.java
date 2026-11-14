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
package de.dataport.suchdienst.elasticsearch.api;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.config.SuchdienstProperties.Boost;
import de.dataport.suchdienst.config.ThreadPoolTaskConfiguration;
import de.dataport.suchdienst.elasticsearch.api.model.PersonalMattersMode;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.Verwaltungsleistung;
import de.dataport.suchdienst.util.DevAsserter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest(
    classes = {
        JacksonAutoConfiguration.class,
        ElasticPublicSearchConnector.class,
        IndexRefreshMgr.class,
        ThreadPoolTaskConfiguration.class,
        DevAsserter.class,
        SuchdienstProperties.class,
    }
)
class WeightedSearchTest {

    @MockitoBean
    private IndexConfiguration indexCfg;

    @MockitoBean
    private ElasticsearchClient esClient;

    @MockitoBean
    private MessageSource messageSource;

    @MockitoSpyBean
    private SuchdienstProperties props;

    @Autowired
    private ElasticPublicSearchConnector readConn;

    private final Boost boost = new SuchdienstProperties.Boost();

    @BeforeEach
    void setupMocks() {
        when(props.getBoost()).thenReturn(boost);
        when(indexCfg.getCurrentIndexName(any())).thenReturn("index");
        when(indexCfg.getCurrentReadAliasName(any())).thenReturn("index");
    }

    @Test
    void checkWeights() throws IOException {
        // Gewichte setzen
        boost.setDescription(123F);
        boost.setName(111F);
        boost.setEnglish(99F);
        boost.setSynonyms(22F);
        boost.setDecompounder(777F);

        SearchResponse<?> response = mock(SearchResponse.class);
        when(response.hits()).thenReturn(HitsMetadata.of(h -> h.hits(List.of())));

        when(esClient.search(any(SearchRequest.class), eq(Verwaltungsleistung.class))).then(
            invocation -> {
                SearchRequest request = invocation.getArgument(0);
                Query query = request.query();
                assertThat(query, notNullValue());
                assertThat(query._kind(), is(Query.Kind.Bool));

                BoolQuery boolQuery = query.bool();
                AtomicBoolean foundFields = new AtomicBoolean(false);

                visitFields(boolQuery, fields -> {
                    if (fields.stream().anyMatch(f -> f.startsWith("searchTerms"))) {
                        foundFields.set(true);
                        assertBoostField(fields, "searchTerms.de.searchNames", boost.getName());
                        assertBoostField(
                            fields,
                            "searchTerms.de.searchNames.decompounder",
                            boost.getDecompounder()
                        );
                        assertBoostField(
                            fields,
                            "searchTerms.de.searchDescriptions",
                            boost.getDescription()
                        );
                        assertBoostField(
                            fields,
                            "searchTerms.de.searchValues",
                            boost.getSynonyms()
                        );
                        assertBoostField(fields, "searchTerms.en.searchNames", boost.getEnglish());
                        assertBoostField(
                            fields,
                            "searchTerms.en.searchDescriptions",
                            boost.getEnglish()
                        );
                        assertBoostField(fields, "searchTerms.en.searchValues", boost.getEnglish());
                    }
                });

                assertThat("Felder mit Gewichtungen nicht gefunden!", foundFields.get(), is(true));
                return response;
            }
        );

        Assertions.assertDoesNotThrow(() ->
            readConn.searchServiceDescription(
                List.of(),
                Set.of("foo"),
                emptySet(),
                "ars",
                true,
                emptySet(),
                PersonalMattersMode.NONE,
                0,
                1,
                null,
                false,
                LocalDate.now()
            )
        );

        verify(esClient, times(1)).search(any(SearchRequest.class), eq(Verwaltungsleistung.class));
    }

    @Test
    void testCreateBoost() {
        assertBoostString("leistungsbeschreibung.name", 1F, "leistungsbeschreibung.name^1");
        assertBoostString("leistungsbeschreibung.name", 1.5F, "leistungsbeschreibung.name^1.5");
        assertBoostString("leistungsbeschreibung.name", 1.59F, "leistungsbeschreibung.name^1.59");
        assertBoostString("leistungsbeschreibung.name", 1.599F, "leistungsbeschreibung.name^1.6");
        assertBoostString("leistungsbeschreibung.name", 1.519F, "leistungsbeschreibung.name^1.52");
    }

    @Test
    void searchServiceDescriptionsByArsWithSearchAfterUsesStableCursorRequest() throws IOException {
        SearchResponse<Verwaltungsleistung> response = mock(SearchResponse.class);
        when(response.hits()).thenReturn(HitsMetadata.of(h -> h.hits(List.of())));

        when(esClient.search(any(SearchRequest.class), eq(Verwaltungsleistung.class))).then(
            invocation -> {
                SearchRequest request = invocation.getArgument(0);

                assertThat(request.index(), is(List.of("index")));
                assertThat(request.size(), is(500));
                assertThat(request.source().filter().includes(), is(List.of("id", "name")));
                assertThat(request.source().filter().excludes(), is(List.of("internal")));
                assertThat(
                    request.sort().getFirst().field().field(),
                    is("leistungsbeschreibung.id")
                );
                assertThat(request.sort().getFirst().field().order(), is(SortOrder.Asc));
                assertThat(request.searchAfter().getFirst().stringValue(), is("last-lb-id"));
                assertThat(request.trackTotalHits().enabled(), is(false));

                return response;
            }
        );

        readConn.searchServiceDescriptionsByArsWithSearchAfter(
            "020000000000",
            LocalDate.of(2026, 1, 1),
            "last-lb-id",
            List.of("id", "name"),
            List.of("internal"),
            500
        );

        verify(esClient).search(any(SearchRequest.class), eq(Verwaltungsleistung.class));
    }

    // Traversiert rekursiv BoolQuery und liefert die Multi-Match Felder.
    private void visitFields(BoolQuery boolQuery, Consumer<List<String>> consumer) {
        Stream.concat(boolQuery.must().stream(), boolQuery.should().stream()).forEach(q -> {
            if (q.isMultiMatch()) {
                consumer.accept(q.multiMatch().fields());
            } else if (q.isBool()) {
                visitFields(q.bool(), consumer);
            }
        });
    }

    private void assertBoostField(List<String> fields, String field, Float value) {
        assertThat(fields, containsInRelativeOrder(readConn.createBoostField(field, value)));
    }

    private void assertBoostString(String field, float value, String expected) {
        assertThat(readConn.createBoostField(field, value), is(expected));
    }
}
