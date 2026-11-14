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
package suchdienst.integrationstest.tests.x22.datenimport;

import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig;
import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
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
import suchdienst.integrationstest.connectors.AdminConnector;
import suchdienst.integrationstest.connectors.ClientStatsConnector;
import suchdienst.integrationstest.connectors.ElasticsearchConnector;
import suchdienst.integrationstest.connectors.OrganisationunitsConnector;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.admin.DataOverviewValidator;
import suchdienst.integrationstest.validator.clientstats.ClientStatsV2Validator;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.OrganisationValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.OnlineServiceLinkValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.ServiceDescriptionValidator;

/**
 * Test-Ziel: Der Suchdienst soll Daten aus verschiedenen Redaktionssystem-Quellen verarbeiten können.
 * Dabei unterscheidet der Suchdienst zwischen Bundesland-Redaktionssystemen (z.B. SH/HH) und anderen (Bund, IHK).
 * Alle Daten sollen in der Suche auffindbar sein. Für die Datenübersicht und Statistik wird zwischen ihnen unterschieden.
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class AnySchemeAgencyIdTest {

    private static XzufiFile fileAnyBY;
    private static XzufiFile fileSH;
    private static XzufiFile fileBund;

    @BeforeAll
    static void setup(TestInfo testInfo) {
        ElasticsearchConnector.removeAllData();

        String testcasename = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());

        BundeslandConfig anyArsConfig = new BundeslandConfig(
            "BY",
            testcasename,
            "090000000000",
            List.of("095640000000", "095620000000"),
            "Custom-Bayern"
        );
        BundeslandConfig knownArsConfigSH = new BundeslandConfig(Bundesland.SH);
        BundeslandConfig knownArsConfigBUND = new BundeslandConfig(Bundesland.BUND);

        fileAnyBY = XzufiFileCreator.newInstance(anyArsConfig).name(testcasename).lb(1).oe(1).od(1).sp(1).getMin();
        fileSH = XzufiFileCreator.newInstance(knownArsConfigSH).name(testcasename).lb(1).oe(1).od(1).sp(1).getMin();
        fileBund = XzufiFileCreator.newInstance(knownArsConfigBUND).name(testcasename).lb(1).oe(1).od(1).sp(1).getMin();

        ImportUtil.push(fileAnyBY, anyArsConfig);
        ImportUtil.push(fileSH, knownArsConfigSH);
        ImportUtil.pushAndWait(fileBund, knownArsConfigBUND);
    }

    @Test
    void datenImport() {
        Leistungsbeschreibung leistung = fileAnyBY.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = fileAnyBY.getOrganisationseinheiten().getFirst();
        Onlinedienst onlinedienst = fileAnyBY.getOnlinedienste().getFirst();

        //***** Testcase *****
        String arsOE = fileAnyBY.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOE, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId())
            .validateName("spezialisierung 03");

        OnlineServiceLinkValidator validatorOD = new OnlineServiceLinkValidator(response);
        validatorOD.validateCount(1).validateUri(0, onlinedienst.getLinks().getFirst().getUri());
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        OrganisationValidator validatorOE = new OrganisationValidator(responseOE);
        validatorOE.validateTitle(organisationseinheit.getTitel());
    }

    @Test
    void dataOverview() {
        // Ein Redaktionssystem, welches nicht direkt einem Bundesland zugeordnet ist, wird nur mit seinen
        // Entity-Objekten aufgeführt. Die Verwaltungsleistungen werden dem Bundesland-Redaktionssystem zugeordnet.

        Response response = AdminConnector.dataOverview();
        DataOverviewValidator validator = new DataOverviewValidator(response);

        validator
            //
            // Bund Redaktionssystem
            .validateLbCount("Bund", 1)
            .validateOeCount("Bund", 1)
            .validateOdCount("Bund", 1)
            .validateSpCount("Bund", 1)
            .validateZsCount("Bund", 2)
            // Dem nicht Bundesland-Redaktionssystem werden keine Verwaltungsleistungen zugeordnet
            .validateVlCount("Bund", 0)
            .validateAchemeAgencyId("Bund", "B100019")
            //
            // Schleswig-Holstein Redaktionssystem
            .validateLbCount("Schleswig-Holstein", 1)
            .validateOeCount("Schleswig-Holstein", 1)
            .validateOdCount("Schleswig-Holstein", 1)
            .validateSpCount("Schleswig-Holstein", 1)
            .validateZsCount("Schleswig-Holstein", 2)
            .validateVlCount("Schleswig-Holstein", 2)
            .validateAchemeAgencyId("Schleswig-Holstein", "L100012")
            //
            // Bayern Redaktionssystem
            .validateLbCount("Bayern", 0)
            .validateOeCount("Bayern", 0)
            .validateOdCount("Bayern", 0)
            .validateSpCount("Bayern", 0)
            .validateZsCount("Bayern", 0)
            // Dem Bundesland-Redaktionssystem werden die Custom-Bayern und Bund Verwaltungsleistungen zugeordnet
            .validateVlCount("Bayern", 2)
            .validateAchemeAgencyId("Bayern", "L100042")
            //
            // Abweichendes Redaktionssystem Custom-Bayern
            .validateLbCount("Custom-Bayern", 1)
            .validateOeCount("Custom-Bayern", 1)
            .validateOdCount("Custom-Bayern", 1)
            .validateSpCount("Custom-Bayern", 1)
            .validateZsCount("Custom-Bayern", 2)
            // Dem nicht Bundesland-Redaktionssystem werden keine Verwaltungsleistungen zugeordnet
            .validateVlCount("Custom-Bayern", 0)
            .validateAchemeAgencyId("Custom-Bayern", fileAnyBY.getSchemeAgencyId());
    }

    @Test
    void statistic() {
        Response response = ClientStatsConnector.getClientStatsV2();
        ClientStatsV2Validator validator = new ClientStatsV2Validator(response);

        validator
            //
            // Bund Redaktionssystem
            .validateLbCount("00", 1)
            .validateOeCount("00", 1)
            .validateOdCount("00", 1)
            .validateSpCount("00", 1)
            //
            // Schleswig-Holstein Redaktionssystem
            .validateLbCount("01", 1)
            .validateOeCount("01", 1)
            .validateOdCount("01", 1)
            .validateSpCount("01", 1)
            //
            // Redaktionssystem Bayern, enthält nur "Custom" Daten die hier nicht zugerechnet werden sollen
            // Bayern wird im Response nicht ausgegeben
            .validateArsNotPresent("09");
    }
}
