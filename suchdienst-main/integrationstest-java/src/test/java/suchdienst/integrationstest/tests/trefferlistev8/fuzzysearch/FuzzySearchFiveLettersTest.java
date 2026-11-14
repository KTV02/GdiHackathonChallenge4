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
package suchdienst.integrationstest.tests.trefferlistev8.fuzzysearch;

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
import suchdienst.integrationstest.validator.servicedescriptionv8.ServiceDescriptionResultListValidator;

/**
 * Testziel: Die Fuzzy Suche wird hier mit dem Trefferbegriff "Lauch" getestet.
 * Eingestellt ist "AUTO5;8"
 * Wenn die Fuzzy Suche aktiv ist, dann sollte wenn der Suchbegriff >= 5 & < 8 Buchstaben hat eine Tippfehler-Toleranz von 1,
 * bei < 5 Buchstaben keine Tippfehler-Toleranz bezüglich des Suchbegriffs aktiv sein.
 * Tippfehler die durch Fuzzy erkannt werden sind in der Elasticsearch Doku zu finden:
 * Einen Buchstaben austauschen (box → fox)
 * Einen Buchstaben entfernen (black → lack)
 * Einen Buchstaben hinzufügen (sic → sick)
 * Zwei benachbarte Buchstaben vertauschen (act → cat)
 * <p>
 * Die Beta-Such-API (V8) kennt keinen "fuzzysearch"-Parameter mehr, die Fuzzy-Suche ist immer aktiv. Da bereits
 * die v7-Variante dieses Tests ausschließlich mit fuzzysearch=true arbeitet, ist der Test unverändert gültig.
 */
class FuzzySearchFiveLettersTest {

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
    @MethodSource({ "fuzzySearch5LettersNegative" })
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
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, q);
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateNotContains(lbId);
    }

    @ParameterizedTest
    @MethodSource({ "fuzzySearch5LettersPositive" })
    void fuzzySearchPositive5LettersTest(String trefferBegriff, String q, TestInfo testInfo) {
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
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, q);
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(lbId);
    }

    @SuppressWarnings("java:1144") // Wird als @MethodSource verwendet
    // Tippfehler-Toleranz von 1, bei 2 Tippfehlern wird der gesuchte Trefferbegriff nicht gefunden.
    private static Stream<Arguments> fuzzySearch5LettersNegative() {
        return Stream.of(
            // Ein Buchstabe wird entfernt
            Arguments.of("Lauch", "Lauc"),
            // Zwei Buchstaben werden hinzugefügt
            Arguments.of("Lauch", "Laauuch"),
            // Zwei benachbarte Buchstaben werden getauscht, ein Buchstabe wird ausgetauscht
            Arguments.of("Lauch", "Kuach")
        );
    }

    @SuppressWarnings("java:1144") // Wird als @MethodSource verwendet
    // Tippfehler-Toleranz von 1, bei einem Tippfehler wird der gesuchte Trefferbegriff gefunden.
    private static Stream<Arguments> fuzzySearch5LettersPositive() {
        return Stream.of(
            Arguments.of("Lauch", "Lauch"),
            // Zwei benachbarte Buchstaben werden vertauscht
            Arguments.of("Lauch", "Luach"),
            // Ein Buchstabe wird geändert
            Arguments.of("Lauch", "Bauch"),
            // Ein Buchstabe wird hinzugefügt
            Arguments.of("Lauch", "Laauch")
        );
    }
}
