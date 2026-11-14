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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dataport.suchdienst.client.api.BetaLeistungenApi;
import de.dataport.suchdienst.client.model.LifeSituationAggOutModel;
import de.dataport.suchdienst.client.model.ServiceDescriptionSearchHit;
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

/** Testziel: GET /v8beta1/servicedescriptions erfüllt HTTP-Vertrag, Pagination, Lebenslagenfilter und Defaults. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SearchFixtureExtension.class)
class ServiceDescriptionSearchV8Beta1IT {

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
            services.getServiceDescriptionsWithHttpInfo(
                SearchFixture.ARS,
                List.of(fixture.query()),
                null,
                false,
                false,
                0,
                5
            )
        );
    }

    @Test
    void rejectsMalformedArs() {
        assertApiError(400, () ->
            services.getServiceDescriptions("invalid", List.of(fixture.query()), null, false, false, 0, 5)
        );
    }

    @Test
    void paginatesWithoutDuplicateIdsAndBindsHitValues() {
        Set<String> ids = new HashSet<>();
        for (int page = 0; page < 2; page++) {
            var result = services
                .getServiceDescriptions(SearchFixture.ARS, List.of(fixture.query()), null, false, false, page, 5)
                .getServiceDescriptions();
            assertEquals(6L, result.getPage().getTotalElements());
            assertEquals(2L, result.getPage().getTotalPages());
            assertEquals(page, result.getPage().getNumber());
            assertEquals(page == 0 ? 5 : 1, result.getContent().size());
            for (var hit : result.getContent()) {
                assertTrue(ids.add(hit.getId()), "Doppelter Beta-Treffer über Seitengrenzen");
                assertEquals(fixture.title(fixture.indexOf(hit.getId())), hit.getName());
                assertEquals(SearchFixture.TEASER, hit.getTeaser());
                assertEquals(1, hit.getOnlineServiceLinks().size());
                assertEquals(SearchFixture.LINK_URI, hit.getOnlineServiceLinks().getFirst().getUri());
            }
        }
        assertEquals(fixture.expectedIds(), ids);
    }

    @Test
    void bindsLifeSituationLevelsAndFiltersByLeaf() {
        var response = services.getServiceDescriptions(
            SearchFixture.ARS,
            List.of(fixture.query()),
            Set.of("1180200"),
            true,
            false,
            0,
            10
        );
        assertEquals(
            fixture.expectedIds(),
            response
                .getServiceDescriptions()
                .getContent()
                .stream()
                .map(ServiceDescriptionSearchHit::getId)
                .collect(Collectors.toSet())
        );
        var root = lifeSituation(response.getFacades().getPersonalMatters(), "1000000");
        assertEquals("SDK Bürger", root.getName());
        assertEquals(6L, root.getCount());
        var middle = lifeSituation(root.getChildren(), "1180000");
        assertEquals("SDK Ruhestand", middle.getName());
        assertEquals(6L, middle.getCount());
        var leaf = lifeSituation(middle.getChildren(), "1180200");
        assertEquals("SDK Rente", leaf.getName());
        assertEquals(6L, leaf.getCount());
        var empty = services.getServiceDescriptions(
            SearchFixture.ARS,
            List.of(fixture.query()),
            Set.of("1180300"),
            false,
            false,
            0,
            10
        );
        assertEquals(0L, empty.getServiceDescriptions().getPage().getTotalElements());
        assertTrue(empty.getServiceDescriptions().getContent().isEmpty());
    }

    @Test
    void defaultsMatchExplicitDefaults() {
        var implicit = services.getServiceDescriptions(
            SearchFixture.ARS,
            List.of(fixture.query()),
            null,
            null,
            null,
            null,
            null
        );
        var explicit = services.getServiceDescriptions(
            SearchFixture.ARS,
            List.of(fixture.query()),
            null,
            false,
            true,
            0,
            10
        );
        assertEquals(explicit, implicit);
    }

    private static LifeSituationAggOutModel lifeSituation(Iterable<LifeSituationAggOutModel> nodes, String code) {
        assertNotNull(nodes);
        for (var node : nodes) {
            if (code.equals(node.getCode())) {
                return node;
            }
        }
        throw new AssertionError("Lebenslage fehlt: " + code);
    }
}
