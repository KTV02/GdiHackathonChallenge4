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
package suchdienst.integrationstest.tests.x22.autocomplete;

import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig;
import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Suchbegriff;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.servicedescriptionssuggestionv1.SuggestionResultListValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class LeistungTest {

    private static String randomPrefix;
    private static String ars;
    //fields used for suggestions
    private static String firstbeschreibungTM02;
    private static String firstTitelTM03;
    private static String firstBegriffImKontextA;
    private static String firstBegriffImKontextB;
    private static String firstUrsprungsportalTitel;

    //fields not used for suggestion but used for matching resultlist
    private static String firstOdBezeichnung;
    private static String firstOdBeschreibung;
    private static String firstVolltextTM06;

    @BeforeAll
    static void setupTestclass(TestInfo testInfo) {
        randomPrefix = TestUtil.generateRandomWord();
        String testclassname = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        BundeslandConfig bundeslandConfig = new BundeslandConfig(Bundesland.BY);

        // Daten erzeugen
        XzufiFile file = XzufiFileCreator.newInstance(bundeslandConfig).name(testclassname).lb(1).oe(1).od(1).getMin();

        //set relevant values for trefferliste
        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();

        List<Textmodul> textmodule = new ArrayList<>();
        firstbeschreibungTM02 = randomPrefix + "suggBeschreibung02tm";
        Textmodul tm02 = Textmodul.builder()
            .inhalt(firstbeschreibungTM02)
            .positionDarstellung(2)
            .leikaCode("02")
            .build();
        firstTitelTM03 = randomPrefix + "suggTitel03tm";
        Textmodul tm03 = Textmodul.builder().inhalt(firstTitelTM03).positionDarstellung(3).leikaCode("03").build();
        firstVolltextTM06 = randomPrefix + "suggVolltext06tm";
        Textmodul tm06 = Textmodul.builder().inhalt(firstVolltextTM06).positionDarstellung(6).leikaCode("06").build();
        textmodule.add(tm02);
        textmodule.add(tm03);
        textmodule.add(tm06);
        lb.setTextmodule(textmodule);

        List<Suchbegriff> suchbegriffe = new ArrayList<>();
        firstBegriffImKontextA = randomPrefix + "suggBegriffImKontextA";
        Suchbegriff suchbegriffA = new Suchbegriff();
        suchbegriffA.setBegriff(firstBegriffImKontextA);
        suchbegriffe.add(suchbegriffA);
        firstBegriffImKontextB = randomPrefix + "suggBegriffImKontextB";
        Suchbegriff suchbegriffB = new Suchbegriff();
        suchbegriffB.setBegriff(firstBegriffImKontextB);
        suchbegriffe.add(suchbegriffB);
        lb.setSuchbegriffe(suchbegriffe);

        firstUrsprungsportalTitel = randomPrefix + "suggUrsprungsportalTitel";
        lb.getUrsprungsportale().getFirst().setTitle(firstUrsprungsportalTitel);

        firstOdBezeichnung = randomPrefix + "suggODBezeichnung";
        firstOdBeschreibung = randomPrefix + "suggODBeschreibung";
        Onlinedienst od = file.getOnlinedienste().getFirst();
        od.setBeschreibung(firstOdBeschreibung);
        od.setBezeichnung(firstOdBezeichnung);

        ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        ImportUtil.pushAndWait(file);
    }

    @Test
    void matchAllSuggestions() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(ars, randomPrefix, true);
        SuggestionResultListValidator validator = new SuggestionResultListValidator(response);
        List<String> expectedSuggestions = Arrays.asList(
            firstBegriffImKontextA,
            firstBegriffImKontextB,
            firstbeschreibungTM02,
            firstTitelTM03,
            firstUrsprungsportalTitel
        );
        validator.validateListContains(expectedSuggestions);
        validator.validateCount(5);
    }

    @Test
    void matchAllSuggestionsIgnoreCase() {
        //search item - lower case
        Response response = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            ars,
            randomPrefix.toLowerCase(),
            true
        );
        SuggestionResultListValidator validator = new SuggestionResultListValidator(response);
        List<String> expectedSuggestions = Arrays.asList(
            firstBegriffImKontextA,
            firstBegriffImKontextB,
            firstbeschreibungTM02,
            firstTitelTM03,
            firstUrsprungsportalTitel
        );
        validator.validateListContains(expectedSuggestions);
        validator.validateCount(5);

        //search item - upper case
        response = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            ars,
            randomPrefix.toUpperCase(),
            true
        );
        validator = new SuggestionResultListValidator(response);
        validator.validateListContains(expectedSuggestions);
        validator.validateCount(5);
    }

    @Test
    void matchBegriffImKontext() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            ars,
            randomPrefix + "suggBegriffImKontext",
            true
        );
        SuggestionResultListValidator validator = new SuggestionResultListValidator(response);
        List<String> expectedSuggestions = Arrays.asList(firstBegriffImKontextA, firstBegriffImKontextB);
        validator.validateListContains(expectedSuggestions);
        validator.validateCount(2);
    }

    @Test
    void matchUrsprungsPortal() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            ars,
            randomPrefix + "suggU",
            true
        );
        SuggestionResultListValidator validator = new SuggestionResultListValidator(response);
        List<String> expectedSuggestions = Collections.singletonList(firstUrsprungsportalTitel);
        validator.validateListContains(expectedSuggestions);
        validator.validateCount(1);
    }

    @Test
    void noMatchODBeschreibung() {
        //Onlinedienst Beschreibung is used for matching resultlist but not as suggestion
        Response response = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            ars,
            firstOdBeschreibung,
            true
        );
        SuggestionResultListValidator validator = new SuggestionResultListValidator(response);
        validator.validateNotContains(firstOdBeschreibung);
        validator.validateCount(0);
    }

    @Test
    void noMatchODBezeichnung() {
        //Onlinedienst Bezeichnung is used for matching resultlist but not as suggestion
        Response response = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            ars,
            firstOdBezeichnung,
            true
        );
        SuggestionResultListValidator validator = new SuggestionResultListValidator(response);
        validator.validateNotContains(firstOdBezeichnung);
        validator.validateCount(0);
    }

    @Test
    void noMatchVolltextTM06() {
        //Textmodul 06 bzw. Volltext is used for matching resultlist but not as suggestion
        Response response = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            ars,
            firstVolltextTM06,
            true
        );
        SuggestionResultListValidator validator = new SuggestionResultListValidator(response);
        validator.validateNotContains(firstVolltextTM06);
        validator.validateCount(0);
    }

    @Test
    void noMatchAnderesBundesland() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            "033550022022",
            firstbeschreibungTM02,
            true
        );
        SuggestionResultListValidator validator = new SuggestionResultListValidator(response);
        validator.validateNotContains(firstbeschreibungTM02);
        validator.validateCount(0);
    }
}
