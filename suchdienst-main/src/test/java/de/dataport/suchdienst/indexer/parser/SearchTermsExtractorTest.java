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
package de.dataport.suchdienst.indexer.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SearchTerms;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceBlockSpecialization;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SpezialisierungContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SpezialisierungVL;
import de.dataport.suchdienst.indexer.util.ModulesStringExtractor;
import de.dataport.suchdienst.util.ServiceModuleCode;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Tests für die Zusammenführung und Extraktion von Suchbegriffen aus verschiedenen
 * Leistungsbestandteilen. Schwerpunkt:
 *  - Merge deutscher + weiterer Sprachen
 *  - Null-/Leer-Behandlung
 *  - HTML-Bereinigung bei Values
 *  - Extraktion einzelner Modul-Felder über {@link ModulesStringExtractor} (statisch gemockt).
 */
class SearchTermsExtractorTest {

    @Test
    void mergeSearchTermsLocalized_shouldMergeGermanContent() {
        // Arrange: LB, Spezialisierung und Onlinedienst liefern jeweils eigene SearchTerms
        LeistungsbeschreibungVL lb = mock(LeistungsbeschreibungVL.class);
        SpezialisierungVL sp = mock(SpezialisierungVL.class);
        OnlinedienstVL od = mock(OnlinedienstVL.class);

        when(lb.getSearchTerms()).thenReturn(searchTerms("lb-name", "lb-desc", "lb-value"));
        when(sp.getSearchTerms()).thenReturn(searchTerms("sp-name", "sp-desc", "sp-value"));
        when(od.getSearchTerms()).thenReturn(searchTerms("od-name", "od-desc", "od-value"));
        when(lb.getOtherLanguages()).thenReturn(Collections.emptyMap());

        // Act
        Map<Locale, SearchTerms> result = SearchTermsExtractor.mergeSearchTermsLocalized(
            lb,
            List.of(sp),
            List.of(od)
        );

        // Assert
        assertThat(result).containsKey(Locale.GERMAN);
        SearchTerms merged = result.get(Locale.GERMAN);
        assertThat(merged.searchNames()).containsExactlyInAnyOrder("lb-name", "sp-name", "od-name");
        assertThat(merged.searchDescriptions()).containsExactlyInAnyOrder(
            "lb-desc",
            "sp-desc",
            "od-desc"
        );
        assertThat(merged.searchValues()).containsExactlyInAnyOrder(
            "lb-value",
            "sp-value",
            "od-value"
        );
    }

    @Test
    void mergeSearchTermsLocalized_shouldHandleNullSearchTerms() {
        // Arrange: LB hat keine Suchbegriffe
        LeistungsbeschreibungVL lb = mock(LeistungsbeschreibungVL.class);
        when(lb.getSearchTerms()).thenReturn(null);
        when(lb.getOtherLanguages()).thenReturn(Collections.emptyMap());

        // Act
        Map<Locale, SearchTerms> result = SearchTermsExtractor.mergeSearchTermsLocalized(
            lb,
            Collections.emptyList(),
            Collections.emptyList()
        );

        // Assert: Leere Sets statt null
        assertThat(result).containsKey(Locale.GERMAN);
        SearchTerms merged = result.get(Locale.GERMAN);
        assertThat(merged.searchNames()).isEmpty();
        assertThat(merged.searchDescriptions()).isEmpty();
        assertThat(merged.searchValues()).isEmpty();
    }

    @Test
    void mergeSearchTermsLocalized_shouldHandleOtherLanguages() {
        // Arrange: Englische Variante vorhanden
        LeistungsbeschreibungVL lb = mock(LeistungsbeschreibungVL.class);
        LeistungsbeschreibungContent enContent = mock(LeistungsbeschreibungContent.class);

        when(lb.getSearchTerms()).thenReturn(searchTerms("de-name", "", ""));
        when(enContent.getSearchTerms()).thenReturn(searchTerms("en-name", "", ""));
        when(lb.getOtherLanguages()).thenReturn(Map.of(Locale.ENGLISH, enContent));

        // Act
        Map<Locale, SearchTerms> result = SearchTermsExtractor.mergeSearchTermsLocalized(
            lb,
            Collections.emptyList(),
            Collections.emptyList()
        );

        // Assert
        assertThat(result).containsKeys(Locale.GERMAN, Locale.ENGLISH);
        assertThat(result.get(Locale.GERMAN).searchNames()).contains("de-name");
        assertThat(result.get(Locale.ENGLISH).searchNames()).contains("en-name");
    }

    @Test
    void extractUrsprungsportalTitel_fromLeistungsbeschreibung_shouldExtractTitles() {
        LeistungsbeschreibungContent lb = mockLbContentWithDetails();

        try (
            MockedStatic<ModulesStringExtractor> mockedExtractor = Mockito.mockStatic(
                ModulesStringExtractor.class
            )
        ) {
            mockStringFieldExtraction(
                mockedExtractor,
                lb.getDetails(),
                ServiceModuleCode.URSPRUNGSPORTAL,
                "Test Title"
            );

            assertThat(SearchTermsExtractor.extractUrsprungsportalTitel(lb)).contains("Test Title");
        }
    }

    @Test
    void extractUrsprungsportalTitel_fromSpezialisierung_shouldExtractTitles() {
        SpezialisierungContent sp = mockSpContentWithDetails();

        try (
            MockedStatic<ModulesStringExtractor> mockedExtractor = Mockito.mockStatic(
                ModulesStringExtractor.class
            )
        ) {
            mockStringFieldSpExtraction(
                mockedExtractor,
                sp.getDetails(),
                ServiceModuleCode.URSPRUNGSPORTAL,
                "Sp Title"
            );

            assertThat(SearchTermsExtractor.extractUrsprungsportalTitel(sp)).contains("Sp Title");
        }
    }

    @Test
    void extractSearchValues_shouldExtractFromOnlinedienstContent() {
        // HTML Inhalte werden bereinigt
        OnlinedienstContent content1 = mock(OnlinedienstContent.class);
        when(content1.getName()).thenReturn("<p>Service Name</p>");
        when(content1.getDescription()).thenReturn("Service <b>Description</b>");
        assertThat(SearchTermsExtractor.extractSearchValues(content1)).containsExactlyInAnyOrder(
            "Service Name",
            "Service Description"
        );

        // Null / leere Strings -> keine Werte
        OnlinedienstContent content2 = mock(OnlinedienstContent.class);
        when(content2.getName()).thenReturn(null);
        when(content2.getDescription()).thenReturn("");
        assertThat(SearchTermsExtractor.extractSearchValues(content2)).isEmpty();
    }

    @Test
    void extractSearchNames_fromLeistungsbeschreibung_shouldExtractNames() {
        LeistungsbeschreibungContent content = mockLbContentWithDetails();

        try (
            MockedStatic<ModulesStringExtractor> mockedExtractor = Mockito.mockStatic(
                ModulesStringExtractor.class
            )
        ) {
            mockStringFieldExtraction(
                mockedExtractor,
                content.getDetails(),
                ServiceModuleCode.LEISTUNGSBEZEICHNUNG,
                "Leistung 1"
            );
            mockStringFieldExtraction(
                mockedExtractor,
                content.getDetails(),
                ServiceModuleCode.LEISTUNGSNAME,
                "Name 1"
            );

            assertThat(SearchTermsExtractor.extractSearchNames(content)).containsExactlyInAnyOrder(
                "Leistung 1",
                "Name 1"
            );
        }
    }

    @Test
    void extractSearchDescriptions_fromLeistungsbeschreibung_shouldExtractDescriptions() {
        LeistungsbeschreibungContent content = mockLbContentWithDetails();

        try (
            MockedStatic<ModulesStringExtractor> mockedExtractor = Mockito.mockStatic(
                ModulesStringExtractor.class
            )
        ) {
            mockStringFieldExtraction(
                mockedExtractor,
                content.getDetails(),
                ServiceModuleCode.VOLLTEXT,
                "Full text"
            );
            mockStringFieldExtraction(
                mockedExtractor,
                content.getDetails(),
                ServiceModuleCode.TEASER,
                "Teaser text"
            );

            assertThat(
                SearchTermsExtractor.extractSearchDescriptions(content)
            ).containsExactlyInAnyOrder("Full text", "Teaser text");
        }
    }

    @Test
    void extractSearchNames_fromSpezialisierung_shouldExtractNames() {
        SpezialisierungContent content = mockSpContentWithDetails();

        try (
            MockedStatic<ModulesStringExtractor> mockedExtractor = Mockito.mockStatic(
                ModulesStringExtractor.class
            )
        ) {
            mockStringFieldSpExtraction(
                mockedExtractor,
                content.getDetails(),
                ServiceModuleCode.LEISTUNGSBEZEICHNUNG,
                "Sp Leistung"
            );
            mockStringFieldSpExtraction(
                mockedExtractor,
                content.getDetails(),
                ServiceModuleCode.LEISTUNGSNAME,
                "Sp Name"
            );

            assertThat(SearchTermsExtractor.extractSearchNames(content)).containsExactlyInAnyOrder(
                "Sp Leistung",
                "Sp Name"
            );
        }
    }

    @Test
    void extractSearchDescriptions_fromSpezialisierung_shouldExtractDescriptions() {
        SpezialisierungContent content = mockSpContentWithDetails();

        try (
            MockedStatic<ModulesStringExtractor> mockedExtractor = Mockito.mockStatic(
                ModulesStringExtractor.class
            )
        ) {
            mockStringFieldSpExtraction(
                mockedExtractor,
                content.getDetails(),
                ServiceModuleCode.VOLLTEXT,
                "Sp Full text"
            );
            mockStringFieldSpExtraction(
                mockedExtractor,
                content.getDetails(),
                ServiceModuleCode.TEASER,
                "Sp Teaser"
            );

            assertThat(
                SearchTermsExtractor.extractSearchDescriptions(content)
            ).containsExactlyInAnyOrder("Sp Full text", "Sp Teaser");
        }
    }

    // Helper
    private static SearchTerms searchTerms(String name, String desc, String value) {
        return new SearchTerms(Set.of(name), Set.of(desc), Set.of(value));
    }

    private static LeistungsbeschreibungContent mockLbContentWithDetails() {
        LeistungsbeschreibungContent content = mock(LeistungsbeschreibungContent.class);
        when(content.getDetails()).thenReturn(new HashMap<>());
        return content;
    }

    private static SpezialisierungContent mockSpContentWithDetails() {
        SpezialisierungContent content = mock(SpezialisierungContent.class);
        when(content.getDetails()).thenReturn(new HashMap<>());
        return content;
    }

    private static void mockStringFieldExtraction(
        MockedStatic<ModulesStringExtractor> mockedExtractor,
        Map<String, List<AbstractServiceModule>> details,
        ServiceModuleCode code,
        String returnValue
    ) {
        mockedExtractor
            .when(() -> ModulesStringExtractor.extractStringField(eq(details), eq(code), any()))
            .thenReturn(Stream.of(returnValue));
    }

    private static void mockStringFieldSpExtraction(
        MockedStatic<ModulesStringExtractor> mockedExtractor,
        Map<String, List<ServiceBlockSpecialization>> details,
        ServiceModuleCode code,
        String returnValue
    ) {
        mockedExtractor
            .when(() -> ModulesStringExtractor.extractStringFieldSp(eq(details), eq(code), any()))
            .thenReturn(Stream.of(returnValue));
    }
}
