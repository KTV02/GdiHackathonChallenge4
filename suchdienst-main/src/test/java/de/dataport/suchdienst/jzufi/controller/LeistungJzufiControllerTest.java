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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.dataport.suchdienst.config.CustomCorsProperties;
import de.dataport.suchdienst.config.DevSecurityConfig;
import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.config.ResourceBundleConfig;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceResolver;
import de.dataport.suchdienst.importer.model.GenericLeistung;
import de.dataport.suchdienst.indexer.service.LeikaService;
import de.dataport.suchdienst.suchen.dynamicparams.DynamicParamsFetcherService;
import de.dataport.suchdienst.suchen.service.ServiceDescriptionService;
import de.xoev.xzufi.v2_3_1.Leistung;
import de.xoev.xzufi.v2_3_1.SchreibeObjekt;
import de.xoev.xzufi.v2_3_1.TransferOperation050502;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = LeistungJzufiController.class)
@ActiveProfiles({ "integration-test" })
@Import({
    ApiAudienceResolver.class,
    ResourceBundleConfig.class,
    PvogRequestContext.class,
    SuchdienstProperties.class,
    DevSecurityConfig.class,
    CustomCorsProperties.class,
})
class LeistungJzufiControllerTest {

    @MockitoBean
    private LeikaService leikaService;

    @MockitoBean
    private DynamicParamsFetcherService dynamicParamsFetcherService;

    @MockitoBean
    private ServiceDescriptionService serviceDescriptionService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testServicedescriptionJzufi220231UnknownLBV1() throws Exception {
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/v1/servicedescriptions/0123456/jzufi-2-2").accept(
                    "application/json"
                )
            )
            .andExpect(status().isNotFound());

        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/v1/servicedescriptions/0123456/jzufi-2-3").accept(
                    "application/json"
                )
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void testServicedescriptionJzufiV3() throws Exception {
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/v3/servicedescriptions/jzufi?id=0123456").accept(
                    "application/json;xzufi-version=2.3.1"
                )
            )
            .andExpect(status().isNotFound());

        mockMvc
            .perform(MockMvcRequestBuilders.get("/v3/servicedescriptions/jzufi"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testServicedescriptionJzufi231V1WithValidQuery() throws Exception {
        var xzufiFile = "x23/xzufi-sh-00001.xml";
        var transferOperation = loadXzufiXmlIntoTransferOperation(xzufiFile);
        Leistung leistung = transferOperation
            .getTransferOperation()
            .stream()
            .map(JAXBElement::getValue)
            .filter(SchreibeObjekt.class::isInstance)
            .map(SchreibeObjekt.class::cast)
            .map(SchreibeObjekt::getLeistung)
            .filter(Objects::nonNull)
            .findFirst()
            .orElseThrow();
        GenericLeistung genericLeistung = new GenericLeistung(leistung);

        when(serviceDescriptionService.fetchJzufi(eq("0123456"), any())).thenReturn(
            Optional.of(genericLeistung)
        );

        mockMvc
            .perform(MockMvcRequestBuilders.get("/v1/servicedescriptions/0123456/jzufi-2-3"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("informationsbereicheSDG")));
    }

    @Test
    void testServicedescriptionJzufi220V1WithKonverterValidQuery() throws Exception {
        var xzufiFile = "x23/xzufi-sh-00001.xml";
        var transferOperation = loadXzufiXmlIntoTransferOperation(xzufiFile);
        Leistung leistung = transferOperation
            .getTransferOperation()
            .stream()
            .map(JAXBElement::getValue)
            .filter(SchreibeObjekt.class::isInstance)
            .map(SchreibeObjekt.class::cast)
            .map(SchreibeObjekt::getLeistung)
            .filter(Objects::nonNull)
            .findFirst()
            .orElseThrow();
        GenericLeistung genericLeistung = new GenericLeistung(leistung);

        when(serviceDescriptionService.fetchJzufi(eq("0123456"), any())).thenReturn(
            Optional.of(genericLeistung)
        );

        mockMvc
            .perform(MockMvcRequestBuilders.get("/v1/servicedescriptions/0123456/jzufi-2-2"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("informationsbereichSDG")));
    }

    @Test
    void testServicedescriptionJzufi23WithValidQuery() throws Exception {
        var xzufiFile = "x23/xzufi-sh-00001.xml";
        var transferOperation = loadXzufiXmlIntoTransferOperation(xzufiFile);
        Leistung leistung = transferOperation
            .getTransferOperation()
            .stream()
            .map(JAXBElement::getValue)
            .filter(SchreibeObjekt.class::isInstance)
            .map(SchreibeObjekt.class::cast)
            .map(SchreibeObjekt::getLeistung)
            .filter(Objects::nonNull)
            .findFirst()
            .orElseThrow();
        GenericLeistung genericLeistung = new GenericLeistung(leistung);

        when(serviceDescriptionService.fetchJzufi(eq("0123456"), any())).thenReturn(
            Optional.of(genericLeistung)
        );

        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/v3/servicedescriptions/jzufi?id=0123456").accept(
                    "application/json;xzufi-version=2.3.1"
                )
            )
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("informationsbereicheSDG")));
    }

    @Test
    void testServicedescriptionJzufi22WithKonverterValidQuery() throws Exception {
        var xzufiFile = "x23/xzufi-sh-00001.xml";
        var transferOperation = loadXzufiXmlIntoTransferOperation(xzufiFile);
        Leistung leistung = transferOperation
            .getTransferOperation()
            .stream()
            .map(JAXBElement::getValue)
            .filter(SchreibeObjekt.class::isInstance)
            .map(SchreibeObjekt.class::cast)
            .map(SchreibeObjekt::getLeistung)
            .filter(Objects::nonNull)
            .findFirst()
            .orElseThrow();
        GenericLeistung genericLeistung = new GenericLeistung(leistung);

        when(serviceDescriptionService.fetchJzufi(eq("0123456"), any())).thenReturn(
            Optional.of(genericLeistung)
        );

        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/v3/servicedescriptions/jzufi?id=0123456").accept(
                    "application/json;xzufi-version=2.2.0"
                )
            )
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("informationsbereichSDG")));
    }

    protected TransferOperation050502 loadXzufiXmlIntoTransferOperation(String xzufiFileName)
        throws JAXBException {
        var xmlStream = this.getClass().getClassLoader().getResourceAsStream(xzufiFileName);
        var jaxbContext = JAXBContext.newInstance(TransferOperation050502.class);
        var unmarshaller = jaxbContext.createUnmarshaller();
        return (TransferOperation050502) unmarshaller.unmarshal(xmlStream);
    }
}
