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
import de.dataport.suchdienst.suchen.model.out.leistung.SdOutModelBase;
import de.dataport.suchdienst.suchen.model.out.leistung.detail.AbstractServiceModule;
import de.dataport.suchdienst.util.ServiceModuleCode;
import java.util.Locale;
import java.util.Optional;

public class DetailModuleTitleRenamer {

    private DetailModuleTitleRenamer() {
        // Util class
    }

    public static AbstractServiceModule renameModuleTitle(
        AbstractServiceModule module,
        Locale locale
    ) {
        ServiceModuleCode.fromCode(module.getCode())
            .flatMap(code -> getModuleTitle(code, locale))
            .ifPresent(module::setTitle);
        return module;
    }

    public static void renameModuleTitles(SdOutModelBase sdOutModelBase, Locale locale) {
        var langToUse = selectLanguage(sdOutModelBase, locale);
        sdOutModelBase.getDetails().forEach(module ->
            ServiceModuleCode.fromCode(module.getCode())
                .flatMap(code -> getModuleTitle(code, langToUse))
                .ifPresent(module::setTitle)
        );
    }

    static Optional<String> getModuleTitle(ServiceModuleCode code, Locale locale) {
        return Optional.ofNullable(MustContain.create(code).getTitle(locale));
    }

    private static Locale selectLanguage(SdOutModelBase sdOutModel, Locale requestedLocale) {
        return sdOutModel.getAvailableLanguages().contains(requestedLocale.toLanguageTag())
            ? requestedLocale
            : Locale.GERMAN;
    }
}
