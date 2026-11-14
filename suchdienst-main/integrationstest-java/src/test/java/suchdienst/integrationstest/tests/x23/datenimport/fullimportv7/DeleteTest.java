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
package suchdienst.integrationstest.tests.x23.datenimport.fullimportv7;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.LbCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsOnlinedienstCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsOrganisationseinheitCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.ModulSpezialisierung;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.Spezialisierung;
import de.dataport.pvog.testdatengenerator.model.Suchbegriff;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.Constants;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.OrganisationunitsConnector;
import suchdienst.integrationstest.connectors.RelationsConnector;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.ErrorValidator;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.OrganisationValidator;
import suchdienst.integrationstest.validator.relations.RelationV3ResultListValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.OnlineServiceLinkValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.ServiceDescriptionValidator;
import suchdienst.integrationstest.validator.servicedescriptionssuggestionv1.SuggestionResultListValidator;
import suchdienst.integrationstest.validator.servicedescriptionv8.ServiceDescriptionResultListValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class DeleteTest {

    @Test
    void fullDeleteZSOE(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance()
            .name(testcasename)
            .lb(1)
            .oe(1)
            .zsOe(2)
            .od(0)
            .zsOd(0)
            .sp(0)
            .getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Zustaendigkeit zustaendigkeit = fileSetup.getZustaendigkeitenOrganisationseinheit().get(1);
        ImportUtil.pushAndWait23(fileSetup);

        String ars = zustaendigkeit.getArss().getFirst();
        String leistungFullId = leistung.getFullId();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistungFullId);
        ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
        validatorSetup.validateId(leistungFullId);
        Response responseSetup2 = RelationsConnector.getRelationsLbIdsArsV3(
            List.of(leistungFullId),
            ars,
            0,
            10,
            Constants.XzufiVersion.V2_3_1
        );
        RelationV3ResultListValidator validatorSetup2 = new RelationV3ResultListValidator(responseSetup2);
        validatorSetup2.validateCount(1);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        zustaendigkeit.setGeloescht(true);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeit);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistungFullId);
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();
    }

    @Test
    void fullDeleteZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance()
            .name(testcasename)
            .lb(1)
            .oe(0)
            .zsOe(0)
            .od(1)
            .zsOd(2)
            .sp(0)
            .getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Zustaendigkeit zustaendigkeit = fileSetup.getZustaendigkeitenOnlinedienst().get(1);
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait23(fileSetup);

        String ars = zustaendigkeit.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        OnlineServiceLinkValidator validatorSetup = new OnlineServiceLinkValidator(responseSetup);
        validatorSetup.validateCount(1);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        zustaendigkeit.setGeloescht(true);
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeit);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();

        // Prueft ob das Synonym aus dem Onlinedienst aus der Leistungsbeschreibung entfernt wurde
        Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(responseList);
        validatorListOD.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullDeleteSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).sp(1).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        List<Textmodul> textmodule = new ArrayList<>();
        textmodule.add(Textmodul.builder().leikaCode("02").inhalt("Textmodul02").positionDarstellung(1).build());
        textmodule.add(Textmodul.builder().leikaCode("03").inhalt("Textmodul03").positionDarstellung(2).build());
        textmodule.add(Textmodul.builder().leikaCode("06").inhalt("Textmodul06").positionDarstellung(3).build());
        leistung.setTextmodule(textmodule);
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
        ImportUtil.pushAndWait23(fileSetup);

        String ars = spezialisierung.getArs().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
        validatorSetup.validateName(spezTextmodulInhalt);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        spezialisierung.setGeloescht(true);
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateName("Textmodul03");

        // Prueft ob das Synonym aus der Spezialisierung auch in der Leistungsbeschreibung entfernt wurde.
        Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(responseList);
        validatorListSP.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullDeleteLBandZSOE(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(2).oe(1).od(0).sp(0).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().get(1);
        Zustaendigkeit zustaendigkeit = fileSetup.getZustaendigkeitenOrganisationseinheit().get(1);
        ImportUtil.pushAndWait23(fileSetup);

        String ars = zustaendigkeit.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
        validatorSetup.validateId(leistung.getFullId());

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        leistung.setGeloescht(true);
        file.getLeistungsbeschreibungen().add(leistung);
        zustaendigkeit.setGeloescht(true);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeit);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();
    }

    @Test
    void fullDeleteLBandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(2).oe(0).od(1).sp(0).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().get(1);
        Zustaendigkeit zustaendigkeit = fileSetup.getZustaendigkeitenOnlinedienst().get(1);
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait23(fileSetup);

        String ars = zustaendigkeit.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        OnlineServiceLinkValidator validatorSetup = new OnlineServiceLinkValidator(responseSetup);
        validatorSetup.validateCount(1);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        leistung.setGeloescht(true);
        file.getLeistungsbeschreibungen().add(leistung);
        zustaendigkeit.setGeloescht(true);
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeit);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();

        // Prueft ob das Synonym aus dem Onlinedienst aus der Leistungsbeschreibung entfernt wurde
        Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(responseList);
        validatorListOD.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullDeleteOEandZSOE(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(2).od(0).sp(0).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().get(1);
        Zustaendigkeit zustaendigkeit = fileSetup.getZustaendigkeitenOrganisationseinheit().get(1);
        ImportUtil.pushAndWait23(fileSetup);

        String ars = zustaendigkeit.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
        validatorSetup
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(2)
            .validateOrganisationIdsContains(fileSetup.getOrganisationseinheiten().getFirst().getFullId())
            .validateOrganisationIdsContains(organisationseinheit.getFullId());
        Response responseOESetup = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            organisationseinheit.getFullId()
        );
        OrganisationValidator validatorOESetup = new OrganisationValidator(responseOESetup);
        validatorOESetup.validateTitle(organisationseinheit.getTitel());

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        organisationseinheit.setGeloescht(true);
        file.getOrganisationseinheiten().add(organisationseinheit);
        zustaendigkeit.setGeloescht(true);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeit);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(fileSetup.getOrganisationseinheiten().getFirst().getFullId());
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        ErrorValidator validatorOE = new ErrorValidator(responseOE);
        validatorOE.validateNotFound();
    }

    @Test
    void fullDeleteZSOEandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance()
            .name(testcasename)
            .lb(1)
            .oe(1)
            .zsOe(2)
            .od(1)
            .zsOd(2)
            .sp(0)
            .getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zustaendigkeitOD = fileSetup.getZustaendigkeitenOnlinedienst().get(1);
        Zustaendigkeit zustaendigkeitOE = fileSetup.getZustaendigkeitenOrganisationseinheit().get(1);
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        ImportUtil.pushAndWait23(fileSetup);

        String ars = zustaendigkeitOE.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
        validatorSetup
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());
        OnlineServiceLinkValidator validatorODSetup = new OnlineServiceLinkValidator(responseSetup);
        validatorODSetup.validateCount(1);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        zustaendigkeitOD.setGeloescht(true);
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        zustaendigkeitOE.setGeloescht(true);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();

        // Prueft ob das Synonym aus dem Onlinedienst aus der Leistungsbeschreibung entfernt wurde
        Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(responseList);
        validatorListOD.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullDeleteZSOEandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance()
            .name(testcasename)
            .lb(1)
            .oe(1)
            .zsOe(2)
            .od(0)
            .zsOd(0)
            .sp(1)
            .getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zustaendigkeit = fileSetup.getZustaendigkeitenOrganisationseinheit().get(1);
        List<Textmodul> textmodule = new ArrayList<>();
        textmodule.add(Textmodul.builder().leikaCode("02").inhalt("Textmodul02").positionDarstellung(1).build());
        textmodule.add(Textmodul.builder().leikaCode("03").inhalt("Textmodul03").positionDarstellung(2).build());
        textmodule.add(Textmodul.builder().leikaCode("06").inhalt("Textmodul06").positionDarstellung(3).build());
        leistung.setTextmodule(textmodule);
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
        ImportUtil.pushAndWait23(fileSetup);

        String arsOE = zustaendigkeit.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            arsOE,
            leistung.getFullId()
        );
        ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
        validatorSetup
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());
        String arsSP = spezialisierung.getArs().getFirst();
        Response responseSPSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            arsSP,
            leistung.getFullId()
        );
        ServiceDescriptionValidator validatorSPSetup = new ServiceDescriptionValidator(responseSPSetup);
        validatorSPSetup.validateId(leistung.getFullId()).validateName(spezTextmodulInhalt);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        zustaendigkeit.setGeloescht(true);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeit);
        spezialisierung.setGeloescht(true);
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOE, leistung.getFullId());
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();
        Response responseSP = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsSP, leistung.getFullId());
        ServiceDescriptionValidator validatorSP = new ServiceDescriptionValidator(responseSP);
        validatorSP.validateId(leistung.getFullId()).validateName("Textmodul03");

        // Prueft ob das Synonym aus der Spezialisierung auch in der Leistungsbeschreibung entfernt wurde.
        Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(arsSP, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(responseList);
        validatorListSP.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullDeleteODandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(0).od(2).sp(0).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().get(1);
        onlinedienst.setBezeichnung(odBezeichnung);
        Zustaendigkeit zustaendigkeitOD = fileSetup.getZustaendigkeitenOnlinedienst().get(1);
        ImportUtil.pushAndWait23(fileSetup);

        String ars = zustaendigkeitOD.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        OnlineServiceLinkValidator validatorSetup = new OnlineServiceLinkValidator(responseSetup);
        validatorSetup
            .validateCount(2)
            .validateId(0, fileSetup.getOnlinedienste().getFirst().getFullId())
            .validateId(1, onlinedienst.getFullId());

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        onlinedienst.setGeloescht(true);
        file.getOnlinedienste().add(onlinedienst);
        zustaendigkeitOD.setGeloescht(true);
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        OnlineServiceLinkValidator validator = new OnlineServiceLinkValidator(response);
        validator.validateCount(1).validateId(0, fileSetup.getOnlinedienste().getFirst().getFullId());

        // Prueft ob das Synonym aus dem Onlinedienst aus der Leistungsbeschreibung entfernt wurde
        Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(responseList);
        validatorListOD.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullDeleteZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance()
            .name(testcasename)
            .lb(1)
            .oe(0)
            .zsOe(0)
            .od(1)
            .zsOd(2)
            .sp(1)
            .getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Zustaendigkeit zustaendigkeitOD = fileSetup.getZustaendigkeitenOnlinedienst().get(1);
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        List<Textmodul> textmodule = new ArrayList<>();
        textmodule.add(Textmodul.builder().leikaCode("02").inhalt("Textmodul02").positionDarstellung(1).build());
        textmodule.add(Textmodul.builder().leikaCode("03").inhalt("Textmodul03").positionDarstellung(2).build());
        textmodule.add(Textmodul.builder().leikaCode("06").inhalt("Textmodul06").positionDarstellung(3).build());
        leistung.setTextmodule(textmodule);
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
        ImportUtil.pushAndWait23(fileSetup);

        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            arsOD,
            leistung.getFullId()
        );
        OnlineServiceLinkValidator validatorSetup = new OnlineServiceLinkValidator(responseSetup);
        validatorSetup.validateCount(1);
        String arsSP = spezialisierung.getArs().getFirst();
        Response responseSPSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            arsSP,
            leistung.getFullId()
        );
        ServiceDescriptionValidator validatorSPSetup = new ServiceDescriptionValidator(responseSPSetup);
        validatorSPSetup.validateId(leistung.getFullId()).validateName(spezTextmodulInhalt);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        zustaendigkeitOD.setGeloescht(true);
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        spezialisierung.setGeloescht(true);
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOD, leistung.getFullId());
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();
        Response responseSP = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsSP, leistung.getFullId());
        ServiceDescriptionValidator validatorSP = new ServiceDescriptionValidator(responseSP);
        validatorSP.validateId(leistung.getFullId()).validateName("Textmodul03");

        // Prueft ob das Synonym aus der Spezialisierung auch in der Leistungsbeschreibung entfernt wurde.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(arsSP, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateNotContains(leistung.getFullId());

        // Prueft ob das Synonym aus dem Onlinedienst aus der Leistungsbeschreibung entfernt wurde
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullDeleteLBandOEandZSOE(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zustaendigkeit = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst();
        ImportUtil.pushAndWait23(fileSetup);

        String ars = zustaendigkeit.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
        validatorSetup.validateId(leistung.getFullId());
        Response responseOESetup = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            organisationseinheit.getFullId()
        );
        OrganisationValidator validatorOESetup = new OrganisationValidator(responseOESetup);
        validatorOESetup.validateTitle(organisationseinheit.getTitel());

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        leistung.setGeloescht(true);
        file.getLeistungsbeschreibungen().add(leistung);
        organisationseinheit.setGeloescht(true);
        file.getOrganisationseinheiten().add(organisationseinheit);
        zustaendigkeit.setGeloescht(true);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeit);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        ErrorValidator validatorOE = new ErrorValidator(responseOE);
        validatorOE.validateNotFound();
    }

    @Test
    void fullDeleteLBandZSOEandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(2).oe(1).od(1).sp(0).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().get(1);
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        Zustaendigkeit zustaendigkeitOD = fileSetup.getZustaendigkeitenOnlinedienst().get(1);
        Zustaendigkeit zustaendigkeitOE = fileSetup.getZustaendigkeitenOrganisationseinheit().get(1);
        ImportUtil.pushAndWait23(fileSetup);

        String arsOE = zustaendigkeitOE.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            arsOE,
            leistung.getFullId()
        );
        ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
        validatorSetup
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(fileSetup.getOrganisationseinheiten().getFirst().getFullId());
        OnlineServiceLinkValidator validatorODSetup = new OnlineServiceLinkValidator(responseSetup);
        validatorODSetup.validateCount(1).validateId(0, fileSetup.getOnlinedienste().getFirst().getFullId());

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        leistung.setGeloescht(true);
        file.getLeistungsbeschreibungen().add(leistung);
        zustaendigkeitOD.setGeloescht(true);
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        zustaendigkeitOE.setGeloescht(true);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOE, leistung.getFullId());
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();

        // Prueft ob das Synonym aus dem Onlinedienst aus der Leistungsbeschreibung entfernt wurde
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(responseList);
        validatorListOD.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullDeleteLBandZSOEandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(2).oe(1).od(0).sp(1).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().get(1);
        Leistungsbeschreibung leistungSP = fileSetup.getLeistungsbeschreibungen().get(0);
        Zustaendigkeit zustaendigkeit = fileSetup.getZustaendigkeitenOrganisationseinheit().get(1);
        List<Textmodul> textmodule = new ArrayList<>();
        textmodule.add(Textmodul.builder().leikaCode("02").inhalt("Textmodul02").positionDarstellung(1).build());
        textmodule.add(Textmodul.builder().leikaCode("03").inhalt("Textmodul03").positionDarstellung(2).build());
        textmodule.add(Textmodul.builder().leikaCode("06").inhalt("Textmodul06").positionDarstellung(3).build());
        leistungSP.setTextmodule(textmodule);
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
        ImportUtil.pushAndWait23(fileSetup);

        String ars = zustaendigkeit.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
        validatorSetup
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(fileSetup.getOrganisationseinheiten().getFirst().getFullId());
        String arsSP = spezialisierung.getArs().getFirst();
        Response responseSPSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            arsSP,
            leistungSP.getFullId()
        );
        ServiceDescriptionValidator validatorSPSetup = new ServiceDescriptionValidator(responseSPSetup);
        validatorSPSetup.validateName(spezTextmodulInhalt);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        leistung.setGeloescht(true);
        file.getLeistungsbeschreibungen().add(leistung);
        zustaendigkeit.setGeloescht(true);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeit);
        spezialisierung.setGeloescht(true);
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();
        Response responseSP = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsSP, leistungSP.getFullId());
        ServiceDescriptionValidator validatorSP = new ServiceDescriptionValidator(responseSP);
        validatorSP.validateName("Textmodul03");

        // Prueft ob das Synonym aus der Spezialisierung auch in der Leistungsbeschreibung entfernt wurde.
        Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(arsSP, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(responseList);
        validatorListSP.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullDeleteLBandODandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).od(1).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        Zustaendigkeit zustaendigkeit = fileSetup.getZustaendigkeitenOnlinedienst().getFirst();
        ImportUtil.pushAndWait23(fileSetup);

        String ars = zustaendigkeit.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        OnlineServiceLinkValidator validatorSetup = new OnlineServiceLinkValidator(responseSetup);
        validatorSetup.validateCount(1).validateId(0, onlinedienst.getFullId());

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        leistung.setGeloescht(true);
        file.getLeistungsbeschreibungen().add(leistung);
        onlinedienst.setGeloescht(true);
        file.getOnlinedienste().add(onlinedienst);
        zustaendigkeit.setGeloescht(true);
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeit);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();

        // Prueft ob das Synonym aus dem Onlinedienst aus der Leistungsbeschreibung entfernt wurde
        Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(responseList);
        validatorListOD.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullDeleteOEandZSOEandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance()
            .name(testcasename)
            .lb(1)
            .oe(1)
            .zsOe(1)
            .od(1)
            .zsOd(2)
            .sp(0)
            .getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zustaendigkeitOD = fileSetup.getZustaendigkeitenOnlinedienst().getFirst();
        Zustaendigkeit zustaendigkeitOE = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst();
        ImportUtil.pushAndWait23(fileSetup);

        String arsOE = zustaendigkeitOE.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            arsOE,
            leistung.getFullId()
        );
        ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
        validatorSetup
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());
        OnlineServiceLinkValidator validatorODSetup = new OnlineServiceLinkValidator(responseSetup);
        validatorODSetup.validateCount(1).validateId(0, fileSetup.getOnlinedienste().getFirst().getFullId());

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        organisationseinheit.setGeloescht(true);
        file.getOrganisationseinheiten().add(organisationseinheit);
        zustaendigkeitOD.setGeloescht(true);
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        zustaendigkeitOE.setGeloescht(true);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOE, leistung.getFullId());
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();

        // Prueft ob das Synonym aus dem Onlinedienst aus der Leistungsbeschreibung entfernt wurde
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(responseList);
        validatorListOD.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullDeleteOEandZSOEandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(2).od(0).sp(1).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().get(1);
        Zustaendigkeit zustaendigkeit = fileSetup.getZustaendigkeitenOrganisationseinheit().get(1);
        List<Textmodul> textmodule = new ArrayList<>();
        textmodule.add(Textmodul.builder().leikaCode("02").inhalt("Textmodul02").positionDarstellung(1).build());
        textmodule.add(Textmodul.builder().leikaCode("03").inhalt("Textmodul03").positionDarstellung(2).build());
        textmodule.add(Textmodul.builder().leikaCode("06").inhalt("Textmodul06").positionDarstellung(3).build());
        leistung.setTextmodule(textmodule);
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
        ImportUtil.pushAndWait23(fileSetup);

        String ars = zustaendigkeit.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
        validatorSetup
            .validateId(leistung.getFullId())
            .validateName(spezTextmodulInhalt)
            .validateOrganisationIdsCount(2)
            .validateOrganisationIdsContains(organisationseinheit.getFullId())
            .validateOrganisationIdsContains(fileSetup.getOrganisationseinheiten().getFirst().getFullId());

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        organisationseinheit.setGeloescht(true);
        file.getOrganisationseinheiten().add(organisationseinheit);
        zustaendigkeit.setGeloescht(true);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeit);
        spezialisierung.setGeloescht(true);
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateName("Textmodul03")
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(fileSetup.getOrganisationseinheiten().getFirst().getFullId());

        // Prueft ob das Synonym aus der Spezialisierung auch in der Leistungsbeschreibung entfernt wurde.
        Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(responseList);
        validatorListSP.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullDeleteZSOEandODandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance()
            .name(testcasename)
            .lb(1)
            .oe(1)
            .zsOe(2)
            .od(1)
            .zsOd(1)
            .sp(0)
            .getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        Zustaendigkeit zustaendigkeitOD = fileSetup.getZustaendigkeitenOnlinedienst().getFirst();
        Zustaendigkeit zustaendigkeitOE = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst();
        ImportUtil.pushAndWait23(fileSetup);

        String ars = zustaendigkeitOD.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
        validatorSetup
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(fileSetup.getOrganisationseinheiten().getFirst().getFullId());
        OnlineServiceLinkValidator validatorOESetup = new OnlineServiceLinkValidator(responseSetup);
        validatorOESetup.validateCount(1).validateId(0, onlinedienst.getFullId());

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        onlinedienst.setGeloescht(true);
        file.getOnlinedienste().add(onlinedienst);
        zustaendigkeitOD.setGeloescht(true);
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        zustaendigkeitOE.setGeloescht(true);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();

        // Prueft ob das Synonym aus dem Onlinedienst aus der Leistungsbeschreibung entfernt wurde
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(responseList);
        validatorListOD.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullDeleteZSOEandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance()
            .name(testcasename)
            .lb(1)
            .oe(1)
            .zsOe(2)
            .od(1)
            .zsOd(2)
            .sp(1)
            .getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        Zustaendigkeit zustaendigkeitOD = fileSetup.getZustaendigkeitenOnlinedienst().get(1);
        Zustaendigkeit zustaendigkeitOE = fileSetup.getZustaendigkeitenOrganisationseinheit().get(1);
        List<Textmodul> textmodule = new ArrayList<>();
        textmodule.add(Textmodul.builder().leikaCode("02").inhalt("Textmodul02").positionDarstellung(1).build());
        textmodule.add(Textmodul.builder().leikaCode("03").inhalt("Textmodul03").positionDarstellung(2).build());
        textmodule.add(Textmodul.builder().leikaCode("06").inhalt("Textmodul06").positionDarstellung(3).build());
        leistung.setTextmodule(textmodule);
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
        ImportUtil.pushAndWait23(fileSetup);

        String arsOE = zustaendigkeitOE.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            arsOE,
            leistung.getFullId()
        );
        ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
        validatorSetup
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(fileSetup.getOrganisationseinheiten().getFirst().getFullId());
        OnlineServiceLinkValidator validatorOESetup = new OnlineServiceLinkValidator(responseSetup);
        validatorOESetup.validateCount(1).validateId(0, fileSetup.getOnlinedienste().getFirst().getFullId());
        String arsSP = spezialisierung.getArs().getFirst();
        Response responseSPSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            arsSP,
            leistung.getFullId()
        );
        ServiceDescriptionValidator validatorSPSetup = new ServiceDescriptionValidator(responseSPSetup);
        validatorSPSetup.validateId(leistung.getFullId()).validateName(spezTextmodulInhalt);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        zustaendigkeitOD.setGeloescht(true);
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        zustaendigkeitOE.setGeloescht(true);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        spezialisierung.setGeloescht(true);
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOE, leistung.getFullId());
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();
        Response responseSP = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsSP, leistung.getFullId());
        ServiceDescriptionValidator validatorSP = new ServiceDescriptionValidator(responseSP);
        validatorSP.validateId(leistung.getFullId()).validateName("Textmodul03");

        // Prueft ob das Synonym aus der Spezialisierung auch in der Leistungsbeschreibung entfernt wurde.
        Response responseListOE = ServiceDescriptionConnector.getServiceDescriptionsV8(arsSP, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListOE
        );
        validatorListSP.validateNotContains(leistung.getFullId());

        // Prueft ob das Synonym aus dem Onlinedienst aus der Leistungsbeschreibung entfernt wurde
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullDeleteODandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).sp(1).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        Zustaendigkeit zustaendigkeitOD = fileSetup.getZustaendigkeitenOnlinedienst().getFirst();
        List<Textmodul> textmodule = new ArrayList<>();
        textmodule.add(Textmodul.builder().leikaCode("02").inhalt("Textmodul02").positionDarstellung(1).build());
        textmodule.add(Textmodul.builder().leikaCode("03").inhalt("Textmodul03").positionDarstellung(2).build());
        textmodule.add(Textmodul.builder().leikaCode("06").inhalt("Textmodul06").positionDarstellung(3).build());
        leistung.setTextmodule(textmodule);
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
        ImportUtil.pushAndWait23(fileSetup);

        String ars = zustaendigkeitOD.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
        validatorSetup.validateId(leistung.getFullId()).validateName(spezTextmodulInhalt);
        OnlineServiceLinkValidator validatorODSetup = new OnlineServiceLinkValidator(responseSetup);
        validatorODSetup.validateCount(1).validateId(0, onlinedienst.getFullId());

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        onlinedienst.setGeloescht(true);
        file.getOnlinedienste().add(onlinedienst);
        zustaendigkeitOD.setGeloescht(true);
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        spezialisierung.setGeloescht(true);
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateId(leistung.getFullId()).validateName("Textmodul03");
        OnlineServiceLinkValidator validatorOD = new OnlineServiceLinkValidator(response);
        validatorOD.validateCount(0);

        // Prueft ob das Synonym aus der Spezialisierung auch in der Leistungsbeschreibung entfernt wurde.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateNotContains(leistung.getFullId());

        // Prueft ob das Synonym aus dem Onlinedienst aus der Leistungsbeschreibung entfernt wurde
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullDeleteLBandOEandZSOEandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        Leistungsbeschreibung leistungsbeschreibung = LbCreator.getInstance().getMin(testcasename);
        fileSetup.getLeistungsbeschreibungen().add(leistungsbeschreibung);
        Zustaendigkeit zustaendigkeit = ZsOnlinedienstCreator.getInstance().getMin(
            leistungsbeschreibung.getId(),
            onlinedienst.getId()
        );
        fileSetup.getZustaendigkeitenOnlinedienst().add(zustaendigkeit);
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zustaendigkeitOD = fileSetup.getZustaendigkeitenOnlinedienst().getFirst();
        Zustaendigkeit zustaendigkeitOE = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst();
        ImportUtil.pushAndWait23(fileSetup);

        String ars = zustaendigkeitOD.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
        validatorSetup
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());
        OnlineServiceLinkValidator validatorODSetup = new OnlineServiceLinkValidator(responseSetup);
        validatorODSetup.validateCount(1).validateId(0, onlinedienst.getFullId());
        Response responseOESetup = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            organisationseinheit.getFullId()
        );
        OrganisationValidator validatorOESetup = new OrganisationValidator(responseOESetup);
        validatorOESetup.validateTitle(organisationseinheit.getTitel());

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        leistung.setGeloescht(true);
        file.getLeistungsbeschreibungen().add(leistung);
        organisationseinheit.setGeloescht(true);
        file.getOrganisationseinheiten().add(organisationseinheit);
        zustaendigkeitOD.setGeloescht(true);
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        zustaendigkeitOE.setGeloescht(true);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        ErrorValidator validatorOE = new ErrorValidator(responseOE);
        validatorOE.validateNotFound();

        // Prueft ob das Synonym aus dem Onlinedienst aus der Leistungsbeschreibung entfernt wurde
        Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(responseList);
        validatorListOD.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullDeleteLBandOEandZSOEandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).sp(1).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zustaendigkeitOE = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst();
        List<Textmodul> textmodule = new ArrayList<>();
        textmodule.add(Textmodul.builder().leikaCode("02").inhalt("Textmodul02").positionDarstellung(1).build());
        textmodule.add(Textmodul.builder().leikaCode("03").inhalt("Textmodul03").positionDarstellung(2).build());
        textmodule.add(Textmodul.builder().leikaCode("06").inhalt("Textmodul06").positionDarstellung(3).build());
        leistung.setTextmodule(textmodule);
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
        ImportUtil.pushAndWait23(fileSetup);

        String ars = zustaendigkeitOE.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
        validatorSetup
            .validateId(leistung.getFullId())
            .validateName(spezTextmodulInhalt)
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        leistung.setGeloescht(true);
        file.getLeistungsbeschreibungen().add(leistung);
        spezialisierung.setGeloescht(true);
        file.getSpezialisierungen().add(spezialisierung);
        organisationseinheit.setGeloescht(true);
        file.getOrganisationseinheiten().add(organisationseinheit);
        zustaendigkeitOE.setGeloescht(true);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();

        // Prueft ob das Synonym aus der Spezialisierung auch in der Leistungsbeschreibung entfernt wurde.
        Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(responseList);
        validatorListSP.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullDeleteLBandZSOEandODandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Leistungsbeschreibung leistung = LbCreator.getInstance().getMin(testcasename);
        fileSetup.getLeistungsbeschreibungen().add(leistung);
        Zustaendigkeit zustaendigkeit = ZsOrganisationseinheitCreator.getInstance().getMin(
            leistung.getId(),
            fileSetup.getOrganisationseinheiten().getFirst().getId()
        );
        fileSetup.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeit);
        Leistungsbeschreibung leistungsbeschreibung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        Zustaendigkeit zustaendigkeitOD = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst();
        Zustaendigkeit zustaendigkeitOE = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst();
        ImportUtil.pushAndWait23(fileSetup);

        String arsOE = zustaendigkeitOE.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            arsOE,
            leistungsbeschreibung.getFullId()
        );
        ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
        validatorSetup
            .validateId(leistungsbeschreibung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(fileSetup.getOrganisationseinheiten().getFirst().getFullId());
        OnlineServiceLinkValidator validatorODSetup = new OnlineServiceLinkValidator(responseSetup);
        validatorODSetup.validateCount(1).validateId(0, onlinedienst.getFullId());

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        leistungsbeschreibung.setGeloescht(true);
        file.getLeistungsbeschreibungen().add(leistungsbeschreibung);
        onlinedienst.setGeloescht(true);
        file.getOnlinedienste().add(onlinedienst);
        zustaendigkeitOD.setGeloescht(true);
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        zustaendigkeitOE.setGeloescht(true);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            arsOE,
            leistungsbeschreibung.getFullId()
        );
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();

        // Prueft ob das Synonym aus dem Onlinedienst aus der Leistungsbeschreibung entfernt wurde
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(responseList);
        validatorListOD.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullDeleteLBandZSOEandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(2).oe(1).od(1).sp(1).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().get(1);
        Leistungsbeschreibung leistungSP = fileSetup.getLeistungsbeschreibungen().get(0);
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        Zustaendigkeit zustaendigkeitOD = fileSetup.getZustaendigkeitenOnlinedienst().get(1);
        Zustaendigkeit zustaendigkeitOE = fileSetup.getZustaendigkeitenOrganisationseinheit().get(1);
        List<Textmodul> textmodule = new ArrayList<>();
        textmodule.add(Textmodul.builder().leikaCode("02").inhalt("Textmodul02").positionDarstellung(1).build());
        textmodule.add(Textmodul.builder().leikaCode("03").inhalt("Textmodul03").positionDarstellung(2).build());
        textmodule.add(Textmodul.builder().leikaCode("06").inhalt("Textmodul06").positionDarstellung(3).build());
        leistungSP.setTextmodule(textmodule);
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
        ImportUtil.pushAndWait23(fileSetup);

        String ars = zustaendigkeitOE.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
        validatorSetup
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(fileSetup.getOrganisationseinheiten().getFirst().getFullId());
        OnlineServiceLinkValidator validatorODSetup = new OnlineServiceLinkValidator(responseSetup);
        validatorODSetup.validateCount(1).validateId(0, fileSetup.getOnlinedienste().getFirst().getFullId());
        String arsSP = spezialisierung.getArs().getFirst();
        Response responseSPSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            arsSP,
            leistungSP.getFullId()
        );
        ServiceDescriptionValidator validatorSPSetup = new ServiceDescriptionValidator(responseSPSetup);
        validatorSPSetup.validateId(leistungSP.getFullId()).validateName(spezTextmodulInhalt);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        leistung.setGeloescht(true);
        file.getLeistungsbeschreibungen().add(leistung);
        zustaendigkeitOD.setGeloescht(true);
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        zustaendigkeitOE.setGeloescht(true);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        spezialisierung.setGeloescht(true);
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();
        Response responseSP = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsSP, leistungSP.getFullId());
        ServiceDescriptionValidator validatorSP = new ServiceDescriptionValidator(responseSP);
        validatorSP.validateId(leistungSP.getFullId()).validateName("Textmodul03");

        // Prueft ob das Synonym aus der Spezialisierung auch in der Leistungsbeschreibung entfernt wurde.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(arsSP, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateNotContains(leistung.getFullId());

        // Prueft ob das Synonym aus dem Onlinedienst aus der Leistungsbeschreibung entfernt wurde
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullDeleteLBandODandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).od(1).sp(1).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Zustaendigkeit zustaendigkeitOD = fileSetup.getZustaendigkeitenOnlinedienst().getFirst();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        List<Textmodul> textmodule = new ArrayList<>();
        textmodule.add(Textmodul.builder().leikaCode("02").inhalt("Textmodul02").positionDarstellung(1).build());
        textmodule.add(Textmodul.builder().leikaCode("03").inhalt("Textmodul03").positionDarstellung(2).build());
        textmodule.add(Textmodul.builder().leikaCode("06").inhalt("Textmodul06").positionDarstellung(3).build());
        leistung.setTextmodule(textmodule);
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
        ImportUtil.pushAndWait23(fileSetup);

        String ars = zustaendigkeitOD.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
        validatorSetup.validateId(leistung.getFullId()).validateName(spezTextmodulInhalt);
        OnlineServiceLinkValidator validatorODSetup = new OnlineServiceLinkValidator(responseSetup);
        validatorODSetup.validateCount(1).validateId(0, onlinedienst.getFullId());

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        leistung.setGeloescht(true);
        file.getLeistungsbeschreibungen().add(leistung);
        zustaendigkeitOD.setGeloescht(true);
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        onlinedienst.setGeloescht(true);
        file.getOnlinedienste().add(onlinedienst);
        spezialisierung.setGeloescht(true);
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();

        // Prueft ob das Synonym aus der Spezialisierung auch in der Leistungsbeschreibung entfernt wurde.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateNotContains(leistung.getFullId());

        // Prueft ob das Synonym aus dem Onlinedienst aus der Leistungsbeschreibung entfernt wurde
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullDeleteOEandZSOEandODandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(2).od(1).sp(0).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        Zustaendigkeit zustaendigkeitOD = fileSetup.getZustaendigkeitenOnlinedienst().getFirst();
        Zustaendigkeit zustaendigkeitOE = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst();
        ImportUtil.pushAndWait23(fileSetup);

        String arsOE = zustaendigkeitOE.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            arsOE,
            leistung.getFullId()
        );
        ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
        validatorSetup
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(2)
            .validateOrganisationIdsContains(organisationseinheit.getFullId())
            .validateOrganisationIdsContains(fileSetup.getOrganisationseinheiten().get(1).getFullId());
        OnlineServiceLinkValidator validatorODSetup = new OnlineServiceLinkValidator(responseSetup);
        validatorODSetup.validateCount(1).validateId(0, onlinedienst.getFullId());

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        organisationseinheit.setGeloescht(true);
        file.getOrganisationseinheiten().add(organisationseinheit);
        onlinedienst.setGeloescht(true);
        file.getOnlinedienste().add(onlinedienst);
        zustaendigkeitOD.setGeloescht(true);
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        zustaendigkeitOE.setGeloescht(true);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOE, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(fileSetup.getOrganisationseinheiten().get(1).getFullId());
        OnlineServiceLinkValidator validatorOD = new OnlineServiceLinkValidator(response);
        validatorOD.validateCount(0);

        // Prueft ob das Synonym aus dem Onlinedienst aus der Leistungsbeschreibung entfernt wurde
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(responseList);
        validatorListOD.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullDeleteOEandZSOEandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance()
            .name(testcasename)
            .lb(1)
            .oe(1)
            .zsOe(1)
            .od(1)
            .zsOd(2)
            .sp(1)
            .getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zustaendigkeitOD = fileSetup.getZustaendigkeitenOnlinedienst().getFirst();
        Zustaendigkeit zustaendigkeitOE = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst();
        List<Textmodul> textmodule = new ArrayList<>();
        textmodule.add(Textmodul.builder().leikaCode("02").inhalt("Textmodul02").positionDarstellung(1).build());
        textmodule.add(Textmodul.builder().leikaCode("03").inhalt("Textmodul03").positionDarstellung(2).build());
        textmodule.add(Textmodul.builder().leikaCode("06").inhalt("Textmodul06").positionDarstellung(3).build());
        leistung.setTextmodule(textmodule);
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
        ImportUtil.pushAndWait23(fileSetup);

        String arsOE = zustaendigkeitOE.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            arsOE,
            leistung.getFullId()
        );
        ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
        validatorSetup
            .validateId(leistung.getFullId())
            .validateName(spezTextmodulInhalt)
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());
        OnlineServiceLinkValidator validatorODSetup = new OnlineServiceLinkValidator(responseSetup);
        validatorODSetup.validateCount(1).validateId(0, fileSetup.getOnlinedienste().getFirst().getFullId());
        Response responseOESetup = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            organisationseinheit.getFullId()
        );
        OrganisationValidator validatorOESetup = new OrganisationValidator(responseOESetup);
        validatorOESetup.validateTitle(organisationseinheit.getTitel());

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        organisationseinheit.setGeloescht(true);
        file.getOrganisationseinheiten().add(organisationseinheit);
        zustaendigkeitOD.setGeloescht(true);
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        zustaendigkeitOE.setGeloescht(true);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        spezialisierung.setGeloescht(true);
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOE, leistung.getFullId());
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        ErrorValidator validatorOE = new ErrorValidator(responseOE);
        validatorOE.validateNotFound();

        // Prueft ob das Synonym aus der Spezialisierung auch in der Leistungsbeschreibung entfernt wurde.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOE, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateNotContains(leistung.getFullId());

        // Prueft ob das Synonym aus dem Onlinedienst aus der Leistungsbeschreibung entfernt wurde
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullDeleteZSOEandODandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance()
            .name(testcasename)
            .lb(1)
            .oe(1)
            .zsOe(2)
            .od(1)
            .zsOd(1)
            .sp(1)
            .getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zustaendigkeitOD = fileSetup.getZustaendigkeitenOnlinedienst().getFirst();
        Zustaendigkeit zustaendigkeitOE = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst();
        List<Textmodul> textmodule = new ArrayList<>();
        textmodule.add(Textmodul.builder().leikaCode("02").inhalt("Textmodul02").positionDarstellung(1).build());
        textmodule.add(Textmodul.builder().leikaCode("03").inhalt("Textmodul03").positionDarstellung(2).build());
        textmodule.add(Textmodul.builder().leikaCode("06").inhalt("Textmodul06").positionDarstellung(3).build());
        leistung.setTextmodule(textmodule);
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
        ImportUtil.pushAndWait23(fileSetup);

        String arsOE = zustaendigkeitOE.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            arsOE,
            leistung.getFullId()
        );
        ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
        validatorSetup
            .validateId(leistung.getFullId())
            .validateName(spezTextmodulInhalt)
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());
        OnlineServiceLinkValidator validatorODSetup = new OnlineServiceLinkValidator(responseSetup);
        validatorODSetup.validateCount(1).validateId(0, onlinedienst.getFullId());

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        onlinedienst.setGeloescht(true);
        file.getOnlinedienste().add(onlinedienst);
        zustaendigkeitOD.setGeloescht(true);
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        zustaendigkeitOE.setGeloescht(true);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        spezialisierung.setGeloescht(true);
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOE, leistung.getFullId());
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();

        // Prueft ob das Synonym aus der Spezialisierung auch in der Leistungsbeschreibung entfernt wurde.
        Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOE, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(responseList);
        validatorListSP.validateNotContains(leistung.getFullId());

        // Prueft ob das Synonym aus dem Onlinedienst aus der Leistungsbeschreibung entfernt wurde
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullDeletedLBandOEandZSOEandODandZSOD(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        Zustaendigkeit zustaendigkeitOD = fileSetup.getZustaendigkeitenOnlinedienst().getFirst();
        Zustaendigkeit zustaendigkeitOE = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst();
        ImportUtil.pushAndWait23(fileSetup);

        String arsOE = zustaendigkeitOE.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            arsOE,
            leistung.getFullId()
        );
        ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
        validatorSetup
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());
        OnlineServiceLinkValidator validatorODSetup = new OnlineServiceLinkValidator(responseSetup);
        validatorODSetup.validateCount(1).validateId(0, onlinedienst.getFullId());
        Response responseOESetup = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            organisationseinheit.getFullId()
        );
        OrganisationValidator validatorOESetup = new OrganisationValidator(responseOESetup);
        validatorOESetup.validateTitle(organisationseinheit.getTitel());

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        leistung.setGeloescht(true);
        file.getLeistungsbeschreibungen().add(leistung);
        organisationseinheit.setGeloescht(true);
        file.getOrganisationseinheiten().add(organisationseinheit);
        onlinedienst.setGeloescht(true);
        file.getOnlinedienste().add(onlinedienst);
        zustaendigkeitOD.setGeloescht(true);
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        zustaendigkeitOE.setGeloescht(true);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOE, leistung.getFullId());
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        ErrorValidator validatorOE = new ErrorValidator(responseOE);
        validatorOE.validateNotFound();

        // Prueft ob das Synonym aus dem Onlinedienst aus der Leistungsbeschreibung entfernt wurde
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(responseList);
        validatorListOD.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullDeleteLBandOEandZSOEandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).sp(1).getMin();
        Leistungsbeschreibung leistungsbeschreibung = LbCreator.getInstance().getMin(testcasename);
        fileSetup.getLeistungsbeschreibungen().add(leistungsbeschreibung);
        Zustaendigkeit zustaendigkeit = ZsOnlinedienstCreator.getInstance().getMin(
            leistungsbeschreibung.getId(),
            fileSetup.getOnlinedienste().getFirst().getId(),
            2
        );
        fileSetup.getZustaendigkeitenOnlinedienst().add(zustaendigkeit);
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zustaendigkeitOE = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst();
        Zustaendigkeit zustaendigkeitOD = fileSetup.getZustaendigkeitenOnlinedienst().getFirst();
        List<Textmodul> textmodule = new ArrayList<>();
        textmodule.add(Textmodul.builder().leikaCode("02").inhalt("Textmodul02").positionDarstellung(1).build());
        textmodule.add(Textmodul.builder().leikaCode("03").inhalt("Textmodul03").positionDarstellung(2).build());
        textmodule.add(Textmodul.builder().leikaCode("06").inhalt("Textmodul06").positionDarstellung(3).build());
        leistung.setTextmodule(textmodule);
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
        ImportUtil.pushAndWait23(fileSetup);

        String arsOE = zustaendigkeitOE.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            arsOE,
            leistung.getFullId()
        );
        ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
        validatorSetup
            .validateId(leistung.getFullId())
            .validateName(spezTextmodulInhalt)
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(organisationseinheit.getFullId());
        OnlineServiceLinkValidator validatorODSetup = new OnlineServiceLinkValidator(responseSetup);
        validatorODSetup.validateCount(1).validateId(0, fileSetup.getOnlinedienste().getFirst().getFullId());
        Response responseOESetup = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            organisationseinheit.getFullId()
        );
        OrganisationValidator validatorOESetup = new OrganisationValidator(responseOESetup);
        validatorOESetup.validateTitle(organisationseinheit.getTitel());

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        leistung.setGeloescht(true);
        file.getLeistungsbeschreibungen().add(leistung);
        organisationseinheit.setGeloescht(true);
        file.getOrganisationseinheiten().add(organisationseinheit);
        zustaendigkeitOE.setGeloescht(true);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        zustaendigkeitOD.setGeloescht(true);
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        spezialisierung.setGeloescht(true);
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOE, leistung.getFullId());
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        ErrorValidator validatorOE = new ErrorValidator(responseOE);
        validatorOE.validateNotFound();

        // Prueft ob das Synonym aus der Spezialisierung auch in der Leistungsbeschreibung entfernt wurde.
        Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOE, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(responseList);
        validatorListSP.validateNotContains(leistung.getFullId());

        // Prueft ob das Synonym aus dem Onlinedienst aus der Leistungsbeschreibung entfernt wurde
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullDeleteLBandZSOEandODandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).sp(1).getMin();
        Leistungsbeschreibung leistung = LbCreator.getInstance().getMin(testcasename);
        fileSetup.getLeistungsbeschreibungen().add(leistung);
        Zustaendigkeit zustaendigkeit = ZsOrganisationseinheitCreator.getInstance().getMin(
            leistung.getId(),
            fileSetup.getOrganisationseinheiten().getFirst().getId()
        );
        fileSetup.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeit);
        Leistungsbeschreibung leistungsbeschreibung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        Zustaendigkeit zustaendigkeitOD = fileSetup.getZustaendigkeitenOnlinedienst().getFirst();
        Zustaendigkeit zustaendigkeitOE = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst();
        List<Textmodul> textmodule = new ArrayList<>();
        textmodule.add(Textmodul.builder().leikaCode("02").inhalt("Textmodul02").positionDarstellung(1).build());
        textmodule.add(Textmodul.builder().leikaCode("03").inhalt("Textmodul03").positionDarstellung(2).build());
        textmodule.add(Textmodul.builder().leikaCode("06").inhalt("Textmodul06").positionDarstellung(3).build());
        leistungsbeschreibung.setTextmodule(textmodule);
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
        ImportUtil.pushAndWait23(fileSetup);

        String arsOE = zustaendigkeitOE.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            arsOE,
            leistungsbeschreibung.getFullId()
        );
        ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
        validatorSetup
            .validateId(leistungsbeschreibung.getFullId())
            .validateName(spezTextmodulInhalt)
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(fileSetup.getOrganisationseinheiten().getFirst().getFullId());
        OnlineServiceLinkValidator validatorODSetup = new OnlineServiceLinkValidator(responseSetup);
        validatorODSetup.validateCount(1).validateId(0, fileSetup.getOnlinedienste().getFirst().getFullId());

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        leistungsbeschreibung.setGeloescht(true);
        file.getLeistungsbeschreibungen().add(leistungsbeschreibung);
        onlinedienst.setGeloescht(true);
        file.getOnlinedienste().add(onlinedienst);
        zustaendigkeitOD.setGeloescht(true);
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        zustaendigkeitOE.setGeloescht(true);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        spezialisierung.setGeloescht(true);
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            arsOE,
            leistungsbeschreibung.getFullId()
        );
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();

        // Prueft ob das Synonym aus der Spezialisierung auch in der Leistungsbeschreibung entfernt wurde.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOE, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateNotContains(leistung.getFullId());

        // Prueft ob das Synonym aus dem Onlinedienst aus der Leistungsbeschreibung entfernt wurde
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseListOE = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOE
        );
        validatorListOD.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullDeleteOEandZSOEandODandZSODandSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(2).od(1).sp(1).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        Zustaendigkeit zustaendigkeitOD = fileSetup.getZustaendigkeitenOnlinedienst().getFirst();
        Zustaendigkeit zustaendigkeitOE = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst();
        List<Textmodul> textmodule = new ArrayList<>();
        textmodule.add(Textmodul.builder().leikaCode("02").inhalt("Textmodul02").positionDarstellung(1).build());
        textmodule.add(Textmodul.builder().leikaCode("03").inhalt("Textmodul03").positionDarstellung(2).build());
        textmodule.add(Textmodul.builder().leikaCode("06").inhalt("Textmodul06").positionDarstellung(3).build());
        leistung.setTextmodule(textmodule);
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
        ImportUtil.pushAndWait23(fileSetup);

        String arsOE = zustaendigkeitOE.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            arsOE,
            leistung.getFullId()
        );
        ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
        validatorSetup
            .validateId(leistung.getFullId())
            .validateName(spezTextmodulInhalt)
            .validateOrganisationIdsCount(2)
            .validateOrganisationIdsContains(fileSetup.getOrganisationseinheiten().get(0).getFullId())
            .validateOrganisationIdsContains(fileSetup.getOrganisationseinheiten().get(1).getFullId());
        OnlineServiceLinkValidator validatorODSetup = new OnlineServiceLinkValidator(responseSetup);
        validatorODSetup.validateCount(1).validateId(0, fileSetup.getOnlinedienste().getFirst().getFullId());
        Response responseOESetup = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            organisationseinheit.getFullId()
        );
        OrganisationValidator validatorOESetup = new OrganisationValidator(responseOESetup);
        validatorOESetup.validateTitle(organisationseinheit.getTitel());

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        organisationseinheit.setGeloescht(true);
        file.getOrganisationseinheiten().add(organisationseinheit);
        onlinedienst.setGeloescht(true);
        file.getOnlinedienste().add(onlinedienst);
        zustaendigkeitOD.setGeloescht(true);
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        zustaendigkeitOE.setGeloescht(true);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        spezialisierung.setGeloescht(true);
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOE, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateName("Textmodul03")
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(fileSetup.getOrganisationseinheiten().get(1).getFullId());
        OnlineServiceLinkValidator validatorOD = new OnlineServiceLinkValidator(response);
        validatorOD.validateCount(0);
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        ErrorValidator validatorOE = new ErrorValidator(responseOE);
        validatorOE.validateNotFound();

        // Prueft ob das Synonym aus der Spezialisierung auch in der Leistungsbeschreibung entfernt wurde.
        Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOE, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(responseList);
        validatorListSP.validateNotContains(leistung.getFullId());

        // Prueft ob das Synonym aus dem Onlinedienst aus der Leistungsbeschreibung entfernt wurde
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateNotContains(leistung.getFullId());
    }

    @Test
    void fullDeleteLBandOEandZSOEandODandZSODandSP(TestInfo testInfo) {
        String randomPrefix = TestUtil.generateRandomWord();
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";
        String odBezeichnung = testcasename + "OdBezeichnung";

        //***** Setup *****
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).sp(1).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);
        Zustaendigkeit zustaendigkeitOD = fileSetup.getZustaendigkeitenOnlinedienst().getFirst();
        Zustaendigkeit zustaendigkeitOE = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst();
        List<Textmodul> textmodule = new ArrayList<>();
        textmodule.add(
            Textmodul.builder()
                .leikaCode("02")
                .inhalt(randomPrefix + "zwei")
                .positionDarstellung(1)
                .build()
        );
        textmodule.add(
            Textmodul.builder()
                .leikaCode("03")
                .inhalt(randomPrefix + "drei")
                .positionDarstellung(2)
                .build()
        );
        textmodule.add(
            Textmodul.builder()
                .leikaCode("06")
                .inhalt(randomPrefix + "06")
                .positionDarstellung(3)
                .build()
        );
        leistung.setTextmodule(textmodule);
        List<Suchbegriff> suchbegriffe = new ArrayList<>();
        Suchbegriff suchbegriff = new Suchbegriff();
        suchbegriff.setBegriff(randomPrefix + "Suchbegriff");
        suchbegriffe.add(suchbegriff);
        leistung.getSuchbegriffe().clear();
        leistung.setSuchbegriffe(suchbegriffe);
        leistung
            .getUrsprungsportale()
            .getFirst()
            .setTitle(randomPrefix + "Ursprungsportal");

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
        ImportUtil.pushAndWait23(fileSetup);

        String arsOE = zustaendigkeitOE.getArss().getFirst();
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            arsOE,
            leistung.getFullId()
        );
        ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
        validatorSetup
            .validateId(leistung.getFullId())
            .validateName(spezTextmodulInhalt)
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(fileSetup.getOrganisationseinheiten().getFirst().getFullId());
        OnlineServiceLinkValidator validatorODSetup = new OnlineServiceLinkValidator(responseSetup);
        validatorODSetup.validateCount(1).validateId(0, fileSetup.getOnlinedienste().getFirst().getFullId());
        Response responseOESetup = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            organisationseinheit.getFullId()
        );
        OrganisationValidator validatorOESetup = new OrganisationValidator(responseOESetup);
        validatorOESetup.validateTitle(organisationseinheit.getTitel());

        Response responseSuggestionSetup = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            arsOE,
            randomPrefix,
            true
        );
        SuggestionResultListValidator suggestionValidator = new SuggestionResultListValidator(responseSuggestionSetup);
        suggestionValidator.validateListContains(
            List.of(
                randomPrefix + "zwei",
                randomPrefix + "drei",
                randomPrefix + "Suchbegriff",
                randomPrefix + "Ursprungsportal"
            )
        );
        suggestionValidator.validateCount(4);

        //***** Testcase *****
        XzufiFile file = new XzufiFile();
        leistung.setGeloescht(true);
        file.getLeistungsbeschreibungen().add(leistung);
        organisationseinheit.setGeloescht(true);
        file.getOrganisationseinheiten().add(organisationseinheit);
        onlinedienst.setGeloescht(true);
        file.getOnlinedienste().add(onlinedienst);
        zustaendigkeitOD.setGeloescht(true);
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitOD);
        zustaendigkeitOE.setGeloescht(true);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        spezialisierung.setGeloescht(true);
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(arsOE, leistung.getFullId());
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();
        Response responseOE = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());
        ErrorValidator validatorOE = new ErrorValidator(responseOE);
        validatorOE.validateNotFound();

        Response responseSuggestion = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            arsOE,
            randomPrefix,
            true
        );
        suggestionValidator = new SuggestionResultListValidator(responseSuggestion);
        suggestionValidator.validateCount(0);

        // Prueft ob das Synonym aus der Spezialisierung auch in der Leistungsbeschreibung entfernt wurde.
        Response responseListSP = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOE, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
            responseListSP
        );
        validatorListSP.validateNotContains(leistung.getFullId());

        // Prueft ob das Synonym aus dem Onlinedienst aus der Leistungsbeschreibung entfernt wurde
        String arsOD = zustaendigkeitOD.getArss().getFirst();
        Response responseListOD = ServiceDescriptionConnector.getServiceDescriptionsV8(arsOD, odBezeichnung);
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
            responseListOD
        );
        validatorListOD.validateNotContains(leistung.getFullId());
    }
}
