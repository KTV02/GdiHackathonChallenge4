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
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.ModulSpezialisierung;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.Spezialisierung;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulursprungsportal.UrsprungsportalModul;
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
class ModifyTest {

    @Test
    void fullModifyLB(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        leistung.getUrsprungsportale().clear();
        leistung.getUrsprungsportale().add(UrsprungsportalModul.builder().uri("www.newPortal.de").build());
        file.getLeistungsbeschreibungen().add(leistung);
        ImportUtil.pushAndWait(file);

        String ars = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateSourcePortalsCount(1)
            .validateSourcePortalUri(0, "www.newPortal.de");
    }

    @Test
    void fullModifyOE(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        organisationseinheit.setTitel("newTitle");
        file.getOrganisationseinheiten().add(organisationseinheit);
        ImportUtil.pushAndWait(file);

        Response response = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        OrganisationValidator validator = new OrganisationValidator(response);
        validator.validateTitle("newTitle");
    }

    @Test
    void fullModifyOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).od(1).getMin();
        ImportUtil.pushAndWait(fileSetup);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        onlinedienst.getLinks().getFirst().setUri("www.newUri.de");
        file.getOnlinedienste().add(onlinedienst);
        ImportUtil.pushAndWait(file);

        String ars = fileSetup.getZustaendigkeitenOnlinedienst().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(response);
        onlineServiceLinkValidator.validateCount(1).validateUri(0, onlinedienst.getLinks().getFirst().getUri());

        // Prueft ob das Synonym aus dem Onlinedienst in der Leistungsbeschreibung aktualisiert wurde.
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void fullModifySP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String spezTextmodulInhaltModify = spezTextmodulInhalt + "Modify";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).sp(1).getMin();
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
        fileSetup.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(fileSetup);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Textmodul textmodul = (Textmodul) spezialisierung.getModulSpezialisierungen().getFirst().getModul();
        textmodul.setInhalt(spezTextmodulInhaltModify);
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(file);

        String ars = spezialisierung.getArs().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateName(spezTextmodulInhaltModify);

        // Prueft ob die Synonymliste aktualisiert wurde
        Response responseListModify = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            spezTextmodulInhaltModify
        );
        ServiceDescriptionResultListValidator validatorListModifySP = new ServiceDescriptionResultListValidator(
            responseListModify
        );
        validatorListModifySP.validateContains(leistung.getFullId());

        Response responseListDelete = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListDeleteSP = new ServiceDescriptionResultListValidator(
            responseListDelete
        );
        validatorListDeleteSP.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullModifyLBandOE(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait(fileSetup);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        leistung.getUrsprungsportale().clear();
        leistung.getUrsprungsportale().add(UrsprungsportalModul.builder().uri("www.newPortal.de").build());
        file.getLeistungsbeschreibungen().add(leistung);
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        organisationseinheit.setTitel("newTitle");
        file.getOrganisationseinheiten().add(organisationseinheit);
        ImportUtil.pushAndWait(file);

        String ars = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response responseLB = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validatorLB = new ServiceDescriptionValidator(responseLB);
        validatorLB
            .validateId(leistung.getFullId())
            .validateSourcePortalsCount(1)
            .validateSourcePortalUri(0, "www.newPortal.de");

        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        OrganisationValidator validatorOE = new OrganisationValidator(responseOE);
        validatorOE.validateTitle("newTitle");
    }

    @Test
    void fullModifyLBandOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        ImportUtil.pushAndWait(fileSetup);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        leistung.getUrsprungsportale().clear();
        leistung.getUrsprungsportale().add(UrsprungsportalModul.builder().uri("www.newPortal.de").build());
        file.getLeistungsbeschreibungen().add(leistung);
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        onlinedienst.getLinks().getFirst().setUri("www.newUri.de");
        file.getOnlinedienste().add(onlinedienst);
        ImportUtil.pushAndWait(file);

        String ars = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateSourcePortalsCount(1)
            .validateSourcePortalUri(0, "www.newPortal.de");
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(response);
        onlineServiceLinkValidator.validateCount(1).validateUri(0, onlinedienst.getLinks().getFirst().getUri());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void fullModifyLBandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String spezTextmodulInhaltModify = spezTextmodulInhalt + "Modify";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).sp(1).getMin();
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
        fileSetup.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(fileSetup);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        leistung.getUrsprungsportale().clear();
        leistung.getUrsprungsportale().add(UrsprungsportalModul.builder().uri("www.newPortal.de").build());
        file.getLeistungsbeschreibungen().add(leistung);
        Textmodul textmodul = (Textmodul) spezialisierung.getModulSpezialisierungen().getFirst().getModul();
        textmodul.setInhalt(spezTextmodulInhaltModify);
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(file);

        String ars = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateName(spezTextmodulInhaltModify)
            .validateSourcePortalsCount(1)
            .validateSourcePortalUri(0, "www.newPortal.de");

        // Prueft ob die Synonymliste aktualisiert wurde
        Response responseListModify = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            spezTextmodulInhaltModify
        );
        ServiceDescriptionResultListValidator validatorListModifySP = new ServiceDescriptionResultListValidator(
            responseListModify
        );
        validatorListModifySP.validateContains(leistung.getFullId());

        Response responseListDelete = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListDeleteSP = new ServiceDescriptionResultListValidator(
            responseListDelete
        );
        validatorListDeleteSP.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullModifyOEandOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        ImportUtil.pushAndWait(fileSetup);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        organisationseinheit.setTitel("newTitle");
        file.getOrganisationseinheiten().add(organisationseinheit);
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        onlinedienst.getLinks().getFirst().setUri("www.newUri.de");
        file.getOnlinedienste().add(onlinedienst);
        ImportUtil.pushAndWait(file);

        String ars = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(response);
        onlineServiceLinkValidator.validateCount(1).validateUri(0, onlinedienst.getLinks().getFirst().getUri());
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        OrganisationValidator validatorOE = new OrganisationValidator(responseOE);
        validatorOE.validateTitle("newTitle");

        // Prueft ob das Synonym aus dem Onlinedienst in der Leistungsbeschreibung aktualisiert wurde.
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void fullModifyOEandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String spezTextmodulInhaltModify = spezTextmodulInhalt + "Modify";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).sp(1).getMin();
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
        fileSetup.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(fileSetup);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        organisationseinheit.setTitel("newTitle");
        file.getOrganisationseinheiten().add(organisationseinheit);
        Textmodul textmodul = (Textmodul) spezialisierung.getModulSpezialisierungen().getFirst().getModul();
        textmodul.setInhalt(spezTextmodulInhaltModify);
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(file);

        String ars = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateId(leistung.getFullId()).validateName(spezTextmodulInhaltModify);
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        OrganisationValidator validatorOE = new OrganisationValidator(responseOE);
        validatorOE.validateTitle("newTitle");

        // Prueft ob die Synonymliste aktualisiert wurde
        Response responseListModify = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            spezTextmodulInhaltModify
        );
        ServiceDescriptionResultListValidator validatorListModifySP = new ServiceDescriptionResultListValidator(
            responseListModify
        );
        validatorListModifySP.validateContains(leistung.getFullId());

        Response responseListDelete = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListDeleteSP = new ServiceDescriptionResultListValidator(
            responseListDelete
        );
        validatorListDeleteSP.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullModifyODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String spezTextmodulInhaltModify = spezTextmodulInhalt + "Modify";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).od(1).sp(1).getMin();
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
        fileSetup.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(fileSetup);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);

        onlinedienst.getLinks().getFirst().setUri("www.newUri.de");
        file.getOnlinedienste().add(onlinedienst);
        Textmodul textmodul = (Textmodul) spezialisierung.getModulSpezialisierungen().getFirst().getModul();
        textmodul.setInhalt(spezTextmodulInhaltModify);
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(file);

        String arsSP = fileSetup.getSpezialisierungen().getFirst().getArs().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsSP, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateId(leistung.getFullId()).validateName(spezTextmodulInhaltModify);
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(response);
        onlineServiceLinkValidator.validateCount(1).validateUri(0, onlinedienst.getLinks().getFirst().getUri());

        // Prueft ob die Synonymliste aktualisiert wurde
        Response responseListModify = ServiceDescriptionConnector.getServiceDescriptionsV8(
            arsSP,
            spezTextmodulInhaltModify
        );
        ServiceDescriptionResultListValidator validatorListModifySP = new ServiceDescriptionResultListValidator(
            responseListModify
        );
        validatorListModifySP.validateContains(leistung.getFullId());

        Response responseListDelete = ServiceDescriptionConnector.getServiceDescriptionsV8(arsSP, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListDeleteSP = new ServiceDescriptionResultListValidator(
            responseListDelete
        );
        validatorListDeleteSP.validateNotContains(leistung.getFullId());

        // Prueft ob das Synonym aus dem Onlinedienst auch der Leistungsbeschreibung hinzugefügt wird.
        String arsOD = fileSetup.getZustaendigkeitenOnlinedienst().getFirst().getArss().getFirst();
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void fullModifyLBandOEandOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        ImportUtil.pushAndWait(fileSetup);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        leistung.getUrsprungsportale().clear();
        leistung.getUrsprungsportale().add(UrsprungsportalModul.builder().uri("www.newPortal.de").build());
        file.getLeistungsbeschreibungen().add(leistung);
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        organisationseinheit.setTitel("newTitle");
        file.getOrganisationseinheiten().add(organisationseinheit);
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        onlinedienst.getLinks().getFirst().setUri("www.newUri.de");
        file.getOnlinedienste().add(onlinedienst);
        ImportUtil.pushAndWait(file);

        String ars = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId())
            .validateSourcePortalsCount(1)
            .validateSourcePortalUri(0, "www.newPortal.de");
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(response);
        onlineServiceLinkValidator.validateCount(1).validateUri(0, onlinedienst.getLinks().getFirst().getUri());
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        OrganisationValidator validatorOE = new OrganisationValidator(responseOE);
        validatorOE.validateTitle("newTitle");

        // Prueft ob das Synonym aus dem Onlinedienst in der Leistungsbeschreibung aktualisiert wurde.
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void fullModifyLBandOEandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String spezTextmodulInhaltModify = spezTextmodulInhalt + "Modify";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).sp(1).getMin();
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
        fileSetup.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(fileSetup);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        leistung.getUrsprungsportale().clear();
        leistung.getUrsprungsportale().add(UrsprungsportalModul.builder().uri("www.newPortal.de").build());
        file.getLeistungsbeschreibungen().add(leistung);
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        organisationseinheit.setTitel("newTitle");
        file.getOrganisationseinheiten().add(organisationseinheit);
        Textmodul textmodul = (Textmodul) spezialisierung.getModulSpezialisierungen().getFirst().getModul();
        textmodul.setInhalt(spezTextmodulInhaltModify);
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(file);

        String ars = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateName(spezTextmodulInhaltModify)
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId())
            .validateSourcePortalsCount(1)
            .validateSourcePortalUri(0, "www.newPortal.de");
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        OrganisationValidator validatorOE = new OrganisationValidator(responseOE);
        validatorOE.validateTitle("newTitle");

        // Prueft ob die Synonymliste aktuallisiert wurde
        Response responseListModify = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            spezTextmodulInhaltModify
        );
        ServiceDescriptionResultListValidator validatorListModifySP = new ServiceDescriptionResultListValidator(
            responseListModify
        );
        validatorListModifySP.validateContains(leistung.getFullId());

        Response responseListDelete = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListDeleteSP = new ServiceDescriptionResultListValidator(
            responseListDelete
        );
        validatorListDeleteSP.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullModifyLBandODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String spezTextmodulInhaltModify = spezTextmodulInhalt + "Modify";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).sp(1).getMin();
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
        fileSetup.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(fileSetup);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        leistung.getUrsprungsportale().clear();
        leistung.getUrsprungsportale().add(UrsprungsportalModul.builder().uri("www.newPortal.de").build());
        file.getLeistungsbeschreibungen().add(leistung);
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        file.getOnlinedienste().add(onlinedienst);
        onlinedienst.getLinks().getFirst().setUri("www.newUri.de");
        file.getOnlinedienste().add(onlinedienst);
        Textmodul textmodul = (Textmodul) spezialisierung.getModulSpezialisierungen().getFirst().getModul();
        textmodul.setInhalt(spezTextmodulInhaltModify);
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(file);

        String ars = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateName(spezTextmodulInhaltModify)
            .validateSourcePortalsCount(1)
            .validateSourcePortalUri(0, "www.newPortal.de");
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(response);
        onlineServiceLinkValidator.validateCount(1).validateUri(0, onlinedienst.getLinks().getFirst().getUri());

        // Prueft ob die Synonymliste aktuallisiert wurde
        Response responseListModify = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            spezTextmodulInhaltModify
        );
        ServiceDescriptionResultListValidator validatorListModifySP = new ServiceDescriptionResultListValidator(
            responseListModify
        );
        validatorListModifySP.validateContains(leistung.getFullId());

        Response responseListDelete = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListDeleteSP = new ServiceDescriptionResultListValidator(
            responseListDelete
        );
        validatorListDeleteSP.validateNotContains(leistung.getFullId());

        // Prueft ob das Synonym aus dem Onlinedienst in der Leistungsbeschreibung aktualisiert wurde.
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void fullModifyOEandODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String spezTextmodulInhaltModify = spezTextmodulInhalt + "Modify";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).sp(1).getMin();
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
        fileSetup.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(fileSetup);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        organisationseinheit.setTitel("newTitle");
        file.getOrganisationseinheiten().add(organisationseinheit);
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        onlinedienst.getLinks().getFirst().setUri("www.newUri.de");
        file.getOnlinedienste().add(onlinedienst);
        Textmodul textmodul = (Textmodul) spezialisierung.getModulSpezialisierungen().getFirst().getModul();
        textmodul.setInhalt(spezTextmodulInhaltModify);
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(file);

        String ars = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateName(spezTextmodulInhaltModify)
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(response);
        onlineServiceLinkValidator.validateCount(1).validateUri(0, onlinedienst.getLinks().getFirst().getUri());
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        OrganisationValidator validatorOE = new OrganisationValidator(responseOE);
        validatorOE.validateTitle("newTitle");

        // Prueft ob die Synonymliste aktuallisiert wurde
        Response responseListModify = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            spezTextmodulInhaltModify
        );
        ServiceDescriptionResultListValidator validatorListModifySP = new ServiceDescriptionResultListValidator(
            responseListModify
        );
        validatorListModifySP.validateContains(leistung.getFullId());

        Response responseListDelete = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListDeleteSP = new ServiceDescriptionResultListValidator(
            responseListDelete
        );
        validatorListDeleteSP.validateNotContains(leistung.getFullId());

        // Prueft ob das Synonym aus dem Onlinedienst in der Leistungsbeschreibung aktualisiert wurde.
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }

    @Test
    void fullModifyLBandOEandODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String spezTextmodulInhaltModify = spezTextmodulInhalt + "Modify";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).sp(1).getMin();
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
        fileSetup.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(fileSetup);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        leistung.getUrsprungsportale().clear();
        leistung.getUrsprungsportale().add(UrsprungsportalModul.builder().uri("www.newPortal.de").build());
        file.getLeistungsbeschreibungen().add(leistung);
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        organisationseinheit.setTitel("newTitle");
        file.getOrganisationseinheiten().add(organisationseinheit);
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        onlinedienst.getLinks().getFirst().setUri("www.newUri.de");
        file.getOnlinedienste().add(onlinedienst);
        Textmodul textmodul = (Textmodul) spezialisierung.getModulSpezialisierungen().getFirst().getModul();
        textmodul.setInhalt(spezTextmodulInhaltModify);
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(file);

        String ars = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateName(spezTextmodulInhaltModify)
            .validateSourcePortalsCount(1)
            .validateSourcePortalUri(0, "www.newPortal.de")
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());
        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(response);
        onlineServiceLinkValidator.validateCount(1).validateUri(0, onlinedienst.getLinks().getFirst().getUri());
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        OrganisationValidator validatorOE = new OrganisationValidator(responseOE);
        validatorOE.validateTitle("newTitle");

        // Prueft ob die Synonymliste aktuallisiert wurde
        Response responseListModify = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            spezTextmodulInhaltModify
        );
        ServiceDescriptionResultListValidator validatorListModifySP = new ServiceDescriptionResultListValidator(
            responseListModify
        );
        validatorListModifySP.validateContains(leistung.getFullId());

        Response responseListDelete = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListDeleteSP = new ServiceDescriptionResultListValidator(
            responseListDelete
        );
        validatorListDeleteSP.validateNotContains(leistung.getFullId());

        // Prueft ob das Synonym aus dem Onlinedienst in der Leistungsbeschreibung aktualisiert wurde.
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateContains(leistung.getFullId());
    }
}
