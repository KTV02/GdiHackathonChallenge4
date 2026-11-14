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
package de.dataport.suchdienst.geo.model;

import de.dataport.suchdienst.geo.model.internal.GeoLocationDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.jspecify.annotations.Nullable;

@Schema(
    example = """
            {
                "name": "Mannheim",
                "ars": "082220000000",
                "arsFederalstate": "08",
                "municipality": "Mannheim",
                "plz": "68165",
                "street": "Parkallee",
                "housenumber": "2-4",
                "longitude": "9.96662",
                "latitude": "53.46262"
            }
    """
)
@Builder
@Data
public class GeoArs {

    private String name;
    private String ars;
    private String arsFederalstate;

    @Nullable
    private String municipality;

    @Nullable
    private String plz;

    @Nullable
    private String street;

    @Nullable
    private String housenumber;

    @Nullable
    private String longitude;

    @Nullable
    private String latitude;

    public static GeoArs fromGeoLocation(String ars, GeoLocationDetail geoLoc) {
        return GeoArs.builder()
            .name(geoLoc.getArs().getName())
            .ars(ars)
            .arsFederalstate(ars.substring(0, 2))
            .municipality(geoLoc.getArs().getMunicipality())
            .plz(geoLoc.getArs().getPlz())
            .street(geoLoc.getArs().getStreet())
            .housenumber(geoLoc.getArs().getHousenumber())
            .latitude(geoLoc.getLocation() != null ? geoLoc.getLocation().getLatitude() : null)
            .longitude(geoLoc.getLocation() != null ? geoLoc.getLocation().getLongitude() : null)
            .build();
    }
}
