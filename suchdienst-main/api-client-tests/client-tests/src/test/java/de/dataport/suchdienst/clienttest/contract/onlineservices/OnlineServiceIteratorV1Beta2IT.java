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
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dataport.suchdienst.client.api.BetaOnlinediensteApi;
import de.dataport.suchdienst.client.model.ServiceDescriptionInfo;
import de.dataport.suchdienst.clienttest.fixture.SearchFixture;
import de.dataport.suchdienst.clienttest.fixture.SearchFixtureExtension;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

/** Testziel: GET /v1beta2/onlineservices bindet Leistungsrelationen, Endtoken und Filter ohne fremde Treffer. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SearchFixtureExtension.class)
class OnlineServiceIteratorV1Beta2IT {

    private SearchFixture fixture;
    private BetaOnlinediensteApi onlineServices;

    @BeforeAll
    void initializeApis(SearchFixture sharedFixture) {
        fixture = sharedFixture;
        onlineServices = new BetaOnlinediensteApi(fixture.client());
    }

    @Test
    void returnsDocumentedStatusAndJson() {
        assertJsonSuccess(
            onlineServices.getOnlineServiceWithRelationsWithHttpInfo(SearchFixture.ARS, List.of(fixture.leika(0)), null)
        );
    }

    @Test
    void rejectsFederalArsAsIteratorRegion() {
        assertApiError(400, () -> onlineServices.getOnlineServiceWithRelations("00", null, null));
    }

    @Test
    void bindsServiceRelationsAndEndToken() {
        var response = onlineServices.getOnlineServiceWithRelations(SearchFixture.ARS, List.of(fixture.leika(0)), null);
        assertEquals(1L, response.getHits());
        assertEquals(1L, response.getTotalHits());
        assertEquals("", response.getNextPageToken());
        assertEquals(1, response.getOnlineservices().size());
        var online = response.getOnlineservices().getFirst();
        assertEquals(fixture.onlineServiceId(), online.getId());
        assertEquals(SearchFixture.ONLINE_NAME, online.getName());
        assertEquals("Definierter SDK-Onlinedienst.", online.getDescription());
        assertEquals(1, online.getLinks().size());
        assertEquals(SearchFixture.LINK_URI, online.getLinks().getFirst().getUri());
        assertEquals(
            fixture.expectedIds(),
            online.getServiceDescriptions().stream().map(ServiceDescriptionInfo::getLbId).collect(Collectors.toSet())
        );
        online.getServiceDescriptions().forEach(relation -> {
            assertEquals(Set.of(SearchFixture.ARS), relation.getArs());
            assertEquals(Set.of(fixture.leika(fixture.indexOf(relation.getLbId()))), relation.getLeikakeys());
        });
    }

    @Test
    void excludesOtherLeikasAndRegions() {
        var wrongLeika = onlineServices.getOnlineServiceWithRelations(
            SearchFixture.ARS,
            List.of(SearchFixture.UNMATCHED_LEIKA),
            null
        );
        var wrongRegion = onlineServices.getOnlineServiceWithRelations("01", List.of(fixture.leika(0)), null);
        for (var response : List.of(wrongLeika, wrongRegion)) {
            assertEquals(0L, response.getHits());
            assertEquals(0L, response.getTotalHits());
            assertEquals("", response.getNextPageToken());
            assertTrue(response.getOnlineservices().isEmpty());
        }
    }
}
