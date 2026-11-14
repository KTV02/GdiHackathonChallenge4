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
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CodelistMock {

    private static final WireMockServer wireMockServer = new WireMockServer(wireMockConfig().port(8003));

    static {
        wireMockServer.start();

        // Beende WireMock bei Programm Ende
        Runtime.getRuntime().addShutdownHook(new Thread(wireMockServer::stop));
    }

    public void createCodelistVersionResponse() {
        wireMockServer.stubFor(
            get(urlMatching(".*/gueltigeVersion")).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"version\": \"abc:def\"}")
            )
        );
    }

    public void createBevoelkerungsstatistik() throws Exception {
        String content = Files.readString(
            Paths.get(ClassLoader.getSystemResource("bevoelkerungsstatistik.xml").toURI())
        );
        wireMockServer.stubFor(
            get(urlMatching(".*/genericode")).willReturn(
                aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(content)
            )
        );
    }

    public void createLeistungstextmodulleikaEntryResponse() throws Exception {
        String content = Files.readString(
            Paths.get(
                ClassLoader.getSystemResource("urn__de__xzufi__codeliste__leistungstextmodulleika_20201201.xml").toURI()
            )
        );
        wireMockServer.stubFor(
            get(urlMatching(".*leistungstextmodulleika_20201201/genericode")).willReturn(
                aResponse().withStatus(200).withHeader("Content-Type", "application/xml").withBody(content)
            )
        );
    }

    public void createUnknownCodelistEntryResponse() {
        wireMockServer.stubFor(
            get(urlMatching(".*/unknownuri_20201201/genericode")).willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\": \"Not Found\"}")
            )
        );
    }

    public void simulateXRepositoryOutage() {
        wireMockServer.stubFor(
            get(urlMatching(".*/gueltigeVersion")).willReturn(
                aResponse()
                    .withStatus(503)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\": \"Service Unavailable\"}")
            )
        );
        wireMockServer.stubFor(
            get(urlMatching(".*/genericode")).willReturn(
                aResponse()
                    .withStatus(503)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\": \"Service Unavailable\"}")
            )
        );
    }

    public void verifyVersionEndpointCalled() {
        wireMockServer.verify(moreThanOrExactly(1), getRequestedFor(urlMatching(".*/gueltigeVersion")));
    }

    public void verifyGenericodeEndpointCalled() {
        wireMockServer.verify(moreThanOrExactly(1), getRequestedFor(urlMatching(".*/genericode")));
    }

    public int getVersionRequestCount() {
        return wireMockServer
            .countRequestsMatching(getRequestedFor(urlMatching(".*/gueltigeVersion")).build())
            .getCount();
    }

    public int getGenericodeRequestCount() {
        return wireMockServer.countRequestsMatching(getRequestedFor(urlMatching(".*/genericode")).build()).getCount();
    }

    public int getTotalRequestCount() {
        return getVersionRequestCount() + getGenericodeRequestCount();
    }

    public void reset() {
        wireMockServer.resetAll();
    }
}
