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

import java.util.Map;
import org.springframework.boot.autoconfigure.cache.CacheType;
import org.springframework.boot.cache.autoconfigure.CacheProperties;
import org.springframework.boot.data.redis.health.DataRedisHealthIndicator;
import org.springframework.boot.health.autoconfigure.contributor.CompositeHealthContributorConfiguration;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Sorgt dafuer, dass wir Redis nur ueberwachen, wenn wir das auch zum Cachen benutzen
 *
 * @see CompositeHealthContributorConfiguration
 */
@Configuration
public class RedisHealthConfiguration
    extends CompositeHealthContributorConfiguration<
        DataRedisHealthIndicator,
        RedisConnectionFactory
    >
{

    private final Map<String, RedisConnectionFactory> redisConnectionFactories;
    private final CacheProperties cacheSettings;

    public RedisHealthConfiguration(
        Map<String, RedisConnectionFactory> redisConnectionFactories,
        CacheProperties cacheSettings
    ) {
        super(DataRedisHealthIndicator::new);
        this.cacheSettings = cacheSettings;
        this.redisConnectionFactories = redisConnectionFactories;
    }

    @Bean
    public HealthContributor redisHealthIndicator() {
        if (cacheSettings.getType() == CacheType.REDIS) {
            return createContributor(this.redisConnectionFactories);
        } else {
            return null;
        }
    }
}
