// SPDX-License-Identifier: EUPL-1.2
// Copyright (c) 2026 Dataport AöR.
// Beschreibung: Tests der Beta-Detailausgabe und der unveränderten V5-Ausgabe.
package de.dataport.suchdienst.suchen.controller.beta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.dataport.suchdienst.config.Jackson3Configuration;
import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.config.ResourceBundleConfig;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.config.ValidationConfiguration;
import de.dataport.suchdienst.elasticsearch.api.ElasticPublicSearchConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticQueryConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity23;
import de.dataport.suchdienst.indexer.model.mapping.Communication;
import de.dataport.suchdienst.indexer.model.mapping.Contact;
import de.dataport.suchdienst.indexer.model.mapping.Link;
import de.dataport.suchdienst.indexer.model.mapping.Location;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.Verwaltungsleistung;
import de.dataport.suchdienst.suchen.controller.OrganisationUnitController;
import de.dataport.suchdienst.suchen.service.OrganisationModelConverterService;
import de.dataport.suchdienst.suchen.service.OrganisationUnitService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = { OrganisationUnitBetaController.class, OrganisationUnitController.class }
)
@AutoConfigureMockMvc(addFilters = false)
@Import({
    ResourceBundleConfig.class,
    Jackson3Configuration.class,
    ValidationConfiguration.class,
    PvogRequestContext.class,
    SuchdienstProperties.class,
    OrganisationUnitService.class,
    OrganisationModelConverterService.class,
})
class OrganisationUnitBetaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ElasticPublicSearchConnector publicSearchConnector;

    @MockitoBean
    private ElasticReadConnector readConnector;

    @MockitoBean
    private ElasticQueryConnector queryConnector;

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void returnsFlagsAtEveryLevel(boolean allowed) throws Exception {
        mockOrganisation(allowed);
        LocalDate requestDate = LocalDate.now();

        mockMvc
            .perform(
                get("/v6beta1/organisationunits/{oeid}/detail", "oe-1")
                    .param("ars", "091620000000")
                    .param("lbId", "lb-1")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Musteramt"))
            .andExpect(jsonPath("$.isPublicDisplayAllowed").value(allowed))
            .andExpect(jsonPath("$.communicationSystems[0].isPublicDisplayAllowed").value(allowed))
            .andExpect(
                jsonPath("$.location.communications[0].isPublicDisplayAllowed").value(allowed)
            )
            .andExpect(jsonPath("$.employees[0].isPublicDisplayAllowed").value(allowed))
            .andExpect(jsonPath("$.employees[0].relations").doesNotExist())
            .andExpect(
                jsonPath("$.employees[0].additionalLinks[0].uri").value(
                    "https://example.org/kontakt"
                )
            )
            .andExpect(jsonPath("$.employees[0].additionalLinks[0].title").value("Kontakt"))
            .andExpect(jsonPath("$.employees[0].additionalLinks[0].description").value("Text"))
            .andExpect(jsonPath("$.employees[0].additionalLinks[0].replace").doesNotExist())
            .andExpect(
                jsonPath("$.employees[0].additionalLinks[0].dynamicParameters").doesNotExist()
            )
            .andExpect(
                jsonPath("$.employees[0].location.communications[0].isPublicDisplayAllowed").value(
                    allowed
                )
            )
            .andExpect(jsonPath("$..publicDisplayAllowed").isEmpty())
            .andExpect(jsonPath("$..oeffentlichAnzeigen").isEmpty());

        ArgumentCaptor<LocalDate> validDate = ArgumentCaptor.forClass(LocalDate.class);
        verify(publicSearchConnector).searchVerwaltungsleistungByLbOe(
            any(),
            isNull(),
            validDate.capture(),
            eq("lb-1"),
            eq("oe-1")
        );
        assertThat(validDate.getValue()).isBetween(requestDate, LocalDate.now());
    }

    @Test
    void stableEndpointDoesNotExposeFlags() throws Exception {
        mockOrganisation(false);

        mockMvc
            .perform(
                get("/v5/organisationunits/detail")
                    .param("q", "oe-1")
                    .param("ars", "091620000000")
                    .param("lbId", "lb-1")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.employees[0].name").value("Erika"))
            .andExpect(jsonPath("$.employees[0].relations[0].lbId").value("lb-1"))
            .andExpect(
                jsonPath("$.employees[0].additionalLinks[0].uri").value(
                    "https://example.org/kontakt"
                )
            )
            .andExpect(jsonPath("$.employees[0].additionalLinks[0].replace").value(false))
            .andExpect(jsonPath("$.employees[0].additionalLinks[0].dynamicParameters").isArray())
            .andExpect(jsonPath("$.communicationSystems[0].value").value("kontakt@example.org"))
            .andExpect(jsonPath("$..isPublicDisplayAllowed").isEmpty());
    }

    @Test
    void omitsEmployeesWithoutServiceDescription() throws Exception {
        mockOrganisation(false);

        mockMvc
            .perform(get("/v6beta1/organisationunits/{oeid}/detail", "oe-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isPublicDisplayAllowed").value(false))
            .andExpect(jsonPath("$.employees").isEmpty())
            .andExpect(jsonPath("$.nestedForms").isEmpty());
    }

    @Test
    void returnsNotFound() throws Exception {
        when(
            publicSearchConnector.searchVerwaltungsleistungByLbOe(
                any(),
                isNull(),
                any(),
                any(),
                eq("missing")
            )
        ).thenReturn(Optional.empty());

        mockMvc
            .perform(get("/v6beta1/organisationunits/{oeid}/detail", "missing"))
            .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidParameters() throws Exception {
        mockMvc.perform(get("/v6beta1/organisationunits/detail")).andExpect(status().isNotFound());
        mockMvc
            .perform(get("/v6beta1/organisationunits/{oeid}/detail", "x"))
            .andExpect(status().isBadRequest());
        mockMvc
            .perform(get("/v6beta1/organisationunits/{oeid}/detail", "oe-1").param("ars", "1"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void usesPathIdAndCurrentDateDespiteFormerQueryParameters() throws Exception {
        mockOrganisation(false);
        LocalDate requestDate = LocalDate.now();

        mockMvc
            .perform(
                get("/v6beta1/organisationunits/{oeid}/detail", "oe-1")
                    .param("q", "missing")
                    .param("validDate", "2000-01-01")
            )
            .andExpect(status().isOk());

        ArgumentCaptor<LocalDate> validDate = ArgumentCaptor.forClass(LocalDate.class);
        verify(publicSearchConnector).searchVerwaltungsleistungByLbOe(
            any(),
            isNull(),
            validDate.capture(),
            isNull(),
            eq("oe-1")
        );
        assertThat(validDate.getValue()).isBetween(requestDate, LocalDate.now());
    }

    private void mockOrganisation(boolean allowed) {
        Communication communication = new Communication();
        communication.setName("E-Mail");
        communication.setValue("kontakt@example.org");
        communication.setOeffentlichAnzeigen(allowed);
        Location location = new Location();
        location.setCommunications(List.of(communication));
        Contact contact = new Contact();
        contact.setName("Erika");
        contact.setAdditionalLinks(
            List.of(Link.of("https://example.org/kontakt", "Kontakt", "Text"))
        );
        contact.setLocation(location);
        contact.setOeffentlichAnzeigen(allowed);
        var relation = new ZustaendigkeitEntity23(Instant.parse("2026-10-16T00:00:00Z"));
        relation.setArs("090000000000");
        relation.setLbId("lb-1");
        contact.setRelations(Set.of(relation));
        OrganisationseinheitVL oe = new OrganisationseinheitVL();
        oe.setId("oe-1");
        oe.setName("Musteramt");
        oe.setOeffentlichAnzeigen(allowed);
        oe.setLocation(location);
        oe.setEmployees(List.of(contact));
        oe.setCommunicationSystems(Map.of("090000000000", List.of(communication)));
        Verwaltungsleistung vl = new Verwaltungsleistung();
        vl.setOrganisationseinheiten(List.of(oe));
        when(
            publicSearchConnector.searchVerwaltungsleistungByLbOe(
                any(),
                isNull(),
                any(),
                any(),
                eq("oe-1")
            )
        ).thenReturn(Optional.of(vl));
    }
}
