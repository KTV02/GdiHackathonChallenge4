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
package suchdienst.integrationstest.tests.x22.detailseite.servicedescriptionsdetailv6;

import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig;
import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.ErrorValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class ServiceDescriptionsNegativeTest {

    private static String lbId;
    private static String ars;

    @BeforeAll
    static void setupTestclass(TestInfo testInfo) {
        String testclassname = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testclassname).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(file);

        ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        lbId = file.getLeistungsbeschreibungen().getFirst().getFullId();
    }

    @Test
    void unknownLbId() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, "UnknownLbId");
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();
    }

    @Test
    void emptyArs() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6("", lbId);
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest();
    }

    @Test
    void unknownArs() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6("099999999999", lbId);
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();
    }

    @Test
    void tooShortArs() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6("09999999999", lbId);
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest().validateField("ars", "Größe muss zwischen 12 und 12 sein");
    }

    @Test
    void tooLongArs() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6("0999999999999", lbId);
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest().validateField("ars", "Größe muss zwischen 12 und 12 sein");
    }

    @Test
    void noZustaendigkeitOE(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        BundeslandConfig bundeslandConfig = new BundeslandConfig(Bundesland.BY);

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance(bundeslandConfig).name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup);

        //***** Testcase *****
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        String currentArs = bundeslandConfig.getArss().get(1);
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            currentArs,
            leistung.getFullId()
        );
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();
    }

    @Test
    void noZustaendigkeitOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        BundeslandConfig bundeslandConfig = new BundeslandConfig(Bundesland.BY);

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance(bundeslandConfig).name(testcasename).lb(1).od(1).getMin();
        ImportUtil.pushAndWait(fileSetup);

        //***** Testcase *****
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        String currentArs = bundeslandConfig.getArss().get(1);
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            currentArs,
            leistung.getFullId()
        );
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();
    }

    /**
     * Testet, ob die Anfrage mit einem ARS, zu dem es keine Formular-Zuständigkeit gibt, eine 404 liefert.
     */
    @Test
    void noZustaendigkeitFO(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        BundeslandConfig bundeslandConfig = new BundeslandConfig(Bundesland.BY);

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance(bundeslandConfig).name(testcasename).lb(1).fo(1).getMin();
        ImportUtil.pushAndWait(fileSetup);

        //***** Testcase *****
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        String currentArs = bundeslandConfig.getArss().get(1);
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            currentArs,
            leistung.getFullId()
        );
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();
    }
}
