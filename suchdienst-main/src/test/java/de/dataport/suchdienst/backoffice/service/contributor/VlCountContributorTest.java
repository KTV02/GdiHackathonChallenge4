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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import de.dataport.suchdienst.backoffice.model.out.DataOverviewOutModel;
import de.dataport.suchdienst.elasticsearch.api.AliasNames;
import de.dataport.suchdienst.elasticsearch.api.ElasticInternalReadConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexConfiguration;
import de.dataport.suchdienst.util.Bundeslaender;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit-Tests für {@link VlCountContributor}.
 */
@ExtendWith(MockitoExtension.class)
class VlCountContributorTest {

    @Mock
    private ElasticInternalReadConnector elasticReadConnector;

    @Mock
    private IndexConfiguration indexConfiguration;

    @InjectMocks
    private VlCountContributor contributor;

    @Test
    void collectData_setsVlCountForKnownBundesland() {
        String shAgencyId = Bundeslaender.SCHLESWIG_HOLSTEIN.getSchemeAgencyId();
        String shArs = Bundeslaender.SCHLESWIG_HOLSTEIN.getArsFederalstate(); // "01"

        Map<String, DataOverviewOutModel.Builder> builders = new LinkedHashMap<>();
        builders.put(
            shAgencyId,
            DataOverviewOutModel.builder()
                .fedState(shArs)
                .name("Schleswig-Holstein")
                .schemeAgencyId(shAgencyId)
        );

        // Jeder ARS_MAPPING-Eintrag wird über indexConfiguration.getCurrentWriteAliasName aufgelöst.
        // Wir mocken so, dass der Alias-Name = "write_" + alias.getName() zurückgegeben wird:
        AliasNames.ARS_MAPPING.values().forEach(alias ->
            when(indexConfiguration.getCurrentWriteAliasName(alias)).thenReturn(
                "write_" + alias.getName()
            )
        );

        // countDocuments liefert nur für SH einen Wert:
        String shWriteAlias = "write_" + AliasNames.VERWALTUNGSLEISTUNGEN_SH.getName();
        when(elasticReadConnector.countDocuments(anyList())).thenReturn(Map.of(shWriteAlias, 42L));

        contributor.collectDataParallel(builders.keySet()).accept(builders);

        assertEquals(42, builders.get(shAgencyId).build().vlCount());
    }

    @Test
    void collectData_defaultsToZeroWhenAliasNotInResponse() {
        String hhAgencyId = Bundeslaender.HAMBURG.getSchemeAgencyId();
        String hhArs = Bundeslaender.HAMBURG.getArsFederalstate(); // "02"

        Map<String, DataOverviewOutModel.Builder> builders = new LinkedHashMap<>();
        builders.put(
            hhAgencyId,
            DataOverviewOutModel.builder()
                .fedState(hhArs)
                .name("Hamburg")
                .schemeAgencyId(hhAgencyId)
        );

        AliasNames.ARS_MAPPING.values().forEach(alias ->
            when(indexConfiguration.getCurrentWriteAliasName(alias)).thenReturn(
                "write_" + alias.getName()
            )
        );

        // countDocuments gibt leere Map zurück → getOrDefault greift
        when(elasticReadConnector.countDocuments(anyList())).thenReturn(Map.of());

        contributor.collectDataParallel(builders.keySet()).accept(builders);

        assertEquals(0, builders.get(hhAgencyId).build().vlCount());
    }

    @Test
    void collectData_ignoresBuilderNotInMap() {
        // Nur SH ist als Builder vorhanden, HH-Daten aus Elastic werden ignoriert
        String shAgencyId = Bundeslaender.SCHLESWIG_HOLSTEIN.getSchemeAgencyId();
        String shArs = Bundeslaender.SCHLESWIG_HOLSTEIN.getArsFederalstate();

        Map<String, DataOverviewOutModel.Builder> builders = new LinkedHashMap<>();
        builders.put(
            shAgencyId,
            DataOverviewOutModel.builder()
                .fedState(shArs)
                .name("Schleswig-Holstein")
                .schemeAgencyId(shAgencyId)
        );

        AliasNames.ARS_MAPPING.values().forEach(alias ->
            when(indexConfiguration.getCurrentWriteAliasName(alias)).thenReturn(
                "write_" + alias.getName()
            )
        );

        // Werte für alle Aliase, aber nur SH-Builder existiert
        String shWriteAlias = "write_" + AliasNames.VERWALTUNGSLEISTUNGEN_SH.getName();
        String hhWriteAlias = "write_" + AliasNames.VERWALTUNGSLEISTUNGEN_HH.getName();
        when(elasticReadConnector.countDocuments(anyList())).thenReturn(
            Map.of(shWriteAlias, 10L, hhWriteAlias, 20L)
        );

        contributor.collectDataParallel(builders.keySet()).accept(builders);

        // SH bekommt seinen Wert, kein Crash wegen fehlendem HH-Builder
        assertEquals(10, builders.get(shAgencyId).build().vlCount());
    }
}
