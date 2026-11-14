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
import de.dataport.pvog.testdatengenerator.model.LeiKaTypisierung;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Suchbegriff;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.Arrays;
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

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class IgnoreLeikaTypesTest {

    private static String lbId;
    private static String ars;
    private static String queryString;

    @BeforeAll
    static void setupTestclass(TestInfo testInfo) {
        String testclassname = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testclassname).lb(1).oe(1).getMin();
        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        lb.getTypisierung().clear();
        LeiKaTypisierung typisierung = new LeiKaTypisierung();
        typisierung.setCode("4");
        typisierung.setName("Regelungskompetenz auf Landesebene");
        lb.getTypisierung().add(typisierung);
        LeiKaTypisierung typisierung5 = new LeiKaTypisierung();
        typisierung5.setCode("5");
        typisierung5.setName("Regelungskompetenz auf kommunaler Ebene");
        lb.getTypisierung().add(typisierung5);

        List<Suchbegriff> suchbegriffe = new ArrayList<>();
        Suchbegriff suchbegriff = new Suchbegriff();
        suchbegriff.setBegriff("IgnoreLeikatypesTestEindeutigerSuchbegriff");
        suchbegriffe.add(suchbegriff);
        lb.setSuchbegriffe(suchbegriffe);

        ImportUtil.pushAndWait23(file);

        ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        lbId = file.getLeistungsbeschreibungen().getFirst().getFullId();
        queryString = "IgnoreLeikatypesTestEindeutigerSuchbegriff";
    }

    @Test
    void findNotIgnoredLeikatypes() {
        List<String> leikaTypesToIgnore = Arrays.asList("1", "4", "10", "14");
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7(ars, queryString, leikaTypesToIgnore);

        // Validierung
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(lbId);

        leikaTypesToIgnore = Arrays.asList("1", "5", "10", "14");
        response = ServiceDescriptionConnector.getServiceDescriptionsV7(ars, queryString, leikaTypesToIgnore);

        // Validierung
        validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(lbId);

        leikaTypesToIgnore = Arrays.asList("1", "10", "14", "15");
        response = ServiceDescriptionConnector.getServiceDescriptionsV7(ars, queryString, leikaTypesToIgnore);

        // Validierung
        validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(lbId);
    }

    @Test
    void ignoredLeikatypes() {
        List<String> leikaTypesToIgnore = Arrays.asList("1", "4", "5", "10", "14");
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7(ars, queryString, leikaTypesToIgnore);

        // Validierung
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateNotContains(lbId);
    }
}
