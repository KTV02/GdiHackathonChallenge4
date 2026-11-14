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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.dataport.suchdienst.client.api.OnlinediensteApi;
import de.dataport.suchdienst.client.model.OdRelationOutModel;
import de.dataport.suchdienst.clienttest.fixture.SearchFixture;
import de.dataport.suchdienst.clienttest.fixture.SearchFixtureExtension;
import java.time.LocalDate;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Testziel: GET /v2/onlineservices/detail bindet definierte Links und Leistungsrelationen und meldet ungültige/unbekannte IDs. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SearchFixtureExtension.class)
class OnlineServiceDetailV2IT {

    private SearchFixture fixture;
    private OnlinediensteApi onlineServices;

    @BeforeAll
    void initializeApis(SearchFixture sharedFixture) {
        fixture = sharedFixture;
        onlineServices = new OnlinediensteApi(fixture.client());
    }

    @Test
    void bindsImportedIdentityLinksAndRelations() {
        var online = assertJsonSuccess(onlineServices.getDetailOdV2WithHttpInfo(fixture.onlineServiceId(), null));
        assertEquals(fixture.onlineServiceId(), online.getId());
        assertEquals(SearchFixture.ONLINE_NAME, online.getName());
        assertEquals("Definierter SDK-Onlinedienst.", online.getDescription());
        assertNotNull(online.getLinks());
        assertEquals(1, online.getLinks().size());
        assertEquals(SearchFixture.LINK_URI, online.getLinks().getFirst().getUri());
        assertEquals("SDK-Antragslink", online.getLinks().getFirst().getTitle());
        assertNotNull(online.getRelations());
        assertEquals(
            fixture.expectedIds(),
            online.getRelations().stream().map(OdRelationOutModel::getLbId).collect(Collectors.toSet())
        );
        online.getRelations().forEach(relation -> {
            assertEquals(SearchFixture.ARS, relation.getArs());
            assertEquals(fixture.onlineServiceId(), relation.getParentId());
            assertEquals(OdRelationOutModel.TypEnum.ONLINESERVICE, relation.getTyp());
        });
        assertNotNull(online.getValidityPeriods());
        assertEquals(1, online.getValidityPeriods().size());
        assertEquals(LocalDate.of(2000, 1, 1), online.getValidityPeriods().getFirst().getGte());
        assertEquals(LocalDate.of(2099, 12, 31), online.getValidityPeriods().getFirst().getLte());
    }

    @ParameterizedTest(name = "Gültiger Randtermin {0}")
    @ValueSource(strings = { "2000-01-01", "2099-12-31" })
    void includesOnlineServiceAtValidityBoundaries(String date) {
        assertEquals(
            fixture.onlineServiceId(),
            onlineServices.getDetailOdV2(fixture.onlineServiceId(), LocalDate.parse(date)).getId()
        );
    }

    @ParameterizedTest(name = "Außerhalb der Gültigkeit {0}")
    @ValueSource(strings = { "1999-12-31", "2100-01-01" })
    void returnsNotFoundOutsideValidityPeriod(String date) {
        assertApiError(404, () -> onlineServices.getDetailOdV2(fixture.onlineServiceId(), LocalDate.parse(date)));
    }

    @Test
    void returnsNotFoundForUnknownId() {
        assertApiError(404, () -> onlineServices.getDetailOdV2("missing." + fixture.query(), null));
    }

    @Test
    void rejectsTooShortId() {
        assertApiError(400, () -> onlineServices.getDetailOdV2("x", null));
    }

    @Test
    void rejectsMissingQueryOnServer() {
        // Rohes HTTP: die generierte Client-Methode würde null vor dem HTTP-Aufruf ablehnen.
        assertApiError(400, () ->
            fixture.client().getRestClient().get().uri("/v2/onlineservices/detail").retrieve().toBodilessEntity()
        );
    }
}
