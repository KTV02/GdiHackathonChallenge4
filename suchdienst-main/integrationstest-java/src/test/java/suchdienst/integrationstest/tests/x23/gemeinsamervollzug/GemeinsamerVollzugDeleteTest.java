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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import suchdienst.integrationstest.common.Constants;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.OrganisationunitsConnector;
import suchdienst.integrationstest.connectors.RelationsConnector;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.ErrorValidator;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.OrganisationValidator;
import suchdienst.integrationstest.validator.relations.RelationV3ResultListValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.FormsValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.OnlineServiceLinkValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.ServiceDescriptionValidator;
import suchdienst.integrationstest.validator.servicedescriptionv8.ServiceDescriptionResultListValidator;

/**
 * Es soll geprüft werden, dass bei Leistung im gemeinsamen Vollzug, das Löschen von Businessobjekten korrekt funktioniert
 * Basis Redaktionssystem ist Schleswig-Holstein - zusätzlich werden Daten für HH und NI angelegt bzw. gelöscht
 */
class GemeinsamerVollzugDeleteTest {

    private static final BundeslandConfig BC_SH = new BundeslandConfig(
        "SH",
        "L100012",
        "010000000000",
        List.of("010575755063")
    );

    private static final BundeslandConfig BC_NI = new BundeslandConfig(
        "NI",
        "L100040",
        "030000000000",
        List.of("033555401027")
    );

    private static final BundeslandConfig BC_HH = new BundeslandConfig(
        "HH",
        "S100002",
        "020000000000",
        List.of("020000000000")
    );

    public static final List<String> BL_ARS = List.of(
        BC_SH.getArss().getFirst(),
        BC_HH.getArss().getFirst(),
        BC_NI.getArss().getFirst()
    );

    @Test
    void deleteZSOE(TestInfo testInfo) {
        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());

        //***** Setup *****
        // Testszenario Setup
        // LB (SH)
        // OE SH mit ZS SH, HH, NI
        XzufiFile fileSetup = XzufiFileCreator.newInstance(BC_SH).name(testclassname).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        leistung.setGemeinsamerVollzug(true);
        leistung.setGueltigkeitGebietIDs(List.of(BC_SH.getBasicArs(), BC_HH.getBasicArs(), BC_NI.getBasicArs()));
        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().getFirst();

        Zustaendigkeit zustaendigkeitHH = ZsOrganisationseinheitCreator.getInstance(BC_SH).getMin(
            leistung.getId(),
            organisationseinheit.getId(),
            1
        );
        zustaendigkeitHH.setArss(BC_HH.getArss());

        Zustaendigkeit zustaendigkeitNI = ZsOrganisationseinheitCreator.getInstance(BC_SH).getMin(
            leistung.getId(),
            organisationseinheit.getId(),
            1
        );
        zustaendigkeitNI.setArss(BC_NI.getArss());

        fileSetup.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitHH);
        fileSetup.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitNI);
        ImportUtil.pushAndWait23(fileSetup);

        String leistungFullId = leistung.getFullId();
        BL_ARS.forEach(ars -> {
            Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistungFullId);
            ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
            validatorSetup.validateId(leistungFullId).validateArsFederalstateCount(3);
            Response responseSetup2 = RelationsConnector.getRelationsLbIdsArsV3(
                List.of(leistungFullId),
                ars,
                0,
                10,
                Constants.XzufiVersion.V2_3_1
            );
            RelationV3ResultListValidator validatorSetup2 = new RelationV3ResultListValidator(responseSetup2);
            validatorSetup2.validateCount(1);
        });

        //***** Testcase *****
        //Testszenario - Löschen
        // Löschen von ZS HH und ZS NI von OE SH
        XzufiFile file = new XzufiFile();
        zustaendigkeitHH.setGeloescht(true);
        zustaendigkeitNI.setGeloescht(true);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitHH);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitNI);
        leistung.setGueltigkeitGebietIDs(List.of(BC_SH.getBasicArs()));
        file.getLeistungsbeschreibungen().add(leistung);
        ImportUtil.pushAndWait23(file);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            BC_SH.getArss().getFirst(),
            leistungFullId
        );
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateId(leistungFullId).validateArsFederalstateCount(1);
        Response responseSetup2 = RelationsConnector.getRelationsLbIdsArsV3(
            List.of(leistungFullId),
            BC_SH.getArss().getFirst(),
            0,
            10,
            Constants.XzufiVersion.V2_3_1
        );
        RelationV3ResultListValidator validatorSetup2 = new RelationV3ResultListValidator(responseSetup2);
        validatorSetup2.validateCount(1);

        List.of(BC_HH.getArss().getFirst(), BC_NI.getArss().getFirst()).forEach(ars -> {
            Response responseLand = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistungFullId);
            ErrorValidator validatorLand = new ErrorValidator(responseLand);
            validatorLand.validateNotFound();
        });
    }

    @Test
    void deleteZSOD(TestInfo testInfo) {
        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        String odBezeichnung = testclassname + "OdBezeichnung";

        //***** Setup *****
        // Testszenario Setup
        // LB (SH)
        // OD SH mit ZS SH, HH, NI
        XzufiFile fileSetup = XzufiFileCreator.newInstance(BC_SH).name(testclassname).lb(1).od(1).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        leistung.setGemeinsamerVollzug(true);
        leistung.setGueltigkeitGebietIDs(List.of(BC_SH.getBasicArs(), BC_HH.getBasicArs(), BC_NI.getBasicArs()));
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(odBezeichnung);

        Zustaendigkeit zustaendigkeitHH = ZsOnlinedienstCreator.getInstance(BC_SH).getMin(
            leistung.getId(),
            onlinedienst.getId(),
            1
        );
        zustaendigkeitHH.setArss(BC_HH.getArss());

        Zustaendigkeit zustaendigkeitNI = ZsOnlinedienstCreator.getInstance(BC_SH).getMin(
            leistung.getId(),
            onlinedienst.getId(),
            1
        );
        zustaendigkeitNI.setArss(BC_NI.getArss());

        fileSetup.getZustaendigkeitenOnlinedienst().add(zustaendigkeitHH);
        fileSetup.getZustaendigkeitenOnlinedienst().add(zustaendigkeitNI);
        ImportUtil.pushAndWait23(fileSetup);

        String leistungFullId = leistung.getFullId();
        BL_ARS.forEach(ars -> {
            Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
                ars,
                leistung.getFullId()
            );
            OnlineServiceLinkValidator validatorSetup = new OnlineServiceLinkValidator(responseSetup);
            validatorSetup.validateCount(1);
        });

        //***** Testcase *****
        //Testszenario - Löschen
        // Löschen von ZS HH und ZS NI von OD SH
        XzufiFile file = new XzufiFile();
        zustaendigkeitHH.setGeloescht(true);
        zustaendigkeitNI.setGeloescht(true);
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitHH);
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeitNI);
        leistung.setGueltigkeitGebietIDs(List.of(BC_SH.getBasicArs()));
        file.getLeistungsbeschreibungen().add(leistung);
        ImportUtil.pushAndWait23(file);

        List.of(BC_HH.getArss().getFirst(), BC_NI.getArss().getFirst()).forEach(ars -> {
            Response responseLand = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistungFullId);
            ErrorValidator validatorLand = new ErrorValidator(responseLand);
            validatorLand.validateNotFound();

            Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, odBezeichnung);
            ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(
                responseList
            );
            validatorListOD.validateNotContains(leistung.getFullId());
        });
    }

    @Test
    void deleteZSFO(TestInfo testInfo) {
        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        String foBezeichnung = testclassname + "FoBezeicnung";

        //***** Setup *****
        // Testszenario Setup
        // LB (SH)
        // FO SH mit ZS SH, HH, NI
        XzufiFile fileSetup = XzufiFileCreator.newInstance(BC_SH).name(testclassname).lb(1).oe(1).fo(1).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        leistung.setGemeinsamerVollzug(true);
        leistung.setGueltigkeitGebietIDs(List.of(BC_SH.getBasicArs(), BC_HH.getBasicArs(), BC_NI.getBasicArs()));
        Formular formular = fileSetup.getFormulare().getFirst();
        formular.setBezeichnung(foBezeichnung);

        // OE mit Zuständigkeiten ausstatten, damit Formulare gefunden werden können
        fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst().setArss(BL_ARS);

        String formularId = formular.getId();
        Zustaendigkeit zustaendigkeitHH = ZsFormularCreator.getInstance(BC_SH).getMin(leistung.getId(), formularId);
        zustaendigkeitHH.setArss(BC_HH.getArss());

        Zustaendigkeit zustaendigkeitNI = ZsFormularCreator.getInstance(BC_SH).getMin(leistung.getId(), formularId);
        zustaendigkeitNI.setArss(BC_NI.getArss());

        fileSetup.getZustaendigkeitenFormular().add(zustaendigkeitHH);
        fileSetup.getZustaendigkeitenFormular().add(zustaendigkeitNI);
        ImportUtil.pushAndWait23(fileSetup);

        String leistungFullId = leistung.getFullId();
        BL_ARS.forEach(ars -> {
            Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
                ars,
                leistung.getFullId()
            );
            FormsValidator validatorSetup = new FormsValidator(responseSetup);
            validatorSetup.validateCount(1);
        });

        //***** Testcase *****
        //Testszenario - Löschen
        // Löschen von ZS HH und ZS NI
        XzufiFile file = new XzufiFile();
        zustaendigkeitHH.setGeloescht(true);
        zustaendigkeitNI.setGeloescht(true);
        file.getZustaendigkeitenFormular().add(zustaendigkeitHH);
        file.getZustaendigkeitenFormular().add(zustaendigkeitNI);
        leistung.setGueltigkeitGebietIDs(List.of(BC_SH.getBasicArs()));
        file.getLeistungsbeschreibungen().add(leistung);
        ImportUtil.pushAndWait23(file);

        List.of(BC_HH.getArss().getFirst(), BC_NI.getArss().getFirst()).forEach(ars -> {
            Response responseLand = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistungFullId);
            FormsValidator validatorLand = new FormsValidator(responseLand);
            validatorLand.validateCount(0);

            Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, foBezeichnung);
            ServiceDescriptionResultListValidator validatorListFO = new ServiceDescriptionResultListValidator(
                responseList
            );
            validatorListFO.validateNotContains(leistung.getFullId());
        });
    }

    @Test
    void deleteSPGueltigkeit(TestInfo testInfo) {
        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        String spezTextmodulInhalt = testclassname + "SpezTextModul03";

        //***** Setup *****
        // Testszenario Setup
        // LB (SH)
        // OE SH mit ZS SH, HH, NI
        // SP SH mit Gueltigkeit SH, HH, NI
        XzufiFile fileSetup = XzufiFileCreator.newInstance(BC_SH).name(testclassname).lb(1).sp(1).oe(1).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        leistung.setGemeinsamerVollzug(true);
        leistung.setGueltigkeitGebietIDs(List.of(BC_SH.getBasicArs(), BC_HH.getBasicArs(), BC_NI.getBasicArs()));
        fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().add(BC_HH.getArss().getFirst());
        fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().add(BC_NI.getArss().getFirst());
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
        spezialisierung.setArs(BL_ARS);
        ImportUtil.pushAndWait23(fileSetup);

        BL_ARS.forEach(ars -> {
            Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
                ars,
                leistung.getFullId()
            );
            ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
            validatorSetup.validateName(spezTextmodulInhalt);
        });

        //***** Testcase *****
        //Testszenario - Löschen
        // Löschen von Gültigkeit für HH und NI an SP HH
        XzufiFile file = new XzufiFile();
        spezialisierung.setArs(List.of(BC_SH.getArss().getFirst()));
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait23(file);
        // SH Spezialisierung bleibt
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            BC_SH.getArss().getFirst(),
            leistung.getFullId()
        );
        ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
        validatorSetup.validateName(spezTextmodulInhalt);
        // HH und NI verlieren Spezialisierung
        List.of(BC_HH.getArss().getFirst(), BC_NI.getArss().getFirst()).forEach(ars -> {
            Response responseLand = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
                ars,
                leistung.getFullId()
            );
            ServiceDescriptionValidator validatorLand = new ServiceDescriptionValidator(responseLand);
            validatorLand.validateName("Textmodul03");

            // Prueft ob das Synonym aus der Spezialisierung auch in der Leistungsbeschreibung entfernt wurde.
            Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
            ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
                responseList
            );
            validatorListSP.validateNotContains(leistung.getFullId());
        });
    }

    @Test
    void deleteSP(TestInfo testInfo) {
        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        String spezTextmodulInhalt = testclassname + "SpezTextModul03";

        //***** Setup *****
        // Testszenario Setup
        // LB (SH)
        // OE SH mit ZS SH, HH, NI
        // SP SH mit Gueltigkeit SH, HH, NI
        XzufiFile fileSetup = XzufiFileCreator.newInstance(BC_SH).name(testclassname).lb(1).sp(1).oe(1).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        leistung.setGemeinsamerVollzug(true);
        leistung.setGueltigkeitGebietIDs(List.of(BC_SH.getBasicArs(), BC_HH.getBasicArs(), BC_NI.getBasicArs()));
        fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().add(BC_HH.getArss().getFirst());
        fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().add(BC_NI.getArss().getFirst());
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
        spezialisierung.setArs(BL_ARS);
        ImportUtil.pushAndWait23(fileSetup);

        BL_ARS.forEach(ars -> {
            Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
                ars,
                leistung.getFullId()
            );
            ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
            validatorSetup.validateName(spezTextmodulInhalt);
        });

        //***** Testcase *****
        //Testszenario - Löschen
        // Löschen von SP
        XzufiFile file = new XzufiFile();
        spezialisierung.setGeloescht(true);
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait23(file);
        BL_ARS.forEach(ars -> {
            Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
                ars,
                leistung.getFullId()
            );
            ServiceDescriptionValidator validatorSetup = new ServiceDescriptionValidator(responseSetup);
            validatorSetup.validateName("Textmodul03");

            // Prueft ob das Synonym aus der Spezialisierung auch in der Leistungsbeschreibung entfernt wurde.
            Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, spezTextmodulInhalt);
            ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(
                responseList
            );
            validatorListSP.validateNotContains(leistung.getFullId());
        });
    }

    @Test
    void deleteOEandZSOE(TestInfo testInfo) {
        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());

        //***** Setup *****
        // Testszenario Setup
        // LB (SH)
        // OE 1 SH mit ZS SH
        // OE 2 SH mit ZS SH, HH, NI
        // OE 3 HH mit ZS SH
        // OE 4 NI mit ZS NI
        XzufiFile fileSetup = XzufiFileCreator.newInstance(BC_SH).name(testclassname).lb(1).oe(2).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        leistung.setGemeinsamerVollzug(true);
        leistung.setGueltigkeitGebietIDs(List.of(BC_SH.getBasicArs(), BC_HH.getBasicArs(), BC_NI.getBasicArs()));

        Organisationseinheit organisationseinheit = fileSetup.getOrganisationseinheiten().get(1);
        Zustaendigkeit zustaendigkeit = fileSetup.getZustaendigkeitenOrganisationseinheit().get(1);
        zustaendigkeit.setArss(BL_ARS);

        Map<BundeslandConfig, Organisationseinheit> bcOeToDeleteMap = new HashMap<>();
        Map<BundeslandConfig, Zustaendigkeit> bcZsToDeleteMap = new HashMap<>();
        //OE und ZSOE für HH und NI anlegen
        List.of(BC_HH, BC_NI).forEach(bc -> {
            Organisationseinheit organisationseinheitLand = OeCreator.getInstance(BC_SH).getMin(
                testclassname + bc.getKuerzel()
            );
            bcOeToDeleteMap.put(bc, organisationseinheitLand);
            fileSetup.getOrganisationseinheiten().add(organisationseinheitLand);
            Zustaendigkeit zustaendigkeitLand = ZsOrganisationseinheitCreator.getInstance(BC_SH).getMin(
                leistung.getId(),
                organisationseinheitLand.getId(),
                1
            );
            zustaendigkeitLand.setArss(bc.getArss());
            fileSetup.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitLand);
            bcZsToDeleteMap.put(bc, zustaendigkeitLand);
            fileSetup.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitLand);
        });

        ImportUtil.pushAndWait23(fileSetup);

        //Validiere SH
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            BC_SH.getArss().getFirst(),
            leistung.getFullId()
        );
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
        //Validiere HH und NI
        List.of(BC_HH, BC_NI).forEach(bc -> {
            Response responseLand = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
                bc.getArss().getFirst(),
                leistung.getFullId()
            );
            ServiceDescriptionValidator validatorLand = new ServiceDescriptionValidator(responseLand);
            validatorLand
                .validateId(leistung.getFullId())
                .validateOrganisationIdsCount(2)
                .validateOrganisationIdsContains(organisationseinheit.getFullId())
                .validateOrganisationIdsContains(bcOeToDeleteMap.get(bc).getFullId());
            Response responseOELand = OrganisationunitsConnector.getOrganisationunitsDetailV5(
                bcOeToDeleteMap.get(bc).getFullId()
            );
            OrganisationValidator validatorOELand = new OrganisationValidator(responseOELand);
            validatorOELand.validateTitle(bcOeToDeleteMap.get(bc).getTitel());
        });

        //***** Testcase *****
        //Testszenario - Löschen
        // Löschen von OE 2 SH mit ZS SH, HH, NI
        // Löschen von OE 3 HH mit ZS SH
        // Löschen von OE 4 NI mit ZS NI

        XzufiFile file = new XzufiFile();
        organisationseinheit.setGeloescht(true);
        file.getOrganisationseinheiten().add(organisationseinheit);
        zustaendigkeit.setGeloescht(true);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeit);

        List.of(BC_HH, BC_NI).forEach(bc -> {
            bcOeToDeleteMap.get(bc).setGeloescht(true);
            file.getOrganisationseinheiten().add(bcOeToDeleteMap.get(bc));
            bcZsToDeleteMap.get(bc).setGeloescht(true);
            file.getZustaendigkeitenOrganisationseinheit().add(bcZsToDeleteMap.get(bc));
        });

        ImportUtil.pushAndWait23(file);

        //Validiere SH
        Response responseDelete = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            BC_SH.getArss().getFirst(),
            leistung.getFullId()
        );
        ServiceDescriptionValidator validatorDelete = new ServiceDescriptionValidator(responseDelete);
        validatorDelete
            .validateId(leistung.getFullId())
            .validateOrganisationIdsCount(1)
            .validateOrganisationIdsContains(fileSetup.getOrganisationseinheiten().getFirst().getFullId());
        Response responseOEDelete = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            fileSetup.getOrganisationseinheiten().getFirst().getFullId()
        );
        OrganisationValidator validatorOEDelete = new OrganisationValidator(responseOEDelete);
        validatorOEDelete.validateTitle(fileSetup.getOrganisationseinheiten().getFirst().getTitel());

        List.of(BC_HH, BC_NI).forEach(bc -> {
            Response responseLand = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
                bc.getArss().getFirst(),
                leistung.getFullId()
            );
            ErrorValidator validatorLand = new ErrorValidator(responseLand);
            validatorLand.validateNotFound();

            Response responseOELand = OrganisationunitsConnector.getOrganisationunitsDetailV5(
                bcOeToDeleteMap.get(bc).getFullId()
            );
            ErrorValidator validatorOELand = new ErrorValidator(responseOELand);
            validatorOELand.validateNotFound();
        });
    }

    @Test
    void deleteODandZSOD(TestInfo testInfo) {
        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        String odBezeichnung = testclassname + "OdBezeichnung";
        String odBezeichnungLand = testclassname + "OdBLandezeichnung";

        //***** Setup *****
        // Testszenario Setup
        // LB (SH)
        // OD 1 SH mit ZS SH
        // OD 2 SH mit ZS SH, HH, NI
        // OD 3 HH mit ZS SH
        // OD 4 NI mit ZS NI
        XzufiFile fileSetup = XzufiFileCreator.newInstance(BC_SH).name(testclassname).lb(1).od(2).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        leistung.setGemeinsamerVollzug(true);
        leistung.setGueltigkeitGebietIDs(List.of(BC_SH.getBasicArs(), BC_HH.getBasicArs(), BC_NI.getBasicArs()));
        Onlinedienst onlinedienst = fileSetup.getOnlinedienste().get(1);
        onlinedienst.setBezeichnung(odBezeichnung);
        Zustaendigkeit zustaendigkeit = fileSetup.getZustaendigkeitenOnlinedienst().get(1);
        zustaendigkeit.setArss(BL_ARS);

        Map<BundeslandConfig, Onlinedienst> bcOdToDeleteMap = new HashMap<>();
        Map<BundeslandConfig, Zustaendigkeit> bcZsToDeleteMap = new HashMap<>();
        //OD und ZSOD für HH und NI anlegen
        List.of(BC_HH, BC_NI).forEach(bc -> {
            Onlinedienst onlinedienstLand = OdCreator.getInstance(BC_SH).getMin(testclassname + bc.getKuerzel());
            onlinedienstLand.setBezeichnung(odBezeichnungLand + bc.getKuerzel());
            bcOdToDeleteMap.put(bc, onlinedienstLand);
            fileSetup.getOnlinedienste().add(onlinedienstLand);
            Zustaendigkeit zustaendigkeitLand = ZsOnlinedienstCreator.getInstance(BC_SH).getMin(
                leistung.getId(),
                onlinedienstLand.getId(),
                1
            );
            zustaendigkeitLand.setArss(bc.getArss());
            fileSetup.getZustaendigkeitenOnlinedienst().add(zustaendigkeitLand);
            bcZsToDeleteMap.put(bc, zustaendigkeitLand);
            fileSetup.getZustaendigkeitenOnlinedienst().add(zustaendigkeitLand);
        });

        ImportUtil.pushAndWait23(fileSetup);

        //Validiere SH
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            BC_SH.getArss().getFirst(),
            leistung.getFullId()
        );
        OnlineServiceLinkValidator validatorSetup = new OnlineServiceLinkValidator(responseSetup);
        validatorSetup
            .validateCount(2)
            .validateHasId(fileSetup.getOnlinedienste().getFirst().getFullId())
            .validateHasId(onlinedienst.getFullId());

        //Validiere HH und NI
        List.of(BC_HH, BC_NI).forEach(bc -> {
            Response responseLand = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
                bc.getArss().getFirst(),
                leistung.getFullId()
            );
            OnlineServiceLinkValidator validatorLand = new OnlineServiceLinkValidator(responseLand);
            validatorLand
                .validateCount(2)
                .validateHasId(onlinedienst.getFullId())
                .validateHasId(bcOdToDeleteMap.get(bc).getFullId());
        });

        //***** Testcase *****
        //Testszenario - Löschen
        // Löschen von OD 2 SH mit ZS SH, HH, NI
        // Löschen von OD 3 HH mit ZS SH
        // Löschen von OD 4 NI mit ZS NI
        XzufiFile file = new XzufiFile();
        onlinedienst.setGeloescht(true);
        file.getOnlinedienste().add(onlinedienst);
        zustaendigkeit.setGeloescht(true);
        file.getZustaendigkeitenOnlinedienst().add(zustaendigkeit);
        List.of(BC_HH, BC_NI).forEach(bc -> {
            bcOdToDeleteMap.get(bc).setGeloescht(true);
            file.getOnlinedienste().add(bcOdToDeleteMap.get(bc));
            bcZsToDeleteMap.get(bc).setGeloescht(true);
            file.getZustaendigkeitenOnlinedienst().add(bcZsToDeleteMap.get(bc));
        });
        ImportUtil.pushAndWait23(file);

        //Validiere SH
        Response responseDelete = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            BC_SH.getArss().getFirst(),
            leistung.getFullId()
        );
        OnlineServiceLinkValidator validatorDelete = new OnlineServiceLinkValidator(responseDelete);
        validatorDelete.validateCount(1).validateId(0, fileSetup.getOnlinedienste().getFirst().getFullId());
        // Prueft ob das Synonym aus dem Onlinedienst aus der Leistungsbeschreibung entfernt wurde
        Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(
            BC_SH.getArss().getFirst(),
            odBezeichnung
        );
        ServiceDescriptionResultListValidator validatorListOD = new ServiceDescriptionResultListValidator(responseList);
        validatorListOD.validateNotContains(leistung.getFullId());
        //Bezeichnungen aus Onlinediensten von HH und NI sollen ebenfalls nicht mehr zu einem Treffer führen
        responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(
            BC_SH.getArss().getFirst(),
            odBezeichnungLand + BC_HH.getKuerzel()
        );
        validatorListOD = new ServiceDescriptionResultListValidator(responseList);
        validatorListOD.validateNotContains(leistung.getFullId());

        responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(
            BC_SH.getArss().getFirst(),
            odBezeichnungLand + BC_NI.getKuerzel()
        );
        validatorListOD = new ServiceDescriptionResultListValidator(responseList);
        validatorListOD.validateNotContains(leistung.getFullId());
        List.of(BC_HH, BC_NI).forEach(bc -> {
            Response responseLand = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
                bc.getArss().getFirst(),
                leistung.getFullId()
            );
            ErrorValidator validatorLand = new ErrorValidator(responseLand);
            validatorLand.validateNotFound();
        });
    }

    @Test
    void deleteFOandZSFO(TestInfo testInfo) {
        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = randomPrefix + TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        String foBezeichnung = testclassname + "FoBezeichnung";
        String foBezeichnungLand = testclassname + "FoBLandezeichnung";

        //***** Setup *****
        // Testszenario Setup
        // LB (SH)
        // FO 1 SH mit ZS SH
        // FO 2 SH mit ZS SH, HH, NI
        // FO 3 HH mit ZS SH
        // FO 4 NI mit ZS NI
        XzufiFile fileSetup = XzufiFileCreator.newInstance(BC_SH).name(testclassname).lb(1).oe(2).fo(2).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        leistung.setGemeinsamerVollzug(true);
        leistung.setGueltigkeitGebietIDs(List.of(BC_SH.getBasicArs(), BC_HH.getBasicArs(), BC_NI.getBasicArs()));
        Formular formular = fileSetup.getFormulare().get(1);
        formular.setBezeichnung(foBezeichnung);
        Zustaendigkeit zustaendigkeit = fileSetup.getZustaendigkeitenFormular().get(1);
        zustaendigkeit.setArss(BL_ARS);
        fileSetup.getZustaendigkeitenOrganisationseinheit().get(1).setArss(BL_ARS);

        Map<BundeslandConfig, Formular> bcFoToDeleteMap = new HashMap<>();
        Map<BundeslandConfig, Zustaendigkeit> bcZsToDeleteMap = new HashMap<>();
        //FO,OE und ZSFO,ZSOE für HH und NI anlegen
        List.of(BC_HH, BC_NI).forEach(bc -> {
            Formular formularLand = FoCreator.getInstance(BC_SH).getMin(testclassname + bc.getKuerzel());
            formularLand.setBezeichnung(foBezeichnungLand + bc.getKuerzel());
            bcFoToDeleteMap.put(bc, formularLand);
            fileSetup.getFormulare().add(formularLand);
            Zustaendigkeit zustaendigkeitLand = ZsFormularCreator.getInstance(BC_SH).getMin(
                leistung.getId(),
                formularLand.getId()
            );
            zustaendigkeitLand.setArss(bc.getArss());
            fileSetup.getZustaendigkeitenFormular().add(zustaendigkeitLand);
            bcZsToDeleteMap.put(bc, zustaendigkeitLand);

            Organisationseinheit oeLand = OeCreator.getInstance(BC_SH).getMin(testclassname + bc.getKuerzel());
            fileSetup.getOrganisationseinheiten().add(oeLand);
            Zustaendigkeit zsoeLand = ZsOrganisationseinheitCreator.getInstance(BC_SH).getMin(
                leistung.getId(),
                oeLand.getId()
            );
            zsoeLand.setArss(bc.getArss());
            fileSetup.getZustaendigkeitenOrganisationseinheit().add(zsoeLand);
        });

        ImportUtil.pushAndWait23(fileSetup);

        //Validiere SH
        Response responseSetup = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            BC_SH.getArss().getFirst(),
            leistung.getFullId()
        );
        FormsValidator validatorSetup = new FormsValidator(responseSetup);
        validatorSetup
            .validateCount(2)
            .validateHasId(fileSetup.getFormulare().getFirst().getFullId())
            .validateHasId(formular.getFullId());

        //Validiere HH und NI
        List.of(BC_HH, BC_NI).forEach(bc -> {
            Response responseLand = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
                bc.getArss().getFirst(),
                leistung.getFullId()
            );
            FormsValidator validatorLand = new FormsValidator(responseLand);
            validatorLand
                .validateCount(2)
                .validateHasId(formular.getFullId())
                .validateHasId(bcFoToDeleteMap.get(bc).getFullId());
        });

        //***** Testcase *****
        //Testszenario - Löschen
        // Löschen von FO 2 SH mit ZS SH, HH, NI
        // Löschen von FO 3 HH mit ZS SH
        // Löschen von FO 4 NI mit ZS NI
        XzufiFile file = new XzufiFile();
        formular.setGeloescht(true);
        file.getFormulare().add(formular);
        zustaendigkeit.setGeloescht(true);
        file.getZustaendigkeitenFormular().add(zustaendigkeit);
        List.of(BC_HH, BC_NI).forEach(bc -> {
            bcFoToDeleteMap.get(bc).setGeloescht(true);
            file.getFormulare().add(bcFoToDeleteMap.get(bc));
            bcZsToDeleteMap.get(bc).setGeloescht(true);
            file.getZustaendigkeitenFormular().add(bcZsToDeleteMap.get(bc));
        });
        ImportUtil.pushAndWait23(file);

        //Validiere SH
        Response responseDelete = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            BC_SH.getArss().getFirst(),
            leistung.getFullId()
        );
        FormsValidator validatorDelete = new FormsValidator(responseDelete);
        validatorDelete.validateCount(1).validateId(0, fileSetup.getFormulare().getFirst().getFullId());
        List.of(BC_HH, BC_NI).forEach(bc -> {
            Response responseLand = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
                bc.getArss().getFirst(),
                leistung.getFullId()
            );
            FormsValidator validatorLand = new FormsValidator(responseLand);
            validatorLand.validateCount(0);
        });
    }
}
