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
package suchdienst.integrationstest.tests.sonstiges;

import static io.restassured.RestAssured.given;
import static io.restassured.config.DecoderConfig.decoderConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.restassured.config.DecoderConfig;
import io.restassured.config.RestAssuredConfig;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import suchdienst.integrationstest.common.RestAssuredUtil;

/**
 * Der Server-Compression Test validiert, ob eine beliebige Suchdienst-API sowohl die GZIP-Komprimierung unterstützt.
 * Dabei wird geprüft, dass das Backend entsprechend des `Accept-Encoding` eine korrekt Enkodierte Nachricht versendet.
 */
class ServerCompressionTest {

    @Test
    void serviceDescriptionsArsApi_responseIsGzip() {
        // RestAssured wird so konfiguriert, dass kein automatisches Decoding passiert
        DecoderConfig noDecoderConfig = decoderConfig().noContentDecoders();
        var bodyBytes = given(RestAssuredUtil.getRequestSpec())
            .config(RestAssuredConfig.newConfig().decoderConfig(noDecoderConfig))
            .header("Accept-Encoding", "gzip")
            .accept("application/json")
            .queryParams(Map.of("q", "hund"))
            .when()
            .get("/v7/servicedescriptions/{ars}", "020000000000")
            .then()
            .contentType("application/json")
            .header("Content-Encoding", "gzip")
            .statusCode(is(200))
            .extract()
            .body()
            .asByteArray();

        // Die Antwort wird GZIP-Encodiert. Soll nicht werfen: java.util.zip.ZipException: Not in GZIP format
        String response = assertDoesNotThrow(() -> {
            try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(bodyBytes))) {
                return IOUtils.toString(gis, StandardCharsets.UTF_8);
            }
        }, "Antwort ist nicht im GZIP-Format!");
        assertThat(response, startsWith("{\"facades\":"));
    }

    @Test
    void serviceDescriptionsArsApi_responseIsNoGzip() {
        // RestAssured wird so konfiguriert, dass kein automatisches Decoding passiert
        DecoderConfig noDecoderConfig = decoderConfig().noContentDecoders();
        var bodyBytes = given(RestAssuredUtil.getRequestSpec())
            .config(RestAssuredConfig.newConfig().decoderConfig(noDecoderConfig))
            .accept("application/json")
            .queryParams(Map.of("q", "hund"))
            .when()
            .get("/v7/servicedescriptions/{ars}", "020000000000")
            .then()
            .contentType("application/json")
            .header("Content-Encoding", nullValue())
            .statusCode(is(200))
            .extract()
            .body()
            .asByteArray();

        // Die Antwort wird nicht GZIP-Encodiert. Soll werfen: java.util.zip.ZipException: Not in GZIP format
        assertThrows(
            ZipException.class,
            () -> {
                try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(bodyBytes))) {
                    IOUtils.toString(gis, StandardCharsets.UTF_8);
                }
            },
            "Antwort ist im GZIP-Format"
        );
        assertThat(new String(bodyBytes, StandardCharsets.UTF_8), startsWith("{\"facades\":"));
    }
}
