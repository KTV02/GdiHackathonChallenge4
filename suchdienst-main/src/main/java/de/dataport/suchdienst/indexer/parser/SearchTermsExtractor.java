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

import de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.HasSearchTerms;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SearchTerms;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleLink;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SpezialisierungContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SpezialisierungVL;
import de.dataport.suchdienst.indexer.service.suggestion.SuggestionHelper;
import de.dataport.suchdienst.indexer.util.ModulesStringExtractor;
import de.dataport.suchdienst.util.ServiceModuleCode;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.TriFunction;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

/**
 * Utility-Klasse zum Extrahieren von Suchbegriffen aus Verwaltungsleistungen.
 *
 * Diese Klasse bietet Methoden zum Extrahieren und Zusammenführen von
 * Suchbegriffen
 * aus verschiedenen Inhaltstypen wie Leistungsbeschreibungen, Spezialisierungen
 * und
 * Onlinediensten. Die extrahierten Begriffe werden für die
 * Elasticsearch-Indizierung
 * und Suchfunktionalität verwendet.
 */
public class SearchTermsExtractor {

    /**
     * HTML-Policy für die Bereinigung von HTML-Inhalten.
     * Entfernt alle HTML-Tags und behält nur den Textinhalt.
     */
    private static final PolicyFactory HTML_POLICY = new HtmlPolicyBuilder().toFactory();

    private SearchTermsExtractor() {
        // Util class
    }

    /**
     * Führt Suchbegriffe aus verschiedenen Verwaltungsleistungstypen zusammen
     * und organisiert sie nach Sprachen.
     *
     * @param lb  Leistungsbeschreibung
     * @param sps Liste der Spezialisierungen
     * @param ods Liste der Onlinedienste
     * @return Map mit Suchbegriffen, gruppiert nach Sprachen (Locale)
     */
    public static Map<Locale, SearchTerms> mergeSearchTermsLocalized(
        LeistungsbeschreibungVL lb,
        List<SpezialisierungVL> sps,
        List<OnlinedienstVL> ods
    ) {
        Map<Locale, SearchTerms> localizedSearchTerms = new LinkedHashMap<>();

        // German content
        Stream<? extends HasSearchTerms> deContent = Stream.of(
            Stream.of(lb),
            sps.stream(),
            ods.stream()
        ).flatMap(Function.identity());
        localizedSearchTerms.put(Locale.GERMAN, reduceSearchTerms(deContent));

        // Other languages
        lb.getOtherLanguages().forEach((language, content) -> {
            Stream<HasSearchTerms> otherContent = Stream.of(
                Stream.of(content),
                sps
                    .stream()
                    .map(sp -> sp.getOtherLanguages().get(language))
                    .filter(Objects::nonNull),
                ods
                    .stream()
                    .map(od -> od.getOtherLanguages().get(language))
                    .filter(Objects::nonNull)
            ).flatMap(Function.identity());
            localizedSearchTerms.put(language, reduceSearchTerms(otherContent));
        });

        return localizedSearchTerms;
    }

    /**
     * Reduziert einen Stream von HasSearchTerms-Objekten zu einem einzigen
     * SearchTerms-Objekt.
     * Kombiniert alle Suchbegriffe (Namen, Beschreibungen, Werte) in einem
     * Ergebnisobjekt.
     *
     * @param contents Stream von Objekten, die Suchbegriffe enthalten
     * @return Kombiniertes SearchTerms-Objekt mit allen gefundenen Begriffen
     */
    private static SearchTerms reduceSearchTerms(Stream<? extends HasSearchTerms> contents) {
        return contents
            .map(HasSearchTerms::getSearchTerms)
            .filter(Objects::nonNull)
            .reduce(
                new SearchTerms(new HashSet<>(), new HashSet<>(), new HashSet<>()),
                (result, st) -> {
                    result.searchNames().addAll(st.searchNames());
                    result.searchDescriptions().addAll(st.searchDescriptions());
                    result.searchValues().addAll(st.searchValues());
                    return result;
                }
            );
    }

    /**
     * Extrahiert Ursprungsportal-Titel aus einer Leistungsbeschreibung.
     *
     * @param lb Leistungsbeschreibungsinhalt
     * @return Set von Ursprungsportal-Titeln
     */
    public static Set<String> extractUrsprungsportalTitel(LeistungsbeschreibungContent lb) {
        return extractUrsprungsportalTitel(
            ModulesStringExtractor::extractStringField,
            lb.getDetails()
        );
    }

    /**
     * Extrahiert Ursprungsportal-Titel aus einer Spezialisierung.
     *
     * @param sp Spezialisierungsinhalt
     * @return Set von Ursprungsportal-Titeln
     */
    public static Set<String> extractUrsprungsportalTitel(SpezialisierungContent sp) {
        return extractUrsprungsportalTitel(
            ModulesStringExtractor::extractStringFieldSp,
            sp.getDetails()
        );
    }

    /**
     * Generische Methode zum Extrahieren von Ursprungsportal-Titeln.
     *
     * @param <T>       Typ der Details-Datenstruktur
     * @param extractor Funktion zum Extrahieren von String-Feldern
     * @param details   Die zu durchsuchenden Details
     * @return Set von bereinigten Ursprungsportal-Titeln
     */
    private static <T> Set<String> extractUrsprungsportalTitel(
        TriFunction<
            T,
            ServiceModuleCode,
            Function<AbstractServiceModule, String>,
            Stream<String>
        > extractor,
        T details
    ) {
        return extractor
            .apply(
                details,
                ServiceModuleCode.URSPRUNGSPORTAL,
                SearchTermsExtractor::extractLinkTitle
            )
            .map(SearchTermsExtractor::sanitize)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());
    }

    /**
     * Extrahiert den Link-Titel aus einem ServiceModuleLink.
     * Wird kein ServiceModuleLink übergeben, wird null zurückgegeben.
     *
     * @param module Das Service-Modul
     * @return Der Link-Titel oder null, falls keine Links vorhanden
     */
    private static String extractLinkTitle(AbstractServiceModule module) {
        if (module instanceof ServiceModuleLink sml) {
            return sml.getLink().title();
        }
        return null;
    }

    /**
     * Extrahiert Suchwerte aus Onlinedienst-Inhalten.
     * Die extrahierten Wörter/Sätze werden als "searchValues" für die Suche
     * indexiert.
     *
     * @param onlinedienstContent Inhalt des Onlinedienstes
     * @return Set von Suchwerten aus Name und Beschreibung des Onlinedienstes
     */
    public static Set<String> extractSearchValues(OnlinedienstContent onlinedienstContent) {
        return Stream.of(onlinedienstContent.getName(), onlinedienstContent.getDescription())
            .map(SearchTermsExtractor::sanitize)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());
    }

    /**
     * Extrahiert Suchnamen aus Leistungsbeschreibungsinhalten.
     *
     * @param content Leistungsbeschreibungsinhalt
     * @return Set von Suchnamen aus Leistungsbezeichnung und Leistungsname
     */
    public static Set<String> extractSearchNames(LeistungsbeschreibungContent content) {
        return extractText(
            content.getDetails(),
            ModulesStringExtractor::extractStringField,
            ServiceModuleCode.LEISTUNGSBEZEICHNUNG,
            ServiceModuleCode.LEISTUNGSNAME
        );
    }

    /**
     * Extrahiert Suchbeschreibungen aus Leistungsbeschreibungsinhalten.
     *
     * @param content Leistungsbeschreibungsinhalt
     * @return Set von Suchbeschreibungen aus Volltext und Teaser
     */
    public static Set<String> extractSearchDescriptions(LeistungsbeschreibungContent content) {
        return extractText(
            content.getDetails(),
            ModulesStringExtractor::extractStringField,
            ServiceModuleCode.VOLLTEXT,
            ServiceModuleCode.TEASER
        );
    }

    /**
     * Extrahiert Suchnamen aus Spezialisierungsinhalten.
     *
     * @param content Spezialisierungsinhalt
     * @return Set von Suchnamen aus Leistungsbezeichnung und Leistungsname
     */
    public static Set<String> extractSearchNames(SpezialisierungContent content) {
        return extractText(
            content.getDetails(),
            ModulesStringExtractor::extractStringFieldSp,
            ServiceModuleCode.LEISTUNGSBEZEICHNUNG,
            ServiceModuleCode.LEISTUNGSNAME
        );
    }

    /**
     * Extrahiert Suchbeschreibungen aus Spezialisierungsinhalten.
     *
     * @param content Spezialisierungsinhalt
     * @return Set von Suchbeschreibungen aus Volltext und Teaser
     */
    public static Set<String> extractSearchDescriptions(SpezialisierungContent content) {
        return extractText(
            content.getDetails(),
            ModulesStringExtractor::extractStringFieldSp,
            ServiceModuleCode.VOLLTEXT,
            ServiceModuleCode.TEASER
        );
    }

    /**
     * Generische Methode zum Extrahieren von Text aus verschiedenen
     * Service-Modultypen.
     *
     * @param <T>       Typ der Details-Datenstruktur
     * @param details   Die zu durchsuchenden Details
     * @param extractor Funktion zum Extrahieren von String-Feldern
     * @param codes     ServiceModuleCodes, aus denen Text extrahiert werden soll
     * @return Set von bereinigten Textinhalten
     */
    private static <T> Set<String> extractText(
        T details,
        TriFunction<
            T,
            ServiceModuleCode,
            Function<AbstractServiceModule, String>,
            Stream<String>
        > extractor,
        ServiceModuleCode... codes
    ) {
        return Arrays.stream(codes)
            .flatMap(code -> extractor.apply(details, code, AbstractServiceModule::getText))
            .map(SearchTermsExtractor::sanitize)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());
    }

    /**
     * Bereinigt HTML-Inhalte und normalisiert den Text für die Suchindizierung.
     * Entfernt HTML-Tags, fügt Leerzeichen zwischen Wörtern und Zahlen ein
     * und entfernt überflüssige Leerzeichen.
     *
     * @param input Der zu bereinigende Eingabetext
     * @return Bereinigter und normalisierter Text, oder leerer String bei
     *         null/blank Input
     */
    private static String sanitize(String input) {
        if (StringUtils.isBlank(input)) {
            return "";
        }
        return SuggestionHelper.addSpaceBetweenWordAndNumber(HTML_POLICY.sanitize(input)).trim();
    }
}
