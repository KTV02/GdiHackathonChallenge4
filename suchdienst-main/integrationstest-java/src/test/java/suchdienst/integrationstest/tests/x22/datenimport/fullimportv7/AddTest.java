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
package suchdienst.integrationstest.tests.x22.datenimport.fullimportv7;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.LbCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.OdCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.OeCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.SpCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsOnlinedienstCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsOrganisationseinheitCreator;
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
import suchdienst.integrationstest.connectors.OrganisationunitsConnector;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.OrganisationValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.OnlineServiceLinkValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.ServiceDescriptionValidator;
import suchdienst.integrationstest.validator.servicedescriptionv8.ServiceDescriptionResultListValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class AddTest {

    @Test
    void fullAddZSOE(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup);

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
        ImportUtil.pushAndWait(file);

        String ars = zustaendigkeit.getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIds(0, organisationseinheit.getFullId());
    }

    @Test
    void fullAddZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait(fileSetup);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();

        Zustaendigkeit zustaendigkeitOD = ZsOnlinedienstCreator.getInstance().getMin(
            leistung.getId(),
            onlinedienst.getId(),
            2
        );
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        ImportUtil.pushAndWait(file);

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
    void fullAddSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup);

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
        ImportUtil.pushAndWait(file);

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
    void fullAddLBandZSOE(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup);

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
        ImportUtil.pushAndWait(file);

        String ars = zustaendigkeit.getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIds(0, organisationseinheit.getFullId());
    }

    @Test
    void fullAddLBandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).od(1).getMin();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait(fileSetup);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = LbCreator.getInstance().getMin(testcasename);
        file.getLeistungsbeschreibungen().add(leistung);
        Zustaendigkeit zustaendigkeit = ZsOnlinedienstCreator.getInstance().getMin(
            leistung.getId(),
            onlinedienst.getId()
        );
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeit);
        ImportUtil.pushAndWait(file);

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
    void fullAddOEandZSOE(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup);

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
        ImportUtil.pushAndWait(file);

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
    void fullAddZSOEandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait(fileSetup);

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
        ImportUtil.pushAndWait(file);

        String arsOE = zustaendigkeitOE.getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOE, leistung.getFullId());
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
    void fullAddZSOEandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup);

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
        ImportUtil.pushAndWait(file);

        String arsSP = spezialisierung.getArs().getFirst();
        Response responseSP = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsSP, leistung.getFullId());
        ServiceDescriptionValidator validatorSP = new ServiceDescriptionValidator(responseSP);
        validatorSP.validateId(leistung.getFullId()).validateName(spezTextmodulInhalt);

        String ars = zustaendigkeit.getArss().getFirst();
        Response responseZS = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validatorZS = new ServiceDescriptionValidator(responseZS);
        validatorZS
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIds(0, organisationseinheit.getFullId());

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(arsSP, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());
    }

    @Test
    void fullAddODandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup);

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
        ImportUtil.pushAndWait(file);

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
    void fullAddZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait(fileSetup);

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
        ImportUtil.pushAndWait(file);

        String arsSP = spezialisierung.getArs().getFirst();
        Response responseSP = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsSP, leistung.getFullId());
        ServiceDescriptionValidator validatorSP = new ServiceDescriptionValidator(responseSP);
        validatorSP.validateId(leistung.getFullId()).validateName(spezTextmodulInhalt);

        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseZS = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOD, leistung.getFullId());
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(responseZS);
        onlineServiceLinkValidator.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(arsSP, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(responseList);
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void fullAddLBandOEandZSOE(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Testcase *****
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = file.getOrganisationseinheiten().getFirst();
        ImportUtil.pushAndWait(file);

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
    void fullAddLBandZSOEandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait(fileSetup);

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
        ImportUtil.pushAndWait(file);

        String arsOE = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOE, leistung.getFullId());
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
    void fullAddLBandZSOEandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup);

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
        ImportUtil.pushAndWait(file);

        String arsSP = spezialisierung.getArs().getFirst();
        Response responseSP = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsSP, leistung.getFullId());
        ServiceDescriptionValidator validatorSP = new ServiceDescriptionValidator(responseSP);
        validatorSP.validateId(leistung.getFullId()).validateName(spezTextmodulInhalt);

        String ars = zustaendigkeit.getArss().getFirst();
        Response responseZS = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(responseZS);
        validator
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIds(0, organisationseinheit.getFullId());

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(arsSP, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());
    }

    @Test
    void fullAddOEandZSOEandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait(fileSetup);

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
        ImportUtil.pushAndWait(file);

        String arsOE = zustaendigkeitOE.getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOE, leistung.getFullId());
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
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void fullAddOEandZSOEandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup);

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
        ImportUtil.pushAndWait(file);

        String ars = zustaendigkeit.getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateName(spezTextmodulInhalt)
            .validateOrganisationIdsCount(2)
            .validateOrganisationIdsContains(organisationseinheit.getFullId())
            .validateOrganisationIdsContains(file.getOrganisationseinheiten().getFirst().getFullId());

        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        OrganisationValidator validatorOE = new OrganisationValidator(responseOE);
        validatorOE.validateTitle(organisationseinheit.getTitel());

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());
    }

    @Test
    void fullAddLBandODandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Testcase *****
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).od(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Onlinedienst onlinedienst = file.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);

        ImportUtil.pushAndWait(file);

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
    void fullAddZSOEandODandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup);

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
        ImportUtil.pushAndWait(file);

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
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void fullAddZSOEandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait(fileSetup);

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
        ImportUtil.pushAndWait(file);

        String arsSP = spezialisierung.getArs().getFirst();
        Response responseSP = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsSP, leistung.getFullId());
        ServiceDescriptionValidator validatorSP = new ServiceDescriptionValidator(responseSP);
        validatorSP.validateId(leistung.getFullId()).validateName(spezTextmodulInhalt);

        String arsOE = zustaendigkeitOE.getArss().getFirst();
        Response responseZS = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOE, leistung.getFullId());
        ServiceDescriptionValidator validatorZS = new ServiceDescriptionValidator(responseZS);
        validatorZS
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(responseZS);
        onlineServiceLinkValidator.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(arsSP, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void fullAddODandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup);

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
        ImportUtil.pushAndWait(file);

        String arsSP = spezialisierung.getArs().getFirst();
        Response responseSP = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsSP, leistung.getFullId());
        ServiceDescriptionValidator validatorSP = new ServiceDescriptionValidator(responseSP);
        validatorSP.validateId(leistung.getFullId()).validateName(spezTextmodulInhalt);

        String arsZS = zustaendigkeitOD.getArss().getFirst();
        Response responseZS = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsZS, leistung.getFullId());
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(responseZS);
        onlineServiceLinkValidator.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(arsSP, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void fullAddLBandOEandZSOEandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait(fileSetup);

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
        ImportUtil.pushAndWait(file);

        String arsOE = zustaendigkeitOE.getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOE, leistung.getFullId());
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
    void fullAddLBandOEandZSOEandSP(TestInfo testInfo) {
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
        ImportUtil.pushAndWait(file);

        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateName(spezTextmodulInhalt)
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());

        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        OrganisationValidator validatorOE = new OrganisationValidator(responseOE);
        validatorOE.validateTitle(organisationseinheit.getTitel());

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());
    }

    @Test
    void fullAddLBandZSOEandODandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup);

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
        ImportUtil.pushAndWait(file);

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
    void fullAddLBandODandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Testcase *****
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).od(1).sp(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Onlinedienst onlinedienst = file.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
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
        ImportUtil.pushAndWait(file);

        String ars = file.getZustaendigkeitenOnlinedienst().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateId(leistung.getFullId()).validateName(spezTextmodulInhalt).validateOrganisationIdsCount(0);

        OnlineServiceLinkValidator validatorOD = new OnlineServiceLinkValidator(response);
        validatorOD.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void fullAddLBandZSOEandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait(fileSetup);

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
        ImportUtil.pushAndWait(file);

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
    void fullAddOEandZSOEandODandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup);

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
        ImportUtil.pushAndWait(file);

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
    void fullAddOEandZSOEandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait(fileSetup);

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
        ImportUtil.pushAndWait(file);

        String ars = zustaendigkeitOE.getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateName(spezTextmodulInhalt)
            .validateOrganisationIdsCount(2)
            .validateOrganisationIdsContains(organisationseinheit.getFullId())
            .validateOrganisationIdsContains(fileSetup.getOrganisationseinheiten().getFirst().getFullId());

        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseOD = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOD, leistung.getFullId());
        OnlineServiceLinkValidator validatorOD = new OnlineServiceLinkValidator(responseOD);
        validatorOD.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        OrganisationValidator validatorOE = new OrganisationValidator(responseOE);
        validatorOE.validateTitle(organisationseinheit.getTitel());

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void fullAddZSOEandODandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup);

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
        ImportUtil.pushAndWait(file);

        String arsSP = spezialisierung.getArs().getFirst();
        Response responseSP = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsSP, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(responseSP);
        validator.validateId(leistung.getFullId()).validateName(spezTextmodulInhalt);

        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseOD = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOD, leistung.getFullId());
        OnlineServiceLinkValidator validatorOD = new OnlineServiceLinkValidator(responseOD);
        validatorOD.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());

        String arsOE = zustaendigkeitOE.getArss().getFirst();
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

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void fullAddLBandOEandZSOEandODandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Testcase *****
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = file.getOrganisationseinheiten().getFirst();
        Onlinedienst onlinedienst = file.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait(file);

        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
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
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void fullAddLBandOEandZSOEandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait(fileSetup);

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
        ImportUtil.pushAndWait(file);

        String arsOE = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOE, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateName(spezTextmodulInhalt)
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());

        String arsOD = file.getZustaendigkeitenOnlinedienst().getFirst().getArss().getFirst();
        Response responseOD = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOD, leistung.getFullId());
        OnlineServiceLinkValidator validatorOD = new OnlineServiceLinkValidator(responseOD);
        validatorOD.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());

        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        OrganisationValidator validatorOE = new OrganisationValidator(responseOE);
        validatorOE.validateTitle(organisationseinheit.getTitel());

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOE, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void fullAddLBandZSOEandODandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup);

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
        ImportUtil.pushAndWait(file);

        String arsOE = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOE, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateName(spezTextmodulInhalt)
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());

        OnlineServiceLinkValidator validatorOD = new OnlineServiceLinkValidator(response);
        validatorOD.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        String arsSP = spezialisierung.getArs().getFirst();
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(arsSP, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void fullAddOEandZSOEandODandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup);

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
        ImportUtil.pushAndWait(file);

        String arsOE = zustaendigkeitOE.getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOE, leistung.getFullId());
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

        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        OrganisationValidator validatorOE = new OrganisationValidator(responseOE);
        validatorOE.validateTitle(organisationseinheit.getTitel());

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(arsSP, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void fullAddLBandOEandZSOEandODandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Testcase *****
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).sp(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = file.getOrganisationseinheiten().getFirst();
        Onlinedienst onlinedienst = file.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
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
        ImportUtil.pushAndWait(file);

        String arsOE = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOE, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateName(spezTextmodulInhalt)
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());

        OnlineServiceLinkValidator validatorOD = new OnlineServiceLinkValidator(response);
        validatorOD.validateCount(1).validateTitle(0, onlinedienst.getLinks().getFirst().getTitel());
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        OrganisationValidator validatorOE = new OrganisationValidator(responseOE);
        validatorOE.validateTitle(organisationseinheit.getTitel());

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        String arsSP = file.getSpezialisierungen().getFirst().getArs().getFirst();
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(arsSP, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateContains(leistung.getFullId());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        String arsOD = file.getZustaendigkeitenOnlinedienst().getFirst().getArss().getFirst();
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    /**
     * Sendet zuerst eine LB, dann einen OD und dann ZS getrennt zum Suchdienst und prüft ob die Suchbegriffe aus dem OD mit in die externalSynonyms der LB übernommen wurden.
     */
    @Test
    void fullAddLBSaveAddODSaveAddZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String searchValue = testcasename + "OdExternalSynonyms";

        Leistungsbeschreibung lb = LbCreator.getInstance().getMin(testcasename);
        Onlinedienst od = OdCreator.getInstance().getMin(testcasename);
        od.setBezeichnung(searchValue);
        Zustaendigkeit zs = ZsOnlinedienstCreator.getInstance().getMin(lb.getId(), od.getId(), 1);

        XzufiFile xzufiFileLb = new XzufiFile();
        xzufiFileLb.getLeistungsbeschreibungen().add(lb);
        ImportUtil.pushAndWait(xzufiFileLb);

        XzufiFile xzufiFileOd = new XzufiFile();
        xzufiFileOd.getOnlinedienste().add(od);
        ImportUtil.pushAndWait(xzufiFileOd);

        XzufiFile xzufiFileRelation = new XzufiFile();
        xzufiFileRelation.getZustaendigkeitenOnlinedienst().add(zs);
        ImportUtil.pushAndWait(xzufiFileRelation);

        Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(
            zs.getArss().getFirst(),
            searchValue
        );
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(responseList);
        validatorListOD.validateContains(lb.getFullId());
    }

    /**
     * Sendet OD einzeln und LB mit ZS zusammen zum Suchdienst und prüft ob die Suchbegriffe aus dem OD mit in die externalSynonyms der LB übernommen wurden.
     */
    @Test
    void fullAddLBAndODAndZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String searchValue = testcasename + "OdExternalSynonyms";

        Leistungsbeschreibung lb = LbCreator.getInstance().getMin(testcasename);
        Onlinedienst od = OdCreator.getInstance().getMin(testcasename);
        od.setBezeichnung(searchValue);
        Zustaendigkeit zs = ZsOnlinedienstCreator.getInstance().getMin(lb.getId(), od.getId(), 1);

        XzufiFile xzufi = new XzufiFile();
        xzufi.getLeistungsbeschreibungen().add(lb);
        xzufi.getOnlinedienste().add(od);
        xzufi.getZustaendigkeitenOnlinedienst().add(zs);
        ImportUtil.pushAndWait(xzufi);

        Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(
            zs.getArss().getFirst(),
            searchValue
        );
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(responseList);
        validatorListOD.validateContains(lb.getFullId());
    }

    /**
     * Sendet zuerst einen OD, dann eine LB und dann eine ZS getrennt zum Suchdienst und prüft ob die Suchbegriffe aus dem OD mit in die externalSynonyms der LB übernommen wurden.
     */
    @Test
    void fullAddODSaveAddLBSaveAddZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String searchValue = testcasename + "OdExternalSynonyms";

        Leistungsbeschreibung lb = LbCreator.getInstance().getMin(testcasename);
        Onlinedienst od = OdCreator.getInstance().getMin(testcasename);
        od.setBezeichnung(searchValue);
        Zustaendigkeit zs = ZsOnlinedienstCreator.getInstance().getMin(lb.getId(), od.getId(), 1);

        XzufiFile xzufiFileOd = new XzufiFile();
        xzufiFileOd.getOnlinedienste().add(od);
        ImportUtil.pushAndWait(xzufiFileOd);

        XzufiFile xzufiFileLb = new XzufiFile();
        xzufiFileLb.getLeistungsbeschreibungen().add(lb);
        ImportUtil.pushAndWait(xzufiFileLb);

        XzufiFile xzufiFileRelation = new XzufiFile();
        xzufiFileRelation.getZustaendigkeitenOnlinedienst().add(zs);
        ImportUtil.pushAndWait(xzufiFileRelation);

        Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(
            zs.getArss().getFirst(),
            searchValue
        );
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(responseList);
        validatorListOD.validateContains(lb.getFullId());
    }
}
