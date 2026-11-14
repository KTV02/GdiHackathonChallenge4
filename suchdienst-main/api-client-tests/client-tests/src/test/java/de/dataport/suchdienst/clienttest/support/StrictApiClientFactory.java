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
package de.dataport.suchdienst.clienttest.support;

import de.dataport.suchdienst.client.ApiClient;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;

public final class StrictApiClientFactory {

    private StrictApiClientFactory() {}

    public static JsonMapper mapper() {
        return ApiClient.createDefaultMapper(null)
            .rebuild()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)
            .enable(EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .disable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
            .disable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            .build();
    }

    public static RestClient.Builder builder() {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(httpClient);
        requests.setReadTimeout(Duration.ofSeconds(30));
        return RestClient.builder().requestFactory(requests);
    }

    /** Behält auch einen bereits an den Builder gebundenen MockRestServiceServer bei. */
    public static ApiClient create(RestClient.Builder builder, String baseUrl) {
        JsonMapper mapper = mapper();
        // ApiClient und HTTP-Converter müssen denselben strikten Mapper verwenden; reine Mappertests belegen das nicht.
        builder.configureMessageConverters(converters ->
            converters.registerDefaults().withJsonConverter(new JacksonJsonHttpMessageConverter(mapper))
        );
        return new ApiClient(builder.build(), mapper, ApiClient.createDefaultDateFormat()).setBasePath(baseUrl);
    }
}
