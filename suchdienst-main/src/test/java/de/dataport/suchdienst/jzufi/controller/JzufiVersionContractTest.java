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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.dataport.suchdienst.config.CustomCorsProperties;
import de.dataport.suchdienst.config.DevSecurityConfig;
import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.config.ResourceBundleConfig;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.config.WebMvcConfig;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceResolver;
import de.dataport.suchdienst.elasticsearch.api.exception.ElasticSearchException;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.importer.model.GenericLeistung;
import de.dataport.suchdienst.importer.model.GenericOnlinedienst;
import de.dataport.suchdienst.importer.model.GenericOrganisationseinheit;
import de.dataport.suchdienst.importer.model.GenericZustaendigkeitTransferObjekt;
import de.dataport.suchdienst.jzufi.model.mapper.JzufiModelMapper;
import de.dataport.suchdienst.suchen.service.OnlineDienstService;
import de.dataport.suchdienst.suchen.service.OrganisationUnitService;
import de.dataport.suchdienst.suchen.service.RelationService;
import de.dataport.suchdienst.suchen.service.ServiceDescriptionService;
import de.dataport.suchdienst.util.MdcTemplate;
import de.dataport.suchdienst.util.model.MdcKeys;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/**
 * Testziel: Alle Bestandsrouten geben die PV-Schema-Serviceergebnisse unverändert zurück,
 * einschließlich der alten versionsumschaltbaren Routen. Nur die vier neuen 2.3-v2-Routen
 * verwenden eigene Modelle und @type. HTTP-Antworten werden mit der jeweiligen
 * Modellserialisierung verglichen, nicht mit künstlich angepassten PV-Schema-Mix-ins.
 * Nur Bestandsrouten senden den Deprecation-Header; die neuen v2-Routen bleiben aktiv.
 * Zuständigkeitsuntertypen ohne eigene Feldwerte behalten im Relations-JSON ihren @type.
 */
@WebMvcTest(
    controllers = {
        LeistungJzufiController.class,
        OrganisationJzufiController.class,
        OnlinedienstJzufiController.class,
        ZustaendigkeitJzufiController.class,
    }
)
@TestPropertySource(
    properties = {
        "de.dataport.suchdienst.endpoint.enableDeprecated.v3-servicedescriptions-jzufi=true",
        "de.dataport.suchdienst.endpoint.enableDeprecated.v2-organisationunits-jzufi=true",
        "de.dataport.suchdienst.endpoint.enableDeprecated.v3-relations-ars-jzufi=true",
        "de.dataport.suchdienst.endpoint.enableDeprecated.v3-relations-jzufi=true",
    }
)
@Import({
    ApiAudienceResolver.class,
    ResourceBundleConfig.class,
    PvogRequestContext.class,
    SuchdienstProperties.class,
    DevSecurityConfig.class,
    CustomCorsProperties.class,
    WebMvcConfig.class,
})
class JzufiVersionContractTest {

    @MockitoBean
    private ServiceDescriptionService service;

    @MockitoBean
    private OrganisationUnitService organisation;

    @MockitoBean
    private OnlineDienstService online;

    @MockitoBean
    private RelationService relations;

    @Autowired
    private LeistungJzufiController serviceController;

    @Autowired
    private OrganisationJzufiController organisationController;

    @Autowired
    private OnlinedienstJzufiController onlineController;

    @Autowired
    private ZustaendigkeitJzufiController relationController;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JsonMapper mapper;

    private final de.xoev.xzufi.v2_3_1.Leistung service23 = new de.xoev.xzufi.v2_3_1.Leistung();
    private final de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert organisation23 =
        new de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert();
    private final de.xoev.xzufi.v2_3_1.OnlinedienstErweitert online23 =
        new de.xoev.xzufi.v2_3_1.OnlinedienstErweitert();
    private final de.xoev.xzufi.v2_3_1.ZustaendigkeitTransferObjekt relation23 =
        new de.xoev.xzufi.v2_3_1.ZustaendigkeitTransferObjekt();
    private de.xoev.xzufi.v2_2_0.Leistung service22;
    private de.xoev.xzufi.v2_2_0.OrganisationseinheitErweitert organisation22;
    private de.xoev.xzufi.v2_2_0.OnlinedienstErweitert online22;
    private de.xoev.xzufi.v2_2_0.ZustaendigkeitTransferObjekt relation22;

    /** Minimale PV-Serviceergebnisse isolieren Routing und Ausgabeadapter von Import, Fachlogik und Versionskonvertierung. */
    @BeforeEach
    void prepareServiceResults() {
        var responsibility = new de.xoev.xzufi.v2_3_1.ZustaendigkeitOrganisationseinheit();
        organisation23.getZustaendigkeit().add(responsibility);
        relation23.getZustaendigkeit().add(responsibility);
        online23.getZustaendigkeit().add(new de.xoev.xzufi.v2_3_1.ZustaendigkeitOnlinedienst());
        service22 = new de.xoev.xzufi.v2_2_0.Leistung();
        organisation22 = new de.xoev.xzufi.v2_2_0.OrganisationseinheitErweitert();
        organisation22
            .getZustaendigkeit()
            .add(new de.xoev.xzufi.v2_2_0.ZustaendigkeitOrganisationseinheit());
        online22 = new de.xoev.xzufi.v2_2_0.OnlinedienstErweitert();
        online22.getZustaendigkeit().add(new de.xoev.xzufi.v2_2_0.Zustaendigkeit());
        relation22 = new de.xoev.xzufi.v2_2_0.ZustaendigkeitTransferObjekt();
        relation22
            .getZustaendigkeit()
            .add(new de.xoev.xzufi.v2_2_0.ZustaendigkeitOrganisationseinheit());
        var genericService = mock(GenericLeistung.class);
        when(genericService.getOrConvertXZuFi22()).thenReturn(service22);
        when(genericService.getOrConvertXZuFi23()).thenReturn(service23);
        when(service.fetchJzufi(any(), any())).thenReturn(Optional.of(genericService));
        var genericOrganisation = mock(GenericOrganisationseinheit.class);
        when(genericOrganisation.getOrConvertXZuFi22()).thenReturn(organisation22);
        when(genericOrganisation.getOrConvertXZuFi23()).thenReturn(organisation23);
        when(organisation.getJzufi(any(), any())).thenReturn(Optional.of(genericOrganisation));
        var genericOnline = mock(GenericOnlinedienst.class);
        when(genericOnline.getOrConvertXZuFi22()).thenReturn(online22);
        when(genericOnline.getOrConvertXZuFi23()).thenReturn(online23);
        when(genericOnline.isOeffentlichAnzeigen()).thenReturn(true);
        when(online.getJzufi(any(), any())).thenReturn(Optional.of(genericOnline));
        var genericRelation = mock(GenericZustaendigkeitTransferObjekt.class);
        when(genericRelation.getOrConvertXZuFi22()).thenReturn(relation22);
        when(genericRelation.getOrConvertXZuFi23()).thenReturn(relation23);
        // Die Gesamtzahl ist bewusst größer als die Fixture-Seite, damit falsche Metadaten aus content.size() auffallen.
        when(
            relations.getRelationZustaendigkeitTransferObjektXzufiByLbIdAndArs(
                any(),
                any(),
                anyBoolean(),
                any(),
                anyInt(),
                anyInt()
            )
        ).thenReturn(Pair.of(12L, List.of(genericRelation)));
    }

    /**
     * Deckt feste Versionspfade und die alte Accept-Header-Umschaltung ab.
     * Objektidentität und HTTP-JSON prüfen getrennt, dass Bestandsrouten keine lokalen Ausgabemodelle verwenden.
     */
    @ParameterizedTest
    @ValueSource(strings = { "2-3" })
    // Die deprecated Routen werden bis zu ihrer Entfernung bewusst auf Rückwärtskompatibilität geprüft.
    @SuppressWarnings("java:S5738")
    void existingRoutesReturnUnmappedPvSchemaModels(String version) throws Exception {
        boolean v22 = version.equals("2-2");
        Object expectedService = v22 ? service22 : service23;
        Object expectedOrganisation = v22 ? organisation22 : organisation23;
        Object expectedOnline = v22 ? online22 : online23;
        Object expectedRelation = v22 ? relation22 : relation23;
        // Gleiche JSON-Felder allein würden ein versehentliches Mapping auf eine andere Modellklasse nicht erkennen.
        assertSame(
            expectedService,
            v22
                ? serviceController.getJzufiLb220V1("id", null)
                : serviceController.getJzufiLb231V1("id", null)
        );
        assertSame(
            expectedOrganisation,
            v22
                ? organisationController.getJzufiOe220V1("id", null)
                : organisationController.getJzufiOe231V1("id", null)
        );
        assertSame(
            expectedOnline,
            v22
                ? onlineController.getJzufiOd220V1("id", null)
                : onlineController.getJzufiOd231V1("id", null)
        );
        var page = v22
            ? relationController.getZsTrByLbIdAndArs220V1("01", List.of("id"), null, false, 0, 5)
            : relationController.getZsTrByLbIdAndArs231V1("01", List.of("id"), null, false, 0, 5);
        assertSame(expectedRelation, page.getContent().getFirst());
        assertHttpJson("/v1/servicedescriptions/id/jzufi-" + version, expectedService, false, true);
        assertHttpJson(
            "/v1/organisationunits/id/jzufi-" + version,
            expectedOrganisation,
            false,
            true
        );
        assertHttpJson("/v1/onlineservices/id/jzufi-" + version, expectedOnline, false, true);
        assertHttpJson(
            "/v1/relations/jzufi-" + version + "?ars=01&lbids=id&size=5",
            page,
            false,
            true
        );
        var accept = MediaType.parseMediaType(
            "application/json;xzufi-version=" + (v22 ? "2.2.0" : "2.3.1")
        );
        assertSame(expectedService, serviceController.getJzufiLbV3(accept, "id", null).getBody());
        assertSame(
            expectedOrganisation,
            organisationController.getJzufiOeV2(accept, "id", null).getBody()
        );
        assertSame(
            expectedRelation,
            relationController
                .getZsTrByLbIdAndArsV3(accept, "01", List.of("id"), null, false, 0, 5)
                .getContent()
                .getFirst()
        );
        for (var legacy : List.of(
            Pair.of("/v3/servicedescriptions/jzufi?id=id", expectedService),
            Pair.of("/v2/organisationunits/jzufi?id=id", expectedOrganisation),
            Pair.of("/v3/relations/01/jzufi?lbids=id&size=5", page)
        )) {
            assertHttpJson(legacy.getLeft(), legacy.getRight(), false, true, accept);
        }
    }

    /** Bei identischen Basisfeldern darf allein die konkrete Klasse den unterschiedlichen @type bestimmen. */
    @Test
    void relationSubtypeWithOnlyInheritedFieldsKeepsItsDiscriminator() throws Exception {
        relation23.getZustaendigkeit().clear();
        for (var responsibility : List.of(
            new de.xoev.xzufi.v2_3_1.Zustaendigkeit(),
            new de.xoev.xzufi.v2_3_1.ZustaendigkeitOrganisationseinheit()
        )) {
            var serviceId = new de.xoev.xzufi.v2_3_1.Identifikator();
            serviceId.setValue("lb");
            responsibility.getLeistungID().add(serviceId);
            var regionId = new de.xoev.xzufi.v2_3_1.Identifikator();
            regionId.setValue("gebiet");
            responsibility.getGebietID().add(regionId);
            relation23.getZustaendigkeit().add(responsibility);
        }
        var response = mvc
            .perform(
                get("/v2/relations/jzufi-2-3")
                    .param("ars", "01")
                    .param("lbids", "lb")
                    .param("size", "5")
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
        var responsibilities = mapper
            .readTree(response.getContentAsString())
            .at("/content/0/zustaendigkeit");
        assertEquals(
            mapper.readTree(
                """
                [
                  {"@type":"Zustaendigkeit","leistungID":[{"value":"lb"}],"gebietID":[{"value":"gebiet"}]},
                  {"@type":"ZustaendigkeitOrganisationseinheit","leistungID":[{"value":"lb"}],"gebietID":[{"value":"gebiet"}]}
                ]
                """
            ),
            responsibilities
        );
    }

    /**
     * Prüft die Verdrahtung der lokalen Modelle und HTTP-Metadaten, nicht die Mappingwerte selbst.
     * Der produktive Mapper dient hier nur als Serialisierungserwartung; Feldverluste prüfen die separaten Mappertests.
     */
    @Test
    void newRoutesUseOnlyTheLocal23ModelsAndKeepPagination() throws Exception {
        assertInstanceOf(
            de.dataport.suchdienst.jzufi.model.v23.Leistung.class,
            serviceController.getJzufiLb231V2("id")
        );
        assertInstanceOf(
            de.dataport.suchdienst.jzufi.model.v23.OrganisationseinheitErweitert.class,
            organisationController.getJzufiOe231V2("id")
        );
        assertInstanceOf(
            de.dataport.suchdienst.jzufi.model.v23.OnlinedienstErweitert.class,
            onlineController.getJzufiOd231V2("id")
        );
        var page = relationController.getZsTrByLbIdAndArs231V2("01", List.of("id"), false, 0, 5);
        assertInstanceOf(
            de.dataport.suchdienst.jzufi.model.v23.ZustaendigkeitTransferObjekt.class,
            page.getContent().getFirst()
        );
        assertEquals(new PagedModel.PageMetadata(5, 0, 12, 3), page.getMetadata());
        assertHttpJson(
            "/v2/servicedescriptions/id/jzufi-2-3",
            JzufiModelMapper.INSTANCE.map23(service23),
            false,
            false
        );
        assertHttpJson(
            "/v2/organisationunits/id/jzufi-2-3",
            JzufiModelMapper.INSTANCE.map23(organisation23),
            true,
            false
        );
        assertHttpJson(
            "/v2/onlineservices/id/jzufi-2-3",
            JzufiModelMapper.INSTANCE.map23(online23),
            true,
            false
        );
        assertHttpJson("/v2/relations/jzufi-2-3?ars=01&lbids=id&size=5", page, true, false);
    }

    private void assertHttpJson(String path, Object expected, boolean hasType, boolean deprecated)
        throws Exception {
        assertHttpJson(path, expected, hasType, deprecated, MediaType.APPLICATION_JSON);
    }

    /**
     * Vergleicht die MVC-Ausgabe mit der Modellserialisierung und prüft Versionierungsmetadaten.
     *
     * @param hasType ob irgendwo in der Antwort ein {@code @type} erwartet wird, nicht zwingend am Root
     * @param deprecated ob der Deprecation-Header vorhanden sein muss
     * @param accept erlaubt auch die Versionsauswahl der alten headerbasierten Routen
     */
    private void assertHttpJson(
        String path,
        Object expected,
        boolean hasType,
        boolean deprecated,
        MediaType accept
    ) throws Exception {
        var result = mvc.perform(get(path).accept(accept)).andExpect(status().isOk());
        if (deprecated) {
            result.andExpect(header().string("X-PVOG-DEPRECATED", "true"));
        } else {
            result.andExpect(header().doesNotExist("X-PVOG-DEPRECATED"));
        }
        var body = result.andReturn().getResponse().getContentAsString();
        assertEquals(mapper.readTree(mapper.writeValueAsString(expected)), mapper.readTree(body));
        assertEquals(hasType, body.contains("\"@type\""));
    }

    @Test
    void relationFailureReturnsApiErrorWithRequestIdWithoutInternalDetails() throws Exception {
        when(
            relations.getRelationZustaendigkeitTransferObjektXzufiByLbIdAndArs(
                any(),
                any(),
                anyBoolean(),
                any(),
                anyInt(),
                anyInt()
            )
        ).thenThrow(new ElasticSearchException(ErrorCodeEnum.ELSE_IO_ERROR, "internal-es-details"));

        // Der MVC-Slice startet den RequestParameterFilter nicht, der sonst die Request-ID setzt.
        try (var _ = MdcTemplate.scoped(MdcKeys.REQUEST_ID, "jzufi-error-request")) {
            var response = mvc
                .perform(
                    get("/v2/relations/jzufi-2-3")
                        .param("ars", "01")
                        .param("lbids", "lb")
                        .param("size", "5")
                )
                .andExpect(status().isInternalServerError())
                .andExpect(header().doesNotExist("X-PVOG-DEPRECATED"))
                .andReturn()
                .getResponse();
            assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
            var error = mapper.readTree(response.getContentAsString());
            assertEquals(ErrorCodeEnum.UNKNOWN_ERROR.name(), error.path("code").asString());
            assertFalse(error.path("timestamp").asString().isBlank());
            assertEquals("jzufi-error-request", error.path("requestId").asString());
            assertFalse(error.path("message").asString().isBlank());
            assertFalse(response.getContentAsString().contains("internal-es-details"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "servicedescriptions", "organisationunits", "onlineservices" })
    void noNew22VersionIsPublished(String resource) throws Exception {
        mvc.perform(get("/v2/" + resource + "/id/jzufi-2-2")).andExpect(status().isNotFound());
    }

    @Test
    void noNew22RelationVersionIsPublished() throws Exception {
        mvc.perform(get("/v2/relations/jzufi-2-2?ars=01&lbids=id")).andExpect(
            status().isNotFound()
        );
    }
}
