/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2025 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.indexer.parser.x22;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.dataport.suchdienst.importer.model.XZuFiVersion;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequest;
import de.dataport.suchdienst.importer.model.warteschlange.TransactionMetadata;
import de.dataport.suchdienst.importer.service.GenericZsParser;
import de.dataport.suchdienst.indexer.model.mapping.RelationTyp;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class RelationsParserTest extends AbstractX22ParserTest {

    @Test
    void testParsing() {
        GenericZsParser parser = new GenericZsParser(codelistService);
        var metadata = new TransactionMetadata("1", "1", "1", "1", "1");
        var importRequest = ImportRequest.createOpen(
            metadata,
            "hello".getBytes(StandardCharsets.UTF_8),
            XZuFiVersion.XZUFI22
        );

        assertEquals(StandardCharsets.UTF_8, Charset.defaultCharset());

        var relations = parser.parseRelations(provider.getRelations(), importRequest);

        assertThat(relations.size(), is(8));
        assertThat(
            relations
                .stream()
                .filter(r -> r.getTyp() == RelationTyp.ORGANISATION)
                .count(),
            is(7L)
        );
        assertThat(
            relations
                .stream()
                .filter(r -> r.getTyp() == RelationTyp.FORM)
                .count(),
            is(1L)
        );

        var firstOrganisationRelationWithRole = relations
            .stream()
            .filter(r -> r.getTyp() == RelationTyp.ORGANISATION)
            .filter(r -> r.getResponsibility() != null)
            .findFirst()
            .orElseThrow();
        assertThat(firstOrganisationRelationWithRole.getResponsibility().getUri(), notNullValue());
        assertThat(
            firstOrganisationRelationWithRole.getResponsibility().getVersion(),
            notNullValue()
        );
    }

    @Test
    void testNegativeParsing() {
        assertThrows(IllegalArgumentException.class, () ->
            loadIntoProvider("xzufi_parsertests_negative.xml")
        );
    }
}
