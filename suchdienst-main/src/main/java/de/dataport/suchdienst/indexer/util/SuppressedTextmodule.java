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
package de.dataport.suchdienst.indexer.util;

import java.util.stream.Stream;
import lombok.Getter;

@Getter
public enum SuppressedTextmodule {
    // Wird nur von der 115 verwendet, siehe PV-8418
    KURZTEXT("05");

    private final String code;

    SuppressedTextmodule(String code) {
        this.code = code;
    }

    public static boolean isCodeAllowed(String code) {
        return Stream.of(values()).noneMatch(st -> st.getCode().equals(code));
    }
}
