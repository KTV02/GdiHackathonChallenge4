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
package de.dataport.suchdienst.indexer.parser.x22.locale;

import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SearchTerms;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceBlockSpecialization;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SpezialisierungContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SpezialisierungVL;
import de.dataport.suchdienst.indexer.parser.ModuleSorting;
import de.dataport.suchdienst.indexer.parser.ParserSelector;
import de.dataport.suchdienst.indexer.parser.SearchTermsExtractor;
import de.dataport.suchdienst.indexer.parser.XzufiParserUtils;
import de.dataport.suchdienst.util.LocaleUtils;
import de.xoev.xzufi.v2_2_0.Auskunftshinweismodul;
import de.xoev.xzufi.v2_2_0.BegriffImKontextModul;
import de.xoev.xzufi.v2_2_0.FachlicheFreigabeModul;
import de.xoev.xzufi.v2_2_0.LeistungSpezialisierung;
import de.xoev.xzufi.v2_2_0.Leistungsmodul;
import de.xoev.xzufi.v2_2_0.LeistungsmodulSpezialisierung;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service("languageAwareSpParserX22")
@Slf4j
public class LanguageAwareSpParser extends LanguageParser {

    private final LanguageAwareSearchValuesAndSuggestionParser searchValuesAndSuggestionParser;
    private final ParserSelector parserSelector;

    public LanguageAwareSpParser(
        @Qualifier(
            "languageAwareSearchValuesAndSuggestionParserX22"
        ) LanguageAwareSearchValuesAndSuggestionParser searchValuesAndSuggestionParser,
        @Qualifier("parserSelectorX22") ParserSelector parserSelector
    ) {
        this.searchValuesAndSuggestionParser = searchValuesAndSuggestionParser;
        this.parserSelector = parserSelector;
    }

    public Map<Locale, SpezialisierungContent> parse(
        LeistungSpezialisierung xsp,
        SpezialisierungVL sp
    ) {
        String specializationId = sp.getId();
        return detectLanguages(xsp)
            .stream()
            .map(language -> {
                Locale locale = XzufiParserUtils.getSupportedLocale(language);
                log.debug("Parse Sp-Inhalt von {} fuer Sprache {}", specializationId, language);

                SpezialisierungContent content = parseLanguage(specializationId, xsp, locale);
                if (content == null) {
                    return null;
                }
                return Map.entry(locale, content);
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Set<String> detectLanguages(LeistungSpezialisierung xsp) {
        Set<String> languages = new HashSet<>();
        xsp.getBezeichnung().forEach(s -> languages.add(s.getLanguageCode()));
        // Prüfen, ob bereits alle unterstützen Sprachen gefunden wurden
        if (languages.size() == LocaleUtils.SUPPORTED_LANGUAGES.size()) {
            return languages;
        }

        for (LeistungsmodulSpezialisierung module : xsp.getModulSpezialisiert()) {
            Leistungsmodul modul = module.getModul();
            if (isUsedModule(modul)) {
                var parser = parserSelector.getModuleParser(modul);
                if (parser.isPresent()) {
                    languages.addAll(parser.get().getIncludedLanguages(modul));
                    if (languages.size() == LocaleUtils.SUPPORTED_LANGUAGES.size()) {
                        break;
                    }
                }
            }
        }
        return languages;
    }

    private SpezialisierungContent parseLanguage(
        String specializationId,
        LeistungSpezialisierung xsp,
        Locale locale
    ) {
        SpezialisierungContent content = new SpezialisierungContent();

        parseModules(xsp, locale, specializationId, content);
        if (content.getDetails().isEmpty()) {
            return null;
        }
        Set<String> searchNames = SearchTermsExtractor.extractSearchNames(content);
        Set<String> searchDescriptions = SearchTermsExtractor.extractSearchDescriptions(content);
        Set<String> ursprungsportalTitles = SearchTermsExtractor.extractUrsprungsportalTitel(
            content
        );
        Set<String> synonyms = searchValuesAndSuggestionParser.extractSynonyms(xsp, locale);
        Set<String> searchValues = new HashSet<>(ursprungsportalTitles);
        searchValues.addAll(synonyms);
        SearchTerms searchTerms = new SearchTerms(searchNames, searchDescriptions, searchValues);
        content.setSearchTerms(searchTerms);

        return content;
    }

    private void parseModules(
        LeistungSpezialisierung xsp,
        Locale locale,
        String specializationId,
        SpezialisierungContent content
    ) {
        content.setDetails(parseSpecializedModules(xsp, specializationId, locale));
    }

    private Map<String, List<ServiceBlockSpecialization>> parseSpecializedModules(
        LeistungSpezialisierung xsp,
        String specializationId,
        Locale locale
    ) {
        return xsp
            .getModulSpezialisiert()
            .stream()
            .filter(sp -> isUsedModule(sp.getModul()))
            .map(sp -> {
                Leistungsmodul m = sp.getModul();

                try {
                    var entry = parserSelector.accept(m, locale, specializationId);
                    if (entry == null) {
                        return null;
                    }
                    var spContent = new ServiceBlockSpecialization(
                        sp.isErsetzung(),
                        entry.getValue()
                    );
                    return Map.entry(entry.getKey(), spContent);
                } catch (Exception e) {
                    // PV-8946 catch-log-skip
                    log.warn(
                        "Unbekanntes Modul in den Spezialisierungen in der Leistungsbeschreibung mit der ID {}. Modul: {}",
                        specializationId,
                        m,
                        e
                    );
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .sorted(ModuleSorting.getModuleComparatorSp())
            .collect(
                Collectors.groupingBy(
                    Map.Entry::getKey,
                    LinkedHashMap::new,
                    Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                )
            );
    }

    private boolean isUsedModule(Leistungsmodul module) {
        return (
            !(module instanceof BegriffImKontextModul) &&
            !(module instanceof Auskunftshinweismodul) &&
            !(module instanceof FachlicheFreigabeModul)
        );
    }
}
