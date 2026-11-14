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
package de.dataport.suchdienst.config;

import static de.dataport.suchdienst.util.LocaleUtils.DEFAULT_LOCALE;

import de.dataport.suchdienst.util.LocaleUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
@Slf4j
public class PvogRequestContext {

    @Getter
    private Locale locale = Locale.GERMAN;

    public void setLocaleFromRequestHeader(HttpServletRequest request) {
        String languageHeaderValue = request.getHeader("Accept-Language");
        try {
            this.locale = LocaleUtils.getSupportedLocale(languageHeaderValue);
        } catch (RuntimeException _) {
            log.info(
                "Ungültiger Accept-Language Header '{}'. Verwende stattdessen den Default '{}'",
                languageHeaderValue,
                DEFAULT_LOCALE
            );
            this.locale = DEFAULT_LOCALE;
        }
    }
}
