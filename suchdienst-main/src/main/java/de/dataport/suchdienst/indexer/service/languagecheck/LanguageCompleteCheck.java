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

import org.springframework.util.StringUtils;

public interface LanguageCompleteCheck {
    default boolean stringFieldComplete(String german, String otherLanguage) {
        return (
            (StringUtils.hasText(german) && StringUtils.hasText(otherLanguage)) ||
            !StringUtils.hasText(german)
        );
    }
}
