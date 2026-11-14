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
package de.dataport.suchdienst.indexer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import de.dataport.suchdienst.elasticsearch.api.ElasticBulkWriteConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.error.ApiException;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ErrorTypeCodelist;
import de.dataport.suchdienst.error.model.ExceptionType;
import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.model.CodelistKey;
import de.dataport.suchdienst.util.MdcCleanupExtension;
import de.dataport.suchdienst.util.model.MdcKeys;
import de.dataport.suchdienst.util.model.ProtocolType;
import de.xrepository.genericodeservices.CodeListDocument;
import de.xrepository.genericodeservices.Identification;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith({ MockitoExtension.class, MdcCleanupExtension.class })
class CodelistServiceTest {

    @Mock
    private RestCodelistService restCodelistService;

    @Mock
    private RestCodelistService soapService;

    @Mock
    private ElasticBulkWriteConnector writeConnector;

    @Mock
    private ElasticReadConnector readConnector;

    private CodelistService service;

    @BeforeEach
    void setUp() {
        service = new CodelistService(
            restCodelistService,
            readConnector,
            writeConnector,
            soapService
        );
    }

    private CodeListDocument stubDocument(String uri, String version) {
        CodeListDocument doc = new CodeListDocument();
        Identification identification = new Identification();
        identification.setCanonicalUri(uri);
        identification.setVersion(version);
        doc.setIdentification(identification);
        return doc;
    }

    private CodelistEntry entry(String uri, String version, String code, String text) {
        CodelistEntry e = new CodelistEntry();
        e.setUri(uri);
        e.setVersion(version);
        e.setCode(code);
        e.setText(Locale.GERMAN, text);
        e.setId(uri + "_" + version + "_" + code);
        return e;
    }

    @Test
    void downloadCodelist_success_and_bulkIndex_called() {
        CodeListDocument doc = stubDocument("testUri", "v1");
        when(restCodelistService.downloadCodeMap("testUri", "v1", false)).thenReturn(doc);

        List<CodelistEntry> parsed = List.of(
            entry("testUri", "v1", "code1", "Text1"),
            entry("testUri", "v1", "code2", "Text2")
        );

        try (MockedStatic<CodelistParser> mocked = mockStatic(CodelistParser.class)) {
            mocked
                .when(() -> CodelistParser.parseCodelistDocumentToEntries(doc))
                .thenReturn(parsed);

            List<CodelistEntry> result = service.downloadCodelist("testUri", "v1");
            assertEquals(parsed, result);
            verify(writeConnector).bulkIndexThrowIfError(parsed, IndexNames.CODELISTENTRIES);
        }
    }

    @Test
    void downloadCodelist_version_resolved_when_empty() {
        CodeListDocument doc = stubDocument("uriX", "vCurrent");
        when(restCodelistService.getCurrentVersionNameFromCodelist("uriX")).thenReturn("vCurrent");
        when(restCodelistService.downloadCodeMap("uriX", "vCurrent", false)).thenReturn(doc);

        List<CodelistEntry> parsed = List.of(entry("uriX", "vCurrent", "code", "Text"));

        try (MockedStatic<CodelistParser> mocked = mockStatic(CodelistParser.class)) {
            mocked
                .when(() -> CodelistParser.parseCodelistDocumentToEntries(doc))
                .thenReturn(parsed);
            List<CodelistEntry> result = service.downloadCodelist("uriX", "", false);
            assertEquals(1, result.size());
            assertEquals("vCurrent", result.getFirst().getVersion());
            verify(restCodelistService).getCurrentVersionNameFromCodelist("uriX");
        }
    }

    @Test
    void downloadCodelist_null_document_results_in_empty_list_and_cached() {
        when(restCodelistService.downloadCodeMap("u", "1", false)).thenReturn(null);
        List<CodelistEntry> first = service.downloadCodelist("u", "1", false);
        assertTrue(first.isEmpty());
        // Zweiter Aufruf: sollte wegen Cache nicht nochmal downloaden
        List<CodelistEntry> second = service.downloadCodelist("u", "1", false);
        assertTrue(second.isEmpty());
        verify(restCodelistService, times(1)).downloadCodeMap("u", "1", false);
        verifyNoInteractions(writeConnector);
    }

    @Test
    void downloadCodelist_propagates_exception() {
        when(restCodelistService.downloadCodeMap("test", "123", false)).thenThrow(
            new ApiException(ErrorCodeEnum.NOT_FOUND, "", ExceptionType.TECHNISCH)
        );
        ApiException ex = assertThrows(ApiException.class, () ->
            service.downloadCodelist("test", "123")
        );
        assertEquals(ErrorCodeEnum.NOT_FOUND, ex.getCode());
    }

    @Test
    void downloadCodelist_exception_transportiert_extern_mdc_und_stellt_admin_wieder_her() {
        MDC.put(MdcKeys.PROTOKOLLTYP, ProtocolType.ADMIN.name());
        when(restCodelistService.downloadCodeMap("test", "123", false)).thenAnswer(_ -> {
            throw new ApiException(ErrorCodeEnum.NOT_FOUND, "", ExceptionType.TECHNISCH);
        });

        ApiException ex = assertThrows(ApiException.class, () ->
            service.downloadCodelist("test", "123")
        );

        assertEquals(ProtocolType.EXTERN.name(), ex.getMdcContext().get(MdcKeys.PROTOKOLLTYP));
        assertEquals(ProtocolType.ADMIN.name(), MDC.get(MdcKeys.PROTOKOLLTYP));
    }

    @Test
    void getCodelistEntry_found_in_elastic() {
        CodelistEntry e = entry("uri", "v", "c", "Txt");
        when(readConnector.getCodelistEntry(new CodelistKey("uri", "v", "c"))).thenReturn(e);
        CodelistEntry result = service.getCodelistEntry("uri", "v", "c");
        assertSame(e, result);
        verify(readConnector, times(1)).getCodelistEntry(new CodelistKey("uri", "v", "c"));
        verifyNoMoreInteractions(readConnector);
        verifyNoInteractions(writeConnector, restCodelistService);
    }

    @Test
    void getCodelistEntry_list_exists_cannot_connect_returns_error_code() {
        when(readConnector.getCodelistEntry(new CodelistKey("uri", "v", "x"))).thenReturn(null);
        when(readConnector.codeListExists("uri", "v")).thenReturn(true);
        when(soapService.canConnectToXRepository()).thenReturn(false);

        CodelistEntry result = service.getCodelistEntry("uri", "v", "x");
        assertTrue(result.isNonExistent());
        assertEquals(ErrorTypeCodelist.CODE, result.getNonExistentCode());
    }

    @Test
    void getCodelistEntry_list_not_exists_can_connect_downloads_and_finds_code() {
        when(readConnector.getCodelistEntry(new CodelistKey("u", "v", "c1"))).thenReturn(null);
        when(readConnector.codeListExists("u", "v")).thenReturn(false);
        when(soapService.canConnectToXRepository()).thenReturn(true);

        CodeListDocument doc = stubDocument("u", "v");
        when(restCodelistService.downloadCodeMap("u", "v", true)).thenReturn(doc);
        List<CodelistEntry> parsed = List.of(
            entry("u", "v", "c1", "T1"),
            entry("u", "v", "other", "T2")
        );
        try (MockedStatic<CodelistParser> mocked = mockStatic(CodelistParser.class)) {
            mocked
                .when(() -> CodelistParser.parseCodelistDocumentToEntries(doc))
                .thenReturn(parsed);
            CodelistEntry result = service.getCodelistEntry("u", "v", "c1");
            assertEquals("c1", result.getCode());
            assertEquals("T1", result.getText(Locale.GERMAN));
            verify(writeConnector).bulkIndexThrowIfError(parsed, IndexNames.CODELISTENTRIES);
        }
    }

    @Test
    void getCodelistEntry_list_not_exists_can_connect_downloads_missing_code_but_entries_present() {
        when(readConnector.getCodelistEntry(new CodelistKey("u2", "v2", "missing"))).thenReturn(
            null
        );
        when(readConnector.codeListExists("u2", "v2")).thenReturn(false);
        when(soapService.canConnectToXRepository()).thenReturn(true);
        CodeListDocument doc = stubDocument("u2", "v2");
        when(restCodelistService.downloadCodeMap("u2", "v2", true)).thenReturn(doc);
        List<CodelistEntry> parsed = List.of(entry("u2", "v2", "other", "Txt"));
        try (MockedStatic<CodelistParser> mocked = mockStatic(CodelistParser.class)) {
            mocked
                .when(() -> CodelistParser.parseCodelistDocumentToEntries(doc))
                .thenReturn(parsed);
            CodelistEntry result = service.getCodelistEntry("u2", "v2", "missing");
            assertTrue(result.isNonExistent());
            assertEquals(ErrorTypeCodelist.CODE, result.getNonExistentCode());
        }
    }

    @Test
    void getCodelistEntry_list_not_exists_can_connect_downloads_empty_list() {
        when(readConnector.getCodelistEntry(new CodelistKey("u3", "v3", "c"))).thenReturn(null);
        when(readConnector.codeListExists("u3", "v3")).thenReturn(false);
        when(soapService.canConnectToXRepository()).thenReturn(true);
        CodeListDocument doc = stubDocument("u3", "v3");
        when(restCodelistService.downloadCodeMap("u3", "v3", true)).thenReturn(doc);
        List<CodelistEntry> parsed = List.of();
        try (MockedStatic<CodelistParser> mocked = mockStatic(CodelistParser.class)) {
            mocked
                .when(() -> CodelistParser.parseCodelistDocumentToEntries(doc))
                .thenReturn(parsed);
            CodelistEntry result = service.getCodelistEntry("u3", "v3", "c");
            assertTrue(result.isNonExistent());
            assertEquals(ErrorTypeCodelist.LIST, result.getNonExistentCode());
        }
    }

    @Test
    void saveCodelistInElasticSearch_delegates() {
        List<CodelistEntry> entries = List.of(entry("a", "1", "c", "t"));
        service.saveCodelistInElasticSearch(entries);
        verify(writeConnector).bulkIndexThrowIfError(entries, IndexNames.CODELISTENTRIES);
    }
}
