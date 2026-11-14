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

import static de.dataport.suchdienst.clienttest.support.HttpContractAssertions.assertJsonSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.dataport.suchdienst.client.api.OrganisationseinheitenApi;
import de.dataport.suchdienst.clienttest.fixture.SearchFixture;
import de.dataport.suchdienst.clienttest.fixture.SearchFixtureExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

/** Testziel: GET /v5/organisationunits/detail bindet importierte Identität, Titel und Beschreibung. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SearchFixtureExtension.class)
class OrganisationUnitDetailV5IT {

    private SearchFixture fixture;
    private OrganisationseinheitenApi organisations;

    @BeforeAll
    void initializeApis(SearchFixture sharedFixture) {
        fixture = sharedFixture;
        organisations = new OrganisationseinheitenApi(fixture.client());
    }

    @Test
    void bindsImportedIdentityTitleAndDescription() {
        var response = organisations.getDetailOeOutModelV5WithHttpInfo(
            fixture.organisationId(),
            SearchFixture.ARS,
            fixture.id(0),
            null
        );
        assertJsonSuccess(response);
        var organisation = response.getBody();
        assertEquals(fixture.organisationId(), organisation.getId());
        assertEquals(SearchFixture.ORGANISATION_TITLE, organisation.getTitle());
        assertEquals("Definierte Beschreibung der SDK-Testbehörde.", organisation.getDescription());
    }
}
