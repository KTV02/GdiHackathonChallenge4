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
package suchdienst.integrationstest.mocks;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.List;

public class BKGMock {

    private static final WireMockServer wireMockServer = new WireMockServer(wireMockConfig().port(8002));
    private static final String ACCEPT_HEADER =
        "application/json, application/yaml, application/xml, text/xml, application/*+json, application/*+xml";

    static {
        wireMockServer.start();
        // Beende WireMock bei Programm Ende
        Runtime.getRuntime().addShutdownHook(new Thread(wireMockServer::stop));
    }

    public void createLocationsResponseBkgMock(String query, String json) {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/geosearch"))
                .withHeader("Accept", equalTo(ACCEPT_HEADER))
                .withQueryParam("query", equalTo(query))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(json))
        );
    }

    public void createLocationsReverseResponseBkgMock(String lon, String lat, String json) {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/geosearch"))
                .withHeader("Accept", equalTo(ACCEPT_HEADER))
                .withQueryParam("lon", equalTo(lon))
                .withQueryParam("lat", equalTo(lat))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(json))
        );
    }

    public void createSuggestionResponseBkgMock(String query, String json) {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/suggest"))
                .withHeader("Accept", equalTo(ACCEPT_HEADER))
                .withQueryParam("query", equalTo(query))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(json))
        );
    }

    public void createGeocodeResponseBkgMock(String ort, String json) {
        var stub = get(urlPathEqualTo("/geocode")).withHeader("Accept", equalTo(ACCEPT_HEADER));
        if (ort != null) {
            stub = stub.withQueryParam("filter", equalTo("typ:ort AND ort:(" + ort + ")"));
        }
        wireMockServer.stubFor(
            stub.willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(json))
        );
    }

    public void createGeocodeOrtStrasseHausnrResponseBkgMock(
        String ort,
        String strasse,
        String hausnummer,
        String json
    ) {
        String filter =
            "typ:haus AND ort:(" + ort + ") AND strasse:\"" + strasse + "\" AND haus:\"" + hausnummer + "\"";
        wireMockServer.stubFor(
            get(urlPathEqualTo("/geocode"))
                .withHeader("Accept", equalTo(ACCEPT_HEADER))
                .withQueryParam("filter", equalTo(filter))
                .withQueryParam("ort", equalTo(ort))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(json))
        );
    }

    public void createOrtNachgelagertResponseBkgMock(String query, String json) {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/geosearch"))
                .withHeader("Accept", equalTo(ACCEPT_HEADER))
                .withQueryParam("query", equalTo(query))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(json))
        );
    }

    public void createOrtNachgelagertTimeOutResponseBkgMock(String query) {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/geosearch"))
                .withHeader("Accept", equalTo(ACCEPT_HEADER))
                .withQueryParam("query", equalTo(query))
                .willReturn(aResponse().withStatus(400).withFixedDelay(50000))
        );
    }

    public void createOrtNachgelagertErrorResponseBkgMock(String query) {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/geosearch"))
                .withHeader("Accept", equalTo(ACCEPT_HEADER))
                .withQueryParam("query", equalTo(query))
                .willReturn(aResponse().withStatus(403))
        );
    }

    public void createSuggestionErrorResponseBkgMock(String query, int errorCode) {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/suggest"))
                .withHeader("Accept", equalTo(ACCEPT_HEADER))
                .withQueryParam("query", equalTo(query))
                .willReturn(aResponse().withStatus(errorCode))
        );
    }

    public void createSuggestionTimeoutResponseBkgMock(String query, int errorCode) {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/suggest"))
                .withHeader("Accept", equalTo(ACCEPT_HEADER))
                .withQueryParam("query", equalTo(query))
                .willReturn(aResponse().withStatus(errorCode).withFixedDelay(50000))
        );
    }

    public void verifySuggestionBkgMockResponse(String query, int expectedCalls) {
        wireMockServer.verify(
            expectedCalls,
            getRequestedFor(urlPathEqualTo("/suggest"))
                .withHeader("Accept", equalTo(ACCEPT_HEADER))
                .withQueryParam("query", equalTo(query))
        );
    }

    public void verifyOrtBkgMockResponse(String query, int expectedCalls) {
        wireMockServer.verify(
            expectedCalls,
            getRequestedFor(urlPathEqualTo("/geocode"))
                .withHeader("Accept", equalTo(ACCEPT_HEADER))
                .withQueryParam("filter", equalTo("typ:ort AND ort:(" + query + ")"))
        );
    }

    public void verifyRSNachgelagertBkgMockResponse(String query, int expectedCalls) {
        wireMockServer.verify(
            expectedCalls,
            getRequestedFor(urlPathEqualTo("/geosearch"))
                .withHeader("Accept", equalTo(ACCEPT_HEADER))
                .withQueryParam("query", equalTo(query))
        );
    }

    public void createResponseBkgMock(String srsName, List<String> query, String json) {
        String strasse = query.get(0);
        String hausnummer = query.get(1);
        String plz = query.get(2);
        String ort = query.get(3);
        String queryParameters =
            "strasse:(" + strasse + ") " + "haus:(" + hausnummer + ") " + "plz:(" + plz + ") " + "ort:(" + ort + ")";
        wireMockServer.stubFor(
            get(urlPathEqualTo("/geosearch"))
                .withHeader("Accept", equalTo(ACCEPT_HEADER))
                .withQueryParam("srsName", equalTo(srsName))
                .withQueryParam("query", equalTo(queryParameters))
                .withQueryParam("outputformat", equalTo("json"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(json))
        );
    }

    public void verifyBkgMockResponse(String srsName, List<String> query) {
        String strasse = query.get(0);
        String hausnummer = query.get(1);
        String plz = query.get(2);
        String ort = query.get(3);
        String queryParameters =
            "strasse:(" + strasse + ") " + "haus:(" + hausnummer + ") " + "plz:(" + plz + ") " + "ort:(" + ort + ")";
        wireMockServer.verify(
            getRequestedFor(urlPathEqualTo("/geosearch"))
                .withQueryParam("srsName", equalTo(srsName))
                .withQueryParam("query", equalTo(queryParameters))
                .withQueryParam("outputformat", equalTo("json"))
        );
    }

    public void reset() {
        wireMockServer.resetAll();
    }
}
