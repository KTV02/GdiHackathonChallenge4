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
package de.dataport.suchdienst.util.model;

/**
 * Seit PV-13746 können sich Konsumenten der API optional mit einer Client-ID identifizieren.
 * Diese verwenden wir dann in unseren Logs und Metriken, um die Nutzung der API besser analysieren zu können.
 * An dieser Stelle sind die zugehörigen Konstanten für das MDC-Logging, den Request-Header und Tag für
 * Prometheus Metriken definiert.
 */
public record ApiClientId(String value) {
    public static final String MDC_KEY = MdcKeys.APICLIENTID;

    public static final String HEADER_NAME = "pvog-sud-api-client-id";

    public static final String PROMETHEUS_TAG = "apiClientId";

    public static final String UNKNOWN_CLIENT_ID = "unknown";

    @Override
    public String toString() {
        return value;
    }
}
