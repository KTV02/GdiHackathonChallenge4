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
package de.dataport.suchdienst.geo.model.internal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Repraesentiert einen Punkt mit den in OSM verwendeten Koordinaten.
 * <p>
 * <b>Achtung:</b> der volle Klassenname wird bei der Serialisierung fuer das Caching verwendet.
 * Ein Umbenennen der Klasse invalidiert daher den Cache!
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeoLocation {

    @JsonIgnore
    public static final String EPSG_4326 = "EPSG:4326";

    @JsonIgnore
    public static final String DEFAULT_CRS = EPSG_4326;

    @Schema(description = "Geographische Länge", requiredMode = Schema.RequiredMode.REQUIRED)
    private String longitude;

    @Schema(description = "Geographische Breite", requiredMode = Schema.RequiredMode.REQUIRED)
    private String latitude;

    /**
     * definiert, in welchem Koordinatensystem die Koordinaten angegeben sind. Default ist das
     * (mutmaßlich) von OSM verwendete System.
     */
    private String system = DEFAULT_CRS;

    public GeoLocation(String lon, String lat) {
        this(lon, lat, DEFAULT_CRS);
    }
}
