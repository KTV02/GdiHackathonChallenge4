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
package de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi;

/**
 * Rollentyp einer Organisationseinheit im Out-Modell.
 *
 * <p>Vereinfachte Darstellung ohne URI-Referenzen. JSON-Serialisierung erfolgt
 * als Großbuchstaben-Name (z. B. {@code "COMPETENT_OFFICE"}).</p>
 */
public enum RoleType {
    // OD-Rollen (Organisationseinheitsrolle für Onlinedienst)
    PROFESSIONAL_POINT_OF_CONTACT,
    TECHNICAL_POINT_OF_CONTACT,
    TECHNICAL_CONTACT_FOR_THE_OPERATING_PLATFORM,

    // ZS-OE-Rollen (Zuständigkeitsrolle)
    COMPETENT_OFFICE,
    CONTACT_POINT,
    SINGLE_POINT_OF_CONTACT,
    OTHER_SINGLE_POINT_OF_CONTACT,

    UNKNOWN;

    /**
     * Mappt einen Indexer-Modell-{@link de.dataport.suchdienst.importer.model.RoleType}
     * auf den korrespondierenden Out-Modell-{@link RoleType}.
     *
     * <p>Der exhaustive Switch stellt sicher, dass neue Konstanten im Indexer-Modell
     * einen Compile-Fehler auslösen und explizit behandelt werden müssen.
     * Es wird niemals eine Exception geworfen; nicht zuordenbare Werte liefern
     * {@link #UNKNOWN}.</p>
     *
     * @param indexerType Rollentyp aus dem Indexer-Modell, darf nicht {@code null} sein
     * @return korrespondierender {@link RoleType}, niemals {@code null}
     */
    public static RoleType fromIndexerType(
        de.dataport.suchdienst.importer.model.RoleType indexerType
    ) {
        return switch (indexerType) {
            case PROFESSIONAL_POINT_OF_CONTACT -> PROFESSIONAL_POINT_OF_CONTACT;
            case TECHNICAL_POINT_OF_CONTACT -> TECHNICAL_POINT_OF_CONTACT;
            case TECHNICAL_CONTACT_FOR_THE_OPERATING_PLATFORM -> TECHNICAL_CONTACT_FOR_THE_OPERATING_PLATFORM;
            case COMPETENT_OFFICE -> COMPETENT_OFFICE;
            case CONTACT_POINT -> CONTACT_POINT;
            case SINGLE_POINT_OF_CONTACT -> SINGLE_POINT_OF_CONTACT;
            case OTHER_SINGLE_POINT_OF_CONTACT -> OTHER_SINGLE_POINT_OF_CONTACT;
            case UNKNOWN -> UNKNOWN;
        };
    }
}
