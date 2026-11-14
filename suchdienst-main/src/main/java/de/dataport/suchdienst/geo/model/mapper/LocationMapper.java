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
package de.dataport.suchdienst.geo.model.mapper;

import de.dataport.suchdienst.geo.model.GeoArs;
import de.dataport.suchdienst.geo.model.out.GeoArsOutModelV2;
import de.dataport.suchdienst.geo.model.out.GeoArsOutModelV3;
import java.util.Arrays;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface LocationMapper {
    LocationMapper INSTANCE = Mappers.getMapper(LocationMapper.class);

    @Mapping(target = "id", source = "ars")
    @Mapping(target = "zip", source = "displayZip")
    @Mapping(target = "suggest.input", source = "suggest.input")
    @Mapping(target = "location", ignore = true)
    de.dataport.suchdienst.geo.model.out.ArsObject mapArsObject(
        de.dataport.suchdienst.indexer.model.ArsObject arsObject
    );

    GeoArsOutModelV2 mapGeoArsV2(GeoArs geoArs);

    @Mapping(target = "strasse", source = "street")
    @Mapping(target = "hausnummer", source = "housenumber")
    @Mapping(target = "gemeinde", source = "municipality")
    @Mapping(target = "koordinaten", expression = "java(mapKoordinaten(geoArs))")
    GeoArsOutModelV3 mapGeoArsV3(GeoArs geoArs);

    @SuppressWarnings("java:S1168") // Null als Return für eine Liste ist sehr ungewöhnlich. Hier ist es
    // gewollt, die Koordinaten sind bei den verwendenden Stellen (in einem Outmodel) explizit @Nullable.
    default List<String> mapKoordinaten(GeoArs geoArs) {
        if (geoArs.getLongitude() == null || geoArs.getLatitude() == null) {
            return null;
        }
        return Arrays.asList(geoArs.getLongitude(), geoArs.getLatitude());
    }
}
