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

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.dataport.suchdienst.geo.model.internal.GeoLocation;
import de.dataport.suchdienst.geo.service.BkgGeoLocationResolver;
import de.dataport.suchdienst.geo.service.CoordinateTransformationService;
import de.dataport.suchdienst.indexer.model.mapping.Address;
import de.dataport.suchdienst.indexer.util.x22.XZuFiUtil;
import de.xoev.xzufi.v2_2_0.DirectPositionType;
import de.xoev.xzufi.v2_2_0.Geokodierung;
import de.xoev.xzufi.v2_2_0.PointType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

class GeolocationFromXzufiTest {

    private final CoordinateTransformationService service = new CoordinateTransformationService();
    private final GeoLocation transformedLocation = new GeoLocation(
        "10.140497136902583",
        "54.328426300315236"
    );

    @Mock
    private BkgGeoLocationResolver geocodingApi;

    @BeforeEach
    void init() {
        geocodingApi = Mockito.mock(BkgGeoLocationResolver.class);
        Mockito.when(geocodingApi.resolve(Mockito.any(Address.class))).thenReturn(
            Optional.of(transformedLocation)
        );
    }

    @Test
    void EPSG25832LonLat() {
        testTransformation("EPSG:25832", 574167.817000002, 6020663.237, transformedLocation);
    }

    @Test
    void EPSG25832LatLon() {
        testTransformation("EPSG:25832", 6020663.237, 574167.817000002, transformedLocation);
    }

    @Test
    void coordinatesFromAddress() {
        testTransformation("xxx", 6020663.237, 574167.817000002, null);
    }

    @Test
    void testWithAxisLabelsLatLonPos() {
        List<String> axisLabels = new ArrayList<>();
        axisLabels.add("latitude");
        axisLabels.add("longitude");
        testTransformation(
            "EPSG:25832",
            6020663.237,
            574167.817000002,
            transformedLocation,
            axisLabels,
            new ArrayList<>()
        );
    }

    @Test
    void testWithAxisLabelsLonLatPos() {
        List<String> axisLabels = new ArrayList<>();
        axisLabels.add("longitude");
        axisLabels.add("latitude");
        testTransformation(
            "EPSG:25832",
            574167.817000002,
            6020663.237,
            transformedLocation,
            axisLabels,
            new ArrayList<>()
        );
    }

    @Test
    void testWithAxisLabelsLatLonPoint() {
        List<String> axisLabels = new ArrayList<>();
        axisLabels.add("latitude");
        axisLabels.add("longitude");
        testTransformation(
            "EPSG:25832",
            6020663.237,
            574167.817000002,
            transformedLocation,
            new ArrayList<>(),
            axisLabels
        );
    }

    @Test
    void testWithAxisLabelsWrongLatLonPoint() {
        List<String> axisLabels = new ArrayList<>();
        axisLabels.add("longitude");
        axisLabels.add("latitude");
        testTransformation(
            "EPSG:25832",
            6020663.237,
            574167.817000002,
            transformedLocation,
            new ArrayList<>(),
            axisLabels
        );
    }

    @Test
    void testWithAxisLabelsWrongLonLatPoint() {
        List<String> axisLabels = new ArrayList<>();
        axisLabels.add("latitude");
        axisLabels.add("longitude");
        testTransformation(
            "EPSG:25832",
            574167.817000002,
            6020663.237,
            transformedLocation,
            new ArrayList<>(),
            axisLabels
        );
    }

    @Test
    void testWithAxisLabelsLonLatPoint() {
        List<String> axisLabels = new ArrayList<>();
        axisLabels.add("longitude");
        axisLabels.add("latitude");
        testTransformation(
            "EPSG:25832",
            574167.817000002,
            6020663.237,
            transformedLocation,
            new ArrayList<>(),
            axisLabels
        );
    }

    private void testTransformation(
        String srsName,
        Double lon,
        Double lat,
        GeoLocation targetLocation
    ) {
        testTransformation(srsName, lon, lat, targetLocation, new ArrayList<>(), new ArrayList<>());
    }

    private void testTransformation(
        String srsName,
        Double lon,
        Double lat,
        GeoLocation targetLocation,
        List<String> axisLabelsPos,
        List<String> axisLabelsPoint
    ) {
        DirectPositionType pos = new DirectPositionType();
        pos.setSrsName(srsName);
        pos.getValue().add(lon);
        pos.getValue().add(lat);
        pos.getAxisLabels().addAll(axisLabelsPos);
        PointType point = new PointType();
        point.setPos(pos);
        point.getAxisLabels().addAll(axisLabelsPoint);
        Geokodierung geokodierung = new Geokodierung();
        geokodierung.setPoint(point);
        List<Geokodierung> geokodierungList = List.of(geokodierung);
        Optional<GeoLocation> geolocation = XZuFiUtil.calculateGeolocation(
            Mockito.mock(Address.class),
            geokodierungList,
            service,
            Mockito.mock(BkgGeoLocationResolver.class)
        );
        assertEquals(
            targetLocation == null ? Optional.empty() : Optional.of(targetLocation),
            geolocation
        );
    }
}
