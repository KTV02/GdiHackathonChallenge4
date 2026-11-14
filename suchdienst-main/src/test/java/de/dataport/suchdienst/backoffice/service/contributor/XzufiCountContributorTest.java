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
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
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
 * Unit-Tests für {@link XzufiCountContributor}.
 */
@ExtendWith(MockitoExtension.class)
class XzufiCountContributorTest {

    @Mock
    private ElasticInternalReadConnector elasticReadConnector;

    @InjectMocks
    private XzufiCountContributor contributor;

    @Test
    void collectData_writesAllCounters() {
        String shAgencyId = Bundeslaender.SCHLESWIG_HOLSTEIN.getSchemeAgencyId();
        Map<String, DataOverviewOutModel.Builder> builders = new LinkedHashMap<>();
        builders.put(
            shAgencyId,
            DataOverviewOutModel.builder()
                .fedState("01")
                .name("Schleswig-Holstein")
                .schemeAgencyId(shAgencyId)
        );

        Map<IndexNames, Map<String, Long>> mockResult = Map.of(
            IndexNames.LEISTUNGSBESCHREIBUNG,
            Map.of(shAgencyId, 10L),
            IndexNames.ORGANISATIONSEINHEITEN,
            Map.of(shAgencyId, 5L),
            IndexNames.ONLINEDIENSTE,
            Map.of(shAgencyId, 3L),
            IndexNames.SPEZIALISIERUNGEN,
            Map.of(shAgencyId, 7L),
            IndexNames.ZUSTAENDIGKEITEN,
            Map.of(shAgencyId, 2L)
        );
        Mockito.when(
            elasticReadConnector.getAggregatedDocumentCounts(
                Mockito.eq(builders.keySet()),
                Mockito.any(IndexNames[].class)
            )
        ).thenReturn(mockResult);

        contributor.collectDataParallel(builders.keySet()).accept(builders);

        var result = builders.get(shAgencyId).build();
        assertEquals(10, result.lbCount());
        assertEquals(5, result.oeCount());
        assertEquals(3, result.odCount());
        assertEquals(7, result.spCount());
        assertEquals(2, result.zsCount());
    }

    @Test
    void collectData_ignoresUnknownSchemeAgencyId() {
        String hhAgencyId = Bundeslaender.HAMBURG.getSchemeAgencyId();
        Map<String, DataOverviewOutModel.Builder> builders = new LinkedHashMap<>();
        builders.put(
            hhAgencyId,
            DataOverviewOutModel.builder().fedState("02").name("Hamburg").schemeAgencyId(hhAgencyId)
        );

        // ES liefert Daten für eine unbekannte SchemeAgencyId:
        String unknownId = "UNKNOWN";
        Map<IndexNames, Map<String, Long>> mockResult = Map.of(
            IndexNames.LEISTUNGSBESCHREIBUNG,
            Map.of(unknownId, 99L),
            IndexNames.ORGANISATIONSEINHEITEN,
            Map.of(),
            IndexNames.ONLINEDIENSTE,
            Map.of(),
            IndexNames.SPEZIALISIERUNGEN,
            Map.of(),
            IndexNames.ZUSTAENDIGKEITEN,
            Map.of()
        );
        Mockito.when(
            elasticReadConnector.getAggregatedDocumentCounts(
                Mockito.eq(builders.keySet()),
                Mockito.any(IndexNames[].class)
            )
        ).thenReturn(mockResult);

        contributor.collectDataParallel(builders.keySet()).accept(builders);

        // Kein Crash, bekannter Builder bleibt bei 0
        var result = builders.get(hhAgencyId).build();
        assertEquals(0, result.lbCount());
    }

    @Test
    void collectData_emptyResponseLeavesCountersAtZero() {
        String shAgencyId = Bundeslaender.SCHLESWIG_HOLSTEIN.getSchemeAgencyId();
        Map<String, DataOverviewOutModel.Builder> builders = new LinkedHashMap<>();
        builders.put(
            shAgencyId,
            DataOverviewOutModel.builder()
                .fedState("01")
                .name("Schleswig-Holstein")
                .schemeAgencyId(shAgencyId)
        );

        Map<IndexNames, Map<String, Long>> emptyResult = Map.of(
            IndexNames.LEISTUNGSBESCHREIBUNG,
            Map.of(),
            IndexNames.ORGANISATIONSEINHEITEN,
            Map.of(),
            IndexNames.ONLINEDIENSTE,
            Map.of(),
            IndexNames.SPEZIALISIERUNGEN,
            Map.of(),
            IndexNames.ZUSTAENDIGKEITEN,
            Map.of()
        );
        Mockito.when(
            elasticReadConnector.getAggregatedDocumentCounts(
                Mockito.eq(builders.keySet()),
                Mockito.any(IndexNames[].class)
            )
        ).thenReturn(emptyResult);

        contributor.collectDataParallel(builders.keySet()).accept(builders);

        var result = builders.get(shAgencyId).build();
        assertEquals(0, result.lbCount());
        assertEquals(0, result.oeCount());
        assertEquals(0, result.odCount());
        assertEquals(0, result.spCount());
        assertEquals(0, result.zsCount());
    }
}
