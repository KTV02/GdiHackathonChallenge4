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

import de.dataport.suchdienst.indexer.model.mapping.CodeServiceDescription;
import de.dataport.suchdienst.indexer.parser.XzufiParserUtils;
import de.dataport.suchdienst.indexer.parser.x23.locale.model.SynonymsAndSuggestions;
import de.dataport.suchdienst.indexer.service.suggestion.AbstractSuggestionAnalyzer;
import de.dataport.suchdienst.indexer.service.suggestion.LanguageDeterminationComponent;
import de.dataport.suchdienst.indexer.service.suggestion.SuggestionHelper;
import de.xoev.xzufi.v2_3_1.BegriffImKontext;
import de.xoev.xzufi.v2_3_1.BegriffImKontextModul;
import de.xoev.xzufi.v2_3_1.Leistung;
import de.xoev.xzufi.v2_3_1.LeistungSpezialisierung;
import de.xoev.xzufi.v2_3_1.LeistungsmodulSpezialisierung;
import de.xoev.xzufi.v2_3_1.StringLocalized;
import de.xoev.xzufi.v2_3_1.StringLocalizedHTML;
import de.xoev.xzufi.v2_3_1.Textmodul;
import de.xoev.xzufi.v2_3_1.UrsprungsportalModul;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Service;

@Slf4j
@Service("languageAwareSearchValuesAndSuggestionParserX23")
public class LanguageAwareSearchValuesAndSuggestionParser extends LanguageParser {

    private final LanguageDeterminationComponent languageDeterminationService;

    public LanguageAwareSearchValuesAndSuggestionParser(
        LanguageDeterminationComponent languageDeterminationService
    ) {
        this.languageDeterminationService = languageDeterminationService;
    }

    /**
     * Extrahiert Synonyme und Suggestions aus der xLeistung.
     * Suggestions enthalten Informationen aus: ModulBegriffImKontext, ModulUrsprungsportal, ModulText
     * Synonyme enthalten Informationen aus: ModulBegriffImKontext
     * <p>
     * Die Synonyme und Suggestions werden später für die Suche verwendet:
     * Suggestions fließen in den gleichnamigen Index zur Autovervollständigung
     * Synonyme werden für die Berechnung der Trefferliste mit durchsucht.
     */
    public SynonymsAndSuggestions extractSynonymsAndSuggestions(Leistung xlb, Locale locale) {
        AbstractSuggestionAnalyzer analyzer = languageDeterminationService.determineAnalyzer(
            locale
        );

        List<String> fromContextModules = getTextFromContextModules(
            xlb.getModulBegriffImKontext(),
            locale
        );
        List<String> fromPortalModules = getTextFromPortalModules(
            xlb.getModulUrsprungsportal(),
            locale
        );
        List<String> fromTextModules = getTextFromTextModules(xlb.getModulText(), locale);

        // Suggestions: dürfen kein HTML enthalten, werden in Tokens zerlegt, Stop-Words entfernt.
        // Römische Zahlen und Sonderzeichen dürfen nicht entfernt werden, da dem Nutzer die echten Wörter gezeigt werden sollen
        Stream<String> suggestionTokens = Stream.of(
            fromContextModules,
            fromPortalModules,
            fromTextModules
        )
            .flatMap(Collection::stream)
            .map(LanguageAwareSearchValuesAndSuggestionParser::removeAllHtml)
            .map(s -> SuggestionHelper.extractTokens(s, analyzer))
            .flatMap(Collection::stream);
        // Die Werte aus den Textmodulen (Name/NameOtherDescription) werden zusätzlich als gesamter Term hinzugefügt
        // dadurch kann der Titel einer Leistung vervollständigt werden.
        Set<String> suggestions = Stream.concat(suggestionTokens, fromTextModules.stream()).collect(
            Collectors.toSet()
        );

        // Synonyms: dürfen kein HTML enthalten
        Set<String> cleanSynonyms = fromContextModules
            .stream()
            .map(LanguageAwareSearchValuesAndSuggestionParser::removeAllHtml)
            .collect(Collectors.toSet());
        return new SynonymsAndSuggestions(cleanSynonyms, suggestions);
    }

    /**
     * Extrahiert eine Menge an Wörtern/Sätzen aus der Spezialisierung.
     * Die Wörter sollen als "searchValues" für die Suche indexiert werden.
     * Durch Extrahieren von Begriffen aus Spezialisierungen soll die Qualität
     * der Suchtreffer von verwiesenen Leistungen gesteigert werden
     */
    public Set<String> extractSynonyms(LeistungSpezialisierung ls, Locale locale) {
        List<BegriffImKontextModul> contextModules = ls
            .getModulSpezialisiert()
            .stream()
            .filter(Objects::nonNull)
            .map(LeistungsmodulSpezialisierung::getModul)
            .filter(BegriffImKontextModul.class::isInstance)
            .map(BegriffImKontextModul.class::cast)
            .toList();

        List<String> contextModulesText = getTextFromContextModules(contextModules, locale);

        Stream<String> cleanSearchValues = contextModulesText
            .stream()
            .map(LanguageAwareSearchValuesAndSuggestionParser::removeAllHtml)
            .map(SuggestionHelper::addSpaceBetweenWordAndNumber)
            .filter(StringUtils::isNotBlank);

        return cleanSearchValues.collect(Collectors.toSet());
    }

    private Predicate<UrsprungsportalModul> portalLangFilter(Locale locale) {
        return p ->
            XzufiParserUtils.getSupportedLocale(p.getLanguageCode())
                .getLanguage()
                .equals(locale.getLanguage());
    }

    // Soll nur Tokens beachten, keinen ganzen Text
    private List<String> getTextFromContextModules(
        List<BegriffImKontextModul> modules,
        Locale locale
    ) {
        return modules
            .stream()
            .flatMap(m -> m.getBegriffImKontext().stream().map(BegriffImKontext::getBegriff))
            .filter(s -> ObjectUtils.isNotEmpty(s.getLanguageCode()))
            .filter(stringLangFilter(locale))
            .map(StringLocalized::getValue)
            .filter(Objects::nonNull)
            .toList();
    }

    // Soll Tokens und gesamten Text enthalten
    private List<String> getTextFromTextModules(List<Textmodul> modules, Locale locale) {
        return modules
            .stream()
            .filter(m -> m.getLeikaTextmodul() != null)
            .map(m ->
                Map.entry(CodeServiceDescription.getByCode(m.getLeikaTextmodul().getCode()), m)
            )
            .filter(
                map ->
                    map.getKey().isPresent() &&
                    (map.getKey().get() == CodeServiceDescription.NAME ||
                        map.getKey().get() == CodeServiceDescription.NAME_OTHER_DESCRIPTION)
            )
            .flatMap(m -> m.getValue().getInhalt().stream())
            .filter(s -> ObjectUtils.isNotEmpty(s.getLanguageCode()))
            .filter(stringLocalizedHTMLLanguageFilter(locale))
            .map(StringLocalizedHTML::getValue)
            .filter(Objects::nonNull)
            .toList();
    }

    // Soll nur Tokens beachten, keinen ganzen Text
    private List<String> getTextFromPortalModules(
        List<UrsprungsportalModul> modules,
        Locale locale
    ) {
        return modules
            .stream()
            .filter(portalLangFilter(locale))
            .map(UrsprungsportalModul::getTitel)
            .filter(Objects::nonNull)
            .toList();
    }

    private static final PolicyFactory HTML_POLICY = new HtmlPolicyBuilder().toFactory();

    static String removeAllHtml(String untrustedHTML) {
        return Optional.ofNullable(untrustedHTML).map(HTML_POLICY::sanitize).orElse("");
    }
}
