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
package suchdienst.integrationstest.tests.lebenslagen;

import static suchdienst.integrationstest.connectors.ServiceDescriptionConnector.getPersonalMattersV1;

import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig;
import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Suchbegriff;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
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
import suchdienst.integrationstest.validator.personalmattersv1.PersonalMattersResultListValidator;
import suchdienst.integrationstest.validator.servicedescriptionv8.ServiceDescriptionResultListValidator;

/**
 * Testziel ist die Prüfung, dass bei Filterung über ars und personalMatters
 * der korrekte Lebenslagenbaum und Ergebnismenge angezeigt wird
 * WICHTIG - Wird der Lebnslagenbaum angefragt (facades=true) und personalMatters als Filterkriterium angegeben,
 * so schränkt das den Lebenslagenbaum nicht ein
 * Initial wird ein Leistungskatalog mit 4 Leistungen geladen
 * LeikaKey 99038009223001 - Lebenslage 2030700
 * LeikaKey 99001001000000 - Lebenslage 1050200, 1170100
 * LeikaKey 99038009223002 - Lebenslage 1180200
 * LeikaKey 99157002014000 - Lebenslage 2030400, 2160200, 1040100, 1040400
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class LebenslagenFilterTest {

    private static final String RANDOM_WORD = TestUtil.generateRandomWord();
    private static final String PRAEFIX = "L100042.LB.LB_";
    private static final String POSTFIX = "_0001";

    private static final String FIRST_LB = RANDOM_WORD + "FirstLB";
    private static final String FIRST_LB_FULL_ID = PRAEFIX + FIRST_LB + POSTFIX;
    private static final String SECOND_LB = RANDOM_WORD + "SecondLB";
    private static final String SECOND_LB_FULL_ID = PRAEFIX + SECOND_LB + POSTFIX;
    private static final String THIRD_LB = RANDOM_WORD + "ThirdLB";
    private static final String THIRD_LB_FULL_ID = PRAEFIX + THIRD_LB + POSTFIX;
    private static final String FOURTH_LB = RANDOM_WORD + "FourthLB";
    private static final String FOURTH_LB_FULL_ID = PRAEFIX + FOURTH_LB + POSTFIX;

    //erwarteter kompletter Lebenslagenbaum, der sich aus den verwendeten Leikakeys und den Einträgen des Leistungskatalogs zusammensetzt
    private static final Map<String, Integer> FIRST_LEVEL_LAGE_EXPECTED = Map.of("1000000", 3, "2000000", 1);
    private static final Map<String, Integer> SECOND_LEVEL_LAGE_EXPECTED = Map.of(
        "1050000",
        1,
        "1180000",
        1,
        "1170000",
        2,
        "2030000",
        1
    );
    private static final Map<String, Integer> THIRD_LEVEL_LAGE_EXPECTED = Map.of(
        "1050200",
        1,
        "1180200",
        1,
        "1170200",
        1,
        "1170100",
        1,
        "2030700",
        1
    );

    private static final BundeslandConfig BUNDESLAND_CONFIG = new BundeslandConfig(
        Bundesland.BY,
        "L100042",
        "090000000000",
        List.of("091620000000", "091905174129", "091905174133", "091905174149")
    );

    @BeforeAll
    static void importLeistungskatalogSetupData() {
        KatalogImportUtil.resetLeistungskatalog("leistungskatalogOK.xml");

        String synonym = RANDOM_WORD + "synonym";
        // Lage 1180200
        String firstLeika = "99038009223002";
        // Lage 1050200 und 1170100
        String secondLeika = "99001001000000";
        // Lage 2030700
        String thirdLeika = "99038009223001";

        // nicht im Katalog enthalten
        String fourthLeika = "99001001001234";
        String fourthLage = "1170200";

        // verwendet ars "091620000000"
        XzufiFile firstFile = XzufiFileCreator.newInstance(BUNDESLAND_CONFIG).name(FIRST_LB).lb(1).oe(1).getMin();
        Leistungsbeschreibung ersteLB = firstFile.getLeistungsbeschreibungen().getFirst();
        ersteLB.setReferenzLeiKa(firstLeika);
        ersteLB.getSuchbegriffe().add(new Suchbegriff(synonym, synonym + "EN"));
        // verwendet ars "091620000000", ""091905174129""
        XzufiFile secondFile = XzufiFileCreator.newInstance(BUNDESLAND_CONFIG)
            .name(SECOND_LB)
            .lb(1)
            .oe(1)
            .zsOe(2)
            .getMin();
        Leistungsbeschreibung zweiteLB = secondFile.getLeistungsbeschreibungen().getFirst();
        zweiteLB.setReferenzLeiKa(secondLeika);
        zweiteLB.getSuchbegriffe().add(new Suchbegriff(synonym, synonym + "EN"));
        // verwendet ars"091620000000", "091905174129", "091905174133"
        XzufiFile thirdFile = XzufiFileCreator.newInstance(BUNDESLAND_CONFIG)
            .name(THIRD_LB)
            .lb(1)
            .oe(1)
            .zsOe(3)
            .getMin();
        Leistungsbeschreibung dritteLB = thirdFile.getLeistungsbeschreibungen().getFirst();
        dritteLB.setReferenzLeiKa(thirdLeika);
        dritteLB.getSuchbegriffe().add(new Suchbegriff(synonym, synonym + "EN"));
        // verwendet ars "091620000000", "091905174129", "091905174133", "091905174149"
        XzufiFile fourthFile = XzufiFileCreator.newInstance(BUNDESLAND_CONFIG)
            .name(FOURTH_LB)
            .lb(1)
            .oe(1)
            .zsOe(4)
            .getMin();
        Leistungsbeschreibung vierteLB = fourthFile.getLeistungsbeschreibungen().getFirst();
        vierteLB.setReferenzLeiKa(fourthLeika);
        vierteLB.getSuchbegriffe().add(new Suchbegriff(synonym, synonym + "EN"));
        vierteLB.setKlassifizierung(List.of(fourthLage));

        ImportUtil.push(firstFile);
        ImportUtil.push(secondFile);
        ImportUtil.push(thirdFile);
        ImportUtil.pushAndWait23(fourthFile);
    }

    @DisplayName("LebenslagenFilter")
    @ParameterizedTest(name = "{displayName} codes:{0} expectedIds:{1} unexpectedIds:{2}")
    @MethodSource("provideFilter")
    @SuppressWarnings("java:S2699") // Die Assertions werden vom Validator durchgeführt.
    void filterByLebenslage(List<String> codes, List<String> expected, List<String> unexpected) {
        String ars = BUNDESLAND_CONFIG.getArss().getFirst();
        String queryString = RANDOM_WORD + "synonym";

        Response serviceDescResponse = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),
            codes,
            true
        );

        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(
            serviceDescResponse
        );

        for (String id : expected) {
            validator.serviceDescriptionsContainsId(id);
        }

        for (String id : unexpected) {
            validator.serviceDescriptionsNotContainsId(id);
        }
        //Lebenslagenbaum bleibt unabhängig von personalMatters Abfrageparameter gleich
        FIRST_LEVEL_LAGE_EXPECTED.forEach((key, value) ->
            validator.personalMattersFirstLevelContainsCode(key).personalMattersFirstLevelCountByCode(value, key)
        );

        SECOND_LEVEL_LAGE_EXPECTED.forEach((key, value) ->
            validator.personalMattersSecondLevelContainsCode(key).personalMattersSecondLevelCountByCode(value, key)
        );

        THIRD_LEVEL_LAGE_EXPECTED.forEach((key, value) ->
            validator.personalMattersThirdLevelContainsCode(key).personalMattersThirdLevelCountByCode(value, key)
        );
    }

    @DisplayName("ArsFilter")
    @ParameterizedTest(name = "{displayName} ars:{0} expectedIds:{1} unexpectedIds:{2}")
    @MethodSource("provideArsFilter")
    @SuppressWarnings("java:S2699") // Die Assertions werden vom Validator durchgeführt.
    void filterByArs(
        String ars,
        List<String> expected,
        List<String> unexpected,
        Map<String, Integer> firstLevelLage,
        Map<String, Integer> secondLevelLage,
        Map<String, Integer> thirdLevelLage
    ) {
        String queryString = RANDOM_WORD + "synonym";

        Response serviceDescResponse = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),
            null,
            true
        );

        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(
            serviceDescResponse
        );

        for (String id : expected) {
            validator.serviceDescriptionsContainsId(id);
        }

        for (String id : unexpected) {
            validator.serviceDescriptionsNotContainsId(id);
        }
        firstLevelLage.forEach((key, value) ->
            validator.personalMattersFirstLevelContainsCode(key).personalMattersFirstLevelCountByCode(value, key)
        );
        secondLevelLage.forEach((key, value) ->
            validator.personalMattersSecondLevelContainsCode(key).personalMattersSecondLevelCountByCode(value, key)
        );

        thirdLevelLage.forEach((key, value) ->
            validator.personalMattersThirdLevelContainsCode(key).personalMattersThirdLevelCountByCode(value, key)
        );
        validator.personalMattersFirstLevelSize(firstLevelLage.size());
    }

    @DisplayName("ArsFilterPM")
    @ParameterizedTest(name = "{displayName} ars:{0}")
    @MethodSource("provideArsFilterPM")
    void filterPersonalMattersByArs(
        String ars,
        List<String> firstLevel,
        Map<String, List<String>> secondLevel,
        Map<String, Map<String, List<String>>> thirdLevel
    ) {
        final var pmResponse1 = getPersonalMattersV1(ars, Constants.LANGUAGE_DE);
        final var personalMattersValidator = new PersonalMattersResultListValidator(pmResponse1);
        personalMattersValidator.firstLevelExactly(firstLevel.toArray(String[]::new));

        for (var entry : secondLevel.entrySet()) {
            personalMattersValidator.secondLevelExactly(entry.getKey(), entry.getValue().toArray(String[]::new));
        }
        for (var entry : thirdLevel.entrySet()) {
            for (var subentry : entry.getValue().entrySet()) {
                personalMattersValidator.thirdLevelExactly(
                    entry.getKey(),
                    subentry.getKey(),
                    subentry.getValue().toArray(String[]::new)
                );
            }
        }
    }

    private static Stream<Arguments> provideFilter() {
        // 1. Parameter Liste der Codes.
        // 2. Parameter IDs, die gefunden werden sollen.
        // 3. Parameter Ids, die nicht in der Ergebnismenge auftauchen sollen.
        return Stream.of(
            Arguments.of(
                List.of("2000000"),
                List.of(),
                List.of(FIRST_LB_FULL_ID, SECOND_LB_FULL_ID, THIRD_LB_FULL_ID, FOURTH_LB_FULL_ID)
            ),
            Arguments.of(
                List.of("1000000"),
                List.of(),
                List.of(FIRST_LB_FULL_ID, SECOND_LB_FULL_ID, THIRD_LB_FULL_ID, FOURTH_LB_FULL_ID)
            ),
            Arguments.of(
                List.of("1000000", "2000000"),
                List.of(),
                List.of(FIRST_LB_FULL_ID, SECOND_LB_FULL_ID, THIRD_LB_FULL_ID, FOURTH_LB_FULL_ID)
            ),
            Arguments.of(
                List.of("1180000"),
                List.of(),
                List.of(FIRST_LB_FULL_ID, SECOND_LB_FULL_ID, THIRD_LB_FULL_ID, FOURTH_LB_FULL_ID)
            ),
            Arguments.of(
                List.of("1170000"),
                List.of(),
                List.of(FIRST_LB_FULL_ID, SECOND_LB_FULL_ID, THIRD_LB_FULL_ID, FOURTH_LB_FULL_ID)
            ),
            Arguments.of(
                List.of("1100000"),
                List.of(),
                List.of(FIRST_LB_FULL_ID, SECOND_LB_FULL_ID, THIRD_LB_FULL_ID, FOURTH_LB_FULL_ID)
            ),
            Arguments.of(
                List.of("10000"),
                List.of(),
                List.of(FIRST_LB_FULL_ID, SECOND_LB_FULL_ID, THIRD_LB_FULL_ID, FOURTH_LB_FULL_ID)
            ),
            Arguments.of(
                List.of("1180200"),
                List.of(FIRST_LB_FULL_ID),
                List.of(SECOND_LB_FULL_ID, THIRD_LB_FULL_ID, FOURTH_LB_FULL_ID)
            ),
            Arguments.of(
                List.of("1050200"),
                List.of(SECOND_LB_FULL_ID),
                List.of(FIRST_LB_FULL_ID, THIRD_LB_FULL_ID, FOURTH_LB_FULL_ID)
            ),
            Arguments.of(
                List.of("1170100"),
                List.of(SECOND_LB_FULL_ID),
                List.of(FIRST_LB_FULL_ID, THIRD_LB_FULL_ID, FOURTH_LB_FULL_ID)
            ),
            Arguments.of(
                List.of("2030700"),
                List.of(THIRD_LB_FULL_ID),
                List.of(FIRST_LB_FULL_ID, SECOND_LB_FULL_ID, FOURTH_LB_FULL_ID)
            ),
            Arguments.of(
                List.of("1170200"),
                List.of(FOURTH_LB_FULL_ID),
                List.of(FIRST_LB_FULL_ID, SECOND_LB_FULL_ID, THIRD_LB_FULL_ID)
            ),
            Arguments.of(
                List.of("1170200", "2030700"),
                List.of(THIRD_LB_FULL_ID, FOURTH_LB_FULL_ID),
                List.of(FIRST_LB_FULL_ID, SECOND_LB_FULL_ID)
            ),
            Arguments.of(
                List.of("0815", "9877789", "hans", "test", "im", "0", "1170200", "2030700"),
                List.of(THIRD_LB_FULL_ID, FOURTH_LB_FULL_ID),
                List.of(FIRST_LB_FULL_ID, SECOND_LB_FULL_ID)
            )
        );
    }

    private static Stream<Arguments> provideArsFilter() {
        // 1. Parameter Ars
        // 2. Parameter IDs, die gefunden werden sollen
        // 3. Parameter Ids, die nicht in der Ergebnismenge auftauchen sollen
        // 4. Parameter erwartete FirstLevelLage
        // 5. Parameter erwartete SecondLevelLage
        // 6. Parameter ThirdLevelLage
        return Stream.of(
            Arguments.of(
                "091620000000",
                List.of(FIRST_LB_FULL_ID, SECOND_LB_FULL_ID, THIRD_LB_FULL_ID, FOURTH_LB_FULL_ID),
                List.of(),
                Map.of("1000000", 3, "2000000", 1),
                Map.of("1050000", 1, "1180000", 1, "1170000", 2, "2030000", 1),
                Map.of("1050200", 1, "1180200", 1, "1170200", 1, "1170100", 1, "2030700", 1)
            ),
            Arguments.of(
                "091905174129",
                List.of(SECOND_LB_FULL_ID, THIRD_LB_FULL_ID, FOURTH_LB_FULL_ID),
                List.of(FIRST_LB_FULL_ID),
                Map.of("1000000", 2, "2000000", 1),
                Map.of("1050000", 1, "1170000", 2, "2030000", 1),
                Map.of("1050200", 1, "1170200", 1, "1170100", 1, "2030700", 1)
            ),
            Arguments.of(
                "091905174133",
                List.of(THIRD_LB_FULL_ID, FOURTH_LB_FULL_ID),
                List.of(FIRST_LB_FULL_ID, SECOND_LB_FULL_ID),
                Map.of("1000000", 1, "2000000", 1),
                Map.of("1170000", 1, "2030000", 1),
                Map.of("1170200", 1, "2030700", 1)
            ),
            Arguments.of(
                "091905174149",
                List.of(FOURTH_LB_FULL_ID),
                List.of(FIRST_LB_FULL_ID, SECOND_LB_FULL_ID, THIRD_LB_FULL_ID),
                Map.of("1000000", 1),
                Map.of("1170000", 1),
                Map.of("1170200", 1)
            )
        );
    }

    private static Stream<Arguments> provideArsFilterPM() {
        // 1. Parameter Ars
        // 2. erwartete first level Angaben
        // 3. Parameter erwartete secondlevel codes mit zugeordnetem fits level code für das Extrahieren im json Baum
        // 4. Parameter erwartete Thirdlevel Lagen mit übergeordneten second level und first level Zuordnungen
        return Stream.of(
            Arguments.of(
                "091620000000",
                List.of("1000000", "2000000"),
                Map.of("1000000", List.of("1050000", "1170000", "1180000"), "2000000", List.of("2030000")),
                Map.of(
                    "1000000",
                    Map.of(
                        "1050000",
                        List.of("1050200"),
                        "1170000",
                        List.of("1170100", "1170200"),
                        "1180000",
                        List.of("1180200")
                    ),
                    "2000000",
                    Map.of("2030000", List.of("2030700"))
                )
            ),
            Arguments.of(
                "091905174129",
                List.of("1000000", "2000000"),
                Map.of("1000000", List.of("1050000", "1170000"), "2000000", List.of("2030000")),
                Map.of(
                    "1000000",
                    Map.of("1050000", List.of("1050200"), "1170000", List.of("1170100", "1170200")),
                    "2000000",
                    Map.of("2030000", List.of("2030700"))
                )
            ),
            Arguments.of(
                "091905174133",
                List.of("1000000", "2000000"),
                Map.of("1000000", List.of("1170000"), "2000000", List.of("2030000")),
                Map.of(
                    "1000000",
                    Map.of("1170000", List.of("1170200")),
                    "2000000",
                    Map.of("2030000", List.of("2030700"))
                )
            ),
            Arguments.of(
                "091905174149",
                List.of("1000000"),
                Map.of("1000000", List.of("1170000")),
                Map.of("1000000", Map.of("1170000", List.of("1170200")))
            )
        );
    }
}
