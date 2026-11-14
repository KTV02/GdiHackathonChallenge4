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
package de.dataport.suchdienst.indexer.model;

import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Die statischen Methoden dieser Klasse waren früher im wesentlichen default-Methoden am Interface {@link I18nText}.
 */
@Slf4j
final class I18nTextHelper {

    /**
     * Hierher bewegt aus {@link CodelistEntry}.
     * Fragwürdiger Default.
     */
    private static final Locale DEFAULT = Locale.GERMAN;

    private I18nTextHelper() {}

    /**
     * Liefert den Text zu der locale. Dabei wird zuerst die locale inklusive country probiert, z.B. "en-US", und als Fallback
     * dann die erste locale, die wenigstens von der Sprache her passt, also z.B. "en" oder "en-GB".
     * Auch andersherum gibt es einen Fallback. Wenn z.B. mit "en" angefragt wird und wir haben nur "en-GB", so wird dieser Text geliefert.
     * Falls gar kein Text gefunden wird, liefert die Methode null.
     * <p>
     * Hierher bewegt aus {@link I18nText} im Zuge von PV-9536.
     *
     * @param texts      z.B. {@link CodelistEntry#getTexts()} oder {@link CodelistEntry#getComments()}
     * @param locale     Ziel-Locale aus dem Request
     * @param logHint    nur relevant, wenn logWarning == true
     */
    static String getBestMatch(Map<Locale, String> texts, Locale locale, String logHint) {
        if (texts.isEmpty()) {
            return null; // Performance und log-File-Hygiene.
        }
        String ret = texts.get(locale);
        if (ret != null) {
            return ret; // normaler Treffer. Das ist die Regel für Locale.GERMAN
        }
        ret = findTextByMoreSpecificLang(texts, locale);
        if (ret != null) {
            log.trace(
                "Fehlender Eintrag für Locale {} in {}. Verwende sprachgleiche als Ersatz.",
                logHint,
                locale
            );
            return ret;
        }
        return texts.get(DEFAULT);
    }

    /**
     * Sucht eine passende spezifischere "Sprache" (Bsp. en-GB), wenn die angefragte generische Sprache
     * ("en") nicht vorhanden ist.
     * <p>
     * Hierher bewegt aus {@link I18nText} im Zuge von PV-9536.
     *
     * @param locale eine locale, nie NULL
     * @return den Text fuer eine spezifischere Sprache oder NULL wenn keine passende vorhanden ist.
     */
    private static String findTextByMoreSpecificLang(Map<Locale, String> texts, Locale locale) {
        return texts
            .entrySet()
            .stream()
            .filter(e -> e.getKey().getLanguage().equals(locale.getLanguage()))
            .findAny()
            .map(Map.Entry::getValue)
            .orElse(null);
    }
}
