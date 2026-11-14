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
import de.dataport.pvog.testdatengenerator.model.Suchbegriff;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulursprungsportal.UrsprungsportalModul;
import io.restassured.response.Response;
import java.util.ArrayList;
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

/*
In diesem Test soll geprüft werden, dass Suchvorschläge, die sich lediglich durch Groß-/Kleinschreibung unterscheiden bei
-> skipDuplicates=true nur zu einem Suchvorschlag führen
-> skipDuplicates=false zu mehreren Vorschlägen führen

Bei skipDuplicates=true werden von elasticsearch mehrere Ergebnisse geliefert. Das SuD Backend prüft die Ergebnisse
auf Gleichheit, indem auf lowerCase gewandelt wird. Welcher der Begriffe angezeigt wird ist nicht deterministisch

Wurden in den relevanten Ferldern, die für die Suche herahngezogen werden Begriffe verwendet, die mit Großbuchstaben
beginnen, dann werden zusätzlich noch diese Begriffe in lowerCase abgespeichert.

Im Test matchAllSkipTrue soll geprüft werden, dass bei Eingabe des gemeinsamen Prefix von Textmodul02, 03, BegriffImKontext
und UrsprungsportalTitel auch alle 4 Inhalte - entweder mit lowercase oder uppercase Prefix angezeigt werden.

*/
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class LeistungSkipDuplicatesTest {

    private static final String UPPER_CASE_PREFIX = TestUtil.generateRandomWord();
    private static final String LOWER_CASE_PREFIX = UPPER_CASE_PREFIX.toLowerCase();
    private static String ars;

    //fields used for suggestions
    private static final String UPPER_BESCHREIBUNG_TM02 = UPPER_CASE_PREFIX + "suggskipbeschreibung";
    private static final String UPPER_TITEL_TM03 = UPPER_CASE_PREFIX + "suggskiptitel";
    private static final String UPPER_URSPRUNGS_PORTAL_TITEL = UPPER_CASE_PREFIX + "suggskipursprungsportaltitel";
    private static final String UPPER_BEGRIFF_IM_KONTEXT = UPPER_CASE_PREFIX + "suggskipbegriffimkontext";

    private static final String LOWER_BESCHREIBUNG_TM02 = LOWER_CASE_PREFIX + "suggskipbeschreibung";
    private static final String LOWER_TITEL_TM03 = LOWER_CASE_PREFIX + "suggskiptitel";
    private static final String LOWER_URSPRUNGS_PORTAL_TITEL = LOWER_CASE_PREFIX + "suggskipursprungsportaltitel";
    private static final String LOWER_BEGRIFF_IM_KONTEXT = LOWER_CASE_PREFIX + "suggskipbegriffimkontext";

    @BeforeAll
    static void setupTestclass(TestInfo testInfo) {
        String testclassname = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        BundeslandConfig bundeslandConfig = new BundeslandConfig(Bundesland.BE);

        // Daten erzeuegn
        XzufiFile file = XzufiFileCreator.newInstance(bundeslandConfig).name(testclassname).lb(1).oe(1).getMin();

        //erzeuge relevante Werte für Trefferliste
        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();

        List<Textmodul> textmodule = new ArrayList<>();
        Textmodul tm02 = Textmodul.builder()
            .inhalt(UPPER_BESCHREIBUNG_TM02)
            .positionDarstellung(2)
            .leikaCode("02")
            .build();
        Textmodul tm03 = Textmodul.builder().inhalt(UPPER_TITEL_TM03).positionDarstellung(3).leikaCode("03").build();
        Textmodul tm06 = Textmodul.builder().inhalt("DummyTM06").positionDarstellung(6).leikaCode("06").build();
        textmodule.add(tm02);
        textmodule.add(tm03);
        textmodule.add(tm06);
        lb.setTextmodule(textmodule);

        List<Suchbegriff> suchbegriffe = new ArrayList<>();
        Suchbegriff suchbegriffA = new Suchbegriff();
        suchbegriffA.setBegriff(UPPER_BEGRIFF_IM_KONTEXT);
        suchbegriffe.add(suchbegriffA);
        lb.setSuchbegriffe(suchbegriffe);

        UrsprungsportalModul ursprungsportal = lb.getUrsprungsportale().getFirst();
        ursprungsportal.setTitle(UPPER_URSPRUNGS_PORTAL_TITEL);

        ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        ImportUtil.push(file);
        //erzeuge zweite Leistung mit lower case prefix
        XzufiFile lowerFile = XzufiFileCreator.newInstance(bundeslandConfig)
            .name(testclassname + "LOWER")
            .lb(1)
            .oe(1)
            .getMin();

        //erzeuge relevante Werte für Trefferliste
        Leistungsbeschreibung lowerlb = lowerFile.getLeistungsbeschreibungen().getFirst();

        List<Textmodul> lowertextmodule = new ArrayList<>();
        Textmodul lowertm02 = Textmodul.builder()
            .inhalt(LOWER_BESCHREIBUNG_TM02)
            .positionDarstellung(2)
            .leikaCode("02")
            .build();
        Textmodul lowertm03 = Textmodul.builder()
            .inhalt(LOWER_TITEL_TM03)
            .positionDarstellung(3)
            .leikaCode("03")
            .build();
        Textmodul lowertm06 = Textmodul.builder()
            .inhalt("DummySecond06")
            .positionDarstellung(6)
            .leikaCode("06")
            .build();
        lowertextmodule.add(lowertm02);
        lowertextmodule.add(lowertm03);
        lowertextmodule.add(lowertm06);
        lowerlb.setTextmodule(lowertextmodule);

        List<Suchbegriff> lowersuchbegriffe = new ArrayList<>();
        Suchbegriff lowersuchbegriffA = new Suchbegriff();
        lowersuchbegriffA.setBegriff(LOWER_BEGRIFF_IM_KONTEXT);
        lowersuchbegriffe.add(lowersuchbegriffA);
        lowerlb.setSuchbegriffe(lowersuchbegriffe);

        UrsprungsportalModul lowerUrsprungsportal = lowerlb.getUrsprungsportale().getFirst();
        lowerUrsprungsportal.setTitle(LOWER_URSPRUNGS_PORTAL_TITEL);

        ImportUtil.pushAndWait(lowerFile);
    }

    @Test
    void matchBeschreibungSkipTrue() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            ars,
            UPPER_BESCHREIBUNG_TM02,
            true
        );
        SuggestionResultListValidator validator = new SuggestionResultListValidator(response);
        validator.validateContainsAnyOf(List.of(UPPER_BESCHREIBUNG_TM02, LOWER_BESCHREIBUNG_TM02)).validateCount(1);
    }

    @Test
    void matchBeschreibungSkipFalse() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            ars,
            UPPER_BESCHREIBUNG_TM02,
            false
        );
        SuggestionResultListValidator validator = new SuggestionResultListValidator(response);
        List<String> expectedSuggestionsList = List.of(UPPER_BESCHREIBUNG_TM02, LOWER_BESCHREIBUNG_TM02);
        validator.validateListContains(expectedSuggestionsList).validateCount(2);
    }

    @Test
    void matchTitelSkipTrue() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            ars,
            UPPER_TITEL_TM03,
            true
        );
        SuggestionResultListValidator validator = new SuggestionResultListValidator(response);
        validator.validateContainsAnyOf(List.of(UPPER_TITEL_TM03, LOWER_TITEL_TM03)).validateCount(1);
    }

    @Test
    void matchTitelSkipFalse() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            ars,
            UPPER_TITEL_TM03,
            false
        );
        SuggestionResultListValidator validator = new SuggestionResultListValidator(response);
        List<String> expectedSuggestionsList = List.of(UPPER_TITEL_TM03, LOWER_TITEL_TM03);
        validator.validateListContains(expectedSuggestionsList);
        validator.validateCount(2);
    }

    @Test
    void matchBegriffImKontextSkipTrue() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            ars,
            UPPER_BEGRIFF_IM_KONTEXT,
            true
        );
        SuggestionResultListValidator validator = new SuggestionResultListValidator(response);
        validator.validateContainsAnyOf(List.of(UPPER_BEGRIFF_IM_KONTEXT, LOWER_BEGRIFF_IM_KONTEXT)).validateCount(1);
    }

    @Test
    void matchBegriffImKontextSkipFalse() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            ars,
            UPPER_BEGRIFF_IM_KONTEXT,
            false
        );
        SuggestionResultListValidator validator = new SuggestionResultListValidator(response);
        List<String> expectedSuggestionsList = List.of(UPPER_BEGRIFF_IM_KONTEXT, LOWER_BEGRIFF_IM_KONTEXT);
        validator.validateListContains(expectedSuggestionsList);
        validator.validateCount(2);
    }

    @Test
    void matchUrsprungsportalTitelSkipTrue() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            ars,
            UPPER_URSPRUNGS_PORTAL_TITEL,
            true
        );
        SuggestionResultListValidator validator = new SuggestionResultListValidator(response);
        validator
            .validateContainsAnyOf(List.of(UPPER_URSPRUNGS_PORTAL_TITEL, LOWER_URSPRUNGS_PORTAL_TITEL))
            .validateCount(1);
    }

    @Test
    void matchUrsprungsportalTitelSkipFalse() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            ars,
            UPPER_URSPRUNGS_PORTAL_TITEL,
            false
        );
        SuggestionResultListValidator validator = new SuggestionResultListValidator(response);
        List<String> expectedSuggestionsList = List.of(UPPER_URSPRUNGS_PORTAL_TITEL, LOWER_URSPRUNGS_PORTAL_TITEL);
        validator.validateListContains(expectedSuggestionsList);
        validator.validateCount(2);
    }

    @Test
    void matchAllSkipTrue() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            ars,
            LOWER_CASE_PREFIX,
            true
        );
        SuggestionResultListValidator validator = new SuggestionResultListValidator(response);
        validator.validateContainsAnyOf(List.of(UPPER_URSPRUNGS_PORTAL_TITEL, LOWER_URSPRUNGS_PORTAL_TITEL));
        validator.validateContainsAnyOf(List.of(UPPER_BESCHREIBUNG_TM02, LOWER_BESCHREIBUNG_TM02));
        validator.validateContainsAnyOf(List.of(UPPER_BEGRIFF_IM_KONTEXT, LOWER_BEGRIFF_IM_KONTEXT));
        validator.validateContainsAnyOf(List.of(UPPER_TITEL_TM03, LOWER_TITEL_TM03));
        validator.validateCount(4);
    }
}
