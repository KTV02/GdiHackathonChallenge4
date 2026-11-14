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

import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SearchTerms;
import de.dataport.suchdienst.indexer.parser.SearchTermsExtractor;
import de.dataport.suchdienst.indexer.parser.XzufiParserUtils;
import de.dataport.suchdienst.indexer.service.CodelistService;
import de.dataport.suchdienst.indexer.util.x22.XZuFiUtil;
import de.xoev.xzufi.v2_2_0.Onlinedienst;
import de.xoev.xzufi.v2_2_0.Sprache;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service("languageAwareOdParserX22")
@Slf4j
public class LanguageAwareOdParser {

    private final CodelistService codelistService;

    public LanguageAwareOdParser(CodelistService codelistService) {
        this.codelistService = codelistService;
    }

    public Map<Locale, OnlinedienstContent> parseOnlineService(
        de.xoev.xzufi.v2_2_0.Onlinedienst xod,
        OnlinedienstVL od
    ) {
        // return language content
        return xod
            .getSprachversion()
            .stream()
            .map(Sprache::getLanguageCode)
            .filter(languageCode -> {
                if (StringUtils.isEmpty(languageCode)) {
                    log.warn("Invalide Sprache-Info in {}", od.getId());
                    return false;
                }

                return true;
            })
            .map(XzufiParserUtils::getSupportedLocale)
            .map(locale -> {
                String language = locale.toString();
                String onlineServiceId = od.getId();
                log.debug("Parse Od-Inhalt von {} fuer Sprache {}", onlineServiceId, language);

                OnlinedienstContent content = parseLanguageOnlineService(xod, locale);
                if (StringUtils.isBlank(content.getName())) {
                    log.debug(
                        "Fehlerhafte Daten: OD {} enthält in der Sprachvariante '{}' keinen Namen! Versuche den Fehler zu ignorieren.",
                        od.getId(),
                        language
                    );
                }
                return Map.entry(locale, content);
            })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private OnlinedienstContent parseLanguageOnlineService(Onlinedienst xod, Locale locale) {
        OnlinedienstContent content = new OnlinedienstContent();
        content.setName(
            XZuFiUtil.getByLanguage(xod.getBezeichnung(), locale).stream().findFirst().orElse(null)
        );
        content.setDescription(
            XZuFiUtil.getByLanguage(xod.getBeschreibung(), locale).stream().findFirst().orElse(null)
        );
        content.setHelpText(
            XZuFiUtil.getByLanguage(xod.getHilfeText(), locale).stream().findFirst().orElse(null)
        );
        content.setHelpLinks(XZuFiUtil.getHyperlinks(xod.getHilfeLink(), locale));
        content.setPublisher(XZuFiUtil.getPublisher(xod.getHerausgeber(), locale).orElse(null));

        content.setLinks(XZuFiUtil.getOnlinedienstHyperlinks(xod.getLink(), locale));

        Set<String> identificationMethods = xod
            .getIdentifizierungsmittel()
            .stream()
            .map(code -> {
                CodelistEntry entry = codelistService.getCodelistEntry(
                    code.getListURI(),
                    code.getListVersionID(),
                    code.getCode()
                );
                return entry.getText(locale);
            })
            .collect(Collectors.toSet());
        content.setIdentificationMethods(identificationMethods);

        Set<String> paymentMethods = xod
            .getZahlungsweise()
            .stream()
            .map(code -> {
                CodelistEntry entry = codelistService.getCodelistEntry(
                    code.getListURI(),
                    code.getListVersionID(),
                    code.getCode()
                );
                return entry.getText(locale);
            })
            .collect(Collectors.toSet());
        content.setPaymentMethods(paymentMethods);

        SearchTerms searchTerms = new SearchTerms(
            Set.of(),
            Set.of(),
            SearchTermsExtractor.extractSearchValues(content)
        );
        content.setSearchTerms(searchTerms);

        return content;
    }
}
