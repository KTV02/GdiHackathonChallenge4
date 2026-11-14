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
import de.dataport.pvog.testdatengenerator.model.Anschrift;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ElasticsearchConnector;
import suchdienst.integrationstest.connectors.OrganisationunitsConnector;
import suchdienst.integrationstest.mocks.BKGMock;
import suchdienst.integrationstest.utils.ArtifactWriter;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.location.AddressValidator;

class GeokoordinatenAufloesenTest {

    final String srsName = "EPSG:4326";
    final String json1 = """
    {
        "type": "FeatureCollection",
        "features": [{
                "type": "Feature",
                "bbox": [
                    12.2880411148071, 51.2924118041992,
                    12.5057373046875, 51.4160003662109
                ],
                "geometry": {
                    "type": "Point",
                    "coordinates": [12.3751, 51.3408]
                },
                "properties": {
                    "text": "Leipzig",
                    "typ": "Ort",
                    "score": 0.5303301,
                    "treffer": "",
                    "bbox": {
                        "type": "Polygon",
                        "coordinates": [[
                                [12.288, 51.2924],
                                [12.288, 51.416],
                                [12.5057, 51.416],
                                [12.5057, 51.2924],
                                [12.288, 51.2924]
                            ]]
                    },
                    "ags": "14713000",
                    "rs": "147130000000",
                    "schluessel": "1471300000000000",
                    "bundesland": "Sachsen",
                    "kreis": "Leipzig",
                    "verwgem": "Leipzig",
                    "gemeinde": "Leipzig",
                    "ort": "Leipzig",
                    "ortsteil": ""
                },
                "id": "DEGAC00000081785"
            }
        ]
    }
    """;
    final String json2 = """
    {
        "type": "FeatureCollection",
        "features": [{
                "type": "Feature",
                "bbox": [
                    12.2880411148071, 51.2924118041992,
                    12.5057373046875, 51.4160003662109
                ],
                "geometry": {
                    "type": "Point",
                    "coordinates": [11.3751, 52.3408]
                },
                "properties": {
                    "text": "Leipzig",
                    "typ": "Ort",
                    "score": 0.5303301,
                    "treffer": "",
                    "bbox": {
                        "type": "Polygon",
                        "coordinates": [[
                                [12.288, 51.2924],
                                [12.288, 51.416],
                                [12.5057, 51.416],
                                [12.5057, 51.2924],
                                [12.288, 51.2924]
                            ]]
                    },
                    "ags": "14713000",
                    "rs": "147130000000",
                    "schluessel": "1471300000000000",
                    "bundesland": "Sachsen",
                    "kreis": "Leipzig",
                    "verwgem": "Leipzig",
                    "gemeinde": "Leipzig",
                    "ort": "Leipzig",
                    "ortsteil": ""
                },
                "id": "DEGAC00000081785"
            }
        ]
    }
    """;

    final String json3 = """
    {
        "type": "FeatureCollection",
        "features": [{
                "type": "Feature",
                "bbox": [
                    12.2880411148071, 51.2924118041992,
                    12.5057373046875, 51.4160003662109
                ],
                "geometry": {
                    "type": "Point",
                    "coordinates": [13.3751, 53.3408]
                },
                "properties": {
                    "text": "Leipzig",
                    "typ": "Ort",
                    "score": 0.5303301,
                    "treffer": "",
                    "bbox": {
                        "type": "Polygon",
                        "coordinates": [[
                                [12.288, 51.2924],
                                [12.288, 51.416],
                                [12.5057, 51.416],
                                [12.5057, 51.2924],
                                [12.288, 51.2924]
                            ]]
                    },
                    "ags": "14713000",
                    "rs": "147130000000",
                    "schluessel": "1471300000000000",
                    "bundesland": "Sachsen",
                    "kreis": "Leipzig",
                    "verwgem": "Leipzig",
                    "gemeinde": "Leipzig",
                    "ort": "Leipzig",
                    "ortsteil": ""
                },
                "id": "DEGAC00000081785"
            }
        ]
    }
    """;

    final String json4 = """
    {
        "type": "FeatureCollection",
        "features": [{
                "type": "Feature",
                "bbox": [
                    12.2880411148071, 51.2924118041992,
                    12.5057373046875, 51.4160003662109
                ],
                "geometry": {
                    "type": "Point",
                    "coordinates": [14.3751, 54.3408]
                },
                "properties": {
                    "text": "Leipzig",
                    "typ": "Ort",
                    "score": 0.5303301,
                    "treffer": "",
                    "bbox": {
                        "type": "Polygon",
                        "coordinates": [[
                                [12.288, 51.2924],
                                [12.288, 51.416],
                                [12.5057, 51.416],
                                [12.5057, 51.2924],
                                [12.288, 51.2924]
                            ]]
                    },
                    "ags": "14713000",
                    "rs": "147130000000",
                    "schluessel": "1471300000000000",
                    "bundesland": "Sachsen",
                    "kreis": "Leipzig",
                    "verwgem": "Leipzig",
                    "gemeinde": "Leipzig",
                    "ort": "Leipzig",
                    "ortsteil": ""
                },
                "id": "DEGAC00000081785"
            }
        ]
    }
    """;

    private static BKGMock bkgMock;

    @BeforeAll
    static void setup() {
        ElasticsearchConnector.removeAllData();
        bkgMock = new BKGMock();
    }

    @Test
    void bkgTestTwoOe(TestInfo testInfo) {
        //Setup
        String testcaseName = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcaseName).lb(1).oe(2).getMin();
        Organisationseinheit organisationseinheit1 = fileSetup.getOrganisationseinheiten().get(0);
        Organisationseinheit organisationseinheit2 = fileSetup.getOrganisationseinheiten().get(1);
        organisationseinheit1.getAnschriften().clear();
        organisationseinheit1
            .getAnschriften()
            .add(
                Anschrift.builder()
                    .typ("001")
                    .strasse("Bill")
                    .hausnummer("82")
                    .postleitzahl("20539")
                    .ort("Hamburg")
                    .build()
            );
        organisationseinheit1
            .getAnschriften()
            .add(
                Anschrift.builder()
                    .typ("001")
                    .strasse("Alsterufer")
                    .hausnummer("10")
                    .postleitzahl("22303")
                    .ort("Bonn")
                    .build()
            );

        organisationseinheit2.getAnschriften().clear();
        organisationseinheit2
            .getAnschriften()
            .add(
                Anschrift.builder()
                    .typ("001")
                    .strasse("Harvestehuder Weg")
                    .hausnummer("43")
                    .postleitzahl("20149")
                    .ort("Leipzig")
                    .build()
            );
        organisationseinheit2
            .getAnschriften()
            .add(
                Anschrift.builder()
                    .typ("001")
                    .strasse("Lohbrügger Weg")
                    .hausnummer("52")
                    .postleitzahl("20849")
                    .ort("Berlin")
                    .build()
            );

        // Mocken des BKG Service
        List<String> query1 = new ArrayList<>();
        query1.add(organisationseinheit1.getAnschriften().getFirst().getStrasse());
        query1.add(organisationseinheit1.getAnschriften().getFirst().getHausnummer());
        query1.add(organisationseinheit1.getAnschriften().getFirst().getPostleitzahl());
        query1.add(organisationseinheit1.getAnschriften().get(0).getOrt());

        List<String> query2 = new ArrayList<>();
        query2.add(organisationseinheit1.getAnschriften().get(1).getStrasse());
        query2.add(organisationseinheit1.getAnschriften().get(1).getHausnummer());
        query2.add(organisationseinheit1.getAnschriften().get(1).getPostleitzahl());
        query2.add(organisationseinheit1.getAnschriften().get(1).getOrt());

        List<String> query3 = new ArrayList<>();
        query3.add(organisationseinheit2.getAnschriften().getFirst().getStrasse());
        query3.add(organisationseinheit2.getAnschriften().getFirst().getHausnummer());
        query3.add(organisationseinheit2.getAnschriften().getFirst().getPostleitzahl());
        query3.add(organisationseinheit2.getAnschriften().get(0).getOrt());

        List<String> query4 = new ArrayList<>();
        query4.add(organisationseinheit2.getAnschriften().get(1).getStrasse());
        query4.add(organisationseinheit2.getAnschriften().get(1).getHausnummer());
        query4.add(organisationseinheit2.getAnschriften().get(1).getPostleitzahl());
        query4.add(organisationseinheit2.getAnschriften().get(1).getOrt());

        bkgMock.createResponseBkgMock(srsName, query1, json1);
        bkgMock.createResponseBkgMock(srsName, query2, json2);
        bkgMock.createResponseBkgMock(srsName, query3, json3);
        bkgMock.createResponseBkgMock(srsName, query4, json4);

        ImportUtil.pushAndWait23(fileSetup);

        // legt Testordner für Logs an. Damit ist sichtbar, dass dieser Test gelaufen ist.
        new ArtifactWriter(testcaseName);

        // Validierung Request
        bkgMock.verifyBkgMockResponse(srsName, query1);
        bkgMock.verifyBkgMockResponse(srsName, query2);
        bkgMock.verifyBkgMockResponse(srsName, query3);
        bkgMock.verifyBkgMockResponse(srsName, query4);

        Response response = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit1.getFullId());

        //Validierung API - OE1
        AddressValidator validator = new AddressValidator(response);
        validator
            .validateCode(0, "001")
            .validateCode(1, "001")
            .validateCity(0, "Hamburg")
            .validateCity(1, "Bonn")
            .validateHouseNumber(0, "82")
            .validateHouseNumber(1, "10")
            .validateType(0, "Hausanschrift")
            .validateType(1, "Hausanschrift")
            .validateZip(0, "20539")
            .validateZip(1, "22303")
            .validateStreet(0, "Bill")
            .validateStreet(1, "Alsterufer")
            .validateGeo(0, "12.3751", "51.3408", srsName)
            .validateGeo(1, "11.3751", "52.3408", srsName);

        Response response2 = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit2.getFullId());

        //Validierung API - OE2
        AddressValidator validator2 = new AddressValidator(response2);
        validator2
            .validateCode(0, "001")
            .validateCode(1, "001")
            .validateCity(0, "Leipzig")
            .validateCity(1, "Berlin")
            .validateHouseNumber(0, "43")
            .validateHouseNumber(1, "52")
            .validateType(0, "Hausanschrift")
            .validateType(1, "Hausanschrift")
            .validateZip(0, "20149")
            .validateZip(1, "20849")
            .validateStreet(0, "Harvestehuder Weg")
            .validateStreet(1, "Lohbrügger Weg")
            .validateGeo(0, "13.3751", "53.3408", srsName)
            .validateGeo(1, "14.3751", "54.3408", srsName);
    }
}
