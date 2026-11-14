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

import static java.util.Comparator.comparing;

import de.dataport.suchdienst.suchen.model.out.leistung.AccessChannel;
import de.dataport.suchdienst.suchen.model.out.leistung.SdOutModelBase;
import de.dataport.suchdienst.suchen.model.out.leistung.SeDeBlock;
import de.dataport.suchdienst.suchen.model.out.leistung.detail.AbstractServiceModule;
import de.dataport.suchdienst.suchen.model.out.leistung.detail.ServiceModule;
import de.dataport.suchdienst.suchen.model.out.leistung.detail.ServiceModuleDetail;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DetailSorting {

    private DetailSorting() {
        // Util class
    }

    /**
     * Ordnung von Modulen zum Rendering der Detail-Seite.
     * Die Codes werden an anderen Stellen auch als "LeikaCode" beschrieben
     * Factored out from fillSdOutModelBase
     */
    private static final Map<String, Integer> MODULE_SORT_ORDER_MAP;

    static {
        final List<String> moduleSortOrder = List.of(
            "24",
            "23",
            "06",
            "09",
            "08",
            "14",
            "98",
            "97",
            "16",
            "15",
            "07",
            "19",
            "18",
            "22"
        );

        MODULE_SORT_ORDER_MAP = IntStream.range(0, moduleSortOrder.size())
            .boxed()
            .collect(Collectors.toMap(moduleSortOrder::get, Function.identity()));
    }

    public static Comparator<AbstractServiceModule> getModuleComparatorV6() {
        return comparing((AbstractServiceModule sb) ->
            // TODO: Hier gibt es noch einen Konflikt mit TextmodulIndividuell-Codes, falls dort derselbe Code wie für ein sortiertes Textmodul verwendet wird.
            getModuleSortOrdering(sb.getCode())
        ).thenComparing(DetailSorting::getChannelOrdering);
    }

    /**
     * @deprecated Wird perspektivisch entfernt
     */
    @Deprecated(forRemoval = true)
    public static void sortModules(SdOutModelBase sdOutModel) {
        sdOutModel.getDetails().sort(comparing(DetailSorting::getModuleSortOrder));
    }

    private static int getModuleSortOrder(SeDeBlock seDeBlockOut) {
        return getModuleSortOrdering(seDeBlockOut.getCode());
    }

    private static int getModuleSortOrdering(String code) {
        if (code == null) {
            // Module ohne Code kommen ans Ende
            return Integer.MAX_VALUE;
        }

        Integer moduleOrder = MODULE_SORT_ORDER_MAP.get(code);
        if (moduleOrder != null) {
            // Modul mit festgelegter Reihenfolge
            return moduleOrder;
        } else {
            try {
                // Nicht festgelegte Module werden hinter den festgelegten Modulen nach ihrem Code sortiert
                return MODULE_SORT_ORDER_MAP.size() + Integer.parseInt(code);
            } catch (NumberFormatException _) {
                // Module ohne numerischen Code kommen ans Ende
                return Integer.MAX_VALUE;
            }
        }
    }

    private static int getChannelOrdering(AbstractServiceModule block) {
        return switch (block) {
            case ServiceModule module -> channelPriority(module.getAccessChannel());
            case ServiceModuleDetail moduleDetail -> channelPriority(
                moduleDetail.getAccessChannel()
            );
            default -> Integer.MAX_VALUE;
        };
    }

    private static int channelPriority(AccessChannel channel) {
        // kein default → Compiler warnt, wenn ein neuer Channel hinzukommt und hier nicht berücksichtigt wird
        return switch (channel) {
            case ONLINE -> 0;
            case ONLINE_OFFLINE -> 1;
            case OFFLINE -> 2;
        };
    }
}
