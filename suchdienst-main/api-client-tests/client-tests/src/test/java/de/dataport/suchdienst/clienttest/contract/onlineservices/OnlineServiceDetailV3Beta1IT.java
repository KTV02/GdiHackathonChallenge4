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
package de.dataport.suchdienst.clienttest.contract.onlineservices;

import static de.dataport.suchdienst.clienttest.support.HttpContractAssertions.assertApiError;
import static de.dataport.suchdienst.clienttest.support.HttpContractAssertions.assertJsonSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.dataport.suchdienst.client.api.BetaOnlinediensteApi;
import de.dataport.suchdienst.clienttest.fixture.SearchFixture;
import de.dataport.suchdienst.clienttest.fixture.SearchFixtureExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

/** Testziel: GET /v3beta1/onlineservices/{odId}/detail bindet Fixture-Identität und Links und meldet unbekannte IDs. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SearchFixtureExtension.class)
class OnlineServiceDetailV3Beta1IT {

    private SearchFixture fixture;
    private BetaOnlinediensteApi onlineServices;

    @BeforeAll
    void initializeApis(SearchFixture sharedFixture) {
        fixture = sharedFixture;
        onlineServices = new BetaOnlinediensteApi(fixture.client());
    }

    @Test
    void returnsDocumentedStatusAndJson() {
        assertJsonSuccess(onlineServices.getOnlineServiceDetailWithHttpInfo(fixture.onlineServiceId()));
    }

    @Test
    void returnsNotFoundForUnknownId() {
        assertApiError(404, () -> onlineServices.getOnlineServiceDetail("missing." + fixture.query()));
    }

    @Test
    void bindsImportedIdentityAndLinks() {
        var onlineService = onlineServices.getOnlineServiceDetail(fixture.onlineServiceId());
        assertEquals(fixture.onlineServiceId(), onlineService.getId());
        assertEquals(SearchFixture.ONLINE_NAME, onlineService.getName());
        assertEquals("Definierter SDK-Onlinedienst.", onlineService.getDescription());
        assertEquals(1, onlineService.getLinks().size());
        assertEquals(SearchFixture.LINK_URI, onlineService.getLinks().getFirst().getUri());
        assertEquals("SDK-Antragslink", onlineService.getLinks().getFirst().getTitle());
    }
}
