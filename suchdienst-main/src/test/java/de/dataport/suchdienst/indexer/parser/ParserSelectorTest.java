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
package de.dataport.suchdienst.indexer.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetail;
import de.dataport.suchdienst.util.ServiceModuleCode;
import de.xoev.xzufi.v2_2_0.CodeLeistungstextmodul;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests für {@link ParserSelector} zur Sicherstellung folgender Kernfälle:
 *  - Null-Modul liefert null
 *  - Kein passender Parser konfiguriert -> null
 *  - Erfolgreiches Parsen gibt (code, modul) zurück
 *  - Parser gibt null zurück -> Ergebnis null
 *  - Parser hat keinen eigenen Code -> Fallback über Textmodul-Code (Mapping via ServiceModuleCode.fromCode)
 *  - Volltext-Sonderfall mit dynamischem TestParser (zeigt Aufrufreihenfolge)
 */
@ExtendWith(MockitoExtension.class)
class ParserSelectorTest {

    @Mock
    private ParserRegistry parserRegistry;

    @Mock
    private Parser<Object, AbstractServiceModule> parser;

    @Mock
    private de.xoev.xzufi.v2_2_0.Textmodul textmodulV220;

    @Test
    void acceptReturnsNullWhenModuleIsNull() {
        ParserSelector selector = ParserSelector.of(parserRegistry);
        Map.Entry<String, AbstractServiceModule> result = selector.accept(
            null,
            Locale.ENGLISH,
            "id"
        );
        assertNull(result);
    }

    @Test
    void acceptReturnsNullWhenParserIsNotFound() {
        when(parserRegistry.getParsers()).thenReturn(Map.of());
        ParserSelector selector = ParserSelector.of(parserRegistry);
        Map.Entry<String, AbstractServiceModule> result = selector.accept(
            new Object(),
            Locale.ENGLISH,
            "id"
        );
        assertNull(result);
    }

    @Test
    void acceptParsesModuleAndReturnsEntryWhenParserExists() {
        var indexerModule = mock(ServiceModuleDetail.class);
        when(parserRegistry.getParsers()).thenReturn(Map.of(Object.class, parser));
        when(parser.parse(any(), any())).thenReturn(indexerModule);
        when(parser.getCode()).thenReturn(ServiceModuleCode.KOSTEN);

        ParserSelector selector = ParserSelector.of(parserRegistry);
        Map.Entry<String, AbstractServiceModule> result = selector.accept(
            new Object(),
            Locale.ENGLISH,
            "id"
        );

        assertNotNull(result);
        assertEquals(ServiceModuleCode.KOSTEN.getCode(), result.getKey());
        assertEquals(indexerModule, result.getValue());
    }

    @Test
    void acceptReturnsNullWhenParsedModuleIsNull() {
        when(parserRegistry.getParsers()).thenReturn(Map.of(Object.class, parser));
        when(parser.parse(any(), any())).thenReturn(null);
        when(parser.getCode()).thenReturn(ServiceModuleCode.KOSTEN);

        ParserSelector selector = ParserSelector.of(parserRegistry);
        Map.Entry<String, AbstractServiceModule> result = selector.accept(
            new Object(),
            Locale.ENGLISH,
            "id"
        );

        assertNull(result, "Result should be null when parsed module is null");
    }

    @Test
    void acceptUsesCodeFromName22WhenParserCodeIsNull() {
        // Simuliert ein Textmodul dessen Code via CodeLeistungstextmodul ermittelt wird,
        // falls der Parser keinen eigenen Code liefert.
        var codeLeistungstextmodul = mock(CodeLeistungstextmodul.class);
        var textmodul = mock(de.xoev.xzufi.v2_2_0.Textmodul.class);
        var indexerModule = mock(ServiceModule.class);
        var parserMock = mock(Parser.class);
        var parserRegistryMock = mock(ParserRegistry.class);

        when(parserRegistryMock.getParsers()).thenReturn(
            Map.of(de.xoev.xzufi.v2_2_0.Textmodul.class, parserMock)
        );
        when(parserMock.parse(any(), any())).thenReturn(indexerModule);
        when(parserMock.getCode()).thenReturn(null); // Fallback auslösen
        when(textmodul.getLeikaTextmodul()).thenReturn(codeLeistungstextmodul);
        when(codeLeistungstextmodul.getCode()).thenReturn("02");

        try (var mocked = org.mockito.Mockito.mockStatic(ServiceModuleCode.class)) {
            mocked
                .when(() -> ServiceModuleCode.fromCode("02"))
                .thenReturn(Optional.of(ServiceModuleCode.LEISTUNGSBEZEICHNUNG));

            ParserSelector selector = ParserSelector.of(parserRegistryMock);
            Map.Entry<String, ServiceModule> result = selector.accept(
                textmodul,
                Locale.ENGLISH,
                "id"
            );

            assertNotNull(result);
            assertEquals(ServiceModuleCode.LEISTUNGSBEZEICHNUNG.getCode(), result.getKey());
            assertEquals(indexerModule, result.getValue());

            verify(parserMock).parse(textmodul, Locale.ENGLISH);
            verify(parserMock).getCode();
            verify(textmodul).getLeikaTextmodul();
            verify(codeLeistungstextmodul).getCode();
        }
    }

    @Test
    void acceptUsesCodeFromVolltextCustomMocking() {
        // Demonstriert Fallback Code-Ermittlung mit eigenem TestParser (parse()/getCode() Aufrufreihenfolge).
        var codeLeistungstextmodul = mock(CodeLeistungstextmodul.class);
        when(codeLeistungstextmodul.getCode()).thenReturn("06");
        when(textmodulV220.getLeikaTextmodul()).thenReturn(codeLeistungstextmodul);

        class TestParser implements Parser<de.xoev.xzufi.v2_2_0.Textmodul, ServiceModule> {

            boolean parseCalled = false;
            boolean getCodeCalled = false;

            @Override
            public ServiceModule parse(de.xoev.xzufi.v2_2_0.Textmodul modul, Locale locale) {
                parseCalled = true;
                return mock(ServiceModule.class);
            }

            @Override
            public Set<String> getIncludedLanguages(de.xoev.xzufi.v2_2_0.Textmodul module) {
                return Set.of();
            }

            @Override
            public ServiceModuleCode getCode() {
                getCodeCalled = true;
                return null; // Fallback erzwingen
            }
        }

        class TestConfig implements ParserRegistry {

            private final TestParser testParser = new TestParser();

            @Override
            public Map<Class<?>, Parser<?, ?>> getParsers() {
                return Map.of(de.xoev.xzufi.v2_2_0.Textmodul.class, testParser);
            }
        }

        try (var _ = org.mockito.Mockito.mockStatic(ServiceModuleCode.class)) {
            TestConfig config = new TestConfig();
            ParserSelector selector = ParserSelector.of(config);
            Map.Entry<String, ServiceModule> result = selector.accept(
                textmodulV220,
                Locale.ENGLISH,
                "id"
            );

            assertNotNull(result);
            assertEquals(ServiceModuleCode.VOLLTEXT.getCode(), result.getKey());
            assertTrue(config.testParser.parseCalled, "parse sollte aufgerufen werden");
            assertTrue(config.testParser.getCodeCalled, "getCode sollte aufgerufen werden");
        }
    }
}
