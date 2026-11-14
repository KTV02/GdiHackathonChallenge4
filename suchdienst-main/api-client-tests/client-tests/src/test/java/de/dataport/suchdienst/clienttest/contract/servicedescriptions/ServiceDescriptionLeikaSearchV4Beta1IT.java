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
package de.dataport.suchdienst.clienttest.contract.servicedescriptions;

import static de.dataport.suchdienst.clienttest.support.HttpContractAssertions.assertApiError;
import static de.dataport.suchdienst.clienttest.support.HttpContractAssertions.assertJsonSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.dataport.suchdienst.client.api.BetaLeistungenApi;
import de.dataport.suchdienst.clienttest.fixture.SearchFixture;
import de.dataport.suchdienst.clienttest.fixture.SearchFixtureExtension;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

/** Testziel: GET /v4beta1/servicedescriptions/leikaid liefert die gewünschte Leistung und weist kurze Schlüssel ab. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SearchFixtureExtension.class)
class ServiceDescriptionLeikaSearchV4Beta1IT {

    private SearchFixture fixture;
    private BetaLeistungenApi services;

    @BeforeAll
    void initializeApis(SearchFixture sharedFixture) {
        fixture = sharedFixture;
        services = new BetaLeistungenApi(fixture.client());
    }

    @Test
    void returnsDocumentedStatusAndJson() {
        assertJsonSuccess(
            services.findByLeikaIdWithHttpInfo(
                List.of(fixture.leika(0)),
                SearchFixture.ARS,
                List.of(fixture.query()),
                0,
                5,
                false
            )
        );
    }

    @Test
    void rejectsTooShortLeikaId() {
        assertApiError(400, () -> services.findByLeikaId(List.of("short"), SearchFixture.ARS, null, 0, 5, false));
    }

    @Test
    void returnsOnlyRequestedService() {
        var result = services.findByLeikaId(
            List.of(fixture.leika(0)),
            SearchFixture.ARS,
            List.of(fixture.query()),
            0,
            5,
            false
        );
        assertEquals(1L, result.getPage().getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(fixture.id(0), result.getContent().getFirst().getId());
        assertEquals(fixture.title(0), result.getContent().getFirst().getName());
    }
}
