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
 * Testziel: Die Fuzzy Suche wird hier mit dem Trefferbegriff "Jubiläum" getestet.
 * Eingestellt ist "AUTO5;8"
 * Wenn die Fuzzy Suche aktiv ist, dann sollte wenn der Suchbegriff >= 5 & < 8 Buchstaben hat eine Tippfehler-Toleranz von 1,
 * bei einem Suchbegriff mit > 8 Buchstaben eine Tippfehler-Toleranz von 2 bezüglich des Suchbegriffs aktiv sein.
 * Tippfehler die durch Fuzzy erkannt werden sind in der Elasticsearch Doku zu finden:
 * Einen Buchstaben austauschen (box → fox)
 * Einen Buchstaben entfernen (black → lack)
 * Einen Buchstaben hinzufügen (sic → sick)
 * Zwei benachbarte Buchstaben vertauschen (act → cat)
 */
class FuzzySearchEightLettersTest {

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
    @MethodSource({ "fuzzySearch8LettersNegative" })
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
    @MethodSource({ "fuzzySearch8LettersPositive" })
    void fuzzySearchPositive8LettersTest(String trefferBegriff, String q, TestInfo testInfo) {
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

    // Tippfehler-Toleranz von 2, bei 3 Tippfehlern wird der gesuchte Trefferbegriff nicht gefunden.
    private static Stream<Arguments> fuzzySearch8LettersNegative() {
        return Stream.of(
            // Ein Buchstabe fehlt, ein Buchstabe wurde ausgetauscht und der letzte Buchstabe fehlt
            Arguments.of("Jubiläum", "Jubyleu"),
            // Zwei benachbarte Buchstaben werden vertauscht und einer ausgetauscht
            Arguments.of("Jubiläum", "Jbulieum"),
            // Drei Buchstaben werden ausgetauscht
            Arguments.of("Jubiläum", "Yubyleum")
        );
    }

    // Tippfehler-Toleranz von 2, bei 2 Tippfehlern wird der gesuchte Trefferbegriff gefunden.
    private static Stream<Arguments> fuzzySearch8LettersPositive() {
        return Stream.of(
            Arguments.of("Jubiläum", "Jubiläum"),
            // Zwei Buchstaben werden hinzugefügt
            Arguments.of("Jubiläum", "Jubiiiläum"),
            // Zwei Buchstaben werden weggelassen
            Arguments.of("Jubiläum", "Jubiläu"),
            // Zwei Buchstaben werden geändert
            Arguments.of("Jubiläum", "Jubyläun"),
            // Zwei benachbarte Buchstaben werden vertauscht
            Arguments.of("Jubiläum", "Juibluäm")
        );
    }
}
