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

import de.dataport.suchdienst.indexer.model.verwaltungsleistung.FormularContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.FormularVL;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LanguageFoCompleteCheckService implements LanguageCompleteCheck {

    public boolean checkLanguageComplete(
        Map<Locale, FormularContent> languageContent,
        FormularVL fp,
        Locale language
    ) {
        FormularContent content = languageContent.get(language);
        if (content == null) {
            return false;
        }

        return (
            stringFieldComplete(fp.getName(), content.getName()) &&
            stringFieldComplete(fp.getDescription(), content.getDescription())
        );
    }
}
