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
package de.dataport.suchdienst.indexer.parser.x23;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.dataport.suchdienst.importer.model.XZuFiVersion;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequest;
import de.dataport.suchdienst.importer.model.warteschlange.TransactionMetadata;
import de.dataport.suchdienst.importer.service.GenericZsParser;
import de.dataport.suchdienst.indexer.model.mapping.RelationTyp;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.MessageSource;

@ExtendWith(OutputCaptureExtension.class)
class RelationsParserTest extends AbstractX23ParserTest {

    private GenericZsParser xzufiRelationsParserService;
    private MessageSource messageSource;

    @BeforeEach
    void setUpService() {
        xzufiRelationsParserService = new GenericZsParser(codelistService);
        messageSource = Mockito.mock(MessageSource.class);
    }

    @Test
    void testParsing(CapturedOutput output) {
        // Fail fast: wenn das default charset nicht UTF-8 ist, geht dieser Test schief,
        // denn CapturedOutput sammelt stdout mit dem default-encoding auf,
        // und die Ausgabe ist zurzeit stets UTF-8
        var metadata = new TransactionMetadata("1", "1", "1", "1", "1");
        var importRequest = ImportRequest.createOpen(
            metadata,
            "hello".getBytes(),
            XZuFiVersion.XZUFI22
        );
        assertEquals(StandardCharsets.UTF_8, Charset.defaultCharset());
        var relations = xzufiRelationsParserService.parseRelations(
            provider.getRelations(),
            importRequest
        );

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

        // Zuständigkeiten dürfen nur einen ARS enthalten
        assertThat(
            output.toString(),
            output.toString(),
            containsString("Eine Zuständigkeit darf nur einen Ars enthalten.")
        );
        // Zuständigkeiten dürfen nur eine Leistung enthalten
        assertThat(
            output.toString(),
            output.toString(),
            containsString("Eine Zuständigkeit darf nur eine Leistungs-Id enthalten.")
        );
    }

    @Test
    void testNegativeParsing() {
        Mockito.when(
            messageSource.getMessage(Mockito.anyString(), Mockito.eq(null), Mockito.any())
        ).thenReturn("blub");
        assertThrows(IllegalArgumentException.class, () ->
            loadIntoProvider("x23/xzufi_parsertests_negative.xml")
        );
    }
}
