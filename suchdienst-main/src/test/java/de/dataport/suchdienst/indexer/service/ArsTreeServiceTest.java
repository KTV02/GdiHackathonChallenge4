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
package de.dataport.suchdienst.indexer.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.geo.service.CodelistDownloader;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

@ExtendWith(MockitoExtension.class)
class ArsTreeServiceTest {

    private static final Map<String, String> AKTUELLE_ARS = Map.of(
        "010010000000",
        "Flensburg",
        "010020000000",
        "Kiel"
    );
    private static final Map<String, String> ALTE_SEED_ARS = Map.of("020000000000", "Hamburg");

    @Mock
    private CodelistDownloader codelistDownloader;

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private SuchdienstProperties properties;

    private ArsTreeService arsTreeService;

    @BeforeEach
    void setup() {
        arsTreeService = new ArsTreeService(codelistDownloader, taskScheduler, properties, false);
    }

    @Test
    void getArsTree_shouldRefreshWhenNoArsTreeExists() {
        when(codelistDownloader.updateDestatisArsCodelistCaches()).thenReturn(AKTUELLE_ARS);

        assertThat(
            arsTreeService.getArsTree().resolveToGemeinden("010000000000"),
            containsInAnyOrder("010010000000", "010020000000")
        );

        verify(codelistDownloader).updateDestatisArsCodelistCaches();
    }

    @Test
    void getArsTree_shouldThrowWhenNoArsListIsAvailable() {
        when(codelistDownloader.updateDestatisArsCodelistCaches()).thenReturn(Map.of());
        when(codelistDownloader.getCachedReducedDestatisArsCodelist()).thenReturn(Map.of());
        when(codelistDownloader.loadReducedArsSeedIntoCache()).thenReturn(Map.of());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            arsTreeService.getArsTree()
        );

        assertThat(exception.getMessage(), is("ARS-Baum konnte nicht initialisiert werden"));
    }

    @Test
    void refreshArsTree_shouldKeepExistingArsTreeWhenDownloadFails() {
        when(codelistDownloader.updateDestatisArsCodelistCaches())
            .thenReturn(AKTUELLE_ARS)
            .thenReturn(Map.of());
        lenient()
            .when(codelistDownloader.getCachedReducedDestatisArsCodelist())
            .thenReturn(Map.of());
        lenient().when(codelistDownloader.loadReducedArsSeedIntoCache()).thenReturn(ALTE_SEED_ARS);

        arsTreeService.refreshArsTree();
        arsTreeService.refreshArsTree(); // Der zweite Download schlägt fehl; der zuvor geladene Baum muss erhalten bleiben.

        assertThat(
            arsTreeService.getArsTree().resolveToGemeinden("010000000000"),
            containsInAnyOrder("010010000000", "010020000000")
        );
        verify(codelistDownloader, never()).getCachedReducedDestatisArsCodelist();
        verify(codelistDownloader, never()).loadReducedArsSeedIntoCache();
    }

    @Test
    void refreshArsTree_shouldUseFallbackWhenNoArsTreeExists() {
        when(codelistDownloader.updateDestatisArsCodelistCaches()).thenReturn(Map.of());
        when(codelistDownloader.getCachedReducedDestatisArsCodelist()).thenReturn(Map.of());
        when(codelistDownloader.loadReducedArsSeedIntoCache()).thenReturn(ALTE_SEED_ARS);

        arsTreeService.refreshArsTree();

        assertThat(
            arsTreeService.getArsTree().resolveToGemeinden("020000000000"),
            containsInAnyOrder("020000000000")
        );
    }

    @Test
    void refreshArsTree_shouldUseFallbackWhenDownloadThrowsException() {
        when(codelistDownloader.updateDestatisArsCodelistCaches()).thenThrow(
            new IllegalStateException("Codeliste ist leer")
        );
        when(codelistDownloader.getCachedReducedDestatisArsCodelist()).thenReturn(AKTUELLE_ARS);

        arsTreeService.refreshArsTree();

        assertThat(
            arsTreeService.getArsTree().resolveToGemeinden("010000000000"),
            containsInAnyOrder("010010000000", "010020000000")
        );
    }

    @Test
    void refreshArsTree_shouldUseSeedOnlyWithoutDownloadOrCacheLookup() {
        arsTreeService = new ArsTreeService(codelistDownloader, taskScheduler, properties, true);
        when(codelistDownloader.loadReducedArsSeedIntoCache()).thenReturn(ALTE_SEED_ARS);

        arsTreeService.refreshArsTree();

        assertThat(
            arsTreeService.getArsTree().resolveToGemeinden("020000000000"),
            containsInAnyOrder("020000000000")
        );
        verify(codelistDownloader, never()).updateDestatisArsCodelistCaches();
        verify(codelistDownloader, never()).getCachedReducedDestatisArsCodelist();
    }
}
