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
package de.dataport.suchdienst.geo.service;

import de.dataport.pvog.pvexternaldownloader.service.ExternalDownloadException;
import de.dataport.pvog.pvexternaldownloader.service.PvExternalDownloaderCodelistService;
import de.dataport.pvog.pvexternaldownloader.service.ResourceNotFoundException;
import de.dataport.suchdienst.config.CacheConfiguration;
import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.service.CodelistParser;
import de.dataport.suchdienst.indexer.service.CodelistService;
import de.dataport.suchdienst.util.Exceptions;
import de.dataport.suchdienst.util.MdcTemplate;
import de.dataport.suchdienst.util.model.ProtocolType;
import de.xrepository.genericodeservices.CodeListDocument;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CodelistDownloader {

    // Wird in den Caching-Annotationen in dieser Klasse verwendet. Darf also nicht entfernt werden, selbst wenn die IDE "no usages" meldet.
    public static final String ARS_CACHE_KEY = "ars";
    public static final String REDUCED_ARS_CACHE_KEY = "arsWithoutOrtsteileOrStadtbezirke";
    private static final String BEVOELKERUNGSSTATISTIK_RS_URI =
        "urn:de:bund:destatis:bevoelkerungsstatistik:schluessel:rs";
    private static final String ARS_HINT_COLUMN = "Hinweis";
    private static final String STADT_ORTSTEIL_HINT = "Stadt-/Ortsteil";
    private static final String STADTBEZIRK_HINT = "Stadtbezirk";

    private final PvExternalDownloaderCodelistService downloaderCodelistService;
    private final CodelistService codelistService;
    private final CacheManager redisCacheManager;

    public CodelistDownloader(
        PvExternalDownloaderCodelistService downloaderCodelistService,
        CodelistService codelistService,
        @Qualifier(CacheConfiguration.REDIS_MGR) CacheManager redisCacheManager
    ) {
        this.downloaderCodelistService = downloaderCodelistService;
        this.codelistService = codelistService;
        this.redisCacheManager = redisCacheManager;
    }

    @Cacheable(
        cacheNames = CacheConfiguration.CODELIST_ARS_CACHE_NAME,
        key = "#root.target.ARS_CACHE_KEY",
        unless = "#result==null or #result.isEmpty()",
        cacheManager = CacheConfiguration.REDIS_MGR
    )
    public Map<String, String> getLatestDestatisArsCodelist() {
        return getLatestBevoelkerungsstatistikRs();
    }

    @Cacheable(
        cacheNames = CacheConfiguration.CODELIST_ARS_CACHE_NAME,
        key = "#root.target.ARS_CACHE_KEY",
        unless = "#result==null or #result.isEmpty()",
        cacheManager = CacheConfiguration.REDIS_MGR
    )
    public Map<String, String> getCachedDestatisArsCodelist() {
        // Bei einem Cache-Miss soll diese Methode bewusst keinen externen Download auslösen.
        return Collections.emptyMap();
    }

    @Cacheable(
        cacheNames = CacheConfiguration.CODELIST_ARS_CACHE_NAME,
        key = "#root.target.REDUCED_ARS_CACHE_KEY",
        unless = "#result==null or #result.isEmpty()",
        cacheManager = CacheConfiguration.REDIS_MGR
    )
    public Map<String, String> getCachedReducedDestatisArsCodelist() {
        // Bei einem Cache-Miss soll diese Methode bewusst keinen externen Download auslösen.
        return Collections.emptyMap();
    }

    @CachePut(
        cacheNames = CacheConfiguration.CODELIST_ARS_CACHE_NAME,
        key = "#root.target.ARS_CACHE_KEY",
        unless = "#result==null or #result.isEmpty()",
        cacheManager = CacheConfiguration.REDIS_MGR
    )
    public Map<String, String> updateDestatisArsCodelistCache() {
        return getLatestBevoelkerungsstatistikRs();
    }

    public Map<String, String> updateDestatisArsCodelistCaches() {
        return downloadLatestVersion(BEVOELKERUNGSSTATISTIK_RS_URI)
            .map(CodelistParser::parseCodelistDocumentToEntries)
            .map(this::updateArsCacheViews)
            .orElse(Collections.emptyMap());
    }

    /**
     * Loads the ARS codelist seed file from classpath resources and stores it in Redis cache.
     * This provides a fallback when external XRepository download fails.
     *
     * @return Map of ARS codes to German names
     * @throws IllegalStateException if no seed file is found in resources
     */
    @CachePut(
        cacheNames = CacheConfiguration.CODELIST_ARS_CACHE_NAME,
        key = "#root.target.ARS_CACHE_KEY",
        cacheManager = CacheConfiguration.REDIS_MGR
    )
    public Map<String, String> loadArsSeedIntoCache() {
        return toArsMap(loadArsSeedEntries());
    }

    public Map<String, String> loadReducedArsSeedIntoCache() {
        return updateArsCacheViews(loadArsSeedEntries());
    }

    private Collection<CodelistEntry> loadArsSeedEntries() {
        try {
            PathMatchingResourcePatternResolver resolver =
                new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(
                "classpath:cache-seed/urn_de_bund_destatis_bevoelkerungsstatistik_schluessel_rs_*.xml"
            );

            if (resources.length != 1) {
                throw new IllegalStateException(
                    "Keine oder mehrere Codelisten ARS-Seed in classpath:cache-seed/ gefunden. " +
                        "Deployment muss eine Datei 'urn_de_bund_destatis_bevoelkerungsstatistik_schluessel_rs_YYYYMMDD.xml' enthalten."
                );
            }

            var seedResource = resources[0];
            log.info("Befülle Codelisten ARS-Seed mit: {}", seedResource.getFilename());
            var entries = codelistService.parseCodelistDocument(seedResource);

            if (entries.isEmpty()) {
                throw new IllegalStateException(
                    "Codelisten ARS-Seed enthält keine Einträge: " + seedResource.getFilename()
                );
            }

            log.info(
                "Codelisten ARS-Seed erfolgreich geladen: {} Einträge aus Datei {}",
                entries.size(),
                seedResource.getFilename()
            );

            return entries;
        } catch (IOException e) {
            throw new IllegalStateException(
                "Fehler beim Laden des Codelisten ARS-Seed aus classpath:cache-seed/",
                e
            );
        }
    }

    private Map<String, String> getLatestBevoelkerungsstatistikRs() {
        return downloadLatestVersion(BEVOELKERUNGSSTATISTIK_RS_URI)
            .map(CodelistParser::parseCodelistDocumentToEntries)
            .map(this::toArsMap)
            .orElse(Collections.emptyMap());
    }

    private Map<String, String> toArsMap(Collection<CodelistEntry> entries) {
        return entries
            .stream()
            .collect(
                Collectors.toMap(CodelistEntry::getCode, entry -> entry.getText(Locale.GERMAN))
            );
    }

    private Map<String, String> toReducedArsMap(Collection<CodelistEntry> entries) {
        warnIfHinweisColumnIsMissing(entries);

        return entries
            .stream()
            .filter(CodelistDownloader::isNotOrtsteilOrStadtbezirk)
            .collect(
                Collectors.toMap(CodelistEntry::getCode, entry -> entry.getText(Locale.GERMAN))
            );
    }

    private void warnIfHinweisColumnIsMissing(Collection<CodelistEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }

        var hasHinweisColumn = entries
            .stream()
            .anyMatch(entry -> entry.getValue(ARS_HINT_COLUMN).isPresent());

        if (!hasHinweisColumn) {
            log.warn(
                "ARS-Codeliste enthält keine Spalte '{}'. Ortsteile und Stadtbezirke können nicht herausgefiltert werden.",
                ARS_HINT_COLUMN
            );
        }
    }

    private Map<String, String> updateArsCacheViews(Collection<CodelistEntry> entries) {
        var arsMap = toArsMap(entries);
        var reducedArsMap = toReducedArsMap(entries);

        putArsCache(ARS_CACHE_KEY, arsMap);
        putArsCache(REDUCED_ARS_CACHE_KEY, reducedArsMap);

        return reducedArsMap;
    }

    private void putArsCache(String cacheKey, Map<String, String> value) {
        if (value == null || value.isEmpty()) {
            return;
        }

        try {
            var cache = redisCacheManager.getCache(CacheConfiguration.CODELIST_ARS_CACHE_NAME);
            if (cache == null) {
                log.warn(
                    "ARS-Cache {} konnte nicht gefunden werden",
                    CacheConfiguration.CODELIST_ARS_CACHE_NAME
                );
                return;
            }
            cache.put(cacheKey, value);
        } catch (RuntimeException e) {
            Exceptions.findCause(e, InterruptedException.class).ifPresent(_ ->
                Thread.currentThread().interrupt()
            );
            log.warn("Fehler beim Aktualisieren des ARS-Caches für Key {}", cacheKey, e);
        }
    }

    private static boolean isNotOrtsteilOrStadtbezirk(CodelistEntry entry) {
        String hinweis = entry
            .getValue(ARS_HINT_COLUMN)
            .map(CodelistEntry.CodeListValue::value)
            .orElse("");

        return (
            !Strings.CI.contains(hinweis, STADT_ORTSTEIL_HINT) &&
            !Strings.CI.contains(hinweis, STADTBEZIRK_HINT)
        );
    }

    private Optional<CodeListDocument> downloadLatestVersion(String listUri) {
        try (var _ = MdcTemplate.withProtokolltyp(ProtocolType.EXTERN)) {
            try {
                log.info("Download der ARS-Liste {} gestartet", listUri);
                return Optional.of(this.downloaderCodelistService.getCodelist(listUri));
            } catch (ExternalDownloadException | ResourceNotFoundException e) {
                log.warn("Fehler beim Download von ARS Liste", e);
                return Optional.empty();
            }
        }
    }
}
