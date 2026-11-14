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
package de.dataport.suchdienst.indexer.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.elastic.clients.elasticsearch.core.search.CompletionSuggest;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import de.dataport.suchdienst.config.CustomCorsProperties;
import de.dataport.suchdienst.config.DevSecurityConfig;
import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.config.ResourceBundleConfig;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceResolver;
import de.dataport.suchdienst.elasticsearch.api.ElasticPublicSearchConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.elasticsearch.api.model.PersonalMattersMode;
import de.dataport.suchdienst.indexer.model.AccessChannel;
import de.dataport.suchdienst.indexer.model.LbSuggestion;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetail;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetailContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.Verwaltungsleistung;
import de.dataport.suchdienst.indexer.service.LeikaService;
import de.dataport.suchdienst.suchen.controller.ServiceDescriptionController;
import de.dataport.suchdienst.suchen.controller.beta.ServiceDescriptionBetaController;
import de.dataport.suchdienst.suchen.dynamicparams.DynamicParamsFetcherService;
import de.dataport.suchdienst.suchen.model.out.leistung.ServiceDescriptionSuggestion;
import de.dataport.suchdienst.suchen.service.OrganisationModelConverterService;
import de.dataport.suchdienst.suchen.service.ServiceDescriptionCsvService;
import de.dataport.suchdienst.suchen.service.ServiceDescriptionService;
import de.dataport.suchdienst.suchen.service.ServiceDescriptionSuggestionService;
import de.dataport.suchdienst.suchen.service.VerwaltungsleistungMappingService;
import de.dataport.suchdienst.util.ServiceModuleCode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(
    controllers = { ServiceDescriptionController.class, ServiceDescriptionBetaController.class }
)
@AutoConfigureMockMvc(addFilters = false)
@Import({
    ApiAudienceResolver.class,
    ResourceBundleConfig.class,
    PvogRequestContext.class,
    SuchdienstProperties.class,
    ServiceDescriptionService.class,
    OrganisationModelConverterService.class,
    ServiceDescriptionSuggestionService.class,
    ServiceDescriptionCsvService.class,
    CustomCorsProperties.class,
    DevSecurityConfig.class,
    VerwaltungsleistungMappingService.class,
})
@MockitoBean(types = LeikaService.class)
@MockitoBean(types = DynamicParamsFetcherService.class)
class ServiceDescriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ElasticPublicSearchConnector elasticPublicSearchConnector;

    @MockitoBean
    private ElasticReadConnector elasticReadConnector;

    @Autowired
    private ServiceDescriptionSuggestionService suggestionService;

    @Test
    void testServiceDescriptionSuggestionsDuplicates() {
        List<CompletionSuggestOption<LbSuggestion>> options = List.of(
            createOption("müllentsorgung"),
            createOption("Müllentsorgung")
        );
        Mockito.when(
            elasticPublicSearchConnector.suggestServiceDescription(
                "müll",
                "090000000000",
                true,
                Locale.GERMAN
            )
        ).then(t ->
            List.of(
                CompletionSuggest.<LbSuggestion>of(cs ->
                    cs.text("müll").length(50).offset(0).options(options)
                )
            )
        );

        List<ServiceDescriptionSuggestion> suggestions = suggestionService.searchSuggestions(
            "müll",
            "090000000000",
            true,
            Locale.GERMAN
        );
        assertEquals(1, suggestions.size());
        assertEquals("Müllentsorgung", suggestions.getFirst().getSuggestedText());

        Mockito.when(
            elasticPublicSearchConnector.suggestServiceDescription(
                "mülle",
                "090000000000",
                true,
                Locale.GERMAN
            )
        ).then(t ->
            List.of(
                CompletionSuggest.<LbSuggestion>of(cs ->
                    cs.text("mülle").length(50).offset(0).options(options)
                )
            )
        );

        suggestions = suggestionService.searchSuggestions(
            "müll",
            "090000000000",
            true,
            Locale.GERMAN
        );
        assertEquals(1, suggestions.size());
        assertEquals("Müllentsorgung", suggestions.getFirst().getSuggestedText());

        Mockito.when(
            elasticPublicSearchConnector.suggestServiceDescription(
                "müllen",
                "090000000000",
                false,
                Locale.GERMAN
            )
        ).then(t ->
            List.of(
                CompletionSuggest.<LbSuggestion>of(cs ->
                    cs.text("müllen").length(50).offset(0).options(options)
                )
            )
        );

        suggestions = suggestionService.searchSuggestions(
            "müllen",
            "090000000000",
            false,
            Locale.GERMAN
        );
        assertEquals(2, suggestions.size());
        assertEquals("müllentsorgung", suggestions.get(0).getSuggestedText());
        assertEquals("Müllentsorgung", suggestions.get(1).getSuggestedText());
    }

    @Test
    void testServicedescriptionSuggestion() throws Exception {
        Mockito.when(
            elasticPublicSearchConnector.suggestServiceDescription(
                "gewer",
                "09161000",
                false,
                Locale.GERMAN
            )
        ).then(t ->
            List.of(
                CompletionSuggest.of(cs ->
                    cs
                        .text("gewer")
                        .length(50)
                        .offset(0)
                        .options(o ->
                            o
                                .text("gewerbe")
                                .source(new LbSuggestion("id", List.of("09"), "Gewerbe"))
                        )
                )
            )
        );

        mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    "/v1/servicedescriptions/suggestions/09161000?q=Gewer"
                ).accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].suggestedText").value("Gewerbe"));

        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/v1/servicedescriptions/suggestions/09161000").accept(
                    MediaType.APPLICATION_JSON
                )
            )
            .andExpect(status().isBadRequest());

        mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    "/v1/servicedescriptions/suggestions/091610001231321?q=Gewer"
                ).accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isBadRequest());

        mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    "/v1/servicedescriptions/suggestions/09f?q=Gewer"
                ).accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isBadRequest());
    }

    private CompletionSuggestOption<LbSuggestion> createOption(String text) {
        return CompletionSuggestOption.of(cso ->
            cso.text(text).source(new LbSuggestion("1", List.of("09"), text))
        );
    }

    @Test
    void testServiceDescriptionSearch() throws Exception {
        var mockResponse = Mockito.mock(co.elastic.clients.elasticsearch.core.SearchResponse.class);

        when(mockResponse.hits()).thenReturn(emptyHitsMetadata());
        when(mockResponse.aggregations()).thenReturn(Map.of());

        when(
            elasticPublicSearchConnector.searchServiceDescription(
                Mockito.anyCollection(),
                Mockito.anyCollection(),
                Mockito.anySet(),
                Mockito.anyString(),
                Mockito.anyBoolean(),
                Mockito.isNull(),
                Mockito.any(PersonalMattersMode.class),
                Mockito.anyInt(),
                Mockito.anyInt(),
                Mockito.any(),
                Mockito.anyBoolean(),
                Mockito.isNull()
            )
        ).thenReturn(mockResponse);

        mockMvc
            .perform(MockMvcRequestBuilders.get("/v7/servicedescriptions/09161000?q=Gewerbe"))
            .andExpect(status().isOk());

        mockMvc
            .perform(MockMvcRequestBuilders.get("/v7/servicedescriptions/09161000"))
            .andExpect(status().isOk());

        mockMvc
            .perform(MockMvcRequestBuilders.get("/v7/servicedescriptions/09161000123132?q=Gewerbe"))
            .andExpect(status().isBadRequest());

        mockMvc
            .perform(MockMvcRequestBuilders.get("/v7/servicedescriptions/00dfg?q=Gewerbe"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testBetaServiceDescriptionSearch() throws Exception {
        Hit<Verwaltungsleistung> hit = Hit.of(h ->
            h.source(validVerwaltungsleistung()).index("").id("1")
        );

        var mockResponse = Mockito.mock(co.elastic.clients.elasticsearch.core.SearchResponse.class);
        when(mockResponse.hits()).thenReturn(
            HitsMetadata.<Verwaltungsleistung>of(b ->
                b
                    .hits(List.of(hit))
                    .total(v -> v.value(1L).relation(TotalHitsRelation.Eq))
                    .maxScore(0.0)
            )
        );
        when(mockResponse.aggregations()).thenReturn(Map.of());

        when(
            elasticPublicSearchConnector.searchServiceDescription(
                Mockito.anyCollection(),
                Mockito.anyCollection(),
                Mockito.anySet(),
                Mockito.anyString(),
                Mockito.anyBoolean(),
                Mockito.isNull(),
                Mockito.any(PersonalMattersMode.class),
                Mockito.anyInt(),
                Mockito.anyInt(),
                Mockito.any(),
                Mockito.anyBoolean(),
                Mockito.any(LocalDate.class)
            )
        ).thenReturn(mockResponse);

        mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    "/v8beta1/servicedescriptions?ars=091610000000&q=Gewerbe"
                ).accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.serviceDescriptions.content[0].name").value("Test Leistung"))
            .andExpect(jsonPath("$.serviceDescriptions.content[0].teaser").value("Teaser"))
            .andExpect(jsonPath("$.serviceDescriptions.page.totalElements").value(1))
            .andExpect(jsonPath("$.facades.personalMatters").isArray());

        // Beta-API: fuzzysearch, applyDynamicParams und die volle Leika-Typ-Whitelist sind fest
        // vorgegeben, validDate ist stets "jetzt".
        Mockito.verify(elasticPublicSearchConnector).searchServiceDescription(
            Mockito.anyCollection(),
            Mockito.anyCollection(),
            Mockito.eq(Set.of()),
            Mockito.anyString(),
            Mockito.anyBoolean(),
            Mockito.isNull(),
            Mockito.any(PersonalMattersMode.class),
            Mockito.anyInt(),
            Mockito.anyInt(),
            Mockito.any(),
            Mockito.eq(true),
            Mockito.eq(LocalDate.now())
        );
    }

    @Test
    void testBetaServiceDescriptionSearchRequiresQOrPersonalMatters() throws Exception {
        mockMvc
            .perform(MockMvcRequestBuilders.get("/v8beta1/servicedescriptions?ars=091610000000"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testBetaServiceDescriptionSearchAllowsMissingQWithPersonalMatters() throws Exception {
        var mockResponse = Mockito.mock(co.elastic.clients.elasticsearch.core.SearchResponse.class);
        when(mockResponse.hits()).thenReturn(emptyHitsMetadata());
        when(mockResponse.aggregations()).thenReturn(Map.of());

        when(
            elasticPublicSearchConnector.searchServiceDescription(
                Mockito.anyCollection(),
                Mockito.anyCollection(),
                Mockito.anySet(),
                Mockito.anyString(),
                Mockito.anyBoolean(),
                Mockito.any(),
                Mockito.any(PersonalMattersMode.class),
                Mockito.anyInt(),
                Mockito.anyInt(),
                Mockito.any(),
                Mockito.anyBoolean(),
                Mockito.any(LocalDate.class)
            )
        ).thenReturn(mockResponse);

        mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    "/v8beta1/servicedescriptions?ars=091610000000&facades=true&page=0&personalMatters=1010000&personalMatters=1010100&personalMatters=1010200&size=10"
                )
            )
            .andExpect(status().isOk());
    }

    @Test
    void testBetaServiceDescriptionSearchRequiresArs() throws Exception {
        mockMvc
            .perform(MockMvcRequestBuilders.get("/v8beta1/servicedescriptions?q=Gewerbe"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testBetaServiceDescriptionSearchDetectsLeikaId() throws Exception {
        var mockResponse = Mockito.mock(co.elastic.clients.elasticsearch.core.SearchResponse.class);
        when(mockResponse.hits()).thenReturn(emptyHitsMetadata());
        when(mockResponse.aggregations()).thenReturn(Map.of());

        when(
            elasticPublicSearchConnector.searchServiceDescription(
                Mockito.anyCollection(),
                Mockito.anyCollection(),
                Mockito.anySet(),
                Mockito.anyString(),
                Mockito.anyBoolean(),
                Mockito.isNull(),
                Mockito.any(PersonalMattersMode.class),
                Mockito.anyInt(),
                Mockito.anyInt(),
                Mockito.any(),
                Mockito.anyBoolean(),
                Mockito.any(LocalDate.class)
            )
        ).thenReturn(mockResponse);

        mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    "/v8beta1/servicedescriptions?ars=091610000000&q=99001009000555"
                ).accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk());

        // PV-9484: Ein einzelnes, syntaktisch gültiges LeiKa-Schlüssel-Suchkriterium wird als
        // Leika-Id-Suche statt als Volltextsuche interpretiert.
        Mockito.verify(elasticPublicSearchConnector).searchServiceDescription(
            Mockito.eq(List.of("99001009000555")),
            Mockito.eq(Set.of()),
            Mockito.anySet(),
            Mockito.anyString(),
            Mockito.anyBoolean(),
            Mockito.isNull(),
            Mockito.any(PersonalMattersMode.class),
            Mockito.anyInt(),
            Mockito.anyInt(),
            Mockito.any(),
            Mockito.anyBoolean(),
            Mockito.any(LocalDate.class)
        );
    }

    @Test
    void testBetaServiceDescriptionSearchFacadesEnablesFullPersonalMattersMode() throws Exception {
        var mockResponse = Mockito.mock(co.elastic.clients.elasticsearch.core.SearchResponse.class);
        when(mockResponse.hits()).thenReturn(emptyHitsMetadata());
        when(mockResponse.aggregations()).thenReturn(Map.of());

        when(
            elasticPublicSearchConnector.searchServiceDescription(
                Mockito.anyCollection(),
                Mockito.anyCollection(),
                Mockito.anySet(),
                Mockito.anyString(),
                Mockito.anyBoolean(),
                Mockito.isNull(),
                Mockito.any(PersonalMattersMode.class),
                Mockito.anyInt(),
                Mockito.anyInt(),
                Mockito.any(),
                Mockito.anyBoolean(),
                Mockito.any(LocalDate.class)
            )
        ).thenReturn(mockResponse);

        mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    "/v8beta1/servicedescriptions?ars=091610000000&q=Gewerbe&facades=true"
                ).accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk());

        Mockito.verify(elasticPublicSearchConnector).searchServiceDescription(
            Mockito.anyCollection(),
            Mockito.anyCollection(),
            Mockito.anySet(),
            Mockito.anyString(),
            Mockito.anyBoolean(),
            Mockito.isNull(),
            Mockito.eq(PersonalMattersMode.FULL),
            Mockito.anyInt(),
            Mockito.anyInt(),
            Mockito.any(),
            Mockito.anyBoolean(),
            Mockito.any(LocalDate.class)
        );
    }

    @Test
    void testBetaLeikaIdSearchUsesPagedSearchModel() throws Exception {
        var mockResponse = Mockito.mock(co.elastic.clients.elasticsearch.core.SearchResponse.class);
        when(mockResponse.hits()).thenReturn(emptyHitsMetadata());
        when(
            elasticPublicSearchConnector.searchServiceDescription(
                Mockito.anyCollection(),
                Mockito.anyCollection(),
                Mockito.anySet(),
                Mockito.anyString(),
                Mockito.anyBoolean(),
                Mockito.anySet(),
                Mockito.any(PersonalMattersMode.class),
                Mockito.anyInt(),
                Mockito.anyInt(),
                Mockito.any(),
                Mockito.anyBoolean(),
                Mockito.any(LocalDate.class)
            )
        ).thenReturn(mockResponse);

        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/v4beta1/servicedescriptions/leikaid")
                    .param("leikaIds", "99001009000555")
                    .param("ars", "091610000000")
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void testBetaLeikaHierarchySearchMarksEmptyResultAsNone() throws Exception {
        when(
            elasticPublicSearchConnector.searchServiceDescriptionByLeikaHierarchy(
                Mockito.anySet(),
                Mockito.anySet(),
                Mockito.anyString(),
                Mockito.anyInt(),
                Mockito.anyInt(),
                Mockito.any(LocalDate.class)
            )
        ).thenReturn(emptyHitsMetadata());

        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/v3beta1/servicedescriptions/leikahierarchy")
                    .param("leikaIds", "99001009000555")
                    .param("ars", "091610000000")
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.serviceDescriptions.content").isArray())
            .andExpect(jsonPath("$.serviceDescriptions.page.totalElements").value(0))
            .andExpect(jsonPath("$.leikaMatch.matchType").value("NONE"))
            .andExpect(jsonPath("$.leikaMatch.effectiveLeikaIds").isEmpty());
    }

    @Test
    void testServicedescriptionDetail() throws Exception {
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/v5/servicedescriptions/091610000000/detail?q=0123456")
            )
            .andExpect(status().isNotFound());

        mockMvc
            .perform(MockMvcRequestBuilders.get("/v5/servicedescriptions/091610000000/detail"))
            .andExpect(status().isBadRequest());

        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/v5/servicedescriptions/0916100000001/detail?q=123")
            )
            .andExpect(status().isBadRequest());

        mockMvc
            .perform(MockMvcRequestBuilders.get("/v5/servicedescriptions/09161000000/detail?q=123"))
            .andExpect(status().isBadRequest());

        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/v5/servicedescriptions/09161000000f/detail?q=123")
            )
            .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testServicedescriptionDetailV6(boolean publicDisplayAllowed) throws Exception {
        Verwaltungsleistung vl = validVerwaltungsleistung();
        OrganisationseinheitVL oe = new OrganisationseinheitVL();
        oe.setId("oe-1");
        oe.setName("Amt A");
        oe.setOeffentlichAnzeigen(publicDisplayAllowed);
        OrganisationseinheitVL defaultOe = new OrganisationseinheitVL();
        defaultOe.setId("oe-2");
        defaultOe.setName("Amt B");
        vl.setOrganisationseinheiten(List.of(oe, defaultOe));
        when(
            elasticPublicSearchConnector.searchLbByIdAndArs(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyBoolean(),
                Mockito.anyBoolean(),
                Mockito.any()
            )
        ).thenReturn(Optional.of(vl));

        mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    "/v6/servicedescriptions/0123456/detail?ars=091610000000"
                ).accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.organisations[0].id").value("oe-1"))
            .andExpect(
                jsonPath("$.organisations[0].isPublicDisplayAllowed").value(publicDisplayAllowed)
            )
            .andExpect(jsonPath("$.organisations[1].isPublicDisplayAllowed").value(true))
            .andExpect(jsonPath("$.organisations[0].publicDisplayAllowed").doesNotExist())
            .andExpect(jsonPath("$.organisations[0].oeffentlichAnzeigen").doesNotExist())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/json"));

        Mockito.verify(elasticPublicSearchConnector).searchLbByIdAndArs(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyBoolean(),
            Mockito.anyBoolean(),
            Mockito.any()
        );
        Mockito.verifyNoInteractions(elasticReadConnector);
    }

    private Verwaltungsleistung validVerwaltungsleistung() {
        var vl = new Verwaltungsleistung();
        vl.setId("1");

        var lb = new LeistungsbeschreibungVL();
        lb.setId("L100012.LB.257406749");

        vl.setLeistungsbeschreibung(lb);

        Map<String, List<AbstractServiceModule>> details = new HashMap<>(
            Map.of(
                ServiceModuleCode.LEISTUNGSBEZEICHNUNG.getCode(),
                new ArrayList<>(
                    List.of(
                        ServiceModule.builder().title("Test Leistung").text("Test Leistung").build()
                    )
                ),
                ServiceModuleCode.VOLLTEXT.getCode(),
                new ArrayList<>(
                    List.of(
                        ServiceModule.builder()
                            .title("Volltext")
                            .text("Volltext")
                            .accessChannel(AccessChannel.ONLINE_OFFLINE)
                            .build()
                    )
                ),
                ServiceModuleCode.TEASER.getCode(),
                new ArrayList<>(
                    List.of(
                        ServiceModule.builder()
                            .title("Teaser")
                            .text("Teaser")
                            .accessChannel(AccessChannel.OFFLINE)
                            .build(),
                        ServiceModule.builder()
                            .title("Teaser")
                            .text("Teaser")
                            .accessChannel(AccessChannel.ONLINE)
                            .build()
                    )
                ),
                ServiceModuleCode.KOSTEN.getCode(),
                new ArrayList<>(
                    List.of(
                        ServiceModuleDetail.builder()
                            .title("Kosten")
                            .text("Kosten")
                            .accessChannel(AccessChannel.ONLINE_OFFLINE)
                            .details(
                                List.of(
                                    ServiceModuleDetailContent.builder()
                                        .title("Kosten Details")
                                        .text("Details zu Kosten")
                                        .build()
                                )
                            )
                            .build()
                    )
                )
            )
        );

        lb.setDetails(details);

        return vl;
    }

    @Test
    void testSorting() throws Exception {
        when(
            elasticPublicSearchConnector.searchLbByIdAndArs(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyBoolean(),
                Mockito.anyBoolean(),
                Mockito.any()
            )
        ).thenReturn(Optional.of(verwaltungsleistungSorting()));

        mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    "/v6/servicedescriptions/0123456/detail?ars=091610000000"
                ).accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/json"))
            .andExpect(jsonPath("$.details[0].text").value("Teaser"))
            .andExpect(jsonPath("$.details[1].text").value("Volltext"))
            .andExpect(jsonPath("$.details[2].text").value("Kosten"))
            .andExpect(jsonPath("$.details[3].text").value("Frist"));
        Mockito.verify(elasticPublicSearchConnector).searchLbByIdAndArs(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyBoolean(),
            Mockito.anyBoolean(),
            Mockito.any()
        );
        Mockito.verifyNoInteractions(elasticReadConnector);
        // unsortiert
        // .andExpect(jsonPath("$.details[4].text").value("Leistungsbezeichnung"))
        // .andExpect(jsonPath("$.details[5].text").value("Bearbeitungsdauer"))
        // .andExpect(jsonPath("$.details[6].text").value("ModulIndividuell"))
    }

    private Verwaltungsleistung verwaltungsleistungSorting() {
        var vl = new Verwaltungsleistung();
        vl.setId("1");

        var lb = new LeistungsbeschreibungVL();
        lb.setId("L100012.LB.257406749");

        vl.setLeistungsbeschreibung(lb);
        Map<String, List<AbstractServiceModule>> details = new HashMap<>(
            Map.of(
                ServiceModuleCode.LEISTUNGSBEZEICHNUNG.getCode(),
                new ArrayList<>(
                    List.of(
                        ServiceModule.builder()
                            .title("Leistungsbezeichnung")
                            .text("Leistungsbezeichnung")
                            .build()
                    )
                ),
                ServiceModuleCode.INDIVIDUELL.getCode(),
                new ArrayList<>(
                    List.of(
                        ServiceModule.builder()
                            .title("ModulIndividuell")
                            .text("ModulIndividuell")
                            .build()
                    )
                ),
                ServiceModuleCode.BEARBEITUNGSDAUER.getCode(),
                new ArrayList<>(
                    List.of(
                        ServiceModule.builder()
                            .title("Bearbeitungsdauer")
                            .text("Bearbeitungsdauer")
                            .accessChannel(AccessChannel.OFFLINE)
                            .build()
                    )
                ),
                ServiceModuleCode.VOLLTEXT.getCode(),
                new ArrayList<>(
                    List.of(ServiceModule.builder().title("Volltext").text("Volltext").build())
                ),
                ServiceModuleCode.TEASER.getCode(),
                new ArrayList<>(
                    List.of(ServiceModule.builder().title("Teaser").text("Teaser").build())
                ),
                ServiceModuleCode.KOSTEN.getCode(),
                new ArrayList<>(
                    List.of(
                        ServiceModuleDetail.builder()
                            .title("Kosten")
                            .text("Kosten")
                            .accessChannel(AccessChannel.ONLINE_OFFLINE)
                            .details(
                                List.of(
                                    ServiceModuleDetailContent.builder()
                                        .title("Kosten Details")
                                        .text("Details zu Kosten")
                                        .build()
                                )
                            )
                            .build()
                    )
                ),
                ServiceModuleCode.FRIST.getCode(),
                new ArrayList<>(
                    List.of(
                        ServiceModule.builder()
                            .title("Frist")
                            .text("Frist")
                            .accessChannel(AccessChannel.OFFLINE)
                            .build()
                    )
                )
            )
        );

        lb.setDetails(details);

        return vl;
    }

    private <T> HitsMetadata<T> emptyHitsMetadata() {
        return HitsMetadata.of(b ->
            b
                .hits(Collections.emptyList())
                .total(v -> v.value(0L).relation(TotalHitsRelation.Eq))
                .maxScore(0.0)
        );
    }
}
