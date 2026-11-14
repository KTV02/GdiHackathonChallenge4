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

import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstVL;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LanguageOdCompleteCheckService implements LanguageCompleteCheck {

    public boolean checkLanguageComplete(
        Map<Locale, OnlinedienstContent> languageContent,
        OnlinedienstVL od,
        Locale language
    ) {
        OnlinedienstContent content = languageContent.get(language);
        if (content == null) {
            return false;
        }

        return (
            stringFieldComplete(od.getName(), content.getName()) &&
            stringFieldComplete(od.getDescription(), content.getDescription()) &&
            stringFieldComplete(od.getHelpText(), content.getHelpText()) &&
            od.getHelpLinks().size() <= content.getHelpLinks().size()
        );
    }
}
