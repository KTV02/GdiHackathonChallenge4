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
package de.dataport.suchdienst.util;

import java.util.List;
import java.util.Locale;
import org.springframework.util.ObjectUtils;

/**
 * Definitionen und Methoden für das Handling von Locales.
 */
public final class LocaleUtils {

    /**
     * Alle vom SuD unterstützen Locales.
     */
    public static final List<Locale> SUPPORTED_LANGUAGES = List.of(Locale.GERMAN, Locale.ENGLISH);

    /**
     * Fallback-Locale für Situationen, in denen die Locale nicht ermittelt werden kann.
     */
    public static final Locale DEFAULT_LOCALE = Locale.GERMAN;

    private LocaleUtils() {}

    /**
     * Liefert die unterstützte Sprache (bzw. als Fallback GERMAN) anhand der übergebenen String-Repräsentation.
     * @throws RuntimeException kann auftreten, wenn das Parsen der Locale nicht möglich ist.
     * Diese Exception-Fälle müssen je nach Situation unterschiedlich von den Aufrufern behandelt und geloggt werden.
     */
    public static Locale getSupportedLocale(String localeRepresentation) {
        if (ObjectUtils.isEmpty(localeRepresentation)) {
            return DEFAULT_LOCALE;
        }

        List<Locale.LanguageRange> list = Locale.LanguageRange.parse(localeRepresentation);
        Locale match = Locale.lookup(list, SUPPORTED_LANGUAGES);

        if (match != null) {
            return match;
        }

        return DEFAULT_LOCALE;
    }
}
