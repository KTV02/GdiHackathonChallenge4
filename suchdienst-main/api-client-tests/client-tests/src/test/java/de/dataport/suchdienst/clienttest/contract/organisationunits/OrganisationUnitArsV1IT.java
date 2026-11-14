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
package de.dataport.suchdienst.clienttest.contract.organisationunits;

import static de.dataport.suchdienst.clienttest.support.HttpContractAssertions.assertApiError;
import static de.dataport.suchdienst.clienttest.support.HttpContractAssertions.assertJsonSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dataport.suchdienst.client.api.OrganisationseinheitenApi;
import de.dataport.suchdienst.clienttest.fixture.SearchFixture;
import de.dataport.suchdienst.clienttest.fixture.SearchFixtureExtension;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Testziel: GET /v1/organisationunits/{oeId}/ars liefert ausschließlich die importierte LeiKa-Zuständigkeit und validiert IDs. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SearchFixtureExtension.class)
class OrganisationUnitArsV1IT {

    private SearchFixture fixture;
    private OrganisationseinheitenApi organisations;

    @BeforeAll
    void initializeApis(SearchFixture sharedFixture) {
        fixture = sharedFixture;
        organisations = new OrganisationseinheitenApi(fixture.client());
    }

    @Test
    void returnsExactlyImportedArs() {
        var result = assertJsonSuccess(
            organisations.findArsByOeIdAndLeikaIdWithHttpInfo(fixture.organisationId(), fixture.leika(0))
        );
        assertEquals(List.of(SearchFixture.ARS), result);
    }

    @Test
    void returnsEmptyListForUnrelatedLeikaOrUnknownOrganisation() {
        assertTrue(
            organisations.findArsByOeIdAndLeikaId(fixture.organisationId(), SearchFixture.UNMATCHED_LEIKA).isEmpty()
        );
        assertTrue(organisations.findArsByOeIdAndLeikaId("missing." + fixture.query(), fixture.leika(0)).isEmpty());
    }

    @ParameterizedTest(name = "Ungültige LeiKa-Länge {0}")
    @ValueSource(ints = { 13, 22 })
    void rejectsInvalidLeikaLength(int length) {
        assertApiError(400, () -> organisations.findArsByOeIdAndLeikaId(fixture.organisationId(), "9".repeat(length)));
    }

    @ParameterizedTest(name = "Ungültige Organisations-ID-Länge {0}")
    @ValueSource(ints = { 1, 256 })
    void rejectsInvalidOrganisationIdLength(int length) {
        assertApiError(400, () -> organisations.findArsByOeIdAndLeikaId("x".repeat(length), fixture.leika(0)));
    }

    @Test
    void rejectsMissingLeikaQueryOnServer() {
        // Serverprüfung ohne lokale Nullprüfung der generierten API-Methode.
        assertApiError(400, () ->
            fixture
                .client()
                .getRestClient()
                .get()
                .uri("/v1/organisationunits/{oeId}/ars", fixture.organisationId())
                .retrieve()
                .toBodilessEntity()
        );
    }
}
