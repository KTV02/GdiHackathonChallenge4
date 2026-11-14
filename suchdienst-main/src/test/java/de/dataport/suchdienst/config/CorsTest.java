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
package de.dataport.suchdienst.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.dataport.suchdienst.config.apiaudience.ApiAudienceResolver;
import de.dataport.suchdienst.elasticsearch.api.ElasticPublicSearchConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.indexer.model.mapping.LinkDynamic;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstVL;
import de.dataport.suchdienst.suchen.controller.OnlineServiceController;
import de.dataport.suchdienst.suchen.dynamicparams.DynamicParamsFetcherService;
import de.dataport.suchdienst.suchen.service.OnlineDienstService;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;

@ActiveProfiles({ "default", "test", "security_off" })
@SpringBootTest(
    classes = {
        OnlineServiceController.class,
        JacksonAutoConfiguration.class,
        SuchdienstProperties.class,
        DevSecurityConfig.class,
        CustomCorsProperties.class,
        ApiAudienceResolver.class,
        OnlineDienstService.class,
    }
)
@EnableWebMvc
@AutoConfigureMockMvc
class CorsTest {

    @Autowired
    public MockMvc mockMvc;

    @Autowired
    public ObjectMapper mapper;

    @MockitoBean
    private PvogRequestContext context;

    @MockitoBean
    private ElasticPublicSearchConnector elasticPublicSearchConnector;

    @MockitoBean
    private ElasticReadConnector elasticReadConnector;

    @MockitoBean
    private DynamicParamsFetcherService dynamicParamsFetcherService;

    @BeforeEach
    void setupMocks() {
        Mockito.when(
            elasticPublicSearchConnector.searchOnlinedienst(Mockito.anyString(), Mockito.isNull())
        ).thenReturn(getOnlineMocks());
        Mockito.when(context.getLocale()).thenReturn(Locale.GERMAN);
    }

    @Test
    void corsHeaders() throws Exception {
        mockMvc
            .perform(
                get("/v2/onlineservices/detail")
                    .header("Accept-Language", "de")
                    .header("Access-Control-Request-Method", "GET")
                    .header("Origin", "localhost")
                    .param("q", "1.OD.1")
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(header().stringValues("Access-Control-Allow-Origin", "localhost"))
            .andExpect(
                header().stringValues(
                    "Access-Control-Expose-Headers",
                    "Authorization, Access-Control-Allow-Origin"
                )
            );
    }

    @Test
    void denyEvilOrigin() throws Exception {
        mockMvc
            .perform(
                get("/v2/onlineservices/detail")
                    .header("Accept-Language", "de")
                    .header("Access-Control-Request-Method", "GET")
                    .header("Origin", "evil.com")
                    .param("q", "1.OD.1")
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    private Optional<OnlinedienstVL> getOnlineMocks() {
        OnlinedienstVL od = new OnlinedienstVL();
        od.setId("1.OD.1");
        od.setName("Onlinedienst1");
        LinkDynamic link1 = LinkDynamic.of("www.link1od1", "", "");
        LinkDynamic link2 = LinkDynamic.of("www.link2od1", "", "");
        od.getLinks().add(link1);
        od.getLinks().add(link2);

        return Optional.of(od);
    }
}
