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

import com.github.benmanes.caffeine.cache.Caffeine;
import de.dataport.suchdienst.config.CacheConfiguration;
import java.util.List;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public class TestCacheManager {

    @MockitoBean(name = CacheConfiguration.REDIS_MGR)
    protected CacheManager cacheManager;

    protected CacheManager simpleCache;

    protected static SimpleCacheManager getSimpleCache() {
        var inMem = new SimpleCacheManager();
        inMem.setCaches(
            List.of(
                new CaffeineCache(
                    CacheConfiguration.BKG_SUGGESTION_CACHE_NAME,
                    Caffeine.from("maximumSize=10,recordStats").build()
                ),
                new CaffeineCache(
                    CacheConfiguration.BKG_SEARCH_ADDRESS_CACHE_NAME,
                    Caffeine.from("maximumSize=10,recordStats").build()
                ),
                new CaffeineCache(
                    CacheConfiguration.BKG_SEARCH_ARS_CACHE_NAME,
                    Caffeine.from("maximumSize=10,recordStats").build()
                ),
                new CaffeineCache(
                    CacheConfiguration.BKG_SEARCH_QUERY_CACHE_NAME,
                    Caffeine.from("maximumSize=10,recordStats").build()
                )
            )
        );
        inMem.initializeCaches();
        return inMem;
    }
}
