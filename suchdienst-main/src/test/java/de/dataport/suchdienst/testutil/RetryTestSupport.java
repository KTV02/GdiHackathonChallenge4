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
package de.dataport.suchdienst.testutil;

import co.elastic.clients.elasticsearch._types.ErrorResponse;
import de.dataport.suchdienst.config.RetryConfiguration;
import de.dataport.suchdienst.config.SuchdienstProperties;
import java.time.Duration;
import org.springframework.core.retry.RetryTemplate;

/**
 * Gemeinsame Factory-Methoden für Retry-nahe Tests.
 * Die Helper halten die Retry-Konfiguration in den Testklassen konsistent und bewusst sehr klein,
 * damit Backoff-basierte Tests deterministisch und schnell bleiben.
 */
public final class RetryTestSupport {

    private RetryTestSupport() {}

    public static RetryTemplate createRetryTemplate() {
        return createRetryTemplate(3);
    }

    public static RetryTemplate createRetryTemplate(int maxRetries) {
        SuchdienstProperties props = new SuchdienstProperties();
        props.getElastic().getRetry().setMaxRetries(maxRetries);
        props.getElastic().getRetry().setInitialInterval(Duration.ofMillis(1L));
        props.getElastic().getRetry().setMaxInterval(Duration.ofMillis(2L));
        props.getElastic().getRetry().setMultiplier(2d);
        return new RetryConfiguration().retry(props);
    }

    /**
     * Erstellt eine ElasticsearchException mit definierbarem HTTP-Status, damit die Predicate-Logik
     * der RetryConfiguration gezielt auf retrybare und nicht retrybare Statuscodes geprüft werden kann.
     */
    public static co.elastic.clients.elasticsearch._types.ElasticsearchException elasticsearchExceptionWithStatus(
        int status
    ) {
        ErrorResponse errorResponse = ErrorResponse.of(response ->
            response.status(status).error(error -> error.type("test_error").reason("test reason"))
        );
        return new co.elastic.clients.elasticsearch._types.ElasticsearchException(
            "testEndpoint",
            errorResponse
        );
    }
}
