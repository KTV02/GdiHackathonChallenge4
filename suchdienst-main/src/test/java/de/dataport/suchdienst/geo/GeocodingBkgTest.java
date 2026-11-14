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
package de.dataport.suchdienst.geo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.geo.model.internal.GeoLocation;
import de.dataport.suchdienst.geo.service.BkgGeoLocationResolver;
import de.dataport.suchdienst.geo.service.BkgQueryProvider;
import de.dataport.suchdienst.indexer.model.mapping.Address;
import java.nio.charset.Charset;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.support.RestGatewaySupport;

class GeocodingBkgTest {

    // BKG verortet die KDO etwas anders als OSM, aber egal
    private static final GeoLocation KDO_LOCATION = new GeoLocation("8.20087", "53.15156");
    private Address kdo;
    private RestGatewaySupport restGatewaySupport;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void init() {
        kdo = Address.builder()
            .street("Elsässer Strasse")
            .houseNumber("66")
            .zip("26121")
            .city("Oldenburg")
            .build();
        restGatewaySupport = new RestGatewaySupport();
        mockServer = MockRestServiceServer.createServer(restGatewaySupport);
    }

    @Test
    void testKdo() throws Exception {
        String json = IOUtils.resourceToString("/bkg/kdo-bkg.json", Charset.defaultCharset());
        mockServer
            .expect(
                ExpectedCount.once(),
                MockRestRequestMatchers.requestTo(
                    Matchers.allOf(
                        Matchers.containsString("plz:(" + kdo.getZip()),
                        Matchers.containsString("strasse:(Els%C3%A4sser%20Strasse)")
                    )
                )
            )
            .andRespond(
                MockRestResponseCreators.withStatus(HttpStatus.OK)
                    .body(json)
                    .contentType(MediaType.APPLICATION_JSON)
            );
        SuchdienstProperties props = new SuchdienstProperties();
        props.getBkg().setGdzGeokodierungBasisUrl("https://foo");
        props.getBkg().setPassword("pw");
        var queryProvider = new BkgQueryProvider(props);
        Optional<GeoLocation> resolve = new BkgGeoLocationResolver(
            restGatewaySupport.getRestTemplate(),
            props,
            queryProvider,
            null,
            null
        ).resolve(kdo);

        assertAll(
            () -> assertTrue(resolve.isPresent()),
            () -> assertThat(resolve.get(), Matchers.is(KDO_LOCATION))
        );

        mockServer.verify();
    }
}
