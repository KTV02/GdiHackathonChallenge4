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
 * Statisches Mapping der Organisationsrollen aus den XZuFi-Codelisten
 * {@code xzufi.organisationseinheitsrollefueronlinedienst} (OD-Pfad) und
 * {@code xzufi.zustaendigkeitsrolle} (ZS-OE-Pfad).
 *
 * <p>Der Mapping-Schlüssel wird aus URI und Code versionslos zusammengesetzt:
 * {@code &lt;uri&gt;.&lt;code&gt;}. Die URI ist nicht optional – fehlt sie, wird
 * direkt {@link #UNKNOWN} zurückgegeben. Nicht zuordenbare Codes werden ebenfalls
 * auf {@link #UNKNOWN} gemappt.</p>
 */
@Getter
public enum RoleType {
    // OD-Rollen (xzufi.organisationseinheitsrollefueronlinedienst)
    PROFESSIONAL_POINT_OF_CONTACT(
        "urn:xoev-de:fim:codeliste:xzufi.organisationseinheitsrollefueronlinedienst.01"
    ),
    TECHNICAL_POINT_OF_CONTACT(
        "urn:xoev-de:fim:codeliste:xzufi.organisationseinheitsrollefueronlinedienst.11"
    ),
    TECHNICAL_CONTACT_FOR_THE_OPERATING_PLATFORM(
        "urn:xoev-de:fim:codeliste:xzufi.organisationseinheitsrollefueronlinedienst.12"
    ),

    // ZS-OE-Rollen (xzufi.zustaendigkeitsrolle)
    COMPETENT_OFFICE("urn:xoev-de:fim:codeliste:xzufi.zustaendigkeitsrolle.02"),
    CONTACT_POINT("urn:xoev-de:fim:codeliste:xzufi.zustaendigkeitsrolle.03"),
    SINGLE_POINT_OF_CONTACT("urn:xoev-de:fim:codeliste:xzufi.zustaendigkeitsrolle.11"),
    OTHER_SINGLE_POINT_OF_CONTACT("urn:xoev-de:fim:codeliste:xzufi.zustaendigkeitsrolle.19"),

    UNKNOWN(null);

    private final String value;

    RoleType(String value) {
        this.value = value;
    }

    /**
     * Mappt URI und Code auf einen {@link RoleType}.
     *
     * <p>Die URI ist nicht optional. Ist sie {@code null} oder der Code {@code null},
     * wird sofort {@link #UNKNOWN} zurückgegeben. Findet sich kein Treffer, wird
     * ebenfalls {@link #UNKNOWN} zurückgegeben.</p>
     *
     * @param uri  Codelisten-URI (nicht optional, {@code null} → {@link #UNKNOWN})
     * @param code Codelisten-Code (darf {@code null} sein → {@link #UNKNOWN})
     * @return gemappter {@link RoleType}, niemals {@code null}
     */
    public static RoleType fromCode(String uri, String code) {
        if (uri == null || code == null) {
            return UNKNOWN;
        }
        String key = uri + "." + code;
        return Arrays.stream(values())
            .filter(t -> key.equals(t.value))
            .findFirst()
            .orElse(UNKNOWN);
    }
}
