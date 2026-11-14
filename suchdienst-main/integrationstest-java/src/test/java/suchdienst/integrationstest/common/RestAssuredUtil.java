/*-
 * ---license-start
 * Suchdienst Integrationstests
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package suchdienst.integrationstest.common;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.DecoderConfig;
import io.restassured.config.EncoderConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.specification.RequestSpecification;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;

public class RestAssuredUtil {

    /**
     * Bewusst als lokale, unveränderliche Konfiguration statt über den globalen, statischen
     * {@code RestAssured.config}-Singleton gepflegt: So gibt es keinen von allen (parallelen) Tests
     * geteilten, veränderlichen Zustand, und jede erzeugte {@link RequestSpecification} bekommt die
     * Konfiguration explizit und nachvollziehbar mitgegeben.
     */
    private static final RestAssuredConfig CONFIG = buildConfig();

    private static RestAssuredConfig buildConfig() {
        return RestAssuredConfig.config()
            .encoderConfig(
                EncoderConfig.encoderConfig()
                    .defaultCharsetForContentType("UTF-8", "application/xml")
                    .defaultCharsetForContentType("UTF-8", "application/json")
            )
            .decoderConfig(
                DecoderConfig.decoderConfig()
                    .defaultCharsetForContentType("UTF-8", "application/xml")
                    .defaultCharsetForContentType("UTF-8", "application/json")
            )
            .objectMapperConfig(
                RestAssuredConfig.config()
                    .getObjectMapperConfig()
                    .defaultObjectMapperType(ObjectMapperType.JACKSON_3)
                    .jackson3ObjectMapperFactory((_, _) ->
                        JsonMapper.builder().findAndAddModules().addModule(new JakartaXmlBindAnnotationModule()).build()
                    )
            )
            .logConfig(LogConfig.logConfig().enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL));
    }

    public static RequestSpecification getRequestSpec() {
        return new RequestSpecBuilder()
            .setConfig(CONFIG)
            .setBaseUri(Constants.BASE_PATH)
            .setAccept(ContentType.JSON)
            .build();
    }

    public static RequestSpecification getRequestSpec(ContentType contentTypeRequest) {
        return getRequestSpec(contentTypeRequest, ContentType.JSON);
    }

    public static RequestSpecification getRequestSpec(ContentType contentTypeRequest, ContentType contentTypeAccept) {
        return new RequestSpecBuilder()
            .setConfig(CONFIG)
            .setBaseUri(Constants.BASE_PATH)
            .setAccept(contentTypeAccept)
            .build()
            .contentType(contentTypeRequest);
    }

    public static RequestSpecification getElasticsearchRequestSpec() {
        return new RequestSpecBuilder()
            .setConfig(CONFIG)
            .setBaseUri(Constants.ELS_BASE_PATH)
            .setAccept(ContentType.JSON)
            .build();
    }

    public static RequestSpecification getElasticsearchRequestSpec(ContentType contentType) {
        return getElasticsearchRequestSpec().contentType(contentType);
    }
}
