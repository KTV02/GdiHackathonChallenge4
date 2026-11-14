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

import de.dataport.suchdienst.client.api.LeistungenApi;
import de.dataport.suchdienst.client.model.SdOutModelV7;
import de.dataport.suchdienst.clienttest.fixture.SearchFixture;
import de.dataport.suchdienst.clienttest.fixture.SearchFixtureExtension;
import java.util.HashSet;
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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Testziel: GET /v2/servicedescriptions/leikahierarchy bindet Treffer und bewahrt das exact-Metadatum über Seiten. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SearchFixtureExtension.class)
class ServiceDescriptionLeikaHierarchyV2IT {

    private SearchFixture fixture;
    private LeistungenApi services;

    @BeforeAll
    void initializeApis(SearchFixture sharedFixture) {
        fixture = sharedFixture;
        services = new LeistungenApi(fixture.client());
    }

    @Test
    void returnsDocumentedStatusAndJson() {
        assertJsonSuccess(
            services.findByLeikaIdHierarchyV2WithHttpInfo(
                List.of(fixture.leika(1)),
                SearchFixture.ARS,
                null,
                0,
                5,
                null
            )
        );
    }

    @ParameterizedTest(name = "LeiKa {0}: exact={1}")
    @MethodSource("hierarchyCases")
    void bindsHierarchyHitsAndExactMetadata(String key, boolean exact, Set<String> ids) {
        var result = services.findByLeikaIdHierarchyV2(List.of(key), SearchFixture.ARS, null, 0, 10, null);
        assertEquals(exact, result.getMeta().getExact());
        assertEquals(ids.size(), result.getTotalElements());
        assertEquals(ids, result.getContent().stream().map(SdOutModelV7::getId).collect(Collectors.toSet()));
        result.getContent().forEach(hit -> assertEquals(fixture.title(fixture.indexOf(hit.getId())), hit.getName()));
    }

    Stream<Arguments> hierarchyCases() {
        return Stream.of(
            arguments(fixture.leika(1), true, Set.of(fixture.id(1))),
            arguments("99099999000000", false, fixture.expectedIds()),
            arguments("99099999223999", false, Set.of(fixture.id(0))),
            arguments(SearchFixture.UNMATCHED_LEIKA, false, Set.of())
        );
    }

    @Test
    void paginatesDescendantsWithoutDuplicateIds() {
        Set<String> ids = new HashSet<>();
        for (int page = 0; page < 2; page++) {
            var result = services.findByLeikaIdHierarchyV2(
                List.of("99099999000000"),
                SearchFixture.ARS,
                null,
                page,
                5,
                null
            );
            assertEquals(6L, result.getTotalElements());
            assertEquals(2, result.getTotalPages());
            assertEquals(false, result.getMeta().getExact());
            assertEquals(page == 0 ? 5 : 1, result.getContent().size());
            result.getContent().forEach(hit -> assertTrue(ids.add(hit.getId()), "Doppelter Hierarchietreffer"));
        }
        assertEquals(fixture.expectedIds(), ids);
    }

    @Test
    void keepsExactMatchAfterResultEnd() {
        var result = services.findByLeikaIdHierarchyV2(List.of(fixture.leika(1)), SearchFixture.ARS, null, 2, 5, null);
        assertEquals(1L, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        assertEquals(true, result.getMeta().getExact());
    }

    @Test
    void rejectsMixedHierarchyLevels() {
        assertApiError(400, () ->
            services.findByLeikaIdHierarchyV2(
                List.of(fixture.leika(0), fixture.leika(1)),
                SearchFixture.ARS,
                null,
                0,
                5,
                null
            )
        );
    }

    @ParameterizedTest(name = "Ungültige Seite/Größe {0}/{1}")
    @CsvSource({ "-1, 5", "101, 5", "0, 4", "0, 10000" })
    void rejectsInvalidPageBounds(int page, int size) {
        assertApiError(400, () ->
            services.findByLeikaIdHierarchyV2(List.of(fixture.leika(1)), SearchFixture.ARS, null, page, size, null)
        );
    }

    @ParameterizedTest(name = "Fehlender Pflichtparameter: {0}")
    @ValueSource(strings = { "ars", "leikaIds" })
    void rejectsMissingRequiredQueryOnServer(String missing) {
        // Nicht die lokale Nullprüfung des Clientgenerators, sondern den Server prüfen.
        assertApiError(400, () ->
            fixture
                .client()
                .getRestClient()
                .get()
                .uri(uri -> {
                    var request = uri.path("/v2/servicedescriptions/leikahierarchy");
                    return missing.equals("ars")
                        ? request.queryParam("leikaIds", fixture.leika(1)).build()
                        : request.queryParam("ars", SearchFixture.ARS).build();
                })
                .retrieve()
                .toBodilessEntity()
        );
    }
}
