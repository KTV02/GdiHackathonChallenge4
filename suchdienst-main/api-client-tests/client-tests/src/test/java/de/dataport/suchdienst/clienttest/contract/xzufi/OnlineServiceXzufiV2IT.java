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

import de.dataport.suchdienst.client.api.JzuFiApi;
import de.dataport.suchdienst.clienttest.fixture.SearchFixture;
import de.dataport.suchdienst.clienttest.fixture.SearchFixtureExtension;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.ResponseEntity;

/** Testziel: GET /v2/onlineservices/{id}/jzufi-2-3 liefert den definierten Onlinedienst als strikt gebundenes eigenes JZuFi-2.3-Modell. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SearchFixtureExtension.class)
class OnlineServiceXzufiV2IT {

    private SearchFixture fixture;
    private JzuFiApi api;

    @BeforeAll
    void initializeApis(SearchFixture sharedFixture) {
        fixture = sharedFixture;
        api = new JzuFiApi(fixture.client());
    }

    @Test
    void bindsImportedIdentityNameAndLink() {
        var online = tree(assertJsonSuccess(read(fixture.onlineServiceId())));
        assertIdentifier(fixture.onlineServiceId(), online.path("id"));
        assertGermanValue(SearchFixture.ONLINE_NAME, online.path("bezeichnung"));
        assertGermanValue("Definierter SDK-Onlinedienst.", online.path("beschreibung"));
        assertEquals(2, online.path("link").size());
        var languages = new HashSet<String>();
        for (var link : online.path("link")) {
            assertEquals(SearchFixture.LINK_URI, link.path("link").asString());
            assertEquals("SDK-Antragslink", link.path("titel").asString());
            languages.add(link.path("languageCode").asString());
        }
        assertEquals(Set.of("de", "en"), languages);
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
        return api.getJzufiOd231V2WithHttpInfo(id);
    }
}
