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
package de.dataport.suchdienst.suchen.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.config.ResourceBundleConfig;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.config.ValidationConfiguration;
import de.dataport.suchdienst.indexer.model.mapping.LinkDynamic;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstVL;
import de.dataport.suchdienst.suchen.model.mapper.OnlinedienstMapper;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.OnlineServiceOutModelV2;
import de.dataport.suchdienst.suchen.service.OnlineDienstService;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@WebMvcTest(controllers = OnlineServiceController.class)
@EnableWebMvc
@AutoConfigureMockMvc(addFilters = false)
@Import({
    ResourceBundleConfig.class,
    ValidationConfiguration.class,
    PvogRequestContext.class,
    SuchdienstProperties.class,
})
class OnlineServiceControllerTest {

    @Autowired
    public MockMvc mockMvc;

    @MockitoBean
    private OnlineDienstService onlineDienstService;

    @Test
    void testV2() throws Exception {
        Mockito.when(
            onlineDienstService.getOnlineServiceV2ById(Mockito.anyString(), Mockito.isNull())
        ).thenReturn(getOnlineMocks());

        mockMvc
            .perform(
                get("/v2/onlineservices/detail")
                    .header("Accept-Language", "de")
                    .param("q", "1.OD.1")
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").isString())
            .andExpect(jsonPath("$.relations").isArray())
            .andExpect(jsonPath("$.logos").exists())
            .andExpect(jsonPath("$.logos").isArray())
            .andExpect(jsonPath("$.*", hasSize(20)));
    }

    private OnlineServiceOutModelV2 getOnlineMocks() {
        OnlinedienstVL od = new OnlinedienstVL();
        od.setId("1.OD.1");
        od.setName("Onlinedienst1");
        LinkDynamic link1 = LinkDynamic.of("www.link1od1", "", "");
        LinkDynamic link2 = LinkDynamic.of("www.link2od1", "", "");
        od.getLinks().add(link1);
        od.getLinks().add(link2);

        return OnlinedienstMapper.INSTANCE.map(od, Set.of());
    }
}
