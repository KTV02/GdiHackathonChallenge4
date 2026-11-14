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
package de.dataport.suchdienst.suchen.controller.beta;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.config.ResourceBundleConfig;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.config.ValidationConfiguration;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.suchen.exception.SuchenException;
import de.dataport.suchdienst.suchen.model.out.common.HyperLink;
import de.dataport.suchdienst.suchen.model.out.common.ValidityPeriodOutModel;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.OnlineServiceDetail;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.OnlineServiceStatus;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.OnlinedienstParameter;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi.AddressType;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi.OrganisationAddressResult;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi.OrganisationResponse;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi.OrganisationResult;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi.OrganisationRoleResult;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi.RoleType;
import de.dataport.suchdienst.suchen.service.OnlineDienstService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@WebMvcTest(controllers = OnlineServiceBetaController.class)
@EnableWebMvc
@AutoConfigureMockMvc(addFilters = false)
@Import({
    ResourceBundleConfig.class,
    ValidationConfiguration.class,
    PvogRequestContext.class,
    SuchdienstProperties.class,
})
class OnlineServiceBetaControllerOrganisationsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OnlineDienstService onlineDienstService;

    @Test
    void getOnlineServiceDetailReturnsCleanV3Beta1Model() throws Exception {
        OnlineServiceDetail response = new OnlineServiceDetail();
        response.setId("test-od");
        response.setStatus(OnlineServiceStatus.PRODUCTION);
        response.setLinks(List.of(new HyperLink("https://service.example", "Titel", "Text")));
        response.setHelpLinks(List.of(new HyperLink("https://help.example", "Hilfe", null)));
        response.setValidityPeriods(
            List.of(
                new ValidityPeriodOutModel(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
            )
        );
        response.setParametersEfa(List.of(new OnlinedienstParameter("efa.contact.url", "value")));
        response.setParametersCustom(
            List.of(new OnlinedienstParameter("custom.parameter", "value"))
        );
        when(onlineDienstService.getOnlineServiceV3Beta1ById("test-od")).thenReturn(response);

        mockMvc
            .perform(
                get("/v3beta1/onlineservices/{id}/detail", "test-od").accept(
                    MediaType.APPLICATION_JSON
                )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PRODUCTION"))
            .andExpect(jsonPath("$.links[0].uri").value("https://service.example"))
            .andExpect(jsonPath("$.links[0].replace").doesNotExist())
            .andExpect(jsonPath("$.links[0].dynamicParameters").doesNotExist())
            .andExpect(jsonPath("$.validityPeriods[0].validFrom").value("2026-01-01"))
            .andExpect(jsonPath("$.validityPeriods[0].gte").doesNotExist())
            .andExpect(jsonPath("$.parametersEfa[0].name").value("efa.contact.url"))
            .andExpect(jsonPath("$.parametersCustom[0].name").value("custom.parameter"))
            .andExpect(jsonPath("$.parameters").doesNotExist());
    }

    @Test
    void getOrganisations_returns200_withMappedOrganisations() throws Exception {
        var role = new OrganisationRoleResult(
            RoleType.COMPETENT_OFFICE,
            "lb-1",
            Set.of("99014001002000"),
            Set.of("091620000000")
        );
        var address = new OrganisationAddressResult(
            AddressType.ADDRESS,
            "München",
            "80335",
            "Prielmayerstr.",
            "7",
            null
        );
        var org = new OrganisationResult("oe-1", "Musteramt", List.of(address), List.of(role));
        var response = new OrganisationResponse(
            List.of(org),
            Instant.parse("2026-01-15T10:00:00Z")
        );
        when(onlineDienstService.getOrganisationsByOdId("test-od")).thenReturn(response);

        mockMvc
            .perform(
                get("/v1beta2/onlineservices/test-od/organisations").accept(
                    MediaType.APPLICATION_JSON
                )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.organisations", hasSize(1)))
            .andExpect(jsonPath("$.organisations[0].oeId").value("oe-1"))
            .andExpect(jsonPath("$.organisations[0].name").value("Musteramt"))
            .andExpect(jsonPath("$.organisations[0].addresses", hasSize(1)))
            .andExpect(jsonPath("$.organisations[0].addresses[0].type").value("ADDRESS"))
            .andExpect(jsonPath("$.organisations[0].addresses[0].city").value("München"))
            .andExpect(jsonPath("$.organisations[0].addresses[0].zip").value("80335"))
            .andExpect(jsonPath("$.organisations[0].addresses[0].houseNumber").value("7"))
            .andExpect(jsonPath("$.organisations[0].roles", hasSize(1)))
            .andExpect(jsonPath("$.organisations[0].roles[0].role").value("COMPETENT_OFFICE"))
            .andExpect(jsonPath("$.organisations[0].roles[0].lbId").value("lb-1"))
            .andExpect(jsonPath("$.externalLastUpdate").exists());
    }

    @Test
    void getOrganisations_returns200_withEmptyOrganisations() throws Exception {
        var response = new OrganisationResponse(List.of(), null);
        when(onlineDienstService.getOrganisationsByOdId("empty-od")).thenReturn(response);

        mockMvc
            .perform(
                get("/v1beta2/onlineservices/empty-od/organisations").accept(
                    MediaType.APPLICATION_JSON
                )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.organisations", hasSize(0)));
    }

    @Test
    void getOrganisations_returns404_whenOdNotFound() throws Exception {
        when(onlineDienstService.getOrganisationsByOdId("unknown")).thenThrow(
            new SuchenException(ErrorCodeEnum.NOT_FOUND, "Nicht gefunden")
        );

        mockMvc
            .perform(
                get("/v1beta2/onlineservices/unknown/organisations").accept(
                    MediaType.APPLICATION_JSON
                )
            )
            .andExpect(status().isNotFound());
    }
}
