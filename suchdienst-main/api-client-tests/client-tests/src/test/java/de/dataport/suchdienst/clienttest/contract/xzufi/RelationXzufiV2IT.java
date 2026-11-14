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
package de.dataport.suchdienst.clienttest.contract.xzufi;

import static de.dataport.suchdienst.clienttest.support.HttpContractAssertions.assertApiError;
import static de.dataport.suchdienst.clienttest.support.HttpContractAssertions.assertJsonSuccess;
import static de.dataport.suchdienst.clienttest.support.JzufiContractAssertions.assertIdentifier;
import static de.dataport.suchdienst.clienttest.support.JzufiContractAssertions.tree;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dataport.suchdienst.client.api.JzuFiApi;
import de.dataport.suchdienst.clienttest.fixture.SearchFixture;
import de.dataport.suchdienst.clienttest.fixture.SearchFixtureExtension;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.ResponseEntity;

/** Testziel: GET /v2/relations/jzufi-2-3 bindet eigene OE-/OD-Zuständigkeiten und bewahrt Seitenmetadaten. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SearchFixtureExtension.class)
class RelationXzufiV2IT {

    private SearchFixture fixture;
    private JzuFiApi api;

    @BeforeAll
    void initializeApis(SearchFixture sharedFixture) {
        fixture = sharedFixture;
        api = new JzuFiApi(fixture.client());
    }

    @Test
    void bindsOnlyImportedOrganisationAndOnlineServiceRelations() {
        var result = tree(assertJsonSuccess(read(SearchFixture.ARS, List.of(fixture.id(0)), 0, 5)));
        assertEquals(2L, result.path("page").path("totalElements").asLong());
        assertEquals(2, result.path("content").size());
        Set<String> parents = new HashSet<>();
        for (var relation : result.path("content")) {
            var parent = relation.path("uebergeordnetesObjektID");
            var parentId = parent.path("value").asString();
            String fullId = parentId.equals(fixture.organisationId().split("\\.", 3)[2])
                ? fixture.organisationId()
                : fixture.onlineServiceId();
            assertIdentifier(fullId, parent);
            assertTrue(parents.add(fullId));
            assertEquals(1, relation.path("zustaendigkeit").size());
            var responsibility = relation.path("zustaendigkeit").get(0);
            assertEquals(1, responsibility.path("leistungID").size());
            assertIdentifier(fixture.id(0), responsibility.path("leistungID").get(0));
            assertEquals(1, responsibility.path("gebietID").size());
            assertEquals(SearchFixture.ARS, responsibility.path("gebietID").get(0).path("value").asString());
            if (fullId.equals(fixture.organisationId())) {
                assertEquals("ZustaendigkeitOrganisationseinheit", responsibility.path("@type").asString());
                var role = responsibility.path("rolle");
                assertEquals(1, role.size());
                role = role.get(0);
                assertEquals("02", role.path("code").asString());
            } else {
                assertEquals("ZustaendigkeitOnlinedienst", responsibility.path("@type").asString());
            }
        }
        assertEquals(Set.of(fixture.organisationId(), fixture.onlineServiceId()), parents);
        var pastEnd = tree(assertJsonSuccess(read(SearchFixture.ARS, List.of(fixture.id(0)), 1, 5)));
        assertEquals(2L, pastEnd.path("page").path("totalElements").asLong());
        assertEquals(0, pastEnd.path("content").size());
    }

    @Test
    void paginatesAllImportedRelationsWithoutDuplicates() {
        var ids = List.copyOf(fixture.expectedIds());
        Set<String> expected = new HashSet<>();
        for (var parent : List.of(fixture.organisationId(), fixture.onlineServiceId())) {
            for (var id : ids) expected.add(parent.split("\\.", 3)[2] + "|" + id.split("\\.", 3)[2]);
        }
        Set<String> seen = new HashSet<>();
        for (int page = 0; page < 3; page++) {
            var result = tree(assertJsonSuccess(read(SearchFixture.ARS, ids, page, 5)));
            assertEquals(expected.size(), result.path("page").path("totalElements").asLong());
            assertEquals(page == 2 ? 2 : 5, result.path("content").size());
            for (var relation : result.path("content")) {
                assertEquals(1, relation.path("zustaendigkeit").size());
                var responsibility = relation.path("zustaendigkeit").get(0);
                assertEquals(1, responsibility.path("leistungID").size());
                var pair =
                    relation.path("uebergeordnetesObjektID").path("value").asString() +
                    "|" +
                    responsibility.path("leistungID").get(0).path("value").asString();
                assertTrue(expected.contains(pair), "Unzugehörige Relation: " + pair);
                assertTrue(seen.add(pair), "Doppelte Relation: " + pair);
            }
        }
        assertEquals(expected, seen);
        var pastEnd = tree(assertJsonSuccess(read(SearchFixture.ARS, ids, 3, 5)));
        assertEquals(expected.size(), pastEnd.path("page").path("totalElements").asLong());
        assertEquals(0, pastEnd.path("content").size());
    }

    @Test
    void excludesUnknownServicesAndUnrelatedRegions() {
        for (var result : List.of(
            read(SearchFixture.ARS, List.of("missing." + fixture.query()), 0, 5),
            read("010000000000", List.of(fixture.id(0)), 0, 5)
        )) {
            var empty = tree(assertJsonSuccess(result));
            assertEquals(0L, empty.path("page").path("totalElements").asLong());
            assertEquals(0, empty.path("content").size());
        }
    }

    @Test
    void rejectsInvalidArsIdsAndPageBounds() {
        assertApiError(400, () -> read("invalid", List.of(fixture.id(0)), 0, 5));
        assertApiError(400, () -> read(SearchFixture.ARS, List.of("x".repeat(256)), 0, 5));
        for (var bounds : new int[][] { { -1, 5 }, { 0, 4 }, { 0, 10000 } })
            assertApiError(400, () -> read(SearchFixture.ARS, List.of(fixture.id(0)), bounds[0], bounds[1]));
    }

    @Test
    void rejectsMissingRequiredQueryOnServer() {
        for (var missing : new String[] { "ars", "lbids" })
            assertApiError(400, () ->
                fixture
                    .client()
                    .getRestClient()
                    .get()
                    .uri(uri -> {
                        var request = uri.path("/v2/relations/jzufi-2-3");
                        return missing.equals("ars")
                            ? request.queryParam("lbids", fixture.id(0)).build()
                            : request.queryParam("ars", SearchFixture.ARS).build();
                    })
                    .retrieve()
                    .toBodilessEntity()
            );
    }

    private ResponseEntity<?> read(String ars, List<String> ids, int page, int size) {
        return api.getZsTrByLbIdAndArs231V2WithHttpInfo(ars, ids, false, page, size);
    }
}
