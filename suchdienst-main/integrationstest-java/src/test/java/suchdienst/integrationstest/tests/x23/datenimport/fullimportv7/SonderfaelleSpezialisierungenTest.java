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

import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.SpCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.ModulSpezialisierung;
import de.dataport.pvog.testdatengenerator.model.Spezialisierung;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.ServiceDescriptionValidator;
import suchdienst.integrationstest.validator.servicedescriptionv8.ServiceDescriptionResultListValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class SonderfaelleSpezialisierungenTest {

    @Test
    void fullAddArsSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String ars2 = "094729464464";
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";

        //Vorbedingung Daten erzeugen und einspielen
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        ImportUtil.pushAndWait23(fileSetup);

        //Testcase neue Ars zur Spezialisierung hinzufügen
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
        spezialisierung.getArs().add(ars2);
        Zustaendigkeit zustaendigkeitOE = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst();
        zustaendigkeitOE.getArss().add(ars2);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait23(file);

        // Prueft ob mit der hinzugefügten Ars die Leitungsbeschreibung gefunden werden kann
        String id = leistung.getFullId();
        Response responseSP = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars2, id);
        ServiceDescriptionValidator validatorSP = new ServiceDescriptionValidator(responseSP);
        validatorSP.validateArs(0, "090000000000").validateName(spezTextmodulInhalt);

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(ars2, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(responseList);
        validatorListSP.validateContains(leistung.getFullId());
    }

    @Test
    void fullModifyArsSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "Spezialisierung";
        String schemeAgencyId = "L100042";
        String setupArs = "094729464464";
        String modifyArs = "094729468468";
        BundeslandConfig bundeslandConfig = new BundeslandConfig(schemeAgencyId, List.of(setupArs, modifyArs));

        //Vorbedingung Daten erzeugen und einspielen
        XzufiFile fileSetup = XzufiFileCreator.newInstance(bundeslandConfig)
            .name(testcasename)
            .lb(1)
            .oe(1)
            .zsOe(2)
            .sp(1)
            .getMin();
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
        ImportUtil.pushAndWait23(fileSetup);

        //Testcase Ars der Spezialisierung ändern
        XzufiFile file = new XzufiFile();
        spezialisierung.setArs(List.of(modifyArs));
        file.getSpezialisierungen().add(spezialisierung);

        ImportUtil.pushAndWait23(file);

        // Prueft, dass mit der alten Ars die Spezialisierung angewandt wird
        String lbId = leistung.getFullId();
        Response responseSP2 = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(setupArs, lbId);
        ServiceDescriptionValidator validatorSP2 = new ServiceDescriptionValidator(responseSP2);
        validatorSP2.validateName("Textmodul03");

        // Prueft ob mit der modifizierten Ars die Leitungsbeschreibung gefunden werden kann
        Response responseSP = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(modifyArs, lbId);
        ServiceDescriptionValidator validatorSP = new ServiceDescriptionValidator(responseSP);
        validatorSP.validateName(spezTextmodulInhalt);
    }

    @Test
    void fullDeleteArsSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "Spezialisierung";
        String schemeAgencyId = "L100042";
        String setupArs = "091234567891";
        String deleteArs = "091234567892";
        BundeslandConfig bundeslandConfig = new BundeslandConfig(schemeAgencyId, List.of(setupArs, deleteArs));

        //Vorbedingung Daten erzeugen und einspielen
        XzufiFile fileSetup = XzufiFileCreator.newInstance(bundeslandConfig)
            .name(testcasename)
            .lb(1)
            .oe(1)
            .zsOe(2)
            .sp(1)
            .getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        List<Textmodul> textmodule = new ArrayList<>();
        textmodule.add(Textmodul.builder().leikaCode("02").inhalt("Textmodul02").positionDarstellung(1).build());
        textmodule.add(Textmodul.builder().leikaCode("03").inhalt("Textmodul03").positionDarstellung(2).build());
        textmodule.add(Textmodul.builder().leikaCode("06").inhalt("Textmodul06").positionDarstellung(3).build());
        leistung.setTextmodule(textmodule);

        Spezialisierung spezialisierung = fileSetup.getSpezialisierungen().getFirst();
        spezialisierung.getArs().add(deleteArs);
        spezialisierung.getModulSpezialisierungen().clear();
        spezialisierung
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(Textmodul.builder().leikaCode("03").inhalt(spezTextmodulInhalt).build())
                    .build()
            );
        ImportUtil.pushAndWait23(fileSetup);

        //Testcase Ars der Spezialisierung löschen
        XzufiFile file = new XzufiFile();
        spezialisierung.setArs(List.of(setupArs));
        file.getSpezialisierungen().add(spezialisierung);

        ImportUtil.pushAndWait23(file);

        // Prueft, dass mit der entfernten Ars die Spezialisierung nicht angewandt wird
        String lbId = leistung.getFullId();
        Response responseSP2 = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(deleteArs, lbId);
        ServiceDescriptionValidator validatorSP2 = new ServiceDescriptionValidator(responseSP2);
        validatorSP2.validateName("Textmodul03");

        // Prueft, ob mit der modifizierten Ars die Leitungsbeschreibung gefunden werden kann
        Response responseSP = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(setupArs, lbId);
        ServiceDescriptionValidator validatorSP = new ServiceDescriptionValidator(responseSP);
        validatorSP.validateName(spezTextmodulInhalt);
    }

    @Test
    void fullAdd2ArsSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String ars2 = "094729464464";
        String ars3 = "094729468468";
        String spezTextmodulInhalt = testcasename + "SpezTextModul03";

        //Vorbedingung Daten erzeugen und einspielen
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(2).getMin();
        ImportUtil.pushAndWait23(fileSetup);

        // Testcase neue Ars zu den Spezialisierungen hinzufügen
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
        spezialisierung.getArs().add(ars2);
        spezialisierung.getArs().add(ars3);
        Zustaendigkeit zustaendigkeitOE = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst();
        zustaendigkeitOE.getArss().add(ars2);
        zustaendigkeitOE.getArss().add(ars3);
        file.getZustaendigkeitenOrganisationseinheit().add(zustaendigkeitOE);
        file.getSpezialisierungen().add(spezialisierung);

        ImportUtil.pushAndWait23(file);

        // Prueft ob mit der ersten hinzugefügten Ars die Leitungsbeschreibung gefunden werden kann
        String id = leistung.getFullId();
        Response responseSP = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars2, id);
        ServiceDescriptionValidator validatorSP = new ServiceDescriptionValidator(responseSP);
        validatorSP.validateArs(0, "090000000000").validateName(spezTextmodulInhalt);

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseList = ServiceDescriptionConnector.getServiceDescriptionsV8(ars2, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP = new ServiceDescriptionResultListValidator(responseList);
        validatorListSP.validateContains(leistung.getFullId());

        // Prueft ob mit der zweiten hinzugefügten Ars die Leitungsbeschreibung gefunden werden kann
        String id2 = leistung.getFullId();
        Response responseSP2 = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars3, id2);
        ServiceDescriptionValidator validatorSP2 = new ServiceDescriptionValidator(responseSP2);
        validatorSP2.validateArs(0, "090000000000").validateName(spezTextmodulInhalt);

        // Prueft ob das Synonym aus der Spezialisierung auch der Leistungsbeschreibung hinzugefügt wird.
        Response responseList2 = ServiceDescriptionConnector.getServiceDescriptionsV8(ars3, spezTextmodulInhalt);
        ServiceDescriptionResultListValidator validatorListSP2 = new ServiceDescriptionResultListValidator(
            responseList2
        );
        validatorListSP2.validateContains(leistung.getFullId());
    }

    @Test
    void fullModify2ArsSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "Spezialisierung";
        String schemeAgencyId = "L100042";
        String setupArs1 = "091234567891";
        String setupArs2 = "091234567892";
        String modifyArs1 = "091234567893";
        String modifyArs2 = "091234567894";
        BundeslandConfig bundeslandConfig = new BundeslandConfig(
            schemeAgencyId,
            List.of(setupArs1, setupArs2, modifyArs1, modifyArs2)
        );

        //Vorbedingung Daten erzeugen und einspielen
        XzufiFile fileSetup = XzufiFileCreator.newInstance(bundeslandConfig)
            .name(testcasename)
            .lb(1)
            .oe(1)
            .zsOe(4)
            .sp(1)
            .getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        List<Textmodul> textmodule = new ArrayList<>();
        textmodule.add(Textmodul.builder().leikaCode("02").inhalt("Textmodul02").positionDarstellung(1).build());
        textmodule.add(Textmodul.builder().leikaCode("03").inhalt("Textmodul03").positionDarstellung(2).build());
        textmodule.add(Textmodul.builder().leikaCode("06").inhalt("Textmodul06").positionDarstellung(3).build());
        leistung.setTextmodule(textmodule);

        Spezialisierung spezialisierung = fileSetup.getSpezialisierungen().getFirst();
        spezialisierung.getArs().add(setupArs2);
        spezialisierung.getModulSpezialisierungen().clear();
        spezialisierung
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(Textmodul.builder().leikaCode("03").inhalt(spezTextmodulInhalt).build())
                    .build()
            );
        ImportUtil.pushAndWait23(fileSetup);

        //Testcase Ars der Spezialisierung ändern
        XzufiFile file = new XzufiFile();
        spezialisierung.setArs(List.of(modifyArs1, modifyArs2));
        file.getSpezialisierungen().add(spezialisierung);

        ImportUtil.pushAndWait23(file);

        // Prueft, dass mit der alten Ars die Spezialisierung angewandt wird
        String lbId = leistung.getFullId();
        Response responseSP1 = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(setupArs1, lbId);
        ServiceDescriptionValidator validatorSP1 = new ServiceDescriptionValidator(responseSP1);
        validatorSP1.validateName("Textmodul03");

        // Prueft ob mit der modifizierten Ars die Leitungsbeschreibung gefunden werden kann
        Response responseSP2 = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(modifyArs1, lbId);
        ServiceDescriptionValidator validatorSP2 = new ServiceDescriptionValidator(responseSP2);
        validatorSP2.validateName(spezTextmodulInhalt);

        // Prueft, dass mit der alten Ars die Spezialisierung angewandt wird
        Response responseSP3 = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(setupArs2, lbId);
        ServiceDescriptionValidator validatorSP3 = new ServiceDescriptionValidator(responseSP3);
        validatorSP3.validateName("Textmodul03");

        // Prueft ob mit der modifizierten Ars die Leitungsbeschreibung gefunden werden kann
        Response responseSP4 = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(modifyArs2, lbId);
        ServiceDescriptionValidator validatorSP4 = new ServiceDescriptionValidator(responseSP4);
        validatorSP4.validateName(spezTextmodulInhalt);
    }

    @Test
    void fullDelete2ArsSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "Spezialisierung";
        String schemeAgencyId = "L100042";
        String setupArs1 = "091234567891";
        String setupArs2 = "091234567892";
        String deleteArs1 = "091234567893";
        String deleteArs2 = "091234567894";
        BundeslandConfig bundeslandConfig = new BundeslandConfig(
            schemeAgencyId,
            List.of(setupArs1, setupArs2, deleteArs1, deleteArs2)
        );

        //Vorbedingung Daten erzeugen und einspielen
        XzufiFile fileSetup = XzufiFileCreator.newInstance(bundeslandConfig)
            .name(testcasename)
            .lb(1)
            .oe(1)
            .zsOe(4)
            .sp(1)
            .getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        List<Textmodul> textmodule = new ArrayList<>();
        textmodule.add(Textmodul.builder().leikaCode("02").inhalt("Textmodul02").positionDarstellung(1).build());
        textmodule.add(Textmodul.builder().leikaCode("03").inhalt("Textmodul03").positionDarstellung(2).build());
        textmodule.add(Textmodul.builder().leikaCode("06").inhalt("Textmodul06").positionDarstellung(3).build());
        leistung.setTextmodule(textmodule);

        Spezialisierung spezialisierung = fileSetup.getSpezialisierungen().getFirst();
        spezialisierung.setArs(List.of(setupArs1, setupArs2, deleteArs1, deleteArs2));
        spezialisierung.getModulSpezialisierungen().clear();
        spezialisierung
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(Textmodul.builder().leikaCode("03").inhalt(spezTextmodulInhalt).build())
                    .build()
            );
        ImportUtil.pushAndWait23(fileSetup);

        //Testcase Ars der Spezialisierung löschen
        XzufiFile file = new XzufiFile();
        spezialisierung.setArs(List.of(setupArs1, setupArs2));
        file.getSpezialisierungen().add(spezialisierung);

        ImportUtil.pushAndWait23(file);

        // Prueft, dass mit der entfernten Ars die Spezialisierung nicht angewandt wird
        String lbId = leistung.getFullId();
        Response responseSP1 = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(deleteArs1, lbId);
        ServiceDescriptionValidator validatorSP1 = new ServiceDescriptionValidator(responseSP1);
        validatorSP1.validateName("Textmodul03");

        // Prueft, ob mit der modifizierten Ars die Leitungsbeschreibung gefunden werden kann
        Response responseSP2 = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(setupArs1, lbId);
        ServiceDescriptionValidator validatorSP2 = new ServiceDescriptionValidator(responseSP2);
        validatorSP2.validateName(spezTextmodulInhalt);

        // Prueft, dass mit der entfernten Ars die Spezialisierung nicht angewandt wird
        Response responseSP3 = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(deleteArs2, lbId);
        ServiceDescriptionValidator validatorSP3 = new ServiceDescriptionValidator(responseSP3);
        validatorSP3.validateName("Textmodul03");

        // Prueft, ob mit der modifizierten Ars die Leitungsbeschreibung gefunden werden kann
        Response responseSP4 = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(setupArs2, lbId);
        ServiceDescriptionValidator validatorSP4 = new ServiceDescriptionValidator(responseSP4);
        validatorSP4.validateName(spezTextmodulInhalt);
    }
}
