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

import de.dataport.suchdienst.indexer.model.AccessChannel;
import de.dataport.suchdienst.indexer.model.mapping.Link;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetail;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetailContent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.util.Pair;

public class AccessChannelModuleAggregator {

    private AccessChannelModuleAggregator() {
        // Util class
    }

    public static Map<String, List<AbstractServiceModule>> mergeBlocks(
        Map<String, List<AbstractServiceModule>> modulesByCode
    ) {
        return modulesByCode
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(Map.Entry::getKey, entry -> mergeBlocksByCode(entry.getValue()))
            );
    }

    static List<AbstractServiceModule> mergeBlocksByCode(List<AbstractServiceModule> modules) {
        if (modules == null || modules.isEmpty()) {
            return List.of();
        } else if (modules.size() == 1) {
            return modules;
        }

        var modulesByChannel = modules
            .stream()
            .map(module -> Pair.of(module.getAccessChannel(), module))
            .collect(
                Collectors.groupingBy(
                    Pair::getFirst,
                    Collectors.mapping(Pair::getSecond, Collectors.toList())
                )
            );

        var onlineOffline = modulesByChannel.getOrDefault(AccessChannel.ONLINE_OFFLINE, List.of());
        var online = modulesByChannel.getOrDefault(AccessChannel.ONLINE, List.of());
        var offline = modulesByChannel.getOrDefault(AccessChannel.OFFLINE, List.of());

        List<AbstractServiceModule> mappedBlocks = new ArrayList<>();

        mergeBlocksByZugangskanal(onlineOffline).ifPresent(mappedBlocks::add);
        mergeBlocksByZugangskanal(online).ifPresent(mappedBlocks::add);
        mergeBlocksByZugangskanal(offline).ifPresent(mappedBlocks::add);

        return mappedBlocks;
    }

    private static Optional<AbstractServiceModule> mergeBlocksByZugangskanal(
        List<AbstractServiceModule> modules
    ) {
        if (modules == null || modules.isEmpty()) {
            return Optional.empty();
        }

        AbstractServiceModule resultBlock = modules.getFirst();

        StringBuilder mergedText = new StringBuilder();
        List<Link> mergedLinks = new ArrayList<>();
        Collection<ServiceModuleDetailContent> mergedDetails = new ArrayList<>();

        for (AbstractServiceModule module : modules) {
            var text = module.getText();
            if (text != null && !text.isEmpty()) {
                if (!mergedText.isEmpty()) {
                    // TODO In Absprache mit Konsumenten/Frontend muss ein besserer Trenner gefunden werden.
                    mergedText.append("<br>");
                }
                mergedText.append(text);
            }

            if (module instanceof ServiceModuleDetail detailModule) {
                mergedDetails.addAll(detailModule.getDetails());
                mergedLinks.addAll(detailModule.getLinks());
            } else if (module instanceof ServiceModule detailModule) {
                mergedLinks.addAll(detailModule.getLinks());
            }
        }

        resultBlock.setText(mergedText.toString());

        if (resultBlock instanceof ServiceModuleDetail detailBlock) {
            detailBlock.getDetails().clear();
            detailBlock.getDetails().addAll(mergedDetails);
            detailBlock.getLinks().clear();
            detailBlock.getLinks().addAll(mergedLinks);
        } else if (resultBlock instanceof ServiceModule block) {
            block.getLinks().clear();
            block.getLinks().addAll(mergedLinks);
        }

        return Optional.of(resultBlock);
    }
}
