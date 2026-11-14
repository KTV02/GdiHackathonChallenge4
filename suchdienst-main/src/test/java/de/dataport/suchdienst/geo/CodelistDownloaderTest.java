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
package de.dataport.suchdienst.geo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.dataport.pvog.pvexternaldownloader.service.ExternalDownloadException;
import de.dataport.pvog.pvexternaldownloader.service.PvExternalDownloaderCodelistService;
import de.dataport.pvog.pvexternaldownloader.service.ResourceNotFoundException;
import de.dataport.suchdienst.config.CacheConfiguration;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.geo.service.CodelistDownloader;
import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.service.CodelistParser;
import de.dataport.suchdienst.indexer.service.CodelistService;
import de.xrepository.genericodeservices.CodeListDocument;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(classes = { CodelistDownloader.class, CodelistService.class })
class CodelistDownloaderTest {

    @Autowired
    private CodelistDownloader codelistDownloader;

    @MockitoBean
    private PvExternalDownloaderCodelistService pvExternalDownloader;

    @MockitoBean
    private CodelistService codelistService;

    @MockitoBean(name = CacheConfiguration.REDIS_MGR)
    private CacheManager redisCacheManager;

    @Test
    void loadArsSeedIntoCache_shouldLoadSeedFileSuccessfully() {
        List<CodelistEntry> mockEntries = List.of(
            createMockEntry("010010000000", "Flensburg"),
            createMockEntry("010020000000", "Kiel"),
            createMockEntry("010030000000", "Lübeck"),
            createMockEntry(
                "021010101101",
                "Hamburg-Altstadt, OT 101",
                "Stadt-/Ortsteil bzw. Stadtbezirk"
            )
        );
        when(codelistService.parseCodelistDocument(any(Resource.class))).thenReturn(mockEntries);

        Map<String, String> result = codelistDownloader.loadArsSeedIntoCache();

        assertThat("Result should not be null", result, is(notNullValue()));
        assertThat("Result should contain 4 entries", result.size(), is(4));
        assertThat(result, hasEntry("010010000000", "Flensburg"));
        assertThat(result, hasEntry("010020000000", "Kiel"));
        assertThat(result, hasEntry("010030000000", "Lübeck"));
        assertThat(result, hasEntry("021010101101", "Hamburg-Altstadt, OT 101"));
        verify(codelistService).parseCodelistDocument(any(Resource.class));
    }

    private CodelistEntry createMockEntry(String code, String germanText) {
        CodelistEntry entry = new CodelistEntry();
        entry.setCode(code);
        entry.setText(Locale.GERMAN, germanText);
        return entry;
    }

    private CodelistEntry createMockEntry(String code, String germanText, String hinweis) {
        CodelistEntry entry = createMockEntry(code, germanText);
        entry.getValues().add(new CodelistEntry.CodeListValue("Hinweis", hinweis, Locale.GERMAN));
        return entry;
    }

    @Test
    void updateDestatisArsCodelistCache_shouldDownloadAndCacheNewData() throws Exception {
        CodeListDocument mockDocument = new CodeListDocument();
        List<CodelistEntry> expectedEntries = List.of(
            createMockEntry("010010000000", "Flensburg"),
            createMockEntry("010020000000", "Kiel"),
            createMockEntry(
                "021010101101",
                "Hamburg-Altstadt, OT 101",
                "Stadt-/Ortsteil bzw. Stadtbezirk"
            )
        );

        when(pvExternalDownloader.getCodelist(anyString())).thenReturn(mockDocument);

        try (MockedStatic<CodelistParser> mockedParser = mockStatic(CodelistParser.class)) {
            mockedParser
                .when(() ->
                    CodelistParser.parseCodelistDocumentToEntries(any(CodeListDocument.class))
                )
                .thenReturn(expectedEntries);

            Map<String, String> result = codelistDownloader.updateDestatisArsCodelistCache();

            assertThat("Result should not be null", result, is(notNullValue()));
            assertThat("Result should contain 3 entries", result.size(), is(3));
            assertThat(result, hasEntry("010010000000", "Flensburg"));
            assertThat(result, hasEntry("010020000000", "Kiel"));
            assertThat(result, hasEntry("021010101101", "Hamburg-Altstadt, OT 101"));
            verify(pvExternalDownloader).getCodelist(
                "urn:de:bund:destatis:bevoelkerungsstatistik:schluessel:rs"
            );
        }
    }

    @Test
    void updateDestatisArsCodelistCaches_shouldUpdateFullAndReducedCacheFromSingleDownload()
        throws Exception {
        CodeListDocument mockDocument = new CodeListDocument();
        Cache arsCache = org.mockito.Mockito.mock(Cache.class);
        List<CodelistEntry> expectedEntries = List.of(
            createMockEntry("010010000000", "Flensburg"),
            createMockEntry(
                "021010101101",
                "Hamburg-Altstadt, OT 101",
                "Stadt-/Ortsteil bzw. Stadtbezirk"
            ),
            createMockEntry("110000010101", "Berlin-Mitte", "Stadtbezirk")
        );

        when(pvExternalDownloader.getCodelist(anyString())).thenReturn(mockDocument);
        when(redisCacheManager.getCache(CacheConfiguration.CODELIST_ARS_CACHE_NAME)).thenReturn(
            arsCache
        );

        try (MockedStatic<CodelistParser> mockedParser = mockStatic(CodelistParser.class)) {
            mockedParser
                .when(() ->
                    CodelistParser.parseCodelistDocumentToEntries(any(CodeListDocument.class))
                )
                .thenReturn(expectedEntries);

            Map<String, String> result = codelistDownloader.updateDestatisArsCodelistCaches();

            assertThat(result.size(), is(1));
            assertThat(result, hasEntry("010010000000", "Flensburg"));
            verify(arsCache).put(
                CodelistDownloader.ARS_CACHE_KEY,
                Map.of(
                    "010010000000",
                    "Flensburg",
                    "021010101101",
                    "Hamburg-Altstadt, OT 101",
                    "110000010101",
                    "Berlin-Mitte"
                )
            );
            verify(arsCache).put(
                CodelistDownloader.REDUCED_ARS_CACHE_KEY,
                Map.of("010010000000", "Flensburg")
            );
            verify(pvExternalDownloader).getCodelist(
                "urn:de:bund:destatis:bevoelkerungsstatistik:schluessel:rs"
            );
        }
    }

    @Test
    void updateDestatisArsCodelistCaches_shouldWarnOnceWhenHinweisColumnIsMissing(
        CapturedOutput output
    ) throws Exception {
        CodeListDocument mockDocument = new CodeListDocument();
        Cache arsCache = org.mockito.Mockito.mock(Cache.class);
        List<CodelistEntry> expectedEntries = List.of(
            createMockEntry("010010000000", "Flensburg"),
            createMockEntry("010020000000", "Kiel")
        );

        when(pvExternalDownloader.getCodelist(anyString())).thenReturn(mockDocument);
        when(redisCacheManager.getCache(CacheConfiguration.CODELIST_ARS_CACHE_NAME)).thenReturn(
            arsCache
        );

        try (MockedStatic<CodelistParser> mockedParser = mockStatic(CodelistParser.class)) {
            mockedParser
                .when(() ->
                    CodelistParser.parseCodelistDocumentToEntries(any(CodeListDocument.class))
                )
                .thenReturn(expectedEntries);

            Map<String, String> result = codelistDownloader.updateDestatisArsCodelistCaches();

            assertThat(result.size(), is(2));
            assertThat(result, hasEntry("010010000000", "Flensburg"));
            assertThat(result, hasEntry("010020000000", "Kiel"));
            verify(arsCache).put(
                CodelistDownloader.REDUCED_ARS_CACHE_KEY,
                Map.of("010010000000", "Flensburg", "010020000000", "Kiel")
            );
            assertThat(
                countOccurrences(output.toString(), "ARS-Codeliste enthält keine Spalte 'Hinweis'"),
                is(1)
            );
        }
    }

    @Test
    void updateDestatisArsCodelistCache_shouldThrowIllegalStateForEmptyCodelist() throws Exception {
        CodeListDocument mockDocument = new CodeListDocument();
        when(pvExternalDownloader.getCodelist(anyString())).thenReturn(mockDocument);

        try (MockedStatic<CodelistParser> mockedParser = mockStatic(CodelistParser.class)) {
            mockedParser
                .when(() ->
                    CodelistParser.parseCodelistDocumentToEntries(any(CodeListDocument.class))
                )
                .thenThrow(new IllegalStateException("Codeliste ist leer"));

            IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                codelistDownloader.updateDestatisArsCodelistCache()
            );

            assertThat(exception.getMessage(), is("Codeliste ist leer"));
        }
    }

    @Test
    void updateDestatisArsCodelistCache_shouldReturnEmptyMapWhenDownloadFails() throws Exception {
        when(pvExternalDownloader.getCodelist(anyString())).thenThrow(
            new ResourceNotFoundException("Resource not found")
        );

        Map<String, String> result = codelistDownloader.updateDestatisArsCodelistCache();

        assertThat("Should return empty map when download fails", result, is(notNullValue()));
        assertThat("Map should be empty", result.isEmpty(), is(true));
    }

    @Test
    void getLatestDestatisArsCodelist_shouldReturnDataWhenDownloadSucceeds() throws Exception {
        CodeListDocument mockDocument = new CodeListDocument();
        List<CodelistEntry> expectedEntries = List.of(
            createMockEntry("010010000000", "Flensburg"),
            createMockEntry("010020000000", "Kiel"),
            createMockEntry(
                "021010101101",
                "Hamburg-Altstadt, OT 101",
                "Stadt-/Ortsteil bzw. Stadtbezirk"
            )
        );

        when(pvExternalDownloader.getCodelist(anyString())).thenReturn(mockDocument);

        try (MockedStatic<CodelistParser> mockedParser = mockStatic(CodelistParser.class)) {
            mockedParser
                .when(() ->
                    CodelistParser.parseCodelistDocumentToEntries(any(CodeListDocument.class))
                )
                .thenReturn(expectedEntries);

            Map<String, String> result = codelistDownloader.getLatestDestatisArsCodelist();

            assertThat("Result should not be null", result, is(notNullValue()));
            assertThat("Result should contain 3 entries", result.size(), is(3));
            assertThat(result, hasEntry("010010000000", "Flensburg"));
            assertThat(result, hasEntry("010020000000", "Kiel"));
            assertThat(result, hasEntry("021010101101", "Hamburg-Altstadt, OT 101"));
        }
    }

    @Test
    void getLatestDestatisArsCodelist_shouldReturnEmptyMapWhenDownloadFails() throws Exception {
        when(pvExternalDownloader.getCodelist(anyString())).thenThrow(
            new ExternalDownloadException("Download failed")
        );

        Map<String, String> result = codelistDownloader.getLatestDestatisArsCodelist();

        assertThat("Should return empty map when download fails", result, is(notNullValue()));
        assertThat("Map should be empty", result.isEmpty(), is(true));
        verify(pvExternalDownloader).getCodelist(
            "urn:de:bund:destatis:bevoelkerungsstatistik:schluessel:rs"
        );
    }

    @Test
    void getLatestDestatisArsCodelist_shouldReturnEmptyMapOnResourceNotFoundException()
        throws Exception {
        when(pvExternalDownloader.getCodelist(anyString())).thenThrow(
            new ResourceNotFoundException("Resource not found")
        );

        Map<String, String> result = codelistDownloader.getLatestDestatisArsCodelist();

        assertThat("Should return empty map when resource not found", result, is(notNullValue()));
        assertThat("Map should be empty", result.isEmpty(), is(true));
    }

    @Test
    void loadArsSeedIntoCache_shouldThrowExceptionWhenNoResourcesFound() {
        // This test verifies behavior when no seed files exist in classpath
        // In actual deployment, seed file must exist, but test validates error handling
        when(codelistService.parseCodelistDocument(any(Resource.class))).thenReturn(
            Collections.emptyList()
        );

        // The actual implementation would throw when resources.length == 0
        // but in test context with mocked service, we test the empty entries scenario
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            codelistDownloader.loadArsSeedIntoCache()
        );

        assertThat(
            exception.getMessage(),
            containsString("Codelisten ARS-Seed enthält keine Einträge:")
        );
    }

    @Test
    void loadArsSeedIntoCache_shouldVerifyCorrectCacheBehavior() {
        List<CodelistEntry> mockEntries = List.of(
            createMockEntry("010010000000", "Flensburg"),
            createMockEntry("010020000000", "Kiel"),
            createMockEntry("010030000000", "Lübeck"),
            createMockEntry("010040000000", "Neumünster")
        );
        when(codelistService.parseCodelistDocument(any(Resource.class))).thenReturn(mockEntries);

        Map<String, String> result = codelistDownloader.loadArsSeedIntoCache();

        assertThat("Result should have exactly 4 entries", result.size(), is(4));

        mockEntries.forEach(entry ->
            assertThat(result, hasEntry(entry.getCode(), entry.getText(Locale.GERMAN)))
        );
    }

    private int countOccurrences(String text, String search) {
        var count = 0;
        var index = 0;

        while ((index = text.indexOf(search, index)) >= 0) {
            count++;
            index += search.length();
        }

        return count;
    }
}

@SpringBootTest(
    classes = { CodelistDownloader.class, CodelistService.class, SuchdienstProperties.class }
)
@Import(CacheConfiguration.class)
class CodelistDownloaderCacheTest {

    @Autowired
    private CodelistDownloader codelistDownloader;

    @MockitoBean
    private PvExternalDownloaderCodelistService pvExternalDownloader;

    @MockitoBean
    private CodelistService codelistService;

    @MockitoBean
    private org.springframework.data.redis.connection.RedisConnectionFactory redisConnectionFactory;

    @Test
    void getLatestDestatisArsCodelist_shouldCacheDownloadedArsListWithoutRedis() throws Exception {
        CodeListDocument mockDocument = new CodeListDocument();
        CodelistEntry entry = new CodelistEntry();
        entry.setCode("010010000000");
        entry.setText(Locale.GERMAN, "Flensburg");
        when(pvExternalDownloader.getCodelist(anyString())).thenReturn(mockDocument);

        try (MockedStatic<CodelistParser> mockedParser = mockStatic(CodelistParser.class)) {
            mockedParser
                .when(() ->
                    CodelistParser.parseCodelistDocumentToEntries(any(CodeListDocument.class))
                )
                .thenReturn(List.of(entry));

            codelistDownloader.getLatestDestatisArsCodelist();
            codelistDownloader.getLatestDestatisArsCodelist(); // Wiederholung muss den Cache statt des Downloaders nutzen.

            verify(pvExternalDownloader).getCodelist(anyString());
        }
    }
}
