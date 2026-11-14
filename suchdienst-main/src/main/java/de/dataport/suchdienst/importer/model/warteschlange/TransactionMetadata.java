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
package de.dataport.suchdienst.importer.model.warteschlange;

import lombok.NonNull;

public record TransactionMetadata(
    @NonNull String id,
    @NonNull String transactionId,
    @NonNull String transactionPosition,
    @NonNull String schemeAgencyId,
    @NonNull String producer
) {
    @Override
    public String toString() {
        return "uuid: %s, transactionId: %s, transactionPosition: %s, schemeAgencyId: %s, producer: %s".formatted(
            id,
            transactionId,
            transactionPosition,
            schemeAgencyId,
            producer
        );
    }
}
