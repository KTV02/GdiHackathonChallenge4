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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dataport.suchdienst.indexer.model.AccessChannel;
import de.dataport.suchdienst.indexer.model.CodelistKey;
import de.dataport.suchdienst.indexer.model.mapping.LinkDynamic;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.Verwaltungsleistung;
import de.dataport.suchdienst.indexer.service.LeikaService;
import de.dataport.suchdienst.suchen.dynamicparams.DynamicParamsFetcherService;
import de.dataport.suchdienst.suchen.model.out.leistung.resultlist.ServiceDescriptionSearchHit;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.OnlineServiceStatus;
import de.dataport.suchdienst.util.ServiceModuleCode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VerwaltungsleistungMappingServiceV8BetaTest {

    private static final String ARS = "020000000000";

    @Mock
    private LeikaService leikaService;

    @Mock
    private OrganisationModelConverterService oeModelConverterService;

    @Mock
    private DynamicParamsFetcherService dynamicParamsFetcherService;

    private VerwaltungsleistungMappingService service;

    @BeforeEach
    void setUp() {
        service = new VerwaltungsleistungMappingService(
            leikaService,
            oeModelConverterService,
            dynamicParamsFetcherService
        );
    }

    private static ServiceModule module(String text, AccessChannel channel) {
        return ServiceModule.builder().text(text).accessChannel(channel).build();
    }

    private static Verwaltungsleistung verwaltungsleistung(
        LeistungsbeschreibungVL lb,
        Map<String, List<AbstractServiceModule>> details
    ) {
        lb.setDetails(details);
        var vl = new Verwaltungsleistung();
        vl.setLeistungsbeschreibung(lb);
        return vl;
    }

    @Test
    void mapsBaseFieldsFromServiceDescription() {
        LeistungsbeschreibungVL lb = new LeistungsbeschreibungVL();
        lb.setId("L100012.LB.1");
        lb.setArsFederalstate(Set.of("02"));
        lb.setAvailableLanguages(Set.of(Locale.GERMAN, Locale.ENGLISH));

        Verwaltungsleistung vl = verwaltungsleistung(lb, new HashMap<>());

        ServiceDescriptionSearchHit result = service.mapToV8Beta1(
            vl,
            ARS,
            Locale.GERMAN,
            LocalDate.now()
        );

        assertEquals("L100012.LB.1", result.getId());
        assertEquals(Set.of("02"), result.getArsFederalstate());
        assertEquals(Set.of("de", "en"), result.getAvailableLanguages());
    }

    @Test
    void namePrefersOnlineAccessChannel() {
        Map<String, List<AbstractServiceModule>> details = new HashMap<>();
        details.put(
            ServiceModuleCode.LEISTUNGSNAME.getCode(),
            new ArrayList<>(
                List.of(
                    module("Name Offline", AccessChannel.OFFLINE),
                    module("Name Online/Offline", AccessChannel.ONLINE_OFFLINE),
                    module("Name Online", AccessChannel.ONLINE)
                )
            )
        );

        Verwaltungsleistung vl = verwaltungsleistung(new LeistungsbeschreibungVL(), details);

        ServiceDescriptionSearchHit result = service.mapToV8Beta1(
            vl,
            ARS,
            Locale.GERMAN,
            LocalDate.now()
        );

        assertEquals("Name Online", result.getName());
    }

    @Test
    void namePrefersOnlineOfflineOverOfflineWhenNoOnline() {
        Map<String, List<AbstractServiceModule>> details = new HashMap<>();
        details.put(
            ServiceModuleCode.LEISTUNGSNAME.getCode(),
            new ArrayList<>(
                List.of(
                    module("Name Offline", AccessChannel.OFFLINE),
                    module("Name Online/Offline", AccessChannel.ONLINE_OFFLINE)
                )
            )
        );

        Verwaltungsleistung vl = verwaltungsleistung(new LeistungsbeschreibungVL(), details);

        ServiceDescriptionSearchHit result = service.mapToV8Beta1(
            vl,
            ARS,
            Locale.GERMAN,
            LocalDate.now()
        );

        assertEquals("Name Online/Offline", result.getName());
    }

    @Test
    void teaserExtractedFromTeaserModule() {
        Map<String, List<AbstractServiceModule>> details = new HashMap<>();
        details.put(
            ServiceModuleCode.TEASER.getCode(),
            new ArrayList<>(
                List.of(module("Kurze Vorschau auf die Leistung", AccessChannel.ONLINE))
            )
        );

        Verwaltungsleistung vl = verwaltungsleistung(new LeistungsbeschreibungVL(), details);

        ServiceDescriptionSearchHit result = service.mapToV8Beta1(
            vl,
            ARS,
            Locale.GERMAN,
            LocalDate.now()
        );

        assertEquals("Kurze Vorschau auf die Leistung", result.getTeaser());
    }

    @Test
    void fallsBackWhenNameAndTeaserAreMissing() {
        Verwaltungsleistung vl = verwaltungsleistung(
            new LeistungsbeschreibungVL(),
            new HashMap<>()
        );

        ServiceDescriptionSearchHit result = service.mapToV8Beta1(
            vl,
            ARS,
            Locale.GERMAN,
            LocalDate.now()
        );

        assertEquals("Kein Name verfügbar", result.getName());
        assertTrue(result.getTeaser().isEmpty());
    }

    @Test
    void mapsOnlinedienstStatusToSearchHit() {
        Verwaltungsleistung vl = verwaltungsleistung(
            new LeistungsbeschreibungVL(),
            new HashMap<>()
        );
        OnlinedienstVL onlinedienst = new OnlinedienstVL();
        onlinedienst.setId("od-1");
        onlinedienst.setLinks(List.of(LinkDynamic.of("https://service.example", null, null)));
        onlinedienst.setOnlinedienstStatus(
            new CodelistKey(
                de.dataport.suchdienst.indexer.model.OnlinedienstStatus.CODELIST_URI,
                "1",
                "40"
            )
        );
        vl.setOnlinedienste(List.of(onlinedienst));

        ServiceDescriptionSearchHit result = service.mapToV8Beta1(
            vl,
            ARS,
            Locale.GERMAN,
            LocalDate.now()
        );

        assertEquals(
            OnlineServiceStatus.PRODUCTION,
            result.getOnlineServiceLinks().getFirst().getStatus()
        );
    }
}
