/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.backoffice.service.contributor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.dataport.suchdienst.backoffice.model.out.DataOverviewOutModel;
import de.dataport.suchdienst.elasticsearch.api.ElasticInternalReadConnector;
import de.dataport.suchdienst.util.Bundeslaender;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit-Tests für {@link ImportQueueCountContributor}.
 */
@ExtendWith(MockitoExtension.class)
class ImportQueueCountContributorTest {

    @Mock
    private ElasticInternalReadConnector elasticReadConnector;

    @InjectMocks
    private ImportQueueCountContributor contributor;

    @Test
    void collectData_setsImportQueueCountFromElastic() {
        String shAgencyId = Bundeslaender.SCHLESWIG_HOLSTEIN.getSchemeAgencyId();
        String hhAgencyId = Bundeslaender.HAMBURG.getSchemeAgencyId();

        Map<String, DataOverviewOutModel.Builder> builders = new LinkedHashMap<>();
        builders.put(
            shAgencyId,
            DataOverviewOutModel.builder()
                .fedState("01")
                .name("Schleswig-Holstein")
                .schemeAgencyId(shAgencyId)
        );
        builders.put(
            hhAgencyId,
            DataOverviewOutModel.builder().fedState("02").name("Hamburg").schemeAgencyId(hhAgencyId)
        );

        Mockito.when(elasticReadConnector.getDataOverviewImportQueue(builders.keySet())).thenReturn(
            Map.of(shAgencyId, 3L, hhAgencyId, 1L)
        );

        contributor.collectDataParallel(builders.keySet()).accept(builders);

        assertEquals(3, builders.get(shAgencyId).importQueueCount());
        assertEquals(1, builders.get(hhAgencyId).importQueueCount());
    }

    @Test
    void collectData_defaultsToZeroWhenAgencyIdMissing() {
        String shAgencyId = Bundeslaender.SCHLESWIG_HOLSTEIN.getSchemeAgencyId();

        Map<String, DataOverviewOutModel.Builder> builders = new LinkedHashMap<>();
        builders.put(
            shAgencyId,
            DataOverviewOutModel.builder()
                .fedState("01")
                .name("Schleswig-Holstein")
                .schemeAgencyId(shAgencyId)
        );

        // ES liefert leere Map zurück
        Mockito.when(elasticReadConnector.getDataOverviewImportQueue(builders.keySet())).thenReturn(
            Map.of()
        );

        contributor.collectDataParallel(builders.keySet()).accept(builders);

        assertEquals(0, builders.get(shAgencyId).importQueueCount());
    }

    @Test
    void collectData_handlesMultipleBundeslaender() {
        Map<String, DataOverviewOutModel.Builder> builders = new LinkedHashMap<>();
        for (Bundeslaender bl : Bundeslaender.values()) {
            builders.put(
                bl.getSchemeAgencyId(),
                DataOverviewOutModel.builder()
                    .fedState(bl.getArsFederalstate())
                    .name(bl.getFederalstateName())
                    .schemeAgencyId(bl.getSchemeAgencyId())
            );
        }

        // Nur SH hat Queue-Einträge
        String shAgencyId = Bundeslaender.SCHLESWIG_HOLSTEIN.getSchemeAgencyId();
        Mockito.when(elasticReadConnector.getDataOverviewImportQueue(builders.keySet())).thenReturn(
            Map.of(shAgencyId, 5L)
        );

        contributor.collectDataParallel(builders.keySet()).accept(builders);

        assertEquals(5, builders.get(shAgencyId).importQueueCount());
        // Alle anderen bei 0
        builders.forEach((id, builder) -> {
            if (!id.equals(shAgencyId)) {
                assertEquals(0, builder.importQueueCount(), "Erwartet 0 für " + id);
            }
        });
    }
}
