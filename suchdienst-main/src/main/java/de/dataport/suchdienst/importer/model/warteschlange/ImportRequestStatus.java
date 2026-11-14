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

import static java.util.stream.Collectors.toSet;

import java.util.Set;
import java.util.stream.Stream;
import lombok.Getter;

/**
 * Zeigt an, in welchem Zustand ein ImportRequest ist. Die Requests können nach einem Fehler erneut verarbeitet werden.
 */
@Getter
public enum ImportRequestStatus {
    /**
     * Der Request wurde noch nicht verarbeitet.
     */
    OPEN(false),

    /**
     * Die Verarbeitung des Requests ist fehlgeschlagen.
     */
    ERROR(true),

    /**
     * Die Verarbeitung des Requests ist beendet.
     */
    DONE(true),

    /**
     * Die Verarbeitung wurde erneut beauftragt. (PV-7793)
     */
    REOPENED(false);

    // True bedeutet: Ein ImportRequest in diesem Zustand ist verarbeitet worden.
    private final boolean isProcessed;

    ImportRequestStatus(boolean isProcessed) {
        this.isProcessed = isProcessed;
    }

    public static Set<ImportRequestStatus> getProcessed() {
        return Stream.of(ImportRequestStatus.values())
            .filter(s -> s.isProcessed)
            .collect(toSet());
    }

    public static Set<ImportRequestStatus> getPending() {
        return Stream.of(ImportRequestStatus.values())
            .filter(s -> !s.isProcessed)
            .collect(toSet());
    }
}
