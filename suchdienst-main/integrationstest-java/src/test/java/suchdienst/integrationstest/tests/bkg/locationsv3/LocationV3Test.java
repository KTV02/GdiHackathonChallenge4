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
package suchdienst.integrationstest.tests.bkg.locationsv3;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Streams;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.connectors.LocationsConnector;
import suchdienst.integrationstest.mocks.BKGMock;
import suchdienst.integrationstest.mocks.CodelistMock;
import suchdienst.integrationstest.utils.RedisUtil;

@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class LocationV3Test {

    private static BKGMock bkgMock;
    private static CodelistMock codelistMock;

    @BeforeAll
    static void setup() {
        bkgMock = new BKGMock();
        codelistMock = new CodelistMock();
    }

    @BeforeEach
    void beforeEach() throws Exception {
        codelistMock.createCodelistVersionResponse();
        codelistMock.createBevoelkerungsstatistik();

        RedisUtil.flushRedisCache();
    }

    @AfterEach
    void reset() {
        bkgMock.reset();
        codelistMock.reset();
    }

    /**
     * <p>Testet das Verhalten bei Anfragen an den BKG, die dazu führen, dass nachgelagerte Requests ausgeführt werden.</p>
     * <p>Zusätzliche Anfragen entstehen, wenn im Schlüssel neben dem Inhalt aus Feld rs weitere Arsschlüssel aus dem Feld schluessel (Liste von Werten) extrahiert werden kann </p>
     * <p>Hier wird mitgetestet, dass die Extraktion aus dem Schlüssel Feld korrekt funktioniert.</p>
     * <p>2 nachgelagerte Anfragen werden erwartet.</p>
     * <p>Ergebnisse aus nachgelagerten Requests tauchen auf aber erst nach den Ergebnissen aus dem ersten Request</p>
     */

    @Test
    void bkgNachgelagertOk() throws IOException {
        String json = IOUtils.resourceToString("/bkg/OrtHamburgHarburg.json", StandardCharsets.UTF_8);
        String jsonNachgelagert1 = IOUtils.resourceToString(
            "/bkg/OrtHamburgHarburgNachgelagert1rs.json",
            StandardCharsets.UTF_8
        );
        String jsonNachgelagert2 = IOUtils.resourceToString(
            "/bkg/OrtHamburgHarburgNachgelagert2rs.json",
            StandardCharsets.UTF_8
        );
        bkgMock.createGeocodeResponseBkgMock("Hamburg Harburg", json);
        bkgMock.createOrtNachgelagertResponseBkgMock("rs:027000000009 OR rs:027000000004", jsonNachgelagert2);
        bkgMock.createOrtNachgelagertResponseBkgMock("rs:027000000005", jsonNachgelagert1);
        Response response = LocationsConnector.getLocationsV3Details("Hamburg Harburg", 10);
        JsonPath jsonResponsePath = response.jsonPath();
        List<String> ortsListe = jsonResponsePath.getList("name");
        assertThat(response.getStatusCode(), is(HttpStatus.SC_OK));
        //Namen aus erster BKG Anfrage tauchen im Response auf
        assertThat(ortsListe, hasItem("Hamburg, Freie und Hansestadt"));
        assertThat(ortsListe, hasItem("Hamburg 'Harburg'"));
        //Name aus nachgelagertem Request 2 wird angehängt - im Response taucht es nach den 4 Ergebnissen aus der ersten Anfrage auf
        assertThat(ortsListe, hasItem("Hamburg 'Eißendorf'"));
        assertThat(ortsListe.indexOf("Hamburg 'Eißendorf'"), greaterThan(3));
        //Name aus nachgelagertem Request 1 wird angehängt - taucht im Response wegen limit 10 nicht auf
        assertThat(ortsListe, not(hasItem("Hamburg 'Rönneburg'")));
        //obwohl mehr Daten gefunden wurden - limit 10
        assertThat(ortsListe.size(), is(10));

        //Abfrage mit limit 15
        response = LocationsConnector.getLocationsV3Details("Hamburg Harburg", 15);
        jsonResponsePath = response.jsonPath();
        ortsListe = jsonResponsePath.getList("name");
        assertThat(response.getStatusCode(), is(HttpStatus.SC_OK));
        //Rönneburg taucht bei limit 15 auf
        assertThat(ortsListe, hasItem("Hamburg 'Rönneburg'"));
        assertThat(ortsListe.indexOf("Hamburg 'Rönneburg'"), greaterThan(9));
        assertThat(ortsListe.size(), is(13));

        bkgMock.verifyOrtBkgMockResponse("Hamburg Harburg", 2);
        bkgMock.verifyRSNachgelagertBkgMockResponse("rs:027000000009 OR rs:027000000004", 2);
        bkgMock.verifyRSNachgelagertBkgMockResponse("rs:027000000005", 2);
    }

    /**
     * <p>Testet das Verhalten bei Anfragen an den BKG, die dazu führen, dass ein nachgelagerter Request ausgeführt werden soll aber zu einem Timeout führt.</p>
     * <p>Ergebnisse aus dem ersten Request werden trotzdem mit Status 200 zurückgegeben</p>
     */

    @Test
    void bkgTimeoutNachgelagert() throws IOException {
        String json = IOUtils.resourceToString("/bkg/OrtDannewerk.json", StandardCharsets.UTF_8);
        bkgMock.createGeocodeResponseBkgMock("Dannewerk", json);
        bkgMock.createOrtNachgelagertTimeOutResponseBkgMock("rs:010595993023");
        Response response = LocationsConnector.getLocationsV3Details("Dannewerk", 10);
        JsonPath jsonResponsePath = response.jsonPath();
        List<String> suggestionList = jsonResponsePath.getList("name");
        assertThat(response.getStatusCode(), is(HttpStatus.SC_OK));
        assertThat(suggestionList.size(), is(1));
        assertThat(suggestionList.getFirst(), equalTo("Dannewerk"));
        bkgMock.verifyOrtBkgMockResponse("Dannewerk", 1);
    }

    @Test
    void bkgTimeoutPartiellNachgelagert() throws IOException {
        String json = IOUtils.resourceToString("/bkg/OrtHamburgHarburg.json", StandardCharsets.UTF_8);
        String jsonNachgelagert2 = IOUtils.resourceToString(
            "/bkg/OrtHamburgHarburgNachgelagert2rs.json",
            StandardCharsets.UTF_8
        );
        bkgMock.createGeocodeResponseBkgMock("Hamburg Harburg", json);
        bkgMock.createOrtNachgelagertResponseBkgMock("rs:027000000009 OR rs:027000000004", jsonNachgelagert2);
        bkgMock.createOrtNachgelagertTimeOutResponseBkgMock("rs:027000000005");
        Response response = LocationsConnector.getLocationsV3Details("Hamburg Harburg", 15);
        JsonPath jsonResponsePath = response.jsonPath();
        List<String> ortsListe = jsonResponsePath.getList("name");
        assertThat(response.getStatusCode(), is(HttpStatus.SC_OK));
        //Name aus Response, der auf keinen Timeout läuft, wird ausgegeben
        assertThat(ortsListe, hasItem("Hamburg 'Eißendorf'"));
        //Name aus Response, der auf Timeout läuft, wird nicht ausgegeben
        assertThat(ortsListe, not(hasItem("Hamburg 'Rönneburg'")));
        //es könnten 13 Einträge zurückgeliefert werden - 2 Einträge aus nachgelagertem Request laufen auf Timeout
        assertThat(ortsListe.size(), is(11));
        bkgMock.verifyOrtBkgMockResponse("Hamburg Harburg", 1);
    }

    /**
     * <p>Testet das Verhalten bei Anfragen an den BKG, die dazu führen, dass zwei nachgelagerte Requests ausgeführt werden sollen wobei ein Request mit Status 403 scheitert.</p>
     * <p>Ergebnisse aus dem ersten Request werden zusammen mit Ergebnissen aus dem erfolgreichen nachgelagerten Request trotzdem mit Status 200 zurückgegeben</p>
     */

    @Test
    void bkgErrorPartiellNachgelagert() throws IOException {
        String json = IOUtils.resourceToString("/bkg/OrtHamburgHarburg.json", StandardCharsets.UTF_8);
        String jsonNachgelagert2 = IOUtils.resourceToString(
            "/bkg/OrtHamburgHarburgNachgelagert2rs.json",
            StandardCharsets.UTF_8
        );
        bkgMock.createGeocodeResponseBkgMock("Hamburg Harburg", json);
        bkgMock.createOrtNachgelagertResponseBkgMock("rs:027000000009 OR rs:027000000004", jsonNachgelagert2);
        bkgMock.createOrtNachgelagertErrorResponseBkgMock("rs:027000000005");
        Response response = LocationsConnector.getLocationsV3Details("Hamburg Harburg", 15);
        JsonPath jsonResponsePath = response.jsonPath();
        List<String> ortsListe = jsonResponsePath.getList("name");
        assertThat(response.getStatusCode(), is(HttpStatus.SC_OK));
        //Name aus Response, der auf keinen Timeout läuft, wird ausgegeben
        assertThat(ortsListe, hasItem("Hamburg 'Eißendorf'"));
        //Name aus Response, der auf Timeout läuft wird, nicht ausgegeben
        assertThat(ortsListe, not(hasItem("Hamburg 'Rönneburg'")));
        //es könnten 13 Einträge zurückgeliefert werden - 2 Einträge aus nachgelagertem Request laufen auf Error
        assertThat(ortsListe.size(), is(11));
        bkgMock.verifyOrtBkgMockResponse("Hamburg Harburg", 1);
    }

    /**
     * <p>Testet, dass bei Antwort des BKG mit einem unbekannten ARS die Antwort ignoriert wird</p>
     * <p>Hier wird ein Element vom BKG mit unbekannter ARS zurückgegeben - v3/locations liefert leere Liste</p>
     */
    @Disabled("Filterung soll nur Stadtstaaten betreffen")
    @Test
    void arsUnbekannt() throws IOException {
        String json = IOUtils.resourceToString("/bkg/OrtLaboeArsUnbekannt.json", StandardCharsets.UTF_8);
        bkgMock.createGeocodeResponseBkgMock("Laboe", json);
        Response response = LocationsConnector.getLocationsV3Details("Laboe", 10);
        assertThat(response.getStatusCode(), is(HttpStatus.SC_OK));
        response.then().body("", Matchers.hasSize(0));
        bkgMock.verifyOrtBkgMockResponse("Laboe", 1);
    }

    /**
     * <p>Testet, dass für eine nachgelagerte Antwort des BKG mit einem unbekannten ARS die betreffenden Elemente ignoriert werden</p>
     * <p>In der nachgelagerten Antwort sind 3 Elemente mit unbekannter ARS und 2 Elemente mit echter ARS - v3/locations liefert Ergebnis aus erster Anfrage und den 2 gültigen Elementen aus der nachgelagerten Anfrage</p>
     */
    @Disabled("Filterung soll nur Stadtstaaten betreffen")
    @Test
    void arsUnbekanntNachgelagert() throws IOException {
        String json = IOUtils.resourceToString("/bkg/OrtMeldorf.json", StandardCharsets.UTF_8);
        String jsonNachgelagert = IOUtils.resourceToString(
            "/bkg/OrtMeldorfNachgelagertArsUnbekannt.json",
            StandardCharsets.UTF_8
        );
        bkgMock.createGeocodeResponseBkgMock("Meldorf", json);
        bkgMock.createOrtNachgelagertResponseBkgMock("rs:019999999999 OR rs:010515175078", jsonNachgelagert);
        Response response = LocationsConnector.getLocationsV3Details("Meldorf", 10);
        JsonPath jsonResponsePath = response.jsonPath();
        List<String> ortsListe = jsonResponsePath.getList("name");
        assertThat(response.getStatusCode(), is(HttpStatus.SC_OK));
        //Namen aus erster BKG Anfrage tauchen im Response auf
        assertThat(ortsListe, hasItem("Meldorf"));
        assertThat(ortsListe, hasItem("Nindorf bei Meldorf"));
        assertThat(ortsListe, hasItem("Nindorf bei Meldorf 'Farnewinkel'"));
        //3 Ergebnisse mit unbekannter ARS werden nicht mit ausgegeben
        assertThat(ortsListe.size(), is(3));
        bkgMock.verifyOrtBkgMockResponse("Meldorf", 1);
        bkgMock.verifyRSNachgelagertBkgMockResponse("rs:019999999999 OR rs:010515175078", 1);
    }

    /**
     * <p>Testet das Verhalten bei Anfragen mit Gruppierung nach ARS.</p>
     * <p>Jeder ARS Wert taucht nur einmal auf.</p>
     */

    @Test
    void groupByArs() throws IOException {
        String json = IOUtils.resourceToString("/bkg/OrtHamburgHarburg.json", StandardCharsets.UTF_8);
        String jsonNachgelagert1 = IOUtils.resourceToString(
            "/bkg/OrtHamburgHarburgNachgelagert1rs.json",
            StandardCharsets.UTF_8
        );
        String jsonNachgelagert2 = IOUtils.resourceToString(
            "/bkg/OrtHamburgHarburgNachgelagert2rs.json",
            StandardCharsets.UTF_8
        );
        bkgMock.createGeocodeResponseBkgMock("Hamburg Harburg", json);
        bkgMock.createOrtNachgelagertResponseBkgMock("rs:027000000009 OR rs:027000000004", jsonNachgelagert2);
        bkgMock.createOrtNachgelagertResponseBkgMock("rs:027000000005", jsonNachgelagert1);
        Response response = LocationsConnector.getLocationsV3Details("Hamburg Harburg", 10, List.of("ARS"));
        JsonPath jsonResponsePath = response.jsonPath();
        List<String> arsListe = jsonResponsePath.getList("ars");
        assertTrue(noDuplicates(arsListe));
        assertThat(arsListe.size(), is(6));
    }

    /**
     * <p>Testet das Verhalten bei Anfragen mit Gruppierung nach ARS.</p>
     * <p>Jeder ARS Wert taucht nur einmal auf.</p>
     */

    @Test
    void groupByArsNoDistrict() throws IOException {
        String json = IOUtils.resourceToString("/bkg/OrtHamburgHarburg.json", StandardCharsets.UTF_8);
        String jsonNachgelagert1 = IOUtils.resourceToString(
            "/bkg/OrtHamburgHarburgNachgelagert1rs.json",
            StandardCharsets.UTF_8
        );
        String jsonNachgelagert2 = IOUtils.resourceToString(
            "/bkg/OrtHamburgHarburgNachgelagert2rs.json",
            StandardCharsets.UTF_8
        );
        bkgMock.createGeocodeResponseBkgMock("Hamburg Harburg", json);
        bkgMock.createOrtNachgelagertResponseBkgMock("rs:027000000009 OR rs:027000000004", jsonNachgelagert2);
        bkgMock.createOrtNachgelagertResponseBkgMock("rs:027000000005", jsonNachgelagert1);
        Response response = LocationsConnector.getLocationsV3Details("Hamburg Harburg", 10, List.of("ARS"), false);
        JsonPath jsonResponsePath = response.jsonPath();
        List<String> arsListe = jsonResponsePath.getList("ars");
        assertTrue(noDuplicates(arsListe));
        assertThat(arsListe.size(), is(1));
        arsListe.forEach(ars -> assertThat(ars, is("020000000000")));
    }

    /**
     * <p>Testet das Verhalten bei Anfragen mit Gruppierung nach PLZ.</p>
     * <p>Jeder PLZ Wert taucht nur einmal auf.</p>
     */
    @Test
    void groupByPlz() throws IOException {
        String json = IOUtils.resourceToString("/bkg/OrtHamburgHarburg.json", StandardCharsets.UTF_8);
        String jsonNachgelagert1 = IOUtils.resourceToString(
            "/bkg/OrtHamburgHarburgNachgelagert1rs.json",
            StandardCharsets.UTF_8
        );
        String jsonNachgelagert2 = IOUtils.resourceToString(
            "/bkg/OrtHamburgHarburgNachgelagert2rs.json",
            StandardCharsets.UTF_8
        );
        bkgMock.createGeocodeResponseBkgMock("Hamburg Harburg", json);
        bkgMock.createOrtNachgelagertResponseBkgMock("rs:027000000009 OR rs:027000000004", jsonNachgelagert2);
        bkgMock.createOrtNachgelagertResponseBkgMock("rs:027000000005", jsonNachgelagert1);
        Response response = LocationsConnector.getLocationsV3Details("Hamburg Harburg", 10, List.of("PLZ"));
        JsonPath jsonResponsePath = response.jsonPath();
        List<String> plzListe = jsonResponsePath.getList("plz");
        assertTrue(noDuplicates(plzListe));
        assertThat(plzListe.size(), is(5));
    }

    /**
     * <p>Testet das Verhalten bei Anfragen mit Gruppierung nach PLZ ohne Ortsteilsuche.</p>
     * <p>Jeder PLZ Wert taucht nur einmal auf.</p>
     */
    @Test
    void groupByPlzNoDistrict() throws IOException {
        String json = IOUtils.resourceToString("/bkg/OrtHamburgHarburg.json", StandardCharsets.UTF_8);
        String jsonNachgelagert1 = IOUtils.resourceToString(
            "/bkg/OrtHamburgHarburgNachgelagert1rs.json",
            StandardCharsets.UTF_8
        );
        String jsonNachgelagert2 = IOUtils.resourceToString(
            "/bkg/OrtHamburgHarburgNachgelagert2rs.json",
            StandardCharsets.UTF_8
        );
        bkgMock.createGeocodeResponseBkgMock("Hamburg Harburg", json);
        bkgMock.createOrtNachgelagertResponseBkgMock("rs:027000000009 OR rs:027000000004", jsonNachgelagert2);
        bkgMock.createOrtNachgelagertResponseBkgMock("rs:027000000005", jsonNachgelagert1);
        Response response = LocationsConnector.getLocationsV3Details("Hamburg Harburg", 10, List.of("PLZ"), false);
        JsonPath jsonResponsePath = response.jsonPath();
        List<String> plzListe = jsonResponsePath.getList("plz");
        assertTrue(noDuplicates(plzListe));
        assertThat(plzListe.size(), is(5));
        List<String> arsListe = jsonResponsePath.getList("ars");
        arsListe.forEach(ars -> assertThat(ars, is("020000000000")));
    }

    /**
     * <p>Testet das Verhalten bei Anfragen mit Gruppierung nach Name.</p>
     * <p>Jeder Name taucht nur einmal auf.</p>
     */
    @Test
    void groupByName() throws IOException {
        String json = IOUtils.resourceToString("/bkg/OrtHamburgHarburg.json", StandardCharsets.UTF_8);
        String jsonNachgelagert1 = IOUtils.resourceToString(
            "/bkg/OrtHamburgHarburgNachgelagert1rs.json",
            StandardCharsets.UTF_8
        );
        String jsonNachgelagert2 = IOUtils.resourceToString(
            "/bkg/OrtHamburgHarburgNachgelagert2rs.json",
            StandardCharsets.UTF_8
        );
        bkgMock.createGeocodeResponseBkgMock("Hamburg Harburg", json);
        bkgMock.createOrtNachgelagertResponseBkgMock("rs:027000000009 OR rs:027000000004", jsonNachgelagert2);
        bkgMock.createOrtNachgelagertResponseBkgMock("rs:027000000005", jsonNachgelagert1);
        Response response = LocationsConnector.getLocationsV3Details("Hamburg Harburg", 10, List.of("NAME"));
        JsonPath jsonResponsePath = response.jsonPath();
        List<String> nameListe = jsonResponsePath.getList("name");
        assertTrue(noDuplicates(nameListe));
        assertThat(nameListe.size(), is(6));
    }

    /**
     * <p>Testet das Verhalten bei Anfragen mit Gruppierung nach Name ohne Ortsteil.</p>
     * <p>Jeder Name taucht nur einmal auf.</p>
     */
    @Test
    void groupByNameNoDistrict() throws IOException {
        String json = IOUtils.resourceToString("/bkg/OrtHamburgHarburg.json", StandardCharsets.UTF_8);
        String jsonNachgelagert1 = IOUtils.resourceToString(
            "/bkg/OrtHamburgHarburgNachgelagert1rs.json",
            StandardCharsets.UTF_8
        );
        String jsonNachgelagert2 = IOUtils.resourceToString(
            "/bkg/OrtHamburgHarburgNachgelagert2rs.json",
            StandardCharsets.UTF_8
        );
        bkgMock.createGeocodeResponseBkgMock("Hamburg Harburg", json);
        bkgMock.createOrtNachgelagertResponseBkgMock("rs:027000000009 OR rs:027000000004", jsonNachgelagert2);
        bkgMock.createOrtNachgelagertResponseBkgMock("rs:027000000005", jsonNachgelagert1);
        Response response = LocationsConnector.getLocationsV3Details("Hamburg Harburg", 10, List.of("NAME"), false);
        JsonPath jsonResponsePath = response.jsonPath();
        List<String> nameListe = jsonResponsePath.getList("name");
        assertTrue(noDuplicates(nameListe));
        assertThat(nameListe.size(), is(6));
        List<String> arsListe = jsonResponsePath.getList("ars");
        arsListe.forEach(ars -> assertThat(ars, is("020000000000")));
    }

    /**
     * <p>Testet das Verhalten bei Anfragen mit Gruppierung nach ARS und Name.</p>
     * <p>Jede Kombination aus ARS und Name taucht nur einmal auf.</p>
     */
    @Test
    void groupByNameArs() throws IOException {
        String json = IOUtils.resourceToString("/bkg/OrtHamburgHarburg.json", StandardCharsets.UTF_8);
        String jsonNachgelagert1 = IOUtils.resourceToString(
            "/bkg/OrtHamburgHarburgNachgelagert1rs.json",
            StandardCharsets.UTF_8
        );
        String jsonNachgelagert2 = IOUtils.resourceToString(
            "/bkg/OrtHamburgHarburgNachgelagert2rs.json",
            StandardCharsets.UTF_8
        );
        bkgMock.createGeocodeResponseBkgMock("Hamburg Harburg", json);
        bkgMock.createOrtNachgelagertResponseBkgMock("rs:027000000009 OR rs:027000000004", jsonNachgelagert2);
        bkgMock.createOrtNachgelagertResponseBkgMock("rs:027000000005", jsonNachgelagert1);
        Response response = LocationsConnector.getLocationsV3Details(
            "Hamburg Harburg",
            10,
            List.of(),
            List.of("NAME", "ARS")
        );
        JsonPath jsonResponsePath = response.jsonPath();
        List<String> nameListe = jsonResponsePath.getList("name");
        List<String> arsListe = jsonResponsePath.getList("ars");
        var arsNameList = Streams.zip(nameListe.stream(), arsListe.stream(), (name, ars) -> name + ars).toList();
        assertTrue(noDuplicates(arsNameList));
        assertThat(arsNameList.size(), is(7));
    }

    /**
     * <p>Testet das Verhalten bei Anfragen mit PLZ Filter.</p>
     * <p>Datensätze ohne PLZ werden rausgefiltert.</p>
     */
    @Test
    void filterPlz() throws IOException {
        String json = IOUtils.resourceToString("/bkg/OrtKiel.json", StandardCharsets.UTF_8);
        bkgMock.createGeocodeResponseBkgMock("Kiel", json);
        Response response = LocationsConnector.getLocationsV3Details("Kiel", 10, List.of("plz"), List.of());
        JsonPath jsonResponsePath = response.jsonPath();
        List<String> nameListe = jsonResponsePath.getList("name");
        assertThat(nameListe, hasItem("Kiel 'Altstadt'"));
        assertThat(nameListe.size(), is(9));
        assertThat(nameListe, not(hasItem("Kiel")));
    }

    /**
     * <p>Testet das Verhalten bei Anfragen mit PLZ Filter ohne Ortsteilsuche</p>
     * <p>Datensätze ohne PLZ werden rausgefiltert.</p>
     * <p>Im ersten nachgelegerten Response liefert der BKG Mock einen Eintrag ohne PLZ - der darf nicht in der Ergebnismenge auftauche</p>
     */
    @Test
    void filterPlzNoOrtsteil() throws IOException {
        String json = IOUtils.resourceToString("/bkg/OrtHamburgHarburg.json", StandardCharsets.UTF_8);
        String jsonNachgelagert1 = IOUtils.resourceToString(
            "/bkg/OrtHamburgHarburgNachgelagert1rsMissingPlz.json",
            StandardCharsets.UTF_8
        );
        String jsonNachgelagert2 = IOUtils.resourceToString(
            "/bkg/OrtHamburgHarburgNachgelagert2rs.json",
            StandardCharsets.UTF_8
        );
        bkgMock.createGeocodeResponseBkgMock("Hamburg Harburg", json);
        bkgMock.createOrtNachgelagertResponseBkgMock("rs:027000000009 OR rs:027000000004", jsonNachgelagert2);
        bkgMock.createOrtNachgelagertResponseBkgMock("rs:027000000005", jsonNachgelagert1);
        Response response = LocationsConnector.getLocationsV3Details(
            "Hamburg Harburg",
            null,
            null,
            null,
            15,
            List.of("plz"),
            null,
            false
        );
        JsonPath jsonResponsePath = response.jsonPath();
        List<String> ortsListe = jsonResponsePath.getList("name");
        List<String> arsListe = jsonResponsePath.getList("ars");

        assertThat(response.getStatusCode(), is(HttpStatus.SC_OK));
        //Namen aus erster BKG Anfrage tauchen im Response auf
        assertThat(ortsListe, not(hasItem("Hamburg 'Ohneplz'")));
        assertThat(ortsListe, hasItem("Hamburg 'Harburg'"));
        //Name aus nachgelagertem Request 2 wird angehängt - im Response taucht es nach den 4 Ergebnissen aus der ersten Anfrage auf
        assertThat(ortsListe, hasItem("Hamburg 'Eißendorf'"));
        assertThat(ortsListe.size(), is(12));
        // alle ars haben den Wert 02000000000000
        arsListe.forEach(ars -> assertThat(ars, is("020000000000")));
        //Name aus nachgelagertem Request 1 wird angehängt - im Response taucht es nach den 4 Ergebnissen aus der ersten Anfrage auf
        assertThat(ortsListe, hasItem("Hamburg 'Rönneburg'"));
        assertThat(ortsListe.indexOf("Hamburg 'Rönneburg'"), greaterThan(3));
        bkgMock.verifyOrtBkgMockResponse("Hamburg Harburg", 1);
        bkgMock.verifyRSNachgelagertBkgMockResponse("rs:027000000009 OR rs:027000000004", 1);
        bkgMock.verifyRSNachgelagertBkgMockResponse("rs:027000000005", 1);
    }

    @Test
    void filterStrasseUndHausnummer() throws IOException {
        String ort = "Kiel";
        String strasse = "Alter Markt";
        String hausnummer = "9";
        String json = IOUtils.resourceToString("/bkg/OrtKiel.json", StandardCharsets.UTF_8);
        bkgMock.createGeocodeOrtStrasseHausnrResponseBkgMock(ort, strasse, hausnummer, json);
        Response response = LocationsConnector.getLocationsV3Details(
            ort,
            null,
            strasse,
            hausnummer,
            10,
            List.of("plz"),
            null,
            null
        );
        JsonPath jsonResponsePath = response.jsonPath();
        List<String> nameListe = jsonResponsePath.getList("name");
        assertThat(nameListe, hasItem("Kiel 'Altstadt'"));
    }

    @Test
    void filterStrasseUndPlz() throws IOException {
        String ort = "Kiel";
        String strasse = "Alter Markt";
        String hausnummer = "9";
        String json = IOUtils.resourceToString("/bkg/OrtKiel.json", StandardCharsets.UTF_8);
        bkgMock.createGeocodeOrtStrasseHausnrResponseBkgMock(ort, strasse, hausnummer, json);
        Response response = LocationsConnector.getLocationsV3Details(
            ort,
            null,
            strasse,
            hausnummer,
            10,
            List.of("plz", "strasse"),
            null,
            null
        );
        JsonPath jsonResponsePath = response.jsonPath();
        List<String> nameListe = jsonResponsePath.getList("name");
        List<String> strasseListe = jsonResponsePath.getList("strasse");
        List<String> hausListe = jsonResponsePath.getList("hausnummer");
        List<List<String>> koordinaten = jsonResponsePath.getList("koordinaten");

        assertThat(nameListe, hasItem("Kiel 'Altstadt'"));
        assertThat(nameListe, hasSize(1));
        assertThat(strasseListe, hasItem(strasse));
        assertThat(strasseListe, hasSize(1));
        assertThat(hausListe, hasItem(hausnummer));
        assertThat(hausListe, hasSize(1));
        // Auch mit Filtern sollten Koordinaten vorhanden sein
        assertThat(koordinaten, hasSize(1));
        assertThat(koordinaten.getFirst(), hasSize(2));
    }

    @Test
    void rejectInvalidDetailsRequest() {
        Response response = LocationsConnector.getLocationsV3Details(
            null,
            null,
            "Alter Markt",
            "9",
            10,
            List.of("plz"),
            null,
            null
        );
        assertThat(response.getStatusCode(), is(400));
        JsonPath jsonResponsePath = response.jsonPath();
        var message = jsonResponsePath.getString("message");
        assertThat(
            message,
            is(
                "Für die Suche mit einer Straße oder Hausnummer muss mindestens einer der Parameter ort oder plz gefüllt sein."
            )
        );
    }

    @Test
    void filterPlzGroupByArs() throws IOException {
        String json = IOUtils.resourceToString("/bkg/OrtKiel.json", StandardCharsets.UTF_8);
        bkgMock.createGeocodeResponseBkgMock("Kiel", json);
        Response response = LocationsConnector.getLocationsV3Details("Kiel", 10, List.of("plz"), List.of("ARS"));
        JsonPath jsonResponsePath = response.jsonPath();
        List<String> nameListe = jsonResponsePath.getList("name");
        assertThat(nameListe, hasItem("Kiel 'Altstadt'"));
        assertThat(nameListe, not(hasItem("Kiel")));
    }

    @Test
    void locationByArsFull() throws IOException {
        String json = IOUtils.resourceToString("/bkg/OrtHamburgHarburg.json", StandardCharsets.UTF_8);
        String jsonNachgelagert1 = IOUtils.resourceToString(
            "/bkg/OrtHamburgHarburgNachgelagert1rs.json",
            StandardCharsets.UTF_8
        );
        bkgMock.createGeocodeResponseBkgMock("Hamburg", json);
        bkgMock.createOrtNachgelagertResponseBkgMock("rs:123456789012 AND schluessel:123456789012", jsonNachgelagert1);
        Response response = LocationsConnector.getLocationsV3ByArs("123456789012", 10, List.of("plz"), List.of("ARS"));
        JsonPath jsonResponsePath = response.jsonPath();
        List<String> nameListe = jsonResponsePath.getList("name");
        assertThat(nameListe, hasItem("Hamburg 'Rönneburg'"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "123456789", "123456789000" })
    void locationByArsWildcard(String ars) throws IOException {
        String json = IOUtils.resourceToString("/bkg/OrtHamburgHarburg.json", StandardCharsets.UTF_8);
        String jsonNachgelagert1 = IOUtils.resourceToString(
            "/bkg/OrtHamburgHarburgNachgelagert1rs.json",
            StandardCharsets.UTF_8
        );
        bkgMock.createGeocodeResponseBkgMock("Hamburg", json);
        bkgMock.createOrtNachgelagertResponseBkgMock("rs:123456789* AND schluessel:123456789", jsonNachgelagert1);
        bkgMock.createOrtNachgelagertResponseBkgMock("rs:123456789* AND schluessel:123456789000", jsonNachgelagert1);
        Response response = LocationsConnector.getLocationsV3ByArs(ars, 10, List.of("plz"), List.of("ARS"));
        JsonPath jsonResponsePath = response.jsonPath();
        List<String> nameListe = jsonResponsePath.getList("name");
        assertThat(nameListe, hasItem("Hamburg 'Rönneburg'"));
    }

    /**
     * <p>Testet das Default Verhalten bei Anfragen gegen locations Endpunkt</p>
     */
    @Test
    void locationByQuery() throws IOException {
        String search = "Kiel Feldstraße 247";
        String json = IOUtils.resourceToString("/bkg/OrtKielFeldstrasse247.json", StandardCharsets.UTF_8);
        bkgMock.createLocationsResponseBkgMock(search, json);
        Response response = LocationsConnector.getLocationsV3ByQuery(search, 10, null);
        JsonPath jsonResponsePath = response.jsonPath();
        List<String> nameListe = jsonResponsePath.getList("name");
        assertThat(nameListe, hasItem("Kiel 'Wik'"));

        List<List<String>> koordinaten = jsonResponsePath.getList("koordinaten");
        assertNotNull(koordinaten);
        // Koordinaten sind im Format [Longitude, Latitude]
        assertThat(koordinaten.getFirst(), hasSize(2));
    }

    /*
        Suche nach Orten ohne Ortsteilsuche. Die ARS Ortsteile von Stadtstaaten werden auf eine ARS des jeweiligen Stadtstaates gemappt.
     */
    @Test
    void locationByQueryNoDistrict() throws IOException {
        String search = "Hamburg Rönneburg";
        String json = IOUtils.resourceToString("/bkg/OrtHamburgRoenneburg.json", StandardCharsets.UTF_8);
        bkgMock.createLocationsResponseBkgMock(search, json);
        Response response = LocationsConnector.getLocationsV3ByQuery(search, 10, false);
        JsonPath jsonResponsePath = response.jsonPath();

        List<String> nameListe = jsonResponsePath.getList("name");
        //erwartet 11 Einträge -> 10 aus bkg Response + ein default Eintrag für Stadtstaaten
        assertEquals(10, nameListe.size());
        //aus der injected List für Stadtstaaten
        assertThat(nameListe, hasItem("Hamburg, Freie und Hansestadt"));
        //aus bkg response
        assertThat(nameListe, hasItem("Hamburg 'Rönneburg'"));

        List<String> arsListe = jsonResponsePath.getList("ars");
        arsListe.forEach(ars -> assertThat(ars, is("020000000000")));

        List<List<String>> koordinaten = jsonResponsePath.getList("koordinaten");
        // Bei Ortsteilsuche ohne Ortsteil sollten auch Koordinaten vorhanden sein
        assertNotNull(koordinaten);
        assertThat(koordinaten.size(), is(10));
    }

    /**
     * <p>Testet das Verhalten bei Anfragen gegen locations Endpunkt mit PLZ Filter</p>
     */
    @Test
    void locationByQueryFilterPlz() throws IOException {
        String search = "Hamburg Rönneburg";
        String json = IOUtils.resourceToString("/bkg/OrtHamburgRoenneburgMissingPlz.json", StandardCharsets.UTF_8);
        bkgMock.createLocationsResponseBkgMock(search, json);
        Response response = LocationsConnector.getLocationsV3ByQuery(search, 10, List.of("plz"), null, true);
        JsonPath jsonResponsePath = response.jsonPath();

        List<String> nameListe = jsonResponsePath.getList("name");
        assertEquals(10, nameListe.size());
        //aus der injected List für Stadtstaaten
        assertThat(nameListe, hasItem("Hamburg, Freie und Hansestadt"));
        //aus bkg response
        assertThat(nameListe, not(hasItem("Hamburg 'Ohneplz'")));
    }

    /**
     * <p>Testet das Verhalten bei Anfragen gegen locations Endpunkt mit PLZ Filter ohne Ortsteilsuche</p>
     */
    @Test
    void locationByQueryFilterPlzNoDistrict() throws IOException {
        String search = "Hamburg Rönneburg";
        String json = IOUtils.resourceToString("/bkg/OrtHamburgRoenneburgMissingPlz.json", StandardCharsets.UTF_8);
        bkgMock.createLocationsResponseBkgMock(search, json);
        Response response = LocationsConnector.getLocationsV3ByQuery(search, 10, List.of("plz"), null, false);
        JsonPath jsonResponsePath = response.jsonPath();

        List<String> nameListe = jsonResponsePath.getList("name");
        assertEquals(10, nameListe.size());
        //aus der injected List für Stadtstaaten
        assertThat(nameListe, hasItem("Hamburg, Freie und Hansestadt"));
        //aus bkg response
        assertThat(nameListe, not(hasItem("Hamburg 'Ohneplz'")));
        List<String> arsListe = jsonResponsePath.getList("ars");
        arsListe.forEach(ars -> assertThat(ars, is("020000000000")));
    }

    /**
     * <p>Testet das Verhalten bei Anfragen gegen locations Endpunkt mit PLZ Gruppierung</p>
     */
    @Test
    void locationByQueryGroupByPlz() throws IOException {
        String search = "Hamburg Rönneburg";
        String json = IOUtils.resourceToString("/bkg/OrtHamburgRoenneburg.json", StandardCharsets.UTF_8);
        bkgMock.createLocationsResponseBkgMock(search, json);
        Response response = LocationsConnector.getLocationsV3ByQuery(search, 10, List.of(), List.of("PLZ"), true);
        JsonPath jsonResponsePath = response.jsonPath();

        List<String> nameListe = jsonResponsePath.getList("name");
        assertEquals(3, nameListe.size());
        //aus der injected List für Stadtstaaten
        assertThat(nameListe, hasItem("Hamburg, Freie und Hansestadt"));
    }

    /**
     * <p>Testet das Verhalten bei Anfragen gegen locations Endpunkt mit PLZ Gruppierung ohne Ortsteilsuche</p>
     */
    @Test
    void locationByQueryGroupByPlzNoDistrict() throws IOException {
        String search = "Hamburg Rönneburg";
        String json = IOUtils.resourceToString("/bkg/OrtHamburgRoenneburg.json", StandardCharsets.UTF_8);
        bkgMock.createLocationsResponseBkgMock(search, json);
        Response response = LocationsConnector.getLocationsV3ByQuery(search, 10, List.of(), List.of("PLZ"), false);
        JsonPath jsonResponsePath = response.jsonPath();

        List<String> nameListe = jsonResponsePath.getList("name");
        assertEquals(3, nameListe.size());
        //aus der injected List für Stadtstaaten
        assertThat(nameListe, hasItem("Hamburg, Freie und Hansestadt"));
        List<String> arsListe = jsonResponsePath.getList("ars");
        arsListe.forEach(ars -> assertThat(ars, is("020000000000")));
    }

    /**
     * <p>Validiert, dass verschiedene Suchergebnisse auch unterschiedliche Koordinaten bekommen.</p>
     */
    @Test
    void locationByQueryWithDifferentCoordinates() throws IOException {
        String search = "Hamburg Rönneburg";
        String json = IOUtils.resourceToString("/bkg/OrtHamburgRoenneburg.json", StandardCharsets.UTF_8);
        bkgMock.createLocationsResponseBkgMock(search, json);
        Response response = LocationsConnector.getLocationsV3ByQuery(search, 10, true);
        JsonPath jsonResponsePath = response.jsonPath();

        List<String> nameListe = jsonResponsePath.getList("name");
        List<List<String>> koordinaten = jsonResponsePath.getList("koordinaten");

        assertThat(response.getStatusCode(), is(HttpStatus.SC_OK));
        assertThat(nameListe, hasSize(10));
        assertThat(koordinaten, hasSize(10));

        // Prüfe, dass nicht alle Koordinaten gleich sind
        Set<List<String>> uniqueKoordinaten = new HashSet<>(koordinaten);
        assertTrue(uniqueKoordinaten.size() > 1, "Unterschiedliche Orte sollten Koordinaten haben");
    }

    /**
     * <p>Testet das Verhalten bei Anfragen gegen locations/reverse Endpunkt mit Breitengrad und Längengrad.</p>
     */
    @Test
    void getLocationsV3Reverse() throws IOException {
        String lon = "9.7323";
        String lat = "53.397";
        String json = IOUtils.resourceToString("/bkg/OrtHamburg.json", StandardCharsets.UTF_8);
        bkgMock.createLocationsReverseResponseBkgMock(lon, lat, json);
        Response response = LocationsConnector.getLocationsV3Reverse(lon, lat);
        JsonPath jsonResponsePath = response.jsonPath();

        assertThat(response.getStatusCode(), is(HttpStatus.SC_OK));

        // Extrahiere die Ergebnisse
        List<String> nameListe = jsonResponsePath.getList("name");
        List<List<String>> koordinaten = jsonResponsePath.getList("koordinaten");

        // Validiere, dass wir Ergebnisse bekommen
        assertThat(nameListe, not(empty()));

        // Validiere Koordinaten
        assertNotNull(koordinaten);
        assertThat(koordinaten, hasSize(nameListe.size()));
        koordinaten.forEach(coord -> assertThat(coord, hasSize(2)));
    }

    private <T> boolean noDuplicates(List<T> listToTest) {
        final var set = new HashSet<T>();
        return listToTest.stream().allMatch(set::add);
    }
}
