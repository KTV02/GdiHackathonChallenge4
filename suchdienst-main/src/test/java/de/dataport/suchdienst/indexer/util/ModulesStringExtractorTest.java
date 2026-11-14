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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dataport.suchdienst.indexer.model.AccessChannel;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModule;
import de.dataport.suchdienst.util.ServiceModuleCode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ModulesStringExtractorTest {

    @Test
    void findTextTest() {
        Map<String, List<AbstractServiceModule>> details = Map.of(
            ServiceModuleCode.LEISTUNGSNAME.getCode(),
            List.of(createModule("name", AccessChannel.ONLINE_OFFLINE)),
            ServiceModuleCode.LEISTUNGSBEZEICHNUNG.getCode(),
            List.of(createModule("bezeichnung", AccessChannel.ONLINE_OFFLINE))
        );

        Optional<String> result = ModulesStringExtractor.findFirstTextByModuleAndChannelPriority(
            details,
            ServiceModuleCode.LEISTUNGSBEZEICHNUNG
        );

        assertTrue(result.isPresent());
        assertEquals("bezeichnung", result.get());
    }

    @Test
    void accessChannelPriorityTest() {
        Map<String, List<AbstractServiceModule>> details = Map.of(
            ServiceModuleCode.LEISTUNGSNAME.getCode(),
            List.of(
                createModule("name_offline", AccessChannel.OFFLINE),
                createModule("name_online", AccessChannel.ONLINE)
            ),
            ServiceModuleCode.LEISTUNGSBEZEICHNUNG.getCode(),
            List.of(
                createModule("bezeichnung_online", AccessChannel.ONLINE),
                createModule("bezeichnung_offline", AccessChannel.OFFLINE)
            )
        );

        Optional<String> result = ModulesStringExtractor.findFirstTextByModuleAndChannelPriority(
            details,
            ServiceModuleCode.LEISTUNGSNAME
        );

        assertTrue(result.isPresent());
        assertEquals("name_online", result.get());
    }

    @Test
    void codePriorityTest() {
        Map<String, List<AbstractServiceModule>> details = Map.of(
            ServiceModuleCode.LEISTUNGSNAME.getCode(),
            List.of(createModule("name", AccessChannel.ONLINE_OFFLINE)),
            ServiceModuleCode.DOKUMENT.getCode(),
            List.of(createModule("dokument", AccessChannel.ONLINE_OFFLINE)),
            ServiceModuleCode.KOSTEN.getCode(),
            List.of(createModule("kosten", AccessChannel.ONLINE_OFFLINE))
        );

        Optional<String> result = ModulesStringExtractor.findFirstTextByModuleAndChannelPriority(
            details,
            ServiceModuleCode.LEISTUNGSBEZEICHNUNG,
            ServiceModuleCode.DOKUMENT
        );

        assertTrue(result.isPresent());
        assertEquals("dokument", result.get());
    }

    @Test
    void extractStringFieldTest() {
        Map<String, List<AbstractServiceModule>> details = Map.of(
            ServiceModuleCode.LEISTUNGSNAME.getCode(),
            List.of(
                createModule("name_offline", AccessChannel.OFFLINE),
                createModule("name_online", AccessChannel.ONLINE)
            ),
            ServiceModuleCode.LEISTUNGSBEZEICHNUNG.getCode(),
            List.of(
                createModule("bezeichnung_online", AccessChannel.ONLINE),
                createModule("bezeichnung_offline", AccessChannel.OFFLINE)
            )
        );

        List<String> result = ModulesStringExtractor.extractStringField(
            details,
            ServiceModuleCode.LEISTUNGSBEZEICHNUNG,
            AbstractServiceModule::getText
        ).toList();

        assertThat(result, is(List.of("bezeichnung_online", "bezeichnung_offline")));
    }

    ServiceModule createModule(String text, AccessChannel accessChannel) {
        return ServiceModule.builder()
            .title("Title of " + text)
            .text(text)
            .accessChannel(accessChannel)
            .build();
    }
}
