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

import de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungContent;
import de.dataport.suchdienst.util.ServiceModuleCode;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LanguageLbCompleteCheckService extends LanguageModuleCompleteCheckService {

    public boolean checkLanguageComplete(
        Map<Locale, LeistungsbeschreibungContent> languageContent,
        LeistungsbeschreibungContent sd,
        Locale language
    ) {
        LeistungsbeschreibungContent content = languageContent.get(language);
        if (content != null) {
            Map<String, List<AbstractServiceModule>> germanDetails = sd.getDetails();
            Map<String, List<AbstractServiceModule>> otherDetails = content.getDetails();

            return Arrays.stream(ServiceModuleCode.values()).allMatch(code ->
                checkModulesComplete(
                    germanDetails.get(code.getCode()),
                    otherDetails.get(code.getCode())
                )
            );
        }
        return false;
    }
}
