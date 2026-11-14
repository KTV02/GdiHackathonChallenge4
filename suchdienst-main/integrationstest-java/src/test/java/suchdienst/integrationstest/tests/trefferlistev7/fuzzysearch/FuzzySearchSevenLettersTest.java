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
package suchdienst.integrationstest.tests.trefferlistev7.fuzzysearch;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ElasticsearchConnector;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.servicedescriptionv7.ServiceDescriptionResultListValidator;

/**
 * Testziel: Die Fuzzy Suche wird hier mit dem Trefferbegriff "Kapitän" getestet.
 * Eingestellt ist "AUTO5;8"
 * Wenn die Fuzzy Suche aktiv ist, dann sollte wenn der Suchbegriff >= 5 & < 8 Buchstaben hat eine Tippfehler-Toleranz von 1,
 * bei einem Suchbegriff mit >= 8 Buchstaben eine Tippfehler-Toleranz von 2 bezüglich des Suchbegriffs aktiv sein.
 * Tippfehler die durch Fuzzy erkannt werden sind in der Elasticsearch Doku zu finden:
 * Einen Buchstaben austauschen (box → fox)
 * Einen Buchstaben entfernen (black → lack)
 * Einen Buchstaben hinzufügen (sic → sick)
 * Zwei benachbarte Buchstaben vertauschen (act → cat)
 */
class FuzzySearchSevenLettersTest {

    private static String lbId;
    private static String ars;

    @BeforeAll
    static void setup() {
        ElasticsearchConnector.removeAllData();
    }

    @AfterAll
    static void cleanup() {
        ElasticsearchConnector.removeAllData();
    }

    @ParameterizedTest
    @MethodSource({ "fuzzySearch7LettersNegative" })
    void fuzzySearchNegativeTest(String trefferBegriff, String q, TestInfo testInfo) {
        String testclassname = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testclassname).lb(1).oe(1).getMin();
        //set relevant values for trefferliste
        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();

        List<Textmodul> textmodule = new ArrayList<>();
        Textmodul tm03 = Textmodul.builder().inhalt(trefferBegriff).positionDarstellung(3).leikaCode("03").build();
        textmodule.add(tm03);
        lb.setTextmodule(textmodule);

        ImportUtil.pushAndWait23(file);

        ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        lbId = file.getLeistungsbeschreibungen().getFirst().getFullId();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsFuzzyV7(ars, q, true);
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateNotContains(lbId);
    }

    @ParameterizedTest
    @MethodSource({ "fuzzySearch7LettersPositive" })
    void fuzzySearchPositive7LettersTest(String trefferBegriff, String q, TestInfo testInfo) {
        String testclassname = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testclassname).lb(1).oe(1).getMin();
        //set relevant values for trefferliste
        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();

        List<Textmodul> textmodule = new ArrayList<>();
        Textmodul tm03 = Textmodul.builder().inhalt(trefferBegriff).positionDarstellung(3).leikaCode("03").build();
        textmodule.add(tm03);
        lb.setTextmodule(textmodule);

        ImportUtil.pushAndWait23(file);

        ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        lbId = file.getLeistungsbeschreibungen().getFirst().getFullId();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsFuzzyV7(ars, q, true);
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(lbId);
    }

    @SuppressWarnings("java:1144") // Wird als @MethodSource verwendet
    // Tippfehler-Toleranz von 1, bei 2 Tippfehlern wird der gesuchte Trefferbegriff nicht gefunden.
    private static Stream<Arguments> fuzzySearch7LettersNegative() {
        return Stream.of(
            // Ein Buchstabe fehlt und der letzte Buchstabe wurde ausgetauscht
            Arguments.of("Kapitän", "Kaitäm"),
            // Ein Buchstabe wird ausgetauscht und ein weiterer hinzugefügt
            Arguments.of("Kapitän", "Kapiteen"),
            // Zwei Buchstaben werden ausgetauscht
            Arguments.of("Kapitän", "Gapiten"),
            // Zwei benachbarte Buchstaben werden vertauscht
            Arguments.of("Kapitän", "Kaiptnä")
        );
    }

    @SuppressWarnings("java:1144") // Wird als @MethodSource verwendet
    // Tippfehler-Toleranz von 1, bei einem Tippfehler wird der gesuchte Trefferbegriff gefunden.
    private static Stream<Arguments> fuzzySearch7LettersPositive() {
        return Stream.of(
            Arguments.of("Kapitän", "Kapitän"),
            // Ein Buchstabe wird geändert
            Arguments.of("Kapitän", "Gapitän"),
            // Ein Buchstabe wird entfernt
            Arguments.of("Kapitän", "Kaitän"),
            // Ein Buchstabe wird hinzugefügt
            Arguments.of("Kapitän", "Kapiitän"),
            // Zwei benachbarte Buchstaben werden vertauscht
            Arguments.of("Kapitän", "Kaiptän")
        );
    }
}
