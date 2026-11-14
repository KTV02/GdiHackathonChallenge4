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
package de.dataport.suchdienst.suchen.service.legacy;

import de.dataport.suchdienst.suchen.model.helper.MustContain;
import de.dataport.suchdienst.suchen.model.mapper.ServiceDescriptionMapper;
import de.dataport.suchdienst.suchen.model.out.leistung.SdOutModelBase;
import de.dataport.suchdienst.suchen.model.out.leistung.SeDeBlock;
import de.dataport.suchdienst.suchen.model.out.leistung.SeDeBlockModule;
import de.dataport.suchdienst.util.ServiceModuleCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @deprecated Wird perspektivisch entfernt
 */
@Deprecated(forRemoval = true)
public class DetailModuleInjection {

    /**
     * Pflichtmodule, auf welche sich fachlich verständigt wurde (siehe PV-6230).
     * Die API muss immer diese Module gefüllt ausgeben. Notfalls mit Dummy-Werten.
     */
    static final List<ServiceModuleCode> MODULES_MANDATORY = List.of(
        ServiceModuleCode.FRIST,
        ServiceModuleCode.KOSTEN,
        ServiceModuleCode.VORAUSSETZUNGEN,
        ServiceModuleCode.ERFORDERLICHE_UNTERLAGEN,
        ServiceModuleCode.RECHTSGRUNDLAGEN,
        ServiceModuleCode.URHEBER,
        ServiceModuleCode.ZUSTAENDIGE_STELLE,
        ServiceModuleCode.ANSPRECHPUNKT
    );

    private DetailModuleInjection() {
        // Utility class
    }

    public static void injectServiceModules(SdOutModelBase sdOutModel, Locale locale) {
        final Locale languageToUse = selectLanguage(sdOutModel, locale);
        var details = new ArrayList<>(sdOutModel.getDetails());

        for (var code : ServiceModuleCode.values()) {
            boolean moduleExists = details
                .stream()
                .anyMatch(m -> m.getCode().equals(code.getCode()));

            if (!moduleExists && MODULES_MANDATORY.contains(code)) {
                details.add(createPlaceholderModule(code, languageToUse));
            }
        }

        sdOutModel.setDetails(details);
    }

    private static Locale selectLanguage(SdOutModelBase sdOutModel, Locale requestedLocale) {
        return sdOutModel.getAvailableLanguages().contains(requestedLocale.toLanguageTag())
            ? requestedLocale
            : Locale.GERMAN;
    }

    private static SeDeBlock createPlaceholderModule(ServiceModuleCode code, Locale locale) {
        MustContain mustContain = MustContain.create(code);
        SeDeBlock placeholderModule = new SeDeBlockModule(false, true);
        placeholderModule.setCode(code.getCode());
        placeholderModule.setTitle(mustContain.getTitle(locale));
        placeholderModule.setCategory(ServiceDescriptionMapper.INSTANCE.map(code.getCategory()));
        placeholderModule.setText(mustContain.getInformation(locale));
        return placeholderModule;
    }
}
