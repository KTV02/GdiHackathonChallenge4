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
import de.dataport.suchdienst.elasticsearch.api.AliasNames;
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
 * Unit-Tests für {@link OdRelationsCountContributor}.
 */
@ExtendWith(MockitoExtension.class)
class OdRelationsCountContributorTest {

    @Mock
    private ElasticInternalReadConnector elasticReadConnector;

    @InjectMocks
    private OdRelationsCountContributor contributor;

    @Test
    void collectData_setsOdRelCountFromElastic() {
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

        Map<AliasNames, Map<String, Long>> mockResult = Map.of(
            AliasNames.ONLINEDIENSTE_RELATIONEN,
            Map.of(shAgencyId, 15L, hhAgencyId, 7L)
        );
        Mockito.when(
            elasticReadConnector.getAggregatedDocumentCounts(
                builders.keySet(),
                AliasNames.ONLINEDIENSTE_RELATIONEN
            )
        ).thenReturn(mockResult);

        contributor.collectDataParallel(builders.keySet()).accept(builders);

        assertEquals(15, builders.get(shAgencyId).build().odRelCount());
        assertEquals(7, builders.get(hhAgencyId).build().odRelCount());
    }

    @Test
    void collectData_handlesNullMapForAlias() {
        String shAgencyId = Bundeslaender.SCHLESWIG_HOLSTEIN.getSchemeAgencyId();

        Map<String, DataOverviewOutModel.Builder> builders = new LinkedHashMap<>();
        builders.put(
            shAgencyId,
            DataOverviewOutModel.builder()
                .fedState("01")
                .name("Schleswig-Holstein")
                .schemeAgencyId(shAgencyId)
        );

        // Response enthält keinen Eintrag für ONLINEDIENST_RELATIONS → get() liefert null
        Map<AliasNames, Map<String, Long>> mockResult = Map.of();
        Mockito.when(
            elasticReadConnector.getAggregatedDocumentCounts(
                builders.keySet(),
                AliasNames.ONLINEDIENSTE_RELATIONEN
            )
        ).thenReturn(mockResult);

        contributor.collectDataParallel(builders.keySet()).accept(builders);

        // odRelCount bleibt bei 0, kein Crash
        assertEquals(0, builders.get(shAgencyId).build().odRelCount());
    }

    @Test
    void collectData_ignoresUnknownSchemeAgencyId() {
        String hhAgencyId = Bundeslaender.HAMBURG.getSchemeAgencyId();
        String unknownId = "UNKNOWN_AGENCY";

        Map<String, DataOverviewOutModel.Builder> builders = new LinkedHashMap<>();
        builders.put(
            hhAgencyId,
            DataOverviewOutModel.builder().fedState("02").name("Hamburg").schemeAgencyId(hhAgencyId)
        );

        // ES liefert Daten für eine unbekannte SchemeAgencyId:
        Map<AliasNames, Map<String, Long>> mockResult = Map.of(
            AliasNames.ONLINEDIENSTE_RELATIONEN,
            Map.of(unknownId, 99L)
        );
        Mockito.when(
            elasticReadConnector.getAggregatedDocumentCounts(
                builders.keySet(),
                AliasNames.ONLINEDIENSTE_RELATIONEN
            )
        ).thenReturn(mockResult);

        contributor.collectDataParallel(builders.keySet()).accept(builders);

        // Kein Crash, bekannter Builder bleibt bei 0
        assertEquals(0, builders.get(hhAgencyId).build().odRelCount());
    }

    @Test
    void collectData_emptyInnerMapLeavesCountAtZero() {
        String shAgencyId = Bundeslaender.SCHLESWIG_HOLSTEIN.getSchemeAgencyId();

        Map<String, DataOverviewOutModel.Builder> builders = new LinkedHashMap<>();
        builders.put(
            shAgencyId,
            DataOverviewOutModel.builder()
                .fedState("01")
                .name("Schleswig-Holstein")
                .schemeAgencyId(shAgencyId)
        );

        Map<AliasNames, Map<String, Long>> mockResult = Map.of(
            AliasNames.ONLINEDIENSTE_RELATIONEN,
            Map.of()
        );
        Mockito.when(
            elasticReadConnector.getAggregatedDocumentCounts(
                builders.keySet(),
                AliasNames.ONLINEDIENSTE_RELATIONEN
            )
        ).thenReturn(mockResult);

        contributor.collectDataParallel(builders.keySet()).accept(builders);

        assertEquals(0, builders.get(shAgencyId).build().odRelCount());
    }
}
