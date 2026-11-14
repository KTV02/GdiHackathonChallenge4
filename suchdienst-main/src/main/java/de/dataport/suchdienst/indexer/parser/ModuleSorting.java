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

import static java.util.Comparator.comparing;

import de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceBlockSpecialization;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ModuleSorting {

    private static final Map<String, Integer> MODULE_SORT_ORDER_MAP;

    static {
        final List<String> moduleSortOrder = List.of(
            "02",
            "03",
            "24",
            "06",
            "07",
            "08",
            "09",
            "11",
            "14",
            "15",
            "16",
            "18",
            "22",
            "23",
            "25",
            "94",
            "95",
            "96",
            "97",
            "98"
        );

        MODULE_SORT_ORDER_MAP = IntStream.range(0, moduleSortOrder.size())
            .boxed()
            .collect(Collectors.toMap(moduleSortOrder::get, Function.identity()));
    }

    private ModuleSorting() {
        // Utility class
    }

    public static Comparator<Map.Entry<String, AbstractServiceModule>> getModuleComparator() {
        return comparing(entry -> getModuleSortOrdering(entry.getKey()));
    }

    public static Comparator<
        Map.Entry<String, ServiceBlockSpecialization>
    > getModuleComparatorSp() {
        return comparing(entry -> getModuleSortOrdering(entry.getKey()));
    }

    private static int getModuleSortOrdering(String code) {
        if (code == null) {
            // Module ohne Code kommen ans Ende
            return Integer.MAX_VALUE;
        }
        return MODULE_SORT_ORDER_MAP.getOrDefault(code, Integer.MAX_VALUE);
    }
}
