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

import de.dataport.suchdienst.config.SuchdienstProperties.Elastic;
import de.dataport.suchdienst.elasticsearch.api.exception.ElasticsearchConnectionException;
import de.dataport.suchdienst.util.Exceptions;
import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;

@Configuration
public class RetryConfiguration {

    @Bean(name = "suchdienstRetryTemplate")
    public RetryTemplate retry(SuchdienstProperties props) {
        Elastic elastic = props.getElastic();
        return new RetryTemplate(
            RetryPolicy.builder()
                .maxRetries(Math.max(0L, elastic.getRetry().getMaxRetries()))
                .delay(elastic.getRetry().getInitialInterval())
                .jitter(elastic.getRetry().getInitialInterval())
                .multiplier(elastic.getRetry().getMultiplier())
                .maxDelay(elastic.getRetry().getMaxInterval())
                .predicate(new ElasticSearchRetryPredicate())
                .build()
        );
    }

    @Slf4j
    static class ElasticSearchRetryPredicate implements Predicate<Throwable> {

        @Override
        public boolean test(Throwable t) {
            return Exceptions.findCause(t, InterruptedException.class)
                .map(_ -> {
                    Thread.currentThread().interrupt();
                    log.warn("Kein Retry wegen Thread interrupt");
                    return false;
                })
                .or(() ->
                    Exceptions.findCause(
                        t,
                        co.elastic.clients.elasticsearch._types.ElasticsearchException.class
                    ).map(this::isRetryableElasticsearchException)
                )
                .or(() ->
                    Exceptions.findCause(t, IOException.class).map(io -> {
                        log.debug("Retry wegen IOException {}", io.getMessage(), io);
                        return true;
                    })
                )
                .or(() ->
                    Exceptions.findCause(t, ElasticsearchConnectionException.class).map(ec -> {
                        log.debug(
                            "Retry wegen ElasticsearchConnectionException {}",
                            ec.getMessage(),
                            ec
                        );
                        return true;
                    })
                )
                .orElseGet(() -> {
                    log.warn("Kein Retry wegen unbekanntem Fehler {}", t.getMessage(), t);
                    return false;
                });
        }

        private boolean isRetryableElasticsearchException(
            co.elastic.clients.elasticsearch._types.ElasticsearchException e
        ) {
            List<Integer> errorCodesToRetry = List.of(
                408, // REQUEST_TIMEOUT
                429, // TOO_MANY_REQUESTS
                500, // INTERNAL_SERVER_ERROR
                503, // SERVICE_UNAVAILABLE
                504, // GATEWAY_TIMEOUT
                507 // INSUFFICIENT_STORAGE
            );
            var status = e.status();
            if (errorCodesToRetry.contains(status)) {
                log.warn(
                    "Retry wegen ElasticSearch Status {} bei Exception {}",
                    status,
                    e.getMessage(),
                    e
                );
                return true;
            } else {
                log.warn(
                    "Kein Retry wegen ElasticSearch Status {} bei Exception {}",
                    status,
                    e.getMessage(),
                    e
                );
                return false;
            }
        }
    }
}
