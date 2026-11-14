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
import static de.dataport.suchdienst.clienttest.support.JzufiContractAssertions.assertGermanValue;
import static de.dataport.suchdienst.clienttest.support.JzufiContractAssertions.assertIdentifier;
import static de.dataport.suchdienst.clienttest.support.JzufiContractAssertions.tree;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import de.dataport.suchdienst.client.api.JzuFiApi;
import de.dataport.suchdienst.client.model.Jzufi23LeistungsstrukturObjektMitVerrichtungUndDetail;
import de.dataport.suchdienst.clienttest.fixture.JzufiPolymorphismFixture;
import de.dataport.suchdienst.clienttest.fixture.SearchFixture;
import de.dataport.suchdienst.clienttest.fixture.SearchFixtureExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.ResponseEntity;

/** Testziel: GET /v2/servicedescriptions/{id}/jzufi-2-3 bindet das eigene JZuFi-2.3-Ausgabemodell strikt als JSON. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SearchFixtureExtension.class)
class ServiceDescriptionXzufiV2IT {

    private SearchFixture fixture;
    private JzuFiApi api;

    @BeforeAll
    void initializeApis(SearchFixture sharedFixture) {
        fixture = sharedFixture;
        api = new JzuFiApi(fixture.client());
    }

    @Test
    void bindsImportedIdentityTitleAndLeika() {
        var service = tree(assertJsonSuccess(read(fixture.id(0))));
        assertIdentifier(fixture.id(0), service.path("id"));
        var key = service.path("referenzLeistungsschluessel");
        assertEquals(1, key.size());
        assertEquals(fixture.leika(0), key.get(0).path("value").asString());
        var title = service
            .path("modulText")
            .valueStream()
            .filter(module -> "03".equals(module.path("leikaTextmodul").path("code").asString()))
            .findFirst()
            .orElseThrow();
        assertGermanValue(fixture.title(0), title.path("inhalt"));
    }

    @Test
    void preservesAllThreeLevelsOfTheImportedServiceStructure() {
        var service = api.getJzufiLb231V2(fixture.id(0));
        var structure = assertInstanceOf(
            Jzufi23LeistungsstrukturObjektMitVerrichtungUndDetail.class,
            service.getStruktur()
        );
        assertEquals("100", structure.getLeistungsgruppierungLeiKa().getCode());
        assertIdentifier(fixture.id(0), tree(structure.getLeistungsobjektID()));
        assertEquals("001", structure.getVerrichtungLeiKa().getCode());
        assertGermanValue(JzufiPolymorphismFixture.STRUCTURE_DETAIL, tree(structure.getVerrichtungsdetail()));
    }

    @Test
    void returnsNotFoundForUnknownId() {
        assertApiError(404, () -> read("missing." + fixture.query()));
    }

    @Test
    void rejectsOverlongId() {
        assertApiError(400, () -> read("x".repeat(256)));
    }

    private ResponseEntity<?> read(String id) {
        return api.getJzufiLb231V2WithHttpInfo(id);
    }
}
