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

import static java.util.concurrent.TimeUnit.SECONDS;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.dataport.suchdienst.elasticsearch.api.ElasticBulkWriteConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.elasticsearch.api.exception.ElasticSearchException;
import de.dataport.suchdienst.error.model.ErrorTypeCodelist;
import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.model.CodelistKey;
import de.dataport.suchdienst.util.Exceptions;
import de.dataport.suchdienst.util.MdcTemplate;
import de.dataport.suchdienst.util.model.MdcKeys;
import de.dataport.suchdienst.util.model.ProtocolType;
import de.xrepository.genericodeservices.CodeListDocument;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CodelistService {

    private final RestCodelistService restCodelistService;
    private final ElasticBulkWriteConnector writeConnector;
    private final ElasticReadConnector readConnector;
    private final RestCodelistService soapService;
    private final Cache<String, String> notFoundExpireMap;

    public CodelistService(
        RestCodelistService restCodelistService,
        ElasticReadConnector readConnector,
        ElasticBulkWriteConnector writeConnector,
        RestCodelistService soapService
    ) {
        this.restCodelistService = restCodelistService;
        this.readConnector = readConnector;
        this.writeConnector = writeConnector;
        this.soapService = soapService;
        this.notFoundExpireMap = Caffeine.newBuilder().expireAfterWrite(180, SECONDS).build();
    }

    public List<CodelistEntry> parseCodelistDocument(Resource resource) {
        try (InputStream is = resource.getInputStream()) {
            return CodelistParser.parseCodelistDocument(is);
        } catch (IOException e) {
            //PV-8946 catch-rethrow
            // Unchecked Exception zur Verwendung in Java-Streams
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Lädt anhand der URI und der Version eine Codeliste von xRepository herunter und fügt sie dem
     * Index codelistentries hinzu. Fehler beim Download der Codeliste werden als Exception geworfen.
     *
     * @param uri     URI der gewünschten Codeliste
     * @param version Version der gewünschten Codeliste. Wenn dieser Wert nicht vorhanden ist, dann wird
     *                die aktuelle Version ermittelt.
     * @see #downloadCodelist(String, String, boolean)
     */
    public List<CodelistEntry> downloadCodelist(String uri, String version) {
        return downloadCodelist(uri, version, false);
    }

    /**
     * Lädt anhand der URI und der Version eine Codeliste von xRepository herunter und fügt sie dem
     * Index codelistentries hinzu.
     *
     * @param uri                   URI der gewünschten Codeliste
     * @param version               Version der gewünschten Codeliste. Wenn dieser Wert nicht vorhanden ist, dann wird
     *                              die aktuelle Version ermittelt.
     * @param ignoreDownloadFailure falls {@literal true} werden sämtliche Fehler ignoriert, die beim
     *                              Download der Codeliste vom XRepo passieren koennen.
     * @return Eine Liste von CodelistEntries die heruntergeladen wurde oder eine leere Liste, falls
     * {@literal ignoreDownloadFailure=true} und Fehler beim Download passieren sind
     */
    public List<CodelistEntry> downloadCodelist(
        String uri,
        String version,
        boolean ignoreDownloadFailure
    ) {
        try (var _ = MdcTemplate.withProtokolltyp(ProtocolType.EXTERN)) {
            if (notFoundExpireMap.getIfPresent(uri + version) != null) {
                // Die ExpireMap verhindert, dass bei häufiger Referenzierung der gleichen, falschen Codeliste,
                // wiederholt die gleiche Warnung ausgegeben wird.
                return List.of();
            }

            if (version == null || version.isEmpty()) {
                version = restCodelistService.getCurrentVersionNameFromCodelist(uri);
            }

            CodeListDocument codeListDocument = restCodelistService.downloadCodeMap(
                uri,
                version,
                ignoreDownloadFailure
            );
            if (codeListDocument == null) {
                log.warn(
                    "Fehler beim Laden der Codeliste {}, Version {}. Es wird mit leerer Liste weitergearbeitet.",
                    uri,
                    version
                );
                notFoundExpireMap.put(uri + version, "not_found");
                return List.of();
            }
            List<CodelistEntry> entries = CodelistParser.parseCodelistDocumentToEntries(
                codeListDocument
            );

            saveCodelistInElasticSearch(entries);

            return entries;
        }
    }

    public Optional<CodelistEntry> getCodelistById(String id) {
        return readConnector.getById(IndexNames.CODELISTENTRIES, id, CodelistEntry.class);
    }

    public List<CodelistEntry> getCodelistEntries(String uri, String version) {
        var entriesFromEs = readConnector.getCodeListEntries(uri, version);
        if (entriesFromEs.isEmpty()) {
            return downloadCodelist(uri, version, true);
        }
        return entriesFromEs;
    }

    /**
     * Methode zum Auflösen von Codelisten-Einträgen zur Index-Zeit.
     * Lädt einen Codelist Eintrag aus Elasticsearch. Ist in Elasticsearch kein Eintrag vorhanden,
     * so kann die Codeliste vom XRepository heruntergeladen und in Elasticsearch gespeichert werden.
     */
    @NonNull
    public CodelistEntry getCodelistEntry(String uri, String version, String code) {
        // Fehlerbehandlung: Wenn nicht festgestellt werden kann, ob der Codelisten-Eintrag existiert, loggen wir dies und laden sie danach ggf.
        // herunter.
        CodelistEntry codelistEntry = null;
        try {
            codelistEntry = readConnector.getCodelistEntry(new CodelistKey(uri, version, code));
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
        if (codelistEntry == null) {
            boolean listExists;
            try {
                listExists = readConnector.codeListExists(uri, version);
            } catch (ElasticSearchException e) {
                log.warn(e.getMessage(), e);
                listExists = false;
            }

            if (soapService.canConnectToXRepository() && !listExists) {
                codelistEntry = downloadCodelistAndCreateEntry(uri, version, code);
            } else {
                ErrorTypeCodelist errorType = listExists
                    ? ErrorTypeCodelist.CODE
                    : ErrorTypeCodelist.LIST;
                codelistEntry = CodelistEntry.empty(code, uri, version, errorType);
            }
        }
        return codelistEntry;
    }

    private CodelistEntry downloadCodelistAndCreateEntry(String uri, String version, String code) {
        CodelistEntry codelistEntry;
        try {
            List<CodelistEntry> entries = downloadCodelist(uri, version, true);
            ErrorTypeCodelist errorType = entries.isEmpty()
                ? ErrorTypeCodelist.LIST
                : ErrorTypeCodelist.CODE;
            codelistEntry = entries
                .stream()
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElseGet(() -> CodelistEntry.empty(code, uri, version, errorType));
        } catch (Exception e) {
            // PV-8946 catch-log-skip
            Exceptions.findCause(e, InterruptedException.class).ifPresent(ex ->
                Thread.currentThread().interrupt()
            );
            MdcTemplate.runWith(MdcKeys.PROTOKOLLTYP, ProtocolType.EXTERN.name(), () ->
                log.warn(
                    "EXTERN - FEHLER: Fehler beim Herunterladen einer Codeliste, Codes werden nicht aufgelöst.",
                    e
                )
            );
            codelistEntry = CodelistEntry.empty(code, uri, version, ErrorTypeCodelist.LIST);
        }
        return codelistEntry;
    }

    /**
     * Importiert die übergebene Codeliste in ElasticSearch.
     *
     * @param xmlCodeList Die zu importierende Codeliste als InputStream
     * @return Liste an verarbeiten {@link CodelistEntry}
     */
    public List<CodelistEntry> saveCodelistInElasticSearch(InputStream xmlCodeList) {
        List<CodelistEntry> entries = CodelistParser.parseCodelistDocument(xmlCodeList);
        saveCodelistInElasticSearch(entries);
        return entries;
    }

    /**
     * Speichert die Codeliste im Elasticsearch, ist eine Codeliste unter der ID bereits vorhanden,
     * wird sie überschrieben
     */
    public void saveCodelistInElasticSearch(List<CodelistEntry> entries) {
        writeConnector.bulkIndexThrowIfError(entries, IndexNames.CODELISTENTRIES);
    }
}
