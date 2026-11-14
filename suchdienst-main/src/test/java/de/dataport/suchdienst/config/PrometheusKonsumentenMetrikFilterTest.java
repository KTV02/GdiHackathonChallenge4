/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.config;

import de.dataport.suchdienst.config.prometheus.PrometheusKonsumentenMetrikFilter;
import de.dataport.suchdienst.util.XZuFiVersionHeaderUtil;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;

class PrometheusKonsumentenMetrikFilterTest {

    MeterRegistry meterRegistry;
    PrometheusKonsumentenMetrikFilter filter;
    HttpServletRequest request;
    HttpServletResponse response;
    FilterChain chain;

    @BeforeEach
    void setUp() {
        meterRegistry = Mockito.mock(MeterRegistry.class);
        filter = new PrometheusKonsumentenMetrikFilter(meterRegistry);
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
        chain = Mockito.mock(FilterChain.class);
    }

    @Test
    void filter_ruft_filterChain_auf() throws Exception {
        filter.doFilter(request, response, chain);

        Mockito.verify(chain).doFilter(request, response);
    }

    @ParameterizedTest(
        name = "Filter erhöht den Counter für den Endpunkt {1} mit HTTP-Status {0}: {2}"
    )
    @CsvSource({ "200, /a/b/jzufi, true", "500, /a/b/jzufi, false", "200, /a/b/d, false" })
    void filter_erhoeht_counter(int statusCode, String endpoint, boolean expectIncrementToBeCalled)
        throws Exception {
        Counter counter = Mockito.mock(Counter.class);
        Mockito.when(request.getContextPath()).thenReturn("/a");
        Mockito.when(request.getRequestURI()).thenReturn(endpoint);
        Mockito.when(response.getStatus()).thenReturn(statusCode);
        Mockito.when(
            meterRegistry.counter(Mockito.anyString(), Mockito.any(Tags.class))
        ).thenReturn(counter);
        Mockito.when(request.getHeader(HttpHeaders.ACCEPT)).thenReturn(
            XZuFiVersionHeaderUtil.MEDIA_TYPE_JSON_22
        );

        filter.doFilter(request, response, chain);

        String expectedCounterName = "calls_by_xzufiVersion";
        Tags expectedTags = Tags.of(
            Tag.of("endpoint", "/b/jzufi"),
            Tag.of("xzufiVersion", "2.2.0")
        );
        if (expectIncrementToBeCalled) {
            Mockito.verify(meterRegistry, Mockito.times(1)).counter(
                expectedCounterName,
                expectedTags
            );
        } else {
            Mockito.verify(meterRegistry, Mockito.never()).counter(
                Mockito.anyString(),
                Mockito.anyIterable()
            );
        }
        Mockito.verify(
            counter,
            expectIncrementToBeCalled ? Mockito.times(1) : Mockito.never()
        ).increment();
    }
}
