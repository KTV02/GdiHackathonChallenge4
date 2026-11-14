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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dataport.suchdienst.client.api.JzuFiApi;
import de.dataport.suchdienst.client.model.Jzufi23FormulardateiBinaer;
import de.dataport.suchdienst.client.model.Jzufi23FormulardateiLink;
import de.dataport.suchdienst.client.model.Jzufi23FormulardateiXML;
import de.dataport.suchdienst.client.model.Jzufi23Sprachversion;
import de.dataport.suchdienst.clienttest.fixture.JzufiPolymorphismFixture;
import de.dataport.suchdienst.clienttest.fixture.SearchFixture;
import de.dataport.suchdienst.clienttest.fixture.SearchFixtureExtension;
import java.util.Base64;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.ResponseEntity;

/** Testziel: GET /v2/organisationunits/{id}/jzufi-2-3 bindet die importierte Organisation im eigenen JZuFi-2.3-Modell. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SearchFixtureExtension.class)
class OrganisationUnitXzufiV2IT {

    private SearchFixture fixture;
    private JzuFiApi api;

    @BeforeAll
    void initializeApis(SearchFixture sharedFixture) {
        fixture = sharedFixture;
        api = new JzuFiApi(fixture.client());
    }

    @Test
    void bindsImportedIdentityAndLocalizedName() {
        var organisation = tree(assertJsonSuccess(read(fixture.organisationId())));
        assertIdentifier(fixture.organisationId(), organisation.path("id"));
        assertEquals(1, organisation.path("name").size());
        assertGermanValue(SearchFixture.ORGANISATION_TITLE, organisation.path("name").get(0).path("name"));
        assertGermanValue("Definierte Beschreibung der SDK-Testbehörde.", organisation.path("beschreibung"));
    }

    @Test
    void bindsAllFormFilesAndPreservesPayloadAndNestedLanguageSubtype() {
        var organisation = api.getJzufiOe231V2(fixture.organisationId());
        var files = organisation.getFormular().getFirst().getDatei();
        assertEquals(4, files.size());
        for (var file : files.subList(0, 2)) {
            var link = assertInstanceOf(Jzufi23FormulardateiLink.class, file);
            assertEquals(SearchFixture.LINK_URI, link.getLink().getUri());
            var language = assertInstanceOf(Jzufi23Sprachversion.class, link.getSprache().getFirst());
            assertEquals(JzufiPolymorphismFixture.LANGUAGE_VERSION, language.getVersion());
            assertEquals(true, language.getKennzeichenMaschinelleUebersetzung());
        }
        var xml = assertInstanceOf(Jzufi23FormulardateiXML.class, files.get(2));
        String document = xml.getXmldaten().getAny().getFirst();
        assertTrue(document.contains(JzufiPolymorphismFixture.XML_PAYLOAD));
        assertTrue(document.contains("urn:sdk:form"));
        assertTrue(document.contains("version=\"1\""));
        var binary = assertInstanceOf(Jzufi23FormulardateiBinaer.class, files.get(3)).getBinaerdaten();
        assertArrayEquals(Base64.getDecoder().decode(JzufiPolymorphismFixture.BINARY_BASE64), binary.getValue());
        assertEquals("sdk-form.bin", binary.getFilename());
        assertEquals("application/octet-stream", binary.getMimeCode());
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
        return api.getJzufiOe231V2WithHttpInfo(id);
    }
}
