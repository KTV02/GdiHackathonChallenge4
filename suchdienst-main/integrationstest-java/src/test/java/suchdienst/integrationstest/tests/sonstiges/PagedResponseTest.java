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
package suchdienst.integrationstest.tests.sonstiges;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import io.restassured.response.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.Constants;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.RelationsConnector;
import suchdienst.integrationstest.validator.PagedModelValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class PagedResponseTest {

    private static String ars;
    private static String lbId;

    @BeforeAll
    static void setupTestClass(TestInfo testInfo) {
        String testCasename = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testCasename).lb(20).oe(20).getMin();
        ImportUtil.pushAndWait(file);

        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        Zustaendigkeit zs = file.getZustaendigkeitenOrganisationseinheit().getFirst();
        ars = zs.getArss().getFirst();
        lbId = lb.getFullId();
    }

    @Test
    void pagedModelResponseTest() {
        Response response1 = RelationsConnector.getRelationsLbIdsArsV3(
            List.of(lbId),
            ars,
            0,
            5,
            Constants.XzufiVersion.V2_2_0
        );
        new PagedModelValidator(response1, Constants.XzufiVersion.V2_2_0)
            .validatePage(0)
            .validateSize(5)
            .validateContentCount(5)
            .validateTotalElements(20)
            .validateTotalPages(4);

        Response response2 = RelationsConnector.getRelationsLbIdsArsV3(
            List.of(lbId),
            ars,
            3,
            5,
            Constants.XzufiVersion.V2_3_1
        );
        new PagedModelValidator(response2, Constants.XzufiVersion.V2_3_1)
            .validatePage(3)
            .validateSize(5)
            .validateContentCount(5)
            .validateTotalElements(20)
            .validateTotalPages(4);
    }
}
