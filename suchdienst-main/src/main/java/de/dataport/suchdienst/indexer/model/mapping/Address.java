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
package de.dataport.suchdienst.indexer.model.mapping;

import de.dataport.suchdienst.geo.model.internal.GeoLocation;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class Address {

    private String code;

    @ToString.Include
    private String city;

    @ToString.Include
    private String houseNumber;

    private String type;

    @ToString.Include
    private String zip;

    @ToString.Include
    private String street;

    @ToString.Include
    private String postbox;

    @ToString.Include
    private GeoLocation geo;

    private String additional;
    private String accessible;
    private String oepnv;
    private String parking;
    private Boolean wheelchairAccessible;
    private Boolean lift;
}
