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
package suchdienst.integrationstest.tests.trefferlistev8;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.SpCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.ModulSpezialisierung;
import de.dataport.pvog.testdatengenerator.model.Spezialisierung;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.servicedescriptionv8.ServiceDescriptionResultListValidator;

/**
 * Testet das Verhalten bei leerem oder fehlendem Textmodul mit Code 06.
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class EmptyVolltextTest {

    private static String name1;
    private static String name2;
    private static String name3;
    private static String inhalt1;
    private static String inhalt2;
    private static String inhalt3;
    private static String lbId1;
    private static String lbId2;
    private static String lbId3;
    private static String ars;

    @BeforeAll
    static void setupTestclass(TestInfo testInfo) {
        String randomPrefix = TestUtil.generateRandomWord();
        name1 = randomPrefix + "Gouda";
        name2 = randomPrefix + "Tilsiter";
        name3 = randomPrefix + "Maasdamer";
        inhalt1 = randomPrefix + "Feta";
        inhalt2 = randomPrefix + "Mozzarella";
        inhalt3 = randomPrefix + "Parmesan";
        /*
         * Setup:
         * Leistung 1 mit gefülltem Textmodul 06
         * Leistung 2 mit leerem Textmodul 06 (und Spezialisierung mit gefülltem Textmodul 06)
         * Leistung 3 ohne Textmodul 06 (und Spezialisierung mit gefülltem Textmodul 06)
         */
        String testcasename = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        //Setup mit leeren Textmodul 06
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(3).oe(1).getMin();
        Leistungsbeschreibung leistung1 = fileSetup.getLeistungsbeschreibungen().get(0);
        leistung1
            .getTextmodule()
            .stream()
            .filter(t -> t.getLeikaCode().equals("02"))
            .findFirst()
            .orElseThrow()
            .setInhalt(name1);
        leistung1
            .getTextmodule()
            .stream()
            .filter(t -> t.getLeikaCode().equals("06"))
            .findFirst()
            .orElseThrow()
            .setInhalt(inhalt1);
        lbId1 = leistung1.getFullId();
        Leistungsbeschreibung leistung2 = fileSetup.getLeistungsbeschreibungen().get(1);
        leistung2
            .getTextmodule()
            .stream()
            .filter(t -> t.getLeikaCode().equals("02"))
            .findFirst()
            .orElseThrow()
            .setInhalt(name2);
        leistung2
            .getTextmodule()
            .stream()
            .filter(t -> t.getLeikaCode().equals("06"))
            .findFirst()
            .orElseThrow()
            .setInhalt(null);
        lbId2 = leistung2.getFullId();
        Leistungsbeschreibung leistung3 = fileSetup.getLeistungsbeschreibungen().get(2);
        leistung3
            .getTextmodule()
            .stream()
            .filter(t -> t.getLeikaCode().equals("02"))
            .findFirst()
            .orElseThrow()
            .setInhalt(name3);
        leistung3
            .getTextmodule()
            .stream()
            .filter(t -> t.getLeikaCode().equals("06"))
            .findFirst()
            .ifPresent(t -> leistung3.getTextmodule().remove(t));
        lbId3 = leistung3.getFullId();

        ars = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();

        Spezialisierung spezialisierung2 = SpCreator.getInstance().getMin(testcasename, leistung2.getId());
        spezialisierung2.getModulSpezialisierungen().clear();
        spezialisierung2
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(Textmodul.builder().leikaCode("06").inhalt(inhalt2).build())
                    .build()
            );
        fileSetup.getSpezialisierungen().add(spezialisierung2);

        Spezialisierung spezialisierung3 = SpCreator.getInstance().getMin(testcasename, leistung2.getId());
        spezialisierung3.getModulSpezialisierungen().clear();
        spezialisierung3
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(Textmodul.builder().leikaCode("06").inhalt(inhalt3).build())
                    .build()
            );
        fileSetup.getSpezialisierungen().add(spezialisierung3);

        ImportUtil.pushAndWait23(fileSetup);
    }

    @Test
    void existingTextmodul06() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, name1);
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(lbId1);
        validator.validateCount(1);
        validator.validateAvailableLanguages(0, "de");

        //Suche nach Inhalt liefert die zugehörige Leistung
        Response responseVolltext = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, inhalt1);
        ServiceDescriptionResultListValidator validatorVolltext = new ServiceDescriptionResultListValidator(
            responseVolltext
        );
        validatorVolltext.validateContains(lbId1);
        validatorVolltext.validateCount(1);
    }

    @Test
    void emptyTextmodul06() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, name2);
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(lbId2);
        validator.validateCount(1);

        // Suche nach Inhalt funktioniert, da Spezialisierung indexiert wird
        Response responseVolltext = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, inhalt2);
        ServiceDescriptionResultListValidator validatorVolltext = new ServiceDescriptionResultListValidator(
            responseVolltext
        );
        validatorVolltext.validateCount(1);
    }

    @Test
    void missingTextmodul06() {
        //Suche nach Titel funktioniert
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, name3);
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(lbId3);
        validator.validateCount(1);

        // Suche nach Inhalt funktioniert, da Spezialisierung indexiert wird
        Response responseVolltext = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, inhalt3);
        ServiceDescriptionResultListValidator validatorVolltext = new ServiceDescriptionResultListValidator(
            responseVolltext
        );
        validatorVolltext.validateCount(1);
    }
}
