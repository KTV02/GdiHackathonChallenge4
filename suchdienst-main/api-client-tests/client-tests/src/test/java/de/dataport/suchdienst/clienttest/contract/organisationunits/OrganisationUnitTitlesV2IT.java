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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dataport.suchdienst.client.api.OrganisationseinheitenApi;
import de.dataport.suchdienst.clienttest.fixture.SearchFixture;
import de.dataport.suchdienst.clienttest.fixture.SearchFixtureExtension;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Testziel: GET /v2/organisationunits/titles bindet importierte Titel und Rollen für die passende Leistung und Region. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SearchFixtureExtension.class)
class OrganisationUnitTitlesV2IT {

    private SearchFixture fixture;
    private OrganisationseinheitenApi organisations;

    @BeforeAll
    void initializeApis(SearchFixture sharedFixture) {
        fixture = sharedFixture;
        organisations = new OrganisationseinheitenApi(fixture.client());
    }

    @Test
    void bindsImportedTitleAndCompetentOfficeRole() {
        var result = assertJsonSuccess(
            organisations.getOrganisationUnitTitlesV2WithHttpInfo(SearchFixture.ARS, fixture.id(0), null)
        );
        assertEquals(1, result.size());
        var organisation = result.getFirst();
        assertEquals(fixture.organisationId(), organisation.getId());
        assertEquals(SearchFixture.ORGANISATION_TITLE, organisation.getTitle());
        assertNotNull(organisation.getRole());
        assertEquals("02", organisation.getRole().getCode());
        assertNotNull(organisation.getCriteria());
    }

    @Test
    void excludesUnknownServiceAndUnrelatedRegion() {
        assertTrue(
            organisations.getOrganisationUnitTitlesV2(SearchFixture.ARS, "missing." + fixture.query(), null).isEmpty()
        );
        assertTrue(organisations.getOrganisationUnitTitlesV2("010000000000", fixture.id(0), null).isEmpty());
    }

    @Test
    void missingValidityDateMatchesCurrentDate() {
        assertEquals(
            organisations.getOrganisationUnitTitlesV2(SearchFixture.ARS, fixture.id(0), LocalDate.now()),
            organisations.getOrganisationUnitTitlesV2(SearchFixture.ARS, fixture.id(0), null)
        );
    }

    @ParameterizedTest(name = "Ungültiger ARS: {0}")
    @ValueSource(strings = { "invalid", "0956400000000" })
    void rejectsInvalidArs(String ars) {
        assertApiError(400, () -> organisations.getOrganisationUnitTitlesV2(ars, fixture.id(0), null));
    }

    @ParameterizedTest(name = "Fehlender Pflichtparameter: {0}")
    @ValueSource(strings = { "ars", "lbId" })
    void rejectsMissingRequiredQueryOnServer(String missing) {
        // Rohes HTTP umgeht die lokale Pflichtparameterprüfung des generierten Clients.
        assertApiError(400, () ->
            fixture
                .client()
                .getRestClient()
                .get()
                .uri(uri -> {
                    var request = uri.path("/v2/organisationunits/titles");
                    return missing.equals("ars")
                        ? request.queryParam("lbId", fixture.id(0)).build()
                        : request.queryParam("ars", SearchFixture.ARS).build();
                })
                .retrieve()
                .toBodilessEntity()
        );
    }
}
