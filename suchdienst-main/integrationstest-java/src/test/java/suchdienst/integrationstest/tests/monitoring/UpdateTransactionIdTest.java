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

import com.google.common.collect.Iterables;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.generator.XzufiGenerator;
import de.dataport.pvog.testdatengenerator.generator.model.SaDMessage;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.ModulSpezialisierung;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.Spezialisierung;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulursprungsportal.UrsprungsportalModul;
import io.restassured.response.Response;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.MonitoringConnector;
import suchdienst.integrationstest.validator.monitoring.MonitoringValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class UpdateTransactionIdTest {

    @Test
    void updateLb(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        SaDMessage lastMessageSetup = pushAndWaitAndGetLastMessage(fileSetup);

        Leistungsbeschreibung lb = fileSetup.getLeistungsbeschreibungen().getFirst();
        String lbId = lb.getFullId();

        Response responseSetup = MonitoringConnector.getLastResponse("lb");
        MonitoringValidator validatorSetup = new MonitoringValidator(responseSetup);

        validatorSetup
            .validateContainsId(lbId)
            .validateTransactionId(lbId, lastMessageSetup.getTransaktionsId())
            .validateMetadataFormat(lbId);

        //***** Testcase *****
        XzufiFile fileModify = new XzufiFile();
        Leistungsbeschreibung lbModify = lb.copy();
        lbModify.getUrsprungsportale().clear();
        lbModify.getUrsprungsportale().add(UrsprungsportalModul.builder().uri("www.newPortal.de").build());
        fileModify.getLeistungsbeschreibungen().add(lbModify);

        SaDMessage lastMessageModify = pushAndWaitAndGetLastMessage(fileModify);

        Response responseModify = MonitoringConnector.getLastResponse("lb");
        MonitoringValidator validatorModify = new MonitoringValidator(responseModify);

        validatorModify
            .validateContainsId(lbId)
            .validateTransactionId(lbId, lastMessageModify.getTransaktionsId())
            .validateMetadataFormat(lbId);
    }

    @Test
    void updateZsOe(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String connectParameter = testcasename + "_FitConnect";
        String connectParameterModify = connectParameter + "_Modify";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung lb = fileSetup.getLeistungsbeschreibungen().getFirst();
        Zustaendigkeit zsoe = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst();
        zsoe.setFitConnectId(connectParameter);
        SaDMessage lastMessageSetup = pushAndWaitAndGetLastMessage(fileSetup);

        String relationId =
            zsoe.getSchemeAgencyId() +
            ".ZSOE." +
            lb.getId() +
            "." +
            zsoe.getParentId() +
            "." +
            zsoe.getArss().getFirst();

        Response responseSetup = MonitoringConnector.getLastResponse("zsoe");
        MonitoringValidator validatorSetup = new MonitoringValidator(responseSetup);

        validatorSetup
            .validateContainsId(relationId)
            .validateTransactionId(relationId, lastMessageSetup.getTransaktionsId())
            .validateMetadataFormat(relationId);

        //***** Testcase *****
        XzufiFile fileModify = new XzufiFile();
        Zustaendigkeit zsModify = zsoe.copy();
        zsModify.setFitConnectId(connectParameterModify);
        fileModify.getZustaendigkeitenOrganisationseinheit().add(zsModify);

        SaDMessage lastMessageModify = pushAndWaitAndGetLastMessage(fileModify);

        Response responseModify = MonitoringConnector.getLastResponse("zsoe");
        MonitoringValidator validatorModify = new MonitoringValidator(responseModify);

        validatorModify
            .validateContainsId(relationId)
            .validateTransactionId(relationId, lastMessageModify.getTransaktionsId())
            .validateMetadataFormat(relationId);
    }

    @Test
    void updateZsOd(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String connectParameter = testcasename + "_FitConnect";
        String connectParameterModify = connectParameter + "_Modify";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).od(1).getMin();
        Leistungsbeschreibung lb = fileSetup.getLeistungsbeschreibungen().getFirst();
        Zustaendigkeit zsod = fileSetup.getZustaendigkeitenOnlinedienst().getFirst();
        zsod.setFitConnectId(connectParameter);
        SaDMessage lastMessageSetup = pushAndWaitAndGetLastMessage(fileSetup);

        String relationId =
            zsod.getSchemeAgencyId() +
            ".ZSOD." +
            lb.getId() +
            "." +
            zsod.getParentId() +
            "." +
            zsod.getArss().getFirst();

        Response responseSetup = MonitoringConnector.getLastResponse("zsod");
        MonitoringValidator validatorSetup = new MonitoringValidator(responseSetup);

        validatorSetup
            .validateContainsId(relationId)
            .validateTransactionId(relationId, lastMessageSetup.getTransaktionsId())
            .validateMetadataFormat(relationId);

        //***** Testcase *****
        XzufiFile fileModify = new XzufiFile();
        Zustaendigkeit zsModify = zsod.copy();
        zsModify.setFitConnectId(connectParameterModify);
        fileModify.getZustaendigkeitenOnlinedienst().add(zsModify);

        SaDMessage lastMessageModify = pushAndWaitAndGetLastMessage(fileModify);

        Response responseModify = MonitoringConnector.getLastResponse("zsod");
        MonitoringValidator validatorModify = new MonitoringValidator(responseModify);

        validatorModify
            .validateContainsId(relationId)
            .validateTransactionId(relationId, lastMessageModify.getTransaktionsId())
            .validateMetadataFormat(relationId);
    }

    @Test
    void updateSp(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String spezTextmodulInhaltModify = spezTextmodulInhalt + "Modify";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).sp(1).getMin();
        SaDMessage lastMessageSetup = pushAndWaitAndGetLastMessage(fileSetup);

        Spezialisierung sp = fileSetup.getSpezialisierungen().getFirst();
        sp.getModulSpezialisierungen().clear();
        sp.getModulSpezialisierungen().add(
            ModulSpezialisierung.builder()
                .ersetzung(true)
                .modul(Textmodul.builder().leikaCode("03").inhalt(spezTextmodulInhalt).build())
                .build()
        );

        String spId = sp.getFullId();

        Response responseSetup = MonitoringConnector.getLastResponse("sp");
        MonitoringValidator validatorSetup = new MonitoringValidator(responseSetup);

        validatorSetup
            .validateContainsId(spId)
            .validateTransactionId(spId, lastMessageSetup.getTransaktionsId())
            .validateMetadataFormat(spId);

        //***** Testcase *****
        XzufiFile fileModify = new XzufiFile();
        Spezialisierung spModify = sp.copy();
        Textmodul textmodul = (Textmodul) spModify.getModulSpezialisierungen().getFirst().getModul();
        textmodul.setInhalt(spezTextmodulInhaltModify);
        fileModify.getSpezialisierungen().add(spModify);

        SaDMessage lastMessageModify = pushAndWaitAndGetLastMessage(fileModify);

        Response responseModify = MonitoringConnector.getLastResponse("sp");
        MonitoringValidator validatorModify = new MonitoringValidator(responseModify);

        validatorModify
            .validateContainsId(spId)
            .validateTransactionId(spId, lastMessageModify.getTransaktionsId())
            .validateMetadataFormat(spId);
    }

    @Test
    void updateOe(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        SaDMessage lastMessageSetup = pushAndWaitAndGetLastMessage(fileSetup);

        Organisationseinheit oe = fileSetup.getOrganisationseinheiten().getFirst();
        String oeId = oe.getFullId();

        Response responseSetup = MonitoringConnector.getLastResponse("oe");
        MonitoringValidator validatorSetup = new MonitoringValidator(responseSetup);

        validatorSetup
            .validateContainsId(oeId)
            .validateTransactionId(oeId, lastMessageSetup.getTransaktionsId())
            .validateMetadataFormat(oeId);

        //***** Testcase *****
        XzufiFile fileModify = new XzufiFile();
        Organisationseinheit oeModify = oe.copy();
        oeModify.setTitel("newTitle");
        fileModify.getOrganisationseinheiten().add(oeModify);

        SaDMessage lastMessageModify = pushAndWaitAndGetLastMessage(fileModify);

        Response responseModify = MonitoringConnector.getLastResponse("oe");
        MonitoringValidator validatorModify = new MonitoringValidator(responseModify);

        validatorModify
            .validateContainsId(oeId)
            .validateTransactionId(oeId, lastMessageModify.getTransaktionsId())
            .validateMetadataFormat(oeId);
    }

    @Test
    void updateOd(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).od(1).getMin();
        SaDMessage lastMessageSetup = pushAndWaitAndGetLastMessage(fileSetup);

        Onlinedienst od = fileSetup.getOnlinedienste().getFirst();
        String odId = od.getFullId();

        Response responseSetup = MonitoringConnector.getLastResponse("od");
        MonitoringValidator validatorSetup = new MonitoringValidator(responseSetup);

        validatorSetup
            .validateContainsId(odId)
            .validateTransactionId(odId, lastMessageSetup.getTransaktionsId())
            .validateMetadataFormat(odId);

        //***** Testcase *****
        XzufiFile fileModify = new XzufiFile();
        Onlinedienst odModify = od.copy();
        odModify.getLinks().getFirst().setUri("www.newUri.de");
        fileModify.getOnlinedienste().add(odModify);

        SaDMessage lastMessageModify = pushAndWaitAndGetLastMessage(fileModify);

        Response responseModify = MonitoringConnector.getLastResponse("od");
        MonitoringValidator validatorModify = new MonitoringValidator(responseModify);

        validatorModify
            .validateContainsId(odId)
            .validateTransactionId(odId, lastMessageModify.getTransaktionsId())
            .validateMetadataFormat(odId);
    }

    private SaDMessage pushAndWaitAndGetLastMessage(XzufiFile file) {
        List<SaDMessage> messages = XzufiGenerator.generateSaDMessages23(file);
        SaDMessage lastMessage = Iterables.getLast(messages);
        String lastId = ImportUtil.push(messages);
        ImportUtil.waitForImport(lastId);
        return lastMessage;
    }
}
