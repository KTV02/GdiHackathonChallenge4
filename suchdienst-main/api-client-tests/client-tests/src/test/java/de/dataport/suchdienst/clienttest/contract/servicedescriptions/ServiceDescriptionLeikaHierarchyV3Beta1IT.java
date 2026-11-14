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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import de.dataport.suchdienst.client.api.BetaLeistungenApi;
import de.dataport.suchdienst.client.model.LeikaMatch;
import de.dataport.suchdienst.client.model.ServiceDescriptionSearchHit;
import de.dataport.suchdienst.clienttest.fixture.SearchFixture;
import de.dataport.suchdienst.clienttest.fixture.SearchFixtureExtension;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Testziel: GET /v3beta1/servicedescriptions/leikahierarchy meldet Kaskadenstufe und effektive Schlüssel stabil über Seiten. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SearchFixtureExtension.class)
class ServiceDescriptionLeikaHierarchyV3Beta1IT {

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
            services.findByLeikaHierarchyWithHttpInfo(List.of(fixture.leika(0)), SearchFixture.ARS, 0, 5)
        );
    }

    @Test
    void rejectsMixedHierarchyLevels() {
        assertApiError(400, () ->
            services.findByLeikaHierarchy(List.of(fixture.leika(0), fixture.leika(1)), SearchFixture.ARS, 0, 5)
        );
    }

    @ParameterizedTest(name = "LeiKa-Kaskade: {1}")
    @MethodSource("hierarchyLevels")
    void reportsMatchingLevelAndEffectiveKeys(
        String requested,
        LeikaMatch.MatchTypeEnum match,
        Set<String> ids,
        Set<String> effectiveKeys
    ) {
        var response = services.findByLeikaHierarchy(List.of(requested), SearchFixture.ARS, 0, 10);
        assertEquals(match, response.getLeikaMatch().getMatchType());
        assertEquals(effectiveKeys, response.getLeikaMatch().getEffectiveLeikaIds());
        assertEquals(ids.size(), response.getServiceDescriptions().getPage().getTotalElements());
        assertEquals(
            ids,
            response
                .getServiceDescriptions()
                .getContent()
                .stream()
                .map(ServiceDescriptionSearchHit::getId)
                .collect(Collectors.toSet())
        );
        response
            .getServiceDescriptions()
            .getContent()
            .forEach(hit -> assertEquals(fixture.title(fixture.indexOf(hit.getId())), hit.getName()));
    }

    Stream<Arguments> hierarchyLevels() {
        return Stream.of(
            arguments(
                fixture.leika(1),
                LeikaMatch.MatchTypeEnum.EXACT,
                Set.of(fixture.id(1)),
                Set.of(fixture.leika(1))
            ),
            arguments(
                "99099999000000",
                LeikaMatch.MatchTypeEnum.DESCENDANT,
                fixture.expectedIds(),
                Set.of("99099999000000")
            ),
            arguments(
                "99099999223999",
                LeikaMatch.MatchTypeEnum.ANCESTOR,
                Set.of(fixture.id(0)),
                Set.of(fixture.leika(0))
            ),
            arguments(SearchFixture.UNMATCHED_LEIKA, LeikaMatch.MatchTypeEnum.NONE, Set.of(), Set.of())
        );
    }

    @Test
    void keepsExactMatchAfterResultEnd() {
        var response = services.findByLeikaHierarchy(List.of(fixture.leika(1)), SearchFixture.ARS, 2, 5);
        assertEquals(1L, response.getServiceDescriptions().getPage().getTotalElements());
        assertTrue(response.getServiceDescriptions().getContent().isEmpty());
        assertEquals(LeikaMatch.MatchTypeEnum.EXACT, response.getLeikaMatch().getMatchType());
        assertEquals(Set.of(fixture.leika(1)), response.getLeikaMatch().getEffectiveLeikaIds());
    }
}
