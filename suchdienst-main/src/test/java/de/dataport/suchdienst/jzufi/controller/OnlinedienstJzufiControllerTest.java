/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.jzufi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.dataport.suchdienst.config.CustomCorsProperties;
import de.dataport.suchdienst.config.DevSecurityConfig;
import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.config.ResourceBundleConfig;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceResolver;
import de.dataport.suchdienst.importer.model.GenericOnlinedienst;
import de.dataport.suchdienst.suchen.service.OnlineDienstService;
import de.xoev.xzufi.v2_3_1.OnlinedienstErweitert;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = OnlinedienstJzufiController.class)
@Import({
    ApiAudienceResolver.class,
    ResourceBundleConfig.class,
    PvogRequestContext.class,
    SuchdienstProperties.class,
    DevSecurityConfig.class,
    CustomCorsProperties.class,
})
class OnlinedienstJzufiControllerTest {

    @MockitoBean
    private OnlineDienstService onlineDienstService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testOnlinedienstJzufi220231UnknownODV1() throws Exception {
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/v1/onlineservices/0123456/jzufi-2-2").accept(
                    "application/json"
                )
            )
            .andExpect(status().isNotFound());

        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/v1/onlineservices/0123456/jzufi-2-3").accept(
                    "application/json"
                )
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void testOnlinedienstJzufi231V1WithValidQuery() throws Exception {
        OnlinedienstErweitert onlinedienst = loadOnlinedienstFromXzufi("x23/onlinedienst.xzufi");
        GenericOnlinedienst genericOnlinedienst = new GenericOnlinedienst(onlinedienst);

        when(onlineDienstService.getJzufi(eq("0123456"), any())).thenReturn(
            Optional.of(genericOnlinedienst)
        );

        mockMvc
            .perform(MockMvcRequestBuilders.get("/v1/onlineservices/0123456/jzufi-2-3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.kennzeichenOeffentlichAnzeigen").value(true));
    }

    @Test
    void testOnlinedienstJzufi231V1ReturnsNonPublicOnlinedienst() throws Exception {
        OnlinedienstErweitert onlinedienst = loadOnlinedienstFromXzufi("x23/onlinedienst.xzufi");
        onlinedienst.setKennzeichenOeffentlichAnzeigen(false);
        GenericOnlinedienst genericOnlinedienst = new GenericOnlinedienst(onlinedienst);

        when(onlineDienstService.getJzufi(eq("0123456"), any())).thenReturn(
            Optional.of(genericOnlinedienst)
        );

        mockMvc
            .perform(MockMvcRequestBuilders.get("/v1/onlineservices/0123456/jzufi-2-3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.kennzeichenOeffentlichAnzeigen").value(false));
    }

    @Test
    void testOnlinedienstJzufi220V1WithKonverterValidQuery() throws Exception {
        OnlinedienstErweitert onlinedienst = loadOnlinedienstFromXzufi("x23/onlinedienst.xzufi");
        GenericOnlinedienst genericOnlinedienst = new GenericOnlinedienst(onlinedienst);

        when(onlineDienstService.getJzufi(eq("0123456"), any())).thenReturn(
            Optional.of(genericOnlinedienst)
        );

        mockMvc
            .perform(MockMvcRequestBuilders.get("/v1/onlineservices/0123456/jzufi-2-2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.kennzeichenOeffentlichAnzeigen").doesNotExist());
    }

    @Test
    void testOnlinedienstJzufi220V1FiltersNonPublicOnlinedienst() throws Exception {
        OnlinedienstErweitert onlinedienst = loadOnlinedienstFromXzufi("x23/onlinedienst.xzufi");
        onlinedienst.setKennzeichenOeffentlichAnzeigen(false);
        GenericOnlinedienst genericOnlinedienst = new GenericOnlinedienst(onlinedienst);

        when(onlineDienstService.getJzufi(eq("0123456"), any())).thenReturn(
            Optional.of(genericOnlinedienst)
        );

        mockMvc
            .perform(MockMvcRequestBuilders.get("/v1/onlineservices/0123456/jzufi-2-2"))
            .andExpect(status().isNotFound());
    }

    private OnlinedienstErweitert loadOnlinedienstFromXzufi(String xzufiFileName)
        throws JAXBException {
        var transferOperation = loadXzufiXmlIntoTransferOperation(xzufiFileName);
        return transferOperation
            .getTransferOperation()
            .stream()
            .map(JAXBElement::getValue)
            .filter(SchreibeObjekt.class::isInstance)
            .map(SchreibeObjekt.class::cast)
            .map(SchreibeObjekt::getOnlinedienst)
            .filter(Objects::nonNull)
            .map(OnlinedienstErweitert.class::cast)
            .findFirst()
            .orElseThrow();
    }

    protected TransferOperation050502 loadXzufiXmlIntoTransferOperation(String xzufiFileName)
        throws JAXBException {
        var xmlStream = this.getClass().getClassLoader().getResourceAsStream(xzufiFileName);
        var jaxbContext = JAXBContext.newInstance(TransferOperation050502.class);
        var unmarshaller = jaxbContext.createUnmarshaller();
        return (TransferOperation050502) unmarshaller.unmarshal(xmlStream);
    }
}
