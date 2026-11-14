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

import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.elasticsearch.api.ElasticPublicSearchConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetail;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.Verwaltungsleistung;
import de.dataport.suchdienst.suchen.dynamicparams.DynamicParamsFetcherService;
import de.dataport.suchdienst.suchen.model.out.leistung.SdOutModelV7;
import de.dataport.suchdienst.suchen.service.OrganisationModelConverterService;
import de.dataport.suchdienst.suchen.service.ServiceDescriptionService;
import de.dataport.suchdienst.suchen.service.VerwaltungsleistungMappingService;
import de.dataport.suchdienst.util.ServiceModuleCode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoBeans;

@SpringBootTest(
    classes = {
        ServiceDescriptionService.class,
        JacksonAutoConfiguration.class,
        ElasticPublicSearchConnector.class,
        OrganisationModelConverterService.class,
        VerwaltungsleistungMappingService.class,
    }
)
@ContextConfiguration(classes = { SuchdienstProperties.class })
@MockitoBeans(
    value = {
        @MockitoBean(types = ElasticPublicSearchConnector.class),
        @MockitoBean(types = ElasticReadConnector.class),
        @MockitoBean(types = LeikaService.class),
        @MockitoBean(types = DynamicParamsFetcherService.class),
        @MockitoBean(types = PvogRequestContext.class),
    }
)
class ServiceDescriptionServiceTest {

    @Autowired
    private ServiceDescriptionService serviceDescriptionService;

    @Test
    void testSorting() {
        HashMap<String, List<AbstractServiceModule>> details = new HashMap<>() {
            {
                put(
                    ServiceModuleCode.LEISTUNGSBEZEICHNUNG.getCode(),
                    new ArrayList<>(
                        List.of(
                            ServiceModule.builder()
                                .title("Test Leistung")
                                .text("Test Leistung")
                                .build()
                        )
                    )
                );
                put(
                    ServiceModuleCode.VOLLTEXT.getCode(),
                    new ArrayList<>(
                        List.of(ServiceModule.builder().text("Eine Beschreibung").build())
                    )
                );
                put(
                    ServiceModuleCode.FRIST.getCode(),
                    new ArrayList<>(List.of(ServiceModuleDetail.builder().text("Fristen").build()))
                );
            }
        };

        LeistungsbeschreibungVL lb = new LeistungsbeschreibungVL();
        lb.setId("1.LB.1");
        Verwaltungsleistung sd = new Verwaltungsleistung();
        sd.setId("id");
        sd.setLeistungsbeschreibung(lb);
        sd.getLeistungsbeschreibung().setDetails(details);

        var out = serviceDescriptionService.mapServiceDescriptionToOutModelBase(
            sd,
            "020000000000",
            Locale.GERMAN,
            LocalDate.now(),
            false,
            SdOutModelV7.class
        );

        Assertions.assertEquals(10, out.getDetails().size());
        Assertions.assertEquals("Eine Beschreibung", out.getDetails().get(1).getText());
        Assertions.assertEquals("Fristen", out.getDetails().get(5).getText());
    }
}
