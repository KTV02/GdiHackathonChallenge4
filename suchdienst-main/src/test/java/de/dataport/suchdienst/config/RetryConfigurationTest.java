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
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.dataport.suchdienst.elasticsearch.api.exception.ElasticsearchConnectionException;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.testutil.RetryTestSupport;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryTemplate;

class RetryConfigurationTest {

    @AfterEach
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void clearThreadInterruptStatus() {
        Thread.interrupted();
    }

    @Test
    void retriesIoExceptionUpToMaxAttempts() {
        RetryTemplate retryTemplate = RetryTestSupport.createRetryTemplate();
        AtomicInteger attempts = new AtomicInteger();

        RetryException exception = assertThrows(RetryException.class, () ->
            retryTemplate.execute(() -> {
                attempts.incrementAndGet();
                throw new IOException("IO failure");
            })
        );

        assertThat(exception.getCause(), instanceOf(IOException.class));
        assertThat(exception.getCause().getMessage(), is("IO failure"));
        assertThat(attempts.get(), is(4));
    }

    @Test
    void retriesElasticsearchConnectionExceptionUpToMaxAttempts() {
        RetryTemplate retryTemplate = RetryTestSupport.createRetryTemplate();
        AtomicInteger attempts = new AtomicInteger();

        RetryException exception = assertThrows(RetryException.class, () ->
            retryTemplate.execute(() -> {
                attempts.incrementAndGet();
                throw new ElasticsearchConnectionException(
                    ErrorCodeEnum.ELSE_IO_ERROR,
                    "Connection failure"
                );
            })
        );

        assertThat(exception.getCause(), instanceOf(ElasticsearchConnectionException.class));
        assertThat(
            ((ElasticsearchConnectionException) exception.getCause()).getCode(),
            is(ErrorCodeEnum.ELSE_IO_ERROR)
        );
        assertThat(attempts.get(), is(4));
    }

    @ParameterizedTest
    @ValueSource(ints = { 408, 429, 500, 503, 504, 507 })
    void retriesRetryableElasticsearchStatusUpToMaxAttempts(int status) {
        RetryTemplate retryTemplate = RetryTestSupport.createRetryTemplate();
        AtomicInteger attempts = new AtomicInteger();

        RetryException exception = assertThrows(RetryException.class, () ->
            retryTemplate.execute(() -> {
                attempts.incrementAndGet();
                throw RetryTestSupport.elasticsearchExceptionWithStatus(status);
            })
        );

        assertThat(
            exception.getCause(),
            instanceOf(co.elastic.clients.elasticsearch._types.ElasticsearchException.class)
        );
        assertThat(
            (
                (co.elastic.clients.elasticsearch._types.ElasticsearchException) exception.getCause()
            ).status(),
            is(status)
        );
        assertThat(attempts.get(), is(4));
    }

    @ParameterizedTest
    @ValueSource(ints = { 400, 401, 403, 404, 409, 422 })
    void doesNotRetryNonRetryableElasticsearchStatus(int status) {
        RetryTemplate retryTemplate = RetryTestSupport.createRetryTemplate();
        AtomicInteger attempts = new AtomicInteger();

        RetryException exception = assertThrows(RetryException.class, () ->
            retryTemplate.execute(() -> {
                attempts.incrementAndGet();
                throw RetryTestSupport.elasticsearchExceptionWithStatus(status);
            })
        );

        assertThat(
            exception.getCause(),
            instanceOf(co.elastic.clients.elasticsearch._types.ElasticsearchException.class)
        );
        assertThat(
            (
                (co.elastic.clients.elasticsearch._types.ElasticsearchException) exception.getCause()
            ).status(),
            is(status)
        );
        assertThat(attempts.get(), is(1));
    }

    @Test
    void retriesNestedIOExceptionUpToMaxAttempts() {
        RetryTemplate retryTemplate = RetryTestSupport.createRetryTemplate();
        AtomicInteger attempts = new AtomicInteger();

        RetryException exception = assertThrows(RetryException.class, () ->
            retryTemplate.execute(() -> {
                attempts.incrementAndGet();
                throw new RuntimeException("wrapper", new IOException("nested IO failure"));
            })
        );

        assertThat(exception.getCause(), instanceOf(RuntimeException.class));
        assertThat(exception.getCause().getCause(), instanceOf(IOException.class));
        assertThat(exception.getCause().getCause().getMessage(), is("nested IO failure"));
        assertThat(attempts.get(), is(4));
    }

    @Test
    void doesNotRetryInterruptedExceptionAndRestoresInterruptFlag() {
        RetryTemplate retryTemplate = RetryTestSupport.createRetryTemplate();
        AtomicInteger attempts = new AtomicInteger();

        RetryException exception = assertThrows(RetryException.class, () ->
            retryTemplate.execute(() -> {
                attempts.incrementAndGet();
                throw new RuntimeException(new InterruptedException("stop"));
            })
        );

        assertThat(exception.getCause(), instanceOf(RuntimeException.class));
        assertThat(exception.getCause().getCause(), instanceOf(InterruptedException.class));
        assertThat(attempts.get(), is(1));
        assertThat(Thread.currentThread().isInterrupted(), is(true));
    }

    @Test
    void prioritizesInterruptedExceptionOverRetryableCause() {
        RetryTemplate retryTemplate = RetryTestSupport.createRetryTemplate();
        AtomicInteger attempts = new AtomicInteger();

        RetryException exception = assertThrows(RetryException.class, () ->
            retryTemplate.execute(() -> {
                attempts.incrementAndGet();
                throw new RuntimeException(
                    "wrapper",
                    new IOException("IO failure", new InterruptedException("stop"))
                );
            })
        );

        assertThat(exception.getCause(), instanceOf(RuntimeException.class));
        assertThat(exception.getCause().getCause(), instanceOf(IOException.class));
        assertThat(
            exception.getCause().getCause().getCause(),
            instanceOf(InterruptedException.class)
        );
        assertThat(attempts.get(), is(1));
        assertThat(Thread.currentThread().isInterrupted(), is(true));
    }

    @Test
    void doesNotRetryUnknownException() {
        RetryTemplate retryTemplate = RetryTestSupport.createRetryTemplate();
        AtomicInteger attempts = new AtomicInteger();

        RetryException exception = assertThrows(RetryException.class, () ->
            retryTemplate.execute(() -> {
                attempts.incrementAndGet();
                throw new IllegalStateException("unexpected");
            })
        );

        assertThat(exception.getCause(), instanceOf(IllegalStateException.class));
        assertThat(exception.getCause().getMessage(), is("unexpected"));
        assertThat(attempts.get(), is(1));
    }

    @Test
    void doesNotRetryWhenMaxRetriesIsZero() {
        RetryTemplate retryTemplate = RetryTestSupport.createRetryTemplate(0);
        AtomicInteger attempts = new AtomicInteger();

        RetryException exception = assertThrows(RetryException.class, () ->
            retryTemplate.execute(() -> {
                attempts.incrementAndGet();
                throw new IOException("IO failure");
            })
        );

        assertThat(exception.getCause(), instanceOf(IOException.class));
        assertThat(attempts.get(), is(1));
    }
}
