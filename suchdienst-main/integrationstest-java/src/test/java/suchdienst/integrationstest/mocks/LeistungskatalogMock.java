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
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;

public class LeistungskatalogMock {

    private static final WireMockServer wireMockServer = new WireMockServer(wireMockConfig().port(8001));

    public LeistungskatalogMock() {
        wireMockServer.start();
        // Beende WireMock bei Programm Ende
        Runtime.getRuntime().addShutdownHook(new Thread(wireMockServer::stop));
    }

    public void registerMock(String contentToDeliver, int status) {
        wireMockServer.stubFor(
            get(urlEqualTo("/leistungskatalog")).willReturn(aResponse().withStatus(status).withBody(contentToDeliver))
        );
    }

    public void clear() {
        wireMockServer.resetAll();
    }

    public void stop() {
        wireMockServer.stop();
    }
}
