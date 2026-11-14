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
 * Anschrifttyp einer Organisationseinheit im Out-Modell.
 *
 * <p>Vereinfachte Darstellung ohne URI-Referenzen. JSON-Serialisierung erfolgt
 * als Großbuchstaben-Name (z. B. {@code "ADDRESS"}).</p>
 */
public enum AddressType {
    ADDRESS,
    DELIVERY_ADDRESS,
    POSTAL_ADDRESS,
    UNBESTIMMT;

    /**
     * Mappt einen Indexer-Modell-{@link de.dataport.suchdienst.importer.model.AddressType}
     * auf den korrespondierenden Out-Modell-{@link AddressType}.
     *
     * <p>Der exhaustive Switch stellt sicher, dass neue Konstanten im Indexer-Modell
     * einen Compile-Fehler auslösen und explizit behandelt werden müssen.
     * Es wird niemals eine Exception geworfen; nicht zuordenbare Werte liefern
     * {@link #UNBESTIMMT}.</p>
     *
     * @param indexerType Anschrifttyp aus dem Indexer-Modell, darf nicht {@code null} sein
     * @return korrespondierender {@link AddressType}, niemals {@code null}
     */
    public static AddressType fromIndexerType(
        de.dataport.suchdienst.importer.model.AddressType indexerType
    ) {
        return switch (indexerType) {
            case ADDRESS -> ADDRESS;
            case DELIVERY_ADDRESS -> DELIVERY_ADDRESS;
            case POSTAL_ADDRESS -> POSTAL_ADDRESS;
            case UNBESTIMMT -> UNBESTIMMT;
        };
    }
}
