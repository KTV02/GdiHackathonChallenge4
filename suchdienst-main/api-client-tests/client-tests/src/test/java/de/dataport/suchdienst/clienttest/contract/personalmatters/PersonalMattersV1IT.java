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
package de.dataport.suchdienst.clienttest.contract.personalmatters;

import static de.dataport.suchdienst.clienttest.support.HttpContractAssertions.assertApiError;
import static de.dataport.suchdienst.clienttest.support.HttpContractAssertions.assertJsonSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dataport.suchdienst.client.api.LeistungenApi;
import de.dataport.suchdienst.client.model.FullLifeSituationAggOutModelSearch;
import de.dataport.suchdienst.clienttest.fixture.SearchFixture;
import de.dataport.suchdienst.clienttest.fixture.SearchFixtureExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Testziel: GET /v1/personalmatters/{ars} bindet den dreistufigen Fixture-Lebenslagenbaum mit Namen und Zählwerten. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SearchFixtureExtension.class)
class PersonalMattersV1IT {

    private SearchFixture fixture;
    private LeistungenApi services;

    @BeforeAll
    void initializeApis(SearchFixture sharedFixture) {
        fixture = sharedFixture;
        services = new LeistungenApi(fixture.client());
    }

    @Test
    void returnsDocumentedStatusAndJson() {
        assertJsonSuccess(services.getPersonalMattersWithHttpInfo(SearchFixture.ARS));
    }

    @Test
    void bindsAllThreeFixtureLevels() {
        var root = find(services.getPersonalMatters(SearchFixture.ARS), "1000000");
        assertEquals("SDK Bürger", root.getName());
        var middle = find(root.getChildren(), "1180000");
        assertEquals("SDK Ruhestand", middle.getName());
        var leaf = find(middle.getChildren(), "1180200");
        assertEquals("SDK Rente", leaf.getName());
        // Dieser Pfad hat keinen Query-Filter: vorhandene fremde Leistungen können zusätzlich zählen.
        for (var node : new FullLifeSituationAggOutModelSearch[] { root, middle, leaf }) {
            assertNotNull(node.getCount());
            assertTrue(node.getCount() >= SearchFixture.COUNT, "Die sechs Fixture-Leistungen müssen enthalten sein");
        }
    }

    @ParameterizedTest(name = "Ungültiger ARS: {0}")
    @ValueSource(strings = { "invalid", "0956400000000" })
    void rejectsInvalidArs(String ars) {
        assertApiError(400, () -> services.getPersonalMatters(ars));
    }

    private static FullLifeSituationAggOutModelSearch find(
        Iterable<FullLifeSituationAggOutModelSearch> nodes,
        String code
    ) {
        assertNotNull(nodes);
        for (var node : nodes) {
            if (code.equals(node.getCode())) {
                return node;
            }
        }
        throw new AssertionError("Lebenslage fehlt: " + code);
    }
}
