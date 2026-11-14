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
package suchdienst.integrationstest.tests.monitoring;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.LbCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsOnlinedienstCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsOrganisationseinheitCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.MonitoringConnector;
import suchdienst.integrationstest.validator.monitoring.MonitoringValidator;

/**
 * Tests, die überprüfen, ob die Transaktions Metadaten an den Ods und Oes vorhanden sind. Siehe PV-7725.
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class MissingMetadataTest {

    @Test
    void addLBandZSOE(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.push(fileSetup);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = LbCreator.getInstance().getMin(testcasename);
        file.getLeistungsbeschreibungen().add(leistung);
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        String oeId = organisationseinheit.getId();
        Zustaendigkeit zustaendigkeit = ZsOrganisationseinheitCreator.getInstance().getMin(leistung.getId(), oeId);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeit);
        ImportUtil.pushAndWait23(file);

        validateMetadataFormat("oe", organisationseinheit.getFullId());
    }

    @Test
    void addLBandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).od(1).getMin();
        ImportUtil.push(fileSetup);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = LbCreator.getInstance().getMin(testcasename);
        file.getLeistungsbeschreibungen().add(leistung);
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        Zustaendigkeit zustaendigkeit = ZsOnlinedienstCreator.getInstance().getMin(
            leistung.getId(),
            onlinedienst.getId()
        );
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeit);
        ImportUtil.pushAndWait23(file);

        validateMetadataFormat("od", onlinedienst.getFullId());
    }

    /**
     * Dieser Test verhält sich so wie der SaD. Von dort werden erst ODs/OEs in einer Nachricht importiert, dann kommen in weiteren
     * Nachrichten LBs zusammen mit ZS.
     * Prüfen, ob dadurch die Metadaten an dem OD erhalten bleiben.
     */
    @Test
    void importOdThenImportLbAndZsOd(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Setup: Eine OD importieren *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(0).oe(0).od(1).sp(0).getMin();
        ImportUtil.push(fileSetup);

        //***** Testcase: Eine LB mit ZSOD importieren:  *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung lb = LbCreator.getInstance().getMin(testcasename);
        file.getLeistungsbeschreibungen().add(lb);
        Onlinedienst od = fileSetup.getOnlinedienste().getFirst();

        Zustaendigkeit zsOd = ZsOnlinedienstCreator.getInstance().getMin(lb.getId(), od.getId());
        file.getZustaendigkeitenOnlinedienst().add(zsOd);
        ImportUtil.pushAndWait23(file);

        validateMetadataFormat("od", od.getFullId());
    }

    /**
     * Etwas komplexerer Test. In einer ersten Nachricht sowohl eine OE als auch OD importieren.
     * Dann eine Nachricht mit einer LB samt ZS auf die OE.
     * Dann eine Nachricht mit einer LB samt ZS auf OE und OD.
     * Dann eine Nachricht mit einer LB samt ZS auf OD.
     * Die Metadaten der OE und OD müssen erhalten bleiben.
     */
    @Test
    void importOeAndOdAndSomeLbs(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        XzufiFile fileContainingOeAndOd = XzufiFileCreator.newInstance()
            .name(testcasename)
            .lb(0)
            .oe(1)
            .od(1)
            .sp(0)
            .getMin();
        ImportUtil.push(fileContainingOeAndOd);

        Onlinedienst od = fileContainingOeAndOd.getOnlinedienste().getFirst();
        Organisationseinheit oe = fileContainingOeAndOd.getOrganisationseinheiten().getFirst();

        XzufiFile fileContainingLbAndZsOe = new XzufiFile();
        Leistungsbeschreibung lb1 = LbCreator.getInstance().getMin(testcasename);
        fileContainingLbAndZsOe.getLeistungsbeschreibungen().add(lb1);
        Zustaendigkeit zsOe1 = ZsOrganisationseinheitCreator.getInstance().getMin(lb1.getId(), oe.getId());
        fileContainingLbAndZsOe.getZustaendigkeitenOrganisationseinheit().add(zsOe1);
        ImportUtil.push(fileContainingLbAndZsOe);

        XzufiFile fileContainingLbAndZsOeAndZsOd = new XzufiFile();
        Leistungsbeschreibung lb2 = LbCreator.getInstance().getMin(testcasename);
        fileContainingLbAndZsOeAndZsOd.getLeistungsbeschreibungen().add(lb2);
        Zustaendigkeit zsOe2 = ZsOrganisationseinheitCreator.getInstance().getMin(lb2.getId(), oe.getId());
        fileContainingLbAndZsOeAndZsOd.getZustaendigkeitenOrganisationseinheit().add(zsOe2);
        Zustaendigkeit zsOd2 = ZsOnlinedienstCreator.getInstance().getMin(lb2.getId(), od.getId());
        fileContainingLbAndZsOeAndZsOd.getZustaendigkeitenOnlinedienst().add(zsOd2);
        ImportUtil.push(fileContainingLbAndZsOeAndZsOd);

        XzufiFile fileContainingLbAndZsOd = new XzufiFile();
        Leistungsbeschreibung lb3 = LbCreator.getInstance().getMin(testcasename);
        fileContainingLbAndZsOd.getLeistungsbeschreibungen().add(lb3);
        Zustaendigkeit zsOd3 = ZsOnlinedienstCreator.getInstance().getMin(lb3.getId(), od.getId());
        fileContainingLbAndZsOd.getZustaendigkeitenOnlinedienst().add(zsOd3);
        ImportUtil.pushAndWait23(fileContainingLbAndZsOd);

        validateMetadataFormat("oe", oe.getFullId());
        validateMetadataFormat("od", od.getFullId());
    }

    private void validateMetadataFormat(String boType, String fullId) {
        Response monitoringResponseOd = MonitoringConnector.getBo(boType);
        MonitoringValidator monitoringValidatorOd = new MonitoringValidator(monitoringResponseOd);
        monitoringValidatorOd.validateMetadataFormat(fullId);
    }
}
