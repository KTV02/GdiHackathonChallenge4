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

import de.dataport.suchdienst.indexer.model.verwaltungsleistung.FormularContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.FormularVL;
import de.dataport.suchdienst.indexer.parser.XzufiParserUtils;
import de.dataport.suchdienst.indexer.util.x22.XZuFiUtil;
import de.xoev.xzufi.v2_2_0.Formular;
import de.xoev.xzufi.v2_2_0.Sprache;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service("languageAwareFoParserX22")
@Slf4j
public class LanguageAwareFoParser {

    public Map<Locale, FormularContent> parseFormular(Formular xfo, FormularVL fo) {
        // return language content
        return xfo
            .getSprachversion()
            .stream()
            .map(Sprache::getLanguageCode)
            .filter(languageCode -> {
                if (StringUtils.isEmpty(languageCode)) {
                    log.warn("Invalide Sprache-Info in {}", fo.getId());
                    return false;
                }

                return true;
            })
            .map(XzufiParserUtils::getSupportedLocale)
            .map(locale -> {
                String language = locale.toString();
                String formularId = fo.getId();
                log.debug("Parse Fo-Inhalt von {} fuer Sprache {}", formularId, language);

                FormularContent content = parseLanguageFormular(xfo, locale);
                if (StringUtils.isBlank(content.getName())) {
                    log.debug(
                        "Fehlerhafte Daten: FO {} enthält in der Sprachvariante '{}' keinen Namen! Versuche den Fehler zu ignorieren.",
                        fo.getId(),
                        language
                    );
                }
                return Map.entry(locale, content);
            })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private FormularContent parseLanguageFormular(Formular xfo, Locale locale) {
        FormularContent content = new FormularContent();
        content.setName(
            XZuFiUtil.getByLanguage(xfo.getBezeichnung(), locale).stream().findFirst().orElse(null)
        );
        content.setDescription(
            XZuFiUtil.getByLanguage(xfo.getBeschreibung(), locale).stream().findFirst().orElse(null)
        );
        return content;
    }
}
