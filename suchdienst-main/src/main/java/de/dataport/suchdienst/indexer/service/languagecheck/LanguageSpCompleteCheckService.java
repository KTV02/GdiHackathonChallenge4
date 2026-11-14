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
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceBlockSpecialization;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SpezialisierungContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SpezialisierungVL;
import de.dataport.suchdienst.util.ServiceModuleCode;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LanguageSpCompleteCheckService extends LanguageModuleCompleteCheckService {

    public boolean checkLanguageComplete(
        Map<Locale, SpezialisierungContent> languageContent,
        SpezialisierungVL sp,
        Locale language
    ) {
        SpezialisierungContent content = languageContent.get(language);
        if (content != null) {
            Map<String, List<ServiceBlockSpecialization>> germanDetails = sp.getDetails();
            Map<String, List<ServiceBlockSpecialization>> otherDetails = content.getDetails();

            return Arrays.stream(ServiceModuleCode.values()).allMatch(code ->
                // Berücksichtigt kein TextmodulIndividuell, da die Values von ServiceModuleCode nicht deren Codes abbilden
                checkModulesComplete(
                    unwrapDetails(germanDetails.get(code.getCode())),
                    unwrapDetails(otherDetails.get(code.getCode()))
                )
            );
        }
        return false;
    }

    private List<AbstractServiceModule> unwrapDetails(List<ServiceBlockSpecialization> details) {
        if (details == null) {
            return List.of();
        }
        return details.stream().map(ServiceBlockSpecialization::serviceBlock).toList();
    }
}
