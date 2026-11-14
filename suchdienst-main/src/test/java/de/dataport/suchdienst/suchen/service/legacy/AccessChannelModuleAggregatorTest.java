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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import de.dataport.suchdienst.indexer.model.AccessChannel;
import de.dataport.suchdienst.indexer.model.mapping.Link;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetail;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetailContent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccessChannelModuleAggregatorTest {

    @Test
    @DisplayName("mergeBlocks gibt leere Map für leere Eingabe zurück")
    void testMergeBlocksEmptyInput() {
        Map<String, List<AbstractServiceModule>> result = AccessChannelModuleAggregator.mergeBlocks(
            Map.of()
        );
        assertThat(result, is(anEmptyMap()));
    }

    @Test
    @DisplayName("mergeBlocks gibt einzelnes Modul für Einzel-Eintrag zurück")
    void testMergeBlocksSingleModule() {
        ServiceModule module = ServiceModule.builder().text("foo").build();
        Map<String, List<AbstractServiceModule>> input = Map.of("02", List.of(module));
        Map<String, List<AbstractServiceModule>> result = AccessChannelModuleAggregator.mergeBlocks(
            input
        );
        assertThat(result.keySet(), contains("02"));
        assertThat(result.get("02"), contains(module));
    }

    @Test
    @DisplayName("mergeBlocks gibt Module unverändert zurück, wenn kein Merging nötig ist")
    void testMergeBlocksByAccessChannel() {
        ServiceModule online = ServiceModule.builder()
            .text("online")
            .accessChannel(AccessChannel.ONLINE)
            .build();
        ServiceModule offline = ServiceModule.builder()
            .text("offline")
            .accessChannel(AccessChannel.OFFLINE)
            .build();
        ServiceModule onlineOffline = ServiceModule.builder()
            .text("onlineOffline")
            .accessChannel(AccessChannel.ONLINE_OFFLINE)
            .build();
        Map<String, List<AbstractServiceModule>> input = Map.of(
            "02",
            List.of(online, offline, onlineOffline)
        );
        Map<String, List<AbstractServiceModule>> result = AccessChannelModuleAggregator.mergeBlocks(
            input
        );
        assertThat(result.get("02"), hasSize(3));
        Set<String> texts = result
            .get("02")
            .stream()
            .map(AbstractServiceModule::getText)
            .collect(java.util.stream.Collectors.toSet());
        assertThat(texts, containsInAnyOrder("online", "offline", "onlineOffline"));
    }

    @Test
    @DisplayName("mergeBlocksByCode gibt Modul mit Standard-Zugangskanal zurück")
    void testMergeBlocksByCodeDefaultsToOnlineOffline() {
        ServiceModule module = ServiceModule.builder().text("foo").build();
        var merged = AccessChannelModuleAggregator.mergeBlocksByCode(List.of(module));
        assertThat(merged, hasSize(1));
        assertThat(
            ((ServiceModule) merged.get(0)).getAccessChannel(),
            is(AccessChannel.ONLINE_OFFLINE)
        );
        assertThat(merged.get(0).getText(), containsString("foo"));
    }

    @Test
    @DisplayName("mergeBlocksByCode fasst mehrere Module mit gleichem Kanal zusammen")
    void testMergeBlocksByCodeMergesText() {
        ServiceModule online1 = ServiceModule.builder()
            .text("foo")
            .accessChannel(AccessChannel.ONLINE)
            .build();
        ServiceModule online2 = ServiceModule.builder()
            .text("bar")
            .accessChannel(AccessChannel.ONLINE)
            .build();
        ServiceModule offline = ServiceModule.builder()
            .text("offline")
            .accessChannel(AccessChannel.OFFLINE)
            .build();
        var merged = AccessChannelModuleAggregator.mergeBlocksByCode(
            List.of(online1, online2, offline)
        );
        assertThat(merged, hasSize(2));
        assertThat(((ServiceModule) merged.get(0)).getAccessChannel(), is(AccessChannel.ONLINE));
        assertThat(merged.get(0).getText(), containsString("foo"));
        assertThat(merged.get(0).getText(), containsString("bar"));
        assertThat(merged.get(0).getText(), containsString("<br>"));
        assertThat(((ServiceModule) merged.get(1)).getAccessChannel(), is(AccessChannel.OFFLINE));
        assertThat(merged.get(1).getText(), containsString("offline"));
    }

    @Test
    @DisplayName("mergeBlocksByCode gibt leere Liste für null oder leere Eingabe zurück")
    void testMergeBlocksByCodeEmpty() {
        assertThat(AccessChannelModuleAggregator.mergeBlocksByCode(null), is(empty()));
        assertThat(AccessChannelModuleAggregator.mergeBlocksByCode(List.of()), is(empty()));
    }

    @Test
    @DisplayName("mergeBlocksByCode gibt gleiche Liste für einzelnes Modul zurück")
    void testMergeBlocksByCodeSingle() {
        ServiceModuleDetail module = ServiceModuleDetail.builder().text("foo").build();
        List<AbstractServiceModule> result = AccessChannelModuleAggregator.mergeBlocksByCode(
            List.of(module)
        );
        assertThat(result, contains(module));
    }

    @Test
    @DisplayName("mergeBlocksByZugangskanal fasst Details und Links zusammen")
    void testMergeBlocksByZugangskanalDetailsAndLinks() {
        Link link1 = new Link("url1", null, null);
        Link link2 = new Link("url2", null, null);
        ServiceModuleDetailContent detail1 = ServiceModuleDetailContent.builder()
            .text("d1")
            .build();
        ServiceModuleDetailContent detail2 = ServiceModuleDetailContent.builder()
            .text("d2")
            .build();
        ServiceModuleDetail m1 = ServiceModuleDetail.builder()
            .text("foo")
            .links(
                new ArrayList<>() {
                    {
                        add(link1);
                    }
                }
            )
            .details(
                new ArrayList<>() {
                    {
                        add(detail1);
                    }
                }
            )
            .build();
        ServiceModuleDetail m2 = ServiceModuleDetail.builder()
            .text("bar")
            .links(
                new ArrayList<>() {
                    {
                        add(link2);
                    }
                }
            )
            .details(
                new ArrayList<>() {
                    {
                        add(detail2);
                    }
                }
            )
            .build();
        List<AbstractServiceModule> merged = AccessChannelModuleAggregator.mergeBlocksByCode(
            List.of(m1, m2)
        );
        assertThat(merged, hasSize(1));
        var result = (ServiceModuleDetail) merged.get(0);
        assertThat(result.getAccessChannel(), is(AccessChannel.ONLINE_OFFLINE));
        assertThat(result.getLinks(), containsInAnyOrder(link1, link2));
        assertThat(result.getDetails(), containsInAnyOrder(detail1, detail2));
        assertThat(result.getText(), containsString("foo"));
        assertThat(result.getText(), containsString("bar"));
    }
}
