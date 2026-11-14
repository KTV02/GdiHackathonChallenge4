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
import de.dataport.suchdienst.client.model.OrganisationRoleResult;
import de.dataport.suchdienst.clienttest.fixture.SearchFixture;
import de.dataport.suchdienst.clienttest.fixture.SearchFixtureExtension;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

/** Testziel: GET /v1beta2/onlineservices/{odId}/organisations bindet Zuständigkeitsrollen mit Leistungs- und Gebietsschlüsseln. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SearchFixtureExtension.class)
class OnlineServiceOrganisationsV1Beta2IT {

    private SearchFixture fixture;
    private BetaOnlinediensteApi onlineServices;

    @BeforeAll
    void initializeApis(SearchFixture sharedFixture) {
        fixture = sharedFixture;
        onlineServices = new BetaOnlinediensteApi(fixture.client());
    }

    @Test
    void returnsDocumentedStatusAndJson() {
        assertJsonSuccess(onlineServices.getOrganisationsByOnlineServiceWithHttpInfo(fixture.onlineServiceId()));
    }

    @Test
    void returnsNotFoundForUnknownId() {
        assertApiError(404, () -> onlineServices.getOrganisationsByOnlineService("missing." + fixture.query()));
    }

    @Test
    void bindsCompetentOfficeRoles() {
        var response = onlineServices.getOrganisationsByOnlineService(fixture.onlineServiceId());
        assertEquals(1, response.getOrganisations().size());
        var organisation = response.getOrganisations().getFirst();
        assertEquals(fixture.organisationId(), organisation.getOeId());
        assertEquals(SearchFixture.ORGANISATION_TITLE, organisation.getName());
        var roles = organisation
            .getRoles()
            .stream()
            .filter(role -> role.getRole() == OrganisationRoleResult.RoleEnum.COMPETENT_OFFICE)
            .toList();
        assertEquals(SearchFixture.COUNT, roles.size());
        assertEquals(
            fixture.expectedIds(),
            roles.stream().map(OrganisationRoleResult::getLbId).collect(Collectors.toSet())
        );
        roles.forEach(role -> {
            assertEquals(Set.of(SearchFixture.ARS), role.getArs());
            assertEquals(Set.of(fixture.leika(fixture.indexOf(role.getLbId()))), role.getLeikakeys());
        });
    }
}
