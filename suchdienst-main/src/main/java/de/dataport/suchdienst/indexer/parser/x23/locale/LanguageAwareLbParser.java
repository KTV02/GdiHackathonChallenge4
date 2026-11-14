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
package de.dataport.suchdienst.indexer.parser.x23.locale;

import de.dataport.suchdienst.indexer.model.mapping.IdentificationType;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SearchTerms;
import de.dataport.suchdienst.indexer.parser.ModuleSorting;
import de.dataport.suchdienst.indexer.parser.ParserSelector;
import de.dataport.suchdienst.indexer.parser.SearchTermsExtractor;
import de.dataport.suchdienst.indexer.parser.XzufiParserUtils;
import de.dataport.suchdienst.indexer.service.suggestion.SuggestionHelper;
import de.dataport.suchdienst.indexer.util.ModulesStringExtractor;
import de.dataport.suchdienst.indexer.util.x23.XZuFiUtil;
import de.dataport.suchdienst.util.IdentifikatorUtil;
import de.xoev.xzufi.v2_3_1.Leistung;
import de.xoev.xzufi.v2_3_1.Sprache;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service("languageAwareLbParserX23")
public class LanguageAwareLbParser {

    private final ParserSelector parserSelector;
    private final LanguageAwareSearchValuesAndSuggestionParser synonymAndSuggestionParser;

    public LanguageAwareLbParser(
        @Qualifier(
            "languageAwareSearchValuesAndSuggestionParserX23"
        ) LanguageAwareSearchValuesAndSuggestionParser synonymAndSuggestionParser,
        @Qualifier("parserSelectorX23") ParserSelector parserSelector
    ) {
        this.synonymAndSuggestionParser = synonymAndSuggestionParser;
        this.parserSelector = parserSelector;
    }

    public Map<Locale, LeistungsbeschreibungContent> parse(
        Leistung xlb,
        LeistungsbeschreibungVL sd
    ) {
        return xlb
            .getSprachversion()
            .stream()
            .map(Sprache::getLanguageCode)
            .filter(languageCode -> {
                if (StringUtils.isEmpty(languageCode)) {
                    log.warn("Invalide Sprache-Info in {}", sd.getId());
                    return false;
                }

                return true;
            })
            .map(XzufiParserUtils::getSupportedLocale)
            .map(locale -> this.parseLanguage(xlb, sd.getId(), locale))
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map.Entry<Locale, LeistungsbeschreibungContent> parseLanguage(
        Leistung xlb,
        String lbId,
        Locale locale
    ) {
        String language = locale.toString();
        log.debug("Parse Lb-Inhalt von {} fuer Sprache {}", lbId, language);

        LeistungsbeschreibungContent content = parseLanguage(xlb, locale);
        if (ModulesStringExtractor.findNameOtherOrName(content.getDetails()).isEmpty()) {
            if (XZuFiUtil.isLanguageOrCountry(locale, "de")) {
                log.warn(
                    "Fehlerhafte Daten: LB {} enthält keinen Namen auf Deutsch! Versuche den Fehler zu ignorieren.",
                    lbId
                );
            } else {
                log.debug(
                    "Ignoriere unvollständigen Inhalt von LB {} für Sprache {}! Name muss vorhanden sein!",
                    lbId,
                    language
                );
                // Strenges Vorgehen: Englisch wird komplett ignoriert, wenn kein Name gefunden wurde.
                // Alternativ könnte man Titel-Fallbacks etc. auch später noch ermitteln.
                return null;
            }
        }
        return Map.entry(locale, content);
    }

    private LeistungsbeschreibungContent parseLanguage(Leistung xlb, Locale locale) {
        var content = new LeistungsbeschreibungContent();
        var details = parseLbModules(xlb, locale);
        var synonymsAndSuggestions = synonymAndSuggestionParser.extractSynonymsAndSuggestions(
            xlb,
            locale
        );

        content.setDetails(details);

        Set<String> searchNames = SearchTermsExtractor.extractSearchNames(content);
        Set<String> searchDescriptions = SearchTermsExtractor.extractSearchDescriptions(content);
        Set<String> ursprungsportal = SearchTermsExtractor.extractUrsprungsportalTitel(content);
        Set<String> synonyms = SuggestionHelper.addSpaceBetweenWordAndNumber(
            synonymsAndSuggestions.synonyms()
        );
        Set<String> searchValues = new HashSet<>(ursprungsportal);
        searchValues.addAll(synonyms);
        SearchTerms searchTerms = new SearchTerms(searchNames, searchDescriptions, searchValues);
        content.setSearchTerms(searchTerms);

        content.getSuggestions().addAll(synonymsAndSuggestions.suggestions());
        content.getSynonyms().addAll(synonymsAndSuggestions.synonyms());

        return content;
    }

    protected Map<String, List<AbstractServiceModule>> parseLbModules(Leistung xlb, Locale locale) {
        var lbId = IdentifikatorUtil.getIdentificatorString(xlb.getId(), IdentificationType.LB);
        return Stream.of(
            xlb.getModulText().stream(),
            xlb.getModulTextIndividuell().stream(),
            xlb.getModulDokument().stream(),
            xlb.getModulKosten().stream(),
            xlb.getModulBearbeitungsdauer().stream(),
            xlb.getModulFrist().stream(),
            xlb.getModulUrsprungsportal().stream(),
            xlb.getModulFachlicheFreigabe().stream()
        )
            .flatMap(Function.identity())
            .flatMap(m -> parserSelector.acceptList(m, locale, lbId).stream())
            .sorted(ModuleSorting.getModuleComparator())
            .collect(
                Collectors.groupingBy(
                    Map.Entry::getKey,
                    LinkedHashMap::new,
                    Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                )
            );
    }
}
