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
package de.dataport.suchdienst.util.model;

import java.util.Arrays;
import lombok.Getter;

@Getter
public enum TransaktionsPosition {
    /**
     * Erste Nachricht einer Fachtransaktion.
     */
    START("0", true, false),

    /**
     * Folgenachricht.
     */
    FORTSCHRITT("1", false, false),

    /**
     * Letzte Nachricht.
     */
    STOP("2", false, true),

    /**
     * Wenn es nur eine Nachricht gibt, signalisiert dies die erste-und-letzte Nachricht.
     */
    START_STOP("3", true, true),

    /**
     * Der Sammlerdienst schickt diese Art von Nachrichten im Rahmen eines explizit vom User angestoßenen Neu-Exports.
     * Treten nicht im "regulären" Workflow auf.
     */
    KORREKTUR("4", false, false);

    private final String position;

    private final boolean isStart;

    private final boolean isStop;

    TransaktionsPosition(String position, boolean isStart, boolean isStop) {
        this.isStart = isStart;
        this.isStop = isStop;
        this.position = position;
    }

    public static TransaktionsPosition findByPosition(String position) {
        return Arrays.stream(values())
            .filter(p -> position.equals(p.getPosition()))
            .findFirst()
            .orElseThrow(() ->
                new IllegalArgumentException("Transaktionsposition unbekannt: " + position)
            );
    }
}
