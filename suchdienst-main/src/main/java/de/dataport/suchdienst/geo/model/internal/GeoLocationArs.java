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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.dataport.suchdienst.indexer.model.ArsObject;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

@Slf4j
@Builder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeoLocationArs {

    private String name;
    private String ars;

    @Nullable
    private String municipality;

    private float weight;
    private List<String> keys;

    @Nullable
    private String plz;

    private String street;

    @Nullable
    private String housenumber;

    /**
     * Gibt auf ARS-Länge zugeschnittene Schlüsselteile zurück, die
     * ungleich des Basis-ARS sind, und zur Auflösung nach RS am BK
     * genutzt werden können.
     */
    @JsonIgnore
    public List<String> getTailoredDistinctKeys() {
        return this.keys
            .stream()
            .map(k -> StringUtils.left(k, ArsObject.ARS_LENGTH))
            .distinct()
            .filter(k -> !k.equals(ars))
            .toList();
    }
}
