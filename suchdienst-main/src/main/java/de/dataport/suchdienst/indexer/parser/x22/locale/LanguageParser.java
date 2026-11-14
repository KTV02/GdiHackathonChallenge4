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

import de.dataport.suchdienst.indexer.parser.XzufiParserUtils;
import de.xoev.xzufi.v2_2_0.StringLocalized;
import java.util.Locale;
import java.util.function.Predicate;

public abstract class LanguageParser {

    protected Predicate<StringLocalized> stringLangFilter(Locale locale) {
        return s ->
            XzufiParserUtils.getSupportedLocale(s.getLanguageCode())
                .getLanguage()
                .equals(locale.getLanguage());
    }
}
