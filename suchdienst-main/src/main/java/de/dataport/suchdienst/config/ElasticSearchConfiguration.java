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

import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.NodeSelector;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.elasticsearch.autoconfigure.Rest5ClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Nimmt notwendige Anpassungen / Erweiterungen an der Spring-Boot Autokonfiguration von
 * ElasticSearch vor.
 */
@Configuration
public class ElasticSearchConfiguration {

    @Bean
    public Rest5ClientTransport restClientTransport(
        Rest5ClientBuilder restClientBuilder,
        @Qualifier("elasticsearchJsonMapper") JsonMapper jsonMapper
    ) {
        Rest5Client client = restClientBuilder.build();
        Jackson3JsonpMapper jsonpMapper = new Jackson3JsonpMapper(jsonMapper);
        return new Rest5ClientTransport(client, jsonpMapper);
    }

    @Bean
    public Rest5ClientBuilderCustomizer elasticCustomizer(SuchdienstProperties props) {
        return new ElasticCustomizer(props);
    }

    record ElasticCustomizer(SuchdienstProperties props) implements Rest5ClientBuilderCustomizer {
        @Override
        public void customize(Rest5ClientBuilder builder) {
            builder.setNodeSelector(NodeSelector.SKIP_DEDICATED_MASTERS);
        }

        @Override
        public void customize(ConnectionConfig.Builder builder) {
            builder.setConnectTimeout(
                Timeout.ofMilliseconds(props.getElastic().getConnectTimeout())
            );
            builder.setSocketTimeout(Timeout.ofMilliseconds(props.getElastic().getSocketTimeout()));
            builder.setValidateAfterInactivity(
                TimeValue.of(props.getElastic().getHttp().getValidateAfterInactivity())
            );
        }

        @Override
        public void customize(RequestConfig.Builder builder) {
            builder.setConnectionRequestTimeout(
                Timeout.ofMilliseconds(props.getElastic().getConnectionRequestTimeout())
            );
        }

        @Override
        public void customize(PoolingAsyncClientConnectionManagerBuilder builder) {
            SuchdienstProperties.Http httpConfig = props.getElastic().getHttp();
            builder.setMaxConnPerRoute(httpConfig.getMaxConnPerRoute());
            builder.setMaxConnTotal(httpConfig.getMaxConnTotal());
        }
    }
}
