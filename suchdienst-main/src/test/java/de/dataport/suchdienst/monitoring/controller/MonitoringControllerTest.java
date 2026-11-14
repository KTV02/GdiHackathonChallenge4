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
package de.dataport.suchdienst.monitoring.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.config.ResourceBundleConfig;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.elasticsearch.api.ElasticInternalReadConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.importer.model.warteschlange.TransactionMetadata;
import de.dataport.suchdienst.indexer.model.Identifiable;
import de.dataport.suchdienst.monitoring.model.MonitoringTransactionMetaData;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MonitoringController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ ResourceBundleConfig.class, PvogRequestContext.class, SuchdienstProperties.class })
@ActiveProfiles({ "default", "dev", "openapi_show_internal" })
class MonitoringControllerTest {

    @Autowired
    public MockMvc mockMvc;

    @MockitoBean
    private ElasticInternalReadConnector elasticInternalReadConnector;

    @MockitoBean
    private ElasticReadConnector elasticReadConnector;

    @Test
    void shouldReturnTrueIfImportActive() throws Exception {
        String schemeAgencyId = "testId";
        when(elasticInternalReadConnector.countPendingImportQueue(schemeAgencyId)).thenReturn(1L);

        mockMvc
            .perform(
                get("/monitoring/import-active")
                    .param("scheme_agency_id", schemeAgencyId)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(content().string("true"));
    }

    @Test
    void shouldReturnFalseIfImportNotActive() throws Exception {
        String schemeAgencyId = "testId";
        when(elasticInternalReadConnector.countPendingImportQueue(schemeAgencyId)).thenReturn(0L);

        mockMvc
            .perform(
                get("/monitoring/import-active")
                    .param("scheme_agency_id", schemeAgencyId)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(content().string("false"));
    }

    @Test
    void shouldReturnLbPageable() throws Exception {
        int sizeValue = 10;
        int sizeTotal = 15;

        HitsMetadata<MonitoringTransactionMetaData> firstResponse = createMetadata(
            sizeTotal,
            0,
            10
        );
        HitsMetadata<MonitoringTransactionMetaData> secondResponse = createMetadata(
            sizeTotal,
            10,
            sizeTotal
        );
        when(
            elasticReadConnector.searchAfter(
                eq(IndexNames.LEISTUNGSBESCHREIBUNG),
                isNull(),
                anyInt(),
                any(Query.class),
                any(SourceConfig.class),
                anyBoolean(),
                eq(MonitoringTransactionMetaData.class)
            )
        )
            // 1. Anfrage Teil 1
            .thenReturn(firstResponse)
            // 2. Anfrage
            .thenReturn(secondResponse);

        mockMvc
            .perform(
                get("/monitoring/lb")
                    .param("size", Integer.toString(sizeValue))
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.queryResponse.lastId").value("9"))
            .andExpect(jsonPath("$.queryResponse.content").isArray())
            .andExpect(jsonPath("$.queryResponse.content", hasSize(sizeValue)))
            .andExpect(jsonPath("$.totalElements").value(sizeTotal))
            .andExpect(jsonPath("$.queryResponse.lastResponse").value(false));

        mockMvc
            .perform(
                get("/monitoring/lb")
                    .param("size", Integer.toString(sizeValue))
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.queryResponse.lastId").value(14))
            .andExpect(jsonPath("$.queryResponse.content").isArray())
            .andExpect(jsonPath("$.queryResponse.content", hasSize(sizeTotal - sizeValue)))
            .andExpect(jsonPath("$.totalElements").value(sizeTotal))
            .andExpect(jsonPath("$.queryResponse.lastResponse").value(true));
    }

    @Test
    void shouldReturnOePageable() throws Exception {
        int sizeValue = 10;
        HitsMetadata<MonitoringTransactionMetaData> metadata = createMetadata(
            sizeValue,
            0,
            sizeValue
        );
        when(
            elasticReadConnector.searchAfter(
                eq(IndexNames.ORGANISATIONSEINHEITEN),
                eq("9"),
                anyInt(),
                any(Query.class),
                any(SourceConfig.class),
                anyBoolean(),
                eq(MonitoringTransactionMetaData.class)
            )
        ).thenReturn(metadata);
        mockMvc
            .perform(
                get("/monitoring/oe")
                    .param("page", "0")
                    .param("size", Integer.toString(sizeValue))
                    .param("lastId", "9")
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.queryResponse.lastId").isNotEmpty())
            .andExpect(jsonPath("$.queryResponse.content").isArray())
            .andExpect(jsonPath("$.queryResponse.content", hasSize(sizeValue)))
            .andExpect(jsonPath("$.totalElements").value(sizeValue));

        verify(elasticReadConnector).searchAfter(
            eq(IndexNames.ORGANISATIONSEINHEITEN),
            eq("9"),
            eq(sizeValue),
            any(Query.class),
            any(SourceConfig.class),
            eq(true),
            eq(MonitoringTransactionMetaData.class)
        );
        verifyNoInteractions(elasticInternalReadConnector);
    }

    @Test
    void shouldReturnEmptyLbPageableResponse() throws Exception {
        int sizeValue = 10;
        HitsMetadata<MonitoringTransactionMetaData> metadata = createMetadata(0, 0, 0);
        when(
            elasticReadConnector.searchAfter(
                eq(IndexNames.LEISTUNGSBESCHREIBUNG),
                isNull(),
                anyInt(),
                any(Query.class),
                any(SourceConfig.class),
                anyBoolean(),
                eq(MonitoringTransactionMetaData.class)
            )
        ).thenReturn(metadata);

        mockMvc
            .perform(
                get("/monitoring/lb")
                    .param("size", Integer.toString(sizeValue))
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.queryResponse.lastId").doesNotExist())
            .andExpect(jsonPath("$.queryResponse.content").isArray())
            .andExpect(jsonPath("$.queryResponse.content", hasSize(0)))
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.queryResponse.lastResponse").value(true));

        verify(elasticReadConnector).searchAfter(
            eq(IndexNames.LEISTUNGSBESCHREIBUNG),
            isNull(),
            eq(sizeValue),
            any(Query.class),
            any(SourceConfig.class),
            eq(true),
            eq(MonitoringTransactionMetaData.class)
        );
        verifyNoInteractions(elasticInternalReadConnector);
    }

    @Test
    void shouldReturnSpPageable() throws Exception {
        int sizeValue = 10;
        HitsMetadata<MonitoringTransactionMetaData> metadata = createMetadata(
            sizeValue,
            0,
            sizeValue
        );
        when(
            elasticReadConnector.searchAfter(
                eq(IndexNames.SPEZIALISIERUNGEN),
                isNull(),
                anyInt(),
                any(Query.class),
                any(SourceConfig.class),
                anyBoolean(),
                eq(MonitoringTransactionMetaData.class)
            )
        ).thenReturn(metadata);
        mockMvc
            .perform(
                get("/monitoring/sp")
                    .param("page", "0")
                    .param("size", Integer.toString(sizeValue))
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.queryResponse.lastId").isNotEmpty())
            .andExpect(jsonPath("$.queryResponse.content").isArray())
            .andExpect(jsonPath("$.queryResponse.content", hasSize(sizeValue)))
            .andExpect(jsonPath("$.totalElements").value(sizeValue));
    }

    @Test
    void shouldReturnLbError() throws Exception {
        int sizeValue = 10;

        HitsMetadata<Identifiable> dd = mock(HitsMetadata.class);
        when(
            elasticReadConnector.searchAfter(
                eq(IndexNames.LEISTUNGSBESCHREIBUNG),
                isNull(),
                anyInt(),
                any(Query.class),
                any(SourceConfig.class),
                anyBoolean(),
                eq(MonitoringTransactionMetaData.class)
            )
        ).then(p -> dd);
        when(dd.hits()).then(r -> null);

        mockMvc
            .perform(
                get("/monitoring/lb")
                    .param("size", Integer.toString(sizeValue))
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().is5xxServerError());
    }

    @Test
    void shouldReturnOeError() throws Exception {
        int sizeValue = 10;
        HitsMetadata<Identifiable> dd = mock(HitsMetadata.class);
        when(
            elasticReadConnector.searchAfter(
                eq(IndexNames.ORGANISATIONSEINHEITEN),
                isNull(),
                anyInt(),
                any(Query.class),
                any(SourceConfig.class),
                anyBoolean(),
                eq(MonitoringTransactionMetaData.class)
            )
        ).then(p -> dd);
        when(dd.hits()).then(r -> null);
        mockMvc
            .perform(
                get("/monitoring/oe")
                    .param("size", Integer.toString(sizeValue))
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().is5xxServerError());
    }

    @Test
    void shouldReturnSpError() throws Exception {
        int sizeValue = 10;

        HitsMetadata<Identifiable> dd = mock(HitsMetadata.class);
        when(
            elasticReadConnector.searchAfter(
                eq(IndexNames.SPEZIALISIERUNGEN),
                isNull(),
                anyInt(),
                any(Query.class),
                any(SourceConfig.class),
                anyBoolean(),
                eq(MonitoringTransactionMetaData.class)
            )
        ).then(p -> dd);
        when(dd.hits()).then(r -> null);

        mockMvc
            .perform(
                get("/monitoring/sp")
                    .param("size", Integer.toString(sizeValue))
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().is5xxServerError());
    }

    private HitsMetadata<MonitoringTransactionMetaData> createMetadata(
        int total,
        int skip,
        int limit
    ) {
        List<Hit<MonitoringTransactionMetaData>> hits = IntStream.range(0, total)
            .limit(limit)
            .skip(skip)
            .mapToObj(i -> {
                String id = String.valueOf(i);
                TransactionMetadata transactionMetadata = new TransactionMetadata(
                    id,
                    "1",
                    "1",
                    "1",
                    "1"
                );
                MonitoringTransactionMetaData monitoringTransactionMetaData =
                    new MonitoringTransactionMetaData();
                monitoringTransactionMetaData.setMetaData(transactionMetadata);
                monitoringTransactionMetaData.setId(id);
                return Hit.<MonitoringTransactionMetaData>of(hit ->
                    hit
                        .index("index")
                        .id(id)
                        .source(monitoringTransactionMetaData)
                        .sort(FieldValue.of(id))
                );
            })
            .toList();
        return HitsMetadata.of(m ->
            m.hits(hits).total(TotalHits.of(t -> t.value(total).relation(TotalHitsRelation.Eq)))
        );
    }
}
