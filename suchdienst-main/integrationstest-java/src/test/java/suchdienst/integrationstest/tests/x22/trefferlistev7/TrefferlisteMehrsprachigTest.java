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
package suchdienst.integrationstest.tests.x22.trefferlistev7;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Suchbegriff;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulursprungsportal.UrsprungsportalModul;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import suchdienst.integrationstest.common.Constants;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.servicedescriptionv7.ServiceDescriptionResultListValidator;

/*
Testet, dass englischsprachige Suchbegriffe zu einem Treffer führen
Relevant für die Suche sind:
Beschreibung -> Textmodul 02
Titel -> Textmodul 06
Volltext -> Textmodul 06
BegriffImKontext
Ursprungsportal Titel
Onlinedienst Bezeichnung
Onlinedienst Beschreibung

Test 1 - matchItems -> es wird eine Liste von Testparametern übergeben
- bei allen einzelnen Suchparameter soll sowohl für deutschen und englische Begriffe bei Abfrage über Language-Code en und de ein Treffer gefunden werden
- in den en und de Suchbegriffen wurde ein X eingebaut - bei fuzzysearch=true sollen die Suchbegriffe gefunden werden
- Verwendung mehrerer de Suchbegriffe und mehrerer en Suchbegriffe führt zu einem Treffer - Mischen von de und en Werten funktioniert nicht!

Test 2 - noMatchItems
- in den en und de Suchbegriffen wurde ein X eingebaut - bei fuzzysearch=false soll kein Begriff gefunden werden
- es werden mehrere korrekte en bzw. de Parameter in der Abfrage übergeben und zusätzlich ein unbekannter Wert - es soll kein Treffer gefunden werden

Test 7 - matchMultipleLBs
- Test prüft, dass die Trefferliste bei Hinzufügen eines weiteren Suchbegriffs entsprechend eingeschränkt wird
*/

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class TrefferlisteMehrsprachigTest {

    private static final String RANDOM_PREFIX = TestUtil.generateRandomWord();

    private static String lbId;
    private static String ars;

    private static final String BESCHREIBUNG_TM02_DE = RANDOM_PREFIX + "BeschreibungTM02DE";
    private static final String TITEL_TM03_DE = RANDOM_PREFIX + "TitelTM03DE";
    private static final String VOLLTEXT_TM06_DE = RANDOM_PREFIX + "VolltextTM06DE";
    private static final String BEGRIFF_IM_KONTEXT_DE = RANDOM_PREFIX + "BegriffImKontextDE";
    private static final String OD_BEZEICHNUNG_DE = RANDOM_PREFIX + "ODBezeichnungDE";
    private static final String OD_BESCHREIBUNG_DE = RANDOM_PREFIX + "ODBeschreibungDE";
    private static final String URSPRUNGSPORTAL_TITEL_DE = RANDOM_PREFIX + "UrsprungsportalTitelDE";

    private static final String BESCHREIBUNG_TM02_EN = RANDOM_PREFIX + "DescriptionTM02EN";
    private static final String TITEL_TM03_EN = RANDOM_PREFIX + "TitleTM03EN";
    private static final String VOLLTEXT_TM06_EN = RANDOM_PREFIX + "FulltextTM06EN";
    private static final String BEGRIFF_IM_KONTEXT_EN = RANDOM_PREFIX + "SynonymEN";
    private static final String OD_BEZEICHNUNG_EN = RANDOM_PREFIX + "ODDefinitionEN";
    private static final String OD_BESCHREIBUNG_EN = RANDOM_PREFIX + "ODDescriptionEN";
    private static final String URSPRUNGSPORTAL_TITEL_EN = RANDOM_PREFIX + "SourcePortalTitleEN";

    private static final String BESCHREIBUNG_TM02_DE_FUZZY = RANDOM_PREFIX + "X" + "BeschreibungTM02DE";
    private static final String TITEL_TM03_DE_FUZZY = RANDOM_PREFIX + "X" + "TitelTM03DE";
    private static final String VOLLTEXT_TM06_DE_FUZZY = RANDOM_PREFIX + "X" + "VolltextTM06DE";
    private static final String BEGRIFF_IM_KONTEXT_DE_FUZZY = RANDOM_PREFIX + "X" + "BegriffImKontextDE";
    private static final String OD_BEZEICHNUNG_DE_FUZZY = RANDOM_PREFIX + "X" + "ODBezeichnungDE";
    private static final String OD_BESCHREIBUNG_DE_FUZZY = RANDOM_PREFIX + "X" + "ODBeschreibungDE";
    private static final String URSPRUNGSPORTAL_TITEL_DE_FUZZY = RANDOM_PREFIX + "X" + "UrsprungsportalTitelDE";

    private static final String BESCHREIBUNG_TM02_EN_FUZZY = RANDOM_PREFIX + "X" + "DescriptionTM02EN";
    private static final String TITEL_TM03_EN_FUZZY = RANDOM_PREFIX + "X" + "TitleTM03EN";
    private static final String VOLLTEXT_TM06_EN_FUZZY = RANDOM_PREFIX + "X" + "FulltextTM06EN";
    private static final String BEGRIFF_IM_KONTEXT_EN_FUZZY = RANDOM_PREFIX + "X" + "SynonymEN";
    private static final String OD_BEZEICHNUNG_EN_FUZZY = RANDOM_PREFIX + "X" + "ODDefinitionEN";
    private static final String OD_BESCHREIBUNG_EN_FUZZY = RANDOM_PREFIX + "X" + "ODDescriptionEN";
    private static final String URSPRUNGSPORTAL_TITEL_EN_FUZZY = RANDOM_PREFIX + "X" + "SourcePortalTitleEN";

    @BeforeAll
    static void setupTestclass(TestInfo testInfo) {
        String testclassname = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testclassname).lb(1).oe(1).od(1).getMin();
        //set relevant values for trefferliste
        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();

        List<Textmodul> textmodule = new ArrayList<>();
        Textmodul tm02 = Textmodul.builder()
            .inhalt(BESCHREIBUNG_TM02_DE)
            .inhaltEnglisch(BESCHREIBUNG_TM02_EN)
            .positionDarstellung(2)
            .leikaCode("02")
            .build();

        Textmodul tm03 = Textmodul.builder()
            .inhalt(TITEL_TM03_DE)
            .inhaltEnglisch(TITEL_TM03_EN)
            .positionDarstellung(3)
            .leikaCode("03")
            .build();

        Textmodul tm06 = Textmodul.builder()
            .inhalt(VOLLTEXT_TM06_DE)
            .inhaltEnglisch(VOLLTEXT_TM06_EN)
            .positionDarstellung(6)
            .leikaCode("06")
            .build();

        textmodule.add(tm02);
        textmodule.add(tm03);
        textmodule.add(tm06);
        lb.setTextmodule(textmodule);

        List<Suchbegriff> suchbegriffe = new ArrayList<>();
        suchbegriffe.add(
            Suchbegriff.builder().begriff(BEGRIFF_IM_KONTEXT_DE).begriffEnglisch(BEGRIFF_IM_KONTEXT_EN).build()
        );
        lb.setSuchbegriffe(suchbegriffe);

        UrsprungsportalModul ursprungsportal = lb.getUrsprungsportale().getFirst();

        ursprungsportal.setTitle(URSPRUNGSPORTAL_TITEL_DE);
        ursprungsportal.setTitleEnglisch(URSPRUNGSPORTAL_TITEL_EN);
        ursprungsportal.setUriEnglisch("www.ursprungsportalEnglish.en");

        Onlinedienst od = file.getOnlinedienste().getFirst();
        od.setBeschreibung(OD_BESCHREIBUNG_DE);
        od.setBeschreibungEnglisch(OD_BESCHREIBUNG_EN);
        od.setBezeichnung(OD_BEZEICHNUNG_DE);
        od.setBezeichnungEnglisch(OD_BEZEICHNUNG_EN);

        ImportUtil.pushAndWait(file);

        ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        lbId = file.getLeistungsbeschreibungen().getFirst().getFullId();
    }

    @DisplayName("matchWithItems")
    @ParameterizedTest(name = "{displayName} SearchParam:{0} fuzzySearch:{1}")
    @MethodSource("provideItems")
    void matchItems(List<String> queryParams, Boolean fuzzySearch) {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7(
            ars,
            queryParams,
            0,
            20,
            fuzzySearch,
            Collections.emptyList(),
            null,
            null,
            true,
            Constants.LANGUAGE_EN
        );
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(lbId);
        response = ServiceDescriptionConnector.getServiceDescriptionsV7(
            ars,
            queryParams,
            0,
            20,
            fuzzySearch,
            Collections.emptyList(),
            null,
            null,
            true,
            Constants.LANGUAGE_DE
        );
        validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(lbId);
    }

    @DisplayName("noMatchWithItems")
    @ParameterizedTest(name = "{displayName} SearchParam:{0} fuzzySearch:{1}")
    @MethodSource("provideNoMatchItems")
    void noMatchItems(List<String> queryParams, Boolean fuzzySearch) {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7(
            ars,
            queryParams,
            0,
            20,
            fuzzySearch,
            Collections.emptyList(),
            null,
            null,
            true,
            Constants.LANGUAGE_EN
        );
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateNotContains(lbId);
        response = ServiceDescriptionConnector.getServiceDescriptionsV7(
            ars,
            queryParams,
            0,
            20,
            fuzzySearch,
            Collections.emptyList(),
            null,
            null,
            true,
            Constants.LANGUAGE_DE
        );
        validator = new ServiceDescriptionResultListValidator(response);
        validator.validateNotContains(lbId);
    }

    @Test
    void matchMultipleLBs(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String firstBegriffImKontext = TestUtil.generateRandomWord() + "firstItem";
        String secondBegriffImKontext = TestUtil.generateRandomWord() + "secondItem";
        String thirdBegriffImKontext = TestUtil.generateRandomWord() + "thirdItem";

        //***** Setup *****.
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(3).oe(1).od(1).getMax();

        Leistungsbeschreibung lb1 = file.getLeistungsbeschreibungen().getFirst();
        lb1.getSuchbegriffe().clear();
        lb1.setSuchbegriffe(
            List.of(Suchbegriff.builder().begriff("Dummy01").begriffEnglisch(firstBegriffImKontext).build())
        );
        Leistungsbeschreibung lb2 = file.getLeistungsbeschreibungen().get(1);
        lb2.getSuchbegriffe().clear();
        lb2.setSuchbegriffe(
            List.of(
                Suchbegriff.builder().begriff("Dummy01").begriffEnglisch(firstBegriffImKontext).build(),
                Suchbegriff.builder().begriff("Dummy02").begriffEnglisch(secondBegriffImKontext).build()
            )
        );
        Leistungsbeschreibung lb3 = file.getLeistungsbeschreibungen().get(2);
        lb3.getSuchbegriffe().clear();
        lb3.setSuchbegriffe(
            List.of(
                Suchbegriff.builder().begriff("Dummy01").begriffEnglisch(firstBegriffImKontext).build(),
                Suchbegriff.builder().begriff("Dummy02").begriffEnglisch(secondBegriffImKontext).build(),
                Suchbegriff.builder().begriff("Dummy03").begriffEnglisch(thirdBegriffImKontext).build()
            )
        );

        ImportUtil.pushAndWait(file);

        ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();

        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7(
            ars,
            firstBegriffImKontext,
            Constants.LANGUAGE_EN
        );
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(List.of(lb1.getFullId(), lb2.getFullId(), lb3.getFullId()));

        response = ServiceDescriptionConnector.getServiceDescriptionsV7(
            ars,
            List.of(firstBegriffImKontext, secondBegriffImKontext),
            0,
            20,
            false,
            Collections.emptyList(),
            null,
            null,
            true,
            Constants.LANGUAGE_EN
        );

        validator = new ServiceDescriptionResultListValidator(response);
        validator
            .validateContains(List.of(lb2.getFullId(), lb3.getFullId()))
            .validateContainsNoneOf(List.of(lb1.getFullId()));

        response = ServiceDescriptionConnector.getServiceDescriptionsV7(
            ars,
            List.of(firstBegriffImKontext, secondBegriffImKontext, thirdBegriffImKontext),
            0,
            20,
            false,
            Collections.emptyList(),
            null,
            null,
            true,
            Constants.LANGUAGE_EN
        );
        validator = new ServiceDescriptionResultListValidator(response);
        validator
            .validateContains(List.of(lb3.getFullId()))
            .validateContainsNoneOf(List.of(lb1.getFullId(), lb2.getFullId()));
    }

    private static Stream<Arguments> provideItems() {
        return Stream.of(
            //EN params without fuzzy
            Arguments.of(List.of(BESCHREIBUNG_TM02_EN), false),
            Arguments.of(List.of(TITEL_TM03_EN), false),
            Arguments.of(List.of(VOLLTEXT_TM06_EN), false),
            Arguments.of(List.of(BEGRIFF_IM_KONTEXT_EN), false),
            Arguments.of(List.of(URSPRUNGSPORTAL_TITEL_EN), false),
            Arguments.of(List.of(OD_BESCHREIBUNG_EN), false),
            Arguments.of(List.of(OD_BEZEICHNUNG_EN), false),
            //EN params with fuzzy
            Arguments.of(List.of(BESCHREIBUNG_TM02_EN_FUZZY), true),
            Arguments.of(List.of(TITEL_TM03_EN_FUZZY), true),
            Arguments.of(List.of(VOLLTEXT_TM06_EN_FUZZY), true),
            Arguments.of(List.of(BEGRIFF_IM_KONTEXT_EN_FUZZY), true),
            Arguments.of(List.of(URSPRUNGSPORTAL_TITEL_EN_FUZZY), true),
            Arguments.of(List.of(OD_BESCHREIBUNG_EN_FUZZY), true),
            Arguments.of(List.of(OD_BEZEICHNUNG_EN_FUZZY), true),
            //DE params without fuzzy
            Arguments.of(List.of(BESCHREIBUNG_TM02_DE), false),
            Arguments.of(List.of(TITEL_TM03_DE), false),
            Arguments.of(List.of(VOLLTEXT_TM06_DE), false),
            Arguments.of(List.of(BEGRIFF_IM_KONTEXT_DE), false),
            Arguments.of(List.of(URSPRUNGSPORTAL_TITEL_DE), false),
            Arguments.of(List.of(OD_BESCHREIBUNG_DE), false),
            Arguments.of(List.of(OD_BEZEICHNUNG_DE), false),
            //DE params with fuzzy
            Arguments.of(List.of(BESCHREIBUNG_TM02_DE_FUZZY), true),
            Arguments.of(List.of(TITEL_TM03_DE_FUZZY), true),
            Arguments.of(List.of(VOLLTEXT_TM06_DE_FUZZY), true),
            Arguments.of(List.of(BEGRIFF_IM_KONTEXT_DE_FUZZY), true),
            Arguments.of(List.of(URSPRUNGSPORTAL_TITEL_DE_FUZZY), true),
            Arguments.of(List.of(OD_BESCHREIBUNG_DE_FUZZY), true),
            Arguments.of(List.of(OD_BEZEICHNUNG_DE_FUZZY), true),
            //Multiple EN params
            Arguments.of(
                List.of(BESCHREIBUNG_TM02_EN, TITEL_TM03_EN, URSPRUNGSPORTAL_TITEL_EN, BEGRIFF_IM_KONTEXT_EN),
                false
            ),
            //Multiple DE params
            Arguments.of(
                List.of(BESCHREIBUNG_TM02_DE, TITEL_TM03_DE, URSPRUNGSPORTAL_TITEL_DE, BEGRIFF_IM_KONTEXT_DE),
                false
            )
        );
    }

    private static Stream<Arguments> provideNoMatchItems() {
        return Stream.of(
            //EN fuzzy params with fuzzySearch false
            Arguments.of(List.of(BESCHREIBUNG_TM02_EN_FUZZY), false),
            Arguments.of(List.of(TITEL_TM03_EN_FUZZY), false),
            Arguments.of(List.of(VOLLTEXT_TM06_EN_FUZZY), false),
            Arguments.of(List.of(BEGRIFF_IM_KONTEXT_EN_FUZZY), false),
            Arguments.of(List.of(URSPRUNGSPORTAL_TITEL_EN_FUZZY), false),
            Arguments.of(List.of(OD_BESCHREIBUNG_EN_FUZZY), false),
            Arguments.of(List.of(OD_BEZEICHNUNG_EN_FUZZY), false),
            //DE fuzzy params with fuzzySearch false
            Arguments.of(List.of(BESCHREIBUNG_TM02_DE_FUZZY), false),
            Arguments.of(List.of(TITEL_TM03_DE_FUZZY), false),
            Arguments.of(List.of(VOLLTEXT_TM06_DE_FUZZY), false),
            Arguments.of(List.of(BEGRIFF_IM_KONTEXT_DE_FUZZY), false),
            Arguments.of(List.of(URSPRUNGSPORTAL_TITEL_DE_FUZZY), false),
            Arguments.of(List.of(OD_BESCHREIBUNG_DE_FUZZY), false),
            Arguments.of(List.of(OD_BEZEICHNUNG_DE_FUZZY), false),
            //Multiple EN params
            Arguments.of(
                List.of(
                    BESCHREIBUNG_TM02_EN,
                    TITEL_TM03_EN,
                    URSPRUNGSPORTAL_TITEL_EN,
                    BEGRIFF_IM_KONTEXT_EN,
                    "noMatchItem"
                ),
                false
            ),
            //Multiple DE params
            Arguments.of(
                List.of(
                    BESCHREIBUNG_TM02_DE,
                    TITEL_TM03_DE,
                    URSPRUNGSPORTAL_TITEL_DE,
                    BEGRIFF_IM_KONTEXT_DE,
                    "keinTrefferBegriff"
                ),
                false
            )
        );
    }
}
