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
package de.dataport.suchdienst.config.prometheus;

import de.dataport.suchdienst.util.RequestUtils;
import de.dataport.suchdienst.util.model.ProtocolType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.Nullable;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

/**
 * Diese GenericFilterBean dient der Bereitstellung zusätzlicher Prometheus Metriken über die
 * Zugriffe auf die Konsumentenschnittstelle.
 * Die bisher einzige solche Metrik ist "calls_by_xzufiVersion_total", sie zählt pauschal die Anzahl der Zugriffe auf
 * die Jzufi-Endpunkte pro XZuFi-Version.
 * <p>
 * Beispiel eines Metrik-Eintrags:
 * <pre>
 * calls_by_xzufiVersion_total{application="suchdienst",endpoint="/v2/organisationunits",instanceType="MASTER",xzufiVersion="2.2.0"} 2.0
 * </pre>
 * Es werden nur erfolgreiche Zugriffe (HTTP 200) gezählt.
 */
@Component
@Order(3)
@Slf4j
public class PrometheusKonsumentenMetrikFilter extends GenericFilterBean {

    /* Name der Metrik, die die Xzufi-Version pro Endpunkt zählt. Diese Metrik wird nur bei den jzufi-Endpunkten
     * geführt, da nur dort die XZuFi-Version relevant ist. */
    private static final String METRIC_NAME = "calls_by_xzufiVersion";

    private final MeterRegistry meterRegistry;

    /**
     * @param meterRegistry Im Kontext eines Unit-Tests sind keine Beans für ApplicationAvailability und/oder
     *                      MeterRegistry vorhanden.
     *                      <code>@Nullable</code> sorgt dafür, dass der Filter im Betrieb und in den Unit-Tests immer
     *                      da ist, aber keine Fehler erzeugt, wenn er nicht arbeiten kann.
     */
    public PrometheusKonsumentenMetrikFilter(@Nullable MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        if (meterRegistry == null) {
            log.warn(
                "{}: Der PrometheusKonsumentenMetrikFilter ist nicht aktiv, da keine MeterRegistry zur Verfügung steht.",
                ProtocolType.SYSTEM
            );
        } else {
            log.info("{}: Der PrometheusKonsumentenMetrikFilter ist aktiv.", ProtocolType.SYSTEM);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        chain.doFilter(request, response);

        if (meterRegistry == null || !(request instanceof HttpServletRequest httpRequest)) {
            return;
        }

        try {
            filterSuccessfulRequestAndIncrementMetrics(httpRequest, response);
        } catch (Exception e) {
            // Diese lokale Exception-Behandlung bewirkt, dass Fehler hier nicht den Request in seiner normalen
            // Funktionalität beeinträchtigen. Eine solche Exception darf keinesfalls
            // hier den eigentlichen Request abbrechen ("aborted"). Denn die Prometheus-Metrik ist nur ein
            // Add-On, aber nicht kritisch für die Funktionalität des Suchdienstes.
            log.warn(
                "{}: Fehler beim Bilden der Prometheus-Konsumenten-Metrik für die Anfrage {}: {}",
                ProtocolType.SYSTEM,
                httpRequest.getRequestURI(),
                e.getMessage(),
                e
            );
        }
    }

    private void filterSuccessfulRequestAndIncrementMetrics(
        HttpServletRequest httpRequest,
        ServletResponse response
    ) {
        int status =
            response instanceof HttpServletResponse httpServletResponse
                ? httpServletResponse.getStatus()
                : -1;
        if (status != HttpStatus.OK.value()) {
            return;
        }
        extractXzufiVersionAndIncrementMetricCounter(httpRequest);
    }

    /**
     * Inkrementiert die speziell für die XZuFi-Version angelegte Prometheus-Metrik. Die Metrik ist nur für die Jzufi
     * Endpunkte gedacht, da nur dort die XZuFi-Version überhaupt übergeben wird.
     */
    private void extractXzufiVersionAndIncrementMetricCounter(HttpServletRequest httpRequest) {
        String requestPath = httpRequest.getRequestURI().replace(httpRequest.getContextPath(), "");
        if (!requestPath.endsWith("/jzufi")) {
            return;
        }

        Tag versionTag = Tag.of(
            "xzufiVersion",
            RequestUtils.getXZuFiVersion(httpRequest).getVersion()
        );
        incrementXzufiMetricCounter(requestPath, List.of(versionTag));
    }

    /**
     * Der Abfrage-Counter für die RequestUri und die XZuFi-Version wird inkrementiert. Dieser spezifische Counter
     * wird angelegt und registriert, falls er noch nicht existiert. Solche Counter müssen nie, können aber
     * jederzeit zurückgesetzt werden (z.B. durch Neustart). Prometheus kümmert sich selbst darum, aus den
     * abgefragten Zählerständen eine sinnvolle Statistik zu erzeugen.
     *
     * @param requestPath die RequestUri des jeweilig gerufenen Endpunktes
     * @param tags Tags für den Counter, zusätzlich zu dem immer hinzufügenden "endpoint" Tag
     */
    private void incrementXzufiMetricCounter(String requestPath, Iterable<Tag> tags) {
        String[] splitUri = StringUtils.split(requestPath, "/", 3);
        String firstPartsRequestUri =
            "/" + splitUri[0] + "/" + (splitUri.length > 1 ? splitUri[1] : "null");
        Tag firstPartsRequestTag = Tag.of("endpoint", firstPartsRequestUri);

        // Tags zusammenführen
        Tags combinedTags = Tags.of(firstPartsRequestTag).and(tags);

        // Die Registry liefert einen existierenden oder einen neu angelegten Counter mit dieser Kombination
        // aus Name und Tags.
        Counter counter = meterRegistry.counter(METRIC_NAME, combinedTags);
        counter.increment();
    }
}
