/*-
 * ---license-start
 * Suchdienst Integrationstests
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package suchdienst.integrationstest.tests.datenbestanduebersicht;

import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig;
import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ClientStatsConnector;
import suchdienst.integrationstest.validator.clientstats.ClientStatsV2Validator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class ClientStatsTest {

    private static JsonPath initialClientStatsV2;

    private static final List<BundeslandConfig> bundeslandConfigs = List.of(
        new BundeslandConfig(Bundesland.BB),
        new BundeslandConfig(Bundesland.BE),
        new BundeslandConfig(Bundesland.BW),
        new BundeslandConfig(Bundesland.BY),
        new BundeslandConfig(Bundesland.HB),
        new BundeslandConfig(Bundesland.HE),
        new BundeslandConfig(Bundesland.HH),
        new BundeslandConfig(Bundesland.MV),
        new BundeslandConfig(Bundesland.NI),
        new BundeslandConfig(Bundesland.NW),
        new BundeslandConfig(Bundesland.RP),
        new BundeslandConfig(Bundesland.SH),
        new BundeslandConfig(Bundesland.SL),
        new BundeslandConfig(Bundesland.SN),
        new BundeslandConfig(Bundesland.TH),
        new BundeslandConfig(Bundesland.ST),
        new BundeslandConfig(Bundesland.BUND)
    );

    @BeforeAll
    static void setup(TestInfo testInfo) {
        String testclassname = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());

        initialClientStatsV2 = ClientStatsConnector.getClientStatsV2()
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .extract()
            .jsonPath();

        // Import Daten fuer jedes Bundesland
        String importId = null;
        for (BundeslandConfig bundeslandConfig : bundeslandConfigs) {
            XzufiFile xzufiFile = XzufiFileCreator.newInstance(bundeslandConfig)
                .name(testclassname + "_" + bundeslandConfig.getKuerzel())
                .lb(1)
                .oe(1)
                .od(1)
                .sp(1)
                .getMin();
            importId = ImportUtil.push23(xzufiFile);
        }
        ImportUtil.waitForImport(importId);
    }

    @Test
    void clientStatsV2() {
        Response actualClientStats = ClientStatsConnector.getClientStatsV2();
        ClientStatsV2Validator clientStatsV2Validator = new ClientStatsV2Validator(actualClientStats);

        for (BundeslandConfig bundeslandConfig : bundeslandConfigs) {
            String bundeslandPrefix = bundeslandConfig.getBasicArs().substring(0, 2);
            String stateStatisticsV2 = initialClientStatsV2.getString(
                "stateStatistics.find {it.ars == '" + bundeslandPrefix + "'}"
            );
            if (stateStatisticsV2 != null) {
                int lbs = initialClientStatsV2.getInt(
                    "stateStatistics.find {it.ars == '" + bundeslandPrefix + "'}.lbs.count"
                );
                clientStatsV2Validator
                    .validateLbCount(bundeslandPrefix, lbs + 1)
                    .validateLbLastNotEmpty(bundeslandPrefix);

                int oes = initialClientStatsV2.getInt(
                    "stateStatistics.find {it.ars == '" + bundeslandPrefix + "'}.oes.count"
                );
                clientStatsV2Validator
                    .validateOeCount(bundeslandPrefix, oes + 1)
                    .validateOeLastUpdateNotEmpty(bundeslandPrefix);

                int ods = initialClientStatsV2.getInt(
                    "stateStatistics.find {it.ars == '" + bundeslandPrefix + "'}.ods.count"
                );

                clientStatsV2Validator
                    .validateOdCount(bundeslandPrefix, ods + 1)
                    .validateOdLastUpdateNotEmpty(bundeslandPrefix);
                int sps = initialClientStatsV2.getInt(
                    "stateStatistics.find {it.ars == '" + bundeslandPrefix + "'}.sps.count"
                );
                clientStatsV2Validator
                    .validateSpCount(bundeslandPrefix, sps + 1)
                    .validateSpLastUpdateNotEmpty(bundeslandPrefix);
            } else {
                clientStatsV2Validator
                    .validateLbCount(bundeslandPrefix, 1)
                    .validateLbLastNotEmpty(bundeslandPrefix)
                    .validateOeCount(bundeslandPrefix, 1)
                    .validateOeLastUpdateNotEmpty(bundeslandPrefix)
                    .validateOdCount(bundeslandPrefix, 1)
                    .validateOeLastUpdateNotEmpty(bundeslandPrefix)
                    .validateSpCount(bundeslandPrefix, 1)
                    .validateSpLastUpdateNotEmpty(bundeslandPrefix);
            }
        }
    }
}
