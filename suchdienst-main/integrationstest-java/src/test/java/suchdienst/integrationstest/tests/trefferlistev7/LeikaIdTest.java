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
package suchdienst.integrationstest.tests.trefferlistev7;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Suchbegriff;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.servicedescriptionv7.ServiceDescriptionResultListValidator;

/**
 * Test für die Änderungen aus PV-9484. Suche nach LeikaId, falls genau ein Suchkriterium angegeben ist.
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class LeikaIdTest {

    public static final String LEIKA_1 = "12345678901234";
    public static final String LEIKA_2 = "12345678901235";
    public static final String LEIKA_UNKNOWN = "42424242424242";
    private static String lb1Id;
    private static String lb2Id;
    private static String ars;

    private static String commonSuchbegriff;
    private static String lb1Suchbegriff;
    private static String lb2Suchbegriff;

    /**
     * Legt zwei LBs an, Suchbegriffe "Leistung" und "LB1" bzw. "LB2". LeikaIds "12345678901234" und "12345678901235".
     */
    @BeforeAll
    static void setupTestclass(TestInfo testInfo) {
        String testclassname = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testclassname).lb(2).oe(1).getMin();
        Leistungsbeschreibung lb1 = file.getLeistungsbeschreibungen().getFirst();
        Leistungsbeschreibung lb2 = file.getLeistungsbeschreibungen().get(1);
        commonSuchbegriff = testclassname + "-common";
        lb1Suchbegriff = testclassname + "-lb1";
        lb2Suchbegriff = testclassname + "-lb2";
        setSuchbegriffe(lb1, commonSuchbegriff, lb1Suchbegriff);
        setSuchbegriffe(lb2, commonSuchbegriff, lb2Suchbegriff);
        lb1.setReferenzLeiKa(LEIKA_1);
        lb2.setReferenzLeiKa(LEIKA_2);
        ImportUtil.pushAndWait23(file);
        ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        lb1Id = lb1.getFullId();
        lb2Id = lb2.getFullId();
    }

    private static void setSuchbegriffe(Leistungsbeschreibung lb, String begriff1, String begriff2) {
        List<Suchbegriff> suchbegriffe = new ArrayList<>();
        Suchbegriff suchbegriff = new Suchbegriff();
        suchbegriff.setBegriff(begriff1);
        suchbegriffe.add(suchbegriff);
        suchbegriff = new Suchbegriff();
        suchbegriff.setBegriff(begriff2);
        suchbegriffe.add(suchbegriff);
        lb.setSuchbegriffe(suchbegriffe);
    }

    @Test
    void findByLeikaIdOnlyFindsSingle() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7(
            ServiceDescriptionConnector.V7ConnectorProperties.builder().ars(ars).q(LEIKA_1).build()
        );
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(lb1Id);
        validator.validateNotContains(lb2Id);
    }

    @Test
    void findByMultipleLeikaIdFindsNone() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7(
            ServiceDescriptionConnector.V7ConnectorProperties.builder().ars(ars).q(LEIKA_1).q(LEIKA_2).build()
        );
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        // Findet keine LB, da die Suchbegriffe und-verknüpft sind
        validator.validateCount(0);
    }

    @Test
    void findByUnknownLeikaIdFindsNone() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7(
            ServiceDescriptionConnector.V7ConnectorProperties.builder().ars(ars).q(LEIKA_UNKNOWN).build()
        );
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateCount(0);
    }

    @Test
    void findByCommonSuchbegriffFindsAll() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7(
            ServiceDescriptionConnector.V7ConnectorProperties.builder().ars(ars).q(commonSuchbegriff).build()
        );
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(lb1Id);
        validator.validateContains(lb2Id);
    }

    @Test
    void findByCommonAndSpecificSuchbegriffFindsSingle() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7(
            ServiceDescriptionConnector.V7ConnectorProperties.builder()
                .ars(ars)
                .q(commonSuchbegriff)
                .q(lb1Suchbegriff)
                .build()
        );
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(lb1Id);
        validator.validateNotContains(lb2Id);
    }

    @Test
    void findByLeikaIdAndCommonSuchbegriffFindsNone() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7(
            ServiceDescriptionConnector.V7ConnectorProperties.builder().ars(ars).q(LEIKA_1).q(commonSuchbegriff).build()
        );
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        // Findet keine LB, da die Suchbegriffe und-verknüpft sind
        validator.validateCount(0);
    }

    @Test
    void findBySpecificSuchbegriffFindsSingle() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7(
            ServiceDescriptionConnector.V7ConnectorProperties.builder().ars(ars).q(lb1Suchbegriff).build()
        );
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(lb1Id);
    }
}
