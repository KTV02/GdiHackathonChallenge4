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

import static java.util.Map.entry;

import com.github.benmanes.caffeine.cache.Caffeine;
import de.dataport.suchdienst.util.Exceptions;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.cache.CacheType;
import org.springframework.boot.cache.autoconfigure.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.cache.support.NoOpCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheManager.RedisCacheManagerBuilder;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
@Slf4j
public class CacheConfiguration implements CachingConfigurer {

    public static final String IN_MEM_MGR = "inMemoryCacheManager";
    public static final String REDIS_MGR = "redisCacheManager";

    public static final String CACHE_NAME_CODELIST_DOWNLOAD = "codelistDownload";
    public static final String CACHE_NAME_CODELIST_ENTRIES = "codelistEntries";
    public static final String CACHE_NAME_PERSONAL_MATTERS = "personalMatters";

    // BKG Caches
    public static final String BKG_CACHE_NAME = "bkgGeoLocation";
    public static final String BKG_SEARCH_QUERY_CACHE_NAME = "bkgSearchQuery";
    public static final String BKG_SEARCH_ADDRESS_CACHE_NAME = "bkgSearchAddress";
    public static final String BKG_SEARCH_ARS_CACHE_NAME = "bkgSearchARS";
    public static final String BKG_SUGGESTION_CACHE_NAME = "bkgSuggestion";

    // CodelistDownloader Caches
    public static final String CODELIST_ARS_CACHE_NAME = "xoevArsList";
    public static final String DEFAULT_CACHE_CONFIGURATION =
        "maximumSize=1000,expireAfterWrite=5m,recordStats";

    private final CacheProperties cacheProps;
    private final SuchdienstProperties suchdienstProps;
    private final Environment environment;

    public CacheConfiguration(
        CacheProperties cacheProps,
        SuchdienstProperties suchdienstProps,
        Environment environment
    ) {
        this.cacheProps = cacheProps;
        this.suchdienstProps = suchdienstProps;
        this.environment = environment;
    }

    /**
     * Fängt Cache-Fehler ab, sodass die Anfragen vom eigentlichen Service beantwortet werden können.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                handleException(exception);
            }

            @Override
            public void handleCachePutError(
                RuntimeException exception,
                Cache cache,
                Object key,
                Object value
            ) {
                handleException(exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                handleException(exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                handleException(exception);
            }

            private void handleException(RuntimeException exception) {
                Exceptions.findCause(exception, InterruptedException.class).ifPresent(i ->
                    Thread.currentThread().interrupt()
                );
                log.warn("Fehler beim Cache-Zugriff - wird ignoriert.", exception);
            }
        };
    }

    /**
     * Erstellt einen CacheManager, der sowohl Redis als auch inMem Caches liefern kann. Dienst als
     * Fallback, wenn {@link Cacheable}-Deklarationen nicht explizit den Manager angeben
     *
     * @return einen CacheManager, der Zugriff auf Redis- und inMem-Caches bietet
     */
    @Bean
    @Primary
    public CacheManager cacheManager(
        @Qualifier(IN_MEM_MGR) CacheManager inMemCache,
        @Qualifier(REDIS_MGR) CacheManager redisCache
    ) {
        return new CompositeCacheManager(inMemCache, redisCache);
    }

    /**
     * CacheManager für den (direkten) Zugriff auf den inMem-Cache.
     *
     * @return den Manager für inMem-Caches
     */
    @Bean(IN_MEM_MGR)
    public CacheManager inMemCache() {
        SimpleCacheManager inMem = new SimpleCacheManager();
        inMem.setCaches(
            suchdienstProps
                .getInMemCache()
                .entrySet()
                .stream()
                .map(e -> {
                    if (StringUtils.isNotBlank(e.getValue())) {
                        return new CaffeineCache(e.getKey(), Caffeine.from(e.getValue()).build());
                    }
                    return new NoOpCache(e.getKey());
                })
                .toList()
        );
        return inMem;
    }

    /**
     * CacheManager für den (direkten) Zugriff auf Redis-Caches
     *
     * @return den Manager für Redis-Caches
     */
    @Bean(REDIS_MGR)
    public CacheManager redisCache(
        RedisCacheConfiguration redisCacheConfiguration,
        RedisConnectionFactory factory
    ) {
        if (cacheProps.getType() == CacheType.REDIS) {
            RedisCacheManagerBuilder builder = RedisCacheManager.builder(factory)
                .enableStatistics()
                .withInitialCacheConfigurations(
                    Map.ofEntries(
                        entry(CODELIST_ARS_CACHE_NAME, redisCacheConfiguration),
                        entry(BKG_CACHE_NAME, redisCacheConfiguration),
                        entry(BKG_SUGGESTION_CACHE_NAME, redisCacheConfiguration),
                        entry(BKG_SEARCH_ARS_CACHE_NAME, redisCacheConfiguration),
                        entry(BKG_SEARCH_QUERY_CACHE_NAME, redisCacheConfiguration),
                        entry(BKG_SEARCH_ADDRESS_CACHE_NAME, redisCacheConfiguration)
                    )
                );
            return builder.build();
        }

        if (environment.acceptsProfiles(Profiles.of("production"))) {
            log.warn("Redis ist deaktiviert. Fallback auf inMem-Cache. Cache-Config prüfen!");
        } else {
            log.info(
                "Redis ist deaktiviert. Für Entwicklungs- und Testzwecke wird ein inMem-Cache verwendet."
            );
        }

        SimpleCacheManager inMem = new SimpleCacheManager();
        inMem.setCaches(
            List.of(
                createLocalFallbackCache(
                    CODELIST_ARS_CACHE_NAME,
                    "maximumSize=1,expireAfterWrite=1h,recordStats"
                ),
                createLocalFallbackCache(BKG_CACHE_NAME, DEFAULT_CACHE_CONFIGURATION),
                createLocalFallbackCache(BKG_SUGGESTION_CACHE_NAME, DEFAULT_CACHE_CONFIGURATION),
                createLocalFallbackCache(BKG_SEARCH_ARS_CACHE_NAME, DEFAULT_CACHE_CONFIGURATION),
                createLocalFallbackCache(BKG_SEARCH_QUERY_CACHE_NAME, DEFAULT_CACHE_CONFIGURATION),
                createLocalFallbackCache(BKG_SEARCH_ADDRESS_CACHE_NAME, DEFAULT_CACHE_CONFIGURATION)
            )
        );
        return inMem;
    }

    private CaffeineCache createLocalFallbackCache(String name, String cacheConfiguration) {
        return new CaffeineCache(name, Caffeine.from(cacheConfiguration).build());
    }

    @Bean
    @Primary
    @Override
    public KeyGenerator keyGenerator() {
        return new SimpleKeyGenerator();
    }

    @Bean
    public RedisCacheConfiguration redisConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .serializeValuesWith(SerializationPair.fromSerializer(RedisSerializer.json()))
            .disableCachingNullValues()
            .disableKeyPrefix()
            .entryTtl(suchdienstProps.getBkg().getTtl());
    }
}
