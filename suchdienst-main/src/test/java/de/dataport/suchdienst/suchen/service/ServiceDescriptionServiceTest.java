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
package de.dataport.suchdienst.suchen.service;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import de.dataport.suchdienst.common.model.LeikaKey;
import de.dataport.suchdienst.config.ResourceBundleConfig;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.elasticsearch.api.ElasticPublicSearchConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.model.mapping.LinkDynamic;
import de.dataport.suchdienst.indexer.model.mapping.ResponsibilityCriterion;
import de.dataport.suchdienst.indexer.model.mapping.ResponsibilityRole;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetail;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetailContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.Verwaltungsleistung;
import de.dataport.suchdienst.indexer.service.LeikaService;
import de.dataport.suchdienst.suchen.dynamicparams.DynamicParamsFetcherService;
import de.dataport.suchdienst.suchen.model.out.leistung.LeikaMatchType;
import de.dataport.suchdienst.suchen.model.out.leistung.LeikaMeta;
import de.dataport.suchdienst.suchen.model.out.leistung.SdOutModelV5;
import de.dataport.suchdienst.suchen.model.out.leistung.SdOutModelV7;
import de.dataport.suchdienst.suchen.model.out.leistung.SeDeBlockModule;
import de.dataport.suchdienst.suchen.model.out.leistung.SeDeBlockModuleDetail;
import de.dataport.suchdienst.suchen.model.out.leistung.XZufiCategory;
import de.dataport.suchdienst.suchen.model.out.leistung.detail.ServiceDescriptionDetail;
import de.dataport.suchdienst.suchen.model.out.leistung.resultlist.ServiceDescriptionHierarchySearchResult;
import de.dataport.suchdienst.suchen.model.out.organisation.OeInfoOutModel;
import de.dataport.suchdienst.util.ResultPage;
import de.dataport.suchdienst.util.ResultPageWithMeta;
import de.dataport.suchdienst.util.ServiceModuleCode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
    classes = {
        JacksonAutoConfiguration.class,
        ElasticPublicSearchConnector.class,
        ElasticReadConnector.class,
        ServiceDescriptionService.class,
        OrganisationModelConverterService.class,
        ResourceBundleConfig.class,
        VerwaltungsleistungMappingService.class,
    }
)
@ContextConfiguration(classes = { SuchdienstProperties.class })
class ServiceDescriptionServiceTest {

    public static final String SH = "010020000000";

    @MockitoBean
    private ElasticPublicSearchConnector publicSearchConnector;

    @MockitoBean
    private ElasticReadConnector elasticReadConnector;

    @Autowired
    private MessageSource messageSource;

    @MockitoBean
    private LeikaService leikaService;

    @MockitoBean
    private DynamicParamsFetcherService dynamicParamsFetcherService;

    private final String ars = "020000000000";

    @Autowired
    ServiceDescriptionService serviceDescriptionService;

    @Autowired
    VerwaltungsleistungMappingService vlToOutModelService;

    /**
     * Testet eine Verwaltungsleistung mit Zugangskanälen.
     */
    @Test
    void testServiceDescriptionV5WithAccessChannels() {
        Verwaltungsleistung vl = mockVerwaltungsleistungMitZugangskanaele();

        Mockito.when(
            publicSearchConnector.searchLbByIdAndArs(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyBoolean(),
                Mockito.anyBoolean(),
                Mockito.any(LocalDate.class)
            )
        ).thenReturn(Optional.of(vl));

        Optional<SdOutModelV5> details = serviceDescriptionService.fetchDetailV5(
            "L100012.LB.257406749",
            SH,
            true,
            Locale.GERMAN,
            true,
            LocalDate.now()
        );

        // Kostenmodul mit Kosten Element
        Optional<SeDeBlockModule> serviceBlockModuleDetailContents = details
            .get()
            .getDetails()
            .stream()
            .filter(SeDeBlockModuleDetail.class::isInstance)
            .map(SeDeBlockModuleDetail.class::cast)
            .filter(d -> XZufiCategory.KOSTEN.equals(d.getCategory()))
            .flatMap(m -> m.getDetails().stream())
            .findAny();

        assertTrue(
            serviceBlockModuleDetailContents.isPresent(),
            "Kosten Details should be present"
        );
        assertThat(
            serviceBlockModuleDetailContents.get(),
            hasProperty("title", equalTo("Kosten Details"))
        );

        List<SeDeBlockModuleDetail> seDeBlockDetailFristen = details
            .get()
            .getDetails()
            .stream()
            .filter(SeDeBlockModuleDetail.class::isInstance)
            .map(SeDeBlockModuleDetail.class::cast)
            .filter(d -> d.getCategory().equals(XZufiCategory.FRIST))
            .toList();

        assertThat(
            "Fristen mit dem gleichen Kanal nicht aggregiert",
            seDeBlockDetailFristen.size(),
            equalTo(1)
        );
        assertThat(
            seDeBlockDetailFristen.getFirst().getDetails(),
            not(hasProperty("accessChannel"))
        );
        Mockito.verify(publicSearchConnector).searchLbByIdAndArs(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyBoolean(),
            Mockito.anyBoolean(),
            Mockito.any(LocalDate.class)
        );
        Mockito.verifyNoInteractions(elasticReadConnector);
    }

    /**
     * Testet eine Verwaltungsleistung mit Zugangskanälen.
     *
     */
    @Test
    void testServiceDescriptionDetailWithOrganisation() {
        Verwaltungsleistung vl = mockVerwaltungsleistungMitZugangskanaele();
        vl.setOrganisationseinheiten(List.of(mockOrganisationseinheitVL()));

        Mockito.when(
            publicSearchConnector.searchLbByIdAndArs(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyBoolean(),
                Mockito.anyBoolean(),
                Mockito.any(LocalDate.class)
            )
        ).thenReturn(Optional.of(vl));

        Optional<ServiceDescriptionDetail> details = serviceDescriptionService.fetchDetailV6(
            "L100012.LB.257406749",
            SH,
            true,
            Locale.GERMAN
        );

        List<OeInfoOutModel> oes = List.copyOf(details.get().getOrganisations());

        assertThat(oes.size(), equalTo(1));

        OeInfoOutModel oe = oes.iterator().next();

        assertThat(oe.id(), equalTo("oe1"));

        assertThat(oe.title(), equalTo("Organisationseinheit 1"));

        // Teste, ob die Rolle korrekt gesetzt ist
        assertThat(oe.role(), notNullValue());

        assertThat(oe.role().code(), equalTo("01"));

        assertThat(oe.role().name(), equalTo("Zuständige Stelle und Ansprechpunkt"));

        // Kriterium Testen

        assertThat(oe.criteria().size(), equalTo(1));

        assertThat(oe.criteria().getFirst().code(), equalTo("01"));

        assertThat(oe.criteria().getFirst().name(), equalTo("Nachname"));

        assertThat(oe.criteria().getFirst().auspraegungen().size(), equalTo(3));
        Mockito.verify(publicSearchConnector).searchLbByIdAndArs(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyBoolean(),
            Mockito.anyBoolean(),
            Mockito.any(LocalDate.class)
        );
        Mockito.verifyNoInteractions(elasticReadConnector);
    }

    @Test
    void liefertNichtOeffentlichenOnlineServiceLinkBeiDetailV6Aus() {
        Verwaltungsleistung vl = mockVerwaltungsleistungMitZugangskanaele();
        vl.setOnlinedienste(List.of(mockOnlinedienstVL("od1", false)));

        Mockito.when(
            publicSearchConnector.searchLbByIdAndArs(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyBoolean(),
                Mockito.anyBoolean(),
                Mockito.any(LocalDate.class)
            )
        ).thenReturn(Optional.of(vl));

        Optional<ServiceDescriptionDetail> detail = serviceDescriptionService.fetchDetailV6(
            "L100012.LB.257406749",
            SH,
            true,
            Locale.GERMAN
        );
        assertEquals(1, detail.get().getOnlineServiceLinks().size());
        assertFalse(detail.get().getOnlineServiceLinks().getFirst().getPublicDisplayAllowed());
    }

    private Verwaltungsleistung mockVerwaltungsleistungMitZugangskanaele() {
        var vl = new Verwaltungsleistung();
        vl.setId("1");

        var lb = new LeistungsbeschreibungVL();
        lb.setId("L100012.LB.257406749");

        var accessChannelOnline = new CodelistEntry();
        accessChannelOnline.setId("12345");
        accessChannelOnline.setCode("100");
        accessChannelOnline.setUri("http://example.com/codelist/12345");

        var accessChannelOffline = new CodelistEntry();
        accessChannelOffline.setId("12345");
        accessChannelOffline.setCode("200");
        accessChannelOffline.setUri("http://example.com/codelist/12345");

        var accessChannelOnlineOffline = new CodelistEntry();
        accessChannelOnlineOffline.setId("12345");
        accessChannelOnlineOffline.setCode("000");
        accessChannelOnlineOffline.setUri("http://example.com/codelist/12345");

        vl.setLeistungsbeschreibung(lb);
        lb.setDetails(
            new HashMap<>() {
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
                            List.of(
                                ServiceModule.builder()
                                    .title("Volltext")
                                    .text("Volltext")
                                    .accessChannel(
                                        de.dataport.suchdienst.indexer.model.AccessChannel.ONLINE_OFFLINE
                                    )
                                    .build()
                            )
                        )
                    );
                    put(
                        ServiceModuleCode.TEASER.getCode(),
                        new ArrayList<>(
                            List.of(
                                ServiceModule.builder()
                                    .title("Teaser")
                                    .text("Teaser")
                                    .accessChannel(
                                        de.dataport.suchdienst.indexer.model.AccessChannel.OFFLINE
                                    )
                                    .build(),
                                ServiceModule.builder()
                                    .title("Teaser")
                                    .text("Teaser")
                                    .accessChannel(
                                        de.dataport.suchdienst.indexer.model.AccessChannel.OFFLINE
                                    )
                                    .build(),
                                ServiceModule.builder()
                                    .title("Teaser")
                                    .text("Teaser")
                                    .accessChannel(
                                        de.dataport.suchdienst.indexer.model.AccessChannel.ONLINE
                                    )
                                    .build()
                            )
                        )
                    );
                    put(
                        ServiceModuleCode.KOSTEN.getCode(),
                        new ArrayList<>(
                            List.of(
                                ServiceModuleDetail.builder()
                                    .title("Kosten")
                                    .text("Kosten")
                                    .accessChannel(
                                        de.dataport.suchdienst.indexer.model.AccessChannel.ONLINE_OFFLINE
                                    )
                                    .details(
                                        new ArrayList<>() {
                                            {
                                                add(
                                                    ServiceModuleDetailContent.builder()
                                                        .title("Kosten Details")
                                                        .text("Details zu Kosten")
                                                        .build()
                                                );
                                            }
                                        }
                                    )
                                    .build()
                            )
                        )
                    );
                    put(
                        ServiceModuleCode.FRIST.getCode(),
                        new ArrayList<>(
                            List.of(
                                ServiceModuleDetail.builder()
                                    .title("Fristen")
                                    .text("Fristen")
                                    .accessChannel(
                                        de.dataport.suchdienst.indexer.model.AccessChannel.ONLINE
                                    )
                                    .details(
                                        new ArrayList<>() {
                                            {
                                                add(
                                                    ServiceModuleDetailContent.builder()
                                                        .title("Frist Details")
                                                        .text("Details zu Frist")
                                                        .build()
                                                );
                                            }
                                        }
                                    )
                                    .build(),
                                ServiceModuleDetail.builder()
                                    .title("Fristen 2")
                                    .text("Fristen 2")
                                    .accessChannel(
                                        de.dataport.suchdienst.indexer.model.AccessChannel.ONLINE
                                    )
                                    .details(
                                        new ArrayList<>() {
                                            {
                                                add(
                                                    ServiceModuleDetailContent.builder()
                                                        .title("Frist Details 2")
                                                        .text("Details zu Frist 2")
                                                        .build()
                                                );
                                            }
                                        }
                                    )
                                    .build()
                            )
                        )
                    );
                }
            }
        );

        return vl;
    }

    private OrganisationseinheitVL mockOrganisationseinheitVL() {
        OrganisationseinheitVL oe = new OrganisationseinheitVL();
        oe.setId("oe1");
        oe.setName("Organisationseinheit 1");
        oe.setOeffentlichAnzeigen(true);

        var respRole02 = new ResponsibilityRole();
        respRole02.setName("Zuständige Stelle");
        respRole02.setCode("02");
        var respRole03 = new ResponsibilityRole();
        respRole03.setName("Zuständige Stelle");
        respRole03.setCode("03");

        oe.setRelations(Map.of(SH, List.of(respRole02, respRole03)));

        ResponsibilityCriterion responsibilityCriterion = new ResponsibilityCriterion();
        responsibilityCriterion.setName("Nachname");
        responsibilityCriterion.setCode("01");
        responsibilityCriterion.setAuspraegungen(List.of("A", "B", "C"));

        oe.setCriteria(Map.of(SH, List.of(responsibilityCriterion)));

        return oe;
    }

    /**
     * Direkter Treffer mit speziellem Leikakey
     */
    @Test
    void testSearchLeikaHierarchyCompleteMatch() {
        String leika = "99100923200300";
        List<String> leikaIds = new ArrayList<>();
        leikaIds.add(leika);
        ServiceDescriptionService service = new ServiceDescriptionService(
            publicSearchConnector,
            elasticReadConnector,
            messageSource,
            leikaService,
            vlToOutModelService
        );

        LeistungsbeschreibungVL lb = new LeistungsbeschreibungVL();
        Verwaltungsleistung sd = new Verwaltungsleistung();
        sd.setLeistungsbeschreibung(lb);
        sd.setId("1");

        HitsMetadata<Verwaltungsleistung> hits = createMetadata(sd);

        Mockito.when(
            publicSearchConnector.searchServiceDescriptionByLeikaHierarchy(
                Mockito.eq(Collections.singleton(new LeikaKey(leika))),
                Mockito.anySet(),
                Mockito.anyString(),
                Mockito.anyInt(),
                Mockito.anyInt(),
                Mockito.any(LocalDate.class)
            )
        ).thenReturn(hits);

        ResultPageWithMeta<SdOutModelV7, LeikaMeta> result = service.searchByLeikaHierarchy(
            new HashSet<>(),
            ars,
            0,
            20,
            LocalDate.now(),
            Locale.GERMAN,
            leikaIds
        );

        assertTrue(result.hasContent());
        assertFalse(result.isEmpty());
        assertTrue(result.getMeta().exact());
    }

    /**
     * Treffer mit speziellem Leikakey weiter oben in der Hierarchie
     */
    @Test
    void testSearchLeikaHierarchySpecialLeika() {
        String leika = "99100923200300";
        List<String> leikaIds = new ArrayList<>();
        leikaIds.add(leika);
        ServiceDescriptionService service = new ServiceDescriptionService(
            publicSearchConnector,
            elasticReadConnector,
            messageSource,
            leikaService,
            vlToOutModelService
        );

        LeistungsbeschreibungVL lb = new LeistungsbeschreibungVL();
        Verwaltungsleistung sd = new Verwaltungsleistung();
        sd.setLeistungsbeschreibung(lb);
        sd.setId("1");

        HitsMetadata<Verwaltungsleistung> hits = createMetadata(sd);

        Mockito.when(
            publicSearchConnector.searchServiceDescriptionByLeikaHierarchy(
                Mockito.eq(Collections.singleton(new LeikaKey(leika))),
                Mockito.anySet(),
                Mockito.anyString(),
                Mockito.anyInt(),
                Mockito.anyInt(),
                Mockito.any(LocalDate.class)
            )
        ).thenReturn(emptyHitsMetadata());

        Mockito.when(
            publicSearchConnector.searchServiceDescriptionByLeikaHierarchy(
                Mockito.eq(Collections.singleton(new LeikaKey(leika).getParentKey().get())),
                Mockito.anySet(),
                Mockito.anyString(),
                Mockito.anyInt(),
                Mockito.anyInt(),
                Mockito.any(LocalDate.class)
            )
        ).thenReturn(hits);

        ResultPageWithMeta<SdOutModelV7, LeikaMeta> result = service.searchByLeikaHierarchy(
            new HashSet<>(),
            ars,
            0,
            20,
            LocalDate.now(),
            Locale.GERMAN,
            leikaIds
        );

        assertFalse(result.isEmpty());
        assertFalse(result.getMeta().exact());
    }

    /**
     * Treffer weiter unten in der Hierarchie
     */
    @Test
    void testLeikaHierarchyGeneration() {
        String leika = "99100823200000";
        List<String> leikaIds = new ArrayList<>();
        leikaIds.add(leika);
        ServiceDescriptionService service = new ServiceDescriptionService(
            publicSearchConnector,
            elasticReadConnector,
            messageSource,
            leikaService,
            vlToOutModelService
        );

        LeistungsbeschreibungVL lb = new LeistungsbeschreibungVL();
        Verwaltungsleistung sd = new Verwaltungsleistung();
        sd.setLeistungsbeschreibung(lb);
        sd.setId("1");

        HitsMetadata<Verwaltungsleistung> hits = createMetadata(sd);

        Mockito.when(
            publicSearchConnector.searchServiceDescriptionByLeikaHierarchy(
                Mockito.eq(Collections.singleton(new LeikaKey(leika))),
                Mockito.anySet(),
                Mockito.anyString(),
                Mockito.anyInt(),
                Mockito.anyInt(),
                Mockito.any(LocalDate.class)
            )
        ).thenReturn(emptyHitsMetadata());

        Mockito.when(
            publicSearchConnector.searchServiceDescriptionByLeikaHierarchy(
                Mockito.eq(Collections.singleton(new LeikaKey(leika).getPathPrefix().get())),
                Mockito.anySet(),
                Mockito.anyString(),
                Mockito.anyInt(),
                Mockito.anyInt(),
                Mockito.any(LocalDate.class)
            )
        ).thenReturn(hits);

        ResultPageWithMeta<SdOutModelV7, LeikaMeta> result = service.searchByLeikaHierarchy(
            new HashSet<>(),
            ars,
            0,
            20,
            LocalDate.now(),
            Locale.GERMAN,
            leikaIds
        );

        assertTrue(result.hasContent());
        assertFalse(result.isEmpty());
        assertFalse(result.getMeta().exact());
    }

    /**
     * Beta-Hierarchiesuche: Treffer direkt zu den angefragten LeiKa-Schlüsseln.
     */
    @Test
    void testBetaLeikaHierarchyMarksExactMatch() {
        String leika = "99100923200300";
        ServiceDescriptionService service = newService();

        Mockito.when(
            publicSearchConnector.searchServiceDescriptionByLeikaHierarchy(
                Mockito.eq(Collections.singleton(new LeikaKey(leika))),
                Mockito.anySet(),
                Mockito.anyString(),
                Mockito.anyInt(),
                Mockito.anyInt(),
                Mockito.any(LocalDate.class)
            )
        ).thenReturn(createMetadata(mockLeistungForBetaSearch()));

        ServiceDescriptionHierarchySearchResult result = callBetaLeikaHierarchy(service, leika);

        assertFalse(result.serviceDescriptions().getContent().isEmpty());
        assertEquals(LeikaMatchType.EXACT, result.leikaMatch().matchType());
        assertEquals(Set.of(leika), result.leikaMatch().effectiveLeikaIds());
    }

    /**
     * Beta-Hierarchiesuche: keine exakten Treffer, dafür detailliertere Leistungen eine Ebene tiefer.
     * Die effektiv gesuchten Schlüssel müssen trotz der verkürzten Präfix-Suche wieder als
     * vollständige 14-stellige Schlüssel ausgegeben werden.
     */
    @Test
    void testBetaLeikaHierarchyMarksDescendantMatch() {
        String leika = "99100823200000";
        ServiceDescriptionService service = newService();

        Mockito.when(
            publicSearchConnector.searchServiceDescriptionByLeikaHierarchy(
                Mockito.eq(Collections.singleton(new LeikaKey(leika))),
                Mockito.anySet(),
                Mockito.anyString(),
                Mockito.anyInt(),
                Mockito.anyInt(),
                Mockito.any(LocalDate.class)
            )
        ).thenReturn(emptyHitsMetadata());

        Mockito.when(
            publicSearchConnector.searchServiceDescriptionByLeikaHierarchy(
                Mockito.eq(Collections.singleton(new LeikaKey(leika).getPathPrefix().get())),
                Mockito.anySet(),
                Mockito.anyString(),
                Mockito.anyInt(),
                Mockito.anyInt(),
                Mockito.any(LocalDate.class)
            )
        ).thenReturn(createMetadata(mockLeistungForBetaSearch()));

        ServiceDescriptionHierarchySearchResult result = callBetaLeikaHierarchy(service, leika);

        assertFalse(result.serviceDescriptions().getContent().isEmpty());
        assertEquals(LeikaMatchType.DESCENDANT, result.leikaMatch().matchType());
        assertEquals(Set.of(leika), result.leikaMatch().effectiveLeikaIds());
    }

    /**
     * Beta-Hierarchiesuche: Treffer erst auf einer übergeordneten Ebene. Ausgegeben werden die
     * tatsächlich gesuchten, übergeordneten Schlüssel.
     */
    @Test
    void testBetaLeikaHierarchyMarksAncestorMatch() {
        String leika = "99100923200300";
        LeikaKey parentKey = new LeikaKey(leika).getParentKey().get();
        ServiceDescriptionService service = newService();

        Mockito.when(
            publicSearchConnector.searchServiceDescriptionByLeikaHierarchy(
                Mockito.eq(Collections.singleton(new LeikaKey(leika))),
                Mockito.anySet(),
                Mockito.anyString(),
                Mockito.anyInt(),
                Mockito.anyInt(),
                Mockito.any(LocalDate.class)
            )
        ).thenReturn(emptyHitsMetadata());

        Mockito.when(
            publicSearchConnector.searchServiceDescriptionByLeikaHierarchy(
                Mockito.eq(Collections.singleton(parentKey)),
                Mockito.anySet(),
                Mockito.anyString(),
                Mockito.anyInt(),
                Mockito.anyInt(),
                Mockito.any(LocalDate.class)
            )
        ).thenReturn(createMetadata(mockLeistungForBetaSearch()));

        ServiceDescriptionHierarchySearchResult result = callBetaLeikaHierarchy(service, leika);

        assertFalse(result.serviceDescriptions().getContent().isEmpty());
        assertEquals(LeikaMatchType.ANCESTOR, result.leikaMatch().matchType());
        assertEquals(Set.of(parentKey.getFullKey()), result.leikaMatch().effectiveLeikaIds());
    }

    /**
     * Beta-Hierarchiesuche: An keiner Stelle der Hierarchie gibt es Treffer. Es darf weder ein
     * exakter noch ein Fallback-Treffer signalisiert werden.
     */
    @Test
    void testBetaLeikaHierarchyMarksEmptyResultAsNone() {
        ServiceDescriptionService service = newService();

        Mockito.when(
            publicSearchConnector.searchServiceDescriptionByLeikaHierarchy(
                Mockito.anySet(),
                Mockito.anySet(),
                Mockito.anyString(),
                Mockito.anyInt(),
                Mockito.anyInt(),
                Mockito.any(LocalDate.class)
            )
        ).thenReturn(emptyHitsMetadata());

        ServiceDescriptionHierarchySearchResult result = callBetaLeikaHierarchy(
            service,
            "99100923200300"
        );

        assertTrue(result.serviceDescriptions().getContent().isEmpty());
        assertEquals(LeikaMatchType.NONE, result.leikaMatch().matchType());
        assertTrue(result.leikaMatch().effectiveLeikaIds().isEmpty());
    }

    /**
     * Beta-Hierarchiesuche: Wird eine Seite hinter dem Ende der Treffermenge angefragt, ist die Seite
     * zwar leer, es gibt aber sehr wohl Treffer. Die Kaskade darf dann nicht weiterlaufen, damit
     * matchType und Gesamttrefferzahl über alle Seiten hinweg stabil bleiben.
     */
    @Test
    void testBetaLeikaHierarchyKeepsMatchTypeOnPageBeyondResults() {
        String leika = "99100923200300";
        ServiceDescriptionService service = newService();

        // Die exakte Suche liefert einen Gesamttreffer, auf der angefragten Seite 5 ist er aber nicht.
        Mockito.when(
            publicSearchConnector.searchServiceDescriptionByLeikaHierarchy(
                Mockito.eq(Collections.singleton(new LeikaKey(leika))),
                Mockito.anySet(),
                Mockito.anyString(),
                Mockito.anyInt(),
                Mockito.anyInt(),
                Mockito.any(LocalDate.class)
            )
        ).thenReturn(hitsMetadataWithoutContent(1));

        // Jede weitere Stufe der Kaskade wuerde Treffer liefern und muss daher ungenutzt bleiben.
        Mockito.when(
            publicSearchConnector.searchServiceDescriptionByLeikaHierarchy(
                Mockito.eq(Collections.singleton(new LeikaKey(leika).getParentKey().get())),
                Mockito.anySet(),
                Mockito.anyString(),
                Mockito.anyInt(),
                Mockito.anyInt(),
                Mockito.any(LocalDate.class)
            )
        ).thenReturn(createMetadata(mockLeistungForBetaSearch()));

        ServiceDescriptionHierarchySearchResult result = service.searchHitsByLeikaHierarchy(
            ars,
            5,
            20,
            Locale.GERMAN,
            List.of(leika)
        );

        assertTrue(result.serviceDescriptions().getContent().isEmpty());
        assertEquals(1, result.serviceDescriptions().getMetadata().totalElements());
        assertEquals(LeikaMatchType.EXACT, result.leikaMatch().matchType());
        assertEquals(Set.of(leika), result.leikaMatch().effectiveLeikaIds());
    }

    @Test
    void testPublicLeikaHierarchyKeepsExactOnPageBeyondResults() {
        assertPublicHierarchyPageBeyondResults(true);
    }

    @Test
    void testPublicLeikaHierarchyKeepsDescendantsOnPageBeyondResults() {
        assertPublicHierarchyPageBeyondResults(false);
    }

    private void assertPublicHierarchyPageBeyondResults(boolean exact) {
        var key = new LeikaKey("99100923200000");
        Mockito.when(
            publicSearchConnector.searchServiceDescriptionByLeikaHierarchy(
                Mockito.anySet(),
                Mockito.anySet(),
                Mockito.anyString(),
                Mockito.anyInt(),
                Mockito.anyInt(),
                Mockito.any(LocalDate.class)
            )
        ).thenReturn(emptyHitsMetadata());
        var matchedKey = exact ? key : key.getPathPrefix().orElseThrow();
        Mockito.when(
            publicSearchConnector.searchServiceDescriptionByLeikaHierarchy(
                Mockito.eq(Set.of(matchedKey)),
                Mockito.anySet(),
                Mockito.anyString(),
                Mockito.anyInt(),
                Mockito.anyInt(),
                Mockito.any(LocalDate.class)
            )
        ).thenReturn(hitsMetadataWithoutContent(1));

        var result = newService().searchByLeikaHierarchy(
            Set.of(),
            ars,
            5,
            20,
            LocalDate.now(),
            Locale.GERMAN,
            List.of("99100923200000")
        );

        assertTrue(result.getContent().isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals(exact, result.getMeta().exact());
        Mockito.verify(
            publicSearchConnector,
            Mockito.times(exact ? 1 : 2)
        ).searchServiceDescriptionByLeikaHierarchy(
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anyString(),
            Mockito.anyInt(),
            Mockito.anyInt(),
            Mockito.any(LocalDate.class)
        );
    }

    private ServiceDescriptionService newService() {
        return new ServiceDescriptionService(
            publicSearchConnector,
            elasticReadConnector,
            messageSource,
            leikaService,
            vlToOutModelService
        );
    }

    private ServiceDescriptionHierarchySearchResult callBetaLeikaHierarchy(
        ServiceDescriptionService service,
        String leika
    ) {
        return service.searchHitsByLeikaHierarchy(ars, 0, 20, Locale.GERMAN, List.of(leika));
    }

    private Verwaltungsleistung mockLeistungForBetaSearch() {
        Verwaltungsleistung sd = new Verwaltungsleistung();
        sd.setLeistungsbeschreibung(new LeistungsbeschreibungVL());
        sd.setId("1");
        return sd;
    }

    /**
     * Antwort ohne Treffer auf der angefragten Seite, aber mit Gesamttreffern. Entspricht einer
     * Anfrage auf eine Seite hinter dem Ende der Treffermenge.
     */
    private <T> HitsMetadata<T> hitsMetadataWithoutContent(long totalHits) {
        return HitsMetadata.of(b ->
            b
                .hits(Collections.emptyList())
                .total(v -> v.value(totalHits).relation(TotalHitsRelation.Eq))
                .maxScore(0.0)
        );
    }

    private <T> HitsMetadata<T> emptyHitsMetadata() {
        return HitsMetadata.of(b ->
            b
                .hits(Collections.emptyList())
                .total(v -> v.value(0L).relation(TotalHitsRelation.Eq))
                .maxScore(0.0)
        );
    }

    @SuppressWarnings("unchecked")
    private HitsMetadata<Verwaltungsleistung> createMetadata(Verwaltungsleistung obj) {
        Hit<Verwaltungsleistung> hit = Hit.of(h -> h.index("index").id(obj.getId()).source(obj));
        return HitsMetadata.of(m ->
            m
                .hits(hit)
                .total(
                    co.elastic.clients.elasticsearch.core.search.TotalHits.of(t ->
                        t.value(1).relation(TotalHitsRelation.Eq)
                    )
                )
        );
    }

    private OnlinedienstVL mockOnlinedienstVL(String id, boolean oeffentlichAnzeigen) {
        OnlinedienstVL od = new OnlinedienstVL();
        od.setId(id);
        od.setName("Onlinedienst " + id);
        od.setOeffentlichAnzeigen(oeffentlichAnzeigen);
        od.setLinks(List.of(LinkDynamic.of("https://" + id + ".example", null, null)));
        return od;
    }

    private Verwaltungsleistung mockMinimalVl(String vlId, String lbId) {
        LeistungsbeschreibungVL lb = new LeistungsbeschreibungVL();
        lb.setId(lbId);
        Verwaltungsleistung vl = new Verwaltungsleistung();
        vl.setId(vlId);
        vl.setLeistungsbeschreibung(lb);
        return vl;
    }

    @Test
    void liefertNichtOeffentlichenOnlineServiceLinkInV7Aus() {
        Verwaltungsleistung vlMitVersstecktemOd = mockMinimalVl("1", "1.LB.1");
        vlMitVersstecktemOd.setOnlinedienste(List.of(mockOnlinedienstVL("od1", false)));

        HitsMetadata<Verwaltungsleistung> hits = createMetadata(vlMitVersstecktemOd);

        ResultPage<SdOutModelV7> result = serviceDescriptionService.createSdOutModelsFromResponse(
            hits,
            ars,
            0,
            10,
            Locale.GERMAN,
            LocalDate.now(),
            false,
            SdOutModelV7.class
        );

        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getContent().getFirst().getOnlineServiceLinks().size());
    }
}
