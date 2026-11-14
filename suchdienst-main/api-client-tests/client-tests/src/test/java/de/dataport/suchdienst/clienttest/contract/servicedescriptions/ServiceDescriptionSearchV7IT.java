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
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dataport.suchdienst.client.api.LeistungenApi;
import de.dataport.suchdienst.client.model.ResultPageSdOutModelV7Search;
import de.dataport.suchdienst.client.model.SdOutModelV7Search;
import de.dataport.suchdienst.clienttest.fixture.SearchFixture;
import de.dataport.suchdienst.clienttest.fixture.SearchFixtureExtension;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

/** Testziel: GET /v7/servicedescriptions/{ars} bindet Fixture-Werte, Seiten und rekursive Lebenslagen korrekt. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SearchFixtureExtension.class)
class ServiceDescriptionSearchV7IT {

    private SearchFixture fixture;
    private LeistungenApi services;

    @BeforeAll
    void initializeApis(SearchFixture sharedFixture) {
        fixture = sharedFixture;
        services = new LeistungenApi(fixture.client());
    }

    @Test
    void paginatesWithoutDuplicateIdsAndBindsHitValues() {
        Set<String> ids = new HashSet<>();
        for (int page = 0; page < 2; page++) {
            var result = page(page);
            assertEquals(6L, result.getTotalElements());
            assertEquals(2, result.getTotalPages());
            assertEquals(page == 0 ? 5 : 1, result.getContent().size());
            for (var hit : result.getContent()) {
                assertTrue(ids.add(hit.getId()), "Doppelter Treffer über Seitengrenzen");
                int index = fixture.indexOf(hit.getId());
                assertEquals(fixture.title(index), hit.getName());
                assertEquals(SearchFixture.DESCRIPTION, hit.getDescription());
                assertEquals(SearchFixture.TEASER, hit.getTeaser());
                assertEquals(Set.of(fixture.leika(index)), hit.getLeikakeys());
                assertTrue(hit.getArs().contains(SearchFixture.ARS));
                assertEquals(1, hit.getOnlineServiceLinks().size());
                var link = hit.getOnlineServiceLinks().getFirst();
                assertEquals(fixture.onlineServiceId(), link.getId());
                assertEquals(SearchFixture.LINK_URI, link.getUri());
            }
        }
        assertEquals(fixture.expectedIds(), ids);
    }

    @Test
    void returnsEmptyPageForUnmatchedQuery() {
        var result = services.getSearchOutModelV7(
            SearchFixture.ARS,
            null,
            false,
            List.of(fixture.query() + "nichtvorhanden"),
            0,
            5,
            false,
            null,
            null,
            false,
            false
        );
        assertEquals(0L, result.getServiceDescriptions().getTotalElements());
        assertTrue(result.getServiceDescriptions().getContent().isEmpty());
    }

    @Test
    void bindsRecursiveLifeSituationFacades() {
        var response = services.getSearchOutModelV7(
            SearchFixture.ARS,
            Set.of("1180200"),
            true,
            List.of(fixture.query()),
            0,
            10,
            false,
            null,
            null,
            false,
            false
        );
        assertEquals(
            fixture.expectedIds(),
            response
                .getServiceDescriptions()
                .getContent()
                .stream()
                .map(SdOutModelV7Search::getId)
                .collect(Collectors.toSet())
        );
        var root = response
            .getFacades()
            .getPersonalMatters()
            .stream()
            .filter(node -> "1000000".equals(node.getCode()))
            .findFirst()
            .orElseThrow();
        assertEquals("SDK Bürger", root.getName());
        assertEquals(6L, root.getCount());
        var middle = root
            .getChildren()
            .stream()
            .filter(node -> "1180000".equals(node.getCode()))
            .findFirst()
            .orElseThrow();
        assertEquals("SDK Ruhestand", middle.getName());
        assertEquals(6L, middle.getCount());
        var leaf = middle
            .getChildren()
            .stream()
            .filter(node -> "1180200".equals(node.getCode()))
            .findFirst()
            .orElseThrow();
        assertEquals("SDK Rente", leaf.getName());
        assertEquals(6L, leaf.getCount());
    }

    private ResultPageSdOutModelV7Search page(int page) {
        var response = services.getSearchOutModelV7WithHttpInfo(
            SearchFixture.ARS,
            null,
            false,
            List.of(fixture.query()),
            page,
            5,
            false,
            null,
            null,
            false,
            false
        );
        assertJsonSuccess(response);
        return response.getBody().getServiceDescriptions();
    }
}
