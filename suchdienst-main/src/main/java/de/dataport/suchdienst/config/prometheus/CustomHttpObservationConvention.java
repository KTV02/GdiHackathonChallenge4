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
import de.dataport.suchdienst.util.model.ApiClientId;
import io.micrometer.common.KeyValues;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.http.server.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.stereotype.Component;

/**
 * Seit PV-13746 sollen sich Konsumenten der API optional mit einer API-Client-ID identifizieren.
 * Diese verwenden wir dann in unseren Logs und Metriken, um die Nutzung der API besser analysieren zu können.
 * <p>
 * Diese Klasse sorgt dafür, dass in den Standard Prometheus HTTP Metriken
 * (http_server_requests_seconds_count, http_server_requests_seconds_sum etc.) zusätzlich das Tag "apiClientId"
 * mit der im Header übergebenen Client-ID (bzw. "unknown", falls keine übergeben wurde) mitgeführt wird.
 * Beispiel:
 * <pre>
 * http_server_requests_active_seconds_count{apiClientId="b4824fd7-8808-4366-983c-c5cdba9919a4",application="suchdienst",exception="none",instanceType="MASTER",method="GET",outcome="SUCCESS",status="200",uri="UNKNOWN"} 0
 * </pre>
 */
@Component
public class CustomHttpObservationConvention extends DefaultServerRequestObservationConvention {

    @Override
    public KeyValues getLowCardinalityKeyValues(ServerRequestObservationContext context) {
        return super
            .getLowCardinalityKeyValues(context)
            .and(ApiClientId.PROMETHEUS_TAG, getApiClientIdValueFromHeader(context));
    }

    private String getApiClientIdValueFromHeader(ServerRequestObservationContext context) {
        HttpServletRequest request = context.getCarrier();
        Optional<ApiClientId> apiClientIdOpt = RequestUtils.getApiClientId(request);
        return apiClientIdOpt.map(ApiClientId::value).orElse(ApiClientId.UNKNOWN_CLIENT_ID);
    }
}
