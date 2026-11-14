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
package suchdienst.integrationstest.tests.x23.gemeinsamervollzug;

import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.FoCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.OdCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.OeCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.SpCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsFormularCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsOnlinedienstCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsOrganisationseinheitCreator;
import de.dataport.pvog.testdatengenerator.model.Formular;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.ModulSpezialisierung;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.Spezialisierung;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import io.restassured.response.Response;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.ErrorValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.FormsValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.OnlineServiceLinkValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.ServiceDescriptionValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.TextmodulValidator;
import suchdienst.integrationstest.validator.servicedescriptionv8.ServiceDescriptionResultListValidator;

/**
 * Eine Leistung aus Schleswig-Holstein bekommt Zuständigkeiten aus Hamburg und Hessen. Dazu bekommt die Leistung
 * ein update, dass gemeinsamer Vollzug aktiviert ist. Die Zuständigkeit bekommt die Leistung über die Zuständigkeit
 * eines bereits existierenden Onlinedienst/Organisationseinheit oder bekommt dafür eine neue.
 */
class GemeinsamerVollzugAddTest {

    private static final BundeslandConfig BUNDESLAND_CONFIG_SH = new BundeslandConfig(
        "SH",
        "L100012",
        "010000000000",
        List.of("010575755063", "010515163089")
    );

    private static final String BASIS_ARS_SH = BUNDESLAND_CONFIG_SH.getBasicArs();
    private static final String ARS_SH = "010575755063";
    // Stadtstaat Hamburg
    private static final String ARS_HAMBURG = "020000000000";
    // Flächenland Hessen - Gemeinde ARS
    private static final String ARS_HESSEN = "065310005005";
    private static final String BASIS_ARS_HESSEN = "060000000000";

    private static final List<String> ARS_FEDERAL_STATE_LIST = List.of("01", "02", "06");

    @Test
    void addZSOE(TestInfo testInfo) {
        //***** Setup *****
        // LB (SH)
        // OE SH mit ZS SH
        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile fileSetup = XzufiFileCreator.newInstance(BUNDESLAND_CONFIG_SH)
            .name(testclassname)
            .lb(1)
            .oe(1)
            .getMin();
        String lbId = fileSetup.getLeistungsbeschreibungen().getFirst().getFullId();
        fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst().setArss(BUNDESLAND_CONFIG_SH.getArss());

        String searchItem = fileSetup.getLeistungsbeschreibungen().getFirst().getTextmodule().getFirst().getInhalt();
        ImportUtil.pushAndWait23(fileSetup);

        //***** Testcase *****
        // Hinzufügen von ZSOE HH und ZSOE HE
        XzufiFile file = new XzufiFile();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        leistung.setGemeinsamerVollzug(true);
        leistung.setGueltigkeitGebietIDs(List.of(BASIS_ARS_SH, ARS_HAMBURG, BASIS_ARS_HESSEN));
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zustaendigkeit = ZsOrganisationseinheitCreator.getInstance(BUNDESLAND_CONFIG_SH).getMin(
            leistung.getId(),
            organisationseinheit.getId(),
            2
        );
        zustaendigkeit.setArss(List.of(ARS_HAMBURG, ARS_HESSEN));
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeit);
        file.getLeistungsbeschreibungen().add(leistung);
        ImportUtil.pushAndWait23(file);

        //Test - Trefferliste
        // SH wird noch gefunden
        Response responseSH = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_SH, searchItem);
        ServiceDescriptionResultListValidator validatorSH = new ServiceDescriptionResultListValidator(responseSH);
        validatorSH.validateContains(lbId).validateIdList(List.of(lbId));

        // Hamburg wird gefunden
        Response responseHH = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HAMBURG, searchItem);
        ServiceDescriptionResultListValidator validatorHH = new ServiceDescriptionResultListValidator(responseHH);
        validatorHH.validateContains(lbId).validateIdList(List.of(lbId));

        // Gemeinde in Hessen wird gefunden
        Response responseHE = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HESSEN, searchItem);
        ServiceDescriptionResultListValidator validatorHE = new ServiceDescriptionResultListValidator(responseHE);
        validatorHE.validateContains(lbId).validateIdList(List.of(lbId));

        //Test - Detail
        // SH wird noch gefunden
        Response responseDetailSH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_SH, lbId);
        ServiceDescriptionValidator validatorDetailSH = new ServiceDescriptionValidator(responseDetailSH);
        validatorDetailSH
            .validateId(lbId)
            .validateArsFederalstateCount(3)
            .validateArsFederalstateListContains(ARS_FEDERAL_STATE_LIST)
            .validateOrganisationIdsCount(1)
            .validateOrganisationIds(0, organisationseinheit.getFullId());

        // Hamburg wird gefunden
        Response responseDetailHH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HAMBURG, lbId);
        ServiceDescriptionValidator validatorDetailHH = new ServiceDescriptionValidator(responseDetailHH);
        validatorDetailHH
            .validateId(lbId)
            .validateArsFederalstateCount(3)
            .validateArsFederalstateListContains(ARS_FEDERAL_STATE_LIST)
            .validateOrganisationIdsCount(1)
            .validateOrganisationIds(0, organisationseinheit.getFullId());

        // Gemeinde in Hessen wird gefunden
        Response responseDetailHE = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HESSEN, lbId);
        ServiceDescriptionValidator validatorDetailHE = new ServiceDescriptionValidator(responseDetailHE);
        validatorDetailHE
            .validateId(lbId)
            .validateArsFederalstateCount(3)
            .validateArsFederalstateListContains(ARS_FEDERAL_STATE_LIST)
            .validateOrganisationIdsCount(1)
            .validateOrganisationIds(0, organisationseinheit.getFullId());
    }

    @Test
    void addOEandZSOE(TestInfo testInfo) {
        //***** Setup *****
        // LB (SH)
        // OE SH mit ZS SH
        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile fileSetup = XzufiFileCreator.newInstance(BUNDESLAND_CONFIG_SH)
            .name(testclassname)
            .lb(1)
            .oe(1)
            .getMin();
        String lbId = fileSetup.getLeistungsbeschreibungen().getFirst().getFullId();
        fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst().setArss(BUNDESLAND_CONFIG_SH.getArss());

        String searchItem = fileSetup.getLeistungsbeschreibungen().getFirst().getTextmodule().getFirst().getInhalt();
        Organisationseinheit organisationseinheitSH = fileSetup.getOrganisationseinheiten().getFirst();
        ImportUtil.pushAndWait23(fileSetup);

        //***** Testcase *****
        // Hinzufügen von OE (HH,HE) mit ZS HH und ZS HE
        XzufiFile file = new XzufiFile();

        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        leistung.setGemeinsamerVollzug(true);
        leistung.setGueltigkeitGebietIDs(List.of(BASIS_ARS_SH, ARS_HAMBURG, BASIS_ARS_HESSEN));
        Organisationseinheit organisationseinheitHH_HE = OeCreator.getInstance().getMin(testclassname);
        organisationseinheitHH_HE.setSchemeAgencyId(BUNDESLAND_CONFIG_SH.getSchemeAgencyId());
        Zustaendigkeit zustaendigkeit = ZsOrganisationseinheitCreator.getInstance(BUNDESLAND_CONFIG_SH).getMin(
            leistung.getId(),
            organisationseinheitHH_HE.getId(),
            2
        );
        zustaendigkeit.setArss(List.of(ARS_HAMBURG, ARS_HESSEN));
        file.getOrganisationseinheiten().add(organisationseinheitHH_HE);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeit);
        file.getLeistungsbeschreibungen().add(leistung);
        ImportUtil.pushAndWait23(file);

        //Test - Trefferliste
        // SH wird noch gefunden
        Response responseSH = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_SH, searchItem);
        ServiceDescriptionResultListValidator validatorSH = new ServiceDescriptionResultListValidator(responseSH);
        validatorSH.validateContains(lbId).validateIdList(List.of(lbId));

        // Hamburg wird gefunden
        Response responseHH = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HAMBURG, searchItem);
        ServiceDescriptionResultListValidator validatorHH = new ServiceDescriptionResultListValidator(responseHH);
        validatorHH.validateContains(lbId).validateIdList(List.of(lbId));

        // Gemeinde in Hessen wird gefunden
        Response responseHE = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HESSEN, searchItem);
        ServiceDescriptionResultListValidator validatorHE = new ServiceDescriptionResultListValidator(responseHE);
        validatorHE.validateContains(lbId).validateIdList(List.of(lbId));

        //Test - Details
        // SH wird noch gefunden
        Response responseDetailSH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_SH, lbId);
        ServiceDescriptionValidator validatorDetailSH = new ServiceDescriptionValidator(responseDetailSH);
        validatorDetailSH
            .validateId(lbId)
            .validateArsFederalstateCount(3)
            .validateArsFederalstateListContains(ARS_FEDERAL_STATE_LIST)
            .validateOrganisationIdsCount(1)
            .validateOrganisationIds(0, organisationseinheitSH.getFullId());

        // Hamburg wird gefunden
        Response responseDetailHH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HAMBURG, lbId);
        ServiceDescriptionValidator validatorDetailHH = new ServiceDescriptionValidator(responseDetailHH);
        validatorDetailHH
            .validateId(lbId)
            .validateArsFederalstateCount(3)
            .validateArsFederalstateListContains(ARS_FEDERAL_STATE_LIST)
            .validateOrganisationIdsCount(1)
            .validateOrganisationIds(0, organisationseinheitHH_HE.getFullId());

        // Gemeinde in Hessen wird gefunden
        Response responseDetailHE = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HESSEN, lbId);
        ServiceDescriptionValidator validatorDetailHE = new ServiceDescriptionValidator(responseDetailHE);
        validatorDetailHE
            .validateId(lbId)
            .validateArsFederalstateCount(3)
            .validateArsFederalstateListContains(ARS_FEDERAL_STATE_LIST)
            .validateOrganisationIdsCount(1)
            .validateOrganisationIds(0, organisationseinheitHH_HE.getFullId());
    }

    @Test
    void addZSOD(TestInfo testInfo) {
        //***** Setup *****
        // LB (SH)
        // OD SH mit ZS SH
        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile fileSetup = XzufiFileCreator.newInstance(BUNDESLAND_CONFIG_SH)
            .name(testclassname)
            .lb(1)
            .od(1)
            .getMin();
        String lbId = fileSetup.getLeistungsbeschreibungen().getFirst().getFullId();
        fileSetup.getZustaendigkeitenOnlinedienst().getFirst().setArss(List.of(ARS_SH));
        String searchItem = fileSetup.getLeistungsbeschreibungen().getFirst().getTextmodule().getFirst().getInhalt();
        ImportUtil.pushAndWait23(fileSetup);

        //***** Testcase *****
        // Hinzufügen von ZSOD HH und ZSOD HE
        XzufiFile file = new XzufiFile();

        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        leistung.setGemeinsamerVollzug(true);
        leistung.setGueltigkeitGebietIDs(List.of(BASIS_ARS_SH, ARS_HAMBURG, BASIS_ARS_HESSEN));
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        Zustaendigkeit zustaendigkeit = ZsOnlinedienstCreator.getInstance(BUNDESLAND_CONFIG_SH).getMin(
            leistung.getId(),
            onlinedienst.getId(),
            2
        );
        zustaendigkeit.setArss(List.of(ARS_HAMBURG, ARS_HESSEN));
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeit);
        file.getLeistungsbeschreibungen().add(leistung);
        ImportUtil.pushAndWait23(file);

        //Test Trefferliste
        // SH wird noch gefunden
        Response responseSH = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_SH, searchItem);
        ServiceDescriptionResultListValidator validatorSH = new ServiceDescriptionResultListValidator(responseSH);
        validatorSH.validateContains(lbId).validateIdList(List.of(lbId));

        // Hamburg wird gefunden
        Response responseHH = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HAMBURG, searchItem);
        ServiceDescriptionResultListValidator validatorHH = new ServiceDescriptionResultListValidator(responseHH);
        validatorHH.validateContains(lbId).validateIdList(List.of(lbId));

        // Gemeinde in Hessen wird gefunden
        Response responseHE = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HESSEN, searchItem);
        ServiceDescriptionResultListValidator validatorHE = new ServiceDescriptionResultListValidator(responseHE);
        validatorHE.validateContains(lbId).validateIdList(List.of(lbId));

        //Test - Details
        // SH wird noch gefunden
        Response responseDetailSH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_SH, lbId);
        OnlineServiceLinkValidator validatorDetailSH = new OnlineServiceLinkValidator(responseDetailSH);
        validatorDetailSH
            .validateId(0, onlinedienst.getFullId())
            .validateCount(1)
            .validateUri(0, onlinedienst.getLinks().getFirst().getUri());

        // Hamburg wird gefunden
        Response responseDetailHH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HAMBURG, lbId);
        OnlineServiceLinkValidator validatorOdHH = new OnlineServiceLinkValidator(responseDetailHH);
        validatorOdHH
            .validateId(0, onlinedienst.getFullId())
            .validateCount(1)
            .validateUri(0, onlinedienst.getLinks().getFirst().getUri());

        // Gemeinde in Hessen wird gefunden
        Response responseDetailHE = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HESSEN, lbId);
        OnlineServiceLinkValidator validatorOdHE = new OnlineServiceLinkValidator(responseDetailHE);
        validatorOdHE
            .validateId(0, onlinedienst.getFullId())
            .validateCount(1)
            .validateUri(0, onlinedienst.getLinks().getFirst().getUri());
    }

    @Test
    void addODandZSOD(TestInfo testInfo) {
        //***** Setup *****
        // LB (SH)
        // OD SH mit ZS SH
        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile fileSetup = XzufiFileCreator.newInstance(BUNDESLAND_CONFIG_SH)
            .name(testclassname)
            .lb(1)
            .od(1)
            .getMin();
        String lbId = fileSetup.getLeistungsbeschreibungen().getFirst().getFullId();
        fileSetup.getZustaendigkeitenOnlinedienst().getFirst().setArss(List.of(ARS_SH));
        String searchItem = fileSetup.getLeistungsbeschreibungen().getFirst().getTextmodule().getFirst().getInhalt();
        ImportUtil.pushAndWait23(fileSetup);

        //***** Testcase *****
        // Hinzufügen von OD (HH,HE) mit ZS HH und ZS HE
        XzufiFile file = new XzufiFile();

        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        leistung.setGemeinsamerVollzug(true);
        leistung.setGueltigkeitGebietIDs(List.of(BASIS_ARS_SH, ARS_HAMBURG, BASIS_ARS_HESSEN));
        Onlinedienst onlinedienstSH = fileSetup.getOnlinedienste().getFirst();
        Onlinedienst onlinedienstHH_HE = OdCreator.getInstance().getMin(testclassname);
        onlinedienstHH_HE.setSchemeAgencyId(BUNDESLAND_CONFIG_SH.getSchemeAgencyId());
        Zustaendigkeit zustaendigkeit = ZsOnlinedienstCreator.getInstance(BUNDESLAND_CONFIG_SH).getMin(
            leistung.getId(),
            onlinedienstHH_HE.getId(),
            2
        );
        zustaendigkeit.setArss(List.of(ARS_HAMBURG, ARS_HESSEN));
        file.getOnlinedienste().add(onlinedienstHH_HE);
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeit);
        file.getLeistungsbeschreibungen().add(leistung);
        ImportUtil.pushAndWait23(file);

        //Test Trefferliste
        // SH wird noch gefunden
        Response responseSH = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_SH, searchItem);
        ServiceDescriptionResultListValidator validatorSH = new ServiceDescriptionResultListValidator(responseSH);
        validatorSH
            .validateContains(lbId)
            .validateIdList(List.of(lbId))
            .validateArsFederalListSize(0, 3)
            .validateArsFederalListContains(0, ARS_FEDERAL_STATE_LIST);

        // Hamburg wird gefunden
        Response responseHH = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HAMBURG, searchItem);
        ServiceDescriptionResultListValidator validatorHH = new ServiceDescriptionResultListValidator(responseHH);
        validatorHH
            .validateContains(lbId)
            .validateIdList(List.of(lbId))
            .validateArsFederalListSize(0, 3)
            .validateArsFederalListContains(0, ARS_FEDERAL_STATE_LIST);

        // Gemeinde in Hessen wird gefunden
        Response responseHE = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HESSEN, searchItem);
        ServiceDescriptionResultListValidator validatorHE = new ServiceDescriptionResultListValidator(responseHE);
        validatorHE
            .validateContains(lbId)
            .validateIdList(List.of(lbId))
            .validateArsFederalListSize(0, 3)
            .validateArsFederalListContains(0, ARS_FEDERAL_STATE_LIST);

        //Test - Details
        // SH wird noch gefunden
        Response responseDetailSH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_SH, lbId);
        OnlineServiceLinkValidator validatorDetailSH = new OnlineServiceLinkValidator(responseDetailSH);
        validatorDetailSH
            .validateId(0, onlinedienstSH.getFullId())
            .validateCount(1)
            .validateUri(0, onlinedienstSH.getLinks().getFirst().getUri());

        // Hamburg wird gefunden
        Response responseDetailHH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HAMBURG, lbId);
        OnlineServiceLinkValidator validatorOdHH = new OnlineServiceLinkValidator(responseDetailHH);
        validatorOdHH
            .validateId(0, onlinedienstHH_HE.getFullId())
            .validateCount(1)
            .validateUri(0, onlinedienstHH_HE.getLinks().getFirst().getUri());

        // Hessen wird gefunden
        Response responseDetailHE = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HESSEN, lbId);
        OnlineServiceLinkValidator validatorOdHE = new OnlineServiceLinkValidator(responseDetailHE);
        validatorOdHE
            .validateId(0, onlinedienstHH_HE.getFullId())
            .validateCount(1)
            .validateUri(0, onlinedienstHH_HE.getLinks().getFirst().getUri());
    }

    @Test
    void addZSFO(TestInfo testInfo) {
        //***** Setup *****
        // LB (SH)
        // FO und OE SH mit ZS SH
        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile fileSetup = XzufiFileCreator.newInstance(BUNDESLAND_CONFIG_SH)
            .name(testclassname)
            .lb(1)
            .oe(1)
            .fo(1)
            .getMin();
        String lbId = fileSetup.getLeistungsbeschreibungen().getFirst().getFullId();
        fileSetup.getZustaendigkeitenFormular().getFirst().setArss(List.of(ARS_SH));
        String searchItem = fileSetup.getLeistungsbeschreibungen().getFirst().getTextmodule().getFirst().getInhalt();
        ImportUtil.pushAndWait23(fileSetup);

        //***** Testcase *****
        // Hinzufügen von ZSFO HH und ZSFO HE
        XzufiFile file = new XzufiFile();

        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        leistung.setGemeinsamerVollzug(true);
        leistung.setGueltigkeitGebietIDs(List.of(BASIS_ARS_SH, ARS_HAMBURG, BASIS_ARS_HESSEN));
        Formular formular = fileSetup.getFormulare().getFirst();
        Zustaendigkeit zsFo = ZsFormularCreator.getInstance(BUNDESLAND_CONFIG_SH).getMin(
            leistung.getId(),
            formular.getId(),
            2
        );
        Organisationseinheit oe = fileSetup.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zsOe = ZsOrganisationseinheitCreator.getInstance(BUNDESLAND_CONFIG_SH).getMin(
            leistung.getId(),
            oe.getId(),
            2
        );
        zsFo.setArss(List.of(ARS_HAMBURG, ARS_HESSEN));
        zsOe.setArss(List.of(ARS_HAMBURG, ARS_HESSEN));
        file.getZustaendigkeitenFormular().add(zsFo);
        file.getZustaendigkeitenOrganisationseinheit().add(zsOe);
        file.getLeistungsbeschreibungen().add(leistung);
        ImportUtil.pushAndWait23(file);

        //Test Trefferliste
        // SH wird noch gefunden
        Response responseSH = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_SH, searchItem);
        ServiceDescriptionResultListValidator validatorSH = new ServiceDescriptionResultListValidator(responseSH);
        validatorSH.validateContains(lbId).validateIdList(List.of(lbId));

        // Hamburg wird gefunden
        Response responseHH = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HAMBURG, searchItem);
        ServiceDescriptionResultListValidator validatorHH = new ServiceDescriptionResultListValidator(responseHH);
        validatorHH.validateContains(lbId).validateIdList(List.of(lbId));

        // Gemeinde in Hessen wird gefunden
        Response responseHE = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HESSEN, searchItem);
        ServiceDescriptionResultListValidator validatorHE = new ServiceDescriptionResultListValidator(responseHE);
        validatorHE.validateContains(lbId).validateIdList(List.of(lbId));

        //Test - Details
        // SH wird noch gefunden
        Response responseDetailSH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_SH, lbId);
        FormsValidator formsValidatorSH = new FormsValidator(responseDetailSH);
        formsValidatorSH.validateId(0, formular.getFullId()).validateCount(1);

        // Hamburg wird gefunden
        Response responseDetailHH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HAMBURG, lbId);
        FormsValidator formsValidatorHH = new FormsValidator(responseDetailHH);
        formsValidatorHH.validateId(0, formular.getFullId()).validateCount(1);

        // Gemeinde in Hessen wird gefunden
        Response responseDetailHE = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HESSEN, lbId);
        FormsValidator formsValidatorHE = new FormsValidator(responseDetailHE);
        formsValidatorHE.validateId(0, formular.getFullId()).validateCount(1);
    }

    @Test
    void addFOandZSFO(TestInfo testInfo) {
        //***** Setup *****
        // LB (SH)
        // OD SH mit ZS SH
        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile fileSetup = XzufiFileCreator.newInstance(BUNDESLAND_CONFIG_SH)
            .name(testclassname)
            .lb(1)
            .oe(1)
            .fo(1)
            .getMin();
        String lbId = fileSetup.getLeistungsbeschreibungen().getFirst().getFullId();
        fileSetup.getZustaendigkeitenFormular().getFirst().setArss(List.of(ARS_SH));
        String searchItem = fileSetup.getLeistungsbeschreibungen().getFirst().getTextmodule().getFirst().getInhalt();
        ImportUtil.pushAndWait23(fileSetup);

        //***** Testcase *****
        // Hinzufügen von FO (HH,HE) mit ZS HH und ZS HE
        XzufiFile file = new XzufiFile();

        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        leistung.setGemeinsamerVollzug(true);
        leistung.setGueltigkeitGebietIDs(List.of(BASIS_ARS_SH, ARS_HAMBURG, BASIS_ARS_HESSEN));
        Formular formularSH = fileSetup.getFormulare().getFirst();
        Formular formularHH_HE = FoCreator.getInstance().getMin(testclassname);
        formularHH_HE.setSchemeAgencyId(BUNDESLAND_CONFIG_SH.getSchemeAgencyId());
        Zustaendigkeit zsfo = ZsFormularCreator.getInstance(BUNDESLAND_CONFIG_SH).getMin(
            leistung.getId(),
            formularHH_HE.getId(),
            2
        );
        zsfo.setArss(List.of(ARS_HAMBURG, ARS_HESSEN));
        file.getFormulare().add(formularHH_HE);
        file.getZustaendigkeitenFormular().add(zsfo);
        file.getLeistungsbeschreibungen().add(leistung);

        // Beimischen einer OE auf Bundesland-Ebene, damit das Formular gefunden werden kann.
        Organisationseinheit oe = fileSetup.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zsoe = ZsOrganisationseinheitCreator.getInstance(BUNDESLAND_CONFIG_SH).getMin(
            leistung.getId(),
            oe.getId()
        );
        zsoe.setArss(List.of(ARS_HAMBURG, BASIS_ARS_HESSEN));
        file.getZustaendigkeitenOrganisationseinheit().add(zsoe);

        ImportUtil.pushAndWait23(file);

        //Test Trefferliste
        // SH wird noch gefunden
        Response responseSH = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_SH, searchItem);
        ServiceDescriptionResultListValidator validatorSH = new ServiceDescriptionResultListValidator(responseSH);
        validatorSH
            .validateContains(lbId)
            .validateIdList(List.of(lbId))
            .validateArsFederalListSize(0, 3)
            .validateArsFederalListContains(0, ARS_FEDERAL_STATE_LIST);

        // Hamburg wird gefunden
        Response responseHH = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HAMBURG, searchItem);
        ServiceDescriptionResultListValidator validatorHH = new ServiceDescriptionResultListValidator(responseHH);
        validatorHH
            .validateContains(lbId)
            .validateIdList(List.of(lbId))
            .validateArsFederalListSize(0, 3)
            .validateArsFederalListContains(0, ARS_FEDERAL_STATE_LIST);

        // Gemeinde in Hessen wird gefunden
        Response responseHE = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HESSEN, searchItem);
        ServiceDescriptionResultListValidator validatorHE = new ServiceDescriptionResultListValidator(responseHE);
        validatorHE
            .validateContains(lbId)
            .validateIdList(List.of(lbId))
            .validateArsFederalListSize(0, 3)
            .validateArsFederalListContains(0, ARS_FEDERAL_STATE_LIST);

        //Test - Details
        // SH wird noch gefunden
        Response responseDetailSH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_SH, lbId);
        FormsValidator formsValidatorSH = new FormsValidator(responseDetailSH);
        formsValidatorSH.validateCount(1).validateId(0, formularSH.getFullId());

        // Hamburg wird gefunden
        Response responseDetailHH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HAMBURG, lbId);
        FormsValidator formsValidatorHH = new FormsValidator(responseDetailHH);
        formsValidatorHH.validateCount(1).validateId(0, formularHH_HE.getFullId());

        // Hessen wird gefunden
        Response responseDetailHE = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HESSEN, lbId);
        FormsValidator formsValidatorHE = new FormsValidator(responseDetailHE);
        formsValidatorHE.validateCount(1).validateId(0, formularHH_HE.getFullId());
    }

    @Test
    void addSpOeFuerFremdesLand(TestInfo testInfo) {
        //***** Setup *****
        // LB (SH)
        // OE SH mit ZS HH und ZS HE
        String randomPrefix = TestUtil.generateRandomWord();
        String randomSPcontent = randomPrefix + "SpezialisierungNullDrei";
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());

        XzufiFile fileSetup = XzufiFileCreator.newInstance(BUNDESLAND_CONFIG_SH)
            .name(testclassname)
            .lb(1)
            .oe(1)
            .getMin();

        Leistungsbeschreibung lb = fileSetup.getLeistungsbeschreibungen().getFirst();
        lb.setGemeinsamerVollzug(true);
        lb.setGueltigkeitGebietIDs(List.of(ARS_HAMBURG, BASIS_ARS_HESSEN));
        fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst().setArss(List.of(ARS_HAMBURG, ARS_HESSEN));
        String searchItem = fileSetup.getLeistungsbeschreibungen().getFirst().getTextmodule().getFirst().getInhalt();
        ImportUtil.pushAndWait23(fileSetup);

        //***** Testcase *****
        // Hinzufügen von SP mut GebietsId HH und HE
        XzufiFile file = new XzufiFile();

        Spezialisierung spezialisierung = SpCreator.getInstance(BUNDESLAND_CONFIG_SH).getMin(testclassname, lb.getId());

        spezialisierung
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(Textmodul.builder().leikaCode("03").inhalt(randomSPcontent).build())
                    .build()
            );
        spezialisierung.setArs(List.of(ARS_HAMBURG, ARS_HESSEN));
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait23(file);

        String lbId = fileSetup.getLeistungsbeschreibungen().getFirst().getFullId();
        Organisationseinheit organisationseinheitHH_HE = fileSetup.getOrganisationseinheiten().getFirst();

        //Test Trefferliste
        // SH wird nicht gefunden - fehlende Zuordnung
        Response responseSH = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_SH, searchItem);
        ServiceDescriptionResultListValidator validatorSH = new ServiceDescriptionResultListValidator(responseSH);
        validatorSH.validateEmptyResultList();

        // Hamburg wird gefunden
        Response responseHH = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HAMBURG, searchItem);
        ServiceDescriptionResultListValidator validatorHH = new ServiceDescriptionResultListValidator(responseHH);
        validatorHH
            .validateContains(lbId)
            .validateIdList(List.of(lbId))
            .validateCount(1)
            .validateArsFederalListSize(0, 2)
            .validateArsFederalListContains(0, List.of("02", "06"));
        // Suche über Spezialisierung
        Response responseHHSP = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HAMBURG, randomSPcontent);
        ServiceDescriptionResultListValidator validatorHHSP = new ServiceDescriptionResultListValidator(responseHHSP);
        validatorHHSP
            .validateContains(lbId)
            .validateIdList(List.of(lbId))
            .validateCount(1)
            .validateArsFederalListSize(0, 2)
            .validateArsFederalListContains(0, List.of("02", "06"));

        // Gemeinde in Hessen wird gefunden
        Response responseHE = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HESSEN, searchItem);
        ServiceDescriptionResultListValidator validatorHE = new ServiceDescriptionResultListValidator(responseHE);
        validatorHE
            .validateContains(lbId)
            .validateIdList(List.of(lbId))
            .validateCount(1)
            .validateArsFederalListSize(0, 2)
            .validateArsFederalListContains(0, List.of("02", "06"));
        // Suche über Spezialisierung
        Response responseHESP = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HESSEN, randomSPcontent);
        ServiceDescriptionResultListValidator validatorHESP = new ServiceDescriptionResultListValidator(responseHESP);
        validatorHESP
            .validateContains(lbId)
            .validateIdList(List.of(lbId))
            .validateCount(1)
            .validateArsFederalListSize(0, 2)
            .validateArsFederalListContains(0, List.of("02", "06"));

        //Test - Details
        // SH wird nicht gefunden - fehlende Zuordnung
        Response responseDetailSH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_SH, lbId);
        ErrorValidator validatorDetailSH = new ErrorValidator(responseDetailSH);
        validatorDetailSH.validateNotFound();

        // Hamburg wird gefunden
        Response responseDetailHH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HAMBURG, lbId);
        TextmodulValidator validatorTextmodul = new TextmodulValidator(responseDetailHH);
        validatorTextmodul.validateTextByCode("03", randomSPcontent);
        ServiceDescriptionValidator validatorDetailHH = new ServiceDescriptionValidator(responseDetailHH);
        validatorDetailHH
            .validateId(lbId)
            .validateArsFederalstateCount(2)
            .validateArsFederalstateListContains(List.of("02", "06"))
            .validateOrganisationIdsCount(1)
            .validateOrganisationIds(0, organisationseinheitHH_HE.getFullId());

        // Gemeinde in Hessen wird gefunden
        Response responseDetailHE = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HESSEN, lbId);
        TextmodulValidator validatorTextmodulHE = new TextmodulValidator(responseDetailHE);
        validatorTextmodulHE.validateTextByCode("03", randomSPcontent);
        ServiceDescriptionValidator validatorDetailHE = new ServiceDescriptionValidator(responseDetailHE);
        validatorDetailHE
            .validateId(lbId)
            .validateArsFederalstateCount(2)
            .validateArsFederalstateListContains(List.of("02", "06"))
            .validateOrganisationIdsCount(1)
            .validateOrganisationIds(0, organisationseinheitHH_HE.getFullId());
    }

    @Test
    void addSpOdFuerFremdesLand(TestInfo testInfo) {
        //***** Setup *****
        // LB (SH)
        // OD SH mit ZS HH und ZS HE
        String randomPrefix = TestUtil.generateRandomWord();
        String randomSPcontent = randomPrefix + "SpezialisierungNullDrei";
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());

        XzufiFile fileSetup = XzufiFileCreator.newInstance(BUNDESLAND_CONFIG_SH)
            .name(testclassname)
            .lb(1)
            .od(1)
            .getMin();

        Leistungsbeschreibung lb = fileSetup.getLeistungsbeschreibungen().getFirst();
        lb.setGemeinsamerVollzug(true);
        lb.setGueltigkeitGebietIDs(List.of(ARS_HAMBURG, BASIS_ARS_HESSEN));
        fileSetup.getZustaendigkeitenOnlinedienst().getFirst().setArss(List.of(ARS_HAMBURG, ARS_HESSEN));
        String searchItem = fileSetup.getLeistungsbeschreibungen().getFirst().getTextmodule().getFirst().getInhalt();
        ImportUtil.pushAndWait23(fileSetup);

        //***** Testcase *****
        // Hinzufügen von SP mut GebietsId HH und HE
        XzufiFile file = new XzufiFile();
        Spezialisierung spezialisierung = SpCreator.getInstance(BUNDESLAND_CONFIG_SH).getMin(testclassname, lb.getId());
        Onlinedienst onlinedienstSH = fileSetup.getOnlinedienste().getFirst();
        spezialisierung
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(Textmodul.builder().leikaCode("03").inhalt(randomSPcontent).build())
                    .build()
            );
        spezialisierung.setArs(List.of(ARS_HAMBURG, ARS_HESSEN));
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait23(file);

        String lbId = fileSetup.getLeistungsbeschreibungen().getFirst().getFullId();

        //Test Trefferliste
        // SH wird nicht gefunden - fehlende Zuordnung
        Response responseSH = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_SH, testclassname);
        ServiceDescriptionResultListValidator validatorSH = new ServiceDescriptionResultListValidator(responseSH);
        validatorSH.validateEmptyResultList();

        // Hamburg wird gefunden
        Response responseHH = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HAMBURG, searchItem);
        ServiceDescriptionResultListValidator validatorHH = new ServiceDescriptionResultListValidator(responseHH);
        validatorHH
            .validateContains(lbId)
            .validateIdList(List.of(lbId))
            .validateCount(1)
            .validateArsFederalListSize(0, 2)
            .validateArsFederalListContains(0, List.of("02", "06"));
        // Suche über Spezialisierung
        Response responseHHSP = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HAMBURG, randomSPcontent);
        ServiceDescriptionResultListValidator validatorHHSP = new ServiceDescriptionResultListValidator(responseHHSP);
        validatorHHSP
            .validateContains(lbId)
            .validateIdList(List.of(lbId))
            .validateCount(1)
            .validateArsFederalListSize(0, 2)
            .validateArsFederalListContains(0, List.of("02", "06"));

        // Gemeinde in Hessen wird gefunden
        Response responseHE = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HESSEN, searchItem);
        ServiceDescriptionResultListValidator validatorHE = new ServiceDescriptionResultListValidator(responseHE);
        validatorHE
            .validateContains(lbId)
            .validateIdList(List.of(lbId))
            .validateCount(1)
            .validateArsFederalListSize(0, 2)
            .validateArsFederalListContains(0, List.of("02", "06"));
        // Suche über Spezialisierung
        Response responseHESP = ServiceDescriptionConnector.getServiceDescriptionsV8(ARS_HESSEN, randomSPcontent);
        ServiceDescriptionResultListValidator validatorHESP = new ServiceDescriptionResultListValidator(responseHESP);
        validatorHESP
            .validateContains(lbId)
            .validateIdList(List.of(lbId))
            .validateCount(1)
            .validateArsFederalListSize(0, 2)
            .validateArsFederalListContains(0, List.of("02", "06"));

        //Test - Details
        // SH wird nicht gefunden - fehlende Zuordnung
        Response responseDetailSH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_SH, lbId);
        ErrorValidator validatorDetailSH = new ErrorValidator(responseDetailSH);
        validatorDetailSH.validateNotFound();

        // Hamburg wird gefunden
        Response responseDetailHH = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HAMBURG, lbId);
        TextmodulValidator validatorTextmodul = new TextmodulValidator(responseDetailHH);
        validatorTextmodul.validateTextByCode("03", randomSPcontent);
        OnlineServiceLinkValidator validatorOdHH = new OnlineServiceLinkValidator(responseDetailHH);
        validatorOdHH
            .validateId(0, onlinedienstSH.getFullId())
            .validateCount(1)
            .validateUri(0, onlinedienstSH.getLinks().getFirst().getUri());

        // Gemeinde in Hessen wird gefunden
        Response responseDetailHE = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS_HESSEN, lbId);
        TextmodulValidator validatorTextmodulHE = new TextmodulValidator(responseDetailHE);
        validatorTextmodulHE.validateTextByCode("03", randomSPcontent);
        OnlineServiceLinkValidator validatorOdHE = new OnlineServiceLinkValidator(responseDetailHE);
        validatorOdHE
            .validateId(0, onlinedienstSH.getFullId())
            .validateCount(1)
            .validateUri(0, onlinedienstSH.getLinks().getFirst().getUri());
    }
}
