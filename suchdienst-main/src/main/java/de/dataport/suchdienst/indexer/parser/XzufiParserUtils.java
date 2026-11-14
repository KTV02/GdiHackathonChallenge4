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
package de.dataport.suchdienst.indexer.parser;

import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstParameter;
import de.dataport.suchdienst.util.Bundeslaender;
import de.dataport.suchdienst.util.LocaleUtils;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class XzufiParserUtils {

    public static final Set<String> ALL_FEDERAL_STATES = Set.of(
        "01",
        "02",
        "03",
        "04",
        "05",
        "06",
        "07",
        "08",
        "09",
        "10",
        "11",
        "12",
        "13",
        "14",
        "15",
        "16"
    );

    private XzufiParserUtils() {}

    public static Set<String> arsToUse(Set<String> arsFederalState) {
        if (arsFederalState.contains(Bundeslaender.BUND.getArsFederalstate())) {
            return ALL_FEDERAL_STATES;
        } else {
            return arsFederalState;
        }
    }

    public static Set<String> arsToUse(String arsFederalState) {
        if (arsFederalState.equals(Bundeslaender.BUND.getArsFederalstate())) {
            return ALL_FEDERAL_STATES;
        } else {
            return Set.of(arsFederalState);
        }
    }

    /**
     * Liefert die unterstützte Sprache (bzw. als Fallback GERMAN) anhand der übergebenen String-Repräsentation.
     */
    public static Locale getSupportedLocale(String localeRepresentation) {
        try {
            return LocaleUtils.getSupportedLocale(localeRepresentation);
        } catch (RuntimeException _) {
            log.info(
                "Ungültige Sprache '{}' in XZuFi-Daten. Verwende stattdessen den Default '{}'",
                localeRepresentation,
                LocaleUtils.DEFAULT_LOCALE
            );
            return LocaleUtils.DEFAULT_LOCALE;
        }
    }

    public static <T> void addLocalizedCustomParameters(
        Map<Locale, OnlinedienstContent> contents,
        List<T> parameters,
        Function<T, List<String>> languageCodes,
        Function<T, OnlinedienstParameter> parameterMapper
    ) {
        addLocalizedParameters(
            contents,
            parameters,
            languageCodes,
            parameterMapper,
            (content, parameter) -> content.getParametersCustom().add(parameter)
        );
    }

    public static <T> void addLocalizedEfaParameters(
        Map<Locale, OnlinedienstContent> contents,
        List<T> parameters,
        Function<T, List<String>> languageCodes,
        Function<T, OnlinedienstParameter> parameterMapper
    ) {
        addLocalizedParameters(
            contents,
            parameters,
            languageCodes,
            parameterMapper,
            (content, parameter) -> content.getParametersEfa().add(parameter)
        );
    }

    private static <T> void addLocalizedParameters(
        Map<Locale, OnlinedienstContent> contents,
        List<T> parameters,
        Function<T, List<String>> languageCodes,
        Function<T, OnlinedienstParameter> parameterMapper,
        BiConsumer<OnlinedienstContent, OnlinedienstParameter> parameterConsumer
    ) {
        parameters.forEach(parameter -> {
            Set<Locale> parameterLanguages = languageCodes
                .apply(parameter)
                .stream()
                .map(XzufiParserUtils::getSupportedLocale)
                .collect(Collectors.toSet());
            contents.forEach((locale, content) -> {
                if (parameterLanguages.isEmpty() || parameterLanguages.contains(locale)) {
                    parameterConsumer.accept(content, parameterMapper.apply(parameter));
                }
            });
        });
    }
}
