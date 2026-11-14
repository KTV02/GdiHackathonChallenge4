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
package de.dataport.suchdienst.importer.model;

import java.util.Arrays;
import lombok.Getter;

/**
 * Statisches Mapping der Anschrifttypen aus der XZuFi-Codeliste {@code xzufi.anschrifttyp}.
 *
 * <p>Der Mapping-Schlüssel wird aus URI (optional, dann Default) und Code versionslos
 * zusammengesetzt: {@code &lt;uri&gt;.&lt;code&gt;}. Nicht zuordenbare Codes werden auf
 * {@link #UNBESTIMMT} gemappt.</p>
 */
@Getter
public enum AddressType {
    ADDRESS("urn:xoev-de:fim:codeliste:xzufi.anschrifttyp.001"),
    DELIVERY_ADDRESS("urn:xoev-de:fim:codeliste:xzufi.anschrifttyp.005"),
    POSTAL_ADDRESS("urn:xoev-de:fim:codeliste:xzufi.anschrifttyp.006"),
    UNBESTIMMT(null);

    private static final String DEFAULT_URI = "urn:xoev-de:fim:codeliste:xzufi.anschrifttyp";

    private final String value;

    AddressType(String value) {
        this.value = value;
    }

    /**
     * Mappt URI und Code auf einen {@link AddressType}.
     *
     * <p>Die URI ist optional. Fehlt sie, wird die Default-URI
     * {@code urn:xoev-de:fim:codeliste:xzufi.anschrifttyp} verwendet.
     * Kann kein ENUM-Wert gefunden werden, wird {@link #UNBESTIMMT} zurückgegeben.</p>
     *
     * @param uri  Codelisten-URI (optional, darf {@code null} sein)
     * @param code Codelisten-Code (darf {@code null} sein → {@link #UNBESTIMMT})
     * @return gemappter {@link AddressType}, niemals {@code null}
     */
    public static AddressType fromCode(String uri, String code) {
        if (code == null) {
            return UNBESTIMMT;
        }
        String effectiveUri = uri != null ? uri : DEFAULT_URI;
        String key = effectiveUri + "." + code;
        return Arrays.stream(values())
            .filter(t -> key.equals(t.value))
            .findFirst()
            .orElse(UNBESTIMMT);
    }
}
