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

import static de.dataport.suchdienst.clienttest.support.HttpContractAssertions.assertJsonSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dataport.suchdienst.client.api.LeistungenApi;
import de.dataport.suchdienst.client.model.AbstractServiceModule;
import de.dataport.suchdienst.client.model.ServiceModule;
import de.dataport.suchdienst.client.model.ServiceModuleDetail;
import de.dataport.suchdienst.clienttest.fixture.SearchFixture;
import de.dataport.suchdienst.clienttest.fixture.SearchFixtureExtension;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

/** Testziel: GET /v6/servicedescriptions/{lbid}/detail bindet Identität, Organisation und konkrete Text-/Kostenmodule. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SearchFixtureExtension.class)
class ServiceDescriptionDetailV6IT {

    private SearchFixture fixture;
    private LeistungenApi services;

    @BeforeAll
    void initializeApis(SearchFixture sharedFixture) {
        fixture = sharedFixture;
        services = new LeistungenApi(fixture.client());
    }

    @Test
    void bindsImportedIdentityAndPolymorphicTextAndCostModules() {
        var response = services.getSdOutModelV6WithHttpInfo(fixture.id(0), SearchFixture.ARS, false);
        assertJsonSuccess(response);
        var detail = response.getBody();
        assertEquals(fixture.id(0), detail.getId());
        assertEquals(fixture.title(0), detail.getName());
        assertEquals(Set.of(fixture.leika(0)), detail.getLeikakeys());
        assertTrue(
            detail
                .getOrganisations()
                .stream()
                .anyMatch(
                    organisation ->
                        fixture.organisationId().equals(organisation.getId()) &&
                        SearchFixture.ORGANISATION_TITLE.equals(organisation.getTitle())
                )
        );
        var modules = detail
            .getDetails()
            .stream()
            .map(module -> assertInstanceOf(AbstractServiceModule.class, module))
            .toList();
        var fullText = modules
            .stream()
            .filter(module -> SearchFixture.DESCRIPTION.equals(module.getText()))
            .findFirst()
            .orElseThrow();
        assertInstanceOf(ServiceModule.class, fullText);
        assertEquals("_module", fullText.getAtType());
        assertEquals("06", fullText.getCode());
        var fee = modules
            .stream()
            .filter(module -> SearchFixture.FEE_DESCRIPTION.equals(module.getText()))
            .findFirst()
            .orElseThrow();
        var structuredFee = assertInstanceOf(ServiceModuleDetail.class, fee);
        assertEquals("_moduleDetail", structuredFee.getAtType());
        assertNotNull(structuredFee.getDetails());
        assertEquals(1, structuredFee.getDetails().size());
        assertTrue(structuredFee.getDetails().getFirst().getTitle().endsWith("42,50 Euro"));
    }
}
