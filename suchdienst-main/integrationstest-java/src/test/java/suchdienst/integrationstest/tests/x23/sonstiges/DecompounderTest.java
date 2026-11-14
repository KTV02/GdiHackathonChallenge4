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
package suchdienst.integrationstest.tests.x23.sonstiges;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.response.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.connectors.ElasticsearchConnector;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.servicedescriptionv8.ServiceDescriptionResultListValidator;

/**
 * Der Decompounder trennt den Titel in seine Teilwörter.
 * Die minimale Zeichenzahl für Teilwörter betrögt 4.
 * In der Testsuite werden mit den Teilwörtern Suchanfragen gestellt und geprüft, ob die Leistungen in der Trefferliste gefunden werden.
 */
class DecompounderTest {

    @BeforeAll
    static void getVLInfo() {
        // Bereinige Datenbestand, um Suchtreffer stabiler vorherzusagen
        ElasticsearchConnector.removeAllData();
    }

    @Test
    void decompounderPositive(TestInfo testInfo) {
        String testcasename = testInfo.getTestMethod().orElseThrow().getName();
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(2).oe(1).getMin();

        // Setze Leistungs-Titel für Suche über Trefferliste
        Leistungsbeschreibung lbBuergermeisterwahl = file.getLeistungsbeschreibungen().getFirst();
        Textmodul tm03 = Textmodul.builder().inhalt("Bürgermeisterwahl").positionDarstellung(3).leikaCode("03").build();
        lbBuergermeisterwahl.setTextmodule(List.of(tm03));

        Leistungsbeschreibung lbKfzVersicherung = file.getLeistungsbeschreibungen().get(1);
        Textmodul tm03KfzVersicherung = Textmodul.builder()
            .inhalt("Donaudampfschiffkapitän")
            .positionDarstellung(3)
            .leikaCode("03")
            .build();
        lbKfzVersicherung.setTextmodule(List.of(tm03KfzVersicherung));

        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        String lbIdBurgermeisterWahl = lbBuergermeisterwahl.getFullId();
        String lbIdDampfSchiff = lbKfzVersicherung.getFullId();
        ImportUtil.pushAndWait23(file);

        // Titel Bürgermeisterwahl wird decompounded in "Bürger", "Meister", "Wahl"
        // Die Identität des Einzelnen wortes "Bürgermeisterwahl" wird ebenfalls gefunden
        searchAndValidate(ars, "Bürgermeisterwahl", lbIdBurgermeisterWahl);
        searchAndValidate(ars, "Bürger", lbIdBurgermeisterWahl);
        searchAndValidate(ars, "Meister", lbIdBurgermeisterWahl);
        searchAndValidate(ars, "Wahl", lbIdBurgermeisterWahl);
        // Der Decompounder wird nicht auf die Eingabeworte angewendet, sondern nur auf die Dokumente im Index
        // dadurch führen Teilworte außerhalb des Wörterbuchs nicht zu treffern.
        searchAndValidateNoContent(ars, "Bürgermeister");
        searchAndValidateNoContent(ars, "Meisterwahl");

        // Titel Donaudampfschiffkapitän wird decompounded in "Donau", "Dampf", "Schiff", "Kapitän"
        // Die Identität des Einzelnen wortes "Donaudampfschiffkapitän" wird ebenfalls gefunden
        searchAndValidate(ars, "Donaudampfschiffkapitän", lbIdDampfSchiff);
        searchAndValidate(ars, "Donau", lbIdDampfSchiff);
        searchAndValidate(ars, "Dampf", lbIdDampfSchiff);
        searchAndValidate(ars, "Schiff", lbIdDampfSchiff);
        searchAndValidate(ars, "Kapitän", lbIdDampfSchiff);
        // Der Decompounder wird nicht auf die Eingabeworte angewendet, sondern nur auf die Dokumente im Index
        // dadurch führen Teilworte außerhalb des Wörterbuchs nicht zu treffern.
        searchAndValidateNoContent(ars, "Donaudampf");
        searchAndValidateNoContent(ars, "schiffkapitän");
    }

    @Test
    void decompounderNegative(TestInfo testInfo) {
        String testcasename = testInfo.getTestMethod().orElseThrow().getName();

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();

        // Setze Leistungs-Titel für Suche über Trefferliste

        Leistungsbeschreibung lbAuditorien = file.getLeistungsbeschreibungen().getFirst();
        Textmodul tm03lbAuditorien = Textmodul.builder()
            .inhalt("Auditorien")
            .positionDarstellung(3)
            .leikaCode("03")
            .build();
        lbAuditorien.setTextmodule(List.of(tm03lbAuditorien));

        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();

        ImportUtil.pushAndWait23(file);

        // Au ist im Wörterbuch, hat aber die minimale "Subword Size" von 4 nicht erreicht
        searchAndValidateNoContent(ars, "Au");
        // Audi ist nicht im Wörterbuch
        searchAndValidateNoContent(ars, "Audi");
    }

    private void searchAndValidate(String ars, String searchValue, String expectedLbId) {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, searchValue);
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(expectedLbId);
    }

    private void searchAndValidateNoContent(String ars, String searchValue) {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, searchValue);
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateNoContent();
    }
}
