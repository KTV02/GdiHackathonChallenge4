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
package de.dataport.suchdienst.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.cache.CacheType;
import org.springframework.boot.cache.autoconfigure.CacheProperties;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.mock.env.MockEnvironment;

@ExtendWith(OutputCaptureExtension.class)
class CacheConfigurationTest {

    @Test
    void redisCache_shouldUseLocalCachesForAllRedisCacheNamesWhenRedisIsDisabled() {
        CacheProperties cacheProperties = new CacheProperties();
        cacheProperties.setType(CacheType.NONE);
        SuchdienstProperties suchdienstProperties = new SuchdienstProperties();
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        CacheConfiguration cacheConfiguration = new CacheConfiguration(
            cacheProperties,
            suchdienstProperties,
            environment
        );

        CacheManager fallbackCache = cacheConfiguration.redisCache(
            RedisCacheConfiguration.defaultCacheConfig(),
            org.mockito.Mockito.mock(RedisConnectionFactory.class)
        );
        ((SimpleCacheManager) fallbackCache).initializeCaches();

        assertThat(
            fallbackCache.getCacheNames(),
            containsInAnyOrder(
                CacheConfiguration.CODELIST_ARS_CACHE_NAME,
                CacheConfiguration.BKG_CACHE_NAME,
                CacheConfiguration.BKG_SUGGESTION_CACHE_NAME,
                CacheConfiguration.BKG_SEARCH_ARS_CACHE_NAME,
                CacheConfiguration.BKG_SEARCH_QUERY_CACHE_NAME,
                CacheConfiguration.BKG_SEARCH_ADDRESS_CACHE_NAME
            )
        );

        Cache arsCache = fallbackCache.getCache(CacheConfiguration.CODELIST_ARS_CACHE_NAME);
        arsCache.put("ars", Map.of("010010000000", "Flensburg"));

        Cache.ValueWrapper cachedValue = arsCache.get("ars");
        assertThat(cachedValue, is(notNullValue()));
        assertThat(cachedValue.get(), is(Map.of("010010000000", "Flensburg")));
    }

    @Test
    void redisCache_shouldUseLocalCachesAndWarnInProductionWhenRedisIsDisabled(
        CapturedOutput output
    ) {
        CacheProperties cacheProperties = new CacheProperties();
        cacheProperties.setType(CacheType.NONE);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        CacheConfiguration cacheConfiguration = new CacheConfiguration(
            cacheProperties,
            new SuchdienstProperties(),
            environment
        );

        CacheManager fallbackCache = cacheConfiguration.redisCache(
            RedisCacheConfiguration.defaultCacheConfig(),
            org.mockito.Mockito.mock(RedisConnectionFactory.class)
        );
        ((SimpleCacheManager) fallbackCache).initializeCaches();

        assertThat(
            fallbackCache.getCache(CacheConfiguration.CODELIST_ARS_CACHE_NAME),
            is(notNullValue())
        );
        assertThat(
            output.toString(),
            containsString("Redis ist deaktiviert. Fallback auf inMem-Cache. Cache-Config prüfen!")
        );
    }
}
