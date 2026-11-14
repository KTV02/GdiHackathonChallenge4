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
package de.dataport.suchdienst.indexer.service.languagecheck;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import de.dataport.suchdienst.indexer.model.mapping.Link;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetail;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetailContent;
import de.dataport.suchdienst.util.ServiceModuleCode;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LanguageCompleteServiceDescriptionTest {

    private LanguageLbCompleteCheckService completeCheckService;

    @BeforeEach
    void setUp() {
        completeCheckService = new LanguageLbCompleteCheckService();
    }

    @Test
    void testMainModulesComplete() {
        LeistungsbeschreibungVL sd = new LeistungsbeschreibungVL();
        Map<Locale, LeistungsbeschreibungContent> otherLang = new HashMap<>();
        var otherLangContent = new LeistungsbeschreibungContent();
        otherLang.put(Locale.ENGLISH, otherLangContent);

        ServiceModule nameModule = ServiceModule.builder().text("name").title("name").build();
        sd.getDetails().put(ServiceModuleCode.LEISTUNGSBEZEICHNUNG.getCode(), List.of(nameModule));

        assertFalse(completeCheckService.checkLanguageComplete(otherLang, sd, Locale.ENGLISH));

        ServiceModule enModuleName = ServiceModule.builder()
            .text("englishName")
            .title("englishName")
            .build();
        Map<String, List<AbstractServiceModule>> enDetails = new HashMap<>();
        enDetails.put(ServiceModuleCode.LEISTUNGSBEZEICHNUNG.getCode(), List.of(enModuleName));
        otherLangContent.setDetails(enDetails);

        assertTrue(completeCheckService.checkLanguageComplete(otherLang, sd, Locale.ENGLISH));

        ServiceModule enModuleDesc = ServiceModule.builder()
            .text("englishDescription")
            .title("englishDescription")
            .build();
        enDetails.put(ServiceModuleCode.VOLLTEXT.getCode(), List.of(enModuleDesc));

        assertTrue(completeCheckService.checkLanguageComplete(otherLang, sd, Locale.ENGLISH));

        ServiceModule deModuleDesc = ServiceModule.builder()
            .text("deutschDescription")
            .title("deutschDescription")
            .build();
        sd.getDetails().put(ServiceModuleCode.VOLLTEXT.getCode(), List.of(deModuleDesc));
        // Remove description from english
        enDetails.put(ServiceModuleCode.VOLLTEXT.getCode(), List.of());

        assertFalse(completeCheckService.checkLanguageComplete(otherLang, sd, Locale.ENGLISH));

        // Add description back
        enDetails.put(ServiceModuleCode.VOLLTEXT.getCode(), List.of(enModuleDesc));

        // Set nameOtherDescription module in detailsNg
        ServiceModule deModuleNameOther = ServiceModule.builder()
            .text("nameOther")
            .title("nameOther")
            .build();
        sd.getDetails().put(ServiceModuleCode.LEISTUNGSNAME.getCode(), List.of(deModuleNameOther));

        ServiceModule nameOtherModule = ServiceModule.builder()
            .text("englishNameOther")
            .title("englishNameOther")
            .build();
        enDetails.put(ServiceModuleCode.LEISTUNGSNAME.getCode(), List.of(nameOtherModule));

        assertTrue(completeCheckService.checkLanguageComplete(otherLang, sd, Locale.ENGLISH));
    }

    @Test
    void testDetailsComplete() {
        LeistungsbeschreibungVL sd = new LeistungsbeschreibungVL();
        Map<Locale, LeistungsbeschreibungContent> otherLang = new HashMap<>();
        LeistungsbeschreibungContent otherLangContent = new LeistungsbeschreibungContent();
        otherLang.put(Locale.ENGLISH, otherLangContent);

        Link germanLink = new Link("uri", "titel", "beschreibung");

        ServiceModule germanModule = ServiceModule.builder()
            .text("text")
            .title("title")
            .links(List.of(germanLink))
            .build();
        sd.getDetails().put(
            ServiceModuleCode.LEISTUNGSBEZEICHNUNG.getCode(),
            List.of(germanModule)
        );

        assertFalse(completeCheckService.checkLanguageComplete(otherLang, sd, Locale.ENGLISH));

        var englishDetailBuilder = ServiceModule.builder()
            .text("englishText")
            .title("englishTitle");

        Map<String, List<AbstractServiceModule>> englishEnumMap = new HashMap<>();
        englishEnumMap.put(
            ServiceModuleCode.LEISTUNGSBEZEICHNUNG.getCode(),
            List.of(englishDetailBuilder.build())
        );

        otherLangContent.setDetails(englishEnumMap);
        assertFalse(completeCheckService.checkLanguageComplete(otherLang, sd, Locale.ENGLISH));

        englishEnumMap.clear();
        Link englishLink = new Link("englishUri", "englishTitle", "englishDescription");
        englishDetailBuilder.links(List.of(englishLink));
        englishEnumMap.put(
            ServiceModuleCode.LEISTUNGSBEZEICHNUNG.getCode(),
            List.of(englishDetailBuilder.build())
        );
        assertTrue(completeCheckService.checkLanguageComplete(otherLang, sd, Locale.ENGLISH));

        englishEnumMap.clear();
        englishDetailBuilder.text(null);
        englishEnumMap.put(
            ServiceModuleCode.LEISTUNGSBEZEICHNUNG.getCode(),
            List.of(englishDetailBuilder.build())
        );
        assertFalse(completeCheckService.checkLanguageComplete(otherLang, sd, Locale.ENGLISH));

        englishEnumMap.clear();
        englishDetailBuilder.text("englishText");
        englishDetailBuilder.title(null);
        englishEnumMap.put(
            ServiceModuleCode.LEISTUNGSBEZEICHNUNG.getCode(),
            List.of(englishDetailBuilder.build())
        );
        assertFalse(completeCheckService.checkLanguageComplete(otherLang, sd, Locale.ENGLISH));

        englishEnumMap.clear();
        englishDetailBuilder.title("englishTitle");
        englishEnumMap.put(
            ServiceModuleCode.LEISTUNGSBEZEICHNUNG.getCode(),
            List.of(englishDetailBuilder.build())
        );
        assertTrue(completeCheckService.checkLanguageComplete(otherLang, sd, Locale.ENGLISH));

        sd.setSuggestions(Sets.newHashSet("germanSuggestion"));
        sd.setSynonyms(Sets.newHashSet("germanSynonym"));
        assertTrue(completeCheckService.checkLanguageComplete(otherLang, sd, Locale.ENGLISH));
    }

    @Test
    void testDetailsOrderComplete() {
        LeistungsbeschreibungVL sd = new LeistungsbeschreibungVL();
        Map<Locale, LeistungsbeschreibungContent> otherLang = new HashMap<>();
        var otherLangContent = new LeistungsbeschreibungContent();
        otherLang.put(Locale.ENGLISH, otherLangContent);

        var germanDetailBuilder1 = ServiceModule.builder().text("text").title("title");

        var englishDetailBuilder1 = ServiceModule.builder()
            .text("englishText")
            .title("englishTitle");

        var englishDetailBuilder2 = ServiceModule.builder().title("englishTitle");

        Map<String, List<AbstractServiceModule>> germanEnumMap = new HashMap<>();
        germanEnumMap.put(
            ServiceModuleCode.LEISTUNGSBEZEICHNUNG.getCode(),
            List.of(germanDetailBuilder1.build())
        );
        germanEnumMap.put(
            ServiceModuleCode.VERFAHRENSABLAUF.getCode(),
            List.of(germanDetailBuilder1.build())
        );

        Map<String, List<AbstractServiceModule>> englishEnumMap = new HashMap<>();
        englishEnumMap.put(
            ServiceModuleCode.LEISTUNGSBEZEICHNUNG.getCode(),
            List.of(englishDetailBuilder2.build())
        );
        englishEnumMap.put(
            ServiceModuleCode.VERFAHRENSABLAUF.getCode(),
            List.of(englishDetailBuilder1.build())
        );

        sd.setDetails(englishEnumMap);
        assertFalse(completeCheckService.checkLanguageComplete(otherLang, sd, Locale.ENGLISH));

        otherLangContent.setDetails(englishEnumMap);

        assertTrue(completeCheckService.checkLanguageComplete(otherLang, sd, Locale.ENGLISH));
    }

    @Test
    void testModuleDetailComplete() {
        LeistungsbeschreibungContent otherLangContent = new LeistungsbeschreibungContent();
        LeistungsbeschreibungVL sd = new LeistungsbeschreibungVL();
        Map<Locale, LeistungsbeschreibungContent> otherLang = new HashMap<>();
        otherLang.put(Locale.ENGLISH, otherLangContent);

        List<Link> linksKosten = Lists.newArrayList(new Link("uri", "titel", "beschreibung"));

        // German detailsNg
        Map<String, List<AbstractServiceModule>> germanDetailsNg = new HashMap<>();
        ServiceModuleDetail germanDetailKosten = ServiceModuleDetail.builder()
            .text("Kosten Text")
            .title("Kosten Titel")
            .links(linksKosten)
            .build();
        germanDetailsNg.put(ServiceModuleCode.KOSTEN.getCode(), List.of(germanDetailKosten));
        sd.setDetails(germanDetailsNg);

        // Kosten only in German
        assertFalse(completeCheckService.checkLanguageComplete(otherLang, sd, Locale.ENGLISH));

        // English detailsNg
        Map<String, List<AbstractServiceModule>> englishDetailsNg = new HashMap<>();
        ServiceModuleDetail englishDetailKosten = ServiceModuleDetail.builder()
            .text("Costs Text")
            .title("Costs title")
            .build();
        englishDetailsNg.put(ServiceModuleCode.KOSTEN.getCode(), List.of(englishDetailKosten));
        otherLangContent.setDetails(englishDetailsNg);

        // Kosten in English without links
        assertFalse(completeCheckService.checkLanguageComplete(otherLang, sd, Locale.ENGLISH));

        // Add links to English Kosten
        englishDetailKosten = ServiceModuleDetail.builder()
            .text("Costs Text")
            .title("Costs title")
            .links(linksKosten)
            .build();
        englishDetailsNg.put(ServiceModuleCode.KOSTEN.getCode(), List.of(englishDetailKosten));
        otherLangContent.setDetails(englishDetailsNg);

        // Kosten in both with links
        assertTrue(completeCheckService.checkLanguageComplete(otherLang, sd, Locale.ENGLISH));

        // Add Bearbeitungsdauer to German
        ServiceModuleDetail germanDetailBearbeitung = ServiceModuleDetail.builder()
            .text("Bearbeitungsdauer Text")
            .title("Bearbeitungsdauer Titel")
            .build();
        germanDetailsNg.put(
            ServiceModuleCode.BEARBEITUNGSDAUER.getCode(),
            List.of(germanDetailBearbeitung)
        );
        sd.setDetails(germanDetailsNg);

        // Add Fristen to English
        ServiceModuleDetail englishDetailFristen = ServiceModuleDetail.builder()
            .text("fristen Text")
            .title("fristen title")
            .build();
        englishDetailsNg.put(ServiceModuleCode.FRIST.getCode(), List.of(englishDetailFristen));
        otherLangContent.setDetails(englishDetailsNg);

        // German has Bearbeitung, English has Fristen
        assertFalse(completeCheckService.checkLanguageComplete(otherLang, sd, Locale.ENGLISH));

        // Add Bearbeitung to English
        ServiceModuleDetail englishDetailBearbeitung = ServiceModuleDetail.builder()
            .text("Bearbeitungsdauer Text")
            .title("Bearbeitungsdauer title")
            .build();
        englishDetailsNg.put(
            ServiceModuleCode.BEARBEITUNGSDAUER.getCode(),
            List.of(englishDetailBearbeitung)
        );
        otherLangContent.setDetails(englishDetailsNg);

        // Both have Bearbeitung
        assertTrue(completeCheckService.checkLanguageComplete(otherLang, sd, Locale.ENGLISH));

        // Add Dokument with details to German
        ServiceModuleDetailContent module = ServiceModuleDetailContent.builder()
            .text("detailmoduletext")
            .title("detailmoduletitel")
            .build();
        ServiceModuleDetail germanDetailDokument = ServiceModuleDetail.builder()
            .text("Dokument Text")
            .title("Dokument Titel")
            .details(Lists.newArrayList(module))
            .build();
        germanDetailsNg.put(ServiceModuleCode.DOKUMENT.getCode(), List.of(germanDetailDokument));
        sd.setDetails(germanDetailsNg);

        // Add Dokument without details to English
        ServiceModuleDetail englishDetailDokument = ServiceModuleDetail.builder()
            .text("Dokument Text")
            .title("Dokument Titel")
            .build();
        englishDetailsNg.put(ServiceModuleCode.DOKUMENT.getCode(), List.of(englishDetailDokument));
        otherLangContent.setDetails(englishDetailsNg);

        // Only German has details in Dokument
        assertFalse(completeCheckService.checkLanguageComplete(otherLang, sd, Locale.ENGLISH));

        // Add details to English Dokument
        englishDetailDokument = ServiceModuleDetail.builder()
            .text("Dokument Text")
            .title("Dokument Titel")
            .details(Lists.newArrayList(module))
            .build();
        englishDetailsNg.put(ServiceModuleCode.DOKUMENT.getCode(), List.of(englishDetailDokument));
        otherLangContent.setDetails(englishDetailsNg);
    }
}
