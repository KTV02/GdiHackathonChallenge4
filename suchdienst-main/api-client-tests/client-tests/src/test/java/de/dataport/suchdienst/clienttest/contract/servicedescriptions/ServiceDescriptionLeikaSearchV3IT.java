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

import de.dataport.suchdienst.client.api.LeistungenApi;
import de.dataport.suchdienst.clienttest.fixture.SearchFixture;
import de.dataport.suchdienst.clienttest.fixture.SearchFixtureExtension;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Testziel: GET /v3/servicedescriptions/leikaid liefert Fixture-Treffer, Organisations-IDs und korrekte Seiten/Fehler. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SearchFixtureExtension.class)
class ServiceDescriptionLeikaSearchV3IT {

    private SearchFixture fixture;
    private LeistungenApi services;

    @BeforeAll
    void initializeApis(SearchFixture sharedFixture) {
        fixture = sharedFixture;
        services = new LeistungenApi(fixture.client());
    }

    @Test
    void returnsImportedServiceAndOrganisationIds() {
        var result = assertJsonSuccess(
            services.findByLeikaIdV3WithHttpInfo(
                List.of(fixture.leika(0)),
                SearchFixture.ARS,
                List.of(fixture.query()),
                true,
                0,
                5,
                null,
                false
            )
        );
        assertEquals(1L, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        var hit = result.getContent().getFirst();
        assertEquals(fixture.id(0), hit.getId());
        assertEquals(fixture.title(0), hit.getName());
        assertEquals(SearchFixture.DESCRIPTION, hit.getDescription());
        assertEquals(Set.of(fixture.leika(0)), hit.getLeikakeys());
        assertEquals(Set.of(fixture.organisationId()), hit.getOrganisationIds());
    }

    @Test
    void paginatesAllRequestedServicesWithoutDuplicateIds() {
        var keys = IntStream.range(0, SearchFixture.COUNT).mapToObj(fixture::leika).toList();
        Set<String> ids = new HashSet<>();
        for (int page = 0; page < 2; page++) {
            var result = services.findByLeikaIdV3(
                keys,
                SearchFixture.ARS,
                List.of(fixture.query()),
                true,
                page,
                5,
                null,
                false
            );
            assertEquals(6L, result.getTotalElements());
            assertEquals(2, result.getTotalPages());
            assertEquals(page == 0 ? 5 : 1, result.getContent().size());
            result.getContent().forEach(hit -> assertTrue(ids.add(hit.getId()), "Doppelter Treffer"));
        }
        assertEquals(fixture.expectedIds(), ids);
        var pastEnd = services.findByLeikaIdV3(
            keys,
            SearchFixture.ARS,
            List.of(fixture.query()),
            true,
            2,
            5,
            null,
            false
        );
        assertEquals(6L, pastEnd.getTotalElements());
        assertTrue(pastEnd.getContent().isEmpty());
    }

    @Test
    void omitsOrganisationIdsWhenDisabled() {
        var result = services.findByLeikaIdV3(
            List.of(fixture.leika(0)),
            SearchFixture.ARS,
            null,
            false,
            0,
            5,
            null,
            false
        );
        assertEquals(1L, result.getTotalElements());
        var ids = result.getContent().getFirst().getOrganisationIds();
        assertTrue(ids == null || ids.isEmpty());
    }

    @Test
    void returnsEmptyPageForUnmatchedLeika() {
        var result = services.findByLeikaIdV3(
            List.of(SearchFixture.UNMATCHED_LEIKA),
            SearchFixture.ARS,
            List.of(fixture.query()),
            true,
            0,
            5,
            null,
            false
        );
        assertEquals(0L, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void defaultsMatchExplicitDefaults() {
        var implicit = services.findByLeikaIdV3(
            List.of(fixture.leika(0)),
            SearchFixture.ARS,
            List.of(fixture.query()),
            null,
            null,
            null,
            null,
            null
        );
        var explicit = services.findByLeikaIdV3(
            List.of(fixture.leika(0)),
            SearchFixture.ARS,
            List.of(fixture.query()),
            true,
            0,
            10,
            null,
            true
        );
        // Zeitstempel werden pro Anfrage erzeugt; nur stabile Vertragswerte vergleichen.
        assertEquals(explicit.getTotalElements(), implicit.getTotalElements());
        assertEquals(explicit.getPageable(), implicit.getPageable());
        assertEquals(explicit.getContent().getFirst().getId(), implicit.getContent().getFirst().getId());
        assertEquals(
            explicit.getContent().getFirst().getOrganisationIds(),
            implicit.getContent().getFirst().getOrganisationIds()
        );
    }

    @ParameterizedTest(name = "Gültigkeit {0}: {1} Treffer")
    @CsvSource({ "2000-01-01, 1", "2099-12-31, 1", "1999-12-31, 0", "2100-01-01, 0" })
    void respectsValidityBoundaries(String date, long hits) {
        var result = services.findByLeikaIdV3(
            List.of(fixture.leika(0)),
            SearchFixture.ARS,
            List.of(fixture.query()),
            true,
            0,
            5,
            LocalDate.parse(date),
            false
        );
        assertEquals(hits, result.getTotalElements());
        assertEquals((int) hits, result.getContent().size());
    }

    @ParameterizedTest(name = "Ungültige Seite/Größe {0}/{1}")
    @CsvSource({ "-1, 5", "101, 5", "0, 4", "0, 10000" })
    void rejectsInvalidPageBounds(int page, int size) {
        assertApiError(400, () ->
            services.findByLeikaIdV3(List.of(fixture.leika(0)), SearchFixture.ARS, null, true, page, size, null, false)
        );
    }

    @Test
    void rejectsTooShortLeikaId() {
        assertApiError(400, () ->
            services.findByLeikaIdV3(List.of("short"), SearchFixture.ARS, null, true, 0, 5, null, false)
        );
    }

    @ParameterizedTest(name = "Fehlender Pflichtparameter: {0}")
    @ValueSource(strings = { "ars", "leikaIds" })
    void rejectsMissingRequiredQueryOnServer(String missing) {
        // Rohes HTTP umgeht die lokale Nullprüfung des generierten Clients.
        assertApiError(400, () ->
            fixture
                .client()
                .getRestClient()
                .get()
                .uri(uri -> {
                    var request = uri.path("/v3/servicedescriptions/leikaid");
                    return missing.equals("ars")
                        ? request.queryParam("leikaIds", fixture.leika(0)).build()
                        : request.queryParam("ars", SearchFixture.ARS).build();
                })
                .retrieve()
                .toBodilessEntity()
        );
    }
}
