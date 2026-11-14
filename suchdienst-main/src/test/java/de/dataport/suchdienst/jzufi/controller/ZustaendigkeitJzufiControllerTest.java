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
package de.dataport.suchdienst.jzufi.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import de.dataport.suchdienst.config.CustomCorsProperties;
import de.dataport.suchdienst.config.DevSecurityConfig;
import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.config.ResourceBundleConfig;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceResolver;
import de.dataport.suchdienst.elasticsearch.api.ElasticPublicSearchConnector;
import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity;
import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity22;
import de.dataport.suchdienst.indexer.model.mapping.RelationTyp;
import de.dataport.suchdienst.indexer.service.LeikaService;
import de.dataport.suchdienst.suchen.dynamicparams.DynamicParamsFetcherService;
import de.dataport.suchdienst.suchen.service.RelationService;
import de.xoev.xzufi.v2_2_0.SchreibeObjekt;
import de.xoev.xzufi.v2_2_0.TransferOperation040502;
import de.xoev.xzufi.v2_2_0.ZustaendigkeitTransferObjekt;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = ZustaendigkeitJzufiController.class)
@ActiveProfiles({ "integration-test" })
@Import({
    ApiAudienceResolver.class,
    ResourceBundleConfig.class,
    PvogRequestContext.class,
    SuchdienstProperties.class,
    DevSecurityConfig.class,
    CustomCorsProperties.class,
    RelationService.class,
})
class ZustaendigkeitJzufiControllerTest {

    @MockitoBean
    private LeikaService leikaService;

    @MockitoBean
    private DynamicParamsFetcherService dynamicParamsFetcherService;

    @MockitoBean
    private ElasticPublicSearchConnector publicSearchConnector;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testZustaendigkeitJzufi220231V1() throws Exception {
        HitsMetadata<ZustaendigkeitEntity> hitsMetadata =
            new HitsMetadata.Builder<ZustaendigkeitEntity>().hits(Collections.EMPTY_LIST).build();

        when(
            publicSearchConnector.searchRelationByLbIdAndArs(
                eq(List.of("S1000030000000042")),
                anyString(),
                anyBoolean(),
                any(),
                anyInt(),
                anyInt()
            )
        ).thenReturn(hitsMetadata);

        mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    "/v1/relations/jzufi-2-3?ars=040110000000&lbids=S1000030000000042"
                )
            )
            .andExpect(status().isOk());

        mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    "/v1/relations/jzufi-2-2?ars=040110000000&lbids=S1000030000000042"
                )
            )
            .andExpect(status().isOk());

        mockMvc
            .perform(MockMvcRequestBuilders.get("/v1/relations/jzufi-2-3?ars=040110000000"))
            .andExpect(status().isBadRequest());

        mockMvc
            .perform(MockMvcRequestBuilders.get("/v1/relations/jzufi-2-2?ars=040110000000"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testZustaendigkeitJzufiV3() throws Exception {
        HitsMetadata<ZustaendigkeitEntity> hitsMetadata =
            new HitsMetadata.Builder<ZustaendigkeitEntity>().hits(Collections.EMPTY_LIST).build();

        when(
            publicSearchConnector.searchRelationByLbIdAndArs(
                eq(List.of("S1000030000000042")),
                anyString(),
                anyBoolean(),
                any(),
                anyInt(),
                anyInt()
            )
        ).thenReturn(hitsMetadata);

        mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    "/v3/relations/040110000000/jzufi?lbids=S1000030000000042"
                ).accept("application/json;xzufi-version=2.3.1")
            )
            .andExpect(status().isOk());

        mockMvc
            .perform(MockMvcRequestBuilders.get("/v3/relations/040110000000/jzufi"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testZustaendigkeitJzufi230V1WithValidQuery() throws Exception {
        var xzufiFile = "x22/xzufi-zustaendigkeit-schreibe.xml";
        var transferOperation = loadXzufiXmlIntoTransferOperation(xzufiFile);
        ZustaendigkeitTransferObjekt zustaendigkeitTransferObjekt = transferOperation
            .getTransferOperation()
            .stream()
            .filter(SchreibeObjekt.class::isInstance)
            .map(SchreibeObjekt.class::cast)
            .map(SchreibeObjekt::getZustaendigkeitTransferObjekt)
            .filter(Objects::nonNull)
            .findFirst()
            .orElseThrow();

        ZustaendigkeitEntity22 zustaendigkeitEntity = new ZustaendigkeitEntity22(Instant.now());
        zustaendigkeitEntity.setArs("040110000000");
        zustaendigkeitEntity.setId("S1000030000000041.S1000030000323115.040110000000");
        zustaendigkeitEntity.setTyp(RelationTyp.ORGANISATION);
        zustaendigkeitEntity.setLbId("S1000030000000041");
        zustaendigkeitEntity.setJzufi(zustaendigkeitTransferObjekt);
        zustaendigkeitEntity.setParentId("S1000030000323115");
        zustaendigkeitEntity.setArsFederalState("040110000000");

        HitsMetadata<ZustaendigkeitEntity> hitsMetadata =
            new HitsMetadata.Builder<ZustaendigkeitEntity>()
                .hits(
                    List.of(
                        new Hit.Builder<ZustaendigkeitEntity>()
                            .source(zustaendigkeitEntity)
                            .index("1")
                            .build()
                    )
                )
                .build();

        when(
            publicSearchConnector.searchRelationByLbIdAndArs(
                eq(List.of("S1000030000000041")),
                anyString(),
                anyBoolean(),
                any(),
                anyInt(),
                anyInt()
            )
        ).thenReturn(hitsMetadata);

        mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    "/v1/relations/jzufi-2-3?ars=040110000000&lbids=S1000030000000041"
                )
            )
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("\"@type\""))))
            .andExpect(content().string(containsString("\"code\":\"03\"")));
    }

    @Test
    void testZustaendigkeitJzufi220V1WithValidQuery() throws Exception {
        var xzufiFile = "x22/xzufi-zustaendigkeit-schreibe.xml";
        var transferOperation = loadXzufiXmlIntoTransferOperation(xzufiFile);
        ZustaendigkeitTransferObjekt zustaendigkeitTransferObjekt = transferOperation
            .getTransferOperation()
            .stream()
            .filter(SchreibeObjekt.class::isInstance)
            .map(SchreibeObjekt.class::cast)
            .map(SchreibeObjekt::getZustaendigkeitTransferObjekt)
            .filter(Objects::nonNull)
            .findFirst()
            .orElseThrow();

        ZustaendigkeitEntity22 zustaendigkeitEntity = new ZustaendigkeitEntity22(Instant.now());
        zustaendigkeitEntity.setArs("040110000000");
        zustaendigkeitEntity.setId("S1000030000000041.S1000030000323115.040110000000");
        zustaendigkeitEntity.setTyp(RelationTyp.ORGANISATION);
        zustaendigkeitEntity.setLbId("S1000030000000041");
        zustaendigkeitEntity.setJzufi(zustaendigkeitTransferObjekt);
        zustaendigkeitEntity.setParentId("S1000030000323115");
        zustaendigkeitEntity.setArsFederalState("040110000000");

        HitsMetadata<ZustaendigkeitEntity> hitsMetadata =
            new HitsMetadata.Builder<ZustaendigkeitEntity>()
                .hits(
                    List.of(
                        new Hit.Builder<ZustaendigkeitEntity>()
                            .source(zustaendigkeitEntity)
                            .index("1")
                            .build()
                    )
                )
                .build();

        when(
            publicSearchConnector.searchRelationByLbIdAndArs(
                eq(List.of("S1000030000000041")),
                anyString(),
                anyBoolean(),
                any(),
                anyInt(),
                anyInt()
            )
        ).thenReturn(hitsMetadata);

        mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    "/v1/relations/jzufi-2-2?ars=040110000000&lbids=S1000030000000041"
                )
            )
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("\"@type\""))))
            .andExpect(content().string(containsString("\"code\":\"01\"")));
    }

    @Test
    void testZustaendigkeitJzufi23WithValidQuery() throws Exception {
        var xzufiFile = "x22/xzufi-zustaendigkeit-schreibe.xml";
        var transferOperation = loadXzufiXmlIntoTransferOperation(xzufiFile);
        ZustaendigkeitTransferObjekt zustaendigkeitTransferObjekt = transferOperation
            .getTransferOperation()
            .stream()
            .filter(SchreibeObjekt.class::isInstance)
            .map(SchreibeObjekt.class::cast)
            .map(SchreibeObjekt::getZustaendigkeitTransferObjekt)
            .filter(Objects::nonNull)
            .findFirst()
            .orElseThrow();

        ZustaendigkeitEntity22 zustaendigkeitEntity = new ZustaendigkeitEntity22(Instant.now());
        zustaendigkeitEntity.setArs("040110000000");
        zustaendigkeitEntity.setId("S1000030000000041.S1000030000323115.040110000000");
        zustaendigkeitEntity.setTyp(RelationTyp.ORGANISATION);
        zustaendigkeitEntity.setLbId("S1000030000000041");
        zustaendigkeitEntity.setJzufi(zustaendigkeitTransferObjekt);
        zustaendigkeitEntity.setParentId("S1000030000323115");
        zustaendigkeitEntity.setArsFederalState("040110000000");

        HitsMetadata<ZustaendigkeitEntity> hitsMetadata =
            new HitsMetadata.Builder<ZustaendigkeitEntity>()
                .hits(
                    List.of(
                        new Hit.Builder<ZustaendigkeitEntity>()
                            .source(zustaendigkeitEntity)
                            .index("1")
                            .build()
                    )
                )
                .build();

        when(
            publicSearchConnector.searchRelationByLbIdAndArs(
                eq(List.of("S1000030000000041")),
                anyString(),
                anyBoolean(),
                any(),
                anyInt(),
                anyInt()
            )
        ).thenReturn(hitsMetadata);

        mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    "/v3/relations/040110000000/jzufi?lbids=S1000030000000041"
                ).accept("application/json;xzufi-version=2.3.1")
            )
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("\"@type\""))))
            .andExpect(content().string(containsString("\"code\":\"03\"")));
    }

    @Test
    void testZustaendigkeitJzufi22WithValidQuery() throws Exception {
        var xzufiFile = "x22/xzufi-zustaendigkeit-schreibe.xml";
        var transferOperation = loadXzufiXmlIntoTransferOperation(xzufiFile);
        ZustaendigkeitTransferObjekt zustaendigkeitTransferObjekt = transferOperation
            .getTransferOperation()
            .stream()
            .filter(SchreibeObjekt.class::isInstance)
            .map(SchreibeObjekt.class::cast)
            .map(SchreibeObjekt::getZustaendigkeitTransferObjekt)
            .filter(Objects::nonNull)
            .findFirst()
            .orElseThrow();

        ZustaendigkeitEntity22 zustaendigkeitEntity = new ZustaendigkeitEntity22(Instant.now());
        zustaendigkeitEntity.setArs("040110000000");
        zustaendigkeitEntity.setId("S1000030000000041.S1000030000323115.040110000000");
        zustaendigkeitEntity.setTyp(RelationTyp.ORGANISATION);
        zustaendigkeitEntity.setLbId("S1000030000000041");
        zustaendigkeitEntity.setJzufi(zustaendigkeitTransferObjekt);
        zustaendigkeitEntity.setParentId("S1000030000323115");
        zustaendigkeitEntity.setArsFederalState("040110000000");

        HitsMetadata<ZustaendigkeitEntity> hitsMetadata =
            new HitsMetadata.Builder<ZustaendigkeitEntity>()
                .hits(
                    List.of(
                        new Hit.Builder<ZustaendigkeitEntity>()
                            .source(zustaendigkeitEntity)
                            .index("1")
                            .build()
                    )
                )
                .build();

        when(
            publicSearchConnector.searchRelationByLbIdAndArs(
                eq(List.of("S1000030000000041")),
                anyString(),
                anyBoolean(),
                any(),
                anyInt(),
                anyInt()
            )
        ).thenReturn(hitsMetadata);

        mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    "/v3/relations/040110000000/jzufi?lbids=S1000030000000041"
                ).accept("application/json;xzufi-version=2.2.0")
            )
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("\"@type\""))))
            .andExpect(content().string(containsString("\"code\":\"01\"")));
    }

    protected TransferOperation040502 loadXzufiXmlIntoTransferOperation(String xzufiFileName)
        throws JAXBException {
        var xmlStream = this.getClass().getClassLoader().getResourceAsStream(xzufiFileName);
        var jaxbContext = JAXBContext.newInstance(TransferOperation040502.class);
        var unmarshaller = jaxbContext.createUnmarshaller();
        return (TransferOperation040502) unmarshaller.unmarshal(xmlStream);
    }
}
