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
package suchdienst.integrationstest.tests.trefferlistev7;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Suchbegriff;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
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
 * Beim Indexieren von durchsuchbaren Feldern, werde arabische Zahlen von 1 bis 20 zusätzlich um ihre römischen Werte ergänzt (nicht ersetzt!).
 * Dadurch harmonisieren wir Suchanfragen nach bspw. "Hartz 4" und "Hartz IV"
 * Beim Generieren der Tokens für Suggestions werden nun Zahlen von Wort-Enden abgetrennt, sodass aus "Hartz4" -> "Hartz 4" wird. Diese Ersetzung
 * greift ebenfalls bei `searchNames` und `searchValues`. Diese Veränderung wird nicht auf Texte angewandt, welche dargestellt werden. Da diese
 * Felder aber nicht direkt, sondern mittelbar durch bspw. `searchNames` durchsucht werden, und diese Felder entsprechend angepasst wurden, passt
 * Anfrage und Ergebnis weiterhin zusammen.
 */
class RomanNumeralsTest {

    @BeforeAll
    static void cleanUp() {
        ElasticsearchConnector.removeAllData();
    }

    @ParameterizedTest
    @MethodSource({ "romanNumerals" })
    void romanNumerals(String trefferBegriff, String q, TestInfo testInfo) {
        String testclassname = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testclassname).lb(1).oe(1).getMin();

        //set relevant values for trefferliste
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        List<Suchbegriff> suchbegriffe = new ArrayList<>();
        suchbegriffe.add(Suchbegriff.builder().begriff(trefferBegriff).begriffEnglisch(trefferBegriff).build());
        leistung.setSuchbegriffe(suchbegriffe);
        ImportUtil.pushAndWait23(file);

        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        String lbId = file.getLeistungsbeschreibungen().getFirst().getFullId();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsFuzzyV7(ars, q, false);
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(lbId);
    }

    private static Stream<Arguments> romanNumerals() {
        return Stream.of(
            Arguments.of("Formel I", "Formel1"),
            Arguments.of("Rennen IV", "Rennen 4"),
            Arguments.of("Gans VI", "Gans 6"),
            Arguments.of("Alle IX", "Alle 9"),
            Arguments.of("JinPing XI", "JinPing11"),
            Arguments.of("Romanum XIV", "Romanum 14"),
            Arguments.of("Sechzehn XVI", "Sechzehn 16"),
            Arguments.of("Geburtstag XiX", "Geburtstag 19"),
            Arguments.of("Follow Xx", "Follow 20")
        );
    }
}
