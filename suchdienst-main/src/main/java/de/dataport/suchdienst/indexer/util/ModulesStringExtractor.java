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
package de.dataport.suchdienst.indexer.util;

import de.dataport.suchdienst.indexer.model.AccessChannel;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceBlockSpecialization;
import de.dataport.suchdienst.util.ServiceModuleCode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.springframework.util.StringUtils;

/**
 * Utility-Klasse zum Extrahieren von String-Werten aus Service-Modulen.
 * <p>
 * Diese Klasse bietet statische Methoden zum Auffinden und Extrahieren von
 * Textinhalten aus Verwaltungsleistungsmodulen basierend auf verschiedenen
 * Kriterien wie Modultyp und Zugangskanal-Prioritäten.
 */
public final class ModulesStringExtractor {

    private ModulesStringExtractor() {
        // Private constructor to prevent instantiation
    }

    /**
     * Findet den Namen einer Leistung anhand der Leistungsbezeichnung.
     *
     * @param details Map mit Service-Modulen, gruppiert nach ServiceModuleCode
     * @return Optional mit dem gefundenen Namen oder leer, falls kein Name gefunden
     *         wurde
     */
    public static Optional<String> findName(Map<String, List<AbstractServiceModule>> details) {
        return findFirstTextByModuleAndChannelPriority(
            details,
            ServiceModuleCode.LEISTUNGSBEZEICHNUNG
        );
    }

    /**
     * Findet eine alternative Beschreibung für den Leistungsnamen.
     *
     * @param details Map mit Service-Modulen, gruppiert nach ServiceModuleCode
     * @return Optional mit der alternativen Beschreibung oder leer, falls keine
     *         gefunden wurde
     */
    public static Optional<String> findNameOtherDescription(
        Map<String, List<AbstractServiceModule>> details
    ) {
        return findFirstTextByModuleAndChannelPriority(details, ServiceModuleCode.LEISTUNGSNAME);
    }

    /**
     * Findet entweder den alternativen Leistungsnamen oder den regulären Namen.
     * Priorisiert den alternativen Namen (LEISTUNGSNAME) vor der
     * Standardbezeichnung.
     *
     * @param details Map mit Service-Modulen, gruppiert nach ServiceModuleCode
     * @return Optional mit dem gefundenen Namen oder leer, falls kein Name gefunden
     *         wurde
     */
    public static Optional<String> findNameOtherOrName(
        Map<String, List<AbstractServiceModule>> details
    ) {
        return findFirstTextByModuleAndChannelPriority(
            details,
            ServiceModuleCode.LEISTUNGSNAME,
            ServiceModuleCode.LEISTUNGSBEZEICHNUNG
        );
    }

    /**
     * Extrahiert String-Felder aus Service-Modulen basierend auf einem spezifischen
     * Code.
     *
     * @param details    Map mit Service-Modulen, gruppiert nach ServiceModuleCode
     * @param code       Der ServiceModuleCode, nach dem gefiltert werden soll
     * @param propGetter Funktion zum Extrahieren der gewünschten Eigenschaft
     * @return Stream von String-Werten, die den Kriterien entsprechen
     */
    public static Stream<String> extractStringField(
        Map<String, List<AbstractServiceModule>> details,
        ServiceModuleCode code,
        Function<AbstractServiceModule, String> propGetter
    ) {
        return extractStringFieldGeneric(details, code, Function.identity(), propGetter);
    }

    /**
     * Extrahiert String-Felder aus ServiceBlockSpecialization-Objekten.
     *
     * @param details    Map mit ServiceBlockSpecialization-Objekten, gruppiert nach
     *                   ServiceModuleCode
     * @param code       Der ServiceModuleCode, nach dem gefiltert werden soll
     * @param propGetter Funktion zum Extrahieren der gewünschten Eigenschaft
     * @return Stream von String-Werten, die den Kriterien entsprechen
     */
    public static Stream<String> extractStringFieldSp(
        Map<String, List<ServiceBlockSpecialization>> details,
        ServiceModuleCode code,
        Function<AbstractServiceModule, String> propGetter
    ) {
        return extractStringFieldGeneric(
            details,
            code,
            ServiceBlockSpecialization::serviceBlock,
            propGetter
        );
    }

    /**
     * Generische Methode zum Extrahieren von String-Feldern aus verschiedenen
     * Modultypen.
     *
     * @param <T>           Der Typ der Elemente in der Details-Map
     * @param details       Map mit Modulen, gruppiert nach ServiceModuleCode
     * @param code          Der ServiceModuleCode, nach dem gefiltert werden soll
     * @param elementMapper Funktion zur Umwandlung des Elements in ein
     *                      AbstractServiceModule
     * @param propGetter    Funktion zum Extrahieren der gewünschten Eigenschaft
     * @return Stream von String-Werten, die den Kriterien entsprechen
     */
    private static <T> Stream<String> extractStringFieldGeneric(
        Map<String, List<T>> details,
        ServiceModuleCode code,
        Function<T, AbstractServiceModule> elementMapper,
        Function<AbstractServiceModule, String> propGetter
    ) {
        if (details == null) {
            return Stream.empty();
        }

        return details
            .getOrDefault(code.getCode(), List.of())
            .stream()
            .map(elementMapper)
            .map(propGetter)
            .filter(StringUtils::hasText);
    }

    /**
     * Extrahiert den Text aus einer Liste von AbstractServiceModule-Objekten
     * basierend auf einer
     * Prioritätenreihenfolge von ServiceModuleCode und AccessChannel.
     * Es wird erst nach dem ServiceModuleCode mit der höchsten Priorität über alle
     * AccessChannel hinweg gesucht,
     * bevor der nächste ServiceModuleCode über alle AccessChannel hinweg durchsucht
     * wird.
     *
     * @param details         Eine EnumMap, die ServiceModuleCode-Werte den
     *                        zugehörigen Listen von AbstractServiceModule zuordnet.
     * @param codesInPriority Eine Liste von ServiceModuleCode-Werten, die in der
     *                        angegebenen Reihenfolge durchsucht werden.
     * @return Ein Optional, das den ersten gefundenen Text enthält, der den
     *         Kriterien entspricht,
     *         oder ein leeres Optional, wenn kein passender Text gefunden wurde.
     */
    public static Optional<String> findFirstTextByModuleAndChannelPriority(
        Map<String, List<AbstractServiceModule>> details,
        ServiceModuleCode... codesInPriority
    ) {
        if (details == null || details.isEmpty()) {
            return Optional.empty();
        }
        return Stream.of(codesInPriority)
            .map(code -> details.get(code.getCode()))
            .filter(modules -> modules != null && !modules.isEmpty())
            .flatMap(List::stream)
            .filter(m -> StringUtils.hasText(m.getText()))
            // Selects the module with the lowest channel priority value (highest priority)
            .min(CHANNEL_COMPARATOR)
            .map(AbstractServiceModule::getText);
    }

    /**
     * Comparator zum Vergleichen von Service-Modulen basierend auf ihrer
     * Zugangskanal-Priorität.
     * Module mit niedrigeren Prioritätswerten (höhere Priorität) werden bevorzugt.
     */
    private static final Comparator<AbstractServiceModule> CHANNEL_COMPARATOR =
        Comparator.comparingInt(m -> channelPriority(m.getAccessChannel()));

    /**
     * Bestimmt die Priorität eines Zugangskanals.
     * Niedrigere Werte bedeuten höhere Priorität.
     *
     * @param channel Der Zugangskanal, dessen Priorität bestimmt werden soll
     * @return Prioritätswert (0 = höchste Priorität)
     */
    private static int channelPriority(AccessChannel channel) {
        return switch (channel) {
            case ONLINE -> 0;
            case ONLINE_OFFLINE -> 1;
            case OFFLINE -> 2;
            case null -> Integer.MAX_VALUE;
        };
    }
}
