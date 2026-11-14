/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.clienttest.fixture;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.github.tomakehurst.wiremock.WireMockServer;
import de.dataport.suchdienst.client.ApiClient;
import de.dataport.suchdienst.client.model.GeoArsOutModelV3;
import de.dataport.suchdienst.clienttest.support.StrictApiClientFactory;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ein BKG-Mock pro Operationsklasse; der Client spricht weiterhin mit dem laufenden Backend.
 * Zufällige Queries trennen die Klassen. Feste ARS-/Adressantworten sind bewusst identisch,
 * damit auch vom Backend gecachte Antworten zu den Erwartungen der nächsten Klasse passen.
 * Codelistdownloads oder globale Cache-Löschungen sind dafür nicht erforderlich.
 */
public final class LocationFixture implements AutoCloseable {

    public static final String ARS = "082220000000";
    public static final String CITY = "SDK-Ort";
    public static final String STREET = "SDK-Allee";
    public static final String ZIP = "68165";
    public static final String LONGITUDE = "8.484444";
    public static final String LATITUDE = "49.452778";

    private final WireMockServer server;
    private final ApiClient client;
    private final String query = "sdkort" + UUID.randomUUID().toString().replace("-", "");

    public LocationFixture() {
        // Das separat gestartete Backend braucht vorab eine feste Mock-URL; Ortsklassen dürfen nicht parallel laufen.
        int port = Integer.getInteger("suchdienst.geo-mock-port", 18082);
        server = new WireMockServer(wireMockConfig().bindAddress("127.0.0.1").port(port));
        server.start();
        try {
            var baseUrl = System.getProperty("suchdienst.base-url", "http://localhost:8080");
            client = StrictApiClientFactory.create(StrictApiClientFactory.builder().baseUrl(baseUrl), baseUrl);
            // Unbekannte, aber gültige Suchwerte liefern leer; die konkreten Erfolgs-Stubs haben Vorrang.
            for (var path : List.of("/geosearch", "/geocode")) {
                server.stubFor(
                    get(urlPathEqualTo(path))
                        .atPriority(10)
                        .willReturn(
                            aResponse().withHeader("Content-Type", "application/json").withBody("{\"features\":[]}")
                        )
                );
            }
            server.stubFor(
                get(urlPathEqualTo("/suggest"))
                    .atPriority(10)
                    .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("[]"))
            );
            var features = List.of(
                feature("town", null, null, null),
                feature("street", ZIP, STREET, null),
                feature("house", ZIP, STREET, "2a"),
                feature("other", "68167", "SDK-Weg", "7")
            );
            stubQuery(query, features);
            // Exakte ARS-Abfrage: die Hierarchie wird serverseitig aus dem vollständigen ARS abgeleitet.
            stubQuery("rs:08222* AND schluessel:" + ARS, features);
            var house = response(List.of(features.get(2)));
            server.stubFor(
                get(urlPathEqualTo("/geocode"))
                    .withQueryParam("ort", equalTo("SDK Ort"))
                    .withQueryParam("plz", equalTo(ZIP))
                    .withQueryParam(
                        "filter",
                        equalTo(
                            "typ:haus AND ort:(SDK Ort) AND plz:\"68165\" AND strasse:\"SDK Allee\" AND haus:\"2a\""
                        )
                    )
                    .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(house))
            );
            server.stubFor(
                get(urlPathEqualTo("/geosearch"))
                    .withQueryParam("lat", equalTo(LATITUDE))
                    .withQueryParam("lon", equalTo(LONGITUDE))
                    .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(house))
            );
            stubSuggestions(
                query,
                List.of(
                    Map.of("suggestion", "SDK-Ort", "score", 2.5),
                    Map.of("suggestion", "SDK-Ort Süd", "score", 1.75)
                )
            );
        } catch (RuntimeException | Error error) {
            server.stop();
            throw error;
        }
    }

    public ApiClient client() {
        return client;
    }

    public String query() {
        return query;
    }

    public void stubQuery(String input, List<Map<String, Object>> features) {
        server.stubFor(
            get(urlPathEqualTo("/geosearch"))
                .withQueryParam("query", equalTo(input))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(response(features)))
        );
        server.stubFor(
            get(urlPathEqualTo("/geosearch"))
                .atPriority(1)
                .withQueryParam("query", equalTo(input))
                .withQueryParam("count", equalTo("1"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(response(features.stream().limit(1).toList()))
                )
        );
    }

    public void stubSuggestions(String input, List<Map<String, Object>> values) {
        server.stubFor(
            get(urlPathEqualTo("/suggest"))
                .withQueryParam("query", equalTo(input))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(StrictApiClientFactory.mapper().writeValueAsString(values))
                )
        );
        server.stubFor(
            get(urlPathEqualTo("/suggest"))
                .atPriority(1)
                .withQueryParam("query", equalTo(input))
                .withQueryParam("count", equalTo("1"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(StrictApiClientFactory.mapper().writeValueAsString(values.stream().limit(1).toList()))
                )
        );
    }

    public void verifyQuery(String path, String input) {
        server.verify(getRequestedFor(urlPathEqualTo(path)).withQueryParam("query", equalTo(input)));
    }

    public static void assertTown(GeoArsOutModelV3 town) {
        assertEquals(ARS, town.getArs());
        assertEquals("08", town.getArsFederalstate());
        assertEquals(CITY, town.getName());
        assertEquals(CITY, town.getGemeinde());
        assertNull(town.getPlz());
        assertNull(town.getStrasse());
        assertNull(town.getHausnummer());
        // Generierte optionale Arrays dürfen beim DTO-Binding als leere Liste initialisiert sein.
        assertEquals(List.of(), town.getKoordinaten() == null ? List.of() : town.getKoordinaten());
    }

    public static void assertHouse(GeoArsOutModelV3 house) {
        assertEquals(ARS, house.getArs());
        assertEquals("08", house.getArsFederalstate());
        assertEquals(CITY, house.getName());
        assertEquals(CITY, house.getGemeinde());
        assertEquals(ZIP, house.getPlz());
        assertEquals(STREET, house.getStrasse());
        assertEquals("2a", house.getHausnummer());
        assertEquals(List.of(LONGITUDE, LATITUDE), house.getKoordinaten());
    }

    private static Map<String, Object> feature(String id, String zip, String street, String house) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("rs", ARS);
        properties.put("schluessel", ARS);
        properties.put("typ", house != null ? "Haus" : street != null ? "Strasse" : "Ort");
        properties.put("ort", CITY);
        properties.put("gemeinde", CITY);
        properties.put("score", 2.5);
        if (zip != null) properties.put("plz", zip);
        if (street != null) properties.put("strasse", street);
        if (house != null) properties.put("haus", house);
        Map<String, Object> feature = new LinkedHashMap<>();
        feature.put("id", id);
        feature.put("properties", properties);
        if (zip != null) feature.put("geometry", Map.of("type", "Point", "coordinates", List.of(LONGITUDE, LATITUDE)));
        return feature;
    }

    private static String response(List<Map<String, Object>> features) {
        return StrictApiClientFactory.mapper().writeValueAsString(Map.of("features", features));
    }

    @Override
    public void close() {
        server.stop();
    }
}
