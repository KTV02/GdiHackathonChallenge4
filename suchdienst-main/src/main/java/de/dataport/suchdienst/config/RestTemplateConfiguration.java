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

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.routing.SystemDefaultRoutePlanner;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfiguration {

    @Bean(name = "bkgRestTemplate")
    public RestTemplate bkgRestTemplate(SuchdienstProperties props, RestTemplateBuilder builder) {
        var timeout = Timeout.of(props.getBkg().getRequestTimeout());

        // FIXME Hier könnte man Anstreben auf den seit Spring 6.1 verfügbaren RestClient zu migrieren.
        return builder
            .requestFactory(() -> {
                PoolingHttpClientConnectionManager mgr = new PoolingHttpClientConnectionManager();
                mgr.setDefaultMaxPerRoute(props.getBkg().getMaxConnections());
                // kann maxPerRoute entsprechen, weil nur ein Zielhost (BKG):
                mgr.setMaxTotal(props.getBkg().getMaxConnections());
                RequestConfig config = RequestConfig.custom()
                    .setConnectionRequestTimeout(timeout)
                    .setResponseTimeout(timeout)
                    .build();

                ConnectionConfig conConfig = ConnectionConfig.custom()
                    .setConnectTimeout(timeout)
                    .build();

                mgr.setDefaultConnectionConfig(conConfig);

                return new HttpComponentsClientHttpRequestFactory(
                    HttpClientBuilder.create()
                        .setConnectionManager(mgr)
                        .setRoutePlanner(new SystemDefaultRoutePlanner(null))
                        .setDefaultRequestConfig(config)
                        .build()
                );
            })
            .build();
    }

    @Bean(name = "codelistTemplate")
    public RestTemplate codelistTemplate(SuchdienstProperties props, RestTemplateBuilder builder) {
        return builder
            .requestFactory(() -> {
                SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                factory.setConnectTimeout(props.getCodelist().getConnectTimeout());
                factory.setReadTimeout(props.getCodelist().getRequestTimeout());
                return factory;
            })
            .build();
    }
}
