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
package de.dataport.suchdienst.indexer.parser.x23.modul;

import de.dataport.suchdienst.indexer.model.mapper.BusinessObjectMapper;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SpezialisierungContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SpezialisierungVL;
import de.dataport.suchdienst.indexer.parser.x23.locale.LanguageAwareSpParser;
import de.dataport.suchdienst.indexer.service.languagecheck.LanguageSpCompleteCheckService;
import de.xoev.xzufi.v2_3_1.Identifikator;
import de.xoev.xzufi.v2_3_1.LeistungSpezialisierung;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service("leistungSpezialisierungParserX23")
public class LeistungSpezialisierungParser {

    private final LanguageAwareSpParser languageSpParser;
    private final LanguageSpCompleteCheckService languageSpCompleteCheckService;

    public LeistungSpezialisierungParser(
        @Qualifier("languageAwareSpParserX23") LanguageAwareSpParser languageSpParser,
        LanguageSpCompleteCheckService languageSpCompleteCheckService
    ) {
        this.languageSpParser = languageSpParser;
        this.languageSpCompleteCheckService = languageSpCompleteCheckService;
    }

    public SpezialisierungVL parse(LeistungSpezialisierung ls, String lbId, String spId) {
        SpezialisierungVL spezialisierung = new SpezialisierungVL();
        spezialisierung.setIdLB(lbId);
        Set<String> ars = ls
            .getGebietID()
            .stream()
            .map(Identifikator::getValue)
            .collect(Collectors.toSet());
        spezialisierung.setArs(ars);
        spezialisierung.setId(spId);

        // Inhalte nach Sprachen parsen
        Map<Locale, SpezialisierungContent> languages = languageSpParser.parse(ls, spezialisierung);

        applyGerman(languages.get(Locale.GERMAN), spezialisierung);

        // Deutsch als Standard uebernehmen
        languages.remove(Locale.GERMAN);
        spezialisierung.setOtherLanguages(languages);
        checkLanguageComplete(spezialisierung, languages);
        return spezialisierung;
    }

    private void checkLanguageComplete(
        SpezialisierungVL spezialisierung,
        Map<Locale, SpezialisierungContent> languages
    ) {
        languages
            .keySet()
            .stream()
            .filter(locale -> !Locale.GERMAN.equals(locale))
            .forEach(locale ->
                spezialisierung
                    .getLanguageComplete()
                    .put(
                        locale,
                        languageSpCompleteCheckService.checkLanguageComplete(
                            languages,
                            spezialisierung,
                            locale
                        )
                    )
            );
    }

    private void applyGerman(
        SpezialisierungContent contentFromLanguages,
        SpezialisierungContent actualSpecialization
    ) {
        if (contentFromLanguages != null && actualSpecialization != null) {
            BusinessObjectMapper.INSTANCE.copyContent(contentFromLanguages, actualSpecialization);
        }
    }
}
