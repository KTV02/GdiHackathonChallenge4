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
package suchdienst.integrationstest.tests.x22.datenimport.pushimportv7;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.LbCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.OdCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.OeCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.SpCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsOnlinedienstCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsOrganisationseinheitCreator;
import de.dataport.pvog.testdatengenerator.generator.distributor.PushDistributor;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.ModulSpezialisierung;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.Spezialisierung;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
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
import suchdienst.integrationstest.connectors.OrganisationunitsConnector;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.monitoring.MonitoringValidator;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.OrganisationValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.OnlineServiceLinkValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.ServiceDescriptionValidator;
import suchdienst.integrationstest.validator.servicedescriptionv8.ServiceDescriptionResultListValidator;

// Die Redaktionssysteme schicken die ZSs immer im OD oder OE weshalb die ZSs immer in der selben Nachricht wie der OD bzw OE sind
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class AddTest {

    private static final PushDistributor distributor = new PushDistributor();

    @Test
    void pushAddLB(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = LbCreator.getInstance().getMin(testcasename);
        file.getLeistungsbeschreibungen().add(leistung);
        ImportUtil.pushAndWait(file, distributor);

        // Da eine LB ohne Zustaendigkeit nur ueber die MonitoringAPI sichtbar ist wird diese zur validierung benutzt
        Response response = MonitoringConnector.getBo("lb");
        MonitoringValidator validator = new MonitoringValidator(response);
        validator.validateContainsId(leistung.getFullId());
    }

    @Test
    void pushAddZSOE(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zustaendigkeit = ZsOrganisationseinheitCreator.getInstance().getMin(
            leistung.getId(),
            organisationseinheit.getId(),
            2
        );
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeit);
        ImportUtil.pushAndWait(file, distributor);

        String ars = zustaendigkeit.getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIds(0, organisationseinheit.getFullId());
    }

    @Test
    void pushAddZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();

        Zustaendigkeit zustaendigkeitOD = ZsOnlinedienstCreator.getInstance().getMin(
            leistung.getId(),
            onlinedienst.getId(),
            2
        );
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        ImportUtil.pushAndWait(file, distributor);

        String ars = zustaendigkeitOD.getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateId(leistung.getFullId());
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(response);
        onlineServiceLinkValidator.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void pushAddSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Spezialisierung spezialisierung = SpCreator.getInstance().getMin(testcasename, leistung.getId());
        spezialisierung.getModulSpezialisierungen().clear();
        spezialisierung
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(Textmodul.builder().leikaCode("03").inhalt(spezTextmodulInhalt).build())
                    .build()
            );
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(file, distributor);

        String ars = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateId(leistung.getFullId()).validateName(spezTextmodulInhalt);

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(responseList);
        validatorListSP.validateContains(leistung.getFullId());
    }

    @Test
    void pushAddLBandZSOE(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = LbCreator.getInstance().getMin(testcasename);
        file.getLeistungsbeschreibungen().add(leistung);
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zustaendigkeit = ZsOrganisationseinheitCreator.getInstance().getMin(
            leistung.getId(),
            organisationseinheit.getId()
        );
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeit);
        ImportUtil.pushAndWait(file, distributor);

        String ars = zustaendigkeit.getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIds(0, organisationseinheit.getFullId());
    }

    @Test
    void pushAddLBandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).od(1).getMin();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = LbCreator.getInstance().getMin(testcasename);
        file.getLeistungsbeschreibungen().add(leistung);
        Zustaendigkeit zustaendigkeit = ZsOnlinedienstCreator.getInstance().getMin(
            leistung.getId(),
            onlinedienst.getId()
        );
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeit);
        ImportUtil.pushAndWait(file, distributor);

        String ars = zustaendigkeit.getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateId(leistung.getFullId());
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(response);
        onlineServiceLinkValidator.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void pushAddLBandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = LbCreator.getInstance().getMin(testcasename);
        file.getLeistungsbeschreibungen().add(leistung);
        Spezialisierung spezialisierung = SpCreator.getInstance().getMin(testcasename, leistung.getId());
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(file, distributor);

        // Da eine LB ohne Zustaendigkeit nur ueber die MonitoringAPI sichtbar ist wird diese zur validierung benutzt
        Response responseLB = MonitoringConnector.getBo("lb");
        MonitoringValidator validatorLB = new MonitoringValidator(responseLB);
        validatorLB.validateContainsId(leistung.getFullId());

        Response responseSP = MonitoringConnector.getBo("sp");
        MonitoringValidator validatorSP = new MonitoringValidator(responseSP);
        validatorSP.validateContainsId(spezialisierung.getFullId());
    }

    @Test
    void pushAddOEandZSOE(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = OeCreator.getInstance().getMin(testcasename);
        file.getOrganisationseinheiten().add(organisationseinheit);
        Zustaendigkeit zustaendigkeit = ZsOrganisationseinheitCreator.getInstance().getMin(
            leistung.getId(),
            organisationseinheit.getId()
        );
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeit);
        ImportUtil.pushAndWait(file, distributor);

        String ars = zustaendigkeit.getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(2)
            .validateOrganisationIdsContains(organisationseinheit.getFullId())
            .validateOrganisationIdsContains(fileSetup.getOrganisationseinheiten().getFirst().getFullId());
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        OrganisationValidator validatorOE = new OrganisationValidator(responseOE);
        validatorOE.validateTitle(organisationseinheit.getTitel());
    }

    @Test
    void pushAddZSOEandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zustaendigkeitOD = ZsOnlinedienstCreator.getInstance().getMin(
            leistung.getId(),
            onlinedienst.getId(),
            2
        );
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        Zustaendigkeit zustaendigkeitOE = ZsOrganisationseinheitCreator.getInstance().getMin(
            leistung.getId(),
            organisationseinheit.getId(),
            2
        );
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        ImportUtil.pushAndWait(file, distributor);

        String ars = zustaendigkeitOE.getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIds(0, organisationseinheit.getFullId());
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(response);
        onlineServiceLinkValidator.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void pushAddZSOEandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zustaendigkeit = ZsOrganisationseinheitCreator.getInstance().getMin(
            leistung.getId(),
            organisationseinheit.getId(),
            2
        );
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeit);
        Spezialisierung spezialisierung = SpCreator.getInstance().getMin(testcasename, leistung.getId());
        spezialisierung.getModulSpezialisierungen().clear();
        spezialisierung
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(Textmodul.builder().leikaCode("03").inhalt(spezTextmodulInhalt).build())
                    .build()
            );
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(file, distributor);

        String arsSP = spezialisierung.getArs().getFirst();
        Response responseSP = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsSP, leistung.getFullId());
        ServiceDescriptionValidator validatorSP = new ServiceDescriptionValidator(responseSP);
        validatorSP.validateId(leistung.getFullId()).validateName(spezTextmodulInhalt);

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(arsSP, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());

        String ars = zustaendigkeit.getArss().getFirst();
        Response responseZS = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validatorZS = new ServiceDescriptionValidator(responseZS);
        validatorZS
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIds(0, organisationseinheit.getFullId());
    }

    @Test
    void pushAddODandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Onlinedienst onlinedienst = OdCreator.getInstance().getMin(testcasename);
        onlinedienst.setBezeichnung(odBezeichnung);
        file.getOnlinedienste().add(onlinedienst);
        Zustaendigkeit zustaendigkeitOD = ZsOnlinedienstCreator.getInstance().getMin(
            leistung.getId(),
            onlinedienst.getId(),
            2
        );
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        ImportUtil.pushAndWait(file, distributor);

        String ars = zustaendigkeitOD.getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(response);
        onlineServiceLinkValidator.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void pushAddZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Zustaendigkeit zustaendigkeitOD = ZsOnlinedienstCreator.getInstance().getMin(
            leistung.getId(),
            onlinedienst.getId(),
            2
        );
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        Spezialisierung spezialisierung = SpCreator.getInstance().getMin(testcasename, leistung.getId());
        spezialisierung.getModulSpezialisierungen().clear();
        spezialisierung
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(Textmodul.builder().leikaCode("03").inhalt(spezTextmodulInhalt).build())
                    .build()
            );
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(file, distributor);

        String arsSP = spezialisierung.getArs().getFirst();
        Response responseSP = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsSP, leistung.getFullId());
        ServiceDescriptionValidator validatorSP = new ServiceDescriptionValidator(responseSP);
        validatorSP.validateId(leistung.getFullId()).validateName(spezTextmodulInhalt);

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(arsSP, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());

        String ars = zustaendigkeitOD.getArss().getFirst();
        Response responseZS = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(responseZS);
        onlineServiceLinkValidator.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void pushAddLBandOEandZSOE(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Testcase *****
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = file.getOrganisationseinheiten().getFirst();
        ImportUtil.pushAndWait(file, distributor);

        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIds(0, organisationseinheit.getFullId());
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        OrganisationValidator validatorOE = new OrganisationValidator(responseOE);
        validatorOE.validateTitle(organisationseinheit.getTitel());
    }

    @Test
    void pushAddLBandZSOEandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = LbCreator.getInstance().getMin(testcasename);
        file.getLeistungsbeschreibungen().add(leistung);
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zustaendigkeitOE = ZsOrganisationseinheitCreator.getInstance().getMin(
            leistung.getId(),
            organisationseinheit.getId(),
            2
        );
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        Zustaendigkeit zustaendigkeitOD = ZsOnlinedienstCreator.getInstance().getMin(
            leistung.getId(),
            onlinedienst.getId(),
            2
        );
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        ImportUtil.pushAndWait(file, distributor);

        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIds(0, organisationseinheit.getFullId());
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(response);
        onlineServiceLinkValidator.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        String arsOD = file.getZustaendigkeitenOnlinedienst().getFirst().getArss().getFirst();
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void pushAddLBandZSOEandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = LbCreator.getInstance().getMin(testcasename);
        file.getLeistungsbeschreibungen().add(leistung);
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zustaendigkeit = ZsOrganisationseinheitCreator.getInstance().getMin(
            leistung.getId(),
            organisationseinheit.getId()
        );
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeit);
        Spezialisierung spezialisierung = SpCreator.getInstance().getMin(testcasename, leistung.getId());
        spezialisierung.getModulSpezialisierungen().clear();
        spezialisierung
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(Textmodul.builder().leikaCode("03").inhalt(spezTextmodulInhalt).build())
                    .build()
            );
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(file, distributor);

        String arsSP = spezialisierung.getArs().getFirst();
        Response responseSP = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsSP, leistung.getFullId());
        ServiceDescriptionValidator validatorSP = new ServiceDescriptionValidator(responseSP);
        validatorSP.validateId(leistung.getFullId()).validateName(spezTextmodulInhalt);

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(arsSP, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());

        String ars = zustaendigkeit.getArss().getFirst();
        Response responseZS = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(responseZS);
        validator
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIds(0, organisationseinheit.getFullId());
    }

    @Test
    void pushAddLBandODandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).od(1).getMin();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = LbCreator.getInstance().getMin(testcasename);
        file.getLeistungsbeschreibungen().add(leistung);
        file.getOnlinedienste().add(onlinedienst);
        Zustaendigkeit zustaendigkeit = ZsOnlinedienstCreator.getInstance().getMin(
            leistung.getId(),
            onlinedienst.getId(),
            2
        );
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeit);
        ImportUtil.pushAndWait(file, distributor);

        String ars = file.getZustaendigkeitenOnlinedienst().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateId(leistung.getFullId());
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(response);
        onlineServiceLinkValidator.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void pushAddLBandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).od(1).getMin();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = LbCreator.getInstance().getMin(testcasename);
        file.getLeistungsbeschreibungen().add(leistung);
        Zustaendigkeit zustaendigkeit = ZsOnlinedienstCreator.getInstance().getMin(
            leistung.getId(),
            onlinedienst.getId()
        );
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeit);
        Spezialisierung spezialisierung = SpCreator.getInstance().getMin(testcasename, leistung.getId());
        spezialisierung.getModulSpezialisierungen().clear();
        spezialisierung
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(Textmodul.builder().leikaCode("03").inhalt(spezTextmodulInhalt).build())
                    .build()
            );
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(file, distributor);

        String arsSP = spezialisierung.getArs().getFirst();
        Response responseSP = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsSP, leistung.getFullId());
        ServiceDescriptionValidator validatorSP = new ServiceDescriptionValidator(responseSP);
        validatorSP.validateId(leistung.getFullId()).validateName(spezTextmodulInhalt);

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(arsSP, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());

        String ars = zustaendigkeit.getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateId(leistung.getFullId());
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(response);
        onlineServiceLinkValidator.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        String arsOD = file.getZustaendigkeitenOnlinedienst().getFirst().getArss().getFirst();
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void pushAddOEandZSOEandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = OeCreator.getInstance().getMin(testcasename);
        file.getOrganisationseinheiten().add(organisationseinheit);
        Zustaendigkeit zustaendigkeitOE = ZsOrganisationseinheitCreator.getInstance().getMin(
            leistung.getId(),
            organisationseinheit.getId()
        );
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        Zustaendigkeit zustaendigkeitOD = ZsOnlinedienstCreator.getInstance().getMin(
            leistung.getId(),
            onlinedienst.getId(),
            2
        );
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        ImportUtil.pushAndWait(file, distributor);

        String ars = zustaendigkeitOE.getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(2)
            .validateOrganisationIdsContains(organisationseinheit.getFullId())
            .validateOrganisationIdsContains(file.getOrganisationseinheiten().getFirst().getFullId());
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(response);
        onlineServiceLinkValidator.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(responseOD);
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void pushAddOEandZSOEandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = OeCreator.getInstance().getMin(testcasename);
        file.getOrganisationseinheiten().add(organisationseinheit);
        Zustaendigkeit zustaendigkeit = ZsOrganisationseinheitCreator.getInstance().getMin(
            leistung.getId(),
            organisationseinheit.getId()
        );
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeit);
        Spezialisierung spezialisierung = SpCreator.getInstance().getMin(testcasename, leistung.getId());
        spezialisierung.getModulSpezialisierungen().clear();
        spezialisierung
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(Textmodul.builder().leikaCode("03").inhalt(spezTextmodulInhalt).build())
                    .build()
            );
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(file, distributor);

        String ars = zustaendigkeit.getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateName(spezTextmodulInhalt)
            .validateOrganisationIdsCount(2)
            .validateOrganisationIdsContains(organisationseinheit.getFullId())
            .validateOrganisationIdsContains(file.getOrganisationseinheiten().getFirst().getFullId());

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());

        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        OrganisationValidator validatorOE = new OrganisationValidator(responseOE);
        validatorOE.validateTitle(organisationseinheit.getTitel());
    }

    @Test
    void pushAddZSOEandODandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        Onlinedienst onlinedienst = OdCreator.getInstance().getMin(testcasename);
        onlinedienst.setBezeichnung(odBezeichnung);
        file.getOnlinedienste().add(onlinedienst);
        Zustaendigkeit zustaendigkeitOD = ZsOnlinedienstCreator.getInstance().getMin(
            leistung.getId(),
            onlinedienst.getId(),
            2
        );
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        Zustaendigkeit zustaendigkeitOE = ZsOrganisationseinheitCreator.getInstance().getMin(
            leistung.getId(),
            organisationseinheit.getId(),
            2
        );
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        ImportUtil.pushAndWait(file, distributor);

        String ars = zustaendigkeitOE.getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(response);
        onlineServiceLinkValidator.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(responseOD);
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void pushAddZSOEandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zustaendigkeitOD = ZsOnlinedienstCreator.getInstance().getMin(
            leistung.getId(),
            onlinedienst.getId(),
            2
        );
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        Zustaendigkeit zustaendigkeitOE = ZsOrganisationseinheitCreator.getInstance().getMin(
            leistung.getId(),
            organisationseinheit.getId(),
            2
        );
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        Spezialisierung spezialisierung = SpCreator.getInstance().getMin(testcasename, leistung.getId());
        spezialisierung.getModulSpezialisierungen().clear();
        spezialisierung
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(Textmodul.builder().leikaCode("03").inhalt(spezTextmodulInhalt).build())
                    .build()
            );
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(file, distributor);

        String arsSP = spezialisierung.getArs().getFirst();
        Response responseSP = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsSP, leistung.getFullId());
        ServiceDescriptionValidator validatorSP = new ServiceDescriptionValidator(responseSP);
        validatorSP.validateId(leistung.getFullId()).validateName(spezTextmodulInhalt);

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(arsSP, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());

        String arsZS = zustaendigkeitOE.getArss().getFirst();
        Response responseZS = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsZS, leistung.getFullId());
        ServiceDescriptionValidator validatorZS = new ServiceDescriptionValidator(responseZS);
        validatorZS
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(responseZS);
        onlineServiceLinkValidator.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        String arsZSOD = zustaendigkeitOD.getArss().getFirst();
        Response responseOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsZSOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(responseOD);
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void pushAddODandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Onlinedienst onlinedienst = OdCreator.getInstance().getMin(testcasename);
        onlinedienst.setBezeichnung(odBezeichnung);
        file.getOnlinedienste().add(onlinedienst);
        Zustaendigkeit zustaendigkeitOD = ZsOnlinedienstCreator.getInstance().getMin(
            leistung.getId(),
            onlinedienst.getId(),
            2
        );
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        Spezialisierung spezialisierung = SpCreator.getInstance().getMin(testcasename, leistung.getId());
        spezialisierung.getModulSpezialisierungen().clear();
        spezialisierung
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(Textmodul.builder().leikaCode("03").inhalt(spezTextmodulInhalt).build())
                    .build()
            );
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(file, distributor);

        String arsSP = spezialisierung.getArs().getFirst();
        Response responseSP = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsSP, leistung.getFullId());
        ServiceDescriptionValidator validatorSP = new ServiceDescriptionValidator(responseSP);
        validatorSP.validateId(leistung.getFullId()).validateName(spezTextmodulInhalt);

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(arsSP, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());

        String arsZS = zustaendigkeitOD.getArss().getFirst();
        Response responseZS = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsZS, leistung.getFullId());
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(responseZS);
        onlineServiceLinkValidator.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsZS, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(responseOD);
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void pushAddLBandOEandZSOEandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = LbCreator.getInstance().getMin(testcasename);
        Organisationseinheit organisationseinheit = OeCreator.getInstance().getMin(testcasename);
        file.getLeistungsbeschreibungen().add(leistung);
        file.getOrganisationseinheiten().add(organisationseinheit);
        Zustaendigkeit zustaendigkeitOE = ZsOrganisationseinheitCreator.getInstance().getMin(
            leistung.getId(),
            organisationseinheit.getId()
        );
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        Zustaendigkeit zustaendigkeitOD = ZsOnlinedienstCreator.getInstance().getMin(
            leistung.getId(),
            onlinedienst.getId()
        );
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        ImportUtil.pushAndWait(file, distributor);

        String ars = zustaendigkeitOE.getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(response);
        onlineServiceLinkValidator.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        OrganisationValidator validatorOE = new OrganisationValidator(responseOE);
        validatorOE.validateTitle(organisationseinheit.getTitel());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void pushAddLBandOEandZSOEandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";

        //***** Testcase *****
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).sp(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = file.getOrganisationseinheiten().getFirst();
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        spezialisierung.getModulSpezialisierungen().clear();
        spezialisierung
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(Textmodul.builder().leikaCode("03").inhalt(spezTextmodulInhalt).build())
                    .build()
            );
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(file, distributor);

        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateName(spezTextmodulInhalt)
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());

        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        OrganisationValidator validatorOE = new OrganisationValidator(responseOE);
        validatorOE.validateTitle(organisationseinheit.getTitel());
    }

    @Test
    void pushAddLBandZSOEandODandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = LbCreator.getInstance().getMin(testcasename);
        file.getLeistungsbeschreibungen().add(leistung);
        Onlinedienst onlinedienst = OdCreator.getInstance().getMin(testcasename);
        onlinedienst.setBezeichnung(odBezeichnung);
        file.getOnlinedienste().add(onlinedienst);
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zustaendigkeitOE = ZsOrganisationseinheitCreator.getInstance().getMin(
            leistung.getId(),
            organisationseinheit.getId(),
            2
        );
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        Zustaendigkeit zustaendigkeitOD = ZsOnlinedienstCreator.getInstance().getMin(
            leistung.getId(),
            onlinedienst.getId(),
            2
        );
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        ImportUtil.pushAndWait(file, distributor);

        String arsOE = zustaendigkeitOE.getArss().getFirst();
        Response responseOE = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOE, leistung.getFullId());
        ServiceDescriptionValidator validatorOE = new ServiceDescriptionValidator(responseOE);
        validatorOE
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseOD = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOD, leistung.getFullId());
        OnlineServiceLinkValidator validatorOD = new OnlineServiceLinkValidator(responseOD);
        validatorOD.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void pushAddLBandZSOEandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = LbCreator.getInstance().getMin(testcasename);
        Leistungsbeschreibung leistungSP = fileSetup.getLeistungsbeschreibungen().getFirst();
        file.getLeistungsbeschreibungen().add(leistung);
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zustaendigkeitOE = ZsOrganisationseinheitCreator.getInstance().getMin(
            leistung.getId(),
            organisationseinheit.getId(),
            2
        );
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        Zustaendigkeit zustaendigkeitOD = ZsOnlinedienstCreator.getInstance().getMin(
            leistung.getId(),
            onlinedienst.getId(),
            2
        );
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        Spezialisierung spezialisierung = SpCreator.getInstance().getMin(testcasename, leistungSP.getId());
        spezialisierung.getModulSpezialisierungen().clear();
        spezialisierung
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(Textmodul.builder().leikaCode("03").inhalt(spezTextmodulInhalt).build())
                    .build()
            );
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(file, distributor);

        String arsZS = zustaendigkeitOE.getArss().getFirst();
        Response responseZS = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsZS, leistung.getFullId());
        ServiceDescriptionValidator validatorZS = new ServiceDescriptionValidator(responseZS);
        validatorZS
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());
        OnlineServiceLinkValidator validatorOD = new OnlineServiceLinkValidator(responseZS);
        validatorOD.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());
        String arsSP = spezialisierung.getArs().getFirst();
        Response responseSP = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsSP, leistungSP.getFullId());
        ServiceDescriptionValidator validatorSP = new ServiceDescriptionValidator(responseSP);
        validatorSP.validateId(leistungSP.getFullId()).validateName(spezTextmodulInhalt);

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(arsSP, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistungSP.getFullId());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void pushAddLBandODandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).od(1).sp(1).getMin();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        file.getOnlinedienste().add(onlinedienst);
        Zustaendigkeit zustaendigkeit = ZsOnlinedienstCreator.getInstance().getMin(
            leistung.getId(),
            onlinedienst.getId()
        );
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeit);

        Spezialisierung spezialisierung = fileSetup.getSpezialisierungen().getFirst();
        spezialisierung.getModulSpezialisierungen().clear();
        spezialisierung
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(Textmodul.builder().leikaCode("03").inhalt(spezTextmodulInhalt).build())
                    .build()
            );
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(file, distributor);

        String ars = file.getZustaendigkeitenOnlinedienst().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateId(leistung.getFullId()).validateName(spezTextmodulInhalt).validateOrganisationIdsCount(0);

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());

        OnlineServiceLinkValidator validatorOD = new OnlineServiceLinkValidator(response);
        validatorOD.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void pushAddOEandZSOEandODandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Onlinedienst onlinedienst = OdCreator.getInstance().getMin(testcasename);
        onlinedienst.setBezeichnung(odBezeichnung);
        Organisationseinheit organisationseinheit = OeCreator.getInstance().getMin(testcasename);
        file.getOrganisationseinheiten().add(organisationseinheit);
        file.getOnlinedienste().add(onlinedienst);
        Zustaendigkeit zustaendigkeitOE = ZsOrganisationseinheitCreator.getInstance().getMin(
            leistung.getId(),
            organisationseinheit.getId()
        );
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        Zustaendigkeit zustaendigkeitOD = ZsOnlinedienstCreator.getInstance().getMin(
            leistung.getId(),
            onlinedienst.getId()
        );
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        ImportUtil.pushAndWait(file, distributor);

        String ars = zustaendigkeitOE.getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(2)
            .validateOrganisationIdsContains(organisationseinheit.getFullId())
            .validateOrganisationIdsContains(fileSetup.getOrganisationseinheiten().getFirst().getFullId());
        OnlineServiceLinkValidator validatorOD = new OnlineServiceLinkValidator(response);
        validatorOD.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        OrganisationValidator validatorOE = new OrganisationValidator(responseOE);
        validatorOE.validateTitle(organisationseinheit.getTitel());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void pushAddOEandZSOEandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = OeCreator.getInstance().getMin(testcasename);
        file.getOrganisationseinheiten().add(organisationseinheit);
        Zustaendigkeit zustaendigkeitOE = ZsOrganisationseinheitCreator.getInstance().getMin(
            leistung.getId(),
            organisationseinheit.getId()
        );
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        Zustaendigkeit zustaendigkeitOD = ZsOnlinedienstCreator.getInstance().getMin(
            leistung.getId(),
            onlinedienst.getId(),
            2
        );
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        Spezialisierung spezialisierung = SpCreator.getInstance().getMin(testcasename, leistung.getId());
        spezialisierung.getModulSpezialisierungen().clear();
        spezialisierung
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(Textmodul.builder().leikaCode("03").inhalt(spezTextmodulInhalt).build())
                    .build()
            );
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(file, distributor);

        String ars = zustaendigkeitOE.getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateName(spezTextmodulInhalt)
            .validateOrganisationIdsCount(2)
            .validateOrganisationIdsContains(organisationseinheit.getFullId())
            .validateOrganisationIdsContains(fileSetup.getOrganisationseinheiten().getFirst().getFullId());

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());

        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseOD = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOD, leistung.getFullId());
        OnlineServiceLinkValidator validatorOD = new OnlineServiceLinkValidator(responseOD);
        validatorOD.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        OrganisationValidator validatorOE = new OrganisationValidator(responseOE);
        validatorOE.validateTitle(organisationseinheit.getTitel());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void pushAddZSOEandODandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        Onlinedienst onlinedienst = OdCreator.getInstance().getMin(testcasename);
        onlinedienst.setBezeichnung(odBezeichnung);
        file.getOnlinedienste().add(onlinedienst);
        Zustaendigkeit zustaendigkeitOD = ZsOnlinedienstCreator.getInstance().getMin(
            leistung.getId(),
            onlinedienst.getId()
        );
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        Zustaendigkeit zustaendigkeitOE = ZsOrganisationseinheitCreator.getInstance().getMin(
            leistung.getId(),
            organisationseinheit.getId(),
            2
        );
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        Spezialisierung spezialisierung = SpCreator.getInstance().getMin(testcasename, leistung.getId());
        spezialisierung.getModulSpezialisierungen().clear();
        spezialisierung
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(Textmodul.builder().leikaCode("03").inhalt(spezTextmodulInhalt).build())
                    .build()
            );
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(file, distributor);

        String ars = zustaendigkeitOD.getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateId(leistung.getFullId()).validateName(spezTextmodulInhalt);

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());

        OnlineServiceLinkValidator validatorOD = new OnlineServiceLinkValidator(response);
        validatorOD.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());
        String arsOE = zustaendigkeitOD.getArss().getFirst();
        Response responseOE = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOE, leistung.getFullId());
        ServiceDescriptionValidator validatorOE = new ServiceDescriptionValidator(responseOE);
        validatorOE
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());
        Response responseOE2 = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            organisationseinheit.getFullId()
        );
        OrganisationValidator validatorOE2 = new OrganisationValidator(responseOE2);
        validatorOE2.validateTitle(organisationseinheit.getTitel());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void pushAddLBandOEandZSOEandODandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        file.getLeistungsbeschreibungen().add(leistung);
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        file.getOrganisationseinheiten().add(organisationseinheit);
        file.getOnlinedienste().add(onlinedienst);
        Zustaendigkeit zustaendigkeitOE = ZsOrganisationseinheitCreator.getInstance().getMin(
            leistung.getId(),
            organisationseinheit.getId()
        );
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        Zustaendigkeit zustaendigkeitOD = ZsOnlinedienstCreator.getInstance().getMin(
            leistung.getId(),
            onlinedienst.getId()
        );
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);

        String arsOE = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOE, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());
        OnlineServiceLinkValidator validatorOD = new OnlineServiceLinkValidator(response);
        validatorOD.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        OrganisationValidator validatorOE = new OrganisationValidator(responseOE);
        validatorOE.validateTitle(organisationseinheit.getTitel());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void pushAddLBandOEandZSOEandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = LbCreator.getInstance().getMin(testcasename);
        Organisationseinheit organisationseinheit = OeCreator.getInstance().getMin(testcasename);
        file.getLeistungsbeschreibungen().add(leistung);
        file.getOrganisationseinheiten().add(organisationseinheit);
        Zustaendigkeit zustaendigkeitOE = ZsOrganisationseinheitCreator.getInstance().getMin(
            leistung.getId(),
            organisationseinheit.getId()
        );
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        Zustaendigkeit zustaendigkeitOD = ZsOnlinedienstCreator.getInstance().getMin(
            leistung.getId(),
            onlinedienst.getId(),
            2
        );
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        Spezialisierung spezialisierung = SpCreator.getInstance().getMin(testcasename, leistung.getId());
        spezialisierung.getModulSpezialisierungen().clear();
        spezialisierung
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(Textmodul.builder().leikaCode("03").inhalt(spezTextmodulInhalt).build())
                    .build()
            );
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(file, distributor);

        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateName(spezTextmodulInhalt)
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());

        String arsOD = file.getZustaendigkeitenOnlinedienst().getFirst().getArss().getFirst();
        Response responseOD = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOD, leistung.getFullId());
        OnlineServiceLinkValidator validatorOD = new OnlineServiceLinkValidator(responseOD);
        validatorOD.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        OrganisationValidator validatorOE = new OrganisationValidator(responseOE);
        validatorOE.validateTitle(organisationseinheit.getTitel());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void pushAddLBandZSOEandODandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = LbCreator.getInstance().getMin(testcasename);
        file.getLeistungsbeschreibungen().add(leistung);
        Onlinedienst onlinedienst = OdCreator.getInstance().getMin(testcasename);
        onlinedienst.setBezeichnung(odBezeichnung);
        file.getOnlinedienste().add(onlinedienst);
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zustaendigkeitOE = ZsOrganisationseinheitCreator.getInstance().getMin(
            leistung.getId(),
            organisationseinheit.getId()
        );
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        Zustaendigkeit zustaendigkeitOD = ZsOnlinedienstCreator.getInstance().getMin(
            leistung.getId(),
            onlinedienst.getId()
        );
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        Spezialisierung spezialisierung = SpCreator.getInstance().getMin(testcasename, leistung.getId());
        spezialisierung.getModulSpezialisierungen().clear();
        spezialisierung
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(Textmodul.builder().leikaCode("03").inhalt(spezTextmodulInhalt).build())
                    .build()
            );
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(file, distributor);

        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateName(spezTextmodulInhalt)
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());

        OnlineServiceLinkValidator validatorOD = new OnlineServiceLinkValidator(response);
        validatorOD.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void pushAddOEandZSOEandODandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Onlinedienst onlinedienst = OdCreator.getInstance().getMin(testcasename);
        onlinedienst.setBezeichnung(odBezeichnung);
        Organisationseinheit organisationseinheit = OeCreator.getInstance().getMin(testcasename);
        Zustaendigkeit zustaendigkeitOE = ZsOrganisationseinheitCreator.getInstance().getMin(
            leistung.getId(),
            organisationseinheit.getId(),
            2
        );
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        Zustaendigkeit zustaendigkeitOD = ZsOnlinedienstCreator.getInstance().getMin(
            leistung.getId(),
            onlinedienst.getId(),
            2
        );
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        file.getOrganisationseinheiten().add(organisationseinheit);
        file.getOnlinedienste().add(onlinedienst);
        Spezialisierung spezialisierung = SpCreator.getInstance().getMin(testcasename, leistung.getId());
        spezialisierung.getModulSpezialisierungen().clear();
        spezialisierung
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(Textmodul.builder().leikaCode("03").inhalt(spezTextmodulInhalt).build())
                    .build()
            );
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(file, distributor);

        String ars = zustaendigkeitOE.getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());
        OnlineServiceLinkValidator validatorOD = new OnlineServiceLinkValidator(response);
        validatorOD.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());
        String arsSP = spezialisierung.getArs().getFirst();
        Response responseSP = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsSP, leistung.getFullId());
        ServiceDescriptionValidator validatorSP = new ServiceDescriptionValidator(responseSP);
        validatorSP.validateId(leistung.getFullId()).validateName(spezTextmodulInhalt);

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(arsSP, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());

        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        OrganisationValidator validatorOE = new OrganisationValidator(responseOE);
        validatorOE.validateTitle(organisationseinheit.getTitel());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void pushAddLBandOEandZSOEandODandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).sp(1).getMin();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait(fileSetup, distributor);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        file.getLeistungsbeschreibungen().add(leistung);
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        file.getOrganisationseinheiten().add(organisationseinheit);
        file.getOnlinedienste().add(onlinedienst);
        Zustaendigkeit zustaendigkeitOE = ZsOrganisationseinheitCreator.getInstance().getMin(
            leistung.getId(),
            organisationseinheit.getId()
        );
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        Zustaendigkeit zustaendigkeitOD = ZsOnlinedienstCreator.getInstance().getMin(
            leistung.getId(),
            onlinedienst.getId()
        );
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        Spezialisierung spezialisierung = fileSetup.getSpezialisierungen().getFirst();
        spezialisierung.getModulSpezialisierungen().clear();
        spezialisierung
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(Textmodul.builder().leikaCode("03").inhalt(spezTextmodulInhalt).build())
                    .build()
            );
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(file, distributor);

        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateName(spezTextmodulInhalt)
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());

        OnlineServiceLinkValidator validatorOD = new OnlineServiceLinkValidator(response);
        validatorOD.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        OrganisationValidator validatorOE = new OrganisationValidator(responseOE);
        validatorOE.validateTitle(organisationseinheit.getTitel());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }
}
